package pt.isel.gape.learning.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionState;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.Subject;

public final class AssessmentPdfService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final SubjectDAO subjectDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final ClassGroupDAO classGroupDAO;

    public AssessmentPdfService(ConnectionProvider connectionProvider) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider)
        );
    }

    AssessmentPdfService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO
    ) {
        this(
                connectionProvider,
                assessmentDAO,
                questionDAO,
                optionDAO,
                new SubjectDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider)
        );
    }

    AssessmentPdfService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            SubjectDAO subjectDAO,
            ContentBlockDAO contentBlockDAO,
            ClassGroupDAO classGroupDAO
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.contentBlockDAO = Objects.requireNonNull(contentBlockDAO, "contentBlockDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
    }

    public byte[] renderAssessmentPdf(long assessmentId) {
        try (Connection connection = connectionProvider.getConnection()) {
            Assessment assessment = assessmentDAO.findById(connection, assessmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
            AssessmentPdfContext context = assessmentContext(connection, assessment);
            List<Question> questions = questionDAO.findByAssessment(connection, assessmentId).stream()
                    .filter(question -> question.state() == QuestionState.ACTIVE)
                    .toList();

            try (PDDocument document = new PDDocument()) {
                AssessmentPdfWriter writer = new AssessmentPdfWriter(document, context);
                writer.header();
                if (questions.isEmpty()) {
                    writer.emptyState();
                }
                int questionNumber = 1;
                for (Question question : questions) {
                    List<QuestionOption> options = List.of();
                    if (question.type().allowsOptions()) {
                        options = optionDAO.findByQuestion(connection, question.id()).stream()
                                .filter(option -> option.state() == QuestionOptionState.ACTIVE)
                                .toList();
                    }
                    writer.question(questionNumber, question, options);
                    questionNumber += 1;
                }
                return writer.bytes();
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Failed to render assessment PDF", exception);
        }
    }

    private AssessmentPdfContext assessmentContext(Connection connection, Assessment assessment) throws SQLException {
        Subject subject = assessment.subjectId() == null
                ? null
                : subjectDAO.findById(connection, assessment.subjectId()).orElse(null);
        ContentBlock block = assessment.contentBlockId() == null
                ? null
                : contentBlockDAO.findById(connection, assessment.contentBlockId()).orElse(null);
        ClassGroup classGroup = block == null
                ? null
                : classGroupDAO.findById(connection, block.classGroupId()).orElse(null);
        String context = contextLabel(subject, block, classGroup);
        return new AssessmentPdfContext(
                assessment.title(),
                assessment.description(),
                context,
                formatDateTime(assessment.availableFrom()),
                formatDateTime(assessment.availableUntil()),
                assessmentTypeLabel(assessment),
                assessmentModeLabel(assessment)
        );
    }

    private static String contextLabel(Subject subject, ContentBlock block, ClassGroup classGroup) {
        if (classGroup != null && block != null) {
            return classGroup.code() + " | " + block.name();
        }
        if (subject == null) {
            return "No context";
        }
        if (subject.acronym() != null && !subject.acronym().isBlank()) {
            return subject.acronym() + " | " + subject.name();
        }
        return subject.name();
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "Not defined" : DISPLAY_DATE_TIME.format(value);
    }

    private static String assessmentTypeLabel(Assessment assessment) {
        return switch (assessment.type()) {
            case FORM -> "Form";
            case TEST -> "Test";
            case EXAM -> "Exam";
        };
    }

    private static String assessmentModeLabel(Assessment assessment) {
        return switch (assessment.mode()) {
            case ONLINE -> "Online";
            case ONSITE -> "In-Person";
        };
    }

    private static String questionTypeLabel(QuestionType type) {
        return switch (type) {
            case SINGLE_CHOICE -> "Single choice";
            case MULTIPLE_CHOICE -> "Multiple choice";
            case SHORT_TEXT -> "Short text";
            case PARAGRAPH -> "Long text";
            case FILE_UPLOAD -> "Upload";
            case RATING -> "Rating";
        };
    }

    private static String grade(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private record AssessmentPdfContext(
            String title,
            String description,
            String context,
            String startsAt,
            String endsAt,
            String type,
            String mode
    ) {
    }

    private static final class AssessmentPdfWriter {
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 44f;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f);
        private static final float TITLE_LINE = 22f;
        private static final float BODY_LINE = 13.5f;
        private static final float SMALL_LINE = 11.5f;

        private final PDDocument document;
        private final AssessmentPdfContext context;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream stream;
        private float y;

        private AssessmentPdfWriter(PDDocument document, AssessmentPdfContext context) throws IOException {
            this.document = document;
            this.context = context;
            newPage();
        }

        private void header() throws IOException {
            List<String> titleLines = wrap(context.title(), bold, 18f, CONTENT_WIDTH);
            List<String> descriptionLines = context.description() == null || context.description().isBlank()
                    ? List.of()
                    : wrap(context.description(), regular, 10.5f, CONTENT_WIDTH);
            float metadataHeight = 76f;
            float descriptionHeight = descriptionLines.isEmpty() ? 0f : 12f + (descriptionLines.size() * BODY_LINE);
            float height = 14f + (titleLines.size() * TITLE_LINE) + 12f + metadataHeight + descriptionHeight + 16f;
            ensureSpace(height);

            float top = y;
            drawText("Assessment", bold, 9.5f, 0, 150, 136, MARGIN, top - 10f);
            float titleY = top - 34f;
            for (String line : titleLines) {
                drawText(line, bold, 18f, 28, 38, 52, MARGIN, titleY);
                titleY -= TITLE_LINE;
            }

            float metaTop = titleY - 4f;
            strokeLine(MARGIN, metaTop, MARGIN + CONTENT_WIDTH, metaTop, 218, 226, 234, 0.8f);
            float metadataGap = 18f;
            float metadataColumnWidth = (CONTENT_WIDTH - metadataGap) / 2f;
            float metadataSecondColumnX = MARGIN + metadataColumnWidth + metadataGap;
            drawInfoPair("Start", context.startsAt(), MARGIN, metaTop - 22f, metadataColumnWidth);
            drawInfoPair("End", context.endsAt(), metadataSecondColumnX, metaTop - 22f, metadataColumnWidth);
            drawInfoPair("Context", context.context(), MARGIN, metaTop - 50f, metadataColumnWidth);
            drawInfoPair("Mode", context.type() + " | " + context.mode(), metadataSecondColumnX,
                    metaTop - 50f, metadataColumnWidth);

            float nextY = metaTop - metadataHeight;
            if (!descriptionLines.isEmpty()) {
                drawText("Description", bold, 9.5f, 71, 85, 105, MARGIN, nextY - 2f);
                float descY = nextY - 20f;
                for (String line : descriptionLines) {
                    drawText(line, regular, 10.5f, 55, 65, 81, MARGIN, descY);
                    descY -= BODY_LINE;
                }
                nextY = descY + BODY_LINE - 6f;
            }
            strokeLine(MARGIN, nextY, MARGIN + CONTENT_WIDTH, nextY, 218, 226, 234, 0.8f);
            y = nextY - 24f;
        }

        private void question(int number, Question question, List<QuestionOption> options) throws IOException {
            QuestionLayout layout = measureQuestion(number, question, options);
            float pageCapacity = PAGE_HEIGHT - (MARGIN * 2f);
            if (y - (layout.height + 18f) < MARGIN || layout.height > pageCapacity) {
                newPage();
            }

            float top = y;
            float bottom = top - layout.height;
            strokeRoundRect(MARGIN, bottom, CONTENT_WIDTH, layout.height, 8f, 218, 226, 234, 0.9f);

            float x = MARGIN + 18f;
            float contentWidth = CONTENT_WIDTH - 36f;
            float currentY = top - 18f;
            currentY = drawQuestionHeader(layout, question, x, currentY, contentWidth);
            currentY -= 16f;
            drawQuestionControl(question, options, x, currentY, contentWidth);
            y = bottom - 18f;
        }

        private QuestionLayout measureQuestion(int number, Question question, List<QuestionOption> options)
                throws IOException {
            float contentWidth = CONTENT_WIDTH - 36f;
            List<String> statementLines = wrap(question.statement(), bold, 12.8f, contentWidth);
            float headerHeight = 20f + (statementLines.size() * 15.5f);
            float controlHeight = controlHeight(question, options, contentWidth);
            float total = 18f + headerHeight + 16f + controlHeight + 18f;
            return new QuestionLayout(number, statementLines, headerHeight, controlHeight, total);
        }

        private float controlHeight(Question question, List<QuestionOption> options, float width) throws IOException {
            return switch (question.type()) {
                case SINGLE_CHOICE, MULTIPLE_CHOICE -> optionsHeight(options, width);
                case PARAGRAPH -> 86f;
                case FILE_UPLOAD -> 46f;
                case RATING -> 34f;
                case SHORT_TEXT -> 42f;
            };
        }

        private float optionsHeight(List<QuestionOption> options, float width) throws IOException {
            if (options.isEmpty()) {
                return 34f;
            }
            float height = 0f;
            for (int index = 0; index < options.size(); index += 1) {
                height += optionHeight(options.get(index), width);
                if (index < options.size() - 1) {
                    height += 9f;
                }
            }
            return height;
        }

        private float optionHeight(QuestionOption option, float width) throws IOException {
            return Math.max(28f, 14f + (wrap(option.text(), regular, 10.3f, width - 58f).size() * SMALL_LINE));
        }

        private float drawQuestionHeader(QuestionLayout layout, Question question, float x, float top, float width)
                throws IOException {
            drawText("Question " + layout.number(), bold, 9.8f, 0, 150, 136, x, top);
            String meta = questionTypeLabel(question.type()) + " | " + grade(question.score()) + " pts";
            drawText(meta, regular, 9.3f, 100, 116, 139, x + 78f, top);
            if (question.required()) {
                drawRequiredPill(x + 86f + textWidth(meta, regular, 9.3f), top + 2f);
            }
            float statementY = top - 24f;
            for (String line : layout.statementLines()) {
                drawText(line, bold, 12.8f, 28, 38, 52, x, statementY);
                statementY -= 15.5f;
            }
            return top - layout.headerHeight();
        }

        private void drawQuestionControl(Question question, List<QuestionOption> options, float x, float top, float width)
                throws IOException {
            switch (question.type()) {
                case SINGLE_CHOICE -> drawChoiceOptions(options, x, top, width, false);
                case MULTIPLE_CHOICE -> drawChoiceOptions(options, x, top, width, true);
                case SHORT_TEXT -> drawTextInput(x, top, width, 42f, "Type here");
                case PARAGRAPH -> drawTextInput(x, top, width, 86f, "Type here");
                case FILE_UPLOAD -> drawUploadButton(x, top);
                case RATING -> drawRating(question, x, top);
            }
        }

        private void drawChoiceOptions(List<QuestionOption> options, float x, float top, float width, boolean multiple)
                throws IOException {
            if (options.isEmpty()) {
                drawText("No options configured.", regular, 10f, 100, 116, 139, x, top - 18f);
                return;
            }
            float currentTop = top;
            for (QuestionOption option : options) {
                float height = optionHeight(option, width);
                float controlCenterY = currentTop - (height / 2f);
                if (multiple) {
                    drawCheckbox(x, controlCenterY - 6f, 12f);
                } else {
                    drawRadio(x + 6f, controlCenterY, 6f);
                }
                strokeRoundRect(x + 24f, currentTop - height, width - 24f, height, 8f, 78, 93, 109, 0.9f);
                float textY = currentTop - 17f;
                for (String line : wrap(option.text(), regular, 10.3f, width - 58f)) {
                    drawText(line, bold, 10.3f, 71, 85, 105, x + 36f, textY);
                    textY -= SMALL_LINE;
                }
                currentTop -= height + 9f;
            }
        }

        private void drawTextInput(float x, float top, float width, float height, String placeholder) throws IOException {
            strokeRoundRect(x, top - height, width, height, 6f, 78, 93, 109, 1.0f);
            drawText(placeholder, bold, 10f, 71, 85, 105, x + 16f, top - 23f);
        }

        private void drawUploadButton(float x, float top) throws IOException {
            float width = 92f;
            float height = 34f;
            fillRoundRect(x, top - height, width, height, 6f, 65, 196, 183);
            strokeRoundRect(x, top - height, width, height, 6f, 25, 130, 120, 0.8f);
            drawUploadGlyph(x + 20f, top - 17f);
            drawText("Upload", bold, 10f, 255, 255, 255, x + 33f, top - 21f);
        }

        private void drawRating(Question question, float x, float top) throws IOException {
            int max = Math.min(QuestionConfiguration.ratingMax(question.expectedAnswer()), 10);
            float currentX = x;
            for (int index = 0; index < max; index += 1) {
                drawStar(currentX + 10f, top - 17f, 10f, 4.3f, 255, 193, 7, false);
                currentX += 24f;
            }
        }

        private void drawRequiredPill(float x, float baseline) throws IOException {
            float width = 49f;
            float height = 14f;
            fillRoundRect(x, baseline - 8f, width, height, 7f, 230, 248, 245);
            strokeRoundRect(x, baseline - 8f, width, height, 7f, 0, 150, 136, 0.6f);
            drawText("Required", bold, 7.3f, 0, 120, 110, x + 7f, baseline - 4.3f);
        }

        private void emptyState() throws IOException {
            float height = 72f;
            ensureSpace(height);
            strokeRoundRect(MARGIN, y - height, CONTENT_WIDTH, height, 8f, 218, 226, 234, 0.9f);
            drawText("No questions available", bold, 13f, 28, 38, 52, MARGIN + 18f, y - 28f);
            drawText("This assessment has no active questions.", regular, 10f, 100, 116, 139, MARGIN + 18f, y - 48f);
            y -= height + 18f;
        }

        private byte[] bytes() throws IOException {
            closeStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }

        private void drawInfoPair(String label, String value, float x, float baseline, float width) throws IOException {
            drawText(label, bold, 8.8f, 100, 116, 139, x, baseline);
            List<String> lines = wrap(value, regular, 9.5f, width);
            float valueY = baseline - 13f;
            for (int index = 0; index < Math.min(lines.size(), 2); index += 1) {
                drawText(lines.get(index), regular, 9.5f, 28, 38, 52, x, valueY);
                valueY -= SMALL_LINE;
            }
        }

        private void drawCheckbox(float x, float yBottom, float size) throws IOException {
            strokeRoundRect(x, yBottom, size, size, 2f, 78, 93, 109, 0.9f);
        }

        private void drawRadio(float centerX, float centerY, float radius) throws IOException {
            float c = radius * 0.55228475f;
            stream.setStrokingColor(78 / 255f, 93 / 255f, 109 / 255f);
            stream.setLineWidth(0.9f);
            stream.moveTo(centerX + radius, centerY);
            stream.curveTo(centerX + radius, centerY + c, centerX + c, centerY + radius, centerX, centerY + radius);
            stream.curveTo(centerX - c, centerY + radius, centerX - radius, centerY + c, centerX - radius, centerY);
            stream.curveTo(centerX - radius, centerY - c, centerX - c, centerY - radius, centerX, centerY - radius);
            stream.curveTo(centerX + c, centerY - radius, centerX + radius, centerY - c, centerX + radius, centerY);
            stream.closePath();
            stream.stroke();
        }

        private void drawStar(
                float centerX,
                float centerY,
                float outerRadius,
                float innerRadius,
                int r,
                int g,
                int b,
                boolean filled
        ) throws IOException {
            for (int point = 0; point < 10; point += 1) {
                double angle = Math.toRadians(-90 + (point * 36));
                float radius = point % 2 == 0 ? outerRadius : innerRadius;
                float px = centerX + (float) Math.cos(angle) * radius;
                float py = centerY + (float) Math.sin(angle) * radius;
                if (point == 0) {
                    stream.moveTo(px, py);
                } else {
                    stream.lineTo(px, py);
                }
            }
            stream.closePath();
            stream.setStrokingColor(r / 255f, g / 255f, b / 255f);
            stream.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
            stream.setLineWidth(1f);
            if (filled) {
                stream.fill();
            } else {
                stream.stroke();
            }
        }

        private void drawUploadGlyph(float centerX, float centerY) throws IOException {
            stream.setStrokingColor(255 / 255f, 255 / 255f, 255 / 255f);
            stream.setLineWidth(1.4f);
            stream.moveTo(centerX, centerY - 6f);
            stream.lineTo(centerX, centerY + 5f);
            stream.moveTo(centerX - 4.5f, centerY + 0.5f);
            stream.lineTo(centerX, centerY + 5f);
            stream.lineTo(centerX + 4.5f, centerY + 0.5f);
            stream.moveTo(centerX - 6f, centerY - 8f);
            stream.lineTo(centerX + 6f, centerY - 8f);
            stream.stroke();
        }

        private void fillRoundRect(float x, float y, float width, float height, float radius, int r, int g, int b)
                throws IOException {
            stream.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
            addRoundRect(x, y, width, height, radius);
            stream.fill();
        }

        private void strokeRoundRect(
                float x,
                float y,
                float width,
                float height,
                float radius,
                int r,
                int g,
                int b,
                float lineWidth
        ) throws IOException {
            stream.setStrokingColor(r / 255f, g / 255f, b / 255f);
            stream.setLineWidth(lineWidth);
            addRoundRect(x, y, width, height, radius);
            stream.stroke();
        }

        private void addRoundRect(float x, float y, float width, float height, float radius) throws IOException {
            float right = x + width;
            float top = y + height;
            float c = radius * 0.55228475f;
            stream.moveTo(x + radius, y);
            stream.lineTo(right - radius, y);
            stream.curveTo(right - radius + c, y, right, y + radius - c, right, y + radius);
            stream.lineTo(right, top - radius);
            stream.curveTo(right, top - radius + c, right - radius + c, top, right - radius, top);
            stream.lineTo(x + radius, top);
            stream.curveTo(x + radius - c, top, x, top - radius + c, x, top - radius);
            stream.lineTo(x, y + radius);
            stream.curveTo(x, y + radius - c, x + radius - c, y, x + radius, y);
            stream.closePath();
        }

        private void strokeLine(float x1, float y1, float x2, float y2, int r, int g, int b, float width)
                throws IOException {
            stream.setStrokingColor(r / 255f, g / 255f, b / 255f);
            stream.setLineWidth(width);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private void drawText(String text, PDFont font, float fontSize, int r, int g, int b, float x, float baseline)
                throws IOException {
            String value = safeLine(text);
            if (value.isBlank()) {
                return;
            }
            stream.beginText();
            stream.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            closeStream();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String safeText = safe(text);
            List<String> lines = new ArrayList<>();
            for (String paragraph : safeText.split("\\R", -1)) {
                String normalized = paragraph.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (String word : normalized.split("\\s+")) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (textWidth(candidate, font, fontSize) <= maxWidth) {
                        if (!line.isEmpty()) {
                            line.append(' ');
                        }
                        line.append(word);
                    } else {
                        if (!line.isEmpty()) {
                            lines.add(line.toString());
                            line.setLength(0);
                        }
                        if (textWidth(word, font, fontSize) <= maxWidth) {
                            line.append(word);
                        } else {
                            lines.addAll(wrapLongWord(word, font, fontSize, maxWidth));
                        }
                    }
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
            }
            return lines.isEmpty() ? List.of("-") : lines;
        }

        private List<String> wrapLongWord(String word, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (int index = 0; index < word.length(); index += 1) {
                String candidate = line.toString() + word.charAt(index);
                if (!line.isEmpty() && textWidth(candidate, font, fontSize) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(word.charAt(index));
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }

        private float textWidth(String text, PDFont font, float fontSize) throws IOException {
            return font.getStringWidth(safeLine(text)) / 1000f * fontSize;
        }

        private static String safeLine(String text) {
            return safe(text).replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        }

        private static String safe(String text) {
            if (text == null || text.isBlank()) {
                return "-";
            }
            StringBuilder safe = new StringBuilder(text.length());
            for (int index = 0; index < text.length(); index += 1) {
                char character = text.charAt(index);
                if (character == '\u00A0') {
                    safe.append(' ');
                } else if (character == '\n' || character == '\r' || character == '\t') {
                    safe.append(character);
                } else if (character < 32 || character > 255) {
                    safe.append('?');
                } else {
                    safe.append(character);
                }
            }
            return safe.toString();
        }
    }

    private record QuestionLayout(
            int number,
            List<String> statementLines,
            float headerHeight,
            float controlHeight,
            float height
    ) {
    }
}
