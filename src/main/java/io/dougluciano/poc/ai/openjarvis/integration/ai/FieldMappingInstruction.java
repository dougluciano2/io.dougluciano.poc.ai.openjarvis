package io.dougluciano.poc.ai.openjarvis.integration.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class FieldMappingInstruction {

    /**
     * O nome do campo de dados original do nosso DTO (ex: "issuerName", "totalAmount").
     * A IA usará isso para nos dizer qual dado deve ir em qual seletor.
     */
    private String dataFieldName;

    /**
     * O seletor CSS (ex: "#campo-nome-prestador") que a IA identificou no HTML
     * para este campo de dados.
     */
    private String cssSelector;
}
