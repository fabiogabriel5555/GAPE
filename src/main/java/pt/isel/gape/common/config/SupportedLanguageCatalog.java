package pt.isel.gape.common.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class SupportedLanguageCatalog {

    private static final String RESOURCE = "config/xml/languages.xml";
    private static final SupportedLanguageCatalog INSTANCE = load();

    private final List<ControlledValue> languages;
    private final String defaultLanguageCode;

    private SupportedLanguageCatalog(List<ControlledValue> languages, String defaultLanguageCode) {
        this.languages = List.copyOf(languages);
        this.defaultLanguageCode = Objects.requireNonNull(defaultLanguageCode, "defaultLanguageCode is required");
        if (this.languages.stream().noneMatch(language -> language.getCode().equals(defaultLanguageCode))) {
            throw new IllegalStateException("Default language is not listed in " + RESOURCE);
        }
    }

    public static List<ControlledValue> all() {
        return INSTANCE.languages;
    }

    public static String defaultLanguageCode() {
        return INSTANCE.defaultLanguageCode;
    }

    public static boolean contains(String code) {
        return code != null && INSTANCE.languages.stream().anyMatch(language -> language.getCode().equals(code));
    }

    private static SupportedLanguageCatalog load() {
        try (InputStream input = SupportedLanguageCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing language catalog: " + RESOURCE);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            Element root = factory.newDocumentBuilder().parse(input).getDocumentElement();
            String defaultLanguageCode = root.getAttribute("default");
            NodeList nodes = root.getElementsByTagName("language");
            List<ControlledValue> languages = new ArrayList<>();
            for (int index = 0; index < nodes.getLength(); index++) {
                Element language = (Element) nodes.item(index);
                String code = language.getAttribute("code");
                String label = language.getElementsByTagName("label").item(0).getTextContent().trim();
                languages.add(new ControlledValue(code, label));
            }
            return new SupportedLanguageCatalog(languages, defaultLanguageCode);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load supported languages", exception);
        }
    }
}
