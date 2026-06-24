package pt.isel.gape.web.view;

import java.time.LocalDateTime;

public final class BlockActivityView {

    private final BlockContentItemView content;
    private final LessonView lesson;
    private final LocalDateTime sortDate;
    private final int sortKind;
    private final long sortId;

    private BlockActivityView(
            BlockContentItemView content,
            LessonView lesson,
            LocalDateTime sortDate,
            int sortKind,
            long sortId
    ) {
        this.content = content;
        this.lesson = lesson;
        this.sortDate = sortDate == null ? LocalDateTime.MIN : sortDate;
        this.sortKind = sortKind;
        this.sortId = sortId;
    }

    public static BlockActivityView fromContent(BlockContentItemView content) {
        return new BlockActivityView(content, null, content.getCreatedAtRaw(), 0, content.getId());
    }

    public static BlockActivityView fromLesson(LessonView lesson) {
        return new BlockActivityView(null, lesson, lesson.getStartsAtRaw(), 1, lesson.getId());
    }

    public BlockContentItemView getContent() {
        return content;
    }

    public LessonView getLesson() {
        return lesson;
    }

    public boolean isContentActivity() {
        return content != null;
    }

    public boolean isLessonActivity() {
        return lesson != null;
    }

    public LocalDateTime getSortDate() {
        return sortDate;
    }

    public int getSortKind() {
        return sortKind;
    }

    public long getSortId() {
        return sortId;
    }
}
