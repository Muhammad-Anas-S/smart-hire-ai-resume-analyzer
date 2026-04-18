package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.service.GoogleGeminiService; 
//import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; 
import org.apache.tika.Tika; 
import org.apache.tika.exception.TikaException; 
import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity; 
import opennlp.tools.tokenize.Tokenizer; 
import opennlp.tools.tokenize.TokenizerME; 
import opennlp.tools.tokenize.TokenizerModel; 
import opennlp.tools.postag.POSModel; 
import opennlp.tools.postag.POSTaggerME;

import java.io.*; 
import java.util.*;


@CrossOrigin(origins = "*")
@RestController 
@RequestMapping("/resume") 
public class ResumeController {

private final GoogleGeminiService googleGeminiService;


public ResumeController(GoogleGeminiService googleGeminiService) {
    this.googleGeminiService = googleGeminiService;
}

private POSModel loadPOSModel() throws IOException {
    try (InputStream modelIn = getClass().getResourceAsStream("/models/en-pos-maxent.bin")) {
        return new POSModel(modelIn);
    }
}

private String extractTextFromResume(MultipartFile file) throws IOException, TikaException {
    Tika tika = new Tika();
    return tika.parseToString(file.getInputStream());
}

private Tokenizer loadTokenizerModel() throws IOException {
    try (InputStream modelIn = getClass().getResourceAsStream("/models/en-token.bin")) {
        TokenizerModel model = new TokenizerModel(modelIn);
        return new TokenizerME(model);
    }
}

private Set<String> extractSkillsUsingNLP(String text) throws IOException {
    Set<String> skills = new HashSet<>();
    Tokenizer tokenizer = loadTokenizerModel();
    POSModel posModel = loadPOSModel();

    String[] tokens = tokenizer.tokenize(text);
    POSTaggerME tagger = new POSTaggerME(posModel);
    String[] tags = tagger.tag(tokens);

    List<String> itSkills = List.of(
        "Java", "Python", "Spring Boot", "DSA", "MS Office", "MS Excel", "Excel",
        "PostgreSQL", "AWS", "Docker", "React", "Angular", "HTML", "CSS",
        "JavaScript", "Node.js", "MongoDB", "SQL", "MySQL", "NoSQL", "CI/CD",
        "Pandas", "NumPy", "Data Visualization", "Data Preprocessing",
        "Android Studio", "Flutter", "TensorFlow", "PyTorch", "Kubernetes",
        "Linux", "Git", "Jenkins", "C++", "C#", "C", "Hibernate", "SDLC models",
        "Django", "Flask", "Express", "CyberSecurity", "Penetration Testing",
        "Network Security", "Cloud Computing"
    );

    for (int i = 0; i < tokens.length; i++) {
        // Remove punctuation and special characters from tokens
        String token = tokens[i].replaceAll("[^a-zA-Z0-9#+]", "");

        // Debugging - Print token and POS tag
        System.out.println("Token: " + tokens[i] + " -> Tag: " + tags[i]);

        // Strictly Extract "C" Only When Standalone
        if (token.equals("C") && tags[i].startsWith("NN")) { // Ensure "C" is tagged as a noun
            boolean isStandalone = (i == 0 || !Character.isLetterOrDigit(tokens[i - 1].charAt(tokens[i - 1].length() - 1))) &&
                                   (i == tokens.length - 1 || !Character.isLetterOrDigit(tokens[i + 1].charAt(0)));

            if (isStandalone) {
                skills.add("C");
            }
            continue; // Skip further checks for "C"
        }

        // Multi-word skill handling logic
        for (String skill : itSkills) {
            if (!skill.equals("C")) { // Skip "C" since we handled it separately
                String combinedToken = token;

                if (skill.contains(" ")) {
                    // Multi-word skill check (e.g., "Spring Boot")
                    String[] skillWords = skill.split(" ");
                    if (i + skillWords.length <= tokens.length) {
                        combinedToken = String.join(" ", Arrays.copyOfRange(tokens, i, i + skillWords.length));
                    }
                }

                if (combinedToken.equalsIgnoreCase(skill)) {
                    skills.add(skill);
                }
            }

        }
    }

    // Fallback: Use IT domain mapping if no skills found
    if (skills.isEmpty()) {
        Map<String, List<String>> domainSkillMap = getDomainSkillMapping();
        Map<String, List<String>> domainKeywords = getDomainKeywords();

        for (Map.Entry<String, List<String>> entry : domainKeywords.entrySet()) {
            String domain = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (text.toLowerCase().contains(keyword.toLowerCase())) {
                    List<String> relatedSkills = domainSkillMap.get(domain);
                    if (relatedSkills != null) {
                        skills.addAll(relatedSkills);
                    }
                    break;
                }
            }
        }
    }

    System.out.println("Extracted Skills: " + skills);
    return skills;
}

private Map<String, List<String>> getDomainSkillMapping() {
    Map<String, List<String>> domainSkills = new HashMap<>();

    domainSkills.put("Web Development", List.of("HTML", "CSS", "JavaScript"));
    domainSkills.put("Machine Learning", List.of("Pandas", "NumPy", "PyTorch"));
    domainSkills.put("Cloud Computing", List.of("AWS", "Azure", "GCP"));
    domainSkills.put("Cyber Security", List.of("CyberSecurity", "Penetration Testing", "Network Security"));
    domainSkills.put("Data Science", List.of("Data Visualization", "Pandas", "SQL", "Data Preprocessing"));
    domainSkills.put("Software Development", List.of("Java", "Python","C#", "C"));
    domainSkills.put("DevOps", List.of("CI/CD", "Jenkins", "Kubernetes", "Git"));
    domainSkills.put("Database Management", List.of("SQL", "MySQL", "PostgreSQL", "MongoDB"));
    domainSkills.put("Mobile App Development", List.of("Flutter", "Android Studio"));
    domainSkills.put("Big Data", List.of("Hadoop", "Spark"));
    domainSkills.put("AI", List.of("Python", "Chatgpt", "AI integration"));

    return domainSkills;
}

private Map<String, List<String>> getDomainKeywords() {
    Map<String, List<String>> domainKeywords = new HashMap<>();

    domainKeywords.put("Web Development", List.of("web development", "website", "web app", "web application"));
    domainKeywords.put("Machine Learning", List.of("machine learning", "ml","regression", "prediction"));
    domainKeywords.put("Cloud Computing", List.of("cloud computing", "aws", "azure", "cloud deployment"));
    domainKeywords.put("Cyber Security", List.of("cybersecurity", "network security", "ethical hacking", "penetration testing"));
    domainKeywords.put("Data Science", List.of("data science", "data analysis"));
    domainKeywords.put("Software Development", List.of("software development", "application development", "desktop app"));
    domainKeywords.put("DevOps", List.of("devops", "ci/cd", "docker", "jenkins"));
    domainKeywords.put("Database Management", List.of("database"));
    domainKeywords.put("Mobile App Development", List.of("android app", "mobile development", "mobile app"));
    domainKeywords.put("Big Data", List.of("big data", "hadoop", "spark", "large-scale data"));
    domainKeywords.put("AI", List.of("AI", "AI-driven", "AI-Solving"));


    return domainKeywords;
}




private Set<String> extractSoftSkillsUsingNLP(String text) {
    Set<String> softSkills = new HashSet<>();
    String[] itSoftSkills = {
            "Communication", "Teamwork", "Leadership", "Problem-solving","Problem solving",
            "Time Management", "Adaptability", "Critical Thinking", "Creativity"
    };

    try {
        Tokenizer tokenizer = loadTokenizerModel();
        String[] tokens = tokenizer.tokenize(text);

        for (int i = 0; i < tokens.length; i++) {
            for (String skill : itSoftSkills) {
                String combinedToken = tokens[i];
                if (skill.contains(" ")) {
                    String[] skillWords = skill.split(" ");
                    if (i + skillWords.length <= tokens.length) {
                        combinedToken = String.join(" ", Arrays.copyOfRange(tokens, i, i + skillWords.length));
                    }
                }
                if (combinedToken.equalsIgnoreCase(skill) || text.toLowerCase().contains(skill.toLowerCase())) {
                    softSkills.add(skill);
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return softSkills;
}

@PostMapping("/upload")
public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
    try {
        String extractedText = extractTextFromResume(file);

        List<String> skills = new ArrayList<>(extractSkillsUsingNLP(extractedText));
        List<String> softSkills = new ArrayList<>(extractSoftSkillsUsingNLP(extractedText));

        Map<String, List<String>> technicalQuestions = googleGeminiService.generateInterviewQuestions(skills, "technical");
        Map<String, List<String>> softSkillQuestions = googleGeminiService.generateInterviewQuestions(softSkills, "soft");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("Extracted Technical Skills", skills);
        response.put("Extracted Soft Skills", softSkills);
        response.put("Generated Interview Questions for Technical Skills", technicalQuestions);
        response.put("Generated Interview Questions for Soft Skills", softSkillQuestions);

        return ResponseEntity.ok(response);
    }
    catch (Exception e)
    {
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
    }
}

}
