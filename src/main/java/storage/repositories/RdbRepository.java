package storage.repositories;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import collections.QuickList;
import config.ProtocolConstants;
import storage.PersistentRepository;
import storage.expiry.ExpiryPolicy;
import storage.types.ListValue;
import storage.types.StoredValue;
import storage.types.StringValue;

public class RdbRepository implements PersistentRepository {

    private final Map<String, StoredValue<?>> store;

    public RdbRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void saveSnapshot(File rdbFile) {
        ensureParentDirectory(rdbFile);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(rdbFile))) {
            out.writeBytes(ProtocolConstants.RDB_FILE_HEADER);

            for (var entry : store.entrySet()) {
                String key = entry.getKey();
                StoredValue<?> value = entry.getValue();

                if (value.isExpired())
                    continue;

                out.writeByte(ProtocolConstants.RDB_KEY_INDICATOR);
                writeEntry(out, key);

                switch (value.type()) {
                    case STRING -> writeStringValue(out, (StringValue) value);
                    case LIST -> writeListValue(out, (ListValue) value);
                    // case STREAM -> writeStreamValue(out, (StreamValue) value);
                    default -> throw new IllegalStateException("Unsupported value type: " + value.type());
                }
            }

            out.writeByte(ProtocolConstants.RDB_OPCODE_EOF);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save RDB snapshot: " + e.getMessage(), e);
        }
    }

    @Override
    public void loadSnapshot(File rdbFile) throws IOException {
        // If file doesn't exist, just return - this is normal for a fresh start
        if (!rdbFile.exists()) {
            return;
        }
        
        validateFile(rdbFile);

        try (DataInputStream in = new DataInputStream(new FileInputStream(rdbFile))) {
            validateHeader(in);

            store.clear();

            while (true) {
                int type = in.readByte() & 0xFF; // Ensure unsigned byte

                // Check for expiry time opcodes first
                Long expiryTimeMs = null;
                if (type == ProtocolConstants.RDB_OPCODE_EXPIRE_TIME_SEC) {
                    // Read 4-byte timestamp in seconds and convert to milliseconds
                    int expirySec = Integer.reverseBytes(in.readInt()); // Little endian
                    expiryTimeMs = (long) expirySec * 1000;
                    type = in.readByte() & 0xFF; // Read the actual value type
                } else if (type == ProtocolConstants.RDB_OPCODE_EXPIRE_TIME_MS) {
                    // Read 8-byte timestamp in milliseconds
                    expiryTimeMs = Long.reverseBytes(in.readLong()); // Little endian
                    type = in.readByte() & 0xFF; // Read the actual value type
                }

                if (type == ProtocolConstants.RDB_OPCODE_EOF) {
                    break;
                } else if (type == ProtocolConstants.RDB_OPCODE_METADATA) {
                    // Skip metadata sections
                    skipMetadata(in);
                    continue;
                } else if (type == ProtocolConstants.RDB_OPCODE_SELECTDB) {
                    // Skip database selection
                    skipDatabaseSelection(in);
                    continue;
                } else if (type == ProtocolConstants.RDB_OPCODE_RESIZEDB) {
                    // Skip resize database
                    skipResizeDatabase(in);
                    continue;
                } else {
                    // This is a value type indicator, followed by key-value pair
                    String key = readEntry(in);

                    StoredValue<?> value = switch (type) {
                        case ProtocolConstants.RDB_KEY_INDICATOR -> readStringValue(in, expiryTimeMs);
                        case ProtocolConstants.RDB_STRING_VALUE_INDICATOR -> readStringValue(in, expiryTimeMs);
                        case ProtocolConstants.RDB_LIST_VALUE_INDICATOR -> readListValue(in, expiryTimeMs);
                        // case ProtocolConstants.RDB_STREAM_VALUE_INDICATOR -> readStreamValue(in,
                        // expiryTimeMs);
                        default -> throw new IOException("Unknown value type: " + type);
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

    /*
     * private void writeStreamValue(DataOutputStream out, StreamValue streamValue)
     * throws IOException {
     * out.writeByte(ProtocolConstants.RDB_STREAM_VALUE_INDICATOR);
     * ConcurrentNavigableMap<String, StreamEntry> stream = streamValue.value();
     * out.writeInt(stream.size());
     * for (var streamEntry : stream.entrySet()) {
     * writeEntry(out, streamEntry.getKey());
     * Map<String, String> fields = streamEntry.getValue().fields();
     * out.writeInt(fields.size());
     * for (var field : fields.entrySet()) {
     * writeEntry(out, field.getKey());
     * writeEntry(out, field.getValue());
     * }
     * }
     * }
     */

    /* ---------- Read helpers ---------- */

    private StoredValue<?> readStringValue(DataInputStream in, Long expiryTimeMs) throws IOException {
        String value = readEntry(in);
        ExpiryPolicy expiryPolicy = (expiryTimeMs != null)
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
        ExpiryPolicy expiryPolicy = (expiryTimeMs != null)
                ? ExpiryPolicy.at(Instant.ofEpochMilli(expiryTimeMs))
                : ExpiryPolicy.never();
        return new ListValue(list, expiryPolicy);
    } /*
       * private StoredValue<?> readStreamValue(DataInputStream in) throws IOException
       * {
       * int streamSize = in.readInt();
       * ConcurrentNavigableMap<String, StreamEntry> stream = new
       * ConcurrentSkipListMap<>();
       * for (int i = 0; i < streamSize; i++) {
       * String id = readEntry(in);
       * int fieldCount = in.readInt();
       * Map<String, String> fields = new LinkedHashMap<>();
       * for (int j = 0; j < fieldCount; j++) {
       * fields.put(readEntry(in), readEntry(in));
       * }
       * stream.put(id, new StreamEntry(id, fields));
       * }
       * return new StreamValue(stream, ExpiryPolicy.never());
       * }
       */

    /* ---------- Utility methods ---------- */

    private void ensureParentDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalArgumentException("Failed to create directories: " + file.getAbsolutePath());
        }
    }

    private void validateFile(File rdbFile) {
        if (!rdbFile.isFile()) {
            throw new IllegalArgumentException("Invalid RDB file: " + rdbFile.getAbsolutePath());
        }
    }

    private void validateHeader(DataInputStream in) throws IOException {
        byte[] header = new byte[9];
        if (in.read(header) != 9
                || !new String(header, StandardCharsets.UTF_8).equals(ProtocolConstants.RDB_FILE_HEADER)) {
            throw new IOException("Invalid RDB file header");
        }
    }

    private void skipMetadata(DataInputStream in) throws IOException {
        // Read metadata key length and skip it
        int keyLength = readEncodedLength(in);
        in.skipBytes(keyLength);

        // Read metadata value (could be encoded or string)
        int firstByte = in.readByte() & 0xFF;
        if ((firstByte & 0xC0) == 0xC0) {
            // Special encoded value - already read the full value
            if (firstByte == 0xC0) {
                // Read one more byte for the actual value
                in.readByte();
            }
        } else {
            // String value - firstByte is the length
            in.skipBytes(firstByte);
        }
    }

    private void skipDatabaseSelection(DataInputStream in) throws IOException {
        // Skip database number (usually 1 byte)
        in.readByte();
    }

    private void skipResizeDatabase(DataInputStream in) throws IOException {
        // Skip database hash table size and expire hash table size
        readEncodedLength(in); // database hash table size
        readEncodedLength(in); // expire hash table size
    }

    private int readEncodedLength(DataInputStream in) throws IOException {
        int first = in.readByte() & 0xFF;

        if ((first & 0xC0) == 0x00) {
            // 6-bit length
            return first & 0x3F;
        } else if ((first & 0xC0) == 0x40) {
            // 14-bit length
            int second = in.readByte() & 0xFF;
            return ((first & 0x3F) << 8) | second;
        } else if ((first & 0xC0) == 0x80) {
            // 32-bit length
            return in.readInt();
        } else {
            // Special format
            throw new IOException("Unsupported length encoding: " + first);
        }
    }

    private String readEntry(DataInputStream in) throws IOException {
        int length = readEncodedLength(in);
        byte[] keyBytes = in.readNBytes(length);
        if (keyBytes.length != length) {
            throw new IOException("Failed to read complete entry");
        }
        return new String(keyBytes, StandardCharsets.UTF_8);
    }

    private void writeEntry(DataOutputStream out, String entry) throws IOException {
        byte[] entryBytes = entry.getBytes(StandardCharsets.UTF_8);
        out.writeInt(entryBytes.length);
        out.write(entryBytes);
    }
}
