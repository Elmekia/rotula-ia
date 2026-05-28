package ar.com.rotula.service;

import ar.com.rotula.domain.RegulatoryRule;
import ar.com.rotula.dto.RegulatoryRuleResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.RegulatoryRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegulatoryRuleServiceTest {

    @Mock RegulatoryRuleRepository ruleRepository;
    @InjectMocks RegulatoryRuleService ruleService;

    private RegulatoryRule sampleRule(String code, String category) {
        return RegulatoryRule.builder()
                .id(UUID.randomUUID())
                .code(code)
                .category(category)
                .description("Regla de prueba")
                .conditionDsl("{\"category\":[\"" + category + "\"]}")
                .action("DECLARAR_LEYENDA_OBLIGATORIA")
                .actionDetail("Leyenda de prueba")
                .sourceArticle("CAA Art. 1")
                .validFrom(LocalDate.of(2023, 1, 1))
                .version(1)
                .active(true)
                .notes("Notas de prueba")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ── findActive (sin filtro) ──────────────────────────────────────────────

    @Test
    void findActive_sin_categoria_retorna_todas_las_reglas_activas() {
        List<RegulatoryRule> rules = List.of(
                sampleRule("BEB-001", "bebidas"),
                sampleRule("SNK-001", "snacks")
        );
        when(ruleRepository.findByActiveTrueOrderByCategoryAscCodeAsc()).thenReturn(rules);

        List<RegulatoryRuleResponse> result = ruleService.findActive(null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RegulatoryRuleResponse::code)
                .containsExactly("BEB-001", "SNK-001");
    }

    @Test
    void findActive_con_categoria_filtra_correctamente() {
        List<RegulatoryRule> bebidas = List.of(sampleRule("BEB-001", "bebidas"));
        when(ruleRepository.findByCategoryAndActiveTrueOrderByCodeAsc("bebidas"))
                .thenReturn(bebidas);

        List<RegulatoryRuleResponse> result = ruleService.findActive("bebidas");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("BEB-001");
        assertThat(result.get(0).category()).isEqualTo("bebidas");
        verify(ruleRepository, never()).findByActiveTrueOrderByCategoryAscCodeAsc();
    }

    @Test
    void findActive_categoria_vacia_retorna_todas() {
        when(ruleRepository.findByActiveTrueOrderByCategoryAscCodeAsc()).thenReturn(List.of());

        ruleService.findActive("");

        verify(ruleRepository).findByActiveTrueOrderByCategoryAscCodeAsc();
        verify(ruleRepository, never()).findByCategoryAndActiveTrueOrderByCodeAsc(any());
    }

    @Test
    void findActive_categoria_normaliza_a_minusculas() {
        when(ruleRepository.findByCategoryAndActiveTrueOrderByCodeAsc("bebidas")).thenReturn(List.of());

        ruleService.findActive("BEBIDAS");

        verify(ruleRepository).findByCategoryAndActiveTrueOrderByCodeAsc("bebidas");
    }

    // ── findByCode ───────────────────────────────────────────────────────────

    @Test
    void findByCode_retorna_regla_existente() {
        RegulatoryRule rule = sampleRule("BEB-001", "bebidas");
        when(ruleRepository.findByCodeAndActiveTrue("BEB-001")).thenReturn(Optional.of(rule));

        RegulatoryRuleResponse result = ruleService.findByCode("BEB-001");

        assertThat(result.code()).isEqualTo("BEB-001");
        assertThat(result.action()).isEqualTo("DECLARAR_LEYENDA_OBLIGATORIA");
        assertThat(result.conditionDsl()).contains("bebidas");
    }

    @Test
    void findByCode_normaliza_codigo_a_mayusculas() {
        RegulatoryRule rule = sampleRule("BEB-001", "bebidas");
        when(ruleRepository.findByCodeAndActiveTrue("BEB-001")).thenReturn(Optional.of(rule));

        ruleService.findByCode("beb-001");

        verify(ruleRepository).findByCodeAndActiveTrue("BEB-001");
    }

    @Test
    void findByCode_lanza_excepcion_si_no_existe() {
        when(ruleRepository.findByCodeAndActiveTrue("XXX-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.findByCode("XXX-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("XXX-999");
    }

    // ── Mapeo del response ───────────────────────────────────────────────────

    @Test
    void from_mapea_todos_los_campos_del_response() {
        RegulatoryRule rule = sampleRule("SNK-002", "snacks");
        rule.setActionDetail("EXCESO EN GRASAS SATURADAS");
        rule.setValidUntil(LocalDate.of(2025, 12, 31));
        when(ruleRepository.findByCodeAndActiveTrue("SNK-002")).thenReturn(Optional.of(rule));

        RegulatoryRuleResponse r = ruleService.findByCode("SNK-002");

        assertThat(r.code()).isEqualTo("SNK-002");
        assertThat(r.category()).isEqualTo("snacks");
        assertThat(r.actionDetail()).isEqualTo("EXCESO EN GRASAS SATURADAS");
        assertThat(r.validUntil()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(r.version()).isEqualTo(1);
        assertThat(r.sourceArticle()).isEqualTo("CAA Art. 1");
    }
}
