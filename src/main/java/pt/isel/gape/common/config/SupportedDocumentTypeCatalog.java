package pt.isel.gape.common.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class SupportedDocumentTypeCatalog {

    private static final String RESOURCE = "config/xml/document-types.xml";
    private static final SupportedDocumentTypeCatalog INSTANCE = load();

    private final List<ControlledValue> documentTypes;

    private SupportedDocumentTypeCatalog(List<ControlledValue> documentTypes) {
        this.documentTypes = List.copyOf(documentTypes);
        if (this.documentTypes.isEmpty()) {
            throw new IllegalStateException("Document type catalog cannot be empty");
        }
    }

    public static List<ControlledValue> all() {
        return INSTANCE.documentTypes;
    }

    public static boolean contains(String code) {
        return code != null && INSTANCE.documentTypes.stream().anyMatch(documentType -> documentType.getCode().equals(code));
    }

    public static String labelFor(String code) {
        return Optional.ofNullable(code)
                .flatMap(value -> INSTANCE.documentTypes.stream()
                        .filter(documentType -> documentType.getCode().equals(value))
                        .findFirst())
                .map(ControlledValue::getLabel)
                .orElse(code);
    }

    private static SupportedDocumentTypeCatalog load() {
        try (InputStream input = SupportedDocumentTypeCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing document type catalog: " + RESOURCE);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            Element root = factory.newDocumentBuilder().parse(input).getDocumentElement();
            NodeList nodes = root.getElementsByTagName("documentType");
            List<ControlledValue> documentTypes = new ArrayList<>();
            for (int index = 0; index < nodes.getLength(); index++) {
                Element documentType = (Element) nodes.item(index);
                String code = documentType.getAttribute("code");
                String label = documentType.getElementsByTagName("label").item(0).getTextContent().trim();
                documentTypes.add(new ControlledValue(code, label));
            }
            return new SupportedDocumentTypeCatalog(documentTypes);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load supported document types", exception);
        }
    }
}
