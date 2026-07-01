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

Latest execution in this workspace: 2026-07-01, full `mvn test` and `mvn -DskipTests package`, 556 tests, 0 failures, 0 errors, 0 skipped.

Covered cases:

- automatic draft certificate and subject grade sheet creation on active course enrollment;
- automatic draft subject grade sheet creation on active subject enrollment;
- automatic draft class grade sheet creation on active class group enrollment;
- valid grade sheet creation for service-level fixtures in a managed class group;
- coordinator and administrator grade sheet creation in service-level context;
- assessment creation rejects final-grade weights outside the 0 to 100 range;
- created class grade sheets use the final-grade weights defined on assessments;
- manual assessment weight updates require every assessment and each weight in the 0 to 100 range; totals different from 100 are saved with a warning;
- class grade sheet weights can be edited in any grade sheet state;
- ended class group periods redistribute non-100 assessment weights equally before publication;
- grade sheet automatic publication with `released_at` when all required assessment scores and weights exist;
- stale published grade sheets with displayed `-` grades are synchronized back to Draft on read;
- direct grade changes remain blocked after publication, while class assessment weights remain editable;
- student direct consultation of grade sheet definitions blocked;
- grade sheet creation denied outside the teacher context;
- manual final grade entry removed from the service, servlet and JSP;
- automatic grade records generated only from corrected assessment scores and assessment weights;
- active grade records inserted by SQL must use the automatic code pattern (`AUTO-...` or compact `A-...`);
- automatic grade record code and value recalculated when weights change;
- missing assessment scores keep the grade sheet in Draft and leave the final grade as `-`;
- full seed includes draft grade sheet `1015` with one student fully graded and another enrolled student still showing missing assessment grades;
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
- public validation code generated automatically and uniquely;
- certificate publication rejected unless every calculated grade sheet is eligible;
- certificate final grade calculated from every active course subject, not from the submitted subset;
- published certificate revocation;
- certificate revocation rejected for students and partial/unrelated managers;
- revoked certificate rejected by public validation;
- student blocked from certificate publication;
- student consultation limited to own certificates;
- student consultation of another student's certificate blocked;

## Codex Corrections Applied

- Class assessment weights remain configurable through `GradeSheetService.updateGradeSheet` in every grade sheet state; the manager JSP shows the Edit weights action for every class grade sheet and only disables saving when there are no assessment columns.
- Grade sheets are created as `Draft` from active course/subject/class-group enrollments and are not created from the manager JSP.
- Grade sheets are published automatically only when no displayed grade is `-`; ended class periods no longer convert missing grades to zero.
- Certificates are created as `Draft` from course enrollment and are published automatically only after every required grade sheet is complete.
- Certificates now have persisted state and revocation timestamp.
- Public validation accepts only certificates in the persisted `issued` state, shown to users as Published.
- Certificate reads no longer issue, downgrade or otherwise mutate certificate data before authorization.
- Grade records now persist state and enforce one active record per sheet/student.
- Certificates now enforce one non-revoked certificate per course/student.
- Certificate publication now requires every active course subject to have an approved published grade.
- Revoked certificates are blocked from public validation, student access and direct downloads.
- SQL used by the phase services/policy was moved behind DAOs/read services.
- Management option lists are scoped to the manager context and student select labels no longer expose email addresses.
- Management JSPs expose draft weight configuration and revoke certificate forms with CSRF tokens; record-grade, create, publish and issue manual forms were removed.
- Grade-sheet/class-group and certificate/grade-sheet database association rules now validate both inserts and updates.
- Public validation and weight inputs now have labels/ARIA labels.
- Local database credentials are stored directly in `src/main/resources/config/db.properties` for this workspace.

## Manual Review Notes

- `grade_sheet.max_grade` and `grade_sheet.passing_grade` define the scale used by grade record validation.
- Public certificate validation returns only minimal certificate data and never exposes student identity or final grade.
- Service methods audit revocation and public validation operations; grade sheet/certificate publication is lifecycle-driven.
- `src/main/resources/config/db.properties` currently stores the local MySQL credentials used by the IDE and Maven runs.
- `db.bootstrap.mode` is currently versioned as `full`; it recreates and reseeds the database on web application startup.
