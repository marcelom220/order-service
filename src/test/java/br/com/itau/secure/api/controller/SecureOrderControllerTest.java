package br.com.itau.secure.api.controller;

import br.com.itau.secure.api.model.SecureOrderInput;
import br.com.itau.secure.api.model.SecureOrderResponse;

import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.SecureOrderService;
import br.com.itau.secure.domain.service.status.InsuranceCategory;
import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecureOrderController.class)
class SecureOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecureOrderService secureOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    private SecureOrder sampleOrder;
    private SecureOrderInput sampleOrderInput;
    private SecureOrderResponse sampleOrderResponse;
    private String orderId;
    private String customerId;




    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        customerId = UUID.randomUUID().toString();

        // Usando Lombok @Builder para SecureOrder
        // Certifique-se que a classe SecureOrder tem @Builder
        sampleOrder = SecureOrder.builder()
                .customerId(customerId)
                .category(InsuranceCategory.AUTO.toString())
                .insuredAmount(new BigDecimal("50000.00"))
                .build();


        sampleOrderInput = SecureOrderInput.builder()
                .customerId(customerId)
                .category(InsuranceCategory.AUTO.name()) // Input geralmente espera String para enums
                .insuredAmount(new BigDecimal("50000.00"))
                // Adicione outros campos conforme necessário para o input
                .build();

        sampleOrderResponse = SecureOrderResponse.fromEntity(sampleOrder);
    }

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        when(secureOrderService.createUpdateSecureOrder(any(SecureOrder.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/secure-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleOrderInput)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.category").value(InsuranceCategory.AUTO.name()));
    }

    @Test
    void getOrderById_shouldReturnOrder_whenOrderExists() throws Exception {
        when(secureOrderService.findById(orderId)).thenReturn(sampleOrder);

        mockMvc.perform(get("/secure-orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customerId));
    }

    @Test
    void findOrders_shouldReturnListOfOrders_forGivenCustomerId() throws Exception {
        List<SecureOrderResponse> ordersFromService;
        ordersFromService =  Collections.singletonList(sampleOrder).stream().map(SecureOrderResponse::fromEntity)
                .toList();
        when(secureOrderService.findByCustomerId(customerId)).thenReturn(ordersFromService);

        mockMvc.perform(get("/secure-orders")
                        .param("customerId", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].customerId").value(customerId));
    }

    @Test
    void findOrders_shouldReturnEmptyList_whenNoOrdersForCustomerId() throws Exception {
        when(secureOrderService.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/secure-orders")
                        .param("customerId", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void cancelOrder_shouldReturnCancelledOrder() throws Exception {
        // Usando Lombok @Builder para o objeto de retorno esperado
        SecureOrder cancelledOrderEntity = SecureOrder.builder()
                .customerId(customerId)
                .category(InsuranceCategory.AUTO.toString())
                .insuredAmount(new BigDecimal("50000.00"))
                .build();

        when(secureOrderService.cancelOrder(orderId)).thenReturn(cancelledOrderEntity);

        mockMvc.perform(patch("/secure-orders/{id}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}