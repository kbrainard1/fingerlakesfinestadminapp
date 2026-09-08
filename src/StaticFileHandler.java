import com.sun.net.httpserver.HttpExchange;  
import com.sun.net.httpserver.HttpHandler;  
import java.io.IOException;  
import java.nio.file.Files;  
import java.nio.file.Path;  
import java.nio.file.Paths;  
import java.util.HashMap;  
import java.util.Map;  
 
public class StaticFileHandler implements HttpHandler {  
    private final String staticDir; // Path to static files directory  
    private final Map<String, String> mimeTypes = new HashMap<>();  
 
    // Constructor: Accepts path to static directory (e.g., "src/main/resources/static")  
    public StaticFileHandler(String staticDir) {  
        this.staticDir = staticDir;  
        initMimeTypes();  
    }  
 
    // Initialize MIME type mappings for common file extensions  
    private void initMimeTypes() {  
        mimeTypes.put(".html", "text/html");  
        mimeTypes.put(".css", "text/css");  
        mimeTypes.put(".js", "application/javascript");  
        mimeTypes.put(".png", "image/png");  
        mimeTypes.put(".jpg", "image/jpeg");  
        mimeTypes.put(".svg", "image/svg+xml");  
        mimeTypes.put(".txt", "text/plain");  
        // Add more as needed (e.g., .json, .ico)  
    }  
 
    @Override  
    public void handle(HttpExchange exchange) throws IOException {  
        try {  
            // Only allow GET requests  
            if (!"GET".equals(exchange.getRequestMethod())) {  
                sendError(exchange, 405, "Method Not Allowed");  
                return;  
            }  
 
            // Get request path (e.g., "/index.html" → "index.html")  
            String requestPath = exchange.getRequestURI().getPath();  
            String filePath = requestPath.equals("/") ? "index.html" : requestPath.substring(1);  
 
            // Resolve full file path (staticDir + filePath)  
            Path fullPath = Paths.get(staticDir, filePath).normalize();  
 
            // Validate: Prevent directory traversal attacks (e.g., ../../secret.txt)  
            if (!fullPath.startsWith(Paths.get(staticDir).normalize())) {  
                sendError(exchange, 403, "Forbidden");  
                return;  
            }  
 
            // Check if file exists  
            if (!Files.exists(fullPath) || Files.isDirectory(fullPath)) {  
                sendError(exchange, 404, "File Not Found");  
                return;  
            }  
 
            // Read file content  
            byte[] fileContent = Files.readAllBytes(fullPath);  
 
            // Determine MIME type from file extension  
            String extension = getFileExtension(fullPath.getFileName().toString());  
            String contentType = mimeTypes.getOrDefault(extension, "application/octet-stream");  
 
            // Send response  
            exchange.getResponseHeaders().set("Content-Type", contentType);  
            exchange.sendResponseHeaders(200, fileContent.length);  
            try (var os = exchange.getResponseBody()) {  
                os.write(fileContent);  
            }  
 
        } catch (IOException e) {  
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());  
        } finally {  
            exchange.close();  
        }  
    }  
 
    // Helper: Extract file extension (e.g., "style.css" → ".css")  
    private String getFileExtension(String fileName) {  
        int lastDotIndex = fileName.lastIndexOf('.');  
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex).toLowerCase();  
    }  
 
    // Helper: Send error response  
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {  
        byte[] errorBytes = message.getBytes();  
        exchange.sendResponseHeaders(statusCode, errorBytes.length);  
        try (var os = exchange.getResponseBody()) {  
            os.write(errorBytes);  
        }  
    }  
}  