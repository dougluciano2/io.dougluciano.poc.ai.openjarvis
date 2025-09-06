package io.dougluciano.poc.ai.openjarvis.integration.browser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.service.Result;
import io.dougluciano.poc.ai.openjarvis.core.datamining.MinedInvoiceDTO;
import io.dougluciano.poc.ai.openjarvis.integration.ai.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeleniumService {

    private static final Logger log = LoggerFactory.getLogger(SeleniumService.class);
    private static final String FRONTEND_URL = "http://localhost:4200/";
    private static final String PDF_OUTPUT_FILE = "danfe_simulado_pdf_resultado.pdf";


    private WebDriver driver;
    private WebDriverWait wait;
    private final AiFormAnalyzerService aiAnalyzerService;
    private final AiResponseParserService aiResponseParserService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    /**
     * Método principal que recebe os dados da nota e executa a automação.
     * @param invoice O "dossiê" completo da nota a ser preenchida.
     */
    public void fillInvoiceForm(MinedInvoiceDTO invoice) {
        String messageInfo = String.format("Iniciando automação com IA para a nota: {}", invoice.number());
        log.info(messageInfo);

        try{
            initializeBrowser();
            driver.get(FRONTEND_URL);
            String pageHtml = driver.getPageSource();
            String invoiceJson = objectMapper.writeValueAsString(invoice);
            String initialUrl = driver.getCurrentUrl();

            /*FASE 1 - PREENCHER OS DADOS DO FORMULÁRIO*/
            // ia analisando o conteúdo
            log.info("Chamando IA para análise....");
            String rawAiResponse = aiAnalyzerService.analyzeFormStructure(pageHtml, invoiceJson);
            log.debug("Resposta bruta da IA recebida: {}", rawAiResponse);
            // parsing da resposta da ia
            FormMappingResponse plan = aiResponseParserService.parseFormMappingResponse(rawAiResponse);
            // execução
            Map<String, Object> invoiceDataMap = objectMapper.convertValue(invoice, new TypeReference<>() {});
            // --- Preencher o cbeçalho da nota ---
            processHeaderFields(plan.getHeaderFieldMappings(), invoiceDataMap);
            // --- Preencher os itens da lista ---
            processItemList(plan.getListEntryMapping(), invoiceDataMap);

            /*FASE 2 - AGUARDAR O USUÁRIO A CLICAR NO BOTÃO DE SUBMIT*/
            log.info("FASE 2: Formulário preenchido. Aguardando intervenção humana para clicar em 'Confirmar Lançamento'...");
            waitForHumanSubmission(initialUrl);

            /*FASE 3 - APÓS CLIQUE, GERAR ARQUIVO PDF*/
            log.info("FASE 3: Submissão detectada. Aguardando renderização da DANFE para gerar PDF...");
            // 3.1: Atraso solicitado de 10 segundos para renderização da página final
            Thread.sleep(10000);

            // 3.2: Geração do PDF
            capturePageAsPdf(PDF_OUTPUT_FILE);
            log.info("PDF salvo com sucesso em: {}", PDF_OUTPUT_FILE);

        } catch (Exception e){
            log.error("Falha crítica durante a automação com IA da nota {}: {}", invoice.number(), e.getMessage(), e);
            throw new RuntimeException("Falha na automação do Selenium para a nota " + invoice.number(), e);
        } finally {
            closeBrowser();
        }
    }

    /**
     * Pausa a execução do robô e monitora ativamente a mudança de URL, indicando
     * que o usuário submeteu o formulário.
     * @param formUrl A URL original do formulário de preenchimento.
     */
    private void waitForHumanSubmission(String formUrl){
        // Define um timeout longo para a ação humana (ex: 10 minutos)
        WebDriverWait humanWait = new WebDriverWait(driver, Duration.ofMinutes(10));

        try {
            humanWait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(formUrl)));
            log.info("Mudança de URL detectada. Usuário submeteu o formulário.");
        } catch (org.openqa.selenium.TimeoutException e) {
            log.error("Timeout de 10 minutos excedido. Nenhuma ação humana detectada.");
            throw new RuntimeException("Timeout esperando pela submissão humana.");
        }
    }

    /**
     * Captura a visualização atual do navegador e salva como um arquivo PDF.
     * @param fileName Nome do arquivo de saída.
     */
    private void capturePageAsPdf(String fileName) throws IOException {
        PrintOptions printOptions = new PrintOptions();
        // Pode adicionar opções aqui (ex: printOptions.setPageRanges("1-2"))

        Pdf pdf = ((org.openqa.selenium.PrintsPage) driver).print(printOptions);

        // 1. Obter o conteúdo como string Base64.
        String base64Pdf = pdf.getContent();

        // 2. Decodificar a string Base64 para bytes.
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

        String userHomeDirectory = System.getProperty("user.home");

        Path downloadDirectoryPath = Paths.get(userHomeDirectory, "Downloads");

        if (!Files.exists(downloadDirectoryPath) || !Files.isDirectory(downloadDirectoryPath)) {
            log.warn("Diretório de Downloads não encontrado em {}. Salvando na pasta local do projeto.", downloadDirectoryPath);
            downloadDirectoryPath = Paths.get("."); // Salva no diretório de execução atual
        }

        // 2.4. Definir o caminho final completo do arquivo
        Path finalFilePath = downloadDirectoryPath.resolve(fileName);

        // 3. Salvar os bytes no arquivo.
        Files.write(Paths.get(fileName), pdfBytes);

        log.info("PDF salvo com sucesso em: {}", finalFilePath.toAbsolutePath());
    }

    private void processHeaderFields(
            List<FieldMappingInstruction> headerMappings,
            Map<String, Object> dataMap){

        if (headerMappings == null || headerMappings.isEmpty()){
            log.warn("Nenhum mapeamento de cabeçalho foi recebido da IA.");
            return;
        }

        log.info("Iniciando preenchimento de {} campos do cabeçalho...", headerMappings.size());
        for (FieldMappingInstruction instruction : headerMappings) {
            fillFieldFromInstruction(instruction, dataMap, null);
        }

    }

    private void processItemList(ListEntryMapping listMapping, Map<String, Object> dataMap) {
        if (listMapping == null || listMapping.getDataListKeyName() == null) {
            log.info("Nenhuma lista de itens para processar identificada pela IA.");
            return;
        }

        String listKey = listMapping.getDataListKeyName();
        List<Map<String, Object>> items = (List<Map<String, Object>>) dataMap.get(listKey);

        log.info("Iniciando processamento de {} itens da lista '{}'...", items.size(), listKey);

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> currentItemData = items.get(i);
            log.info("Preenchendo item {}/{}...", i + 1, items.size());

            // Preenche os campos do item atual
            for (FieldMappingInstruction itemInstruction : listMapping.getItemFieldMappings()) {
                fillFieldFromInstruction(itemInstruction, null, currentItemData);
            }

            // Clica em "Adicionar Item"
            try {
                WebElement addButton = driver.findElement(By.cssSelector(listMapping.getAddItemButtonSelector()));
                addButton.click();
                log.info("Botão 'Adicionar Item' clicado para o item {}.", i + 1);
                Thread.sleep(250); // Pequena pausa para o JS do frontend processar a adição
            } catch (Exception e) {
                log.error("Falha ao clicar no botão 'Adicionar Item' ({}): {}", listMapping.getAddItemButtonSelector(), e.getMessage());
            }
        }
    }

    private void submitForm(String submitButtonSelector) {
        if (submitButtonSelector == null || submitButtonSelector.isBlank()) {
            log.warn("Nenhum seletor de submissão final foi retornado pela IA.");
            return;
        }
        try {
            log.info("Tentando submeter o formulário usando o seletor final: {}", submitButtonSelector);
            WebElement submitButton = driver.findElement(By.cssSelector(submitButtonSelector));
            submitButton.click();
            log.info("Formulário submetido com sucesso.");
        } catch (Exception e) {
            log.error("Falha ao clicar no botão de submissão final ({}): {}", submitButtonSelector, e.getMessage());
        }
    }

    /**
     * Preenche um campo individual.
     * @param instruction Mapeamento da IA (contém seletor e nome do campo de dados)
     * @param headerDataMap Mapa de dados do cabeçalho (usar se itemDataMap for nulo)
     * @param itemDataMap Mapa de dados do item atual (usar se não for nulo)
     */
    private void fillFieldFromInstruction(FieldMappingInstruction instruction, Map<String, Object> headerDataMap, Map<String, Object> itemDataMap) {
        String fieldName = instruction.getDataFieldName();
        String selector = instruction.getCssSelector();
        try{
            Object objectValue;
            if (itemDataMap != null){
                objectValue = getValueFromMap(itemDataMap, fieldName);
            } else {
                objectValue = getValueFromMap(headerDataMap, fieldName);
            }
            String valueToFill = String.valueOf(objectValue);

            WebElement element = driver.findElement(By.cssSelector(selector));
            element.clear();
            element.sendKeys(valueToFill);
            log.debug("Campo [{}] preenchido com valor [{}] usando seletor [{}].", fieldName, valueToFill, selector);
        } catch (Exception e){
            log.warn("Falha ao tentar preencher campo [{}] usando seletor [{}]: {}", fieldName, selector, e.getMessage());
        }
    }

    private Object getValueFromMap(Map<String, Object> map, String fieldName){
        if (fieldName.contains(".")){
            String[] parts = fieldName.split("\\.", 2);
            Map<String, Object> nestedMap = (Map<String, Object>) map.get(parts[0]);
            return nestedMap.get(parts[1]);
        } else {
            return map.get(fieldName);
        }
    }
    /**
     * Configura e abre uma nova instância do navegador Chrome.
     */
    private void initializeBrowser() {
        log.info("Configurando e inicializando o WebDriver do Chrome...");
        WebDriverManager.chromedriver().setup(); // O WebDriverManager faz a mágica de baixar e configurar o driver
        this.driver = new ChromeDriver();
        this.driver.manage().window().maximize(); // Maximiza a janela
    }

    /**
     * Fecha a instância do navegador e encerra o processo do WebDriver.
     */
    private void closeBrowser() {
        if (driver != null) {
            log.info("Fechando o navegador...");
            driver.quit();
        }
    }
}
