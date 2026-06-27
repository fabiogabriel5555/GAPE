package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class QuestionConfiguration {

    public static final String RATING_STYLE_STARS = "stars";
    public static final String RATING_STYLE_CIRCLES = "circles";
    public static final String RATING_STYLE_HEARTS = "hearts";
    public static final String RATING_STEP_INTEGER = "integer";
    public static final String RATING_STEP_HALF = "half";
    public static final int DEFAULT_RATING_MAX = 5;
    public static final int MAX_RATING_MAX = 100;

    private static final List<String> RATING_STYLES = List.of(
            RATING_STYLE_STARS,
            RATING_STYLE_CIRCLES,
            RATING_STYLE_HEARTS
    );

    private static final List<String> RATING_STEPS = List.of(
            RATING_STEP_INTEGER,
            RATING_STEP_HALF
    );

    private static final List<ContentFormat> RESPONSE_FILE_FORMATS = List.of(
            ContentFormat.PDF,
            ContentFormat.TEXT,
            ContentFormat.IMAGE,
            ContentFormat.VIDEO,
            ContentFormat.AUDIO,
            ContentFormat.ARCHIVE
    );

    private QuestionConfiguration() {
    }

    public static List<String> ratingStyles() {
        return RATING_STYLES;
    }

    public static List<String> ratingSteps() {
        return RATING_STEPS;
    }

    public static String ratingStyle(String expectedAnswer) {
        String value = metadataValue(expectedAnswer, "rating_style");
        if (value == null) {
            return RATING_STYLE_STARS;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return RATING_STYLES.contains(normalized) ? normalized : RATING_STYLE_STARS;
    }

    public static String ratingStep(String expectedAnswer) {
        String value = metadataValue(expectedAnswer, "rating_step");
        if (value == null) {
            return RATING_STEP_INTEGER;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return RATING_STEPS.contains(normalized) ? normalized : RATING_STEP_INTEGER;
    }

    public static int ratingMax(String expectedAnswer) {
        String value = metadataValue(expectedAnswer, "rating_max");
        if (value == null) {
            return DEFAULT_RATING_MAX;
        }
        try {
            return normalizeRatingMax(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return DEFAULT_RATING_MAX;
        }
    }

    public static BigDecimal ratingExpectedValue(String expectedAnswer) {
        String value = metadataValue(expectedAnswer, "expected_value");
        if (value == null && expectedAnswer != null && !expectedAnswer.contains("=")) {
            value = expectedAnswer;
        }
        return validatedRatingValue(value, ratingStep(expectedAnswer), ratingMax(expectedAnswer),
                "Expected rating value is required");
    }

    public static String ratingExpectedAnswer(
            String ratingDesign,
            String ratingMax,
            String expectedValue
    ) {
        RatingDesign design = parseRatingDesign(ratingDesign);
        int max = normalizeRatingMax(integerOrDefault(ratingMax, DEFAULT_RATING_MAX));
        BigDecimal value = validatedRatingValue(expectedValue, design.step(), max,
                "Expected rating value is required");
        return "rating_style=" + design.style()
                + ";rating_step=" + design.step()
                + ";rating_max=" + max
                + ";expected_value=" + value.stripTrailingZeros().toPlainString();
    }

    public static String ratingDesignValue(String expectedAnswer) {
        return ratingStyle(expectedAnswer) + "_" + ratingStep(expectedAnswer);
    }

    public static boolean ratingAllowsFractions(String expectedAnswer) {
        return RATING_STEP_HALF.equals(ratingStep(expectedAnswer));
    }

    public static BigDecimal ratingAnswerValue(String answer, String expectedAnswer) {
        BigDecimal parsed = decimalOrThrow(answer);
        return normalizeRatingValue(
                parsed,
                ratingStep(expectedAnswer),
                ratingMax(expectedAnswer)
        );
    }

    public static boolean isValidRatingAnswerValue(String answer, String expectedAnswer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        BigDecimal parsed;
        try {
            parsed = decimalOrThrow(answer);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        int max = ratingMax(expectedAnswer);
        if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(BigDecimal.valueOf(max)) > 0) {
            return false;
        }
        return normalizeRatingValue(parsed, ratingStep(expectedAnswer), max).compareTo(parsed.stripTrailingZeros()) == 0;
    }

    public static List<ContentFormat> responseFileFormats() {
        return RESPONSE_FILE_FORMATS;
    }

    public static List<ContentFormat> acceptedFileFormats(String expectedAnswer) {
        String value = metadataValue(expectedAnswer, "formats");
        if (value == null) {
            return defaultFileFormats();
        }
        Set<ContentFormat> formats = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String normalized = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            ContentFormat format = ContentFormat.fromDatabaseValue(normalized);
            if (!RESPONSE_FILE_FORMATS.contains(format)) {
                throw new IllegalArgumentException("Unsupported response file format: " + normalized);
            }
            formats.add(format);
        }
        return formats.isEmpty() ? defaultFileFormats() : List.copyOf(formats);
    }

    public static String fileExpectedAnswer(String[] selectedFormats) {
        Set<ContentFormat> formats = new LinkedHashSet<>();
        if (selectedFormats != null) {
            for (String selectedFormat : selectedFormats) {
                if (selectedFormat == null || selectedFormat.isBlank()) {
                    continue;
                }
                ContentFormat format = ContentFormat.fromDatabaseValue(selectedFormat.trim());
                if (!RESPONSE_FILE_FORMATS.contains(format)) {
                    throw new IllegalArgumentException("Unsupported response file format: " + selectedFormat);
                }
                formats.add(format);
            }
        }
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("File upload questions require at least one accepted file type");
        }
        List<String> values = new ArrayList<>();
        for (ContentFormat format : formats) {
            values.add(format.toDatabaseValue());
        }
        return "formats=" + String.join(",", values);
    }

    public static boolean containsFileFormat(String expectedAnswer, ContentFormat format) {
        return acceptedFileFormats(expectedAnswer).contains(format);
    }

    public static String acceptAttribute(List<ContentFormat> formats) {
        List<String> tokens = new ArrayList<>();
        for (ContentFormat format : formats) {
            tokens.add(acceptTokens(format));
        }
        return String.join(",", tokens);
    }

    public static String fileFormatLabel(ContentFormat format) {
        return switch (format) {
            case PDF -> "PDF";
            case TEXT -> "Text";
            case IMAGE -> "Image";
            case VIDEO -> "Video";
            case AUDIO -> "Audio";
            case ARCHIVE -> "Archive";
            default -> format.toDatabaseValue();
        };
    }

    private static String acceptTokens(ContentFormat format) {
        return switch (format) {
            case PDF -> "application/pdf,.pdf";
            case TEXT -> "text/plain,.txt";
            case IMAGE -> "image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.png,.webp,.gif,.bmp";
            case VIDEO -> "video/mp4,video/webm,video/quicktime,video/x-m4v,video/x-matroska,video/x-msvideo,video/mpeg,video/3gpp,.mp4,.webm,.mov,.m4v,.mkv,.avi,.mpeg,.mpg,.3gp,.3gpp";
            case AUDIO -> "audio/mpeg,audio/mp4,audio/x-m4a,audio/wav,audio/x-wav,audio/ogg,audio/flac,audio/aac,audio/webm,audio/opus,application/ogg,.mp3,.m4a,.wav,.ogg,.flac,.aac,.weba,.opus";
            case ARCHIVE -> "application/zip,application/x-zip-compressed,application/vnd.rar,application/x-rar-compressed,application/x-7z-compressed,application/x-tar,application/gzip,application/x-gzip,application/x-bzip2,application/x-xz,.zip,.rar,.7z,.tar,.tar.gz,.tgz,.tar.bz2,.tbz2,.tar.xz,.txz,.gz,.bz2,.xz";
            default -> "";
        };
    }

    private static List<ContentFormat> defaultFileFormats() {
        return List.of(ContentFormat.PDF, ContentFormat.ARCHIVE);
    }

    private static RatingDesign parseRatingDesign(String ratingDesign) {
        if (ratingDesign == null || ratingDesign.isBlank()) {
            return new RatingDesign(RATING_STYLE_STARS, RATING_STEP_INTEGER);
        }
        String normalized = ratingDesign.trim().toLowerCase(Locale.ROOT).replace('|', '_');
        String[] parts = normalized.split("_", 2);
        String style = parts.length > 0 && RATING_STYLES.contains(parts[0]) ? parts[0] : RATING_STYLE_STARS;
        String step = parts.length > 1 && RATING_STEPS.contains(parts[1]) ? parts[1] : RATING_STEP_INTEGER;
        return new RatingDesign(style, step);
    }

    private static int integerOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static BigDecimal decimalOrDefault(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static BigDecimal decimalOrThrow(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid rating value", exception);
        }
    }

    private static BigDecimal decimalOrThrow(String value, String blankMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(blankMessage);
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid rating value", exception);
        }
    }

    private static BigDecimal validatedRatingValue(String value, String step, int max, String blankMessage) {
        BigDecimal parsed = decimalOrThrow(value, blankMessage);
        BigDecimal normalized = normalizeRatingValue(parsed, step, max);
        if (parsed.compareTo(BigDecimal.ZERO) < 0
                || parsed.compareTo(BigDecimal.valueOf(max)) > 0
                || normalized.compareTo(parsed.stripTrailingZeros()) != 0) {
            throw new IllegalArgumentException("Rating value is outside the configured scale");
        }
        return normalized;
    }

    private static int normalizeRatingMax(int value) {
        if (value < 1) {
            return DEFAULT_RATING_MAX;
        }
        return Math.min(value, MAX_RATING_MAX);
    }

    private static BigDecimal normalizeRatingValue(BigDecimal value, String step, int max) {
        BigDecimal bounded = value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(max));
        if (RATING_STEP_HALF.equals(step)) {
            return bounded.multiply(BigDecimal.valueOf(2))
                    .setScale(0, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY)
                    .stripTrailingZeros();
        }
        return BigDecimal.valueOf(bounded.setScale(0, RoundingMode.HALF_UP).longValue())
                .min(BigDecimal.valueOf(max))
                .stripTrailingZeros();
    }

    private static String metadataValue(String source, String key) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String prefix = key + "=";
        for (String part : source.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private record RatingDesign(String style, String step) {
    }
}
