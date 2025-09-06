package io.dougluciano.poc.ai.openjarvis.integration.ai;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AiFormAnalyzerService {

    /**
     * Analisa o HTML de um formulário para encontrar os seletores corretos para os dados fornecidos.
     *
     * @param htmlContent O código fonte HTML completo da página do formulário.
     * @param jsonDataToFill Um JSON contendo os dados da nota fiscal a serem preenchidos.
     * @return Um objeto FormMappingResponse contendo a lista de mapeamentos {dataFieldName, cssSelector}.
     */
    @SystemMessage("""
            Você é um processador de dados JSON. Sua única função é gerar um objeto JSON.
            NÃO forneça explicações, introduções, observações ou qualquer texto fora da estrutura JSON.
            Sua resposta DEVE começar com o caractere '{' e terminar com o caractere '}'.

            **Tarefa:** Analise o HTML e o JSON de entrada para criar um plano de preenchimento detalhado.

            **Instruções de Mapeamento:**
            1.  **Mapeamento Semântico:** Compare os nomes dos campos no JSON de entrada com os labels (<label>) e placeholders do HTML. Por exemplo, "Data de Emissão" no HTML deve ser mapeado para o campo correspondente a data no JSON de entrada.
            2.  **Chave de Saída (`dataFieldName`):** No JSON de saída, o valor de `dataFieldName` DEVE ser o nome exato da chave conforme aparece no JSON de entrada. Se o JSON de entrada tiver {"invoiceId": "123"}, a saída deve ser {"dataFieldName": "invoiceId", ...}.
            3.  **Estrutura de Array OBRIGATÓRIA:** Os campos `headerFieldMappings` e `itemFieldMappings` devem ser arrays JSON, onde cada objeto no array contém as chaves `dataFieldName` e `cssSelector`.

            **Formato de Saída OBRIGATÓRIO (Exemplo de estrutura):**
            {
              "headerFieldMappings": [
                { "dataFieldName": "nomeDoCampoJson1", "cssSelector": "#seletorHtml1" },
                { "dataFieldName": "nomeDoCampoJson2", "cssSelector": "#seletorHtml2" }
              ],
              "listEntryMapping": {
                "dataListKeyName": "items",
                "itemFieldMappings": [
                    { "dataFieldName": "campoItem1", "cssSelector": "#seletorItem1" }
                ],
                "addItemButtonSelector": "#botao_adicionar_item"
              },
              "submitButtonSelector": "#botao_confirmar_envio"
            }
            """)
    @UserMessage("""
            HTML da Página:
            ```html
            {{htmlContent}}
            ```

            Dados a serem preenchidos (JSON de entrada):
            ```json
            {{jsonDataToFill}}
            ```

            Gere o plano de preenchimento JSON. Lembre-se: não inclua nenhum texto explicativo, apenas o JSON.
            """)
    String analyzeFormStructure(
            @dev.langchain4j.service.V("htmlContent") String htmlContent,
            @dev.langchain4j.service.V("jsonDataToFill") String jsonDataToFill
    );

}
