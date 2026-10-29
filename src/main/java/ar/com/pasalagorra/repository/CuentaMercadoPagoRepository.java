package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.CuentaMercadoPago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuentaMercadoPagoRepository extends JpaRepository<CuentaMercadoPago, Long> {
    Optional<CuentaMercadoPago> findOneByArtistaId(Long artistaId);

    Optional<CuentaMercadoPago> findOneByArtistaUserLogin(String login);

    Optional<CuentaMercadoPago> findFirstByMpUserId(Long mpUserId);
}
