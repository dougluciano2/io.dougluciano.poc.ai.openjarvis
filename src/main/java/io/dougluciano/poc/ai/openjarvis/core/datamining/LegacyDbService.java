package io.dougluciano.poc.ai.openjarvis.core.datamining;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegacyDbService {

    private static final Logger log = LoggerFactory.getLogger(LegacyDbService.class);

    private final JdbcTemplate jdbcTemplate;

    public List<Long> findPendingInvoices(){

        log.info("Executando query para buscar notas pendentes no banco de dados legado");

        String sql = "SELECT id FROM tbl_invoices WHERE status = 'PENDING'";

        return jdbcTemplate.queryForList(sql, Long.class);
    }

    public List<MinedInvoiceDTO> findCompletePendingInvoices() {
        log.info("Iniciando mineração dos dados das notas fiscaisi.......");
        List<Long> pendingInvoiceIds = findPendingInvoices();

        log.info("{} notas pendentes encontradas. Buscando detalhes das notas.....", pendingInvoiceIds.size());

        return pendingInvoiceIds.stream()
                .map(this::fetchCompleteInvoiceDetails)
                .collect(Collectors.toList());

        }

        private MinedInvoiceDTO fetchCompleteInvoiceDetails(Long invoiceID){

            MinedInvoiceDTO header = findInvoiceHeaderById(invoiceID);

            List<LegacyInvoiceItemDTO> items = findInvoiceItemsById(invoiceID);

            return new MinedInvoiceDTO(
                    header.invoiceId(),
                    header.series(),
                    header.number(),
                    header.issueDate(),
                    header.issuerName(),
                    header.customerName(),
                    items,
                    header.totalAmount()
            );
    }

    private MinedInvoiceDTO findInvoiceHeaderById(Long invoiceId){
        String sql = """
            SELECT
                i.id, i.series, i.number, i.issue_date, i.amount_value,
                p_issuer.name AS issuer_name,
                p_customer.name AS customer_name
            FROM
                tbl_invoices i
            INNER JOIN
                tbl_persons p_issuer ON i.issuer_id = p_issuer.id
            INNER JOIN
                tbl_persons p_customer ON i.customer_id = p_customer.id
            WHERE
                i.id = ?
        """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapToMinedInvoiceDTO(rs), invoiceId);
    }

    private List<LegacyInvoiceItemDTO> findInvoiceItemsById(Long invoiceId) {
        String sql = """
                SELECT
                    itm.num_seq, prd.name, itm.quantity, itm.unity_value
                FROM
                    tbl_invoice_items itm
                INNER JOIN
                    tbl_products prd ON itm.product_id = prd.id
                WHERE
                    itm.invoice_id = ?
                ORDER BY
                    itm.num_seq
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LegacyInvoiceItemDTO(
                rs.getInt("num_seq"),
                rs.getString("name"),
                rs.getInt("quantity"),
                rs.getBigDecimal("unity_value")
        ), invoiceId);
    }

    private MinedInvoiceDTO mapToMinedInvoiceDTO(ResultSet rs) throws SQLException {
        return new MinedInvoiceDTO(
                rs.getLong("id"),
                rs.getString("series"),
                rs.getString("number"),
                rs.getTimestamp("issue_date").toLocalDateTime(),
                rs.getString("issuer_name"),
                rs.getString("customer_name"),
                null, // A lista de itens será preenchida pelo metodo fetchCompleteInvoiceDetails
                rs.getBigDecimal("amount_value")
        );
    }
}
