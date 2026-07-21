package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.util.Comparator;

public final class BlockActivityView {

    private final BlockContentItemView content;
    private final LessonView lesson;
    private final AssessmentView assessment;
    private final int sortOrder;
    private final LocalDateTime sortDate;
    private final int sortKind;
    private final long sortId;

    private static final int UNORDERED = Integer.MAX_VALUE;
    private static final Comparator<BlockActivityView> PEDAGOGICAL_ORDER = Comparator
            .comparingLong(BlockActivityView::getSortId)
            .thenComparing(BlockActivityView::getSortDate)
            .thenComparingInt(BlockActivityView::getSortKind);

    private BlockActivityView(
            BlockContentItemView content,
            LessonView lesson,
            AssessmentView assessment,
            int sortOrder,
            LocalDateTime sortDate,
            int sortKind,
            long sortId
    ) {
        this.content = content;
        this.lesson = lesson;
        this.assessment = assessment;
        this.sortOrder = sortOrder;
        this.sortDate = sortDate == null ? LocalDateTime.MAX : sortDate;
        this.sortKind = sortKind;
        this.sortId = sortId;
    }

    public static BlockActivityView fromContent(BlockContentItemView content) {
        int orderNo = content.getOrderNoRaw() == null ? UNORDERED : content.getOrderNoRaw();
        return new BlockActivityView(content, null, null, orderNo, content.getCreatedAtRaw(), 0, content.getId());
    }

    public static BlockActivityView fromLesson(LessonView lesson) {
        int orderNo = lesson.getOrderNoRaw() == null ? UNORDERED : lesson.getOrderNoRaw();
        return new BlockActivityView(null, lesson, null, orderNo, lesson.getStartsAtRaw(), 1, lesson.getId());
    }

    public static BlockActivityView fromAssessment(AssessmentView assessment) {
        int orderNo = assessment.getOrderNoRaw() == null ? UNORDERED : assessment.getOrderNoRaw();
        return new BlockActivityView(null, null, assessment, orderNo, assessment.getAvailableFromRaw(), 2, assessment.getId());
    }

    public static Comparator<BlockActivityView> pedagogicalOrder() {
        return PEDAGOGICAL_ORDER;
    }

    public BlockContentItemView getContent() {
        return content;
    }

    public LessonView getLesson() {
        return lesson;
    }

    public AssessmentView getAssessment() {
        return assessment;
    }

    public boolean isContentActivity() {
        return content != null;
    }

    public boolean isLessonActivity() {
        return lesson != null;
    }

    public boolean isAssessmentActivity() {
        return assessment != null;
    }

    public LocalDateTime getSortDate() {
        return sortDate;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public int getSortKind() {
        return sortKind;
    }

    public long getSortId() {
        return sortId;
    }
}
