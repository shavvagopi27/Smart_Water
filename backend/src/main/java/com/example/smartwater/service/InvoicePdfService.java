package com.example.smartwater.service;

import com.example.smartwater.entity.Invoice;
import com.example.smartwater.entity.InvoiceItem;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Renders a one-page A4 invoice with Apache PDFBox. (Standard fonts cannot print the rupee sign, so "Rs." is used.) */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {
    private final InvoiceRepository invoiceRepository;

    @Value("${app.billing.payment-instructions}")
    private String paymentInstructions;

    private static final float LEFT = 50, RIGHT = 545;

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return render(inv);
    }

    /** Call inside a transaction: lazy associations of the invoice are read. */
    public byte[] render(Invoice inv) {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // ---- header band
                cs.setNonStrokingColor(0.05f, 0.35f, 0.62f);
                cs.addRect(0, 752, 595, 90);
                cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                text(cs, bold, 24, LEFT, 800, "WATER INVOICE");
                text(cs, regular, 11, LEFT, 778, inv.getHousehold().getApartment().getName());
                right(cs, regular, 11, RIGHT, 800, "Invoice No: " + inv.getInvoiceNumber());
                right(cs, regular, 11, RIGHT, 782, "Issued: " + inv.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate());
                cs.setNonStrokingColor(0f, 0f, 0f);

                // ---- bill-to block
                float y = 720;
                text(cs, bold, 11, LEFT, y, "BILL TO");
                text(cs, regular, 11, LEFT, y - 16, inv.getResidentName());
                text(cs, regular, 11, LEFT, y - 32, "Flat: " + inv.getHousehold().getFlatNumber());
                text(cs, bold, 11, 330, y, "BILLING PERIOD");
                text(cs, regular, 11, 330, y - 16, inv.getBillingCycle().getName());
                text(cs, regular, 11, 330, y - 32, inv.getBillingCycle().getStartDate() + " to " + inv.getBillingCycle().getEndDate());
                text(cs, regular, 11, 330, y - 48, "Payment status: " + inv.getPaymentStatus());

                // ---- meter readings
                y = 640;
                section(cs, bold, "METER READINGS", y);
                text(cs, regular, 11, LEFT, y - 20, "Previous reading");
                right(cs, regular, 11, RIGHT, y - 20, inv.getPreviousReading().toPlainString() + " kL");
                text(cs, regular, 11, LEFT, y - 36, "Current reading");
                right(cs, regular, 11, RIGHT, y - 36, inv.getCurrentReading().toPlainString() + " kL");
                text(cs, bold, 11, LEFT, y - 52, "Total consumption");
                right(cs, bold, 11, RIGHT, y - 52, inv.getConsumption().toPlainString() + " kL");

                // ---- charges table
                y = 560;
                section(cs, bold, "CHARGES", y);
                y -= 20;
                text(cs, bold, 10, LEFT, y, "Description");
                right(cs, bold, 10, 360, y, "Qty (kL)");
                right(cs, bold, 10, 450, y, "Rate (Rs./kL)");
                right(cs, bold, 10, RIGHT, y, "Amount (Rs.)");
                y -= 6;
                line(cs, y);
                y -= 15;
                for (InvoiceItem it : inv.getItems()) {
                    text(cs, regular, 10, LEFT, y, it.getDescription());
                    if (it.getQuantity() != null) right(cs, regular, 10, 360, y, it.getQuantity().toPlainString());
                    if (it.getRate() != null) right(cs, regular, 10, 450, y, it.getRate().toPlainString());
                    right(cs, regular, 10, RIGHT, y, it.getAmount().toPlainString());
                    y -= 16;
                }
                line(cs, y + 6);

                // ---- totals
                y -= 10;
                right(cs, regular, 11, 450, y, "Water charge (tariff)");
                right(cs, regular, 11, RIGHT, y, inv.getWaterCharge().toPlainString());
                y -= 16;
                right(cs, regular, 11, 450, y, "Shared water cost");
                right(cs, regular, 11, RIGHT, y, inv.getSharedCost().toPlainString());
                y -= 16;
                right(cs, regular, 11, 450, y, "Adjustments");
                right(cs, regular, 11, RIGHT, y, inv.getAdjustment().toPlainString());
                y -= 8;
                cs.setNonStrokingColor(0.9f, 0.95f, 0.99f);
                cs.addRect(300, y - 22, 245, 24);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);
                y -= 15;
                right(cs, bold, 13, 450, y, "TOTAL DUE");
                right(cs, bold, 13, RIGHT, y, "Rs. " + inv.getTotalAmount().toPlainString());

                // ---- notes
                y -= 50;
                section(cs, bold, "PAYMENT INSTRUCTIONS", y);
                y -= 18;
                for (String l : wrap(paymentInstructions, 95)) {
                    text(cs, regular, 10, LEFT, y, l);
                    y -= 14;
                }
                if (inv.getAdjustmentReason() != null && inv.getAdjustment().compareTo(BigDecimal.ZERO) != 0) {
                    y -= 6;
                    text(cs, regular, 10, LEFT, y, "Adjustment note: " + inv.getAdjustmentReason());
                }
                text(cs, regular, 8, LEFT, 40, "Generated automatically by the Smart Water Monitoring & Billing Platform. Shared cost basis: " + inv.getSharedCostBasis());
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate invoice PDF", e);
        }
    }

    private void section(PDPageContentStream cs, PDFont bold, String title, float y) throws IOException {
        cs.setNonStrokingColor(0.05f, 0.35f, 0.62f);
        text(cs, bold, 11, LEFT, y, title);
        cs.setNonStrokingColor(0f, 0f, 0f);
        line(cs, y - 5);
    }

    private void line(PDPageContentStream cs, float y) throws IOException {
        cs.setStrokingColor(0.7f, 0.7f, 0.7f);
        cs.moveTo(LEFT, y);
        cs.lineTo(RIGHT, y);
        cs.stroke();
    }

    private void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(clean(s));
        cs.endText();
    }

    private void right(PDPageContentStream cs, PDFont font, float size, float xRight, float y, String s) throws IOException {
        String c = clean(s);
        float w = font.getStringWidth(c) / 1000f * size;
        text(cs, font, size, xRight - w, y, c);
    }

    /** Standard-14 fonts only support basic Latin; replace anything else. */
    private String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) sb.append(ch >= 32 && ch <= 126 ? ch : '?');
        return sb.toString();
    }

    private List<String> wrap(String text, int max) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (cur.length() + word.length() + 1 > max) {
                lines.add(cur.toString());
                cur = new StringBuilder();
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
