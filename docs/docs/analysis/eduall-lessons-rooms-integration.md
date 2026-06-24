# EduAll lessons and physical rooms integration

## Scope
This integration connects the Phase 10 lessons and physical rooms backend to the EduAll dashboard UI without changing the approved visual language.

## EduAll template areas reviewed
- `docs/templates/Eduall/eduall/lesson-details.html`: reference for lesson detail, access information and lesson content layout.
- `docs/templates/Eduall/eduall/book-online-class.html`: reference for online lesson booking/form flow.
- `docs/templates/Eduall/eduall/events.html`: reference for calendar/listing by date, time and location.
- `docs/templates/Eduall/eduall/event-details.html`: reference for event/lesson detail metadata.
- `docs/templates/Eduall/eduall/course-details.html`: reference for lessons inside course curriculum blocks.
- `docs/templates/Eduall/eduall/student-dashbord-enrolled-courses.html`: reference for student course/class group lesson visibility.
- `docs/templates/Eduall/eduall/instructor-dashboard-my-courses.html` and `dashbord-courses.html`: reference for instructor/admin cards and counters.

## Application files connected
- `src/main/java/pt/isel/gape/web/controller/LessonManagementServlet.java`
- `src/main/java/pt/isel/gape/web/controller/PhysicalRoomManagementServlet.java`
- `src/main/java/pt/isel/gape/web/controller/StudentLessonServlet.java`
- `src/main/java/pt/isel/gape/web/controller/ClassGroupManagementServlet.java`
- `src/main/java/pt/isel/gape/web/controller/StudentEnrollmentServlet.java`
- `src/main/java/pt/isel/gape/web/view/LessonView.java`
- `src/main/java/pt/isel/gape/web/view/LessonFormData.java`
- `src/main/java/pt/isel/gape/web/view/PhysicalRoomView.java`
- `src/main/java/pt/isel/gape/web/view/PhysicalRoomFormData.java`
- `src/main/webapp/WEB-INF/views/learning/lesson-list.jsp`
- `src/main/webapp/WEB-INF/views/learning/lesson-detail.jsp`
- `src/main/webapp/WEB-INF/views/learning/lesson-form.jsp`
- `src/main/webapp/WEB-INF/views/learning/room-list.jsp`
- `src/main/webapp/WEB-INF/views/learning/room-detail.jsp`
- `src/main/webapp/WEB-INF/views/learning/room-form.jsp`
- `src/main/webapp/student/student/lesson/student-lessons.jsp`
- `src/main/webapp/student/student/calendar/student-calendar.jsp`
- `src/main/webapp/student/student/class-group/student-class-group-detail.jsp`
- `src/main/webapp/WEB-INF/fragments/class-group-detail-page.jspf`
- `src/main/webapp/student/student-enrolled-courses.jsp`
- `src/main/webapp/WEB-INF/fragments/dashboard-sidebar.jspf`

## Routes
- Manager lessons:
  - `GET /learning/lessons`
  - `GET /learning/lessons?classGroupId={id}`
  - `GET /learning/lessons/new`
  - `POST /learning/lessons`
  - `GET /learning/lessons/{lessonId}`
  - `GET /learning/lessons/{lessonId}/edit`
  - `POST /learning/lessons/{lessonId}`
  - `POST /learning/lessons/{lessonId}/cancel`
  - `POST /learning/lessons/{lessonId}/complete`
  - `POST /learning/lessons/{lessonId}/delete`
- Calendar:
  - `GET /learning/calendar`
  - `GET /learning/calendar?classGroupId={id}`
  - `GET /student/calendar`
- Physical rooms:
  - `GET /learning/rooms`
  - `GET /learning/rooms/new`
  - `POST /learning/rooms`
  - `GET /learning/rooms/{code}`
  - `GET /learning/rooms/{code}/edit`
  - `POST /learning/rooms/{code}`
  - `POST /learning/rooms/{code}/archive`
  - `POST /learning/rooms/{code}/delete`
- Student lessons:
  - `GET /student/lessons`
  - `GET /student/lessons/{lessonId}/access`

## Permission behavior
- `/learning/lessons` is available to administrator, coordinator and teacher profiles through the existing `/learning/*` authorization rule.
- Lesson create/edit/cancel/complete/delete is still enforced by `LessonService`, including administrator, coordinator and teacher context checks.
- `/learning/calendar` reuses the lessons listing as an EduAll schedule view, ordered by date and available from the dashboard menu after Message. It is personal/contextual: administrators do not see every managed lesson; they only see lessons where the same user is assigned as teacher or coordinator. Coordinators see lessons from coordinated subjects, teachers see lessons from taught class groups.
- `/student/calendar` exposes the same calendar menu to students and only lists lessons from active class group enrollments.
- `/learning/rooms` is exposed for administrator, coordinator and teacher dashboards. Administrators and coordinators keep room management actions in their authorized organization context. Teachers receive read-only room access limited to organizations where they teach active learning contexts.
- `PhysicalRoomService` remains the source of truth for room read and management permissions. The servlet uses separate checks for listing/detail access and create/edit/archive/delete buttons.
- `/student/lessons` is available to students. Meeting URLs are exposed through `/student/lessons/{lessonId}/access`, which calls `LessonService.getLesson`, records access-link audit logging and then redirects to the external meeting URL.
- `ClassGroupManagementServlet` feeds the Class Groups listing with lessons and physical rooms per class group. The list JSP exposes a `Show` toggle before `Detail`, with lesson and room actions filtered by the same backend permissions.

## Manual test roteiro
1. Online lesson
   - Open `/learning/lessons/new`.
   - Choose a class group and pedagogical block.
   - Set type `online`.
   - Fill title, start/end dates and an HTTPS meeting link.
   - Save.
   - Expected: lesson detail opens, type badge is Online and the meeting button is visible.

2. Presential lesson
   - Create an active physical room in `/learning/rooms/new`.
   - Open `/learning/lessons/new`.
   - Select the same organization class group and set type `onsite`.
   - Choose the physical room and save.
   - Expected: lesson detail opens, room code is shown and no meeting URL is required.

3. Room
   - Open `/learning/rooms`.
   - Filter by organization.
   - Open a room detail.
   - Expected: capacity, organization/unit, location and scheduled lessons are visible.

4. Conflict
   - Create a second presential or hybrid lesson using the same room and overlapping period.
   - Expected: form rejects with "The physical room already has a lesson in the selected period."

5. Calendar/listing
   - Open `/learning/calendar`.
   - Filter by class group.
   - Expected: lessons are ordered by date and show type, state, access summary and detail action.
   - Validate with an administrator that has global management permissions but no teacher/coordinator assignments.
   - Expected: the calendar is empty, even if `/learning/lessons` lists managed lessons.

6. Student calendar
   - Open `/student/calendar`.
   - Expected: only lessons from class groups where the student has an active enrollment appear.

7. Student access
   - Enroll a student in a class group with visible content blocks.
   - Open `/student/enrollments` and `/student/lessons`.
   - Expected: lessons appear under the class group blocks and in the student lesson list; the meeting button opens `/student/lessons/{lessonId}/access` before redirecting to the external provider.

## Notes
- The UI does not duplicate backend validations. It uses client-side filtering for convenience, while `LessonService` and `PhysicalRoomService` keep the authoritative checks for organization, room availability, conflicts, URLs and permissions.
- The "calendar" in this phase is an EduAll-style ordered agenda/listing. It does not assume a separate `schedule_event` workflow.
- Calendar visibility intentionally uses `LessonService.listPersonalCalendarLessons`, not the broader class group management visibility used by `/learning/lessons`.
