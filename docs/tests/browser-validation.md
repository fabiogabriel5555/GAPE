# Local validation workflow

This file records the fastest known local setup for Maven and browser validation in this workspace.

Last confirmed locally: 2026-06-26.

## Cleanup rule

Never leave a Tomcat, Maven/Surefire fork, Brave, Chrome, Edge or Playwright process open when it was started only for validation.

Before finishing a validation run:

```powershell
.\docs\dev\scripts\browser-stop.ps1
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq 18080 }
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'mvn|surefire|browser-tomcat10|apache-tomcat-10.1.24|playwright|ms-playwright' } | Select-Object ProcessId,Name,CommandLine
```

The port check should return no listener unless the server was already owned by the user before the validation started.

## Maven full test defaults

Use the full suite command exactly when the user asks for final backend validation:

```powershell
mvn -q test
```

Local timing observed on this machine:

- last successful full run: about 144 seconds;
- use a command timeout of at least 15 minutes (`900000` ms) for the full suite;
- do not treat the Java/WebP native-access warning as a failure when Maven exits with code `0`.

If `mvn -q test` appears stuck or a previous run timed out, check for stale Maven/Surefire/Java test forks before running it again:

```powershell
Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -match 'mvn|surefire|org.apache.maven|junit' } |
  Select-Object ProcessId,Name,CommandLine
```

Stop only processes that were started by the current validation attempt and verify the port/process state afterwards.

To isolate a suspected slow/failing test class, use:

```powershell
mvn -q "-Dtest=<TestClassName>" test
```

For browser validation after code changes, build the WAR without re-running tests:

```powershell
mvn -q -DskipTests package
```

## Browser defaults in this workspace

The integrated Codex Browser is not a useful default in this local setup. It has repeatedly failed with:

```text
No browser is available
```

Do not spend time retrying the integrated Browser for this project. Use Playwright/CDP with the locally installed Brave browser.

Primary browser executable:

```text
C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe
```

The Playwright bundled Chromium binaries may be missing, so direct Playwright launches must pass `executablePath` instead of relying on the default downloaded browser:

```javascript
const { chromium } = await import("playwright");
const browser = await chromium.launch({
  executablePath: "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
  headless: true
});
try {
  // validate pages here
} finally {
  await browser.close();
}
```

The helper scripts under `docs/dev/scripts` already search Brave first, then Edge/Chrome. Prefer them for repeatable checks.

## Tomcat setup for browser validation

Fixed local target:

- base URL: `http://localhost:18080/GAPE`
- HTTP port: `18080`
- shutdown port: `18085`
- Tomcat base: `target/browser-tomcat10`
- default account: `admin@gape.local` / `Password#2026`

Fast setup after building the app:

```powershell
mvn -q -DskipTests package
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
```

Stop it when the validation is finished:

```powershell
.\docs\dev\scripts\browser-stop.ps1
```

If `browser-prepare.ps1` cannot start Tomcat because of local path quoting/encoding around `catalina.bat`, do not keep retrying the same failing command. Start Tomcat with quoted direct Java arguments or fix the script, then still stop the resulting process before finishing.

## Fast browser checks

For a generic authenticated page check:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Prepare `
  -Route "/learning/assessments/new" `
  -ExpectText "Assessment" `
  -Out "target\browser-screenshots\assessment-new.png"
```

Use `-Prepare` only when Tomcat is not already running. When iterating quickly, keep the prepared server only for the duration of the active validation run, then stop it.

For mobile layout checks:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/learning/assessments/new" `
  -Width 390 `
  -Height 844 `
  -Out "target\browser-screenshots\assessment-new-mobile.png"
```

Useful options:

- `-ClickSelector ".some-button"`: click one or more CSS selectors.
- `-ClickText "New Block"`: click a button/link by visible text.
- `-ExpectText "Loaded"`: require visible page text.
- `-ExpectSelector ".modal.show"`: require a DOM selector.
- `-RejectText "Exception"`: fail if text is present.
- `-NoScreenshot`: run faster when DOM/layout proof is enough.

For Class Group Detail changes around the Add Content modal:

```powershell
mvn -q -DskipTests package
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
.\docs\dev\scripts\browser-check-add-content.ps1 `
  -ClassGroupId 53 `
  -ContentType Video `
  -Out "target\browser-screenshots\add-content-modal-fast.png"
.\docs\dev\scripts\browser-stop.ps1
```

## Screenshot fallback

When a screenshot is needed, prefer the local Brave/CDP scripts instead of the integrated Browser screenshot path:

```powershell
.\docs\dev\scripts\browser-screenshot.ps1 `
  -Path "/learning/class-groups/51" `
  -Out "target\browser-screenshots\class-group-51-1920x1200.png" `
  -Width 1920 `
  -Height 1200 `
  -WaitBeforeCaptureMs 5000 `
  -TimeoutSeconds 300
```

## Minimum checks after frontend changes

- Desktop and mobile viewport for the changed screens.
- No JSP stack trace or internal server error.
- No console errors caused by the change.
- Menus and action buttons match the current user's permissions.
- Text does not overlap or overflow on mobile.
- Images/profile assets load or use their intended fallback.
- Any server/browser process started for QA is stopped before finishing.

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
