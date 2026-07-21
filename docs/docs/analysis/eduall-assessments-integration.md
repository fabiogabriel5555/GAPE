# EduAll Assessments Integration

## Scope

This integration adds the assessment flow on top of the tested learning backend:

- questionnaire and exam listing for managers;
- assessment creation and editing;
- question creation and editing;
- option creation and editing;
- student answer page;
- student result and review page;
- automatic/manual correction page;
- assessment menu entries;
- assessment cards inside class group learning plans.
- simplified Class Group Detail `Add Content` entries for `Forms` and `Exams`, each with `Live` and `In-Person` inline creation paths.

The UI keeps the EduAll visual system and adopts only the LearnWorlds patterns that fit the current template: a focused question builder, a searchable question palette, a central editable canvas and compact inline controls.

## Existing EduAll Surfaces Reviewed

Relevant manager surfaces:

- `WEB-INF/fragments/dashboard-sidebar.jspf`
- `WEB-INF/fragments/class-group-detail-page.jspf`
- `WEB-INF/fragments/class-group-activities-panel.jspf`
- `WEB-INF/views/learning/lesson-list.jsp`
- `WEB-INF/views/learning/lesson-form.jsp`
- `WEB-INF/views/learning/lesson-detail.jsp`
- static quiz attempt pages under `admin/`, `coordinator/` and `instructor/`

Relevant student surfaces:

- `WEB-INF/fragments/student-dashboard-sidebar.jspf`
- `WEB-INF/fragments/student-dashboard-start.jspf`
- `WEB-INF/fragments/student-class-group-card.jspf`
- `student/student/class-group/student-class-groups.jsp`
- `student/student/class-group/student-class-group-detail.jsp`
- `student/student/lesson/student-lessons.jsp`
- static quiz/assignment pages under `student/`

Backend surfaces connected:

- `AssessmentDAO`, `QuestionDAO`, `QuestionOptionDAO`, `AttemptDAO`, `ResponseDAO`
- `AssessmentService`, `QuestionService`, `QuestionOptionService`, `AttemptService`, `ResponseService`, `CorrectionService`
- new servlets `AssessmentManagementServlet` and `StudentAssessmentServlet`
- view models `AssessmentView`, `QuestionView`, `QuestionOptionView`, `AttemptView`, `ResponseView`, `AssessmentFormData`

## Implemented Pages

Manager:

- `/learning/assessments`
- `/learning/assessments/new`
- `/learning/assessments/{id}/edit`
- `/learning/assessments/{id}`
- `/learning/assessments/{id}/attempts`
- `/learning/assessments/{id}/attempts/{attemptId}`
- `/learning/assessments/{id}/attempts/{attemptId}/responses/{responseId}/attachment`

Student:

- `/student/lessons` is the single student **Lessons & Assessments** catalogue; `/student/assessments` remains a compatibility redirect to its assessments section.
- `/student/assessments/attempts/{attemptId}`
- `/student/assessments/attempts/{attemptId}/result`
- `/student/assessments/responses/{responseId}/attachment`

Class group integration:

- manager learning plan rows now include assessment activities;
- block toolbar links directly to assessment creation with `contentBlockId`;
- Add Content uses two assessment categories, `Forms` and `Exams`, instead of separate live/in-person form/exam categories;
- each Forms/Exams card keeps only the `Live` and `In-Person` choices, uses compact cards without redundant builder labels, shows the `Assessment Details` fields inside the Add Content modal, and posts to `/learning/assessments` with `type`, `mode`, `correctionMode`, `subjectId` and `contentBlockId`;
- successful inline creation reloads the current Class Group Detail URL with the originating block anchor, preserving session URL rewriting when present; question and option construction remains available from the created assessment row;
- block activity rows use the pedagogical content order first and keep assessments without an availability date after ordered block content, instead of placing forms/exams at the top of the block;
- assessment Builder and Settings & Attempts views expose contextual sidebar entries under `Assessments`, matching the existing nested navigation pattern used by detail pages;
- the assessment Builder focuses on the question editor only; the previous question index and setup side panels were removed, with `Add Question` promoted to the header action next to `Back`;
- question `code`, `order`, `type` and `state` are system-managed in the Builder; managers edit only the question content, score, expected answer and required flag;
- question score inputs are bounded by the assessment maximum grade, cannot be negative, and the backend rejects active-question totals above the assessment maximum grade;
- option `state` is system-managed and option `order` is changed by drag-and-drop, including pointer interactions for touch devices; managers edit option text and select the correct answer through the natural radio/checkbox control shown at the right edge of each option row;
- option-compatible questions are created with default options and the backend prevents removing the last active option; rating questions do not use options, require a configured expected value and are automatically scored against that value;
- question cards render type-specific answer structures: the question text is edited directly inside that structure, score/required stay in the question header, changes are saved automatically, single choice uses radio-style options, multiple choice uses checkbox-style options, rating uses a graphical scale with configurable shape, integer/fractional step, maximum value and expected answer selection, paragraph uses a taller lined expected-answer editor, and text/file questions expose matching controls;
- existing active option rows have only an option delete action; option text/correctness/order edits are persisted automatically with the question form, while new rows use an AJAX `Add Option` flow with inline empty-text validation and no full-page reload; single-choice correctness is represented as a radio group, multiple-choice correctness as checkboxes, the first selected correct answer is labelled in-place, and inactive options are not rendered in the Builder;
- Builder headers show the assessment `Maximum Grade`; when active question scores do not total exactly that maximum, a yellow warning explains that leaving the page will automatically distribute the missing or extra score across active questions without allowing negative question scores;
- file upload questions store accepted file formats in question configuration, expose those formats beside `Score`, and student attempts use real multipart file upload through the same `PdfUploadService` validations used by pedagogical content files;
- rating questions are value-based for managers and students: managers choose `Design: Stars/Circles/Hearts - Integer/Fractional`, set a maximum scale value from 1 to 100 and select the expected answer through the matching graphical picker; students answer through the same picker pattern, including half-step selection when the fractional design is enabled;
- question options are always visible inside option-compatible questions, while the redundant `Options` toggle was removed;
- any started attempt makes the Builder read-only and shows `Assessment questions cannot change after attempts have started`;
- editable questions use an icon-only `Delete` action backed by the question delete flow instead of the previous deactivate-only action;
- `/learning/assessments/{id}/attempts` now combines assessment settings and attempt/correction queue in separated sections, matching the Class Group Detail pattern of related functions on one page;
- the Add Question modal uses a fixed-size builder palette with category filters and visual previews for choice, text, file and rating question types; internal/non-creatable types are not offered for new questions or accepted by direct creation, and selecting a type redirects back to the Builder anchored on the new question without a success flash;
- student study path includes active block assessments with a start/continue action.
- subject-level exams require explicit applicable class groups through `assessment_class_group`; online student listing and automatic enrollment use those groups instead of subject-wide access.
- student assessment POST routes are CSRF-protected; onsite assessments are not listed for online execution and are rejected by the attempt/response services.
- response attachments are downloaded through authorized servlet routes. Hidden attachment fields can only preserve an existing response attachment; first uploads must come from a real multipart upload saved under `contents/`.
- assessment form selectors are filtered by the manager context; failed creation posts do not re-expose unauthorized subjects or class groups from submitted ids.
- student class-group detail cards only expose active online assessments that have active questions and current student access.
- legacy static quiz-attempt JSPs now redirect to `/learning/assessments` or `/student/lessons#assessments`; deep student attempt and result routes remain under `/student/assessments`.
- reusable repository files can be listed, selected and previewed freely by non-student actors; students still need normal content access and cannot use repository reuse.
- services reject responses after the availability window closes and reject responses/correction against inactive questions.
- changing `attemptsLimit` is treated as a structural assessment change once attempts exist.

## Design Mapping

The LearnWorlds screenshots were used as design references, not as a replacement for EduAll:

- central question canvas;
- combined settings and attempts page with separated settings and correction areas;
- Add Content assessment cards focused on the `Live` and `In-Person` choice;
- searchable "Add question" modal with type categories;
- compact cards for attempts/results;
- clear badges for type, state, correction and progress.

EduAll styling remains dominant:

- existing white panels, `rounded-10`, neutral borders and main color utilities;
- EduAll/Phosphor icon classes;
- existing dashboard and student dashboard wrappers;
- table, badge and action patterns already used by lessons and class groups.

## Backend Contract

Question inputs submitted by the student use the servlet contract:

- option questions: `question_{questionId}_option`;
- text/date/other questions: `question_{questionId}_answer`;
- rating questions: `question_{questionId}_answer`, posted from the graphical rating picker as an integer or half-step decimal value;
- file uploads: multipart `question_{questionId}_file`, with `question_{questionId}_attachment_existing` used only to preserve an already uploaded file when the student saves again without selecting a replacement.

Manager forms use the service command fields:

- assessments: `subjectId`, `contentBlockId`, repeated `classGroupIds`, `title`, `description`, `type`, `mode`, `correctionMode` (`automatic`, `mixed`, `manual`), `maxGrade`, `passingGrade`, `attemptsLimit`, `state`, `availableFrom`, `availableUntil`;
- questions: `statement`, `required`, `score`, `expectedAnswer`, plus type-specific configuration such as `ratingDesign`, `ratingMax`, hidden `expectedRatingValue` from the graphical expected-answer picker and repeated `acceptedFormat`; `code`, `type`, `orderNo` and `state` are system-managed after creation;
- options: `optionText_{optionId}`, `optionOrder_{optionId}`, `singleCorrect_{questionId}` or `optionCorrect_{optionId}` for option-compatible questions; option state is system-managed, and new options are created asynchronously through the existing question form payload plus `text`;
- manual correction: `score`.

## Validation

- `mvn -q -DskipTests package`
- `mvn -q test`
- `mvn -q "-Dtest=ContentItemServiceTest,AssessmentServiceTest,QuestionServiceTest,QuestionOptionServiceTest,AttemptServiceTest,ResponseServiceTest,CorrectionServiceTest,SchemaIntegrityTest,DatabaseRestrictionCoverageTest,TemplateStructureTest" test`
- Browser Builder smoke test for `/learning/assessments/95`: no browser errors, no horizontal overflow, no visible obsolete question type, flash message after question creation or full-page `Add Option` submit, and the type-specific question editor plus drag-only option ordering are present.
- Browser Builder check for seeded rating assessment `/learning/assessments/96`: rating renders through `[data-rating-picker]`, the lock message appears after submitted attempts, and legacy rating metadata still resolves to the graphical stars design.
- Functional HTTP checks on temporary questions: Add Question redirects to `#question-{id}`, rating questions create without options, `Design: Stars/Circles/Hearts - Integer/Fractional`, maximum scale and expected answer persist, paragraph questions expose a distinct `Expected answer` editor, file upload accepted formats persist, and direct creation of unsupported/internal types is rejected.
- Student browser check on temporary questions in attempt `123`: rating renders through `.gape-student-rating-picker` with a hidden `question_{id}_answer` field and no numeric input or option radios; the fractional click cycle was exercised from half-step to full value (`3.5 / 5` then `4 / 5`), and all temporary questions are removed afterwards.
- Multipart rejection check: a `.txt` upload to a temporary PDF-only file upload question reaches the student response endpoint, renders an error, and leaves no response that blocks cleanup.

## Notes

- The static EduAll quiz pages remain present only as compatibility entry points; they redirect to the real `/learning/assessments` and `/student/lessons#assessments` flows.
- Binary file upload answers are implemented through multipart submission and `PdfUploadService`; accepted formats are stored in question configuration.
- Started attempts lock assessment structure through the services and the Builder UI.
- Automatic correction is exposed only for `automatic` and `mixed` assessments and scores single-choice, multiple-choice and rating responses; manual score editing is exposed only for `manual` and `mixed` assessments.
