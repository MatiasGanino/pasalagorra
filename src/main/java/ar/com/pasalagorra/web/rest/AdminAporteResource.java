package ar.com.pasalagorra.web.rest;

import ar.com.pasalagorra.domain.EstadoAporte;
import ar.com.pasalagorra.service.ReporteService;
import ar.com.pasalagorra.service.dto.AdminAporteDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Todos los aportes, para el administrador (RF-21), paginados con {@code X-Total-Count}. {@code /api/admin/**} exige
 * ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/aportes")
public class AdminAporteResource {

    private final ReporteService reporteService;

    public AdminAporteResource(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public ResponseEntity<List<AdminAporteDTO>> listar(
        @RequestParam(required = false) EstadoAporte estado,
        @RequestParam(required = false) String texto,
        @PageableDefault(size = 50, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminAporteDTO> pagina = reporteService.aportes(estado, texto, pageable);
        return ResponseEntity.ok()
            .headers(PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), pagina))
            .body(pagina.getContent());
    }
}
