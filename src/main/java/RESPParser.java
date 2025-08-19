import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RESPParser {

    public static String[] parse(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String str = new String(bytes, StandardCharsets.UTF_8);

        if (str.isEmpty())
            return new String[] {};

        if (str.charAt(0) == '*') { // RESP Array
            String[] lines = str.split("\r\n");
            if (lines.length < 2)
                return new String[] {};

            try {
                int arraySize = Integer.parseInt(lines[0].substring(1));
                String[] result = new String[arraySize];
                int idx = 0;

                for (int i = 1; i < lines.length && idx < arraySize; i++) {
                    String line = lines[i];
                    if (line.startsWith("$")) {
                        if (i + 1 < lines.length) {
                            result[idx++] = lines[i + 1];
                            i++; // skip content line
                        } else {
                            break; // malformed RESP, stop parsing
                        }
                    }
                }
                return result;
            } catch (NumberFormatException e) {
                return new String[] {};
            }
        }

        return new String[] {str.trim()}; // Simple string or single line
    }
}
