package io.dougluciano.poc.ai.openjarvis.core.robot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dougluciano.poc.ai.openjarvis.core.datamining.MinedInvoiceDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String QUEUE_KEY = "invoice_processing_queue";

    // esta linha abaixo vai instanciar o logger e observar a classe
    private static final Logger log = LoggerFactory.getLogger(RedisQueueService.class);

    /**
     * Adiciona uma nota fiscal na fila de processamento do Redis.
     * @param invoice O DTO completo da nota a ser enfileirado.
     */
    public void queueInvoice(MinedInvoiceDTO invoice) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, invoice);
    }

    /**
     * Remove e retorna a próxima nota da fila.
     * @return O DTO da nota, ou null se a fila estiver vazia.
     */
    public MinedInvoiceDTO deQueueInvoice() {
        Object rawObject = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (rawObject == null){
            return null;
        }

        try{
            return objectMapper.convertValue(rawObject, MinedInvoiceDTO.class);
        } catch (IllegalArgumentException e){
            log.error("Falha ao converter objeto da fila do Redis para MinedInvoiceDTO.", e);
            return null;
        }
    }

    /**
     * Retorna o tamanho atual da fila.
     * @return O número de itens na fila.
     */
    public Long getQueueSize() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}
