/*
This class is designed to help you automatically generate email replies
using an external AI service (like Google's Gemini).
*/

package com.email.writer.service;

import com.email.writer.model.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service    // Tells Spring this is a service component, managing business logic.
public class EmailGeneratorService {

    // This is the tool for making web requests to other services (like Gemini AI).
    // Spring will automatically give us an instance of WebClient.
    private final WebClient webClient;

    // Spring will fill these in automatically from our application's settings (application.properties)
    @Value("${gemini.api.url}")   // This will hold the web address of the Gemini AI
    private String geminiApiUrl;

    @Value("${gemini.api.key}")  // This will hold the secret key to access the Gemini AI
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // This is the main function that we will call when we want to generate an email reply
    // It takes an 'EmailRequest' (which contains the original email content and desired tone).
    public String generateEmailReply(EmailRequest emailRequest) {

        //1. Build a prompt
        // Prepares the instructions we'll send to the Gemini AI.
        String prompt = buildPrompt(emailRequest);

        //2. Craft a Request
        // This part prepares the data in a specific format that the Gemini AI expects.
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                            Map.of("text", prompt)
                        })
                }
        );


        //3.Do Request and get the response
        // This is where the code would normally send the 'requestBody' to the 'geminiApiUrl'
        // using the 'geminiApiKey', and then wait for Gemini's answer.
        //we use WebClient to send the request to the Gemini AI.
        String response = webClient.post()
                                   .uri(geminiApiUrl + "?key=" + geminiApiKey) // We combine the base Gemini URL with the API key to form the complete endpoint.
                                   .header("Content-Type", "application/json")  // Tell the AI that we're sending JSON data
                                   .bodyValue(requestBody)
                                   .retrieve()  // Execute the request and prepare to get the response
                                   .bodyToMono(String.class) // Expect the response body back as a single String
                                   .block();  // IMPORTANT: '.block()' means "wait here until the response comes back".

        // 4. Extract response and Return response
        // Once we get the raw response String from the AI, this line calls another helper function
        // to dig into that response and pull out just the generated email text.
        return extractResponseContent(response);

    }

    // This is a helper function that creates the specific instructions (the "prompt") for the AI.
    // It takes the 'emailRequest' (which has the original email content and tone).
    private String buildPrompt(EmailRequest emailRequest) {

        StringBuilder prompt  = new StringBuilder();

        //prompt.append("Please review and improve the following email draft. Give only email body, with no subject. Do not give too many options.");
        prompt.append("Generate a professional email reply body for the following email content. Please don't generate the subject line.");


        // Check if the user specified a tone
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            // If a tone is specified, add it to the instructions for the AI.
            prompt.append("Use a ").append(emailRequest.getTone()).append("tone.");
        }

        // Add a new line for clarity and then include the original email content.
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());

        // Convert the 'StringBuilder' (which is mutable text) into a final, immutable 'String'
        // to be used as the prompt for the AI.
        return prompt.toString();
    }

    // This helper function takes the raw JSON response from the AI and extracts
    // the actual generated email content from it.
    private String extractResponseContent(String response) {

        try {
            // ObjectMapper is like a translator for JSON. It helps us read and write JSON.
            ObjectMapper mapper = new ObjectMapper();

            // Read the entire JSON response string into a 'JsonNode' tree.
            // A JsonNode lets us navigate through the JSON data like a tree, without knowing
            // its exact structure beforehand.
            JsonNode rootNode = mapper.readTree(response);

            // Now, we navigate through the JSON tree to find the generated text.
            return rootNode.path("candidates")   // Go to the "candidates" array
                           .get(0)                        // Get the first item in that array
                           .path("content")     // Go to the "content" object
                           .path("parts")      // Go to the "parts" array
                           .get(0)                     // Get the first item in that array
                           .path("text")     // Go to the "text" field
                           .asText();                // Get its value as a plain string

        } catch(Exception e) {
            return "Error processing request: " + e.getMessage();
        }

    }

}
