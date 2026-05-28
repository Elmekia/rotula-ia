package ar.com.rotula.service.allergen;

import ar.com.rotula.domain.AllergenGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AllergenDeclarationServiceTest {

    private final AllergenDeclarationService service = new AllergenDeclarationService();

    // ── Sin alérgenos ────────────────────────────────────────────────────────

    @Test
    void declaration_sin_alergenos_texto_vacio() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Sal", "Agua", "Azúcar"),
                null
        );

        assertThat(r.presentGroups()).isEmpty();
        assertThat(r.crossContaminationGroups()).isEmpty();
        assertThat(r.declarationText()).isEmpty();
    }

    // ── Alérgenos presentes ──────────────────────────────────────────────────

    @Test
    void declaration_detecta_gluten_y_leche() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Harina de trigo", "Leche entera", "Sal", "Azúcar"),
                null
        );

        assertThat(r.presentGroups())
                .contains(AllergenGroup.GLUTEN, AllergenGroup.LECHE);
        assertThat(r.declarationText())
                .startsWith("CONTIENE:")
                .contains("Gluten")
                .contains("Leche")
                .doesNotContain("PUEDE CONTENER TRAZAS DE");
    }

    @Test
    void declaration_detecta_multiples_grupos() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Harina de trigo", "Huevo", "Leche", "Maní", "Sésamo"),
                null
        );

        assertThat(r.presentGroups()).containsExactlyInAnyOrder(
                AllergenGroup.GLUTEN,
                AllergenGroup.HUEVO,
                AllergenGroup.LECHE,
                AllergenGroup.MANI,
                AllergenGroup.SESAMO
        );
        assertThat(r.declarationText()).startsWith("CONTIENE:");
    }

    @Test
    void declaration_no_genera_falsos_positivos_nuez_moscada() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Nuez moscada molida", "Sal"),
                null
        );

        assertThat(r.presentGroups()).isEmpty();
        assertThat(r.declarationText()).isEmpty();
    }

    // ── Contaminación cruzada ────────────────────────────────────────────────

    @Test
    void declaration_solo_contaminacion_cruzada_sin_alergenos_presentes() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Sal", "Agua"),
                "MANI,FRUTOS_DE_CASCARA"
        );

        assertThat(r.presentGroups()).isEmpty();
        assertThat(r.crossContaminationGroups())
                .contains(AllergenGroup.MANI, AllergenGroup.FRUTOS_DE_CASCARA);
        assertThat(r.declarationText())
                .startsWith("PUEDE CONTENER TRAZAS DE:")
                .contains("Maní")
                .contains("Frutos de cáscara")
                .doesNotContain("CONTIENE:");
    }

    @Test
    void declaration_alergenos_presentes_y_contaminacion_cruzada() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Harina de trigo", "Azúcar"),
                "MANI"
        );

        assertThat(r.presentGroups()).contains(AllergenGroup.GLUTEN);
        assertThat(r.crossContaminationGroups()).contains(AllergenGroup.MANI);

        assertThat(r.declarationText())
                .contains("CONTIENE:")
                .contains("PUEDE CONTENER TRAZAS DE:")
                .contains("Gluten")
                .contains("Maní");
    }

    @Test
    void declaration_contaminacion_cruzada_token_invalido_es_ignorado() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Sal"),
                "MANI,GRUPO_INVALIDO,SESAMO"
        );

        assertThat(r.crossContaminationGroups())
                .containsExactlyInAnyOrder(AllergenGroup.MANI, AllergenGroup.SESAMO);
    }

    // ── Texto de declaración ─────────────────────────────────────────────────

    @Test
    void declaration_texto_termina_con_punto() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Leche entera"),
                null
        );

        assertThat(r.declarationText()).endsWith(".");
    }

    @Test
    void declaration_texto_formato_completo() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Harina de trigo", "Huevo"),
                "MANI"
        );

        // Debe contener exactamente "CONTIENE: X, Y. PUEDE CONTENER TRAZAS DE: Z."
        String text = r.declarationText();
        assertThat(text).contains("CONTIENE:");
        assertThat(text).contains("PUEDE CONTENER TRAZAS DE:");
        // El texto de CONTIENE debe aparecer antes de PUEDE CONTENER
        assertThat(text.indexOf("CONTIENE:"))
                .isLessThan(text.indexOf("PUEDE CONTENER TRAZAS DE:"));
    }

    // ── Casos extremos ────────────────────────────────────────────────────────

    @Test
    void declaration_lista_de_ingredientes_vacia() {
        AllergenDeclarationResult r = service.buildDeclaration(List.of(), null);
        assertThat(r.declarationText()).isEmpty();
    }

    @Test
    void declaration_ingrediente_nulo_no_lanza_excepcion() {
        // La API no permite null en la lista, pero conviene que el nombre vacío no rompa
        AllergenDeclarationResult r = service.buildDeclaration(List.of(""), null);
        assertThat(r.presentGroups()).isEmpty();
    }

    @Test
    void declaration_mismo_alergeno_en_varios_ingredientes_no_se_duplica() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Harina de trigo integral", "Sémola de trigo", "Gluten de trigo"),
                null
        );

        // Todos detectan GLUTEN, pero debe aparecer solo una vez
        assertThat(r.presentGroups()).containsOnlyOnce(AllergenGroup.GLUTEN);
    }

    @Test
    void declaration_producto_con_surimi_detecta_pescado() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Surimi", "Almidón de trigo", "Clara de huevo"),
                null
        );

        assertThat(r.presentGroups()).containsExactlyInAnyOrder(
                AllergenGroup.PESCADO, AllergenGroup.GLUTEN, AllergenGroup.HUEVO);
    }

    @Test
    void declaration_soja_lecitina_detecta_soja() {
        AllergenDeclarationResult r = service.buildDeclaration(
                List.of("Lecitina de soja", "Aceite de girasol"),
                null
        );

        assertThat(r.presentGroups()).containsExactly(AllergenGroup.SOJA);
        assertThat(r.declarationText()).contains("Soja");
    }
}
