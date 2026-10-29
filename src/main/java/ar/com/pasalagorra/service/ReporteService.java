package ar.com.pasalagorra.service;

import ar.com.pasalagorra.config.ApplicationProperties;
import ar.com.pasalagorra.domain.Aporte;
import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.repository.AporteRepository;
import ar.com.pasalagorra.service.dto.AdminAporteDTO;
import ar.com.pasalagorra.service.dto.RecaudacionMensualDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas del administrador: listado de aportes (RF-21) y recaudación y comisiones por mes (RF-23).
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final AporteRepository aporteRepository;

    private final ApplicationProperties applicationProperties;

    public ReporteService(AporteRepository aporteRepository, ApplicationProperties applicationProperties) {
        this.aporteRepository = aporteRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Los aportes, del más nuevo al más viejo. {@code texto} busca en la referencia, el login del fan y el título de la
     * campaña; los dos filtros son opcionales.
     */
    public Page<AdminAporteDTO> aportes(EstadoAporte estado, String texto, Pageable pageable) {
        Specification<Aporte> filtro = (aporte, consulta, cb) -> cb.conjunction();
        if (estado != null) {
            filtro = filtro.and((aporte, consulta, cb) -> cb.equal(aporte.get("estado"), estado));
        }
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim().toLowerCase() + "%";
            filtro = filtro.and((aporte, consulta, cb) ->
                cb.or(
                    cb.like(cb.lower(aporte.get("referencia")), patron),
                    cb.like(cb.lower(aporte.get("user").get("login")), patron),
                    cb.like(cb.lower(aporte.get("campania").get("titulo")), patron)
                )
            );
        }
        return aporteRepository.findAll(filtro, pageable).map(AdminAporteDTO::de);
    }

    /**
     * Recaudación de las campañas exitosas (o exitosas y ya cerradas) agrupada por el mes de cierre, del más nuevo al más
     * viejo. Las fallidas no suman: sus aportes se reembolsaron. La comisión se calcula con el porcentaje configurado hoy.
     */
    public List<RecaudacionMensualDTO> recaudacionMensual() {
        Map<YearMonth, Acumulado> meses = new TreeMap<>(Comparator.reverseOrder());
        for (AporteRepository.RecaudacionCampania campania : aporteRepository.recaudacionDeCampaniasExitosas()) {
            meses.computeIfAbsent(YearMonth.from(campania.getFechaCierre()), mes -> new Acumulado()).sumar(campania);
        }
        BigDecimal porcentaje = applicationProperties.getComisionPorcentaje();
        return meses
            .entrySet()
            .stream()
            .map(e ->
                new RecaudacionMensualDTO(
                    e.getKey().toString(),
                    e.getValue().campanias,
                    e.getValue().aportes,
                    e.getValue().recaudado,
                    e.getValue().recaudado.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP)
                )
            )
            .toList();
    }

    private static final class Acumulado {

        private long campanias;
        private long aportes;
        private BigDecimal recaudado = BigDecimal.ZERO;

        void sumar(AporteRepository.RecaudacionCampania campania) {
            campanias++;
            aportes += campania.getAportes();
            recaudado = recaudado.add(campania.getRecaudado());
        }
    }
}
