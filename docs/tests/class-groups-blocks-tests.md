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
  - Teachers cannot archive class groups.
  - Teacher assignment reuses `teach_class_group`.
  - Archived class groups cannot be updated.
  - Class groups with domain dependencies cannot be deleted.
  - Administrators can archive and delete class groups without dependencies.
  - Class group code remains unique within the subject.

- `ClassGroupEnrollmentServiceTest`
  - Students can enroll themselves only when actively enrolled in the subject.
  - Class group enrollment is rejected without subject enrollment.
  - Capacity cannot exceed `max_students`.
  - Assigned teachers can enroll students in their class groups.
  - Duplicate active class group enrollment is blocked.
  - Overlapping active enrollments are blocked across different class groups in the same course/subject context.
  - Students cannot enroll other students.
  - Students can withdraw themselves from a class group.
  - Withdrawing a subject withdraws active class group enrollments in that subject.

- `ContentBlockServiceTest`
  - Assigned teachers can create scheduled content blocks.
  - Inactive blocks can reuse an order already used by an active block.
  - Active block order is unique in the class group.
  - Scheduled access requires `available_from`.
  - Availability end requires a start date and cannot be before the start.
  - Content block code remains unique in the class group.
  - Assigned teachers can update, archive and delete blocks without dependencies.
  - Content blocks cannot be moved to another class group after creation.
  - Archived blocks cannot be updated.
  - Blocks with dependencies cannot be deleted.

- `AuthorizationFilterTest`
  - Administrator and teacher sessions can enter `/learning/class-groups`.
  - Student sessions are redirected away from the management route.

- `SchemaIntegrityTest`
  - `content_block` exposes `uq_content_block_active_order` for active-only order uniqueness.

- `TemplateStructureTest`
  - Profile-specific class group JSPs stay under each profile's class-group folder.
  - Class group edit forms render course/subject as immutable readonly context.
  - `student/student-enrolled-courses.jsp` exposes class group enrollment, withdrawal and visible pedagogical blocks.
  - The student-facing class group action follows the active course/subject enrollment context and does not offer another enrollment when one active class group already exists for that context.

## SQL restriction coverage

- `schema.sql` validates active class group enrollment against student, course, subject, association, capacity and overlap.
- `schema.sql` keeps content block order unique only among active blocks in the same class group through an active-only generated key and validation trigger.
- `schema.sql` prevents changing a class group's course/subject and a content block's class group after creation.
- `schema.sql` rejects active teaching assignments unless the teacher and class group are active.
- `application.sql`, `check.sql` and `unique.sql` include negative scenarios for these rules.
