package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Reclamo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    @EntityGraph(attributePaths = { "aporte", "aporte.user", "aporte.campania", "aporte.campania.artista", "aporte.recompensa" })
    List<Reclamo> findAllByEstadoOrderByFechaAsc(Reclamo.Estado estado);

    @EntityGraph(attributePaths = { "aporte", "aporte.user", "aporte.campania" })
    List<Reclamo> findAllByAporteUserLoginAndEstado(String login, Reclamo.Estado estado);

    boolean existsByAporteIdAndTipoAndEstado(Long aporteId, Reclamo.Tipo tipo, Reclamo.Estado estado);
}
