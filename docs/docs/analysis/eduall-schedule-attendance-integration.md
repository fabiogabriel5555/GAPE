# EduAll schedule and attendance integration

## Scope

This integration connects the Phase 12 schedule, attendance and absence justification backend to the EduAll JSP template without replacing the existing visual system.

Covered flows:

- Staff calendar and schedule events.
- Student calendar with schedule events and lesson access.
- Staff attendance registration.
- Student attendance consultation and absence notifications.
- Student justification submission.
- Staff justification approval and rejection.

## Template analysis

The EduAll template already had calendar and lesson surfaces:

- `WEB-INF/views/learning/lesson-list.jsp` for staff lessons and `/learning/calendar`.
- `student/student/calendar/student-calendar.jsp` for student schedule.
- `WEB-INF/fragments/dashboard-sidebar.jspf` for shared dashboard navigation.
- `WEB-INF/fragments/student-dashboard-sidebar.jspf` for the student dashboard shell.
- `WEB-INF/fragments/flash-messages.jspf`, `dashboard-topbar.jspf`, `dashboard-footer.jspf`, `student-dashboard-start.jspf` and `student-dashboard-end.jspf` for the page frame.

The implementation preserves the same card, table, filter, icon and flash-message patterns:

- `bg-white`, `rounded-10`, `border-neutral-30`, `text-neutral-*`.
- Phosphor icons already used by EduAll.
- Inline filters with `form-select` and `onchange`.
- Tables inside `overflow-x-auto`.
- Student line cards inside the existing student panel style.

## Routes

Calendar and schedule:

- `GET /learning/calendar` lists visible schedule events and lessons.
- `POST /learning/calendar` creates schedule events for selected class groups and can link events to lessons or assessments.
- `GET /student/calendar` lists the student's visible schedule events and lessons.

Attendance and justifications:

- `GET /learning/attendance` lists visible attendance records and justifications.
- `POST /learning/attendance` creates an attendance record.
- `POST /learning/attendance/justifications/{id}/approve` approves a submitted justification.
- `POST /learning/attendance/justifications/{id}/reject` rejects a submitted justification.
- `GET /student/attendance` lists the student's attendance records and justifications.
- `POST /student/attendance/justifications` submits a justification for the student's own compatible record.

## Backend links

The JSP layer is backed by:

- `ScheduleEventService` for schedule event visibility and creation.
- `AttendanceRecordService` for attendance creation and visibility.
- `AbsenceJustificationService` for submission and processing.

Additional visible-list service methods were added for the UI:

- `AttendanceRecordService.listVisibleAttendance`.
- `AbsenceJustificationService.listVisibleJustifications`.

The DAO visibility queries follow the same context model used by schedule events:

- Teacher: active taught class group plus `MANAGE_LEARNING`.
- Coordinator: active coordinated subject plus `MANAGE_LEARNING`.
- Administrator: `MANAGE_ALL` global or `MANAGE_LEARNING` scoped to organization, organic unit, course, subject or class group.
- Student: own attendance and justifications only.

## Pages changed or created

Changed:

- `WEB-INF/views/learning/lesson-list.jsp`
- `student/student/calendar/student-calendar.jsp`
- `WEB-INF/fragments/dashboard-sidebar.jspf`
- `WEB-INF/fragments/student-dashboard-sidebar.jspf`

Created:

- `WEB-INF/views/learning/attendance.jsp`
- `student/student/attendance/student-attendance.jsp`

The calendar page now includes schedule events, filters, class group context, reminders, state badges and event creation. Staff can create generic events and link events to a lesson or assessment. The filter exposes only implemented modes: all items, lessons and events. The existing lesson table remains available so meeting links continue to route through `/student/lessons/{id}/access`.

The student calendar receives a single ordered timeline from `StudentLessonServlet`, combining lessons and schedule events by start date.

The attendance page includes:

- Staff record form with lesson id, student id, status, source, check-in, check-out and notes.
- Attendance table with status, state, source and permanence.
- Justification processing table with approve/reject actions.
- Creation options omit `justified` and `automatic`; `justified` is only produced by approved justification processing, and automatic attendance is reserved for platform integration.

The student attendance page includes:

- Absence notification count.
- Student attendance cards.
- Multipart justification forms only for records that can still be justified.
- File attachments are stored through `JustificationAttachmentStorage` under `uploads/justifications` and persisted as relative paths.
- Submitted justification history and processing state.

## Security

Mutating staff routes under `/learning/*` were already protected by `CsrfFilter`.

The student justification route is now protected by adding:

- `/student/attendance`

Business permissions remain enforced in services. JSP visibility is not treated as an authorization boundary.

Submission and processing timestamps are generated by `AbsenceJustificationService` from the application clock. Request-provided dates are ignored for those operations.

## Tests

Executed:

```bash
mvn -q -DskipTests compile
mvn -q test "-Dtest=TemplateStructureTest,TemplateAssetReferenceTest"
mvn -q test "-Dtest=ScheduleEventServiceTest,AttendanceRecordServiceTest,AbsenceJustificationServiceTest"
```

Additional focused regression run:

```bash
mvn test "-Dtest=ScheduleEventServiceTest,AttendanceRecordServiceTest,AbsenceJustificationServiceTest,TemplateStructureTest"
```

Result on 27-06-2026: 63 tests, 0 failures, 0 errors.

Full regression run:

```bash
mvn test
```

Result on 27-06-2026: 515 tests, 0 failures, 0 errors.
