package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class XmlValidationTest {

    private static final Path CONFIG_DIR = Path.of("src/main/resources/config");
    private static final Path CONFIG_XML_DIR = CONFIG_DIR.resolve("xml");
    private static final Path CONFIG_XSD_DIR = CONFIG_DIR.resolve("xsd");
    private static final Path CONFIG_XSL_DIR = CONFIG_DIR.resolve("xsl");

    private static final Path GAPE_CONFIG_XSD = CONFIG_XSD_DIR.resolve("gape-config.xsd");
    private static final Path CALENDARIO_ACADEMICO_XSD =
            CONFIG_XSD_DIR.resolve("transversal/calendarioAcademico.xsd");
    private static final Path CALENDARIO_PLURIANUAL_XSD =
            CONFIG_XSD_DIR.resolve("transversal/calendarioPlurianual.xsd");

    @Test
    void schemasCompile() {
        for (Path xsdFile : schemaFiles()) {
            assertDoesNotThrow(
                    () -> loadSchema(xsdFile),
                    () -> "Expected valid XSD schema: " + xsdFile
            );
        }
    }

    @Test
    void allConfigXmlFilesValidateAgainstTheirSchemas() throws Exception {
        List<Path> xmlFiles = supportXmlFiles();

        assertFalse(xmlFiles.isEmpty(), "Expected at least one XML support file to validate");

        for (Path xmlFile : xmlFiles) {
            Schema schema = loadSchema(schemaFor(xmlFile));
            assertDoesNotThrow(
                    () -> validate(schema, xmlFile),
                    () -> "Expected valid XML support file: " + xmlFile
            );
        }
    }

    @Test
    void supportXslFilesAreWellFormedXml() throws Exception {
        List<Path> xslFiles = supportXslFiles();

        assertFalse(xslFiles.isEmpty(), "Expected at least one XSL support file to validate");

        for (Path xslFile : xslFiles) {
            assertDoesNotThrow(
                    () -> parseXml(xslFile),
                    () -> "Expected well-formed XSL support file: " + xslFile
            );
        }
    }

    @Test
    void xmlWithInvalidStructureFailsValidation() throws Exception {
        Schema schema = loadSchema(GAPE_CONFIG_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <userState>
                  <state code="active">
                    <label>Conta ativa</label>
                  </state>
                </userState>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <userStates>
                  <item code="active">
                    <label>Conta ativa</label>
                  </item>
                </userStates>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <userStates>
                  <state code="active">
                    <label>Conta ativa</label>
                  </state>
                  <state code="inactive">
                    <label>Conta inativa</label>
                  </state>
                </userStates>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    @Test
    void xmlWithDuplicateControlledValueFailsValidation() throws Exception {
        Schema schema = loadSchema(GAPE_CONFIG_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <userStates>
                  <state code="active">
                    <label>Conta ativa</label>
                  </state>
                  <state code="active">
                    <label>Conta ativa duplicada</label>
                  </state>
                  <state code="blocked">
                    <label>Conta bloqueada</label>
                  </state>
                </userStates>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <attendanceStates>
                  <status code="present">
                    <label>Presente</label>
                  </status>
                  <status code="present">
                    <label>Presente duplicado</label>
                  </status>
                  <status code="justified">
                    <label>Justificada</label>
                  </status>
                  <status code="late">
                    <label>Atraso</label>
                  </status>
                  <status code="partial">
                    <label>Parcial</label>
                  </status>
                  <state code="active">
                    <label>Ativa</label>
                  </state>
                  <state code="corrected">
                    <label>Corrigida</label>
                  </state>
                  <state code="cancelled">
                    <label>Cancelada</label>
                  </state>
                  <state code="archived">
                    <label>Arquivada</label>
                  </state>
                </attendanceStates>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    @Test
    void xmlWithoutRequiredFieldsFailsValidation() throws Exception {
        Schema schema = loadSchema(GAPE_CONFIG_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <sessionStates>
                  <state>
                    <label>Sessao ativa</label>
                  </state>
                  <state code="expired">
                    <label>Sessao expirada</label>
                  </state>
                  <state code="closed">
                    <label>Sessao terminada</label>
                  </state>
                </sessionStates>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <permissionStates>
                  <state code="active"/>
                  <state code="inactive">
                    <label>Inativa</label>
                  </state>
                </permissionStates>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    @Test
    void xmlWithValueOutsideAllowedPatternFailsValidation() throws Exception {
        Schema schema = loadSchema(GAPE_CONFIG_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <assessmentTypes>
                  <type code="questionnaire">
                    <label>Questionario</label>
                  </type>
                  <type code="quiz">
                    <label>Quiz</label>
                  </type>
                </assessmentTypes>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <classShifts>
                  <shift code="morning">
                    <label>Morning</label>
                  </shift>
                  <shift code="afternoon">
                    <label>Afternoon</label>
                  </shift>
                  <shift code="night">
                    <label>Night</label>
                  </shift>
                  <shift code="mixed">
                    <label>Mixed</label>
                  </shift>
                </classShifts>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <permissionStates>
                  <state code="active">
                    <label>A</label>
                  </state>
                  <state code="inactive">
                    <label>Inativa</label>
                  </state>
                </permissionStates>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    @Test
    void invalidAcademicCalendarXmlFailsValidation() throws Exception {
        Schema schema = loadSchema(CALENDARIO_ACADEMICO_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <calendarioAcademico anoInicial="2025" anoFinal="2026">
                  <verao inicio="2026-02-16" fim="2026-06-05">
                    <normal fim="2026-06-22" lancar="2026-06-29"/>
                    <recurso fim="2026-07-10" lancar="2026-07-17"/>
                    <pascoa inicio="2026-03-30" fim="2026-04-05"/>
                  </verao>
                  <inverno inicio="2025-09-15" fim="2026-01-10">
                    <normal fim="2026-01-24" lancar="2026-01-31"/>
                    <recurso fim="2026-02-07" lancar="2026-02-14"/>
                    <natal inicio="2025-12-22" fim="2026-01-02"/>
                  </inverno>
                </calendarioAcademico>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <calendarioAcademico anoFinal="2026">
                  <inverno inicio="2025-09-15" fim="2026-01-10">
                    <normal fim="2026-01-24" lancar="2026-01-31"/>
                    <recurso fim="2026-02-07" lancar="2026-02-14"/>
                    <natal inicio="2025-12-22" fim="2026-01-02"/>
                  </inverno>
                  <verao inicio="2026-02-16" fim="2026-06-05">
                    <normal fim="2026-06-22" lancar="2026-06-29"/>
                    <recurso fim="2026-07-10" lancar="2026-07-17"/>
                    <pascoa inicio="2026-03-30" fim="2026-04-05"/>
                  </verao>
                </calendarioAcademico>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <calendarioAcademico anoInicial="2025-2026" anoFinal="2026">
                  <inverno inicio="2025-09-15" fim="2026-01-10">
                    <normal fim="2026-01-24" lancar="2026-01-31"/>
                    <recurso fim="2026-02-07" lancar="2026-02-14"/>
                    <natal inicio="2025-12-22" fim="2026-01-02"/>
                  </inverno>
                  <verao inicio="2026-02-31" fim="2026-06-05">
                    <normal fim="2026-06-22" lancar="2026-06-29"/>
                    <recurso fim="2026-07-10" lancar="2026-07-17"/>
                    <pascoa inicio="2026-03-30" fim="2026-04-05"/>
                  </verao>
                </calendarioAcademico>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    @Test
    void invalidPlurianualCalendarXmlFailsValidation() throws Exception {
        Schema schema = loadSchema(CALENDARIO_PLURIANUAL_XSD);
        List<String> invalidXmlCases = List.of(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <calendario>
                  <ano valor="2026">
                    <mes id_mes="1" nome="Janeiro">
                      <dia numero="1" dia_semana="Quinta">
                        <util/>
                        <fim-de-semana/>
                      </dia>
                    </mes>
                  </ano>
                </calendario>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <calendario>
                  <ano>
                    <mes id_mes="13" nome="March">
                      <dia numero="32" dia_semana="Tuesday">
                        <feriado>Feriado Municipal</feriado>
                      </dia>
                    </mes>
                  </ano>
                </calendario>
                """
        );

        for (String invalidXml : invalidXmlCases) {
            assertInvalid(schema, invalidXml);
        }
    }

    private static List<Path> schemaFiles() {
        return List.of(GAPE_CONFIG_XSD, CALENDARIO_ACADEMICO_XSD, CALENDARIO_PLURIANUAL_XSD);
    }

    private static List<Path> supportXmlFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(CONFIG_XML_DIR)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static List<Path> supportXslFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(CONFIG_XSL_DIR)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xsl"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static Path schemaFor(Path xmlFile) {
        String fileName = xmlFile.getFileName().toString();
        if ("calendarioAcademico.xml".equals(fileName)) {
            return CALENDARIO_ACADEMICO_XSD;
        }
        if ("calendarioPlurianual.xml".equals(fileName)) {
            return CALENDARIO_PLURIANUAL_XSD;
        }
        return GAPE_CONFIG_XSD;
    }

    private static Schema loadSchema(Path xsdFile) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(xsdFile.toFile());
    }

    private static void validate(Schema schema, Path xmlFile) throws IOException, SAXException {
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(xmlFile.toFile()));
    }

    private static void parseXml(Path xmlFile) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.newDocumentBuilder().parse(xmlFile.toFile());
    }

    private static void assertInvalid(Schema schema, String xml) {
        Validator validator = schema.newValidator();
        assertThrows(SAXException.class, () -> validator.validate(new StreamSource(new StringReader(xml))));
    }
}
