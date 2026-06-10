# EduAll Organizations Integration

## Scope

This integration adds the organization and organic unit management screens to the existing EduAll dashboard shell used by GAPE.

## Template Analysis

The EduAll assets and JSPs were reviewed for organization, institution, unit, hierarchy, department, table, form, detail, dashboard, menu and confirmation modal patterns. The template does not include a complete native organization module. The closest reusable patterns are:

- `WEB-INF/fragments/dashboard-sidebar.jspf` for dashboard navigation and contextual child links.
- `WEB-INF/fragments/dashboard-topbar.jspf` for page titles and primary actions.
- `WEB-INF/fragments/flash-messages.jspf` for success and error messages.
- `admin/admin/user/admin-users.jsp` for summary cards, tables and row actions.
- `admin/admin/user/admin-user-form.jsp` for form layout, photo upload, select controls and save/cancel actions.
- `admin/admin/user/admin-user-detail.jsp` for detail cards, photo rendering and critical action modals.
- `admin/admin/user/admin-deletion-requests.jsp` for modal forms with CSRF tokens.

Older EduAll pages under `src/main/webapp/admin` keep larger duplicated template blocks. The new pages use the consolidated fragments to preserve the current GAPE integration style.

## Pages And Routes

The organization UI is exposed through `/admin/organizations` and `/admin/organizations/*`.

- `GET /admin/organizations`: list organizations managed by the current administrator.
- `GET /admin/organizations/new`: create organization form.
- `POST /admin/organizations`: create organization.
- `GET /admin/organizations/{id}`: organization detail and hierarchy.
- `GET /admin/organizations/{id}/edit`: edit organization form.
- `POST /admin/organizations/{id}`: update organization.
- `POST /admin/organizations/{id}/assign-admin`: assign an administrator.
- `POST /admin/organizations/{id}/archive`: archive organization.
- `POST /admin/organizations/{id}/delete`: delete organization when no dependencies block it.
- `GET /admin/organizations/{id}/units/new`: create organic unit form.
- `POST /admin/organizations/{id}/units`: create organic unit.
- `GET /admin/organizations/{id}/units/{unitId}/edit`: edit organic unit form.
- `POST /admin/organizations/{id}/units/{unitId}`: update organic unit.
- `POST /admin/organizations/{id}/units/{unitId}/archive`: archive organic unit.
- `POST /admin/organizations/{id}/units/{unitId}/delete`: delete organic unit when no dependencies block it.

## JSP Organization

The exclusive administrator tabs are separated under `admin/admin`:

- `admin/admin/user`: user management, user details, deletion requests and audit pages.
- `admin/admin/organization`: organization and organic unit pages.

Organization JSPs:

- `admin/admin/organization/admin-organizations.jsp`: summary metrics, listing table, organization photo, detail/edit/unit/delete actions.
- `admin/admin/organization/admin-organization-form.jsp`: create/edit organization form, photo upload and initial administrator selection.
- `admin/admin/organization/admin-organization-detail.jsp`: organization photo, detail cards, hierarchy view, current administrator assignments, administrator assignment form and critical modals.
- `admin/admin/organization/admin-organic-unit-form.jsp`: create/edit organic unit form with parent selection and read-only generated code on edit.

## Backend Link

The servlet calls the tested services directly:

- `OrganizationService` for CRUD, administrator assignment, archive/delete and contextual permissions.
- `OrganicUnitService` for CRUD, hierarchy validation, archive/delete and contextual permissions.
- `UserService` to list active administrator users for assignment options.
- `ProfilePhotoStorage` to save organization photos under the same WebP resize/crop pipeline used by user profile photos.

Mutating routes are protected by the existing CSRF filter. The sidebar uses `gape.auth.canManageOrganizations`, populated by `SessionManager`.

Organization photos are stored in the `organization.photo` column and served through `/media/{photo}`. Create and edit flows both accept `organizationImage`; the create flow persists the organization first to obtain the id, then stores the uploaded image under `organizations/{id}/profile.webp` through the same WebP pipeline used by user profile photos.

Archive is intentionally not exposed as a normal `State` option in edit forms. Organizations and organic units can only be archived through the dedicated archive actions, so the audit trail records `ORGANIZATION_ARCHIVE` and `ORGANIC_UNIT_ARCHIVE` instead of generic update events.

Organic unit codes are no longer entered by administrators. The service generates them from the unit type with a sequential prefix per organization, for example `SCH-001`, `FAC-001`, `DEP-001`, `CTR-001`, `OFF-001`, `SRV-001`, `SEC-001`, `DIR-001` and `UNT-001`.

## Visual Decisions

The pages keep EduAll dashboard cards, tables, rounded controls, Phosphor icons, modal structure and flash message placement. A simple indented list represents the organic unit hierarchy because EduAll has no dedicated tree component. Select2 arrows are centered through the shared `gape-select-field` styling used by the organization and organic unit Type/State controls.

## Manual Test Checklist

- Create an active organization with an active administrator and verify the success message.
- Try creating an active organization without administrators and verify the error message.
- Edit organization name, acronym, type and active/inactive state.
- Upload an organization photo on create and edit and verify the list/detail/form use the `/media/organizations/{id}/profile.webp` path.
- Confirm the detail page lists current administrator assignments with user state, assignment state and dates.
- Create root and child organic units and verify hierarchy indentation.
- Create organic units of different types and verify the application-generated code prefix matches the selected type.
- Try assigning a parent that would create a cycle through the edit form and verify the error message.
- Archive and delete actions open confirmation modals and return success or dependency errors.

## Verification Run

Executed on 2026-06-10.

- `mvn test "-Dtest=OrganizationServiceTest"`: passed, 7 tests, after reinforcing active-organization administrator invariants.
- `mvn test "-Dtest=OrganicUnitServiceTest"`: passed, 9 tests, after reinforcing hierarchy and archived-operation behavior.
- `mvn test "-Dtest=UserServiceTest"`: passed, 13 tests, including protection against removing the last active administrator of an active organization.
- `mvn test "-Dtest=DatabaseRestrictionCoverageTest"`: passed, 2 tests, including SQL guards for active organizations, hierarchy cycles, controlled assignment state and dependency deletion.
- `mvn test "-Dtest=TemplateStructureTest"`: passed, 18 tests, including organization photo upload, current administrator display, archived action hiding and archive-only flows.
- `mvn test`: passed, 160 tests.
- `mvn package -DskipTests`: passed and packaged `target/gape.war`.

The existing Tomcat instance at `/GAPE` was checked through the in-app Browser, but it was serving an older deployment and was not used as final evidence for the JSP changes. Redeploy `target/gape.war` before the next visual QA pass.

Manual browser checks to run after redeploy:

- Opened `/admin/organizations` after login as the seed administrator. The page loaded with the EduAll dashboard shell, metrics, table, organization photo column and `Organizations` sidebar link.
- Verified missing organization photo paths fall back to the EduAll placeholder image.
- Opened `/admin/organizations/new`. The form used `multipart/form-data`, kept the hidden `photo` field and had Type/State arrows centered.
- Opened `/admin/organizations/10/edit`. The form rendered `organizationImage`, image preview, fallback image data and centered Type/State arrows.
- Edited organization 10 and confirmed `Organization updated successfully.` on the detail page, then restored the seed organization name.
- Opened `/admin/organizations/10/units/new`. The form had no `Code` input, exposed `School`, `Faculty`, `Department`, `Center`, `Office`, `Service`, `Section`, `Direction` and `Other`, and centered Type/State arrows.
- Created a `Faculty` organic unit without submitting a code. The detail page showed `Organic unit created successfully.` and the hierarchy rendered `FAC-001 - Faculdade Browser Teste`.
