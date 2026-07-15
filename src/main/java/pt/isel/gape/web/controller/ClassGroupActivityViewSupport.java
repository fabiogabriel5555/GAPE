package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.PhysicalRoomService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;

final class ClassGroupActivityViewSupport {

    private final LessonService lessonService;
    private final ApplicationReadService.Lessons lessonDAO;
    private final PhysicalRoomService roomService;
    private final ApplicationReadService.Assessments assessmentDAO;
    private final ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO;
    private final ApplicationReadService.Attempts attemptDAO;
    private final ApplicationReadService.PhysicalRooms physicalRoomDAO;
    private final LearningViewFactory viewFactory;
    private final AssessmentViewFactory assessmentViewFactory;

    ClassGroupActivityViewSupport(ConnectionProvider connectionProvider, Clock clock) {
        this(connectionProvider, clock, new ApplicationReadService(connectionProvider));
    }

    private ClassGroupActivityViewSupport(
            ConnectionProvider connectionProvider,
            Clock clock,
            ApplicationReadService readService
    ) {
        this(
                new LessonService(connectionProvider, clock),
                readService.lessons(),
                new PhysicalRoomService(connectionProvider, clock),
                readService.assessments(),
                readService.assessmentEnrollments(),
                readService.attempts(),
                readService.physicalRooms(),
                new LearningViewFactory(
                        readService.organizations(),
                        readService.organicUnits(),
                        readService.courses(),
                        readService.courseOccurrences(),
                        readService.subjects(),
                        readService.courseSubjects(),
                        readService.enrollments(),
                        readService.classGroups(),
                        readService.classGroupEnrollments(),
                        readService.contentBlocks(),
                        readService.users(),
                        readService.teachClassGroups()
                ),
                new AssessmentViewFactory(
                        readService.assessments(),
                        readService.questions(),
                        readService.questionOptions(),
                        readService.attempts(),
                        readService.responses(),
                        readService.subjects(),
                        readService.contentBlocks(),
                        readService.classGroups(),
                        readService.users()
                )
        );
    }

    ClassGroupActivityViewSupport(
            LessonService lessonService,
            ApplicationReadService.Lessons lessonDAO,
            PhysicalRoomService roomService,
            ApplicationReadService.Assessments assessmentDAO,
            ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO,
            ApplicationReadService.Attempts attemptDAO,
            ApplicationReadService.PhysicalRooms physicalRoomDAO,
            LearningViewFactory viewFactory,
            AssessmentViewFactory assessmentViewFactory
    ) {
        this.lessonService = lessonService;
        this.lessonDAO = lessonDAO;
        this.roomService = roomService;
        this.assessmentDAO = assessmentDAO;
        this.assessmentEnrollmentDAO = assessmentEnrollmentDAO;
        this.attemptDAO = attemptDAO;
        this.physicalRoomDAO = physicalRoomDAO;
        this.viewFactory = viewFactory;
        this.assessmentViewFactory = assessmentViewFactory;
    }

    void exposeClassGroupActivities(
            HttpServletRequest request,
            SessionUser actor,
            Long sessionId,
            AccessProfileType profileType,
            Collection<Long> classGroupIds
    ) {
        Map<Long, List<LessonView>> lessonsByClassGroup = lessonsByClassGroup(
                actor,
                sessionId,
                profileType,
                classGroupIds,
                request.getRemoteAddr()
        );
        Map<Long, List<AssessmentView>> assessmentsByClassGroup = assessmentsByClassGroup(classGroupIds);
        Map<Long, Integer> pendingEnrollmentCountByAssessment =
                pendingEnrollmentCountByAssessment(assessmentsByClassGroup);
        Map<Long, Integer> pendingCorrectionCountByAssessment =
                pendingCorrectionCountByAssessment(assessmentsByClassGroup);
        Map<Long, List<PhysicalRoomView>> roomsByClassGroup =
                roomsByClassGroup(lessonsByClassGroup, assessmentsByClassGroup);
        request.setAttribute("classGroupLessonsByClassGroup", lessonsByClassGroup);
        request.setAttribute("classGroupAssessmentsByClassGroup", assessmentsByClassGroup);
        request.setAttribute("classGroupAssessmentPendingEnrollmentCountById", pendingEnrollmentCountByAssessment);
        request.setAttribute("classGroupAssessmentPendingCorrectionCountById", pendingCorrectionCountByAssessment);
        request.setAttribute("classGroupRoomsByClassGroup", roomsByClassGroup);
        request.setAttribute("classGroupActivityCountByClassGroup",
                activityCountByClassGroup(lessonsByClassGroup, assessmentsByClassGroup));
        exposeTimelineRanks(request, lessonsByClassGroup, assessmentsByClassGroup);
        request.setAttribute(
                "canManagePhysicalRoomByCode",
                canManagePhysicalRoomByCode(actor, sessionId, profileType, roomsByClassGroup, request.getRemoteAddr())
        );
    }

    /**
     * Supplies the lightweight counters used by collapsed class-group rows.
     * The detailed lessons, assessments and rooms stay deferred until the user
     * explicitly opens a row.
     */
    void exposeClassGroupActivityCounts(HttpServletRequest request, Collection<Long> classGroupIds) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        classGroupIds.stream().filter(java.util.Objects::nonNull).sorted().forEach(id -> counts.put(id, 0));
        if (counts.isEmpty()) {
            request.setAttribute("classGroupActivityCountByClassGroup", Map.of());
            return;
        }
        try {
            Map<Long, Integer> lessonCounts = lessonDAO.countByClassGroupIds(counts.keySet());
            Map<Long, Integer> assessmentCounts = assessmentDAO.countByClassGroupIds(counts.keySet());
            for (Long classGroupId : counts.keySet()) {
                counts.put(
                        classGroupId,
                        lessonCounts.getOrDefault(classGroupId, 0)
                                + assessmentCounts.getOrDefault(classGroupId, 0)
                );
            }
        } catch (SQLException exception) {
            // Activity counts must not prevent a subject's core structure from opening.
        }
        request.setAttribute("classGroupActivityCountByClassGroup", Map.copyOf(counts));
    }

    private Map<Long, List<LessonView>> lessonsByClassGroup(
            SessionUser actor,
            Long sessionId,
            AccessProfileType profileType,
            Collection<Long> classGroupIds,
            String sourceIp
    ) {
        Map<Long, List<LessonView>> result = new LinkedHashMap<>();
        classGroupIds.stream()
                .sorted()
                .forEach(classGroupId -> result.put(
                        classGroupId,
                        lessonsForClassGroup(actor, sessionId, profileType, classGroupId, sourceIp)
                ));
        return result;
    }

    private List<LessonView> lessonsForClassGroup(
            SessionUser actor,
            Long sessionId,
            AccessProfileType profileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            return lessonService.listLessonsByClassGroup(
                            actor.userId(),
                            sessionId,
                            profileType,
                            classGroupId,
                            sourceIp
                    )
                    .stream()
                    .map(viewFactory::lessonView)
                    .sorted(Comparator.comparing(LessonView::getStartsAtRaw).thenComparingLong(LessonView::getId))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private Map<Long, List<AssessmentView>> assessmentsByClassGroup(Collection<Long> classGroupIds) {
        Map<Long, List<AssessmentView>> result = new LinkedHashMap<>();
        classGroupIds.stream()
                .sorted()
                .forEach(classGroupId -> result.put(classGroupId, assessmentsForClassGroup(classGroupId)));
        return result;
    }

    private static Map<Long, Integer> activityCountByClassGroup(
            Map<Long, List<LessonView>> lessonsByClassGroup,
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Long classGroupId : lessonsByClassGroup.keySet()) {
            result.put(classGroupId,
                    lessonsByClassGroup.getOrDefault(classGroupId, List.of()).size()
                            + assessmentsByClassGroup.getOrDefault(classGroupId, List.of()).size());
        }
        for (Long classGroupId : assessmentsByClassGroup.keySet()) {
            result.putIfAbsent(classGroupId, assessmentsByClassGroup.getOrDefault(classGroupId, List.of()).size());
        }
        return result;
    }

    private List<AssessmentView> assessmentsForClassGroup(long classGroupId) {
        try {
            return assessmentDAO.findByClassGroup(classGroupId).stream()
                    .map(assessmentViewFactory::assessmentView)
                    .sorted(Comparator.comparing(AssessmentView::getAvailableFromRaw,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(AssessmentView::getTitle, String.CASE_INSENSITIVE_ORDER)
                            .thenComparingLong(AssessmentView::getId))
                    .toList();
        } catch (SQLException exception) {
            return List.of();
        }
    }

    private Map<Long, Integer> pendingEnrollmentCountByAssessment(
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        List<Long> assessmentIds = assessmentIds(assessmentsByClassGroup);
        if (assessmentIds.isEmpty()) {
            return Map.of();
        }
        try {
            return assessmentEnrollmentDAO.countByAssessmentIdsAndState(assessmentIds, EnrollmentState.PENDING);
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private Map<Long, Integer> pendingCorrectionCountByAssessment(
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        List<Long> assessmentIds = assessmentIds(assessmentsByClassGroup);
        if (assessmentIds.isEmpty()) {
            return Map.of();
        }
        try {
            return attemptDAO.countByAssessmentIdsAndState(assessmentIds, AttemptState.SUBMITTED);
        } catch (SQLException exception) {
            return Map.of();
        }
    }

    private static List<Long> assessmentIds(Map<Long, List<AssessmentView>> assessmentsByClassGroup) {
        return assessmentsByClassGroup.values().stream()
                .flatMap(Collection::stream)
                .map(AssessmentView::getId)
                .distinct()
                .toList();
    }

    private Map<Long, List<PhysicalRoomView>> roomsByClassGroup(
            Map<Long, List<LessonView>> lessonsByClassGroup,
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        Map<Long, List<PhysicalRoomView>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<LessonView>> entry : lessonsByClassGroup.entrySet()) {
            Map<String, PhysicalRoomView> roomsByCode = new LinkedHashMap<>();
            for (LessonView lesson : entry.getValue()) {
                if (!lesson.isHasRoom()) {
                    continue;
                }
                try {
                    PhysicalRoom room = physicalRoomDAO.findByCode(lesson.getPhysicalRoomCode()).orElse(null);
                    if (room != null) {
                        roomsByCode.putIfAbsent(room.code(), viewFactory.physicalRoomView(room));
                    }
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to load class group physical rooms", exception);
                }
            }
            for (AssessmentView assessment : assessmentsByClassGroup.getOrDefault(entry.getKey(), List.of())) {
                if (!assessment.isHasRoom()) {
                    continue;
                }
                try {
                    PhysicalRoom room = physicalRoomDAO.findByCode(assessment.getPhysicalRoomCode()).orElse(null);
                    if (room != null) {
                        roomsByCode.putIfAbsent(room.code(), viewFactory.physicalRoomView(room));
                    }
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to load class group physical rooms", exception);
                }
            }
            result.put(entry.getKey(), List.copyOf(roomsByCode.values()));
        }
        return result;
    }

    private static void exposeTimelineRanks(
            HttpServletRequest request,
            Map<Long, List<LessonView>> lessonsByClassGroup,
            Map<Long, List<AssessmentView>> assessmentsByClassGroup
    ) {
        Map<Long, Map<Long, Integer>> lessonRanksByClassGroup = new LinkedHashMap<>();
        Map<Long, Map<Long, Integer>> assessmentRanksByClassGroup = new LinkedHashMap<>();
        for (Long classGroupId : lessonsByClassGroup.keySet()) {
            List<ActivityTimelineItem> items = new java.util.ArrayList<>();
            for (LessonView lesson : lessonsByClassGroup.getOrDefault(classGroupId, List.of())) {
                items.add(ActivityTimelineItem.lesson(lesson));
            }
            for (AssessmentView assessment : assessmentsByClassGroup.getOrDefault(classGroupId, List.of())) {
                items.add(ActivityTimelineItem.assessment(assessment));
            }
            items.sort(Comparator
                    .comparing(ActivityTimelineItem::startsAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ActivityTimelineItem::title, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(ActivityTimelineItem::id));
            Map<Long, Integer> lessonRanks = new LinkedHashMap<>();
            Map<Long, Integer> assessmentRanks = new LinkedHashMap<>();
            for (int index = 0; index < items.size(); index++) {
                ActivityTimelineItem item = items.get(index);
                if (item.lesson()) {
                    lessonRanks.put(item.id(), index);
                } else {
                    assessmentRanks.put(item.id(), index);
                }
            }
            lessonRanksByClassGroup.put(classGroupId, lessonRanks);
            assessmentRanksByClassGroup.put(classGroupId, assessmentRanks);
        }
        request.setAttribute("classGroupLessonTimelineRankByClassGroup", lessonRanksByClassGroup);
        request.setAttribute("classGroupAssessmentTimelineRankByClassGroup", assessmentRanksByClassGroup);
    }

    private record ActivityTimelineItem(
            boolean lesson,
            long id,
            LocalDateTime startsAt,
            String title
    ) {
        static ActivityTimelineItem lesson(LessonView lesson) {
            return new ActivityTimelineItem(true, lesson.getId(), lesson.getStartsAtRaw(), lesson.getTitle());
        }

        static ActivityTimelineItem assessment(AssessmentView assessment) {
            return new ActivityTimelineItem(false, assessment.getId(), assessment.getAvailableFromRaw(), assessment.getTitle());
        }
    }

    private Map<String, Boolean> canManagePhysicalRoomByCode(
            SessionUser actor,
            Long sessionId,
            AccessProfileType profileType,
            Map<Long, List<PhysicalRoomView>> roomsByClassGroup,
            String sourceIp
    ) {
        Map<String, Boolean> permissions = new HashMap<>();
        for (List<PhysicalRoomView> rooms : roomsByClassGroup.values()) {
            for (PhysicalRoomView room : rooms) {
                permissions.computeIfAbsent(room.getCode(), code -> canManageRoom(
                        actor,
                        sessionId,
                        profileType,
                        code,
                        sourceIp
                ));
            }
        }
        return permissions;
    }

    private boolean canManageRoom(
            SessionUser actor,
            Long sessionId,
            AccessProfileType profileType,
            String roomCode,
            String sourceIp
    ) {
        try {
            return roomService.canManagePhysicalRoom(
                    actor.userId(),
                    sessionId,
                    profileType,
                    roomCode,
                    sourceIp
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
