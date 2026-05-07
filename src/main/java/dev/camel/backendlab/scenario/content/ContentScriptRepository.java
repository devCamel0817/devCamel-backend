package dev.camel.backendlab.scenario.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ContentScriptRepository extends JpaRepository<ContentScript, Long> {
    List<ContentScript> findAllByOrderByTargetDateDesc();
    boolean existsByTargetDate(LocalDate targetDate);
}

