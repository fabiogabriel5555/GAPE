# Browser validation workflow

Use this workflow after frontend changes. It keeps Browser validation repeatable and avoids losing time on port discovery, manual Tomcat setup and repeated UI login.

## Fast setup

1. Build the app when code changed:

```powershell
mvn -q -DskipTests package
```

2. Start/redeploy the Browser Tomcat on the fixed port:

```powershell
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
```

The script always targets `http://localhost:18080/GAPE`, redeploys `target/gape`, logs in with the seed admin account and prints authenticated URLs using `;jsessionid=...`.

3. Open the printed `Session base` URL in the in-app Browser, then navigate from the printed recommended URLs.

4. Stop the local Browser Tomcat when finished:

```powershell
.\docs\dev\scripts\browser-stop.ps1
```

## Fast Add Content modal check

For general frontend QA, use the generic browser flow runner instead of rebuilding CDP commands by hand:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/learning/class-groups" `
  -ExpectText "Class Groups" `
  -Out "target\browser-screenshots\class-groups.png"
```

It authenticates with the seed admin account, opens the requested route, runs configured clicks/assertions, checks for common server-error text, checks horizontal overflow, captures a screenshot, and returns a compact JSON result with `passed`.

Useful options:

- `-Prepare`: start/redeploy the fixed Tomcat first.
- `-ClickSelector ".some-button"`: click one or more CSS selectors.
- `-ClickText "New Block"`: click a button/link by visible text.
- `-ExpectText "Loaded"`: require visible page text.
- `-ExpectSelector ".modal.show"`: require a DOM selector.
- `-RejectText "Exception"`: fail if text is present.
- `-NoScreenshot`: run faster when DOM/layout proof is enough.

For Class Group Detail changes around the Add Content modal, use the dedicated wrapper:

```powershell
mvn -q -DskipTests package
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
.\docs\dev\scripts\browser-check-add-content.ps1 `
  -ClassGroupId 53 `
  -ContentType Video `
  -Out "target\browser-screenshots\add-content-modal-fast.png"
```

The wrapper calls `browser-check-flow.ps1 -Preset AddContent`. It opens `/learning/class-groups/{id}`, opens Add Content, selects the requested content type, captures a screenshot and fails if the modal title, selected option, footer visibility, file input visibility or horizontal overflow are wrong.

Keep these scripts as the default for frontend work because they avoid manual login, repeated port discovery and ad hoc Browser/CDP commands. Use `-Prepare` only when Tomcat is not already running; otherwise keep the already prepared server alive while iterating.

## Browser plugin defaults

Use longer timeouts for local JSP pages and screenshots:

- Navigation/actions: at least `120000` ms.
- Screenshots: at least `300000` ms.
- Prefer direct authenticated URLs from `browser-prepare.ps1` over typing credentials into the UI for every run.
- If the Browser screenshot call times out with `Page.captureScreenshot`, do not keep retrying the same capture. Finish the Browser checks through DOM, console errors, route transitions and visible page content, then create the required screenshot with the fallback script below.

## Screenshot fallback

The in-app Browser can navigate and inspect the app even when its internal CDP screenshot command times out. In that case, keep Browser validation mandatory and capture the same authenticated page with a local Chromium browser:

```powershell
.\docs\dev\scripts\browser-screenshot.ps1 `
  -Path "/learning/class-groups/51" `
  -Out "target\browser-screenshots\class-group-51-1920x1200.png" `
  -Width 1920 `
  -Height 1200 `
  -WaitBeforeCaptureMs 5000 `
  -TimeoutSeconds 300
```

Use this for visual QA evidence whenever `tab.screenshot(...)` fails. The script uses the fixed `http://localhost:18080/GAPE` target, creates a seed-admin session, opens the requested route with `;jsessionid=...`, waits before capture so the EduAll preloader disappears, and writes a deterministic screenshot under `target\browser-screenshots`.

## Minimum checks after frontend changes

- Desktop and mobile viewport for the changed screens.
- No JSP stack trace or internal server error.
- No console errors caused by the change.
- Menus and action buttons match the current user's permissions.
- Text does not overlap or overflow on mobile.
- Images/profile assets load or use their intended fallback.

## Useful seed accounts

All listed full-seed accounts use `Password#2026`.

- `admin@gape.local`: global administrator.
- `admin.org@gape.local`: organization-scoped administrator.
- `admin.unit@gape.local`: organic-unit-scoped administrator.
- `admin.structure.demo@gape.local`: organization structure administrator.
- `admin.structure.unit@gape.local`: organic-unit structure administrator.
- `coord@gape.local`: coordinator.
- `teacher@gape.local`: teacher.
- `student@gape.local`: student.
- `multi@gape.local`: coordinator, teacher and student profiles.
