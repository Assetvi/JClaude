# Anthropic API Client for Java
This is a Java library for accessing the Anthropic Claude API. It supports sending text messages and images to the Claude AI models, making it easy to integrate advanced AI capabilities into your Java applications.
## Features
- Send text messages to the Claude API
- Send images (JPEG, PNG, GIF, WebP) to the Claude API
- Handles various response statuses and errors from the API
- Supports multiple Claude AI models
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
## Installation
Did your project has gson install? if not, download the [lib/gson-2.11.0.jar](lib/gson-2.11.0.jar)</br>
Then, just download [src/AnthropicApiClient.java](src/AnthropicApiClient.java)
## Usage
First, create an instance of the **AnthropicApiClient**:
```
String apiKey = "your-api-key";
int maxTokens = 100;
String model = "claude-3-5-sonnet-20240620";
double temperature = 0.7;

AnthropicApiClient client = new AnthropicApiClient(apiKey, maxTokens, model, temperature);
```
### Sending a Text Message
```
String message = "Hello, Claude!";
String response = client.sendMessage(message);
System.out.println(response);
```
