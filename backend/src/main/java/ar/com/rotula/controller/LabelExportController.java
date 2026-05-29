package ar.com.rotula.controller;

import ar.com.rotula.service.LabelExportService;
import ar.com.rotula.service.LabelExportService.ExportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Exporta un rótulo como archivo PDF descargable.
 *
 * <pre>
 *   GET /labels/{id}/export?widthCm=10&amp;heightCm=15
 * </pre>
 *
 * <p>El tamaño de página es configurable en centímetros; los valores por defecto
 * (10 × 15 cm) son comunes para productos de góndola argentinos.
 * El PDF generado es vectorial, lo que satisface el requisito de 300 dpi.
 */
@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelExportController {

    private final LabelExportService labelExportService;

    /**
     * Genera y devuelve el PDF del rótulo identificado por {@code id}.
     *
     * @param id       ID del {@code label_projects} a exportar.
     * @param widthCm  Ancho de la etiqueta en centímetros (default 10).
     * @param heightCm Alto de la etiqueta en centímetros (default 15).
     * @return Respuesta con el PDF como body binario y la cabecera
     *         {@code Content-Disposition: attachment; filename="..."}.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") float widthCm,
            @RequestParam(defaultValue = "15") float heightCm) {

        ExportResult result = labelExportService.export(id, widthCm, heightCm);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .body(result.pdfBytes());
    }
}
