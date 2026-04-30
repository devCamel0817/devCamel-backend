package dev.camel.backendlab.scenario.nplus1.service;

import dev.camel.backendlab.scenario.nplus1.api.Nplus1ResultResponse;
import dev.camel.backendlab.scenario.nplus1.domain.Nplus1Author;
import dev.camel.backendlab.scenario.nplus1.repo.Nplus1AuthorRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Nplus1ServiceTest {

    private final Nplus1AuthorRepository authorRepository = mock(Nplus1AuthorRepository.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final SessionFactory sessionFactory = mock(SessionFactory.class);
    private final Statistics statistics = mock(Statistics.class);

    private Nplus1Service service;

    @BeforeEach
    void setUp() {
        when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
        when(sessionFactory.getStatistics()).thenReturn(statistics);
        service = new Nplus1Service(authorRepository, entityManagerFactory);
    }

    @Test
    void runReturnsMeasuredQueryCountAndDataset() {
        Nplus1Author authorOne = mock(Nplus1Author.class);
        Nplus1Author authorTwo = mock(Nplus1Author.class);

        when(authorRepository.findAuthorIds(org.springframework.data.domain.PageRequest.of(0, 2)))
            .thenReturn(List.of(1L, 2L));
        when(authorRepository.findAllByAuthorIds(List.of(1L, 2L)))
            .thenReturn(List.of(authorOne, authorTwo));
        when(authorOne.getBooks()).thenReturn(List.of(mock(dev.camel.backendlab.scenario.nplus1.domain.Nplus1Book.class)));
        when(authorTwo.getBooks()).thenReturn(List.of(
            mock(dev.camel.backendlab.scenario.nplus1.domain.Nplus1Book.class),
            mock(dev.camel.backendlab.scenario.nplus1.domain.Nplus1Book.class)
        ));
        when(statistics.getPrepareStatementCount()).thenReturn(7L);

        Nplus1ResultResponse response = service.run(Nplus1Variant.N_PLUS_ONE, 2);

        verify(statistics).clear();
        assertThat(response.queryCount()).isEqualTo(7L);
        assertThat(response.request().requestedAuthorCount()).isEqualTo(2);
        assertThat(response.request().appliedAuthorCount()).isEqualTo(2);
        assertThat(response.metrics().bookCount()).isEqualTo(3);
        assertThat(response.dataset().averageBooksPerAuthor()).isEqualTo(1.5d);
    }

    @Test
    void runCapsRequestedAuthorCountToConfiguredMaximum() {
        Nplus1Author author = mock(Nplus1Author.class);

        when(authorRepository.findAuthorIds(org.springframework.data.domain.PageRequest.of(0, 200)))
            .thenReturn(List.of(1L));
        when(authorRepository.findAllWithBooksByFetchJoin(List.of(1L)))
            .thenReturn(List.of(author));
        when(author.getBooks()).thenReturn(List.of());
        when(statistics.getPrepareStatementCount()).thenReturn(1L);

        Nplus1ResultResponse response = service.run(Nplus1Variant.FETCH_JOIN, 999);

        assertThat(response.request().requestedAuthorCount()).isEqualTo(999);
        assertThat(response.request().appliedAuthorCount()).isEqualTo(1);
        assertThat(response.request().maxAvailableAuthors()).isEqualTo(200);
    }
}


