package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Campania;
import ar.com.pasalagorra.domain.EstadoCampania;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<Campania> findOneByIdAndArtistaUserLogin(Long id, String login);

    List<Campania> findAllByEstadoAndFechaCierreBefore(EstadoCampania estado, LocalDate fecha);

    List<Campania> findAllByEstado(EstadoCampania estado);

    /**
     * Bloquea la campaña para cerrarla una sola vez aunque la tarea corra dos veces a la vez (RNF-07).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campania c where c.id = :id")
    Optional<Campania> findByIdParaActualizar(@Param("id") Long id);
}
