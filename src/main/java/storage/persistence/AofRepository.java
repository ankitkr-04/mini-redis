package storage.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.ProtocolParser;
import server.ServerContext;
import storage.StorageService;
import storage.types.StoredValue;

/**
 * Append-Only File (AOF) persistence repository.
 *
 * <p>
 * Handles writing commands to the AOF file in Redis RESP format
 * and replaying them on restart to rebuild the dataset.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public class AofRepository implements PersistentRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(AofRepository.class);

    private static final String RESP_LINE_ENDING = "\r\n";
    private static final String SET_COMMAND = "SET";
    private static final String DEL_COMMAND = "DEL";
    private static final String LPUSH_COMMAND = "LPUSH";
    private static final String RPUSH_COMMAND = "RPUSH";
    private static final String LPOP_COMMAND = "LPOP";
    private static final String RPOP_COMMAND = "RPOP";
    private static final String INCR_COMMAND = "INCR";
    private static final String ZADD_COMMAND = "ZADD";
    private static final String ZREM_COMMAND = "ZREM";

    private final Map<String, StoredValue<?>> store;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private File aofFile;
    private BufferedWriter aofWriter;
    private StorageService storageService;
    private final ServerContext serverContext;

    public AofRepository(Map<String, StoredValue<?>> store, ServerContext serverContext) {
        this.store = store;
        this.serverContext = serverContext;
    }

    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Initializes the AOF file in append mode.
     */
    public void initializeAofFile(File aofFile) throws IOException {
        this.aofFile = aofFile;
        ensureParentDirectory(aofFile);
        this.aofWriter = new BufferedWriter(new FileWriter(aofFile, true));
        LOGGER.info("AOF file initialized: {}", aofFile.getAbsolutePath());
    }

    /**
     * Appends a command to the AOF file in RESP format.
     */
    public void appendCommand(String[] command) {
        if (aofWriter == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            String respCommand = formatRespArray(command);
            aofWriter.write(respCommand);
            aofWriter.flush();
            serverContext.getMetricsCollector().incrementAofWrites();
        } catch (IOException e) {
            LOGGER.error("Failed to append command to AOF: {}", String.join(" ", command), e);
            serverContext.getMetricsCollector().incrementPersistenceErrors();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String formatRespArray(String[] command) {
        StringBuilder respBuilder = new StringBuilder();
        respBuilder.append("*").append(command.length).append(RESP_LINE_ENDING);
        for (String arg : command) {
            respBuilder.append("$").append(arg.length()).append(RESP_LINE_ENDING);
            respBuilder.append(arg).append(RESP_LINE_ENDING);
        }
        return respBuilder.toString();
    }

    @Override
    public void saveSnapshot(File rdbFile) {
        // AOF does not require snapshot saving
        LOGGER.debug("saveSnapshot called in AOF mode - ignored");
    }

    @Override
    public void loadSnapshot(File aofFile) throws IOException {
        if (!aofFile.exists()) {
            LOGGER.info("AOF file does not exist: {}", aofFile.getAbsolutePath());
            return;
        }

        LOGGER.info("Loading AOF file: {}", aofFile.getAbsolutePath());
        this.aofFile = aofFile;

        lock.writeLock().lock();
        try {
            store.clear();
            replayAofCommands();
            this.aofWriter = new BufferedWriter(new FileWriter(aofFile, true));
            LOGGER.info("AOF file loaded successfully. Store now contains {} keys", store.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void replayAofCommands() throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(aofFile))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                contentBuilder.append(currentLine).append(RESP_LINE_ENDING);
            }
        }

        if (contentBuilder.length() == 0) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(contentBuilder.toString().getBytes(StandardCharsets.UTF_8));
        List<String[]> commands = ProtocolParser.parseRespArrays(buffer);

        for (String[] command : commands) {
            if (command != null && command.length > 0) {
                replaySingleCommand(command);
            }
        }
    }

    private void replaySingleCommand(String[] command) {
        if (storageService == null) {
            LOGGER.warn("StorageService not set, cannot replay command: {}", String.join(" ", command));
            return;
        }

        String commandName = command[0].toUpperCase();
        try {
            switch (commandName) {
                case SET_COMMAND -> {
                    if (command.length >= 3) {
                        storageService.setString(command[1], command[2], storage.expiry.ExpiryPolicy.never());
                    }
                }
                case DEL_COMMAND -> {
                    for (int i = 1; i < command.length; i++) {
                        storageService.delete(command[i]);
                    }
                }
                case LPUSH_COMMAND -> {
                    if (command.length >= 3) {
                        String[] values = extractValues(command, 2);
                        storageService.leftPush(command[1], values);
                    }
                }
                case RPUSH_COMMAND -> {
                    if (command.length >= 3) {
                        String[] values = extractValues(command, 2);
                        storageService.rightPush(command[1], values);
                    }
                }
                case LPOP_COMMAND -> {
                    if (command.length >= 2) {
                        storageService.leftPop(command[1]);
                    }
                }
                case RPOP_COMMAND -> {
                    if (command.length >= 2) {
                        storageService.rightPop(command[1]);
                    }
                }
                case INCR_COMMAND -> {
                    if (command.length >= 2) {
                        storageService.incrementString(command[1]);
                    }
                }
                case ZADD_COMMAND -> {
                    if (command.length >= 4) {
                        try {
                            double score = Double.parseDouble(command[2]);
                            storageService.zAdd(command[1], command[3], score);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid score in ZADD command: {}", command[2]);
                        }
                    }
                }
                case ZREM_COMMAND -> {
                    if (command.length >= 3) {
                        storageService.zRemove(command[1], command[2]);
                    }
                }
                default -> LOGGER.debug("Unsupported command in AOF replay: {}", commandName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to replay command: {}", String.join(" ", command), e);
        }
    }

    private String[] extractValues(String[] command, int startIndex) {
        String[] values = new String[command.length - startIndex];
        System.arraycopy(command, startIndex, values, 0, values.length);
        return values;
    }

    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            if (aofWriter != null) {
                aofWriter.flush();
                aofWriter.close();
                aofWriter = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void ensureParentDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalArgumentException("Failed to create directories: " + file.getAbsolutePath());
        }
    }
}
