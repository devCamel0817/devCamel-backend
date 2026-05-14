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
                당신은 세계 최고회사 MARKET SIGNAL의 경제·주식 블로그의 전문 작가입니다.
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
}
