# EduAll Courses And Subjects Integration

## Scope

This integration connects the Phase 7 learning backend to EduAll screens for courses, subjects, course-subject associations and student enrollment self-service.

## Template Analysis

The EduAll course, catalog, detail, student dashboard, instructor dashboard, table, filter, card and form pages were reviewed before implementation.

Relevant EduAll pages and assets:

- `docs/templates/Eduall/eduall/course.html`: course card catalog.
- `docs/templates/Eduall/eduall/course-list-view.html`: list/filter catalog variant.
- `docs/templates/Eduall/eduall/course-details.html`: course detail and curriculum-style layout.
- `docs/templates/Eduall/eduall/dashbord-courses.html`: dashboard course cards and progress layout.
- `docs/templates/Eduall/eduall/student-dashbord-enrolled-courses.html`: student enrolled courses pattern.
- `docs/templates/Eduall/eduall/instructor-dashboard-my-courses.html`: managed-course dashboard pattern.
- `docs/templates/Eduall/eduall/assets/css/dataTables.dataTables.min.css`: table styling.
- `docs/templates/Eduall/eduall/assets/sass/components/_form.scss`, `_table.scss`, `_dataTable.scss`: form/table conventions.
- `docs/templates/Eduall/eduall/assets/sass/pages/othersPage/_course-details.scss`, `_course-list-view.scss`, `_dashboard.scss`: course/detail/dashboard layout conventions.

Existing GAPE patterns reused:

- `WEB-INF/fragments/dashboard-sidebar.jspf`
- `WEB-INF/fragments/dashboard-topbar.jspf`
- `WEB-INF/fragments/flash-messages.jspf`
- `WEB-INF/fragments/template-base-head.jspf`
- `WEB-INF/fragments/template-base-scripts.jspf`
- `admin/admin/organization/*` for CRUD tables, forms, detail cards and confirmation modals.

## Routes

Public catalog:

- `GET /courses`: active course catalog with search, organization filter and type filter.
- `GET /courses/{id}`: active course detail, active curricular subjects and optional student enrollment actions.

Admin courses:

- `GET /admin/courses`
- `GET /admin/courses/new`
- `POST /admin/courses`
- `GET /admin/courses/{id}`
- `GET /admin/courses/{id}/edit`
- `POST /admin/courses/{id}`
- `POST /admin/courses/{id}/archive`
- `POST /admin/courses/{id}/delete`

Course-subject association:

- `GET /admin/courses/{id}/subjects/new`
- `POST /admin/courses/{id}/subjects`
- `GET /admin/courses/{id}/subjects/{subjectId}/edit`
- `POST /admin/courses/{id}/subjects/{subjectId}`
- `POST /admin/courses/{id}/subjects/{subjectId}/archive`
- `POST /admin/courses/{id}/subjects/{subjectId}/delete`

Admin subjects:

- `GET /admin/subjects`
- `GET /admin/subjects/new`
- `POST /admin/subjects`
- `GET /admin/subjects/{id}`
- `GET /admin/subjects/{id}/edit`
- `POST /admin/subjects/{id}`
- `POST /admin/subjects/{id}/assign-coordinator`
- `POST /admin/subjects/{id}/archive`
- `POST /admin/subjects/{id}/delete`

Student self-service:

- `GET /student/enrollments`
- `POST /student/enrollments/courses/{courseId}`
- `POST /student/enrollments/courses/{courseId}/withdraw`
- `POST /student/enrollments/courses/{courseId}/subjects/{subjectId}`
- `POST /student/enrollments/courses/{courseId}/subjects/{subjectId}/withdraw`

## JSPs

New administrative JSPs:

- `admin/admin/course/admin-courses.jsp`
- `admin/admin/course/admin-course-form.jsp`
- `admin/admin/course/admin-course-detail.jsp`
- `admin/admin/course/admin-course-subject-form.jsp`
- `admin/admin/subject/admin-subjects.jsp`
- `admin/admin/subject/admin-subject-form.jsp`
- `admin/admin/subject/admin-subject-detail.jsp`

Adapted public/student JSPs:

- `courses.jsp`: dynamic public catalog.
- `course-details.jsp`: dynamic public detail.
- `course.jsp`: redirects to `/courses`.
- `course-list-view.jsp`: redirects to `/courses`.
- `student/student-enrolled-courses.jsp`: dynamic student enrollment dashboard.
- `admin/admin-courses.jsp`: redirects to `/admin/courses`.

## Backend Link

Mutating operations use the tested services:

- `CourseService`
- `SubjectService`
- `CourseSubjectService`
- `EnrollmentService`

Read-only page composition uses DAO methods for catalog and enrollment state:

- `CourseDAO.findCatalogCourses`
- `CourseDAO.findActiveById`
- `SubjectDAO.findActiveByOrganization`
- `SubjectDAO.findByCoordinator`
- `CourseSubjectDAO.findActiveByCourse`
- `EnrollmentDAO.findCourseEnrollmentsByStudent`
- `EnrollmentDAO.findSubjectEnrollmentsByStudent`

`SessionManager` now exposes:

- `gape.auth.canManageCourses`
- `gape.auth.canManageSubjects`
- `gape.auth.canManageEnrollments`

The sidebar now points course navigation to real routes instead of the old static `admin/admin-courses.jsp` mock.

## Image Handling

Courses and subjects now follow the same upload pattern used by users and organizations:

- `course.photo` and `subject.photo` store the relative media path.
- Supported uploaded image formats are converted to WebP by `ProfilePhotoStorage`.
- Course photos are stored as `courses/{id}/profile.webp`.
- Subject photos are stored as `subjects/{id}/profile.webp`.
- Create and edit forms are multipart and include EduAll-style preview/cancel controls.
- Admin lists, admin details, the public catalog, course detail pages and student enrollment cards load photos through `/media/{path}?v=${mediaCacheVersion}`.
- EduAll `course-img*.png` thumbnails remain only as fallback images when no uploaded photo exists or a media file cannot be loaded.

## Visual Decisions

Course cards use EduAll course-card classes and render uploaded WebP course images when available, falling back to existing `course-img*.png` thumbnails.

Subject admin rows and detail headers use compact thumbnails so discipline images remain visible without changing the EduAll table structure.

Admin pages use the consolidated GAPE/EduAll dashboard shell, metric cards, tables, Phosphor icons, Select2 fields and Bootstrap confirmation modals.

The course detail curriculum uses simple bordered subject rows rather than introducing a new tree or lesson component, because modules/content are outside the current Phase 7 model.

## Manual Test Checklist

- Create a valid course as administrator.
- Create a valid course with an uploaded image and verify the saved WebP photo appears in the admin list, admin detail, public catalog and course detail.
- Try creating a course without organization and verify the form error.
- Create a valid subject as administrator.
- Create a valid subject with an uploaded image and verify the saved WebP photo appears in the admin list and admin detail.
- Assign a coordinator to a subject.
- Associate a subject with a course using year and period together.
- Repeat the association and verify the duplicate error.
- Try saving year without period and verify the validation message.
- Open `/courses` and verify active courses render as EduAll cards.
- Open `/courses/{id}` and verify active subjects render in the curriculum.
- Enroll as a student in a course.
- Withdraw as a student from a course.
- Enroll as a student in a subject only after being enrolled in the course.
- Withdraw as a student from a subject.

## Verification Run

Executed on 2026-06-10.

- `mvn -q -DskipTests package`: passed.
- `mvn test "-Dtest=TemplateAssetReferenceTest,TemplateStructureTest"`: passed, 26 tests.
- `mvn test "-Dtest=ProfilePhotoStorageTest,TemplateAssetReferenceTest,TemplateStructureTest"`: passed, 31 tests.
- `mvn test`: passed, 187 tests.

The first full `mvn test` run reached 181 passing tests and exposed two structural template failures, both fixed by whitelisting the new servlet endpoints and restoring the required bottom-footer signature.

Manual Browser verification was executed against a temporary Tomcat 11 instance at `http://localhost:18080/GAPE` using the current `target/gape` deployment:

- Opened `/courses` and verified the public EduAll catalog rendered active courses without login.
- Opened `/courses/30` and verified detail data, organization, organic unit, curriculum rows and sign-in enrollment state.
- Logged in as `admin@gape.local` and created course `Curso Browser QA 296450`; the detail page returned `Course created successfully.`.
- Created subject `Disciplina Browser QA 296450` with coordinator assignment; the detail page returned `Subject created successfully.`.
- Associated the subject with the created course using curricular year and period; the course detail returned `Subject associated with course.`.
- Repeated the same association and verified the form returned `This subject is already associated with the selected course.`.
- Logged in as `student@gape.local`, opened `/student/enrollments`, enrolled in the created course and verified `Course enrollment completed.`.
- Opened the created public course detail, enrolled in the associated subject and verified `Subject enrollment completed.`.
- Withdrew from the subject and verified `Subject withdrawal completed.`.
- Withdrew from the course and verified `Course withdrawal completed.`.
- Checked the course detail DOM for horizontal overflow, broken images and browser console errors; none were detected in the default `1280x720` viewport.

The in-app Browser screenshot call timed out during this run, so the visual evidence was based on page rendering, DOM state, route transitions, flash messages, image load checks and console-error checks.

Additional image verification on 2026-06-10 after adding course and subject photos:

- Packaged the current WAR with `mvn package -DskipTests`.
- Opened `http://localhost:18080/GAPE/courses` in the in-app Browser and verified the catalog rendered 2 course cards, no internal error and loaded fallback course thumbnails.
- Opened `http://localhost:18080/GAPE/courses/30` in the in-app Browser and verified the course detail page rendered `Engenharia Informatica` with a loaded course image and no console errors.
- Authenticated as `admin@gape.local` over the local app and verified `GET /admin/courses/new` returned 200 with `multipart/form-data`, `courseImage` upload input and `courseImagePreview`.
- Authenticated as `admin@gape.local` over the local app and verified `GET /admin/subjects/new` returned 200 with `multipart/form-data`, `subjectImage` upload input and `subjectImagePreview`.
