import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler {
    
    public static AuthHandler AUTH_HANDLER = new AuthHandler();

    private Map<String, String> keyToToken = new HashMap<>();
    
    private AuthHandler() {
            try {
                Files.readAllLines(Path.of("env")).forEach(line -> {
                    String[] keyToken = line.split("=");
                    keyToToken.put(keyToken[0], keyToken.length > 1 ? keyToken[1] : "");
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }
    
    public String getToken(String key, String defaultVal) {
        return keyToToken.getOrDefault(key, defaultVal);
    }
    
    public void putToken(String key, String value) {
        keyToToken.put(key, value);
        try (BufferedWriter buff = new BufferedWriter(new FileWriter("env"))) {
            for (Map.Entry<String, String> entry : keyToToken.entrySet()) {
                buff.write(entry.getKey() + "=" + entry.getValue());
                buff.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
