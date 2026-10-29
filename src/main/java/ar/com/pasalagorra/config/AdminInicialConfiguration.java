package ar.com.pasalagorra.config;

import ar.com.pasalagorra.service.AdminInicialService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, crea el primer administrador si todavía no hay ninguno.
 */
@Component
public class AdminInicialConfiguration implements ApplicationRunner {

    private final AdminInicialService adminInicialService;

    public AdminInicialConfiguration(AdminInicialService adminInicialService) {
        this.adminInicialService = adminInicialService;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminInicialService.crearSiFalta();
    }
}
