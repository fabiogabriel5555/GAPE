# GAPE Uploads

This folder keeps the local upload structure. Real files uploaded by users must not be versioned. The small files explicitly allowed by `.gitignore` are deterministic demo fixtures referenced by `sql/seed/*.sql`; `SeedAssetReferenceTest` verifies that every seeded path exists and has a valid file signature.

Structure:

- `users/` - real user profile photos.
- `contents/` - pedagogical content uploaded to the platform, such as PDF, image, audio, video, presentation, SCORM or other resources.
- `messages/` - attachments associated with messages.
- `justifications/` - attachments for absence justifications.
- `tmp/` - temporary area for uploads being processed.
- `quarantine/` - area for rejected files or files pending security validation.

In production, the location must be configurable and can point to an external folder, for example `C:\gape\uploads` or `/var/gape/uploads`.
