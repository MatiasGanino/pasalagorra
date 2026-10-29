package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoCampania;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaniaRepository extends JpaRepository<Campania, Long> {
    @EntityGraph(attributePaths = "artista")
    List<Campania> findAllByArtistaUserLoginOrderByFechaCreacionDesc(String login);

    @EntityGraph(attributePaths = "artista")
    List<Campania> findAllByEstadoInOrderByFechaCierreAsc(Collection<EstadoCampania> estados);

    @EntityGraph(attributePaths = "artista")
    Optional<Campania> findOneBySlug(String slug);

    boolean existsBySlug(String slug);
}
