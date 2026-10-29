package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Recompensa;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    /**
     * Bloquea la recompensa para descontar stock sin sobreventa (RNF-08).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recompensa r where r.id = :id")
    Optional<Recompensa> findByIdParaActualizar(@Param("id") Long id);
}
