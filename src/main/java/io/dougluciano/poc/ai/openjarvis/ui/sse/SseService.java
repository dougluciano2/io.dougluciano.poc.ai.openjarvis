package io.dougluciano.poc.ai.openjarvis.ui.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void addEmmitter(SseEmitter emitter){
        this.emitters.add(emitter);
        log.info("Novo cliente conectado para streaming de logs. Total de clientes: {}", emitters.size());

        emitter.onCompletion(() -> {
            this.emitters.remove(emitter);
            log.info("Cliente desconectado. Total de clientes: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            this.emitters.remove(emitter);
            log.warn("Cliente desconectado por timeout. Total de clientes: {}", emitters.size());
        });

    }

    public void sendSseEvent(String message){
        log.debug("Enviando evento SSE para {} clientes: {}", emitters.size(), message);
        for (SseEmitter emitter: emitters){
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                log.error("Erro ao enviar evento para SSE de um cliente. Removendo da lista. ", e);
                emitters.remove(emitter);
            }
        }
    }

}
