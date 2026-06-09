package pt.isel.gape.common.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PortugueseDocumentNumberValidatorTest {

    @Test
    void normalizesAndValidatesCitizenCardNumber() {
        assertEquals(
                "000000000ZZ4",
                PortugueseDocumentNumberValidator.normalize("CITIZEN_CARD", "00000000 0 ZZ4")
        );
    }

    @Test
    void normalizesAndValidatesTaxIdentificationNumber() {
        assertEquals(
                "503504564",
                PortugueseDocumentNumberValidator.normalize("TAX_IDENTIFICATION_NUMBER", "503 504 564")
        );
    }

    @Test
    void normalizesAndValidatesPassportNumber() {
        assertEquals(
                "PA123456",
                PortugueseDocumentNumberValidator.normalize("PASSPORT", "pa123456")
        );
    }

    @Test
    void normalizesAndValidatesResidencePermitNumber() {
        assertEquals(
                "123456",
                PortugueseDocumentNumberValidator.normalize("RESIDENCE_PERMIT", "123 456")
        );
    }

    @Test
    void rejectsInvalidDocumentNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PortugueseDocumentNumberValidator.normalize("PASSPORT", "PA123!")
        );
    }
}
