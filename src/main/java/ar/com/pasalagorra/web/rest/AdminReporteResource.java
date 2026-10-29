package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.CambioEstado;
import ar.com.pasalagorra.service.AuditoriaService;
import ar.com.pasalagorra.service.ReporteService;
import ar.com.pasalagorra.service.dto.CambioEstadoDTO;
import ar.com.pasalagorra.service.dto.RecaudacionMensualDTO;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Reportes del administrador: recaudación y comisiones por mes (RF-23) y el historial de cambios de estado (RNF-13).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminReporteResource {

    private final ReporteService reporteService;

    private final AuditoriaService auditoriaService;

    public AdminReporteResource(ReporteService reporteService, AuditoriaService auditoriaService) {
        this.reporteService = reporteService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/reportes/recaudacion")
    public List<RecaudacionMensualDTO> recaudacion() {
        return reporteService.recaudacionMensual();
    }

    /**
     * Los últimos 200 cambios, de todo o de una entidad ({@code ?entidad=CAMPANIA&id=12}).
     */
    @GetMapping("/auditoria")
    public List<CambioEstadoDTO> auditoria(
        @RequestParam(required = false) CambioEstado.Entidad entidad,
        @RequestParam(name = "id", required = false) Long entidadId
    ) {
        return auditoriaService.listar(entidad, entidadId);
    }
}
