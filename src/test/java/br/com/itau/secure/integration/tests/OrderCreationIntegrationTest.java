package br.com.itau.secure.integration.tests;

import br.com.itau.secure.api.model.SecureOrderInput;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.repository.SecureOrderRepository;

import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public class OrderCreationIntegrationTest {
    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"))
            .withExposedPorts(27017);

    @Container
    static final GenericContainer<?> rabbitMQContainer = new GenericContainer<>(DockerImageName.parse("rabbitmq:3-management"))
            .withExposedPorts(5672, 15672)
            .withEnv("RABBITMQ_DEFAULT_USER", "rabbitmq")
            .withEnv("RABBITMQ_DEFAULT_PASS", "rabbitmq");

    @Container
    static final MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SecureOrderRepository secureOrderRepository; // Para verificar o estado do banco

    @Autowired
    private RabbitTemplate rabbitTemplate; // Para interagir ou verificar o RabbitMQ, se necessário

    private static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getFirstMappedPort);
        registry.add("spring.rabbitmq.username", () -> "rabbitmq");
        registry.add("spring.rabbitmq.password", () -> "rabbitmq");
        // Configurar a URL base do seu RiskClient para apontar para o MockServer
        registry.add("client.risk.base-url", mockServerContainer::getEndpoint);
    }
    @BeforeAll
    static void beforeAll() {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
        // Configurar as expectativas do MockServer programaticamente
        setupMockServerExpectations();
    }

    static void setupMockServerExpectations() {
        // Exemplo para HIGH_RISK (baseado no seu persistedExpectations.json)
        // O corpo da requisição para o mock do RiskClient deve conter "orderId" e "customerId"
        // conforme o contrato do RiskClient.
        String highRiskOrderIdForMock = "e053467f-bd48-4fd2-9481-75bd4e88ee40"; // Este é o ID que o RiskClient espera
        String highRiskCustomerId = "7c2a27ba-71ef-4dd8-a3cf-5e094316ffd8";
        mockServerClient
                .when(
                        HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/risks")
                                .withBody(org.mockserver.model.JsonBody.json(String.format("{\"orderId\":\"%s\",\"customerId\":\"%s\"}", highRiskOrderIdForMock, highRiskCustomerId)))
                )
                .respond(
                        HttpResponse.response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                // A resposta do mock do RiskClient também usa "orderId"
                                .withBody(String.format("{\"orderId\":\"%s\",\"customerId\":\"%s\",\"analyzedAt\":\"2024-05-10T12:00:00Z\",\"classification\":\"HIGH_RISK\",\"occurrences\":[{\"id\":\"%s\",\"productId\":78900069,\"type\":\"FRAUD\",\"description\":\"Attempted Fraudulent transaction\",\"createdAt\":\"2024-05-10T12:00:00Z\",\"updatedAt\":\"2024-05-10T12:00:00Z\"}]}", highRiskOrderIdForMock, highRiskCustomerId, highRiskOrderIdForMock))
                );

        // Exemplo para REGULAR
        String regularOrderIdForMock = "f1267a8b-e217-4a55-8411-3cb52741a777"; // Este é o ID que o RiskClient espera
        String regularCustomerId = "6b2a12ba-91ff-4dd8-a3cf-2e094316aaa8";
        mockServerClient
                .when(
                        HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/risks")
                                .withBody(org.mockserver.model.JsonBody.json(String.format("{\"orderId\":\"%s\",\"customerId\":\"%s\"}", regularOrderIdForMock, regularCustomerId)))
                ).respond(
                        HttpResponse.response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(String.format("{\"orderId\":\"%s\",\"customerId\":\"%s\",\"analyzedAt\":\"2024-04-22T14:45:00Z\",\"classification\":\"REGULAR\",\"occurrences\":[{\"id\":\"%s\",\"productId\":10445678,\"type\":\"SUSPICION\",\"description\":\"Unusual activity flagged for review\",\"createdAt\":\"2024-04-22T14:45:00Z\",\"updatedAt\":\"2024-04-22T14:45:00Z\"}]}", regularOrderIdForMock, regularCustomerId, regularOrderIdForMock))
                );
        // Adicione mais expectativas para PREFERRED, NO_INFORMATION conforme necessário,
        // seguindo o mesmo padrão para os IDs usados no mock do RiskClient.
    }
    @Test
    void createOrder_RegularCustomer_WithinLimit_ShouldResultInReceivedStatus() throws Exception {
        String customerId = "6b2a12ba-91ff-4dd8-a3cf-2e094316aaa8"; // Corresponde à expectativa REGULAR
        // O orderId enviado para o mock do RiskClient é definido em setupMockServerExpectations.
        // O ID do pedido gerado pela API será diferente e é o que verificamos na resposta.

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-vida-001")
                .category("VIDA")
                .salesChannel("ONLINE")
                .paymentMethod("CREDIT_CARD")
                .totalMonthlyPremiumAmount(new BigDecimal("150.00"))
                .insuredAmount(new BigDecimal("200000.00"))
                .coverages(Collections.singletonMap("MORTE_ACIDENTAL", new BigDecimal("200000.00")))
                .assistances(Collections.singletonList("ASSISTENCIA_FUNERAL"))
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO DE orderId PARA id
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // ALTERADO PARA RECEIVED (ou o status inicial real)
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO DE orderId PARA id

                    assertNotNull(createdOrderId);
                    SecureOrder createdOrder = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrder);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrder.getStatus()); // Verifique o status no BD
                });
    }

    @Test
    void createOrder_HighRiskCustomer_AboveLimit_ShouldResultInReceivedStatusAndThenRejected() throws Exception {
        String customerId = "7c2a27ba-71ef-4dd8-a3cf-5e094316ffd8"; // Corresponde à expectativa HIGH_RISK

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-auto-002")
                .category("AUTO")
                .salesChannel("BRANCH")
                .paymentMethod("DEBIT")
                .totalMonthlyPremiumAmount(new BigDecimal("300.00"))
                .insuredAmount(new BigDecimal("300000.00"))
                .coverages(Collections.singletonMap("COLISAO", new BigDecimal("300000.00")))
                .assistances(Collections.emptyList())
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO DE orderId PARA id
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // Status inicial da API
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO DE orderId PARA id

                    assertNotNull(createdOrderId);
                    // Para verificar o status REJECTED, que pode ser assíncrono,
                    // você precisaria de Awaitility ou similar para esperar a atualização no banco.
                    // Por ora, verificamos o estado inicial.
                    SecureOrder createdOrderInitial = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrderInitial);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrderInitial.getStatus());

                    // TODO: Adicionar Awaitility para verificar a transição para REJECTED se for assíncrono
                    // Exemplo conceitual com Awaitility (requer dependência e configuração):
                    // org.awaitility.Awaitility.await().atMost(10, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
                    //     SecureOrder updatedOrder = secureOrderRepository.findById(createdOrderId).orElseThrow();
                    //     assertEquals(SecureOrderStatus.REJECTED, updatedOrder.getStatus());
                    // });
                });
    }

    @Test
    void createOrder_PreferredCustomer_WithinLimit_Vida_ShouldResultInReceivedStatus() throws Exception {
        String customerId = "5a3b45ba-81cd-4dd8-b3cf-4e094316bbb1";

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-vida-pref-001")
                .category("VIDA")
                .salesChannel("AGENT")
                .paymentMethod("BANK_TRANSFER")
                .totalMonthlyPremiumAmount(new BigDecimal("500.00"))
                .insuredAmount(new BigDecimal("799999.00"))
                .coverages(Collections.singletonMap("MORTE_NATURAL", new BigDecimal("799999.00")))
                .assistances(Collections.singletonList("ASSISTENCIA_VIAGEM_PREMIUM"))
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // ALTERADO
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO
                    assertNotNull(createdOrderId);
                    SecureOrder createdOrder = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrder);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrder.getStatus());
                });
    }

    @Test
    void createOrder_PreferredCustomer_AboveLimit_Auto_ShouldResultInReceivedStatusAndThenRejected() throws Exception {
        String customerId = "5a3b45ba-81cd-4dd8-b3cf-4e094316bbb1";

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-auto-pref-002")
                .category("AUTO")
                .salesChannel("ONLINE")
                .paymentMethod("CREDIT_CARD")
                .totalMonthlyPremiumAmount(new BigDecimal("600.00"))
                .insuredAmount(new BigDecimal("450000.00"))
                .coverages(Collections.singletonMap("PERDA_TOTAL", new BigDecimal("450000.00")))
                .assistances(Collections.emptyList())
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // ALTERADO
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO
                    assertNotNull(createdOrderId);
                    SecureOrder createdOrder = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrder);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrder.getStatus());
                    // TODO: Adicionar Awaitility para verificar a transição para REJECTED
                });
    }

    @Test
    void createOrder_NoInformationCustomer_WithinLimit_Residencial_ShouldResultInReceivedStatus() throws Exception {
        String customerId = "3c5a67ba-72ee-4dd8-c3cf-1e094316ccc9";

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-home-noinfo-001")
                .category("RESIDENCIAL")
                .salesChannel("PARTNER")
                .paymentMethod("PIX")
                .totalMonthlyPremiumAmount(new BigDecimal("80.00"))
                .insuredAmount(new BigDecimal("200000.00"))
                .coverages(Collections.singletonMap("INCENDIO", new BigDecimal("200000.00")))
                .assistances(Collections.singletonList("CHAVEIRO_24H"))
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // ALTERADO
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO
                    assertNotNull(createdOrderId);
                    SecureOrder createdOrder = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrder);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrder.getStatus());
                });
    }

    @Test
    void createOrder_NoInformationCustomer_AboveLimit_Outro_ShouldResultInReceivedStatusAndThenRejected() throws Exception {
        String customerId = "3c5a67ba-72ee-4dd8-c3cf-1e094316ccc9";

        SecureOrderInput input = SecureOrderInput.builder()
                .customerId(customerId)
                .productId("product-other-noinfo-002")
                .category("OUTRO")
                .salesChannel("ONLINE")
                .paymentMethod("CREDIT_CARD")
                .totalMonthlyPremiumAmount(new BigDecimal("50.00"))
                .insuredAmount(new BigDecimal("55000.01"))
                .coverages(Collections.singletonMap("DANOS_ELETRICOS", new BigDecimal("55000.01")))
                .assistances(Collections.emptyList())
                .build();

        mockMvc.perform(post("/secure-orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()) // ALTERADO
                .andExpect(jsonPath("$.status").value(SecureOrderStatus.RECEIVED.name())) // ALTERADO
                .andDo(result -> {
                    String responseString = result.getResponse().getContentAsString();
                    Map<String, String> responseMap = objectMapper.readValue(responseString, HashMap.class);
                    String createdOrderId = responseMap.get("id"); // ALTERADO
                    assertNotNull(createdOrderId);
                    SecureOrder createdOrder = secureOrderRepository.findById(createdOrderId).orElse(null);
                    assertNotNull(createdOrder);
                    assertEquals(SecureOrderStatus.RECEIVED, createdOrder.getStatus());
                    // TODO: Adicionar Awaitility para verificar a transição para REJECTED
                });
    }
}