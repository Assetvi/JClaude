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

    public String sendMessage(String message) {
        return sendMessageWithImage(message, null);
    }

    public String sendMessageWithImage(String message, String imagePath) {
        try {
            String jsonBody = buildJsonBody(message, imagePath);

            HttpRequest request = buildHttpRequest(jsonBody);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String errorMessage = handleErrorCode(statusCode);

            if (errorMessage != null) {
                return "Error: " + errorMessage;
            } else {
                return parseResponse(response.body());
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String buildJsonBody(String message, String imagePath) throws IOException, InterruptedException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", model);
        jsonBody.addProperty("max_tokens", maxTokens);
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
            // Local file
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
            // Assume it's a URL
            HttpResponse<byte[]> imageResponse = httpClient.send(
                HttpRequest.newBuilder(URI.create(imagePath)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
            );
            
            String mediaType = imageResponse.headers().firstValue("content-type").orElse("");
            if (!SUPPORTED_IMAGE_TYPES.contains(mediaType)) {
                throw new IllegalArgumentException("Unsupported image type. Supported types are: " 
                    + String.join(", ", SUPPORTED_IMAGE_TYPES));
            }
            
            String base64 = Base64.getEncoder().encodeToString(imageResponse.body());
            
            source.addProperty("media_type", mediaType);
            source.addProperty("data", base64);
        }
        
        imageContent.add("source", source);
        return imageContent;
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
            JsonObject content = json.getAsJsonArray("content").get(0).getAsJsonObject();
            return content.get("text").getAsString();
        } catch (Exception e) {
            return "Error parsing JSON: " + e.getMessage();
        }
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
        switch (statusCode) {
            case 200:
                return null; // Successful request
            case 400:
                return "Invalid request error: There was an issue with the format or content of your request.";
            case 401:
                return "Authentication error: There's an issue with your API key.";
            case 403:
                return "Permission error: Your API key does not have permission to use the specified resource.";
            case 404:
                return "Not found error: The requested resource was not found.";
            case 429:
                return "Rate limit error: Your account has hit a rate limit.";
            case 500:
                return "API error: An unexpected error has occurred internal to Anthropic's systems.";
            case 529:
                return "Overloaded error: Anthropic's API is temporarily overloaded.";
            default:
                return "Unknown error: An unexpected HTTP status code was received.";
        }
    }
}