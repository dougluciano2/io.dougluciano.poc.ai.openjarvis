package io.dougluciano.poc.ai.openjarvis.integration.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter @Setter @ToString
public class ListEntryMapping {

    /**
     * O nome da chave que contem a lista de itens.
     * EX: products, itens. A IA deve identificar isso a partir do DTO
     */
    private String dataListKeyName;

    /**
     * Mapeamento dos campos de entrada para um ÚNICO item da lista.
     * Ex: {"dataFieldName": "productId", "cssSelector": "#item_codigo_servico"}
     */
    private List<FieldMappingInstruction> itemFieldMappings;

    /**
     * Seletor CSS do botão "Adicionar Item" que deve ser clicado após preencher
     * os campos de um único item.
     */
    private String addItemButtonSelector;
}
