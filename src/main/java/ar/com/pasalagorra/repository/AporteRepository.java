package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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
public interface AporteRepository extends JpaRepository<Aporte, Long> {
    @EntityGraph(attributePaths = { "campania", "recompensa", "user" })
    Optional<Aporte> findOneByReferencia(String referencia);

    /**
     * Bloquea el aporte: dos notificaciones del mismo pago que llegan a la vez se procesan una después de la otra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aporte a where a.referencia = :referencia")
    Optional<Aporte> findOneByReferenciaParaActualizar(@Param("referencia") String referencia);

    @EntityGraph(attributePaths = { "campania", "recompensa" })
    List<Aporte> findAllByUserLoginAndEstadoNotOrderByFechaCreacionDesc(String login, EstadoAporte estado);

    @EntityGraph(attributePaths = "user")
    List<Aporte> findAllByCampaniaIdAndEstadoIn(Long campaniaId, Collection<EstadoAporte> estados);

    /**
     * Los aportes aprobados de una campaña, para el panel del artista (RF-17).
     */
    @EntityGraph(attributePaths = { "user", "recompensa" })
    List<Aporte> findAllByCampaniaIdAndEstadoOrderByFechaPagoAsc(Long campaniaId, EstadoAporte estado);

    /**
     * Si queda alguna recompensa por entregar (RF-18).
     */
    boolean existsByCampaniaIdAndEstadoAndRecompensaIsNotNullAndEntregadoFalse(Long campaniaId, EstadoAporte estado);

    boolean existsByCampaniaIdAndEstadoIn(Long campaniaId, Collection<EstadoAporte> estados);

    @Query("select coalesce(sum(a.monto), 0) from Aporte a where a.campania.id = :campaniaId and a.estado = 'APROBADO'")
    BigDecimal sumarAprobados(@Param("campaniaId") Long campaniaId);

    @Query("select count(distinct a.user.id) from Aporte a where a.campania.id = :campaniaId and a.estado = 'APROBADO'")
    long contarAportantes(@Param("campaniaId") Long campaniaId);
}
