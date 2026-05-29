package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.LabelExportService;
import ar.com.rotula.service.LabelExportService.ExportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelExportController.class)
class LabelExportControllerIT {

    @Autowired MockMvc mockMvc;

    @MockBean LabelExportService      labelExportService;
    @MockBean JwtService              jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID LABEL_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    // ── GET /labels/{id}/export ───────────────────────────────────────────────

    @Test
    @WithMockUser
    void export_retorna_200_con_content_type_pdf() throws Exception {
        byte[] fakePdf = fakePdfBytes();
        when(labelExportService.export(eq(LABEL_ID), anyFloat(), anyFloat()))
                .thenReturn(new ExportResult(fakePdf, "Galletita_Cracker_v1.pdf"));

        mockMvc.perform(get("/labels/{id}/export", LABEL_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"Galletita_Cracker_v1.pdf\""));
    }

    @Test
    @WithMockUser
    void export_con_dimensiones_personalizadas_usa_parametros() throws Exception {
        byte[] fakePdf = fakePdfBytes();
        when(labelExportService.export(eq(LABEL_ID), eq(12.0f), eq(8.0f)))
                .thenReturn(new ExportResult(fakePdf, "Producto_v2.pdf"));

        mockMvc.perform(get("/labels/{id}/export", LABEL_ID)
                        .param("widthCm", "12.0")
                        .param("heightCm", "8.0"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"Producto_v2.pdf\""));
    }

    @Test
    @WithMockUser
    void export_rotulo_no_encontrado_retorna_404() throws Exception {
        when(labelExportService.export(any(), anyFloat(), anyFloat()))
                .thenThrow(new IllegalArgumentException("Rótulo no encontrado"));

        mockMvc.perform(get("/labels/{id}/export", LABEL_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void export_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/labels/{id}/export", LABEL_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Crea bytes con la cabecera mágica de PDF (%PDF-) para simular un PDF válido. */
    private byte[] fakePdfBytes() {
        return "%PDF-1.4 fake content for test".getBytes();
    }
}
