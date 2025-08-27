package storage.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import collections.QuickList;
import config.ProtocolConstants;
import storage.expiry.ExpiryPolicy;
import storage.types.ListValue;
import storage.types.StoredValue;
import storage.types.StringValue;

/**
 * RdbRepository is responsible for persistence using Redis RDB-like snapshots.
 * <p>
 * Supports saving in RDB file format and restoring the in-memory store
 * from an existing snapshot file.
 * </p>
 */
public class RdbRepository implements PersistentRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdbRepository.class);

    /** RDB file header length in bytes */
    private static final int RDB_HEADER_LENGTH = 9;

    /** Bit masks for RDB length encoding */
    private static final int SIX_BIT_MASK = 0x00;
    private static final int FOURTEEN_BIT_MASK = 0x40;
    private static final int THIRTYTWO_BIT_MASK = 0x80;

    private final Map<String, StoredValue<?>> store;

    public RdbRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    /**
     * Saves a snapshot of the in-memory store to an RDB file.
     *
     * @param rdbFile file to save snapshot into
     */
    @Override
    public void saveSnapshot(File rdbFile) {
        ensureParentDirectory(rdbFile);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(rdbFile))) {
            out.writeBytes(ProtocolConstants.RDB_FILE_HEADER);

            for (var entry : store.entrySet()) {
                String key = entry.getKey();
                StoredValue<?> value = entry.getValue();

                if (value.isExpired()) {
                    continue;
                }

                out.writeByte(ProtocolConstants.RDB_KEY_INDICATOR);
                writeEntry(out, key);

                switch (value.type()) {
                    case STRING -> writeStringValue(out, (StringValue) value);
                    case LIST -> writeListValue(out, (ListValue) value);
                    default -> throw new PersistenceException(
                            "Unsupported value type: " + value.type());
                }
            }

            out.writeByte(ProtocolConstants.RDB_OPCODE_EOF);
            out.flush();

            LOGGER.info("Snapshot saved successfully at {}", rdbFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save RDB snapshot", e);
            throw new PersistenceException("Failed to save RDB snapshot", e);
        }
    }

    /**
     * Loads the in-memory store from a given RDB snapshot file.
     *
     * @param rdbFile snapshot file
     * @throws IOException if file is invalid or corrupted
     */
    @Override
    public void loadSnapshot(File rdbFile) throws IOException {
        if (!rdbFile.exists()) {
            LOGGER.debug("No RDB file found at {}", rdbFile.getAbsolutePath());
            return;
        }

        validateFile(rdbFile);

        try (DataInputStream in = new DataInputStream(new FileInputStream(rdbFile))) {
            validateHeader(in);
            store.clear();

            boolean eofReached = false;
            while (!eofReached) {
                int type = in.readByte() & 0xFF;
                Long expiryTimeMs = null;

                // Handle expiry time
                if (type == ProtocolConstants.RDB_OPCODE_EXPIRE_TIME_SEC) {
                    int expirySec = Integer.reverseBytes(in.readInt());
                    expiryTimeMs = (long) expirySec * 1000;
                    type = in.readByte() & 0xFF;
                } else if (type == ProtocolConstants.RDB_OPCODE_EXPIRE_TIME_MS) {
                    expiryTimeMs = Long.reverseBytes(in.readLong());
                    type = in.readByte() & 0xFF;
                }

                if (type == ProtocolConstants.RDB_OPCODE_EOF) {
                    LOGGER.info("RDB snapshot loaded successfully, {} keys restored", store.size());
                    eofReached = true;
                    continue;
                }

                if (!handleSpecialOpCodes(in, type)) {
                    String key = readEntry(in);
                    StoredValue<?> value = switch (type) {
                        case ProtocolConstants.RDB_KEY_INDICATOR,
                                ProtocolConstants.RDB_STRING_VALUE_INDICATOR ->
                            readStringValue(in, expiryTimeMs);
                        case ProtocolConstants.RDB_LIST_VALUE_INDICATOR ->
                            readListValue(in, expiryTimeMs);
                        default -> throw new PersistenceException("Unknown value type: " + type);
                    };

                    store.put(key, value);
                }
            }
        }
    }

    /* ---------- Write helpers ---------- */

    private void writeStringValue(DataOutputStream out, StringValue value) throws IOException {
        out.writeByte(ProtocolConstants.RDB_STRING_VALUE_INDICATOR);
        writeEntry(out, value.value());
    }

    private void writeListValue(DataOutputStream out, ListValue list) throws IOException {
        out.writeByte(ProtocolConstants.RDB_LIST_VALUE_INDICATOR);
        out.writeInt(list.size());
        for (String item : list.value()) {
            writeEntry(out, item);
        }
    }

    /* ---------- Read helpers ---------- */

    private StoredValue<?> readStringValue(DataInputStream in, Long expiryTimeMs) throws IOException {
        String value = readEntry(in);
        ExpiryPolicy expiryPolicy = expiryTimeMs != null
                ? ExpiryPolicy.at(Instant.ofEpochMilli(expiryTimeMs))
                : ExpiryPolicy.never();
        return StringValue.of(value, expiryPolicy);
    }

    private StoredValue<?> readListValue(DataInputStream in, Long expiryTimeMs) throws IOException {
        int listSize = readEncodedLength(in);
        QuickList<String> list = new QuickList<>();
        for (int i = 0; i < listSize; i++) {
            list.pushRight(readEntry(in));
        }
        ExpiryPolicy expiryPolicy = expiryTimeMs != null
                ? ExpiryPolicy.at(Instant.ofEpochMilli(expiryTimeMs))
                : ExpiryPolicy.never();
        return new ListValue(list, expiryPolicy);
    }

    /* ---------- Utility methods ---------- */

    private boolean handleSpecialOpCodes(DataInputStream in, int type) throws IOException {
        switch (type) {
            case ProtocolConstants.RDB_OPCODE_METADATA -> {
                skipMetadata(in);
                return true;
            }
            case ProtocolConstants.RDB_OPCODE_SELECTDB -> {
                skipDatabaseSelection(in);
                return true;
            }
            case ProtocolConstants.RDB_OPCODE_RESIZEDB -> {
                skipResizeDatabase(in);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void ensureParentDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new PersistenceException("Failed to create directories: " + file.getAbsolutePath());
        }
    }

    private void validateFile(File rdbFile) {
        if (!rdbFile.isFile()) {
            throw new PersistenceException("Invalid RDB file: " + rdbFile.getAbsolutePath());
        }
    }

    private void validateHeader(DataInputStream in) throws IOException {
        byte[] header = new byte[RDB_HEADER_LENGTH];
        if (in.read(header) != RDB_HEADER_LENGTH ||
                !new String(header, StandardCharsets.UTF_8).equals(ProtocolConstants.RDB_FILE_HEADER)) {
            throw new IOException("Invalid RDB file header");
        }
    }

    private void skipMetadata(DataInputStream in) throws IOException {
        int keyLength = readEncodedLength(in);
        in.skipBytes(keyLength);

        int firstByte = in.readByte() & 0xFF;
        if ((firstByte & 0xC0) == 0xC0 && firstByte == 0xC0) {
            in.readByte();
        } else {
            in.skipBytes(firstByte);
        }
    }

    private void skipDatabaseSelection(DataInputStream in) throws IOException {
        in.readByte(); // usually 1 byte
    }

    private void skipResizeDatabase(DataInputStream in) throws IOException {
        readEncodedLength(in); // database hash table size
        readEncodedLength(in); // expire hash table size
    }

    private int readEncodedLength(DataInputStream in) throws IOException {
        int firstByte = in.readByte() & 0xFF;

        if ((firstByte & 0xC0) == SIX_BIT_MASK) {
            return firstByte & 0x3F;
        } else if ((firstByte & 0xC0) == FOURTEEN_BIT_MASK) {
            int secondByte = in.readByte() & 0xFF;
            return ((firstByte & 0x3F) << 8) | secondByte;
        } else if ((firstByte & 0xC0) == THIRTYTWO_BIT_MASK) {
            return in.readInt();
        } else {
            throw new IOException("Unsupported length encoding: " + firstByte);
        }
    }

    private String readEntry(DataInputStream in) throws IOException {
        int length = readEncodedLength(in);
        byte[] data = in.readNBytes(length);
        if (data.length != length) {
            throw new IOException("Failed to read complete entry");
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private void writeEntry(DataOutputStream out, String entry) throws IOException {
        byte[] entryBytes = entry.getBytes(StandardCharsets.UTF_8);
        out.writeInt(entryBytes.length);
        out.write(entryBytes);
    }
}
