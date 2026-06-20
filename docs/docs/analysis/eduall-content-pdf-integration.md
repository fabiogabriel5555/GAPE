# EduAll Content and PDF Integration

## Scope

Phase 9 integrates pedagogical content into the EduAll class group experience. Pedagogical content is contextual to one block or learning context. Stored files can be reused, but reusing a file creates a new pedagogical content item for the target block.

## Implemented Backend Flow

- `ContentItemService` creates and validates content metadata.
- `PdfUploadService` validates and stores PDF, text, image, video and audio uploads.
- `ContentFileService` records stored file metadata.
- `ContentAssociationService` associates content with structural contexts and validates the structural chain.
- `ContentUploadServlet` handles file upload, URL/embed creation and reusable-file selection.
- `ContentDownloadServlet` serves stored files after profile/context authorization.

## File Reuse Rule

The repository picker represents reusable files, not reusable pedagogical contents.

When a user reuses a file:

1. The source file is selected from the global reusable-file repository.
2. The repository does not filter by original context, author or management permission.
3. A new `ContentItem` is created for the target block/context.
4. The new content item points to the same stored file source.

Target-context permissions are still enforced by the association flow. This protects the block/course/subject structure while keeping file reuse itself completely free.

## Delete Rule

Deleting content from a block deletes or archives the pedagogical content item for that block.

- The actor must have management permission over the block/context.
- Authorship is audit metadata, not the delete permission rule.
- Students cannot delete pedagogical content.
- Protected content is archived instead of physically deleted.
- A stored file is physically removed only when no remaining content item uses the same source.

## EduAll UI

Administrator, Coordinator and Teacher class group detail pages use the shared class group detail fragment to:

- list pedagogical blocks;
- list block contents;
- upload content;
- reuse stored files;
- view/download stored files;
- open external URL/embed content;
- delete pedagogical content from a block.

The student `My Courses` page lists visible pedagogical blocks and their content for active class group enrollments. Students can view, download or open content, but cannot create, reuse or delete pedagogical content.

## Security

- `/contents/upload` is covered by `CsrfFilter`.
- Download and upload servlets require an authenticated session.
- Download checks content access before resolving non-reusable content; active file-backed content is readable for repository reuse and preview.
- Stored file paths are validated as safe relative paths below the content storage root.
- External links are escaped in JSP attributes.
- URL and embed sources must be absolute HTTP(S) URLs.
- File-backed content references must match the expected format extension unless they are internal pending-upload paths.

## Known Non-Goals

- The current database still stores the source file path on `content_item`.
- A separate first-class `content_asset` table can be introduced later if the schema is refactored.
- The current implementation enforces file reuse semantics at service/view level without requiring that schema split.

## Verification

Run:

```powershell
mvn -Dtest=ContentItemServiceTest,ContentAssociationServiceTest,PdfUploadServiceTest,ContentServletTest,CsrfFilterTest test
mvn test
```

Manual verification steps are documented in `docs/tests/content-pdf-tests.md`.
