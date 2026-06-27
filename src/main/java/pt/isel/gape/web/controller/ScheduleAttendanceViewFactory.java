package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.User;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.web.view.AbsenceJustificationView;
import pt.isel.gape.web.view.AttendanceRecordView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.LessonView;
import pt.isel.gape.web.view.ScheduleEventView;

final class ScheduleAttendanceViewFactory {

    private final ClassGroupDAO classGroupDAO;
    private final LessonDAO lessonDAO;
    private final UserDAO userDAO;
    private final LearningViewFactory learningViewFactory;

    ScheduleAttendanceViewFactory(
            ClassGroupDAO classGroupDAO,
            LessonDAO lessonDAO,
            UserDAO userDAO,
            LearningViewFactory learningViewFactory
    ) {
        this.classGroupDAO = classGroupDAO;
        this.lessonDAO = lessonDAO;
        this.userDAO = userDAO;
        this.learningViewFactory = learningViewFactory;
    }

    List<ScheduleEventView> scheduleEventViews(List<ScheduleEvent> events) {
        return events.stream()
                .map(this::scheduleEventView)
                .toList();
    }

    ScheduleEventView scheduleEventView(ScheduleEvent event) {
        return ScheduleEventView.from(event, event.classGroupIds().stream()
                .map(this::classGroupLabel)
                .toList());
    }

    List<AttendanceRecordView> attendanceRecordViews(
            List<AttendanceRecord> records,
            Set<Long> justifiedAttendanceRecordIds
    ) {
        return records.stream()
                .map(record -> attendanceRecordView(record, justifiedAttendanceRecordIds.contains(record.id())))
                .toList();
    }

    AttendanceRecordView attendanceRecordView(AttendanceRecord record, boolean hasJustification) {
        User student = user(record.studentUserId());
        return AttendanceRecordView.from(
                record,
                lessonView(record.lessonId()),
                student == null ? null : student.name(),
                student == null ? null : student.email(),
                hasJustification
        );
    }

    List<AbsenceJustificationView> justificationViews(
            List<AbsenceJustification> justifications,
            List<AttendanceRecordView> attendanceRecords
    ) {
        Map<Long, AttendanceRecordView> recordById = attendanceRecords.stream()
                .collect(Collectors.toMap(AttendanceRecordView::getId, Function.identity(), (first, second) -> first));
        return justifications.stream()
                .map(justification -> AbsenceJustificationView.from(
                        justification,
                        recordById.get(justification.attendanceRecordId())
                ))
                .toList();
    }

    private String classGroupLabel(long classGroupId) {
        try {
            ClassGroup classGroup = classGroupDAO.findById(classGroupId).orElse(null);
            if (classGroup == null) {
                return "Class group " + classGroupId;
            }
            ClassGroupView view = learningViewFactory.classGroupView(classGroup);
            return view.getCode() + " - " + view.getSubjectName();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load schedule event class group", exception);
        }
    }

    private LessonView lessonView(long lessonId) {
        try {
            Lesson lesson = lessonDAO.findById(lessonId).orElse(null);
            return lesson == null ? null : learningViewFactory.lessonView(lesson);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load attendance lesson", exception);
        }
    }

    private User user(long userId) {
        try {
            return userDAO.findById(userId).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load attendance user", exception);
        }
    }
}
