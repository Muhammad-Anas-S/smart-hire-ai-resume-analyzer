package com.example.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode; 
import com.fasterxml.jackson.databind.ObjectMapper; 
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service; 
import org.springframework.web.client.RestTemplate; 
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service 
public class GoogleGeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";

    public Map<String, List<String>> generateInterviewQuestions(List<String> skills, String category) {
        Map<String, List<String>> questionsMap = new LinkedHashMap<>();

        for (String skill : skills) {
            String variationFactor = getRandomVariation(); // Adds randomness

            String prompt;
            if (category.equalsIgnoreCase("soft skills")) {
                prompt = "Generate exactly three interview questions for the soft skill '" + skill + "'. " +
                         "Assume the candidate is a beginner with basic understanding.\n" +
                         "Ensure:\n1. One question is scenario-based.\n" +
                         "2. Two questions focus on behavior, thought process, or general application.\n" +
                         "3. Keep them clear, practical, and straightforward.\n" +
                         variationFactor;
            } else {
                prompt = "Generate exactly three direct and straightforward interview questions for the technical skill '" + skill + "'. " +
                         "Assume the candidate is a beginner.\n" +
                         "Ensure:\n1. All three questions focus on core concepts, definitions, or practical application.\n" +
                         "2. Avoid scenario-based questions completely.\n" +
                         "3. Keep them precise, fact-based, and directly related to the skill.\n" +
                         variationFactor;
            }

            String response = callGeminiApi(prompt);
            List<String> extractedQuestions = extractQuestionsFromResponse(response);
            questionsMap.put(skill, extractedQuestions);
        }

        return questionsMap;
    }


    // New helper method to introduce slight variations in each API request
    private String getRandomVariation() {
        String[] variations = {
            "Make the questions slightly unique from common interview questions.",
            "Ensure the questions are varied and not repetitive.",
            "Introduce slight unpredictability in question phrasing.",
            "Phrase the questions in a different way than usual interview patterns."
        };
        return variations[new Random().nextInt(variations.length)];
    }


    

    private String callGeminiApi(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();
            
            String requestJson = "{" +
                    "\"contents\": [{\"parts\":[{\"text\": \"" + prompt + "\"}]}]" +
                    "}";

            String response = restTemplate.postForObject(GEMINI_API_URL + "?key=" + apiKey, requestJson, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response);

            // Handle quota exceeded error
            if (jsonResponse.has("error")) {
                JsonNode error = jsonResponse.get("error");
                int code = error.has("code") ? error.get("code").asInt() : 0;
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";

                if (message.toLowerCase().contains("quota")) {
                    return "Quota exceeded: " + message + ". Please check your Gemini API usage or upgrade your quota.";
                } else {
                    return "Gemini API error (" + code + "): " + message;
                }
            }

            return jsonResponse.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } 
        catch (Exception e)
        {
        	return "Error generating questions: " + e.getMessage();
        }
    }


    private List<String> extractQuestionsFromResponse(String response) {
        List<String> questions = new ArrayList<>();
        
        if (response != null && !response.isEmpty()) {
            String[] splitQuestions = response.split("\n");

            for (String question : splitQuestions) {
                if (!question.trim().isEmpty() && !question.contains("No additional question available.")) {
                    questions.add(question.trim());
                }
            }
        }

        // Ensure we return exactly 3 questions per skill
        while (questions.size() < 3) {
            questions.add("Fallback question: Can you explain more about this skill?");
        }
		return questions.size() > 3 ? questions.subList(0,3):questions;

        
    }

}
