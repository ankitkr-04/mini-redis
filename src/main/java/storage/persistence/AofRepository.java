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

public class AofRepository implements PersistentRepository {
    private static final Logger log = LoggerFactory.getLogger(AofRepository.class);

    private final Map<String, StoredValue<?>> store;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private File aofFile;
    private BufferedWriter aofWriter;
    private StorageService storageService;
    private ServerContext serverContext;

    public AofRepository(Map<String, StoredValue<?>> store, ServerContext serverContext) {
        this.store = store;
        this.serverContext = serverContext;
    }

    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public void initializeAofFile(File aofFile) throws IOException {
        this.aofFile = aofFile;
        ensureParentDirectory(aofFile);
        this.aofWriter = new BufferedWriter(new FileWriter(aofFile, true)); // append mode
        log.info("AOF file initialized: {}", aofFile.getAbsolutePath());
    }

    public void appendCommand(String[] command) {
        if (aofWriter == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            // Write in Redis protocol format (RESP)
            String respCommand = formatRespArray(command);
            aofWriter.write(respCommand);
            aofWriter.flush(); // Ensure immediate write for durability

            // Record AOF write metric
            serverContext.getMetricsCollector().incrementAofWrites();
        } catch (IOException e) {
            log.error("Failed to append command to AOF: {}", String.join(" ", command), e);
            serverContext.getMetricsCollector().incrementPersistenceErrors();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String formatRespArray(String[] command) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(command.length).append("\r\n");
        for (String arg : command) {
            sb.append("$").append(arg.length()).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        return sb.toString();
    }

    @Override
    public void saveSnapshot(File rdbFile) {
        // AOF doesn't need to save snapshots in the traditional sense
        // but we can implement BGREWRITEAOF functionality here in the future
        log.info("AOF saveSnapshot called - no action needed for AOF mode");
    }

    @Override
    public void loadSnapshot(File aofFile) throws IOException {
        if (!aofFile.exists()) {
            log.info("AOF file does not exist: {}", aofFile.getAbsolutePath());
            return;
        }

        log.info("Loading AOF file: {}", aofFile.getAbsolutePath());
        this.aofFile = aofFile;

        lock.writeLock().lock();
        try {
            store.clear();
            replayAofCommands();

            // Initialize writer for future commands
            this.aofWriter = new BufferedWriter(new FileWriter(aofFile, true));

            log.info("AOF file loaded successfully. Store now contains {} keys", store.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void replayAofCommands() throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(aofFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\r\n");
            }
        }

        if (fileContent.length() == 0) {
            return;
        }

        // Parse using the existing ProtocolParser
        ByteBuffer buffer = ByteBuffer.wrap(fileContent.toString().getBytes(StandardCharsets.UTF_8));
        List<String[]> commands = ProtocolParser.parseRespArrays(buffer);

        for (String[] command : commands) {
            if (command != null && command.length > 0) {
                executeCommandForReplay(command);
            }
        }
    }

    private void executeCommandForReplay(String[] command) {
        if (storageService == null) {
            log.warn("StorageService not set, cannot replay command: {}", String.join(" ", command));
            return;
        }

        try {
            // Execute the command directly on storage service based on command type
            String commandName = command[0].toUpperCase();

            switch (commandName) {
                case "SET" -> {
                    if (command.length >= 3) {
                        storageService.setString(command[1], command[2],
                                storage.expiry.ExpiryPolicy.never());
                    }
                }
                case "DEL" -> {
                    for (int i = 1; i < command.length; i++) {
                        storageService.delete(command[i]);
                    }
                }
                case "LPUSH" -> {
                    if (command.length >= 3) {
                        String[] values = new String[command.length - 2];
                        System.arraycopy(command, 2, values, 0, values.length);
                        storageService.leftPush(command[1], values);
                    }
                }
                case "RPUSH" -> {
                    if (command.length >= 3) {
                        String[] values = new String[command.length - 2];
                        System.arraycopy(command, 2, values, 0, values.length);
                        storageService.rightPush(command[1], values);
                    }
                }
                case "LPOP" -> {
                    if (command.length >= 2) {
                        storageService.leftPop(command[1]);
                    }
                }
                case "RPOP" -> {
                    if (command.length >= 2) {
                        storageService.rightPop(command[1]);
                    }
                }
                case "INCR" -> {
                    if (command.length >= 2) {
                        storageService.incrementString(command[1]);
                    }
                }
                case "ZADD" -> {
                    if (command.length >= 4) {
                        try {
                            double score = Double.parseDouble(command[2]);
                            storageService.zAdd(command[1], command[3], score);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid score in ZADD command: {}", command[2]);
                        }
                    }
                }
                case "ZREM" -> {
                    if (command.length >= 3) {
                        storageService.zRemove(command[1], command[2]);
                    }
                }
                // Add more commands as needed
                default -> log.debug("Command not supported for AOF replay: {}", commandName);
            }
        } catch (Exception e) {
            log.error("Failed to replay command: {}", String.join(" ", command), e);
        }
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
