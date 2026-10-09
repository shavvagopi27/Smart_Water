package com.example.smartwater.service;

import com.example.smartwater.dto.BillingDtos.InvoiceResponse;
import com.example.smartwater.entity.Invoice;
import com.example.smartwater.entity.PaymentStatus;
import com.example.smartwater.exception.ResourceNotFoundException;
import com.example.smartwater.repository.InvoiceRepository;
import com.example.smartwater.security.AuthUser;
import com.example.smartwater.security.SecurityUtils;
import com.example.smartwater.util.Mappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService pdfService;
    private final EmailService emailService;

    @Value("${app.billing.payment-instructions}")
    private String paymentInstructions;

    public record PdfFile(String fileName, byte[] content) {}

    /** Admin: all invoices (optionally by cycle / household). Resident: only their own household's invoices. */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(Long cycleId, Long householdId) {
        AuthUser user = SecurityUtils.currentUser();
        List<Invoice> list;
        if (user.isAdmin()) {
            list = householdId != null ? invoiceRepository.findByHouseholdIdOrderByIdDesc(householdId) : invoiceRepository.findAllByOrderByIdDesc();
        } else {
            if (user.householdId() == null) return List.of();
            if (householdId != null && !householdId.equals(user.householdId())) {
                throw new AccessDeniedException("You can only access your own invoices");
            }
            list = invoiceRepository.findByHouseholdIdOrderByIdDesc(user.householdId());
        }
        return list.stream()
                .filter(i -> cycleId == null || i.getBillingCycle().getId().equals(cycleId))
                .map(i -> Mappers.toInvoice(i, paymentInstructions)).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return Mappers.toInvoice(getAuthorized(id), paymentInstructions);
    }

    @Transactional(readOnly = true)
    public PdfFile pdf(Long id) {
        Invoice inv = getAuthorized(id);
        return new PdfFile(inv.getInvoiceNumber() + ".pdf", pdfService.render(inv));
    }

    @Transactional
    public InvoiceResponse markPaid(Long id) {
        Invoice inv = getAuthorized(id);
        inv.setPaymentStatus(PaymentStatus.PAID);
        return Mappers.toInvoice(invoiceRepository.save(inv), paymentInstructions);
    }

    @Transactional(readOnly = true)
    public void resendEmail(Long id) {
        Invoice inv = getAuthorized(id);
        emailService.sendInvoiceAvailable(inv, pdfService.render(inv));
    }

    private Invoice getAuthorized(Long id) {
        Invoice inv = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        SecurityUtils.requireHouseholdAccess(inv.getHousehold().getId());
        return inv;
    }
}
