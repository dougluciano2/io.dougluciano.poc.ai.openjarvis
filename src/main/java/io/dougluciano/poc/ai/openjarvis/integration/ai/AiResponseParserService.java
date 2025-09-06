package io.dougluciano.poc.ai.openjarvis.integration.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiResponseParserService {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParserService.class);
    private final ObjectMapper objectMapper;

    /**
     * Processa a resposta textual bruta da IA, extrai o bloco JSON e o converte
     * no objeto de plano de execução FormMappingResponse.
     *
     * @param rawAiResponse A resposta completa retornada pela IA.
     * @return O objeto FormMappingResponse preenchido.
     * @throws JsonProcessingException Se o JSON extraído for malformado.
     */
    public FormMappingResponse parseFormMappingResponse(String rawAiResponse) throws JsonProcessingException {
        log.info("Recebida resposta bruta da IA. Iniciando extração do JSON...");

        // 1. Extração robusta do JSON usando Regex.
        // Procura pelo primeiro '{' até o último '}' na resposta, ignorando texto antes e depois.
        Pattern pattern = Pattern.compile("\\{(.*)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(rawAiResponse);

        String cleanedJson;
        if (matcher.find()) {
            cleanedJson = matcher.group(0);
            log.info("JSON extraído com sucesso.");
        } else {
            log.error("Nenhum bloco JSON válido (iniciando com '{{' e terminando com '}}') foi encontrado na resposta bruta: {}", rawAiResponse);
            throw new RuntimeException("Falha ao extrair JSON da resposta da IA. Resposta não continha JSON válido.");
        }

        // 2. Conversão do JSON limpo para o objeto Java.
        return objectMapper.readValue(cleanedJson, FormMappingResponse.class);
    }
}
