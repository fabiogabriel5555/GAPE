# EduAll Class Groups And Content Blocks Integration

## Scope

This integration connects the Phase 8 class group, class group enrollment, teacher assignment and pedagogical block backend to EduAll dashboard screens.

## Template Analysis

The EduAll template was reviewed for course, student, instructor, dashboard, forms, tables, lesson/content and message patterns before implementation.

Relevant template areas:

- `docs/templates/Eduall/eduall/instructor-dashboard-my-courses.html`: managed learning dashboard pattern.
- `docs/templates/Eduall/eduall/student-dashbord-enrolled-courses.html`: enrollment cards and student-facing course state.
- `docs/templates/Eduall/eduall/lesson-details.html`: lesson/content accordion and ordered curriculum pattern.
- `docs/templates/Eduall/eduall/course-details.html`: course detail and curriculum-style layout.
- `docs/templates/Eduall/eduall/dashbord-courses.html`: dashboard course cards and progress blocks.
- `docs/templates/Eduall/eduall/assets/sass/components/_form.scss`: form control style.
- `docs/templates/Eduall/eduall/assets/sass/components/_table.scss`: table style.
- `docs/templates/Eduall/eduall/assets/sass/pages/othersPage/_dashboard.scss`: dashboard spacing and card conventions.

Existing GAPE fragments reused:

- `WEB-INF/fragments/dashboard-sidebar.jspf`
- `WEB-INF/fragments/dashboard-topbar.jspf`
- `WEB-INF/fragments/flash-messages.jspf`
- `WEB-INF/fragments/template-base-head.jspf`
- `WEB-INF/fragments/template-base-scripts.jspf`
- `WEB-INF/fragments/dashboard-footer.jspf`

## Routes

Class groups:

- `GET /learning/class-groups`
- `GET /learning/class-groups/new`
- `POST /learning/class-groups`
- `GET /learning/class-groups/{id}`
- `GET /learning/class-groups/{id}/edit`
- `POST /learning/class-groups/{id}`
- `POST /learning/class-groups/{id}/deactivate`
- `POST /learning/class-groups/{id}/delete`

Teacher assignment:

- `POST /learning/class-groups/{id}/teachers`

Class group enrollments:

- `POST /learning/class-groups/{id}/enrollments`
- `POST /learning/class-groups/{id}/enrollments/{studentId}/withdraw`
- `POST /student/enrollments/class-groups/{id}`
- `POST /student/enrollments/class-groups/{id}/withdraw`

Pedagogical blocks:

- `GET /learning/class-groups/{id}/blocks/new`
- `POST /learning/class-groups/{id}/blocks`
- `GET /learning/class-groups/{id}/blocks/{blockId}/edit`
- `POST /learning/class-groups/{id}/blocks/{blockId}`
- `POST /learning/class-groups/{id}/blocks/{blockId}/deactivate`
- `POST /learning/class-groups/{id}/blocks/{blockId}/delete`

## JSPs

New JSPs:

- `admin/admin/class-group/admin-class-groups.jsp`
- `admin/admin/class-group/admin-class-group-detail.jsp`
- `admin/admin/class-group/admin-class-group-form.jsp`
- `admin/admin/class-group/admin-content-block-form.jsp`
- `coordinator/coordinator/class-group/coordinator-class-groups.jsp`
- `coordinator/coordinator/class-group/coordinator-class-group-detail.jsp`
- `coordinator/coordinator/class-group/coordinator-class-group-form.jsp`
- `coordinator/coordinator/class-group/coordinator-content-block-form.jsp`
- `instructor/instructor/class-group/instructor-class-groups.jsp`
- `instructor/instructor/class-group/instructor-class-group-detail.jsp`
- `instructor/instructor/class-group/instructor-class-group-form.jsp`
- `instructor/instructor/class-group/instructor-content-block-form.jsp`

Adapted JSPs:

- `admin/admin/course/admin-course-detail.jsp`: adds class groups for the course and links to create/open groups.
- `admin/admin/subject/admin-subject-detail.jsp`: adds class groups for the subject and links to create/open groups.
- `student/student-enrolled-courses.jsp`: adds class group enrollment, withdrawal and visible pedagogical blocks under the student's active course-occurrence context.
- `WEB-INF/fragments/dashboard-sidebar.jspf`: adds Class Groups navigation and contextual child links.

## Backend Link

Mutating operations call the tested services:

- `ClassGroupService`
- `ClassGroupEnrollmentService`
- `ContentBlockService`

Read composition uses `LearningViewFactory` and DAOs:

- `ClassGroupDAO`
- `ClassGroupEnrollmentDAO`
- `ContentBlockDAO`
- `CourseDAO`
- `CourseSubjectDAO`
- `UserDAO`
- `TeachClassGroupDAO`

No SQL is placed in JSPs. JSPs only render view objects and submit forms to servlets.
Class group course/subject and content block class group are immutable after creation. Edit forms render those contexts as readonly values while posting hidden identifiers required by the existing commands.

## Authorization And CSRF

- `/learning/*` is now protected by `AuthorizationPolicy` for administrator, coordinator and teacher profiles.
- Mutating `/learning/*` requests are covered by `CsrfFilter`.
- The servlet delegates fine-grained context checks to the Phase 8 services.
- UI actions are hidden for completed class groups and for structural operations that teachers cannot perform.
- Student class group actions are routed through `ClassGroupEnrollmentService`, so enrollment requires an active enrollment in the same course occurrence covering the full class-group period, plus capacity, date, approval-policy and overlap checks.

## Visual Decisions

- List and detail pages reuse EduAll dashboard shell, metric cards, tables, badges, Select2 fields and Bootstrap modals.
- Class group context is shown with compact acronym tokens where applicable.
- Pedagogical blocks are rendered as ordered cards with a visible order badge, matching the curriculum/content pattern without introducing a new visual language.
- Student enrollment and withdrawal are kept on the class group detail screen so the enrolled-student list and actions are in one context.
- Students also see eligible class groups in `student/student-enrolled-courses.jsp`; enrolled students see active visible blocks for the class group.
- Teacher assignment is kept on the class group detail screen and uses the existing Phase 4 `teach_class_group` mechanism through `ClassGroupService`.

## Phase 8 Review Corrections

- `ClassGroupService` and `ClassGroupDAO` no longer allow changing a class group's course or subject after creation.
- `ContentBlockService` and `ContentBlockDAO` no longer allow moving a block to another class group after creation.
- `schema.sql` rejects class group and content block retargeting at trigger level.
- `schema.sql` includes `uq_content_block_active_order` using a generated active-only order key, protecting active block order uniqueness against races.
- Class group enrollment overlap is checked across every active class group in the same course-occurrence/subject context, not only the exact same class group.
- Student enrollment pages now include class group request/withdraw actions and visible active blocks. The request action is hidden when the student already has an active or pending class group enrollment in the same course-occurrence/subject context.
- Tests were expanded in `ClassGroupServiceTest`, `ClassGroupEnrollmentServiceTest`, `ContentBlockServiceTest`, `AuthorizationFilterTest`, `SchemaIntegrityTest` and `TemplateStructureTest`.

## Manual Test Checklist

- Open `/learning/class-groups` as an administrator and verify visible class groups.
- Create a class group for an active course-subject association.
- Try to create a class group where the subject is not integrated in the course and verify the error.
- Try `minStudents > maxStudents` and verify the validation error.
- Edit a class group and verify context, state, capacity and dates.
- Enroll a student whose active course enrollment targets the class group's occurrence and covers the full class-group period.
- Try enrolling a student without a covering enrollment in that course occurrence and verify the error.
- Try exceeding max capacity and verify the error.
- Withdraw an active student from the class group.
- Assign an active teacher.
- Create a scheduled content block with `availableFrom`.
- Try creating a scheduled block without `availableFrom` and verify the error.
- Try duplicating an active block order and verify the error.
- Edit a block order and verify the visual order changes.
- Deactivate a block and verify edit/deactivate/delete actions disappear for that block.
- Complete a class group and verify mutating actions disappear.

## Verification Run

Initial implementation verification:

- `mvn -q -DskipTests package`: passed.
- `mvn -q "-Dtest=TemplateAssetReferenceTest,TemplateStructureTest" test`: passed.
- `mvn -q "-Dtest=CsrfFilterTest,AuthorizationFilterTest,AuthenticationFilterTest" test`: passed.

Final verification:

- `mvn -q -DskipTests package`: passed after the final JSP/servlet changes.
- Browser validation ran on `http://localhost:18080/GAPE` with the full seed and `admin@gape.local`.
- Created class group `BD-FLOW-364964` from `/learning/class-groups/new`.
- Verified `minStudents > maxStudents` shows `Minimum students cannot exceed maximum students.`
- Verified the new class group appears in `/learning/class-groups` with context, capacity, block count, state and actions.
- Enrolled `Student Three` in the class group and verified the enrolled-student table.
- Verified `max_students = 1` blocks the second enrollment with `Class group maximum capacity exceeded.`
- Withdrew the active student and verified the table state changes to `Withdrawn`.
- Verified scheduled content blocks require `availableFrom`.
- Created two content blocks and verified visual order `1` then `2`.
- Verified duplicate active block order is blocked with `Another active content block already uses this order.`
- Checked `/admin/courses/30` and `/admin/subjects/42`; both expose the Class Groups section and related links.
- Checked mobile viewport `390x844` for `/learning/class-groups` and `/learning/class-groups/57`; no horizontal overflow or console errors were detected.
- `mvn test`: passed on 13-06-2026 at 00-25-00 Europe/Lisbon with 233 tests, 0 failures, 0 errors and 0 skipped.

The in-app Browser screenshot calls timed out in this run. Validation was completed through rendered DOM state, route transitions, form submissions, flash messages, viewport checks and console-error checks.
