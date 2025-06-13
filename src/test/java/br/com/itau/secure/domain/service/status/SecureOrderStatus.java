package br.com.itau.secure.domain.service.status;

import br.com.itau.secure.api.model.FraudCheckResult;
import br.com.itau.secure.api.model.OccurrenceDTO;
import br.com.itau.secure.api.model.PaymentConfirmation;
import br.com.itau.secure.api.model.SubscriptionAuthorization;
import br.com.itau.secure.domain.model.SecureOrder;
import br.com.itau.secure.domain.service.customer.CustomerRiskProfile; // Importe seu enum
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureOrderStatusTest {

    @Mock
    SecureOrder mockSecureOrder;
    @Mock
    FraudCheckResult mockFraudCheckResult;
    @Mock
    PaymentConfirmation mockPaymentConfirmation;
    @Mock
    SubscriptionAuthorization mockSubscriptionAuthorization;

    OccurrenceDTO occurrenceForTest;

    @BeforeEach
    void setUp() {
        occurrenceForTest = new OccurrenceDTO(
                "e053467f-bd48-4fd2-9481-75bd4e88ee40",
                78900069L,
                "FRAUD",
                "Attempted Fraudulent transaction",
                Instant.parse("2024-05-10T12:00:00Z"),
                Instant.parse("2024-05-10T12:00:00Z")
        );
        List<OccurrenceDTO> exampleOccurrences = List.of(occurrenceForTest);

        lenient().when(mockFraudCheckResult.orderId()).thenReturn("e053467f-bd48-4fd2-9481-75bd4e88ee40");
        lenient().when(mockFraudCheckResult.customerId()).thenReturn("7c2a27ba-71ef-4dd8-a3cf-5e094316ffd8");
        lenient().when(mockFraudCheckResult.analyzedAt()).thenReturn(Instant.parse("2024-05-10T12:00:00Z"));
        // Default: Fraud check classification é um perfil de cliente válido para permitir que as regras sejam testadas.
        // Isso será sobrescrito em testes específicos que precisam de uma classificação de fraude diferente.
        lenient().when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.REGULAR.name());
        lenient().when(mockFraudCheckResult.occurrences()).thenReturn(exampleOccurrences);

        lenient().when(mockSecureOrder.getId()).thenReturn("mockOrderId");
        lenient().when(mockSecureOrder.getInsuredAmount()).thenReturn(BigDecimal.ZERO);
        // mockSecureOrder.getCategory() é usado pelas CustomerTypeValidationRule para determinar o TIPO DE SEGURO.
        // O PERFIL DE RISCO DO CLIENTE para a ValidatedStateStrategy virá de mockFraudCheckResult.classification().
        lenient().when(mockSecureOrder.getCategory()).thenReturn("OUTRO"); // Default para tipo de seguro
    }

    @Nested
    class ReceivedStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.RECEIVED;

        @Test
        void moveToValidate_whenFraudClassificationAllowsValidation_shouldSetStatusToValidated() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.PREFERRED.name());
            state.moveToValidate(mockSecureOrder, mockFraudCheckResult);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.VALIDATED), any(Instant.class));
        }

        @Test
        void moveToValidate_whenFraudClassificationIsProblematicButStrategyMovesToValidated_shouldSetStatusToValidated() {
            // Se a ReceivedStateStrategy SEMPRE move para VALIDATED,
            // a rejeição ocorrerá na ValidatedStateStrategy.
            // "BLOCK_IMMEDIATELY" não é um CustomerRiskProfile, então levaria a UNKNOWN se usado diretamente.
            // Para este teste, vamos usar um perfil de alto risco que ainda passa para validação.
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.HIGH_RISK.name());
            state.moveToValidate(mockSecureOrder, mockFraudCheckResult);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.VALIDATED), any(Instant.class));
        }
        // ... outros testes do ReceivedStateTest permanecem os mesmos ...
        @Test
        void moveToPending_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToPending(mockFraudCheckResult, mockSecureOrder)
            );
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToApprove_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization)
            );
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToReject_shouldSetStatusToRejected() {
            state.moveToReject(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        @Test
        void moveToCancel_shouldSetStatusToCancelled() {
            state.moveToCancel(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.CANCELLED), any(Instant.class));
        }
    }

    @Nested
    class ValidatedStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.VALIDATED;

        @Test
        void moveToPending_whenFraudClassificationItselfIsUnknown_shouldSetStatusToRejected() {
            // Testa se a ValidatedStateStrategy rejeita se a CLASSIFICAÇÃO DA FRAUDE for algo
            // que resulta em CustomerRiskProfile.UNKNOWN.
            when(mockFraudCheckResult.classification()).thenReturn("INVALID_PROFILE_FOR_TEST"); // Isso resultará em UNKNOWN

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        // --- Testes para Cliente Regular ---
        @Test
        void moveToPending_RegularCustomer_OutroSeguro_AbaixoLimite_ShouldSetStatusToPending() {
            // FraudCheckResult indica que o cliente é REGULAR
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.REGULAR.name());
            // SecureOrder tem o tipo de seguro e valor
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO"); // Tipo de seguro
            when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("255000.00")); // Limite

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.PENDING), any(Instant.class));
        }

        @Test
        void moveToPending_RegularCustomer_OutroSeguro_AcimaLimite_ShouldSetStatusToRejected() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.REGULAR.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            lenient().when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("255000.01")); // Acima

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        // --- Testes para Cliente Alto Risco ---
        @Test
        void moveToPending_HighRiskCustomer_OutroSeguro_AbaixoLimite_ShouldSetStatusToPending() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.HIGH_RISK.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("125000.00")); // Limite

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.PENDING), any(Instant.class));
        }

        @Test
        void moveToPending_HighRiskCustomer_OutroSeguro_AcimaLimite_ShouldSetStatusToRejected() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.HIGH_RISK.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            lenient().when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("125000.01")); // Acima

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        // --- Testes para Cliente Preferencial ---
        @Test
        void moveToPending_PreferredCustomer_OutroSeguro_AbaixoLimite_ShouldSetStatusToPending() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.PREFERRED.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("375000.00")); // Limite

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.PENDING), any(Instant.class));
        }

        @Test
        void moveToPending_PreferredCustomer_OutroSeguro_AcimaLimite_ShouldSetStatusToRejected() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.PREFERRED.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            lenient().when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("375000.01")); // Acima

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        // --- Testes para Cliente Sem Informação ---
        @Test
        void moveToPending_NoInformationCustomer_OutroSeguro_AbaixoLimite_ShouldSetStatusToPending() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.NO_INFORMATION.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("55000.00")); // Limite

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.PENDING), any(Instant.class));
        }

        @Test
        void moveToPending_NoInformationCustomer_OutroSeguro_AcimaLimite_ShouldSetStatusToRejected() {
            when(mockFraudCheckResult.classification()).thenReturn(CustomerRiskProfile.NO_INFORMATION.name());
            when(mockSecureOrder.getCategory()).thenReturn("OUTRO");
            lenient().when(mockSecureOrder.getInsuredAmount()).thenReturn(new BigDecimal("55000.01")); // Acima

            state.moveToPending(mockFraudCheckResult, mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }
        // ... outros testes do ValidatedStateTest (moveToValidate, moveToApprove, etc.) permanecem os mesmos ...
        @Test
        void moveToValidate_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToValidate(mockSecureOrder, mockFraudCheckResult)
            );
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToApprove_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization)
            );
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToReject_shouldSetStatusToRejected() {
            state.moveToReject(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        @Test
        void moveToCancel_shouldSetStatusToCancelled() {
            state.moveToCancel(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.CANCELLED), any(Instant.class));
        }
    }

    // ... PendingStateTest, ApprovedStateTest, RejectedStateTest, CancelledStateTest permanecem os mesmos ...
    @Nested
    class PendingStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.PENDING;

        @Test
        void moveToApprove_whenPaymentAndSubscriptionOk_shouldSetStatusToApproved() {
            when(mockPaymentConfirmation.isConfirmed()).thenReturn(true);
            when(mockSubscriptionAuthorization.isAuthorized()).thenReturn(true);
            state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.APPROVED), any(Instant.class));
        }

        @Test
        void moveToApprove_whenPaymentNotOk_shouldSetStatusToRejected() {
            when(mockPaymentConfirmation.isConfirmed()).thenReturn(false);
            when(mockSubscriptionAuthorization.isAuthorized()).thenReturn(true);
            state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        @Test
        void moveToValidate_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToValidate(mockSecureOrder, mockFraudCheckResult)
            );
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToPending_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToPending(mockFraudCheckResult, mockSecureOrder)
            );
        }

        @Test
        void moveToReject_shouldSetStatusToRejected() {
            state.moveToReject(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.REJECTED), any(Instant.class));
        }

        @Test
        void moveToCancel_shouldSetStatusToCancelled() {
            state.moveToCancel(mockSecureOrder);
            verify(mockSecureOrder).setStatus(eq(SecureOrderStatus.CANCELLED), any(Instant.class));
        }
    }

    @Nested
    class ApprovedStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.APPROVED;

        @Test
        void moveToCancel_shouldThrowUnsupportedOperationExceptionWithSpecificMessage() {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToCancel(mockSecureOrder)
            );
            assertEquals("PolicyRequest is APPROVED and cannot be cancelled.", exception.getMessage());
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToValidate_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToValidate(mockSecureOrder, mockFraudCheckResult));
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToPending_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToPending(mockFraudCheckResult, mockSecureOrder));
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToApprove_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization));
            verifyNoInteractions(mockSecureOrder);
        }

        @Test
        void moveToReject_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () ->
                    state.moveToReject(mockSecureOrder));
            verifyNoInteractions(mockSecureOrder);
        }
    }

    @Nested
    class RejectedStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.REJECTED;

        @Test
        void allTransitions_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () -> state.moveToValidate(mockSecureOrder, mockFraudCheckResult));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToPending(mockFraudCheckResult, mockSecureOrder));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToReject(mockSecureOrder));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToCancel(mockSecureOrder));
            verifyNoInteractions(mockSecureOrder);
        }
    }

    @Nested
    class CancelledStateTest {
        private final SecureOrderStatus state = SecureOrderStatus.CANCELLED;

        @Test
        void allTransitions_shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () -> state.moveToValidate(mockSecureOrder, mockFraudCheckResult));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToPending(mockFraudCheckResult, mockSecureOrder));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToApprove(mockSecureOrder, mockPaymentConfirmation, mockSubscriptionAuthorization));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToReject(mockSecureOrder));
            assertThrows(UnsupportedOperationException.class, () -> state.moveToCancel(mockSecureOrder));
            verifyNoInteractions(mockSecureOrder);
        }
    }
}