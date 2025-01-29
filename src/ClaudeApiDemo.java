package src;

import java.util.Scanner;

public class ClaudeApiDemo {
    public static void main(String[] args) {
        String apiKey = ""; // Replace with your actual API key
        int maxTokens = 1024;
        String model = "claude-3-5-sonnet-20240620";
        Double temperature = 0.7;

        try {
            AnthropicApiClient claudeApi = new AnthropicApiClient(apiKey, maxTokens, model, temperature);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Enter your message to Claude (or 'quit' to exit): ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("quit")) {
                    break;
                }

                claudeApi.sendMessageWithStreaming(userInput, null, new AnthropicApiClient.StreamCallback() {
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
            scanner.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}