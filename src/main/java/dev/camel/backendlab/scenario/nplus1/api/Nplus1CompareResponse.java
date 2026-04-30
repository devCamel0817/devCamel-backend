package dev.camel.backendlab.scenario.nplus1.api;

import java.util.List;

public record Nplus1CompareResponse(
    String scenario,
    String title,
    String subtitle,
    Nplus1ResultResponse.RequestSpec request,
    List<Nplus1ResultResponse.VariantOption> variants,
    List<Nplus1ResultResponse> results,
    Summary summary
) {

    public record Summary(
        Winner bestByElapsedMs,
        Winner bestByQueryCount,
        int comparedVariantCount,
        int authorCount,
        int bookCount
    ) {
    }

    public record Winner(
        String value,
        String label,
        String metricKey,
        long metricValue,
        String reason
    ) {
    }
}

