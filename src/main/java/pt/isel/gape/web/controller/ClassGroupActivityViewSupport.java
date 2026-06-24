package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.PhysicalRoomDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.learning.service.PhysicalRoomService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.PhysicalRoomView;

final class ClassGroupActivityViewSupport {

    private final LessonService lessonService;
    private final PhysicalRoomService roomService;
    private final PhysicalRoomDAO physicalRoomDAO;
    private final LearningViewFactory viewFactory;

    ClassGroupActivityViewSupport(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new LessonService(connectionProvider, clock),
                new PhysicalRoomService(connectionProvider, clock),
                new PhysicalRoomDAO(connectionProvider),
                new LearningViewFactory(
                        new OrganizationDAO(connectionProvider),
                        new OrganicUnitDAO(connectionProvider),
                        new CourseDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new CourseSubjectDAO(connectionProvider),
                        new EnrollmentDAO(connectionProvider),
                        new ClassGroupDAO(connectionProvider),
                        new ClassGroupEnrollmentDAO(connectionProvider),
                        new ContentBlockDAO(connectionProvider),
                        new UserDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                )
        );
    }

    ClassGroupActivityViewSupport(
            LessonService lessonService,
            PhysicalRoomService roomService,
            PhysicalRoomDAO physicalRoomDAO,
            LearningViewFactory viewFactory
    ) {
        this.lessonService = lessonService;
        this.roomService = roomService;
        this.physicalRoomDAO = physicalRoomDAO;
        this.viewFactory = viewFactory;
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
        Map<Long, List<PhysicalRoomView>> roomsByClassGroup = roomsByClassGroup(lessonsByClassGroup);
        request.setAttribute("classGroupLessonsByClassGroup", lessonsByClassGroup);
        request.setAttribute("classGroupRoomsByClassGroup", roomsByClassGroup);
        request.setAttribute(
                "canManagePhysicalRoomByCode",
                canManagePhysicalRoomByCode(actor, sessionId, profileType, roomsByClassGroup, request.getRemoteAddr())
        );
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

    private Map<Long, List<PhysicalRoomView>> roomsByClassGroup(
            Map<Long, List<LessonView>> lessonsByClassGroup
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
            result.put(entry.getKey(), List.copyOf(roomsByCode.values()));
        }
        return result;
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
