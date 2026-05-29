package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.LegalNameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegalNameController.class)
class LegalNameControllerIT {

    @Autowired MockMvc mockMvc;

    @MockBean LegalNameService       legalNameService;
    @MockBean JwtService             jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID PRODUCT_ID = UUID.fromString("99990000-0000-0000-0000-000000000001");

    @Test
    @WithMockUser
    void suggest_retorna_200_con_nombre_legal() throws Exception {
        when(legalNameService.suggest(PRODUCT_ID)).thenReturn(
                new LegalNameSuggestion("Leche entera pasteurizada",
                        List.of("Bajo en sodio"), false, "CAA Art. 559"));

        mockMvc.perform(get("/products/{id}/legal-name", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedName").value("Leche entera pasteurizada"))
                .andExpect(jsonPath("$.modifiers[0]").value("Bajo en sodio"))
                .andExpect(jsonPath("$.alertDiffers").value(false))
                .andExpect(jsonPath("$.sourceArticle").value("CAA Art. 559"));
    }

    @Test
    @WithMockUser
    void suggest_con_alerta_retorna_alertDiffers_true() throws Exception {
        when(legalNameService.suggest(PRODUCT_ID)).thenReturn(
                new LegalNameSuggestion("Yogur", List.of(), true, "CAA Art. 576 bis"));

        mockMvc.perform(get("/products/{id}/legal-name", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertDiffers").value(true));
    }

    @Test
    @WithMockUser
    void suggest_categoria_no_encontrada_retorna_null_en_nombre() throws Exception {
        when(legalNameService.suggest(any())).thenReturn(
                new LegalNameSuggestion(null, List.of(), false, null));

        mockMvc.perform(get("/products/{id}/legal-name", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedName").doesNotExist());
    }

    @Test
    @WithMockUser
    void suggest_producto_no_encontrado_retorna_404() throws Exception {
        when(legalNameService.suggest(any()))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID));

        mockMvc.perform(get("/products/{id}/legal-name", PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void suggest_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/products/{id}/legal-name", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }
}
