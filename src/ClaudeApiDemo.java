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
                
                System.out.print("Do you want to include an image? (yes/no): ");
                String imageChoice = scanner.nextLine();
                
                String response;
                if (imageChoice.equalsIgnoreCase("yes")) {
                    System.out.print("Enter the image path or URL: ");
                    String imagePath = scanner.nextLine();
                    response = claudeApi.sendMessageWithImage(userInput, imagePath);
                } else {
                    response = claudeApi.sendMessage(userInput);
                }
                
                System.out.println("Claude's response:");
                System.out.println(response);
            }
            scanner.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}