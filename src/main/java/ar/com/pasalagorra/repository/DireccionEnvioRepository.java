package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.DireccionEnvio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DireccionEnvioRepository extends JpaRepository<DireccionEnvio, Long> {
    Optional<DireccionEnvio> findOneByUserLogin(String login);

    List<DireccionEnvio> findAllByUserIdIn(Collection<Long> userIds);
}
