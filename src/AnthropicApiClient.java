package src;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class AnthropicApiClient {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final List<String> VALID_MODELS = Arrays.asList(
        "claude-3-5-sonnet-20240620",
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307"
    );
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final String apiKey;
    private final int maxTokens;
    private final String model;
    private final Double temperature;
    private final Gson gson;
    private final HttpClient httpClient;

    public AnthropicApiClient(String apiKey, int maxTokens, String model, Double temperature) {
        this.apiKey = apiKey;
        this.maxTokens = maxTokens;
        
        if (!VALID_MODELS.contains(model)) {
            throw new IllegalArgumentException("Invalid model. Supported models are: " + String.join(", ", VALID_MODELS));
        }
        this.model = model;
        
        this.temperature = temperature != null ? temperature : 0.0;
        this.gson = new Gson();
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendMessageWithStreaming(String message, String imagePath, StreamCallback callback) {
        try {
            String jsonBody = buildJsonBody(message, imagePath);
            HttpRequest request = buildHttpRequest(jsonBody);
    
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        int statusCode = response.statusCode();
                        if (statusCode != 200) {
                            callback.onError("Error: " + handleErrorCode(statusCode));
                            return;
                        }
    
                        response.body().forEach(line -> {
                            try {
                                if (!line.isEmpty() && line.startsWith("data: ")) {
                                    // Remove "data: " prefix
                                    line = line.substring(6).trim();
    
                                    JsonObject json = gson.fromJson(line, JsonObject.class);
    
                                    // Extract only text from `content_block_delta` events
                                    if (json.has("type") && json.get("type").getAsString().equals("content_block_delta")) {
                                        JsonObject delta = json.getAsJsonObject("delta");
                                        if (delta.has("text")) {
                                            callback.onMessage(delta.get("text").getAsString());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                callback.onError("Error parsing JSON: " + e.getMessage());
                            }
                        });
    
                        callback.onComplete();
                    })
                    .exceptionally(ex -> {
                        callback.onError("Streaming error: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            callback.onError("Error: " + e.getMessage());
        }
    }
    
    
    
    

    private String buildJsonBody(String message, String imagePath) throws IOException, InterruptedException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", model);
        jsonBody.addProperty("max_tokens", maxTokens);
        jsonBody.addProperty("stream", true); // Enable streaming
        if (temperature != null) {
            jsonBody.addProperty("temperature", temperature);
        }

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        
        JsonArray content = new JsonArray();
        
        if (imagePath != null) {
            content.add(createImageContent(imagePath));
        }
        
        content.add(createTextContent(message));
        
        userMessage.add("content", content);
        messages.add(userMessage);
        jsonBody.add("messages", messages);

        return gson.toJson(jsonBody);
    }

    private JsonObject createTextContent(String text) {
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", text);
        return textContent;
    }

    private JsonObject createImageContent(String imagePath) throws IOException, InterruptedException {
        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("type", "image");
        
        JsonObject source = new JsonObject();
        source.addProperty("type", "base64");
        
        Path path = Paths.get(imagePath);
        if (Files.exists(path)) {
            byte[] imageBytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mediaType = Files.probeContentType(path);
            
            if (mediaType == null || !SUPPORTED_IMAGE_TYPES.contains(mediaType)) {
                throw new IllegalArgumentException("Unsupported image type. Supported types are: " 
                    + String.join(", ", SUPPORTED_IMAGE_TYPES));
            }
            
            source.addProperty("media_type", mediaType);
            source.addProperty("data", base64);
        } else {
            throw new IllegalArgumentException("File does not exist: " + imagePath);
        }
        
        imageContent.add("source", source);
        return imageContent;
    }

    private HttpRequest buildHttpRequest(String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private String handleErrorCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Invalid request error.";
            case 401 -> "Authentication error.";
            case 403 -> "Permission error.";
            case 404 -> "Not found error.";
            case 429 -> "Rate limit error.";
            case 500 -> "API error.";
            case 529 -> "Overloaded error.";
            default -> "Unknown error.";
        };
    }

    public interface StreamCallback {
        void onMessage(String message);
        void onError(String error);
        void onComplete();
    }
}
