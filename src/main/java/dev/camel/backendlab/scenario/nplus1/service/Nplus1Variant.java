package dev.camel.backendlab.scenario.nplus1.service;

import java.util.Arrays;

public enum Nplus1Variant {
    N_PLUS_ONE("n-plus-one", "N+1", "lazy loading으로 인한 추가 쿼리 발생", "warning", "문제 재현용"),
    FETCH_JOIN("fetch-join", "Fetch Join", "JPQL fetch join으로 연관 엔티티를 한 번에 조회", "success", "가장 직관적인 최적화"),
    ENTITY_GRAPH("entity-graph", "EntityGraph", "조회 시점에 fetch plan을 분리해서 적용", "info", "쿼리와 fetch 전략 분리" );

    private final String value;
    private final String label;
    private final String summary;
    private final String badge;
    private final String recommendedUse;

    Nplus1Variant(String value, String label, String summary, String badge, String recommendedUse) {
        this.value = value;
        this.label = label;
        this.summary = summary;
        this.badge = badge;
        this.recommendedUse = recommendedUse;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String summary() {
        return summary;
    }

    public String badge() {
        return badge;
    }

    public String recommendedUse() {
        return recommendedUse;
    }

    public static Nplus1Variant from(String value) {
        return Arrays.stream(values())
            .filter(variant -> variant.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported nplus1 variant: " + value));
    }
}

