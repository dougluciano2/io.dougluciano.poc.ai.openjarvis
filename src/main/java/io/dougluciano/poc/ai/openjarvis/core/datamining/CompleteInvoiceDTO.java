package io.dougluciano.poc.ai.openjarvis.core.datamining;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompleteInvoiceDTO(
        // Dados do cabeçalho
        Long invoiceId,
        String series,
        String number,
        // Dados do emissor
        String issuerName,
        String issuerDocument,
        // Dados do cliente
        String customerName,
        String customerDocument,
        // Lista de itens
        List<LegacyInvoiceItemDTO> items
) {
}
