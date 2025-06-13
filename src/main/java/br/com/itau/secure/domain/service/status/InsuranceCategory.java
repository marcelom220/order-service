package br.com.itau.secure.domain.service.status;

public enum InsuranceCategory {
    LIFE("VIDA"),
    HOME("RESIDENCIAL"),
    AUTO("AUTO"),
    OTHER("OUTRO"); // Categoria genérica para "qualquer outro tipo de seguro"

    private final String displayName;

    InsuranceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static InsuranceCategory fromString(String text) {
        if (text == null) {
            return OTHER;
        }

        for (InsuranceCategory category : InsuranceCategory.values()) {
            if (category.name().equalsIgnoreCase(text)) {
                return category;
            }
        }

        for (InsuranceCategory category : InsuranceCategory.values()) {
            if (category.displayName.equalsIgnoreCase(text)) {
                return category;
            }
        }

        return OTHER;
    }
}
