package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Novedad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NovedadRepository extends JpaRepository<Novedad, Long> {
    List<Novedad> findAllByCampaniaIdOrderByFechaDesc(Long campaniaId);
}
