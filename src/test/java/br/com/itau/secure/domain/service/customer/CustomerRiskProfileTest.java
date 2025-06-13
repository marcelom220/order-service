package br.com.itau.secure.domain.service.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerRiskProfileTest {

    @Test
    void getDescription_shouldReturnCorrectDescription() {
        assertEquals("regular", CustomerRiskProfile.REGULAR.getDescription());
        assertEquals("alto risco", CustomerRiskProfile.HIGH_RISK.getDescription());
        assertEquals("preferencial", CustomerRiskProfile.PREFERRED.getDescription());
        assertEquals("sem Informação", CustomerRiskProfile.NO_INFORMATION.getDescription());
        assertEquals("desconhecido", CustomerRiskProfile.UNKNOWN.getDescription());
    }

    @ParameterizedTest
    @CsvSource({
            "REGULAR, REGULAR",
            "regular, REGULAR",
            "HIGH_RISK, HIGH_RISK",
            "high_risk, HIGH_RISK",
            "PREFERRED, PREFERRED",
            "preferred, PREFERRED",
            "NO_INFORMATION, NO_INFORMATION",
            "no_information, NO_INFORMATION",
            "UNKNOWN, UNKNOWN",
            "unknown, UNKNOWN"
    })
    void fromString_whenMatchingByName_shouldReturnCorrectProfile(String inputText, CustomerRiskProfile expectedProfile) {
        assertEquals(expectedProfile, CustomerRiskProfile.fromString(inputText));
    }

    @ParameterizedTest
    @CsvSource({
            "regular, REGULAR",
            "Regular, REGULAR",
            "alto risco, HIGH_RISK",
            "Alto Risco, HIGH_RISK",
            "preferencial, PREFERRED",
            "Preferencial, PREFERRED",
            "sem Informação, NO_INFORMATION",
            "Sem Informação, NO_INFORMATION",
            "desconhecido, UNKNOWN",
            "Desconhecido, UNKNOWN"
    })
    void fromString_whenMatchingByDescription_shouldReturnCorrectProfile(String inputText, CustomerRiskProfile expectedProfile) {
        assertEquals(expectedProfile, CustomerRiskProfile.fromString(inputText));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void fromString_whenInputIsNullOrBlank_shouldReturnUnknown(String inputText) {
        // O método fromString atual retorna UNKNOWN para null, mas não trata strings vazias/em branco de forma especial,
        // elas cairão no caso de não correspondência e retornarão UNKNOWN, o que é aceitável.
        assertEquals(CustomerRiskProfile.UNKNOWN, CustomerRiskProfile.fromString(inputText));
    }

    @Test
    void fromString_whenInputIsCompletelyUnrelated_shouldReturnUnknown() {
        assertEquals(CustomerRiskProfile.UNKNOWN, CustomerRiskProfile.fromString("completely_unrelated_text"));
    }

    @Test
    void fromString_whenInputIsPartialMatchButNotExact_shouldReturnUnknown() {
        assertEquals(CustomerRiskProfile.UNKNOWN, CustomerRiskProfile.fromString("reg")); // Não é "regular"
        assertEquals(CustomerRiskProfile.UNKNOWN, CustomerRiskProfile.fromString("alto")); // Não é "alto risco"
    }
}