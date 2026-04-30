package dev.camel.backendlab.scenario.nplus1.service;

import dev.camel.backendlab.scenario.nplus1.api.Nplus1CompareResponse;
import dev.camel.backendlab.scenario.nplus1.api.Nplus1ResultResponse;
import dev.camel.backendlab.scenario.nplus1.domain.Nplus1Author;
import dev.camel.backendlab.scenario.nplus1.repo.Nplus1AuthorRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Nplus1Service {

    private static final int DEFAULT_AUTHOR_COUNT = 10;
    private static final int MAX_AUTHOR_COUNT = 200;
    private static final int BOOKS_PER_AUTHOR = 20;

    private final Nplus1AuthorRepository authorRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Transactional(readOnly = true)
    public Nplus1ResultResponse run(Nplus1Variant variant, Integer authorCount) {
        List<Long> authorIds = resolveAuthorIds(authorCount);
        Nplus1ResultResponse.RequestSpec request = buildRequestSpec(authorCount, authorIds.size());
        return runInternal(variant, authorIds, request);
    }

    @Transactional(readOnly = true)
    public Nplus1CompareResponse compare(Integer authorCount) {
        List<Long> authorIds = resolveAuthorIds(authorCount);
        Nplus1ResultResponse.RequestSpec request = buildRequestSpec(authorCount, authorIds.size());
        List<Nplus1ResultResponse> results = List.of(
            runInternal(Nplus1Variant.N_PLUS_ONE, authorIds, request),
            runInternal(Nplus1Variant.FETCH_JOIN, authorIds, request),
            runInternal(Nplus1Variant.ENTITY_GRAPH, authorIds, request)
        );

        Nplus1ResultResponse baseline = results.getFirst();

        return new Nplus1CompareResponse(
            "nplus1",
            "N+1 vs Fetch Join vs EntityGraph",
            "같은 데이터셋에서 쿼리 수와 응답 시간을 한 번에 비교합니다.",
            request,
            getVariantOptions(),
            results,
            new Nplus1CompareResponse.Summary(
                createWinner(results, true),
                createWinner(results, false),
                results.size(),
                baseline.metrics().authorCount(),
                baseline.metrics().bookCount()
            )
        );
    }

    private Nplus1ResultResponse runInternal(Nplus1Variant variant, List<Long> authorIds, Nplus1ResultResponse.RequestSpec request) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        long startedAt = System.nanoTime();
        List<Nplus1Author> authors = findAuthors(variant, authorIds);
        int bookCount = authors.stream()
            .mapToInt(author -> author.getBooks().size())
            .sum();
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        long queryCount = statistics.getPrepareStatementCount();

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("authorCount", authors.size());
        extra.put("bookCount", bookCount);
        extra.put("description", variant.summary());
        extra.put("badge", variant.badge());
        extra.put("requestedAuthorCount", request.requestedAuthorCount());
        extra.put("appliedAuthorCount", request.appliedAuthorCount());
        extra.put("booksPerAuthor", request.booksPerAuthor());

        return new Nplus1ResultResponse(
            "nplus1",
            variant.value(),
            variant.label(),
            variant.summary(),
            request,
            elapsedMs,
            queryCount,
            authors.size(),
            new Nplus1ResultResponse.Metrics(
                elapsedMs,
                queryCount,
                authors.size(),
                authors.size(),
                bookCount
            ),
            new Nplus1ResultResponse.Dataset(
                authors.size(),
                bookCount,
                authors.isEmpty() ? 0.0 : (double) bookCount / authors.size(),
                createChartPoints(elapsedMs, queryCount, authors.size(), bookCount)
            ),
            createComparisonHints(variant, queryCount, request),
            createVariantOptions(variant),
            extra
        );
    }

    private List<Nplus1Author> findAuthors(Nplus1Variant variant, List<Long> authorIds) {
        return switch (variant) {
            case N_PLUS_ONE -> authorRepository.findAllByAuthorIds(authorIds);
            case FETCH_JOIN -> authorRepository.findAllWithBooksByFetchJoin(authorIds);
            case ENTITY_GRAPH -> authorRepository.findAllWithBooksByEntityGraph(authorIds);
        };
    }

    private List<Long> resolveAuthorIds(Integer authorCount) {
        int requested = authorCount == null ? DEFAULT_AUTHOR_COUNT : authorCount;
        if (requested <= 0) {
            throw new IllegalArgumentException("authorCount must be greater than 0");
        }
        int limited = Math.min(requested, MAX_AUTHOR_COUNT);
        return authorRepository.findAuthorIds(PageRequest.of(0, limited));
    }

    private Nplus1ResultResponse.RequestSpec buildRequestSpec(Integer requestedAuthorCount, int appliedAuthorCount) {
        return new Nplus1ResultResponse.RequestSpec(
            requestedAuthorCount == null ? DEFAULT_AUTHOR_COUNT : requestedAuthorCount,
            appliedAuthorCount,
            BOOKS_PER_AUTHOR,
            MAX_AUTHOR_COUNT
        );
    }

    public List<Nplus1ResultResponse.VariantOption> getVariantOptions() {
        return createVariantOptions(null);
    }

    private List<Nplus1ResultResponse.VariantOption> createVariantOptions(Nplus1Variant selectedVariant) {
        List<Nplus1ResultResponse.VariantOption> variants = new ArrayList<>();
        for (Nplus1Variant variant : Nplus1Variant.values()) {
            variants.add(new Nplus1ResultResponse.VariantOption(
                variant.value(),
                variant.label(),
                variant.summary(),
                variant.badge(),
                variant.recommendedUse(),
                variant == selectedVariant
            ));
        }
        return variants;
    }

    private List<Nplus1ResultResponse.MetricPoint> createChartPoints(long elapsedMs, long queryCount, int authorCount, int bookCount) {
        return List.of(
            new Nplus1ResultResponse.MetricPoint("elapsedMs", "응답 시간", elapsedMs, "ms"),
            new Nplus1ResultResponse.MetricPoint("queryCount", "쿼리 수", queryCount, "count"),
            new Nplus1ResultResponse.MetricPoint("authorCount", "작성자 수", authorCount, "rows"),
            new Nplus1ResultResponse.MetricPoint("bookCount", "도서 수", bookCount, "rows")
        );
    }

    private List<String> createComparisonHints(Nplus1Variant variant, long queryCount, Nplus1ResultResponse.RequestSpec request) {
        List<String> hints = new ArrayList<>();
        hints.add("같은 데이터셋 기준으로 queryCount와 elapsedMs를 비교하세요.");
        hints.add("이번 요청은 authorCount=" + request.appliedAuthorCount() + " 기준으로 실행되었습니다.");
        hints.add("rows는 author 수이고, 실제 연관 데이터 크기는 metrics.bookCount를 함께 보세요.");

        switch (variant) {
            case N_PLUS_ONE -> hints.add("현재 variant는 lazy loading 때문에 추가 쿼리가 누적됩니다. queryCount가 높게 나와야 정상입니다.");
            case FETCH_JOIN -> hints.add("현재 variant는 fetch join으로 연관 엔티티를 즉시 로딩하므로 queryCount가 1에 가깝게 나와야 정상입니다.");
            case ENTITY_GRAPH -> hints.add("현재 variant는 조회 쿼리는 유지하면서 fetch 전략만 분리할 수 있는 방식입니다.");
        }

        hints.add("이번 실행의 실제 queryCount는 " + queryCount + " 입니다.");
        return hints;
    }

    private Nplus1CompareResponse.Winner createWinner(List<Nplus1ResultResponse> results, boolean byElapsedMs) {
        Nplus1ResultResponse winner = results.stream()
            .min((left, right) -> byElapsedMs
                ? Long.compare(left.elapsedMs(), right.elapsedMs())
                : Long.compare(left.queryCount(), right.queryCount()))
            .orElseThrow();

        long metricValue = byElapsedMs ? winner.elapsedMs() : winner.queryCount();
        String metricKey = byElapsedMs ? "elapsedMs" : "queryCount";
        String reason = byElapsedMs
            ? "가장 빠른 응답 시간을 기록한 variant"
            : "가장 적은 SQL 쿼리를 실행한 variant";

        return new Nplus1CompareResponse.Winner(
            winner.variant(),
            winner.title(),
            metricKey,
            metricValue,
            reason
        );
    }
}

