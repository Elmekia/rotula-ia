package ar.com.rotula.service;

import ar.com.rotula.domain.AuditLog;
import ar.com.rotula.domain.LabelProject;
import ar.com.rotula.dto.label.IngredientItem;
import ar.com.rotula.dto.label.LabelDTO;
import ar.com.rotula.repository.AuditLogRepository;
import ar.com.rotula.repository.LabelProjectRepository;
import ar.com.rotula.security.AppUserDetails;
import ar.com.rotula.service.nutrition.RoundedNutritionValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Genera la exportación PDF de un rótulo y registra la acción en el log de auditoría.
 *
 * <p>El PDF incluye todos los elementos normativos del rótulo:
 * denominación legal, ingredientes, tabla nutricional, sellos de advertencia,
 * alérgenos y claims.  El tamaño de página es configurable en centímetros;
 * el formato vectorial de PDF satisface el requisito de 300 dpi.
 */
@Service
@RequiredArgsConstructor
public class LabelExportService {

    /** Factor de conversión: 1 cm = 28.35 puntos tipográficos. */
    private static final float CM_TO_PT = 28.35f;

    private final LabelProjectRepository labelProjectRepository;
    private final AuditLogRepository     auditLogRepository;
    private final ObjectMapper           objectMapper;

    /**
     * Exporta el rótulo identificado por {@code labelId} como PDF con las
     * dimensiones físicas indicadas y registra la acción en {@code audit_log}.
     *
     * @param labelId   ID del {@code label_projects} a exportar.
     * @param widthCm   Ancho del PDF en centímetros.
     * @param heightCm  Alto del PDF en centímetros.
     * @return {@link ExportResult} con los bytes del PDF y el nombre de archivo sugerido.
     * @throws jakarta.ws.rs.NotFoundException si el rótulo no existe o no pertenece al tenant.
     * @throws IllegalStateException si el rótulo no tiene datos generados ({@code label_data} nulo).
     */
    @Transactional
    public ExportResult export(UUID labelId, float widthCm, float heightCm) {
        AppUserDetails user     = currentUser();
        UUID           tenantId = user.getTenantId();

        LabelProject project = labelProjectRepository.findById(labelId)
                .filter(lp -> lp.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Rótulo no encontrado: " + labelId));

        if (project.getLabelData() == null || project.getLabelData().isBlank()) {
            throw new IllegalStateException("El rótulo " + labelId + " no tiene datos generados.");
        }

        LabelDTO dto = deserialize(project.getLabelData());
        byte[]   pdf = generatePdf(dto, widthCm, heightCm);

        saveAuditLog(tenantId, user.getId(), labelId, dto.productName());

        String filename = buildFilename(dto.productName(), project.getVersion());
        return new ExportResult(pdf, filename);
    }

    // ── PDF generation ────────────────────────────────────────────────────────

    private byte[] generatePdf(LabelDTO dto, float widthCm, float heightCm) {
        float pageW = widthCm  * CM_TO_PT;
        float pageH = heightCm * CM_TO_PT;

        Rectangle pageSize = new Rectangle(pageW, pageH);
        Document  doc      = new Document(pageSize, 14f, 14f, 16f, 16f);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  "Cp1252", 11f);
            Font bodyFont     = FontFactory.getFont(FontFactory.HELVETICA,        "Cp1252",  8f);
            Font smallFont    = FontFactory.getFont(FontFactory.HELVETICA,        "Cp1252",  7f);
            Font headingFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  "Cp1252",  8f);
            Font sealFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  "Cp1252",  6f);

            // ── Denominación legal ────────────────────────────────────────────
            String denomination = dto.legalDenomination() != null
                    ? dto.legalDenomination()
                    : dto.productName();

            Paragraph denPara = new Paragraph(denomination.toUpperCase(), titleFont);
            denPara.setAlignment(Element.ALIGN_CENTER);
            denPara.setSpacingAfter(6f);
            doc.add(denPara);

            // ── Sellos de advertencia ─────────────────────────────────────────
            if (dto.seals() != null && !dto.seals().isEmpty()) {
                addSeals(doc, dto.seals(), sealFont);
            }

            // ── Peso neto ─────────────────────────────────────────────────────
            if (dto.netWeight() != null) {
                String netW = "Contenido neto: " + dto.netWeight().toPlainString()
                        + " " + (dto.weightUnit() != null ? dto.weightUnit() : "");
                Paragraph netPara = new Paragraph(netW, bodyFont);
                netPara.setAlignment(Element.ALIGN_CENTER);
                netPara.setSpacingAfter(6f);
                doc.add(netPara);
            }

            // ── Ingredientes ──────────────────────────────────────────────────
            if (dto.ingredients() != null && !dto.ingredients().isEmpty()) {
                doc.add(new Paragraph("INGREDIENTES:", headingFont));
                String ingList = buildIngredientList(dto.ingredients());
                Paragraph ingPara = new Paragraph(ingList, bodyFont);
                ingPara.setSpacingAfter(6f);
                doc.add(ingPara);
            }

            // ── Tabla nutricional ─────────────────────────────────────────────
            if (dto.nutrition() != null) {
                doc.add(new Paragraph("INFORMACIÓN NUTRICIONAL", headingFont));
                doc.add(buildNutritionTable(dto, bodyFont, headingFont));
                doc.add(Chunk.NEWLINE);
            }

            // ── Alérgenos ─────────────────────────────────────────────────────
            if (dto.allergens() != null) {
                String allergenText = dto.allergens().declarationText();
                if (allergenText != null && !allergenText.isBlank()) {
                    Paragraph allerPara = new Paragraph(allergenText, smallFont);
                    allerPara.setSpacingBefore(4f);
                    allerPara.setSpacingAfter(4f);
                    doc.add(allerPara);
                }
            }

            // ── Claims nutricionales ──────────────────────────────────────────
            if (dto.claims() != null && !dto.claims().isEmpty()) {
                Paragraph claimPara = new Paragraph(String.join(" · ", dto.claims()), smallFont);
                claimPara.setSpacingAfter(6f);
                doc.add(claimPara);
            }

            // ── RNE / RNPA ────────────────────────────────────────────────────
            addRegistrations(doc, dto, smallFont);

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF del rótulo", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    /** Dibuja los sellos de advertencia como rectángulos negros con texto blanco. */
    private void addSeals(Document doc, java.util.Set<String> seals, Font sealFont)
            throws DocumentException {

        PdfPTable sealTable = new PdfPTable(seals.size());
        sealTable.setWidthPercentage(100f);
        sealTable.setSpacingAfter(6f);

        for (String seal : seals) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(Color.BLACK);
            cell.setBorderColor(Color.BLACK);
            cell.setPadding(4f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Font whiteFont = new Font(sealFont);
            whiteFont.setColor(Color.WHITE);
            Phrase phrase = new Phrase(seal.toUpperCase().replace("_", " "), whiteFont);
            cell.setPhrase(phrase);
            sealTable.addCell(cell);
        }
        doc.add(sealTable);
    }

    /** Construye la tabla de información nutricional normalizada. */
    private PdfPTable buildNutritionTable(LabelDTO dto, Font bodyFont, Font headingFont)
            throws DocumentException {

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{3f, 2f, 2f});
        table.setSpacingBefore(4f);
        table.setSpacingAfter(4f);

        // Encabezado
        addNutritionHeader(table, headingFont,
                "Nutriente",
                "Por porción (" + dto.nutrition().servingSizeG().toPlainString() + " g)",
                "Por 100 g");

        RoundedNutritionValues por  = dto.nutrition().perPortion();
        RoundedNutritionValues c100 = dto.nutrition().per100g();

        addNutritionRow(table, bodyFont, "Valor energético",
                por.energyKcal() + " kcal / " + por.energyKj() + " kJ",
                c100.energyKcal() + " kcal / " + c100.energyKj() + " kJ");
        addNutritionRow(table, bodyFont, "Proteínas",        por.proteinsG() + " g",  c100.proteinsG() + " g");
        addNutritionRow(table, bodyFont, "Carbohidratos",    por.carbsG() + " g",     c100.carbsG() + " g");
        addNutritionRow(table, bodyFont, "  - Azúcares",     por.sugarsG() + " g",    c100.sugarsG() + " g");
        addNutritionRow(table, bodyFont, "Grasas totales",   por.fatTotalG() + " g",  c100.fatTotalG() + " g");
        addNutritionRow(table, bodyFont, "  - Grasas saturadas", por.fatSatG() + " g", c100.fatSatG() + " g");
        addNutritionRow(table, bodyFont, "  - Grasas trans",
                formatTrans(por.fatTransG()), formatTrans(c100.fatTransG()));
        addNutritionRow(table, bodyFont, "Sodio", por.sodiumMg() + " mg", c100.sodiumMg() + " mg");

        return table;
    }

    private void addNutritionHeader(PdfPTable table, Font font, String... headers)
            throws DocumentException {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(3f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addNutritionRow(PdfPTable table, Font font, String nutrient, String portion, String per100)
            throws DocumentException {
        for (String val : new String[]{nutrient, portion, per100}) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setPadding(2f);
            table.addCell(cell);
        }
    }

    private String formatTrans(double v) {
        return (v == 0.0) ? "0 g" : String.format("%.1f g", v);
    }

    /** Convierte la lista de ingredientes a texto con porcentaje entre paréntesis. */
    private String buildIngredientList(List<IngredientItem> ingredients) {
        return ingredients.stream()
                .map(i -> {
                    StringBuilder sb = new StringBuilder(i.name());
                    if (i.percentage() != null) {
                        sb.append(" (").append(i.percentage().toPlainString()).append("%)");
                    }
                    if (i.allergen()) sb.append("*");
                    return sb.toString();
                })
                .collect(Collectors.joining(", "));
    }

    /** Añade la línea de registros RNE / RNPA al final del documento. */
    private void addRegistrations(Document doc, LabelDTO dto, Font font) throws DocumentException {
        StringBuilder reg = new StringBuilder();
        if (dto.rneNumber() != null && !dto.rneNumber().isBlank()) {
            reg.append("RNE: ").append(dto.rneNumber());
        }
        if (dto.rnpaNumber() != null && !dto.rnpaNumber().isBlank()) {
            if (!reg.isEmpty()) reg.append("  |  ");
            reg.append("RNPA: ").append(dto.rnpaNumber());
        }
        if (!reg.isEmpty()) {
            Paragraph regPara = new Paragraph(reg.toString(), font);
            regPara.setAlignment(Element.ALIGN_RIGHT);
            regPara.setSpacingBefore(6f);
            doc.add(regPara);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelDTO deserialize(String json) {
        try {
            return objectMapper.readValue(json, LabelDTO.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error al deserializar label_data", e);
        }
    }

    /** Nombre de archivo: sanitiza el nombre del producto y añade _v{version}.pdf */
    static String buildFilename(String productName, int version) {
        String sanitized = productName
                .replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ ]", "")
                .trim()
                .replaceAll("\\s+", "_");
        return sanitized + "_v" + version + ".pdf";
    }

    private void saveAuditLog(UUID tenantId, UUID userId, UUID labelId, String productName) {
        String details = "{\"productName\":\"" + productName.replace("\"", "\\\"") + "\"}";
        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .entityType("LABEL")
                .entityId(labelId)
                .action("EXPORT")
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Encapsula el resultado de la exportación PDF.
     *
     * @param pdfBytes Bytes del archivo PDF generado.
     * @param filename Nombre de archivo sugerido para la descarga.
     */
    public record ExportResult(byte[] pdfBytes, String filename) {}
}
