package io.dougluciano.poc.ai.openjarvis.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class LangChainConfig {

    @Value("${langchain.openai.api-key}")
    private String apiKey;

    @Value("${langchain.openai.model-name}")
    private String modelName;

    /**
     * Cria e disponibiliza o bean do ChatLanguageModel para ser usado pelo @AiService.
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                // Você pode adicionar outras configurações aqui, como temperatura ou timeout.
                // .temperature(0.7)
                // .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
