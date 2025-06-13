package br.com.itau.secure.domain.service.customer;

public enum CustomerRiskProfile {
    REGULAR("regular"),
    HIGH_RISK("alto risco"),
    PREFERRED("preferencial"),
    NO_INFORMATION("sem Informação"),
    UNKNOWN("desconhecido");

    private final String description;

    CustomerRiskProfile(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static CustomerRiskProfile fromString(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        for (CustomerRiskProfile profile : CustomerRiskProfile.values()) {
            if (profile.name().equalsIgnoreCase(text) || profile.description.equalsIgnoreCase(text)) {
                return profile;
            }
        }
        return UNKNOWN;
    }
}
