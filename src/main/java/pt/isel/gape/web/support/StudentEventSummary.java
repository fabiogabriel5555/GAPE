package pt.isel.gape.web.support;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.model.LearningEvent;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.LearningNotificationView;

/**
 * The student-only, actionable part of the event feed.  These records are
 * derived by {@code LearningEventDAO}; this projection only decides which
 * unread spans belong to each student screen.
 */
public final class StudentEventSummary {

    private static final Set<String> STUDENT_EVENT_TYPES = Set.of(
            "student_class_group_enrollment_accepted",
            "student_lesson_running",
            "student_assessment_enrollment_required",
            "student_assessment_attempt_required",
            "student_grade_sheet_grade_available",
            "student_certificate_published"
    );
    private static final Set<String> CLASS_GROUP_EVENT_TYPES = Set.of(
            "student_class_group_enrollment_accepted",
            "student_lesson_running",
            "student_assessment_enrollment_required",
            "student_assessment_attempt_required",
            "student_grade_sheet_grade_available"
    );

    private final List<LearningNotificationView> unreadEvents;
    private final Map<Long, Integer> unreadCountByClassGroupId;
    private final Map<String, Boolean> unreadByHref;
    private final int classGroupEventCount;
    private final int certificateEventCount;

    private StudentEventSummary(List<LearningNotificationView> unreadEvents) {
        this.unreadEvents = List.copyOf(unreadEvents);
        Map<Long, Integer> byClassGroup = new LinkedHashMap<>();
        Map<String, Boolean> byHref = new LinkedHashMap<>();
        int classGroups = 0;
        int certificates = 0;
        for (LearningNotificationView event : unreadEvents) {
            byHref.put(event.getHref(), Boolean.TRUE);
            if (CLASS_GROUP_EVENT_TYPES.contains(event.getCategoryValue())) {
                classGroups++;
                if (event.getClassGroupId() != null) {
                    byClassGroup.merge(event.getClassGroupId(), 1, Integer::sum);
                }
            }
            if ("student_certificate_published".equals(event.getCategoryValue())) {
                certificates++;
            }
        }
        this.unreadCountByClassGroupId = Map.copyOf(byClassGroup);
        this.unreadByHref = Map.copyOf(byHref);
        this.classGroupEventCount = classGroups;
        this.certificateEventCount = certificates;
    }

    public static StudentEventSummary load(ConnectionProvider connectionProvider, long studentUserId)
            throws SQLException {
        ApplicationReadService readService = new ApplicationReadService(connectionProvider);
        List<LearningNotificationView> events = readService.learningEvents().findVisible(
                        List.of(),
                        List.of(),
                        List.of(),
                        studentUserId,
                        true,
                        false,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.now(ApplicationClock.system()),
                        studentUserId,
                        500,
                        0
                )
                .stream()
                .filter(event -> STUDENT_EVENT_TYPES.contains(event.eventType()))
                .filter(event -> !event.read())
                .map(StudentEventSummary::view)
                .toList();
        return new StudentEventSummary(events);
    }

    public static StudentEventSummary empty() {
        return new StudentEventSummary(List.of());
    }

    private static LearningNotificationView view(LearningEvent event) {
        return new LearningNotificationView(
                event.id(),
                event.eventType(),
                event.categoryLabel(),
                event.title(),
                event.description(),
                event.contextLabel(),
                event.contextTitle(),
                LearningNotificationView.dateTime(event.occurredAt()),
                event.occurredAt(),
                event.classGroupId(),
                event.detailHref(),
                event.iconClass(),
                event.badgeClass(),
                event.stateLabel(),
                event.stateBadgeClass(),
                event.read()
        );
    }

    public List<LearningNotificationView> getUnreadEvents() {
        return unreadEvents;
    }

    public Map<Long, Integer> getUnreadCountByClassGroupId() {
        return unreadCountByClassGroupId;
    }

    public Map<String, Boolean> getUnreadByHref() {
        return unreadByHref;
    }

    public int getUnreadCount() {
        return unreadEvents.size();
    }

    public int getClassGroupEventCount() {
        return classGroupEventCount;
    }

    public int getCertificateEventCount() {
        return certificateEventCount;
    }
}
