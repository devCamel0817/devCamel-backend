package dev.camel.backendlab.scenario.content.client;

import dev.camel.backendlab.scenario.content.GeminiProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;

    public GeminiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(geminiProperties.baseUrl())
                .build();
    }

    @PostConstruct
    void validate() {
        String apiKey = geminiProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되지 않았습니다. IntelliJ가 Gradle로 테스트를 위임 중이면 Gradle 데몬을 재시작하거나 Run tests using을 IntelliJ IDEA로 변경하세요.");
        }
        log.info("Gemini 클라이언트 초기화 완료: model={}, keyPrefix={}...", geminiProperties.model(), apiKey.substring(0, Math.min(8, apiKey.length())));
    }

    public String generateYoutubeScript(List<String> headlines) {
        String apiKey = geminiProperties.apiKey();
        String prompt = buildPrompt(headlines);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.9,
                        "maxOutputTokens", 2048
                )
        );

        GeminiResponse response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", geminiProperties.model(), apiKey)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            log.warn("Gemini 응답 없음");
            return "";
        }

        return response.candidates().get(0).content().parts().get(0).text();
    }

    private String buildPrompt(List<String> headlines) {
        String headlineList = String.join("\n- ", headlines);
        return """
                당신은 경제·재테크 유튜브 채널의 전문 작가입니다.
                아래는 오늘의 뉴스 헤드라인입니다:
                - %s
                
                위 헤드라인 중 가장 시청자 관심이 높을 주제 하나를 선정하고,
                10분 분량의 유튜브 스크립트를 작성해주세요.
                
                형식:
                [제목]
                [후킹 인트로 - 30초]
                [본론 1]
                [본론 2]
                [본론 3]
                [아웃트로 및 CTA]
                """.formatted(headlineList);
    }

    record GeminiResponse(List<Candidate> candidates) {
        record Candidate(Content content) {}
        record Content(List<Part> parts) {}
        record Part(String text) {}
    }
}

