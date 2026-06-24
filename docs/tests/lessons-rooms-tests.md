# Lessons and physical rooms tests

## Scope

This document covers the backend tests for the lessons and physical rooms module.

Implemented classes:

- `LessonServiceTest.java`
- `PhysicalRoomServiceTest.java`
- `StoredVideoConferenceAdapterTest.java`
- `DashboardServletSupportTest.java`
- `SubjectServiceTest.java` for teacher-limited subject visibility related to lesson/class group context.
- `TemplateStructureTest.java` and `TemplateAssetReferenceTest.java` for the EduAll menu routes and JSP structure.

## Commands

```bash
mvn test -Dtest=LessonServiceTest
mvn test -Dtest=PhysicalRoomServiceTest
mvn test -Dtest=StoredVideoConferenceAdapterTest,DashboardServletSupportTest
```

## Seed data used

- Administrator: user `1`
- Coordinator: user `2`, coordinates subject `40`
- Teacher: user `3`, teaches class group `50`
- Student: user `4`, enrolled in class group `50`
- Class group: `50`
- Valid content block: `60`
- Content block from another class group: `62`
- Valid room: `SALA-A1`
- Room from another organization: `SALA-X1`
- Existing active lesson: `80`, in `SALA-A1`, from `2026-02-05 18:00` to `20:00`

## Covered cases

- Valid physical room creation by administrator.
- Coordinator can read, create, update and delete physical rooms in a coordinated organization context.
- Teacher denied when managing physical rooms.
- Teacher can read physical rooms in an organization where they teach an active class group.
- Teacher cannot read physical rooms outside their taught organization context.
- Teacher can read but cannot manage an individual room in their taught organization.
- Teacher cannot read an individual room outside their taught organization context.
- Invalid room capacity rejected.
- Physical room organic unit from another organization rejected.
- Physical room update.
- Physical room with lesson dependencies rejected on delete.
- Physical room without dependencies deleted physically.
- Valid online lesson.
- Valid lesson creation by administrator with scoped `MANAGE_LEARNING` over the class group.
- Online lesson without access URL rejected.
- Invalid online URL rejected.
- Localhost videoconference URL rejected.
- Onsite lesson without room rejected.
- Onsite lesson with nonexistent room rejected.
- Onsite lesson with room from another organization rejected.
- Overlapping room reservation rejected.
- Onsite lesson in available room accepted.
- Onsite lesson rejected when the selected physical room capacity is below active class group enrollments.
- Invalid lesson dates rejected.
- Lesson associated to content block from another class group rejected.
- Student lists lessons only for active class group enrollment.
- Student cannot read a lesson outside the enrolled class group.
- Personal calendar does not use administrator global management scope.
- Personal calendar uses teacher, coordinator and student context only.
- Lesson update.
- Lesson without dependencies deleted physically.
- Lesson with schedule or attendance history rejected on delete.
- Teacher lists and opens only subjects containing class groups they teach.
- Dashboard sidebar exposes `Calendar` after `Message`.
- Dashboard sidebar routes student calendar to `/student/calendar`.
- Student meeting buttons use `/student/lessons/{lessonId}/access`, so access is validated and audited before redirecting to the external provider.
- `returnTo` rejects unsafe attribute/control characters before it is rendered in learning back links.
- Videoconference adapter rejects unknown providers, provider/host mismatches, user-info URLs and private/local hosts.
- Teacher sidebar exposes `Subjects`, then `Class Groups`, `Lessons` and `Rooms`.
- Class Groups listings expose `Show` automation with lessons and physical rooms.

## Expected result

Both commands must finish with zero failures and zero errors.

## Execution result

Executed on 2026-06-24:

- `mvn -q "-Dtest=LessonServiceTest,PhysicalRoomServiceTest,StoredVideoConferenceAdapterTest,DashboardServletSupportTest,TemplateStructureTest" test`: 0 failures, 0 errors.
- `mvn -q "-Dtest=TemplateAssetReferenceTest" test`: 0 failures, 0 errors.
- `mvn -q test`: 412 tests, 0 failures, 0 errors, 0 skipped.
