# Grades and Certificates Tests

## Scope

The Phase 13 tests cover grade sheets, grade records and certificates through the Java service layer, with database triggers left active.

## Automated Tests

Run:

```bash
mvn test "-Dtest=AssessmentServiceTest,EnrollmentServiceTest,ClassGroupEnrollmentServiceTest,GradeSheetServiceTest,GradeRecordServiceTest,CertificateServiceTest,GradeSheetViewTest,TemplateStructureTest"
```

Full regression run:

```bash
mvn test
```

Historical execution recorded on 01-07-2026: full `mvn test` and `mvn -DskipTests package`, 556 tests, 0 failures, 0 errors, 0 skipped. See `docs/tests/README.md` for current validation rules.

Covered cases:

- one automatic final source grade sheet for every class group, created with that class group;
- one automatic consolidated subject-occurrence sheet, created only when the first real class group exists in that exact subject/course-occurrence context;
- independent grade-sheet, assessment, student and certificate contexts when the same subject is associated with courses that have different occurrence calendars;
- valid grade sheet creation for service-level fixtures in a managed class group;
- coordinator and administrator grade sheet creation in service-level context;
- assessment creation rejects final-grade weights outside the 0 to 100 range;
- created class grade sheets use the final-grade weights defined on assessments;
- manual assessment weight updates require every assessment and each weight in the 0 to 100 range; totals different from 100 are saved with a warning;
- class grade sheet weights can be edited in any grade sheet state;
- ended class group periods redistribute non-100 assessment weights equally before publication;
- grade sheet automatic publication with `released_at` when all required assessment scores and weights exist;
- automatic publication of every grade sheet in a completed, non-cancelled course-occurrence period, even when displayed grades remain `-`;
- persistent publication explanations for pending weights, assessment grades, final grades or subject-sheet consolidation;
- published sheets with displayed `-` grades remain Published and do not become artificially zero-valued;
- direct grade changes remain blocked after publication, while class assessment weights remain editable;
- student direct consultation of grade sheet definitions blocked;
- grade sheet creation denied outside the teacher context;
- manual final grade entry removed from the service, servlet and JSP;
- automatic grade records generated only from corrected assessment scores and assessment weights;
- active grade records inserted by SQL must use the automatic code pattern (`AUTO-...` or compact `A-...`);
- automatic grade record code and value recalculated when weights change;
- missing assessment scores keep an active-period grade sheet in Draft and leave the final grade as `-`;
- full-seed conformance publishes completed-period grade sheets such as `1015` with the required pending-grade explanation;
- calculated approved and failed results from the automatic final grade;
- student blocked from grade management;
- student consultation limited to own published grade records;
- student consultation of draft grade records blocked;
- student consultation of another student's grade records blocked;
- non-student use of the own-grade listing blocked;
- valid certificate synchronization for an eligible student;
- administrator certificate synchronization in context;
- certificate publication rejected without course eligibility;
- certificate publication rejected without an approved grade record;
- certificate publication creates a unique public validation code scoped to its course occurrence;
- certificate publication rejected unless every calculated grade sheet is eligible;
- certificate final grade calculated from every active course subject, not from the submitted subset;
- public validation accepts an issued certificate code and rejects blank, malformed or unknown codes;
- student blocked from certificate publication;
- student consultation limited to own certificates;
- student consultation of another student's certificate blocked;

## Codex Corrections Applied

- Class assessment weights remain configurable through `GradeSheetService.updateGradeSheet` in every grade sheet state; the manager JSP shows the Edit weights action for every class grade sheet and only disables saving when there are no assessment columns.
- Grade sheets are not created from a generic course-subject association or course enrollment. A new class group creates its final source sheet, and that first real class group creates the single derived subject-occurrence sheet. Source and consolidated sheets are always resolved through the concrete class-group occurrence.
- Grade sheets are published automatically when complete and, at the end of a non-cancelled course-occurrence period, are also published with the missing values shown as `-`. `publication_explanation` records the concrete pending reason; missing values are never replaced with artificial zeroes.
- Certificates are created as `Draft` from course enrollment and are published automatically only after every required grade sheet is academically complete; `Published` alone is not sufficient.
- Certificates now persist their state, occurrence and unique validation code.
- Public certificate verification is available at `/certificates/validate` without exposing student data.
- Certificate reads no longer issue, downgrade or otherwise mutate certificate data before authorization.
- Grade records now persist state and enforce one active record per sheet/student.
- Certificates now enforce one certificate per course/student.
- Certificate publication now requires every active course subject to have an approved published grade.
- Certificate validation codes remain stable after issuance.
- SQL used by the phase services/policy was moved behind DAOs/read services.
- Management option lists are scoped to the manager context and student select labels no longer expose email addresses.
- Management JSPs expose draft weight configuration with CSRF tokens; manual record-grade, create, publish and issue forms were removed.
- Grade-sheet/class-group and certificate/grade-sheet database association rules now validate both inserts and updates.
- The `full` seed includes realistic Computer Networks cohorts: completed groups with a mix of corrected and pending grades, active groups with current students and assessments, and active associations to Information Systems Master and Mathematics 1. Those courses use distinct occurrence-period calendars; groups, assessments and aggregate sheets must remain isolated by their concrete course occurrence.
- Weight inputs now have labels/ARIA labels.
- Local database credentials are stored directly in `src/main/resources/config/db.properties` for this workspace.

## Manual Review Notes

- `grade_sheet.max_grade` and `grade_sheet.passing_grade` define the scale used by grade record validation.
- Service methods audit certificate publication lifecycle operations; grade sheet/certificate publication is lifecycle-driven.
- `src/main/resources/config/db.properties` currently stores the local MySQL credentials used by the IDE and Maven runs.
- `db.bootstrap.mode` is currently versioned as `full`; it recreates and reseeds the database on web application startup.
