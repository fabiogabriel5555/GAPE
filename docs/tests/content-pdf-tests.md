# Content and PDF Test Plan

This document records the automated and manual checks for the Phase 9 learning content repository, file reuse, PDF upload and EduAll integration.

## Automated Tests

Run:

```powershell
mvn -Dtest=ContentItemServiceTest,ContentAssociationServiceTest,PdfUploadServiceTest,ContentServletTest,CsrfFilterTest test
```

Expected result: all tests pass.

Covered by `ContentItemServiceTest`:

- Creates content items for all supported `ContentFormat` values.
- Rejects unsafe or format-incompatible references, including PDF sources that do not end in `.pdf`.
- Requires a responsible actor for content creation.
- Lists reusable files globally, without filtering by original context, author or target-context permission.
- Allows reusable file reads even when the actor cannot access the original context.
- Allows a block manager to delete pedagogical content even when they are not the author.
- Rejects deletion by students.
- Archives protected content instead of physically deleting it.
- Deletes the physical file only when no other content item uses the same stored source.
- Preserves stored file metadata when another content item still uses the same file.

Covered by `ContentAssociationServiceTest`:

- Associates content to an active block when the actor manages the class group.
- Lists block contents with association metadata.
- Reuses thumbnails from a shared stored file.
- Rejects associations that break the course/subject structural chain.
- Rejects association operations without target-context permission.

Covered by `PdfUploadServiceTest`:

- Accepts a valid PDF.
- Rejects non-PDF, empty, corrupt and oversized PDF uploads.
- Rejects unsafe file names and paths through storage validation.

Covered by `ContentServletTest` and `CsrfFilterTest`:

- Upload servlet rejects unauthenticated requests.
- Download servlet rejects unauthenticated requests.
- `/contents/upload` is covered by CSRF protection.

## Manual EduAll Checklist

Use these profiles:

- Administrator: `admin@gape.local`
- Coordinator: `coord@gape.local`
- Teacher: `teacher@gape.local`
- Student: `student@gape.local`

Use an active class group with visible pedagogical blocks, for example `PRJ-T1`.

### Upload Valid PDF

1. Login as Administrator, Coordinator or Teacher with permission over the class group.
2. Open the class group detail page.
3. Add a pedagogical content item to a block and upload a valid `.pdf`.
4. Confirm the request succeeds and the new content appears under the selected block.
5. Open `View` and `Download`.

Expected result: PDF opens inline and downloads through `/contents/download/{id}`.

### Reject Invalid PDF

Repeat the upload using:

- an empty file;
- a `.txt` renamed as PDF;
- a corrupt PDF payload;
- a PDF larger than the configured limit.

Expected result: upload fails with a validation message and no visible pedagogical content is created.

### Reuse Stored File

1. Upload a valid PDF to one block.
2. Use the repository picker in another permitted block and reuse the same file.
3. Confirm a new pedagogical content item is created for the second block.

Expected result: the two pedagogical contents are distinct, but reference the same stored file.

### Free File Reuse

1. Login with a non-student profile that manages one class group but not another.
2. Open the repository picker.
3. Confirm files from other class groups are listed as reusable files.
4. Reuse a file that originally belongs to another class group.
5. Open the repository preview for that file.

Expected result: the file is listed, previewed and reused freely for non-student actors, creating a new pedagogical content item in the target block. Students cannot use the repository picker. If the user cannot manage the target block, the association itself is rejected.

### Delete Pedagogical Content

1. Create a content item in a block with a file that no other content uses.
2. Delete the content from the block.
3. Confirm the content disappears and the stored file is removed.
4. Repeat with two contents using the same file.

Expected result: deleting one content preserves the file while another content still uses it.

### Protected Delete

1. Mark content as mandatory in an active block, or associate it to an assessment with submitted attempts.
2. Delete the pedagogical content.

Expected result: content is archived instead of physically deleted, and the stored file remains.

### Student View

1. Login as a student enrolled in the class group.
2. Open `My Courses`.
3. Expand/check the class group card.
4. Confirm visible blocks show their pedagogical contents with view/download/open actions.

Expected result: student can access only content from visible blocks and active enrollment contexts.

### Security Checks

1. Submit `/contents/upload` without `csrfToken`.
2. Submit `/contents/upload` with an invalid `csrfToken`.
3. Open external URL/embed content containing characters that require attribute escaping.

Expected result: CSRF requests are rejected; external links render as safe escaped attributes.
