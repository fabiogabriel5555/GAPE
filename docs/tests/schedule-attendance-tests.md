# Schedule and attendance tests

## Scope

These tests cover the Phase 12 back-end rules for schedule events, attendance records, absence justifications and permanence-time calculation.

Implemented classes:

- `ScheduleEventServiceTest.java`
- `AttendanceRecordServiceTest.java`
- `AbsenceJustificationServiceTest.java`

## Seed data used

- Administrator: user `1`, global `MANAGE_ALL`
- Coordinator: user `2`, coordinates subject `40`
- Teacher: user `3`, teaches class group `50`
- Student: user `4`, enrolled in class group `50`
- Active unenrolled student created in-test: user `6`
- Class group: `50`
- Other class group: `52`
- Existing lesson: `80`
- Existing schedule event: `140`
- Existing attendance record: `150`
- Existing absence justification: `160`

Positive creation tests create fresh lessons inside the test transaction to avoid the unique keys already occupied by the base seed.

## Automated coverage

- `ScheduleEventServiceTest`
  - Valid lesson schedule event with matching lesson period.
  - Event class-group association and recipients are persisted.
  - Invalid event dates are rejected.
  - Enabled reminder without minutes is rejected.
  - Lesson event with wrong type is rejected.
  - Lesson event with period different from the lesson is rejected.
  - Assessment event outside availability window is rejected.
  - Subject-level assessment events without explicit groups reject class groups from another subject.
  - Student and teacher visibility is limited to their context.
  - Administrator visibility works with `MANAGE_LEARNING` scoped to organization.
  - Student cannot create schedule events.
  - Teacher cannot create events for unmanaged class groups.

- `AttendanceRecordServiceTest`
  - Teacher records valid attendance for an enrolled student.
  - Permanence minutes are calculated from check-in and check-out.
  - Attendance for a student not enrolled in the lesson class group is rejected.
  - Direct creation of `justified` attendance is rejected.
  - Automatic-source attendance is rejected unless produced by platform integration.
  - Duplicate active attendance for the same student and lesson is rejected.
  - A corrected historical record does not block a new active record for the same student and lesson.
  - Check-out before check-in is rejected.
  - Student cannot record attendance.
  - Coordinator and administrator can record attendance in their context.
  - Teacher lesson listing and student own listing expose only contextual records.
  - Administrator visibility works with `MANAGE_LEARNING` scoped to organization.

- `AbsenceJustificationServiceTest`
  - Student submits a valid justification for own absent record.
  - Justification for a present record is rejected.
  - Justification submitted by another student is rejected.
  - Duplicate justification for an attendance record is rejected.
  - Teacher processes a justification as approved.
  - Approval records processor/date from the service clock and changes attendance to justified/corrected.
  - Submission ignores caller-provided dates and uses the service clock.
  - Processing date before submission is rejected for already inconsistent stored data.
  - Processing without permission is rejected.
  - Processing without a final decision is rejected.
  - Administrator visibility works with `MANAGE_LEARNING` scoped to organization.

## Commands

```bash
mvn test -Dtest=ScheduleEventServiceTest
mvn test -Dtest=AttendanceRecordServiceTest
mvn test -Dtest=AbsenceJustificationServiceTest
```

## Expected result

All commands must finish with zero failures and zero errors.

## Execution result

Executed on 27-06-2026:

- `mvn test -Dtest=ScheduleEventServiceTest`: 10 tests, 0 failures, 0 errors.
- `mvn test -Dtest=AttendanceRecordServiceTest`: 12 tests, 0 failures, 0 errors.
- `mvn test -Dtest=AbsenceJustificationServiceTest`: 9 tests, 0 failures, 0 errors.
- `mvn test "-Dtest=ScheduleEventServiceTest,AttendanceRecordServiceTest,AbsenceJustificationServiceTest,TemplateStructureTest"`: 63 tests, 0 failures, 0 errors.
- `mvn test`: 515 tests, 0 failures, 0 errors.

Note: the first focused local run exposed an attendance unique-key schema issue and a test fixture that used an assessment type requiring a content block. Both were corrected, and the focused suite then passed.
