package dev.camel.backendlab.scenario.content.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import dev.camel.backendlab.scenario.content.GeminiProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private Client client;

    public GeminiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    @PostConstruct
    void init() {
        String apiKey = geminiProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        this.client = Client.builder().apiKey(apiKey).build();
        log.info("Gemini 클라이언트 초기화 완료: model={}, keyPrefix={}...",
                geminiProperties.model(), apiKey.substring(0, Math.min(8, apiKey.length())));
    }

    public String generateYoutubeScript(List<String> headlines) {
        List<String> top5 = headlines.stream().limit(5).toList();
        String prompt = buildPrompt(top5);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.9f)
                .maxOutputTokens(2048)
                .build();

        GenerateContentResponse response = client.models.generateContent(
                geminiProperties.model(), prompt, config);

        String text = response.text();
        if (text == null || text.isBlank()) {
            log.warn("Gemini 응답 없음");
            return "";
        }
        return text;
    }

    private String buildPrompt(List<String> headlines) {
        String headlineList = String.join("\n- ", headlines);
        return """
                오늘의 로또번호 뽑아줘
                """;
    }
}
