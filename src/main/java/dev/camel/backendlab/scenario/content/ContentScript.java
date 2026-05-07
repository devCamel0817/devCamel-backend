package dev.camel.backendlab.scenario.content;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_script")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(columnDefinition = "TEXT")
    private String headlines;

    @Column(columnDefinition = "TEXT")
    private String script;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ContentScript of(LocalDate targetDate, String headlines, String script) {
        ContentScript entity = new ContentScript();
        entity.targetDate = targetDate;
        entity.headlines = headlines;
        entity.script = script;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}

