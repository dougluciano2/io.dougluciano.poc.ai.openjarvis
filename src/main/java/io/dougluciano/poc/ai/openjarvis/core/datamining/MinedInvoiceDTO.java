package io.dougluciano.poc.ai.openjarvis.core.datamining;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MinedInvoiceDTO(
        Long invoiceId,
        String series,
        String number,
        LocalDateTime issueDate,
        String issuerName,
        String customerName,
        List<LegacyInvoiceItemDTO> items,
        BigDecimal totalAmount
) {
}
