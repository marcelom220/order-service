# Serviço de Pedidos Seguros - teste itaú

Este serviço é responsável pelo processamento e gerenciamento do ciclo de vida de pedidos de seguro, integrando-se com sistemas de fraude, pagamento e subscrição (simulados).

## Pré-requisitos

*   Java 17
*   Maven 3.8+
*   MongoDB (rodando localmente na porta padrão 27017 ou configurado via `application.yml`)
*   RabbitMQ (rodando localmente na porta padrão 5672 ou configurado via `application.yml`)
*   Docker (opcional, para rodar MongoDB e RabbitMQ em contêineres, veja `docker-compose.yml`)

arquivo compose.yml disponivel para rodar MongoDB e RabbitMQ em contêineres Docker.
Além do mock da api de fraude.

### Obtendo o Projeto
O projeto será enviado em formato .zip. Basta descompactar o arquivo e abrir o projeto na sua IDE Java de preferência (ex: IntelliJ IDEA, Eclipse).

### Construindo o Projeto
Navegue até o diretório raiz do projeto no seu terminal e execute:
mvn clean package


### Rodando a Aplicação

Existem algumas maneiras de rodar a aplicação:

1.  **Via Maven Spring Boot Plugin:**
    A aplicação estará disponível em http://localhost:8080 (ou a porta configurada).
2. **Via IDE:**
    Execute a classe `br.com.itau.secure.OrderServiceApplication` como uma aplicação Java.
3. **Como um JAR Executável:**
       Após construir com `mvn clean install`, você pode rodar o JAR gerado:
   java -jar target/order-service-0.0.1-SNAPSHOT.jar


**é obrigatório o uso do arquivo compose.yml para rodar o serviço, pois ele depende do MongoDB e RabbitMQ para funcionar corretamente além do mock da api de fraude.**


## Detalhes sobre a Solução e Racional das Decisões

Esta seção detalha as principais escolhas de arquitetura e design feitas durante o desenvolvimento deste serviço.

### 1. Gerenciamento de Estado dos Pedidos (State Pattern com Strategy Pattern)
*   **Decisão:** Utilizar o padrão de projeto **State** para gerenciar o ciclo de vida dos pedidos de seguro (`SecureOrder`).
*   **Racional:**
    *   O ciclo de vida de um pedido possui múltiplos estados (RECEIVED, VALIDATED, PENDING, APPROVED, REJECTED, CANCELLED) com transições e lógicas específicas para cada um.
    *   O padrão State encapsula o comportamento associado a um estado particular e como ele reage a eventos ou transições, tornando o código mais limpo, organizado e fácil de estender.
    *   Evita condicionais complexas (`if/else if/else` ou `switch`) espalhadas pelo código de serviço para tratar diferentes estados.
    *   Cada estado é representado por uma implementação da interface `SecureOrderStateStrategy`, e o `SecureOrderStatus` (enum Java) atua como o contexto que delega as operações para a estratégia do estado atual.

### 2. Validação de Regras de Negócio (Strategy Pattern)
*   **Decisão:** Aplicar o padrão de projeto **Strategy** para as validações de regras de negócio baseadas no perfil de risco do cliente e no tipo de seguro (ex: limites de capital segurado).
*   **Racional:**
    *   As regras de validação variam significativamente dependendo do perfil do cliente (REGULAR, HIGH_RISK, PREFERRED, NO_INFORMATION).
    *   O padrão Strategy permite encapsular cada conjunto de regras em uma classe Java separada (ex: `RegularCustomerValidationRule`, `HighRiskCustomerValidationRule`), implementando uma interface comum (`CustomerTypeValidationRule`).
    *   Isso promove o Princípio Aberto/Fechado: novas regras ou perfis de cliente podem ser adicionados criando novas estratégias sem modificar o código Java existente que as utiliza.
    *   A lógica de seleção da estratégia apropriada (baseada no `CustomerRiskProfile`) é centralizada (ex: em `CustomerTypeValidationRuleFactory` ou em um serviço de validação).

### 3. Comunicação Assíncrona com RabbitMQ
Poderia ser kafka também.
*   **Decisão:** Utilizar RabbitMQ para simular a comunicação com serviços externos (Pagamento e Subscrição) e para o processamento interno de eventos de pedidos.
*   **Racional:**
    *   **Desacoplamento:** Permite que o serviço de pedidos publique eventos (ex: pedido pendente para pagamento) sem conhecer os detalhes dos consumidores (serviços de pagamento/subscrição).
    *   **Resiliência:** Se um serviço consumidor estiver temporariamente indisponível, as mensagens permanecem na fila para processamento posterior.
    *   **Escalabilidade:** Consumidores podem ser escalados independentemente para lidar com a carga de mensagens.
    * **Lembrando que os serviços de pagamento e subscrição foram realizados via mock no código do java mesmo, ou seja, não existe uma comunicação real com esses serviços, apenas simulação de eventos no status pendente do pedido.**


### 4. Persistência com MongoDB
*   **Decisão:** Utilizar MongoDB como banco de dados.
*   **Racional:**
    *   **Flexibilidade de Esquema:** Adequado para armazenar documentos de pedidos (`SecureOrder`, uma classe Java POJO anotada com Spring Data MongoDB) que podem ter uma estrutura com campos opcionais ou listas de tamanho variável (como o histórico de status).
    *   **Escalabilidade Horizontal:** Embora não seja um requisito primário para este teste, MongoDB é conhecido por sua capacidade de escalar.
    *   **Facilidade de Desenvolvimento:** Boa integração com Spring Data MongoDB, simplificando as operações de CRUD e consultas através de interfaces de Repositório Java.

### 5. Estrutura do Projeto
*   **Decisão:** Organizar o projeto Java em pacotes por funcionalidade/camada (ex: `domain`, `api`, `infra`, `service`).
*   **Racional:**
    *   Promove uma separação clara de responsabilidades e facilita a navegação e manutenção do código.
    *   `domain`: Contém as entidades de negócio (classes Java), enums de domínio, exceções de domínio e interfaces de repositório.
    *   `api`: Contém  os controllers, clients e modelos de DTOs (Data Transfer Objects - classes Java) para requisições/respostas da API REST e eventos.
    *   `service`: Lógica de negócio e orquestração (classes Java), incluindo as implementações das estratégias de estado e validação.
    *   `infra`: Configurações de infraestrutura (RabbitMQ, Web, etc. - classes Java de configuração Spring) e implementações de componentes como publicadores de eventos.

## Premissas Assumidas

Durante o desenvolvimento, algumas premissas foram feitas para dar prosseguimento à solução, dado o escopo do teste:

1.  **Simulação de Serviços Externos:**
    *   **Premissa:** Os serviços de Fraude, Pagamento e Subscrição não estão disponíveis.
    *   **Motivação:** Para permitir o teste do fluxo completo do pedido, a API de Fraude é simulada retornando dados pré-definidos ou aleatórios. Os serviços de Pagamento e Subscrição são simulados através de listeners de eventos Spring que, após um pedido ser marcado como `PENDING`, publicam eventos de resposta (sucesso/falha) para as filas correspondentes, como se fossem os serviços reais respondendo.

2.  **Estrutura dos Eventos de Resposta (Pagamento/Subscrição):**
    *   **Premissa:** Os eventos de resposta dos serviços simulados de pagamento e subscrição contêm um `orderId` e um status simples (ex: "CONFIRMED", "DENIED").
    *   **Motivação:** Simplificar a lógica de consumo desses eventos no `SecureOrderService` (classe Java). Em um cenário real, esses eventos poderiam ser mais ricos em informações.

3.  **Validação de Fraude Síncrona (Inicial):**
    *   **Premissa:** A chamada inicial para a API de Fraudes (simulada) ocorre de forma síncrona após a criação do pedido, antes de transitar para o estado `VALIDATED`.
    *   **Motivação:** Permitir que o resultado da análise de fraude (classificação de risco) seja usado imediatamente para as validações subsequentes de regras de negócio.

4.  **Interpretação das Regras de Limite:**
    *   **Premissa:** Para as regras de limite de capital segurado:
        *   "não ultrapasse X" foi interpretado como `valor <= X`.
        *   "inferior a X" foi interpretado como `valor < X`.
    *   **Motivação:** Seguir a interpretação literal das frases para definir os operadores de comparação na lógica Java.

5.  **Categoria "Qualquer outro tipo de seguro":**
    *   **Premissa:** A categoria `InsuranceCategory.OTHER` (um enum Java) foi usada para representar "qualquer outro tipo de seguro" nas regras de validação.
    *   **Motivação:** Ter uma categoria genérica para aplicar as regras quando o tipo de seguro não se encaixa nas categorias específicas (VIDA, AUTO, RESIDENCIAL).

## Configuração

A aplicação pode ser configurada através do arquivo `application.yml` (ou `application.properties`) localizado em `src/main/resources`.

### Configurações Principais
*   Conexão com o MongoDB: `spring.data.mongodb.uri`
*   Conexão com o RabbitMQ: `spring.rabbitmq.host`, `spring.rabbitmq.port`, etc.
*   Nomes de Filas e Exchanges: Sob a chave `mq`.

**Exemplo (`application.yml`):**

## Endpoints da API (Exemplo)
Existe uma collection junto a aplicação que pode ser usada. 
A API REST do serviço de pedidos de seguro expõe os seguintes endpoints principais:

*   **`POST /api/v1/secure-orders`**: Cria um novo pedido de seguro.
    *   **Método:** `POST`
    *   **Corpo da Requisição ({
        "customer_id": "adc56d77-348c-4bf0-908f-22d402ee715c",
        "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
        "category": "AUTO",
        "salesChannel": "MOBILE",
        "paymentMethod": "CREDIT_CARD",
        "total_monthly_premium_amount": 75.25,
        "insured_amount": 275000.50,
        "coverages": {
        "Roubo": 100000.25,
        "Perda Total": 100000.25,
        "Colisão com Terceiros": 75000.00
        },
        "assistances": [
        "Guincho até 250km",
        "Troca de Óleo",
        "Chaveiro 24h"
        ]
        }):**

    *   **Resposta de Sucesso (201 Created):** O pedido criado, incluindo seu ID e status inicial.

*   **`GET /api/v1/secure-orders/{orderId}`**: Busca um pedido de seguro pelo seu ID.
    *   **Método:** `GET`
    *   **Parâmetro de Caminho:** `orderId` (String) - O ID único do pedido.
    *   **Resposta de Sucesso (200 OK):** Os detalhes do pedido.

    *   **Resposta de Erro (404 Not Found):** Se o pedido com o ID fornecido não for encontrado.

*   **`GET /api/v1/secure-orders?customerId={customerId}`**: Busca todos os pedidos de seguro de um cliente específico.
    *   **Método:** `GET`
    *   **Parâmetro de Query:** `customerId` (String, obrigatório) - O ID do cliente.
    *   **Resposta de Sucesso (200 OK):** Uma lista de pedidos do cliente.

    
*   **`PATCH /api/v1/secure-orders/{orderId}/cancel`**: Solicita o cancelamento de um pedido de seguro.
    *   **Método:** `PATCH`
    *   **Parâmetro de Caminho:** `orderId` (String) - O ID único do pedido a ser cancelado.
    *   **Corpo da Requisição:** Opcional. Pode ser vazio ou conter um motivo para o cancelamento.

## Destaques da Solução e Próximos Passos

Esta solução foi desenvolvida com foco em boas práticas de engenharia de software, visando qualidade, manutenibilidade e extensibilidade.

### Pontos Fortes Implementados:

*   **Testabilidade:** Foram criados testes unitários e de integração para garantir a confiabilidade das principais funcionalidades. A cobertura de testes foi uma preocupação constante durante o desenvolvimento.
*   **Arquitetura Robusta:** A arquitetura da solução foi desenhada para ser clara e modular, facilitando o entendimento e futuras evoluções.
    *   **Baixo Acoplamento e Alta Coesão:** Os componentes foram projetados para terem responsabilidades bem definidas e interagirem de forma desacoplada, promovendo a coesão interna de cada módulo.
    *   **Extensibilidade:** A utilização de padrões como State e Strategy permite que novas regras de negócio, estados ou comportamentos sejam adicionados com mínimo impacto no código existente.
*   **Design Patterns:** Foram aplicados padrões de projeto como State e Strategy para resolver problemas específicos de forma elegante e comprovada, contribuindo para a clareza e organização do código.
*   **Princípios de Design:**
    *   **Clean Architecture (Conceitos):** A separação em camadas (domínio, aplicação/serviço, infraestrutura) foi seguida para isolar as regras de negócio da tecnologia e frameworks.
    *   **Clean Code:** Buscou-se escrever um código limpo, legível e de fácil manutenção, seguindo convenções e boas práticas.
    *   **SOLID:** Os princípios SOLID foram considerados no design das classes e interfaces para criar um software mais flexível e robusto.
*   **Documentação:** Este `README.md` detalha as decisões de design, premissas e instruções de uso, visando facilitar a compreensão e colaboração.
*   **Observabilidade (Fundação):** A aplicação já conta com um sistema de logging detalhado (`Slf4j` com `Logback`) para rastrear o fluxo de execução e auxiliar na identificação de problemas.


### Evoluções Futuras (Observabilidade e Refinamentos):

Embora a fundação para observabilidade com logging esteja presente, um próximo passo natural seria enriquecer esta capacidade com ferramentas dedicadas e realizar refinamentos gerais. A intenção é integrar soluções como:

*   **Métricas com Prometheus:** Para coletar métricas de desempenho da aplicação, taxas de erro, latência de endpoints, e saúde dos componentes.
*   **Visualização com Grafana:** Para criar dashboards interativos que permitam monitorar as métricas coletadas pelo Prometheus em tempo real, facilitando a identificação de tendências e anomalias.
*   **Tracing Distribuído (ex: OpenTelemetry, Jaeger/Zipkin):** Para rastrear requisições através dos diferentes componentes e serviços (mesmo os simulados), o que seria crucial em um ambiente de microsserviços real para entender o fluxo completo e identificar gargalos.
*   **Refinamento de Configurações:** Externalizar completamente valores configuráveis (ex: para constantes, variáveis de ambiente) para melhorar a flexibilidade e segurança.
*   **Expansão da Cobertura de Testes:** Incrementar a suíte de testes para abranger mais cenários e alcançar uma cobertura ainda maior.

A implementação dessas ferramentas e refinamentos traria uma visão muito mais completa e proativa sobre o comportamento e a saúde da aplicação em produção, além de otimizar a manutenibilidade.

### Testabilidade:

Foram implementados testes unitários e de integração focados nas funcionalidades centrais do serviço, estabelecendo uma base sólida para a validação da aplicação. Dado o escopo e tempo, a prioridade foi validar os fluxos críticos. O objetivo contínuo é expandir essa cobertura para garantir a máxima confiabilidade e facilitar futuras manutenções, visando manter um alto padrão de qualidade.m implementados testes unitários e de integração focados nas funcionalidades centrais do serviço, estabelecendo uma base sólida para a validação da aplicação. Dado o escopo e tempo, a prioridade foi validar os fluxos críticos. O objetivo contínuo é expandir essa cobertura para garantir a máxima confiabilidade e facilitar futuras manutenções, visando manter um alto padrão de qualidade.am criados os principais testes unitários e de integração para validar as funcionalidades centrais do serviço, com o objetivo de manter uma cobertura de testes superior a 90% para garantir a confiabilidade e facilitar futuras manutenções.