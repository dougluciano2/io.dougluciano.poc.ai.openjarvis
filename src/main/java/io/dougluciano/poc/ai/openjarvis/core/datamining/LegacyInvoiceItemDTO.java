package io.dougluciano.poc.ai.openjarvis.core.datamining;

import java.math.BigDecimal;

public record LegacyInvoiceItemDTO(
        Integer sequence,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
