package dev.camel.backendlab.scenario.nplus1.repo;

import dev.camel.backendlab.scenario.nplus1.domain.Nplus1Author;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Nplus1AuthorRepository extends JpaRepository<Nplus1Author, Long> {

    @Query("select a.id from Nplus1Author a order by a.id")
    List<Long> findAuthorIds(Pageable pageable);

    @Query("select a from Nplus1Author a where a.id in :authorIds order by a.id")
    List<Nplus1Author> findAllByAuthorIds(@Param("authorIds") List<Long> authorIds);

    @Query("select distinct a from Nplus1Author a left join fetch a.books where a.id in :authorIds")
    List<Nplus1Author> findAllWithBooksByFetchJoin(@Param("authorIds") List<Long> authorIds);

    @EntityGraph(value = "Nplus1Author.books")
    @Query("select a from Nplus1Author a where a.id in :authorIds order by a.id")
    List<Nplus1Author> findAllWithBooksByEntityGraph(@Param("authorIds") List<Long> authorIds);
}

