package dev.camel.backendlab.scenario.nplus1.api;

import java.util.List;
import java.util.Map;

public record Nplus1ResultResponse(
    String scenario,
    String variant,
    String title,
    String subtitle,
    RequestSpec request,
    long elapsedMs,
    long queryCount,
    int rows,
    Metrics metrics,
    Dataset dataset,
    List<String> comparisonHints,
    List<VariantOption> variants,
    Map<String, Object> extra
) {

    public record RequestSpec(
        int requestedAuthorCount,
        int appliedAuthorCount,
        int booksPerAuthor,
        int maxAvailableAuthors
    ) {
    }

    public record Metrics(
        long elapsedMs,
        long queryCount,
        int rowCount,
        int authorCount,
        int bookCount
    ) {
    }

    public record Dataset(
        int authorCount,
        int bookCount,
        double averageBooksPerAuthor,
        List<MetricPoint> chart
    ) {
    }

    public record MetricPoint(
        String key,
        String label,
        long value,
        String unit
    ) {
    }

    public record VariantOption(
        String value,
        String label,
        String summary,
        String badge,
        String recommendedUse,
        boolean selected
    ) {
    }
}

