package io.dougluciano.poc.ai.openjarvis.integration.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter @Setter @ToString
public class FormMappingResponse {

    /**
     * Lista de mapeamentos de campos de dados do cabeçalho (que não se repetem).
     */
    private List<FieldMappingInstruction> headerFieldMappings;

    /**
     * Mapeamento da lógica de entrada da lista de itens (se houver).
     */
    private ListEntryMapping listEntryMapping;

    /**
     * O seletor CSS identificado pela IA para o botão principal de submissão final do formulário.
     */
    private String submitButtonSelector;
}
