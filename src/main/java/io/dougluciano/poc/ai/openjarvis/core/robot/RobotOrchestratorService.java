package io.dougluciano.poc.ai.openjarvis.core.robot;

import io.dougluciano.poc.ai.openjarvis.core.datamining.LegacyDbService;
import io.dougluciano.poc.ai.openjarvis.core.datamining.MinedInvoiceDTO;
import io.dougluciano.poc.ai.openjarvis.integration.browser.SeleniumService;
import io.dougluciano.poc.ai.openjarvis.ui.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotOrchestratorService {

    // esta linha abaixo vai instanciar o logger e observar a classe
    private static final Logger log = LoggerFactory.getLogger(RobotOrchestratorService.class);

    // injetando o serviço para o streaming das logs
    private final SseService sseService;
    private final LegacyDbService legacyDbService;
    private final RedisQueueService redisQueueService;
    private final SeleniumService seleniumService;

    @Async
    public void startRobot(){
        String startMessage = ">>> PROCESSO DO ROBÔ INICIADO COM SUCESSO! <<<";
        sseService.sendSseEvent(startMessage);
        log.info("========================================================");
        log.info("Olá, eu me chamo openJarvis, é um imenso prazer lhe servir!");
        log.info("Agora vou iniciar o processamento......");
        log.info("========================================================");

        try{
            // --- FASE 1: PRODUTOR (MINERAR E ENFILEIRAR) ---
            // Este método cuida de toda a lógica de buscar e enfileirar.
            produceAndQueueInvoices();

            // --- FASE 2: CONSUMIDOR (PROCESSAR A FILA) ---
            // Este método cuida de toda a lógica de ler da fila e chamar o Selenium.
            processInvoiceQueue();


        } catch (InterruptedException e){
            handleInterruptedException(e);
        } catch (Exception e){
            handleGenericException(e);
        }

        String endMessage = "[INFO] Processamento finalizado.";
        sseService.sendSseEvent(endMessage);
        log.info(endMessage);
        log.info("==============================================");

    }

    private void produceAndQueueInvoices() throws InterruptedException {
        String beginMining = "[FASE 1] Iniciando mineração de dados...";
        sseService.sendSseEvent(beginMining);
        log.info(beginMining);
        List<MinedInvoiceDTO> pendingInvoices = legacyDbService.findCompletePendingInvoices();

        String miningComplete = String.format("[FASE 1] Mineração concluída. %d notas encontradas.", pendingInvoices.size());
        sseService.sendSseEvent(miningComplete);
        log.info(miningComplete);


        Thread.sleep(1000);

        String sendToQueue = "[FASE 1] Enviando notas para a fila de processamento no Redis...";

        log.info(sendToQueue);
        sseService.sendSseEvent(sendToQueue);

        for (MinedInvoiceDTO invoice: pendingInvoices) {
            redisQueueService.queueInvoice(invoice);
        }

        Long queueSize = redisQueueService.getQueueSize();
        String queueReady = String.format("[FASE 1] %d notas enfileiradas com sucesso! Fila pronta.", queueSize);
        sseService.sendSseEvent(queueReady);
        log.info(queueReady);
        Thread.sleep(1000);
    }

    private void processInvoiceQueue(){
        sseService.sendSseEvent("[FASE 2] Iniciando o processamento da fila.....");

        while (redisQueueService.getQueueSize() > 0){
            MinedInvoiceDTO invoiceToProcess = redisQueueService.deQueueInvoice();

            String processsMsg = String.format("[FASE 2] Processando Nota Nº %s...", invoiceToProcess.number());
            log.info(processsMsg);
            sseService.sendSseEvent(processsMsg);

            try{
                seleniumService.fillInvoiceForm(invoiceToProcess);
                String processedMsg = String.format("[SUCCESS] Nota Nº %s processada com sucesso!", invoiceToProcess.number());
                log.info(processedMsg);
                sseService.sendSseEvent(processedMsg);
            } catch (Exception e){
                String errorMsg = String.format("[ERROR] Falha ao processar Nota Nº %s. Verifique os logs do sistema.", invoiceToProcess.number());
                log.error(String.format("Falha na automação da Nota %s", invoiceToProcess.number()), e); // Log completo do erro
                sseService.sendSseEvent(errorMsg);
            }
        }

        String voidQueueMsg = "[FASE 2] Fila de processamento vazia.";
        log.info(voidQueueMsg);
        sseService.sendSseEvent(voidQueueMsg);
    }

    private void handleInterruptedException(InterruptedException e) {
        log.error("O processo do robô foi interrompido.", e);
        sseService.sendSseEvent("[ERROR] O processo foi interrompido!");
        Thread.currentThread().interrupt();
    }

    private void handleGenericException(Exception e) {
        log.error("Ocorreu um erro inesperado durante a execução do robô.", e);
        sseService.sendSseEvent("[ERROR] Ocorreu um erro inesperado: " + e.getMessage());
        Thread.currentThread().interrupt();
    }
}
