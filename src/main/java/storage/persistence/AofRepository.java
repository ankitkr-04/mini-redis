package storage.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * Handles writing commands to the AOF file in Redis RESP format
 * and replaying them on restart to rebuild the dataset.
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

    /** Initializes the AOF file in append mode. */
    public void initializeAofFile(File aofFile) {
        this.aofFile = aofFile;
        ensureParentDirectory(aofFile);
        try {
            this.aofWriter = new BufferedWriter(new FileWriter(aofFile, true));
            LOGGER.info("AOF file initialized: {}", aofFile.getAbsolutePath());
        } catch (IOException e) {
            throw new PersistenceException("Failed to initialize AOF file", e);
        }
    }

    /** Appends a command to the AOF file in RESP format. */
    public void appendCommand(String[] command) {
        if (aofWriter == null)
            return;

        lock.writeLock().lock();
        try {
            String respCommand = formatRespArray(command);
            aofWriter.write(respCommand);
            aofWriter.flush();
            serverContext.getMetricsCollector().incrementAofWrites();
        } catch (IOException e) {
            serverContext.getMetricsCollector().incrementPersistenceErrors();
            throw new PersistenceException("Failed to append command to AOF: " + String.join(" ", command), e);
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
        LOGGER.debug("saveSnapshot called in AOF mode - ignored");
    }

    @Override
    public void loadSnapshot(File aofFile) {
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
        } catch (IOException e) {
            throw new PersistenceException("Failed to load AOF file: " + aofFile.getAbsolutePath(), e);
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

        if (contentBuilder.length() == 0)
            return;

        ByteBuffer buffer = ByteBuffer.wrap(contentBuilder.toString().getBytes(StandardCharsets.UTF_8));
        List<String[]> commands = ProtocolParser.parseRespArrays(buffer);

        for (String[] command : commands) {
            if (command != null && command.length > 0) {
                replaySingleCommand(command);
            }
        }
    }

    /** Refactored to reduce Cognitive Complexity. */
    private void replaySingleCommand(String[] command) {
        if (storageService == null) {
            LOGGER.warn("StorageService not set, cannot replay command: {}", String.join(" ", command));
            return;
        }

        String cmd = command[0].toUpperCase();
        try {
            switch (cmd) {
                case SET_COMMAND -> handleSet(command);
                case DEL_COMMAND -> handleDel(command);
                case LPUSH_COMMAND -> handleListPush(command, true);
                case RPUSH_COMMAND -> handleListPush(command, false);
                case LPOP_COMMAND -> handleListPop(command, true);
                case RPOP_COMMAND -> handleListPop(command, false);
                case INCR_COMMAND -> handleIncr(command);
                case ZADD_COMMAND -> handleZadd(command);
                case ZREM_COMMAND -> handleZrem(command);
                default -> LOGGER.debug("Unsupported command in AOF replay: {}", cmd);
            }
        } catch (Exception e) {
            throw new PersistenceException("Failed to replay command: " + String.join(" ", command), e);
        }
    }

    /* ---------- Extracted handlers to reduce complexity ---------- */
    private void handleSet(String[] cmd) {
        if (cmd.length >= 3) {
            storageService.setString(cmd[1], cmd[2], storage.expiry.ExpiryPolicy.never());
        }
    }

    private void handleDel(String[] cmd) {
        for (int i = 1; i < cmd.length; i++)
            storageService.delete(cmd[i]);
    }

    private void handleListPush(String[] cmd, boolean left) {
        if (cmd.length >= 3) {
            String[] values = extractValues(cmd, 2);
            if (left)
                storageService.leftPush(cmd[1], values);
            else
                storageService.rightPush(cmd[1], values);
        }
    }

    private void handleListPop(String[] cmd, boolean left) {
        if (cmd.length >= 2) {
            if (left)
                storageService.leftPop(cmd[1]);
            else
                storageService.rightPop(cmd[1]);
        }
    }

    private void handleIncr(String[] cmd) {
        if (cmd.length >= 2)
            storageService.incrementString(cmd[1]);
    }

    private void handleZadd(String[] cmd) {
        if (cmd.length >= 4) {
            double score = parseScore(cmd[2]);
            if (!Double.isNaN(score)) {
                storageService.zAdd(cmd[1], cmd[3], score);
            }
        }
    }

    private void handleZrem(String[] cmd) {
        if (cmd.length >= 3)
            storageService.zRemove(cmd[1], cmd[2]);
    }

    private double parseScore(String scoreStr) {
        try {
            return Double.parseDouble(scoreStr);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid score in ZADD command: {}", scoreStr);
            return Double.NaN;
        }
    }

    private String[] extractValues(String[] command, int startIndex) {
        String[] values = new String[command.length - startIndex];
        System.arraycopy(command, startIndex, values, 0, values.length);
        return values;
    }

    public void close() {
        lock.writeLock().lock();
        try {
            if (aofWriter != null) {
                aofWriter.flush();
                aofWriter.close();
                aofWriter = null;
            }
        } catch (IOException e) {
            throw new PersistenceException("Error closing AOF writer", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void ensureParentDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new PersistenceException("Failed to create directories: " + file.getAbsolutePath());
        }
    }

    /** Clears the AOF file and creates a fresh one. */
    public void clear() {
        lock.writeLock().lock();
        try {
            if (aofWriter != null) {
                aofWriter.flush();
                aofWriter.close();
                aofWriter = null;
            }

            if (aofFile != null && aofFile.exists()) {
                Files.delete(aofFile.toPath());
            }

            if (aofFile != null) {
                ensureParentDirectory(aofFile);
                if (!aofFile.createNewFile()) {
                    throw new PersistenceException("Failed to recreate AOF file: " + aofFile.getAbsolutePath());
                }
                aofWriter = new BufferedWriter(new FileWriter(aofFile, true));
                LOGGER.info("AOF file cleared and recreated: {}", aofFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new PersistenceException("Error while clearing AOF file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
