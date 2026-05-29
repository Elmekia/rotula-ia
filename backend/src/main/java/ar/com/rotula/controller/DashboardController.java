package ar.com.rotula.controller;

import ar.com.rotula.dto.DashboardSummary;
import ar.com.rotula.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proporciona el resumen del dashboard para el tenant del usuario autenticado.
 *
 * <pre>
 *   GET /dashboard/summary
 * </pre>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Devuelve los contadores agregados del tenant y los últimos 5 rótulos
     * generados, listos para mostrar en el dashboard de la aplicación.
     */
    @GetMapping("/summary")
    public DashboardSummary getSummary() {
        return dashboardService.getSummary();
    }
}
