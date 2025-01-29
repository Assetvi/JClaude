# Anthropic API Client for Java
This is a Java library for accessing the Anthropic Claude API. It supports sending text messages and images to the Claude AI models, making it easy to integrate advanced AI capabilities into your Java applications.
## 🚀 Features
- **Streaming responses** for real-time text generation
- Send images (JPEG, PNG, GIF, WebP) to the Claude API
- Handles various response statuses and errors from the API
- Supports multiple Claude AI models
- Simple, **easy-to-use CLI demo**
## Supported Models
- claude-3-5-sonnet-20240620
- claude-3-opus-20240229
- claude-3-sonnet-20240229
- claude-3-haiku-20240307
## Supported Image Types
- image/jpeg
- image/png
- image/gif
- image/webp
## 🛠️ Prerequisites
- Java 17+
- A valid **Anthropic API Key**
- Internet access
## Installation
Did your project has gson install? if not, download the [lib/gson-2.11.0.jar](lib/gson-2.11.0.jar)</br>
Then, just download [src/AnthropicApiClient.java](src/AnthropicApiClient.java)
## 🚀 Example: Streaming Response from Claude API

To stream responses from Claude, use the `sendMessageWithStreaming` method.

### **Code Example**
```java
import src.AnthropicApiClient;

public class ClaudeStreamingExample {
    public static void main(String[] args) {
        // Initialize API client with your API key
        String apiKey = "your_anthropic_api_key"; // Replace with your actual key
        int maxTokens = 1024;
        String model = "claude-3-5-sonnet-20240620";
        Double temperature = 0.7;

        AnthropicApiClient claudeApi = new AnthropicApiClient(apiKey, maxTokens, model, temperature);

        // Message to send
        String userMessage = "Claude, how do I test API streaming?";

        // Stream response from Claude
        claudeApi.sendMessageWithStreaming(userMessage, null, new AnthropicApiClient.StreamCallback() {
            @Override
            public void onMessage(String message) {
                System.out.print(message);  // Print in real-time without extra logs
            }

            @Override
            public void onError(String error) {
                System.err.println("\nError: " + error);
            }

            @Override
            public void onComplete() {
                System.out.println("\n\n--- Response complete ---");
            }
        });
    }
}

