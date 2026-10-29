package ar.com.pasalagorra.repository;

import ar.com.pasalagorra.domain.Artista;
import ar.com.pasalagorra.domain.EstadoArtista;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistaRepository extends JpaRepository<Artista, Long> {
    Optional<Artista> findOneByUserLogin(String login);

    @EntityGraph(attributePaths = "user")
    List<Artista> findAllByEstadoOrderByFechaSolicitudAsc(EstadoArtista estado);

    boolean existsByDniAndUserLoginNot(String dni, String login);

    boolean existsByCuitAndUserLoginNot(String cuit, String login);
}
