package br.com.itau.secure.domain.service;

import br.com.itau.secure.api.model.SecureOrderResponse;
import br.com.itau.secure.domain.exception.ResourceNotFoundException;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.repository.SecureOrderRepository;
// A importação de SecureOrderStatus deve ser a classe/enum real, não o mock.
// Se SecureOrderStatus é um enum ou uma classe concreta que implementa uma interface Status,
// você mockaria a interface/classe base se necessário, ou usaria instâncias reais do enum.
import br.com.itau.secure.domain.service.status.SecureOrderStatus;
import br.com.itau.secure.domain.service.status.SecureOrderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString; // Não utilizado diretamente
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureOrderServiceTest {

    @Mock
    private SecureOrderRepository secureOrderRepository;

    @Mock
    private SecureOrderStatusService secureOrderStatusService;

    @Mock
    private SecureOrderStatus mockOrderStatus; // Este mock é para o campo 'status' de SecureOrder

    @InjectMocks
    private SecureOrderService secureOrderService;

    private SecureOrder sampleOrder;
    private String orderId;
    private String customerId;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        customerId = UUID.randomUUID().toString();
        now = OffsetDateTime.now();

        // Usando Lombok @Builder para SecureOrder
        // Certifique-se que a classe SecureOrder tem @Builder
        sampleOrder = SecureOrder.builder()
                .customerId(customerId)
                .insuredAmount(new BigDecimal("1000.00"))
                .build();
        sampleOrder.setStatus(mockOrderStatus,null);
    }

    @Test
    void findById_whenOrderExists_shouldReturnOrder() {
        when(secureOrderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));

        SecureOrder foundOrder = secureOrderService.findById(orderId);

        assertNotNull(foundOrder);
        verify(secureOrderRepository).findById(orderId);
    }

    @Test
    void findById_whenOrderDoesNotExist_shouldThrowResourceNotFoundException() {
        when(secureOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            secureOrderService.findById(orderId);
        });

       verify(secureOrderRepository).findById(orderId);
    }

    @Test
    void findByCustomerId_whenOrdersExist_shouldReturnListOfSecureOrderResponse() {
        // sampleOrder agora tem um ID, então SecureOrderResponse.fromEntity funcionará como esperado
        when(secureOrderRepository.findByCustomerId(customerId)).thenReturn(Collections.singletonList(sampleOrder));

        List<SecureOrderResponse> responses = secureOrderService.findByCustomerId(customerId);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        verify(secureOrderRepository).findByCustomerId(customerId);
    }

    @Test
    void findByCustomerId_whenNoOrdersExist_shouldReturnEmptyList() {
        when(secureOrderRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        List<SecureOrderResponse> responses = secureOrderService.findByCustomerId(customerId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(secureOrderRepository).findByCustomerId(customerId);
    }


    @Test
    void createUpdateSecureOrder_shouldSaveOrderAndSendStatusUpdate() {
        SecureOrder orderToSave = SecureOrder.builder()
                .customerId("newCustomer")
                .build();

        // Garanta que sampleOrder tenha um ID no setUp para a asserção de ID funcionar
        // sampleOrder = SecureOrder.builder().id(orderId) ... .build();

        when(secureOrderRepository.save(any(SecureOrder.class))).thenReturn(sampleOrder);
        doNothing().when(secureOrderStatusService).sendStatusUpdateToQueueProcessing(any(SecureOrder.class), isNull());

        SecureOrder savedOrder = secureOrderService.createUpdateSecureOrder(orderToSave);

        assertNotNull(savedOrder);


        verify(secureOrderRepository).save(eq(orderToSave)); // Verifique com o objeto que foi passado para save
        // Verifique que o método foi chamado com o objeto que foi retornado por save (que é 'savedOrder' ou 'sampleOrder')
        verify(secureOrderStatusService).sendStatusUpdateToQueueProcessing(any(), isNull());
       }

    @Test
    void cancelOrder_whenOrderExists_shouldCancelAndUpdateOrder() {
        when(secureOrderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(mockOrderStatus).moveToCancel(sampleOrder);
        when(secureOrderRepository.save(sampleOrder)).thenReturn(sampleOrder);
        doNothing().when(secureOrderStatusService).sendStatusUpdateToQueueProcessing(sampleOrder, null);

        SecureOrder cancelledOrder = secureOrderService.cancelOrder(orderId);

        assertNotNull(cancelledOrder);
        verify(secureOrderRepository).findById(orderId);
        verify(mockOrderStatus).moveToCancel(sampleOrder);
        verify(secureOrderRepository).save(sampleOrder);
        verify(secureOrderStatusService).sendStatusUpdateToQueueProcessing(sampleOrder, null);
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_shouldThrowResourceNotFoundException() {
        when(secureOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            secureOrderService.cancelOrder(orderId);
        });

        verify(secureOrderRepository).findById(orderId);
        verify(mockOrderStatus, never()).moveToCancel(any(SecureOrder.class));
        verify(secureOrderRepository, never()).save(any(SecureOrder.class));
        verify(secureOrderStatusService, never()).sendStatusUpdateToQueueProcessing(any(SecureOrder.class), any());
    }
}