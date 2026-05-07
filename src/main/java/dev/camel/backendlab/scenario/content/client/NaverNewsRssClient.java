package dev.camel.backendlab.scenario.content.client;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import dev.camel.backendlab.scenario.content.ContentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsRssClient {

    private final ContentProperties contentProperties;

    public List<String> fetchEconomyHeadlines() {
        List<String> headlines = new ArrayList<>();
        SyndFeedInput input = new SyndFeedInput();
        int maxItems = contentProperties.news().maxItems();

        for (String rssUrl : contentProperties.news().rssUrls()) {
            try {
                URLConnection connection = new URL(rssUrl).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(5_000);

                SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));
                for (SyndEntry entry : feed.getEntries()) {
                    if (headlines.size() >= maxItems) break;
                    headlines.add(entry.getTitle());
                }
            } catch (Exception e) {
                log.warn("RSS 수집 실패: url={}, message={}", rssUrl, e.getMessage());
            }
            if (headlines.size() >= maxItems) break;
        }

        log.info("RSS 뉴스 수집 완료: {}건", headlines.size());
        return headlines;
    }
}
