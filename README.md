# POC OpenJarvis: Robô de Automação com IA Semântica

![Status do Projeto](https://img.shields.io/badge/status-Finalizado-green)

## 📖 Sobre o Projeto

Este repositório contém o projeto central da Prova de Conceito (POC): **OpenJarvis**, um robô de automação inteligente desenvolvido em Java. Ele atua como o orquestrador que consome dados de um sistema legado, interpreta uma interface web desconhecida e realiza o preenchimento de formulários de forma autônoma, com a ajuda de um agente de IA.

O objetivo desta POC é demonstrar uma abordagem de automação resiliente, que não depende de seletores fixos (como IDs ou XPaths), mas sim da **compreensão semântica** do conteúdo de uma página.

## 🎯 O Desafio: Automação Rígida vs. Semântica

Automações tradicionais (RPA) são frequentemente "rígidas". Elas são programadas para encontrar um campo com um ID específico, como `id="cnpj_cliente"`. Se o front-end for atualizado e esse ID mudar para `id="cnpj_tomador"`, a automação quebra.

O OpenJarvis aborda esse problema utilizando uma LLM (através da API da OpenAI) para entender o **contexto**. Ele é capaz de associar o dado "CNPJ" ao campo rotulado como "CNPJ do Tomador de Serviços", mesmo sem nunca ter visto aquela interface antes.

##  diagrama Arquitetura e Fluxo de Execução

O OpenJarvis opera conectando três componentes principais: o back-end legado, o front-end simulado e a API de IA.

```mermaid
graph TD
    A[Sistema Legado (PostgreSQL)] -- 1. Leitura via JDBC --> B{OpenJarvis - Robô IA};
    B -- 2. Cache de dados --> C[Redis];
    D[Front-end Simulador (Angular)] -- 3. Leitura da página via Selenium --> B;
    B -- 4. Envia página + dados para IA --> E[OpenAI API via LangChain4j];
    E -- 5. Retorna Associação Semântica --> B;
    B -- 6. Selenium preenche o formulário --> D;
    D -- 7. Aguarda confirmação humana --> F[Operador];
    F -- 8. Confirma --> B;
    B -- 9. Salva evidência em PDF --> G[Arquivo PDF];
```

**Fluxo Detalhado:**

1.  **Ingestão de Dados:** O robô se conecta ao **[Back-end Legado](https://github.com/dougluciano2/poc-ia-back-end-java)** via JDBC, lê as Notas Fiscais pendentes e armazena essas informações em um cache no **Redis**.
2.  **Análise do Alvo:** Utilizando **Selenium**, o robô acessa o **[Front-end Simulador](https://github.com/dougluciano2/io.dougluciano.ia.poc-openjarvis-front-end-simulador-site-prefeitura-emissao-nf)** e captura a estrutura da página.
3.  **Mapeamento com IA (O Cérebro):** O OpenJarvis envia a estrutura da página e os dados da Nota Fiscal (do Redis) para o agente de IA. A IA realiza a **associação semântica**, determinando qual campo na tela corresponde a qual dado (ex: "CPF/CNPJ do Tomador" -> `notaFiscal.getCnpj()`).
4.  **Ação Automatizada:** De posse desse mapeamento, o Selenium preenche o formulário com precisão.
5.  **Humano-no-Loop:** O processo pausa e aguarda a confirmação de um operador humano através de uma interface simples (gerada com **Thymeleaf**).
6.  **Geração de Evidência:** Após a confirmação, o robô salva um **PDF** da tela de resultado como prova da execução.

## 🚀 Stack de Tecnologias

-   **Java 21** e **Spring Boot 3.x**: Plataforma principal de desenvolvimento.
-   **Redis**: Utilizado como cache de alta performance para os dados das Notas Fiscais.
-   **JDBC**: Para conexão direta com o banco de dados do sistema legado.
-   **Selenium**: Ferramenta de automação de navegador para interagir com o front-end.
-   **LangChain4j**: Framework que simplifica a orquestração e a comunicação com a API da OpenAI.
-   **OpenAI API**: Fornece o modelo de linguagem (LLM) que atua como o cérebro do robô.
-   **Spring Thymeleaf**: Utilizado para criar a tela de confirmação para o operador humano.
-   **Lombok**: Para redução de código boilerplate.

## ⚙️ Como Configurar o Ecossistema Completo

Para executar esta POC, você precisa ter os três componentes rodando simultaneamente.

### Passo 1: Iniciar o Sistema Legado (Back-end)

1.  Clone o repositório: `git clone https://github.com/dougluciano2/poc-ia-back-end-java.git`
2.  Navegue até a pasta: `cd poc-ia-back-end-java`
3.  Execute: `docker-compose up --build -d`
    *(Isto irá subir a API e o banco de dados PostgreSQL com os dados iniciais)*

### Passo 2: Iniciar o Simulador (Front-end)

1.  Clone o repositório: `git clone https://github.com/dougluciano2/io.dougluciano.ia.poc-openjarvis-front-end-simulador-site-prefeitura-emissao-nf.git`
2.  Navegue até a pasta: `cd io.dougluciano.ia.poc-openjarvis-front-end-simulador-site-prefeitura-emissao-nf`
3.  Instale as dependências: `npm install`
4.  Execute: `ng serve`
    *(A aplicação estará disponível em http://localhost:4200)*

### Passo 3: Configurar e Executar o OpenJarvis (Este Projeto)

1.  **Clone este repositório.**
2.  **Configure a Chave da API:** No arquivo `src/main/resources/application.properties`, insira sua chave da OpenAI:
    ```properties
    langchain.open-ai.chat-model.api-key=SUA_CHAVE_API_AQUI
    ```
3.  **Execute a aplicação Spring Boot** através da sua IDE ou utilizando Maven:
    ```bash
    mvn spring-boot:run
    ```

Após o passo 3, o robô iniciará o processo de automação.
