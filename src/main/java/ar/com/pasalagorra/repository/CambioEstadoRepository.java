package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.CambioEstado;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {
    List<CambioEstado> findAllByOrderByFechaDescIdDesc(Pageable pageable);

    List<CambioEstado> findAllByEntidadOrderByFechaDescIdDesc(CambioEstado.Entidad entidad, Pageable pageable);

    List<CambioEstado> findAllByEntidadAndEntidadIdOrderByFechaDescIdDesc(CambioEstado.Entidad entidad, Long entidadId, Pageable pageable);
}
