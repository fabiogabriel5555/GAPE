package pt.isel.gape.common.validation;

import java.util.Locale;
import java.util.Objects;

public final class PortugueseDocumentNumberValidator {

    private PortugueseDocumentNumberValidator() {
    }

    public static String normalize(String documentType, String documentNumber) {
        Objects.requireNonNull(documentType, "documentType is required");
        Objects.requireNonNull(documentNumber, "documentNumber is required");

        String normalizedType = documentType.trim().toUpperCase(Locale.ROOT);
        String rawNumber = documentNumber.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "CITIZEN_CARD" -> normalizeCitizenCard(rawNumber);
            case "TAX_IDENTIFICATION_NUMBER" -> normalizeTaxIdentificationNumber(rawNumber);
            case "PASSPORT" -> normalizePassport(rawNumber);
            case "RESIDENCE_PERMIT" -> normalizeResidencePermit(rawNumber);
            default -> rawNumber;
        };
    }

    private static String normalizeCitizenCard(String rawNumber) {
        String value = compact(rawNumber);
        if (!value.matches("[0-9]{9}[A-Z0-9]{2}[0-9]") || !validCitizenCard(value)) {
            throw invalid("CITIZEN_CARD", rawNumber);
        }
        return value;
    }

    private static String normalizeTaxIdentificationNumber(String rawNumber) {
        String value = compact(rawNumber);
        if (!value.matches("[0-9]{9}") || !validTaxIdentificationPrefix(value) || !validTaxIdentificationNumber(value)) {
            throw invalid("TAX_IDENTIFICATION_NUMBER", rawNumber);
        }
        return value;
    }

    private static String normalizePassport(String rawNumber) {
        String value = rawNumber.trim();
        if (!value.matches("[A-Z0-9]{6,9}")) {
            throw invalid("PASSPORT", rawNumber);
        }
        return value;
    }

    private static String normalizeResidencePermit(String rawNumber) {
        String value = compact(rawNumber);
        if (!value.matches("[A-Z0-9]{6,12}")) {
            throw invalid("RESIDENCE_PERMIT", rawNumber);
        }
        return value;
    }

    private static boolean validCitizenCard(String value) {
        int sum = 0;
        boolean secondDigit = false;
        for (int index = value.length() - 1; index >= 0; index--) {
            int digitValue = citizenCardValue(value.charAt(index));
            if (digitValue < 0) {
                return false;
            }
            if (secondDigit) {
                digitValue *= 2;
                if (digitValue > 9) {
                    digitValue -= 9;
                }
            }
            sum += digitValue;
            secondDigit = !secondDigit;
        }
        return sum % 10 == 0;
    }

    private static int citizenCardValue(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'Z') {
            return value - 'A' + 10;
        }
        return -1;
    }

    private static boolean validTaxIdentificationNumber(String value) {
        int sum = 0;
        for (int index = 0; index < 8; index++) {
            sum += (value.charAt(index) - '0') * (9 - index);
        }
        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? 0 : 11 - remainder;
        return checkDigit == value.charAt(8) - '0';
    }

    private static boolean validTaxIdentificationPrefix(String value) {
        char firstDigit = value.charAt(0);
        return "12356789".indexOf(firstDigit) >= 0 || value.startsWith("45");
    }

    private static String compact(String value) {
        return value.replaceAll("[\\s-]", "");
    }

    private static IllegalArgumentException invalid(String documentType, String documentNumber) {
        return new IllegalArgumentException("Invalid document number for " + documentType + ": " + documentNumber);
    }
}
