# Class Groups, Enrollments and Content Blocks Tests

## Automated coverage

- `ClassGroupServiceTest`
  - Administrator creates class groups for active course-subject associations.
  - Class groups are rejected when the course does not integrate the subject.
  - `min_students <= max_students` is enforced when both values are present.
  - End date cannot be before start date.
  - Course and subject cannot be changed after a class group is created.
  - Coordinators can update class groups for subjects they coordinate.
  - Assigned teachers can update operational class group data.
  - Teachers cannot deactivate class groups.
  - Teacher assignment reuses `teach_class_group`.
  - Completed class groups cannot be updated.
  - Class groups with domain dependencies cannot be deleted.
  - Administrators can deactivate and delete class groups without dependencies.
  - Class group code remains unique within the subject.

- `ClassGroupEnrollmentServiceTest`
  - Students can request their own class group only when enrolled in the same course occurrence for the full class-group period.
  - Class group enrollment is rejected without a covering course-occurrence enrollment.
  - Approval policy determines whether a valid request remains pending or becomes active.
  - Capacity cannot exceed `max_students`.
  - Assigned teachers can enroll students in their class groups.
  - Duplicate active class group enrollment is blocked.
  - Overlapping active enrollments are blocked across different class groups in the same course/subject context.
  - Students cannot enroll other students.
  - Students can withdraw themselves from a class group.
  - Withdrawing a course enrollment withdraws active class group enrollments in that course.

- `ContentBlockServiceTest`
  - Assigned teachers can create restricted content blocks.
  - Content block order is unique in the class group.
  - Content block code remains unique in the class group.
  - Assigned teachers can update and delete blocks without dependencies.
  - Content blocks cannot be moved to another class group after creation.
  - Blocks with dependencies cannot be deleted.

- `AuthorizationFilterTest`
  - Administrator and teacher sessions can enter `/learning/class-groups`.
  - Student sessions are redirected away from the management route.

- `SchemaIntegrityTest`
  - `content_block` exposes `uq_content_block_order` for order uniqueness in each class group.

- `TemplateStructureTest`
  - Profile-specific class group JSPs stay under each profile's class-group folder.
  - Class group edit forms render course/subject as immutable readonly context.
  - `student/student-enrolled-courses.jsp` exposes class group enrollment, withdrawal and visible pedagogical blocks.
  - The student-facing class group action follows the active course-occurrence/subject context and does not offer another request when an active or pending class group enrollment already exists for that context.

## SQL restriction coverage

- `schema.sql` validates class group enrollment against student, course occurrence, subject association, coverage dates, capacity and overlap.
- `schema.sql` keeps content block order unique in the same class group through a unique key and validation trigger.
- `schema.sql` prevents changing a class group's course/subject and a content block's class group after creation.
- `schema.sql` rejects active teaching assignments unless the teacher and class group are active.
- `application.sql`, `check.sql` and `unique.sql` include negative scenarios for these rules.
