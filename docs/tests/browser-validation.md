# Local validation workflow

This file records the fastest known local setup for Maven and browser validation in this workspace.

Last confirmed locally: 13-07-2026.

## Browser driver rule

Do not use the integrated Codex Browser / in-app Browser (`iab`) for this workspace. It has consistently failed locally with `Browser is not available: iab`.

Always use the local Brave browser through Playwright or the CDP helper scripts in `docs/dev/scripts`. In this document, "Browser Tomcat" only means the local Tomcat validation instance under `target/browser-tomcat10`; it does not mean the Codex in-app Browser.

## Cleanup rule

Never leave a Tomcat, Maven/Surefire fork, Brave, Chrome, Edge or Playwright process open when it was started only for validation.

Efficiency rule: start Browser Tomcat at most once per validation pass and stop it once, at the end. Repeated `browser-prepare.ps1` / `browser-stop.ps1` cycles are a failure of process unless the server crashed or Java/backend code was changed and a redeploy is genuinely required.

Before finishing a validation run:

```powershell
.\docs\dev\scripts\browser-stop.ps1
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq 18080 }
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'mvn|surefire|browser-tomcat10|apache-tomcat-10.1.24|playwright|ms-playwright' } | Select-Object ProcessId,Name,CommandLine
```

The port check should return no listener unless the server was already owned by the user before the validation started.

## Backend data-conformance rule

Every backend, schema, migration or domain-rule change must include a data-conformance audit. Historical data is not an exemption for invalid records: update the migration and seed/demo data so every persisted record satisfies the new rule, or make the record explicitly valid under the new lifecycle (for example, an association closed as `historical`). A historical state is valid only when it has its required lifecycle data (such as `ended_at`) and no longer breaks an active invariant; it is never a compatibility flag for legacy garbage.

Before calling backend work complete:

1. Identify every existing record affected by the new invariant.
2. Normalize persisted and seed/demo data; never leave legacy rows that only work because old code tolerated them. For a temporal or lifecycle rule, normalize the parent record, its persisted derived state, every dependent enrollment, assignment, assessment, grade sheet and historical event-recipient snapshot.
3. Add or update an automated conformance test that fails when the invariant is violated.
4. Rebuild/reseed the validation database when schema, migration or seed data changed, then perform the relevant visual validation.

## Fast visual QA loop for frontend changes

For small JSP/CSS/JS layout fixes, use this order. It is intentionally optimized to avoid the slow pattern of package, start Tomcat, capture, stop, edit, repeat.

1. Inspect the changed JSP/CSS first with `rg` and focused file reads.
2. Run only the relevant structural/unit tests. Do not run the full Maven suite for a narrow visual fix unless the user asks for full validation.
3. Build once with `mvn -q -DskipTests package`.
4. Start Browser Tomcat once with `browser-prepare.ps1 -SkipPackage`.
5. Run fast DOM checks first with `browser-check-flow.ps1 -NoScreenshot` for the critical desktop/mobile routes.
6. Fix issues while the server is still running:
   - for JSP/CSS/JS under `src/main/webapp`, sync only the edited files into `target/browser-tomcat10/webapps/GAPE` and refresh/check again;
   - for Java, resources, servlet mappings or compiled classes, rebuild and redeploy once after the changes are stable;
   - do not rebuild/reset the database unless schema, seed/demo data, migrations or the test scenario itself changed.
7. Take final screenshots only after the fast DOM checks pass.
8. Stop Browser Tomcat once in `finally`.

### Fast path for interaction-layout regressions

Use this path for a defect that only appears after an interaction, such as
opening a modal, expanding an accordion, opening a Select2 menu, or choosing
an option. The final code change may be small, so the investigation must stay
small too.

1. Reproduce the exact interaction once and capture three DOM states in the
   same authenticated Brave/Playwright session: **before interaction**, **open
   state**, and **after one real choice/click**.
2. Measure only the boxes that can explain the regression. For a modal/select
   issue, record the modal content rectangle, `clientHeight`/`scrollHeight` of
   the modal root, the visible selection rectangle, and the dropdown rectangle.
   This distinguishes a modal reflow from a wrongly positioned dropdown without
   repeated screenshots or guesswork.
3. Apply one narrow source change, hot-sync the changed web file, then rerun
   the same interaction measurement. Do not restart Tomcat, rebuild Maven, or
   repeat login/browser setup for a JSP/CSS/JS-only iteration.
4. Once the metrics pass, make one real selection/click to prove the interaction
   still works. Then take the final desktop and mobile screenshots and stop the
   validation server.

Do not repeat an exploratory browser run unless the source changed or the
previous run produced new evidence. A generic route check proves that the page
loads; it does **not** prove geometry that only exists while a control is open.

For fixed dialogs, use a viewport screenshot for the final visual inspection.
`fullPage` screenshots can render a fixed overlay at a document-relative
position and therefore misrepresent its real viewport position.

#### Modal and Select2 triage

- If opening a menu increases the modal root's `scrollHeight` or introduces a
  scrollbar, the dropdown is participating in the modal's scrollable layout.
  Render it in a positioned portal outside that scrollable flow, while keeping
  it in the modal stacking context and accessible to pointer input.
- If the modal remains stable but there is a vertical gap below a Select2
  selection, compare the visible selection height with the outer Select2
  container height. Inspect internal helper/wrapper elements before adding a
  visual offset; an invisible wrapper can be included in Select2's placement
  calculation.
- Preserve the original dialog geometry when correcting Select2 internals. If
  removing an internal spacing element changes the form height, move that
  spacing to the field wrapper rather than allowing the dialog to resize.
- A successful check requires all of the following: the dialog rectangle is
  unchanged before/open/after selection, the modal root does not gain overflow,
  the menu begins directly below the visible field, the option can be selected,
  and there are no new console, page, or network errors.

Do not use `-Prepare` on every `browser-check-flow.ps1` call. `-Prepare` runs `browser-prepare.ps1`, and `browser-prepare.ps1` intentionally stops any existing Browser Tomcat before redeploying. For multiple routes in the same QA pass, prepare once and then call `browser-check-flow.ps1` without `-Prepare`.

Recommended single-session command shape:

```powershell
$ErrorActionPreference = "Stop"
$hadBrowserTomcat = @(Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue).Count -gt 0
if ($hadBrowserTomcat) {
    throw "Port 18080 is already in use. Do not take over a server that may belong to the user."
}

try {
    mvn -q "-Dtest=TemplateStructureTest,TemplateAssetReferenceTest" test
    mvn -q -DskipTests package
    .\docs\dev\scripts\browser-prepare.ps1 -SkipPackage

    # Fast checks first: no screenshots, no process restart.
    .\docs\dev\scripts\browser-check-flow.ps1 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 1920 `
      -Height 1200 `
      -NoScreenshot

    .\docs\dev\scripts\browser-check-flow.ps1 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 390 `
      -Height 900 `
      -NoScreenshot

    # Final evidence only after the checks above pass.
    .\docs\dev\scripts\browser-check-flow.ps1 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 1920 `
      -Height 1200 `
      -Out "target\browser-screenshots\assessment-detail-desktop.png"

    .\docs\dev\scripts\browser-check-flow.ps1 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 390 `
      -Height 1100 `
      -Out "target\browser-screenshots\assessment-detail-mobile.png"
} finally {
    .\docs\dev\scripts\browser-stop.ps1
    $listeners = @(Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue)
    Write-Output "Port 18080 listeners after stop: $($listeners.Count)"
}
```

### Hot sync for frontend-only edits

When Browser Tomcat is already running and the change is only in `src/main/webapp`, do not rebuild/restart just to test a JSP, CSS or JS tweak. Copy the edited file into the exploded Browser Tomcat app, preserving the path below `src/main/webapp`.

Use this helper inside the same PowerShell session:

```powershell
function Sync-WebappFile {
    param([Parameter(Mandatory = $true)][string] $Path)

    $webappRoot = (Resolve-Path -LiteralPath "src\main\webapp").Path
    $source = (Resolve-Path -LiteralPath $Path).Path
    $insideWebapp = $source.Equals($webappRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
        $source.StartsWith($webappRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
    if (-not $insideWebapp) {
        throw "Only files under src\main\webapp can be hot-synced: $source"
    }

    $relative = $source.Substring($webappRoot.Length).TrimStart("\")
    $target = Join-Path "target\browser-tomcat10\webapps\GAPE" $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
    Write-Output "Synced $relative"
}

Sync-WebappFile "src\main\webapp\WEB-INF\views\learning\assessment-builder.jsp"
Sync-WebappFile "src\main\webapp\assets\css\main.css"
```

After syncing, rerun the same `browser-check-flow.ps1` command. Do not call `browser-prepare.ps1` unless compiled/backend artifacts changed.

### Java redeploy without database reset

Java changes need compiled classes to be rebuilt and the webapp redeployed, but that does not mean the database must be rebuilt.

Use the smallest valid action:

| Change made | Fast action | Do not do |
| --- | --- | --- |
| JSP, JSPF, CSS, JS, static assets | `Sync-WebappFile`, then rerun `browser-check-flow.ps1` | Do not run Maven, redeploy, reset DB or restart Tomcat |
| Java method body, Servlet logic, Service/DAO logic, auth policy | focused test if relevant, then `mvn -q -DskipTests package` and one `browser-prepare.ps1 -SkipPackage` redeploy | Do not reset/reseed DB unless the change depends on new persisted data |
| New/renamed Java class, servlet mapping, filter, listener, dependency, resource on classpath | `mvn -q -DskipTests package` and one redeploy | Do not reset/reseed DB by default |
| `schema.sql`, `drop.sql`, trigger/constraint, seed data, demo data, data bootstrap code | run the relevant DB/bootstrap tests and rebuild/reseed only the affected validation database | Do not use the browser QA database reset as a reflex for unrelated Java/layout changes |
| Test fixture/data-only change | run the relevant focused tests; reload data only if that test requires it | Do not redeploy Tomcat unless browser runtime changed |

`browser-prepare.ps1 -SkipPackage` does not recreate schema or seed data. It stops only the Browser Tomcat on the target port, initializes the Tomcat base, copies `target\gape` to `target\browser-tomcat10\webapps\GAPE`, starts Tomcat and creates an authenticated session. Treat database rebuild/reseed as a separate, explicit action.

Only rebuild/reseed a database when at least one of these is true:

- `src/main/resources/sql/drop.sql`, `schema.sql` or `sql/seed/*.sql` changed;
- a DB bootstrap/migration path changed;
- the failing test proves persisted data is missing or stale;
- the user explicitly asks for a clean database/demo-data validation;
- the scenario requires newly created seed/demo entities that cannot be created through the UI/API during the test.

### Fast failure triage

When a visual check fails, prefer cheap signals before taking more screenshots:

- read `browser-check-flow.ps1` JSON first: `networkErrors`, `consoleErrors`, `browserErrors`, `brokenImages`, `accessibilityIssues`, `pageOverflowX`, `missingText` and `rejectedTextFound`;
- if `pageOverflowX=true`, inspect the latest screenshot once, then fix the most likely wide container/table/flex item;
- if the screenshot does not show the relevant area, rerun with a taller viewport once instead of many small repeated captures;
- do not repeatedly open and close Tomcat while debugging CSS/JSP.

## Maven full test defaults

Use the full suite command exactly when the user asks for final backend validation:

```powershell
mvn -q test
```

Local timing observed on this machine:

- historical full-run timing on this machine: about 144 seconds; measure the current run instead of treating this as a result;
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

The integrated Codex Browser / in-app Browser (`iab`) is disabled for practical validation in this local setup. It has repeatedly failed with:

```text
Browser is not available: iab
No browser is available
```

Do not attempt, retry, or debug `iab` for this project. Use Playwright/CDP with the locally installed Brave browser from the start of every browser validation pass.

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

The helper scripts under `docs/dev/scripts` already search Brave first, then Edge/Chrome. Prefer them for repeatable checks. If writing a one-off Playwright script, launch Brave explicitly with the executable path above.

## Tomcat setup for browser validation

Fixed local target:

- base URL: `http://localhost:18080/GAPE`
- HTTP port: `18080`
- shutdown port: `18085`
- Tomcat base: `target/browser-tomcat10`
- default account: `admin@gape.local` / `Password#2026`

Tomcat is provisioned outside `target`, under `docs/dev/.tools/apache-tomcat-10.1.24`. On first use, `browser-prepare.ps1` downloads the official Windows archive and its SHA-512 file, verifies the archive and installs it. It can also be installed explicitly:

```powershell
.\docs\dev\scripts\tomcat-install.ps1
```

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

For a generic authenticated page check after Tomcat is already prepared:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/learning/assessments/new" `
  -ExpectText "Assessment" `
  -Out "target\browser-screenshots\assessment-new.png"
```

For a one-off check where Tomcat is not running, `-Prepare` is acceptable. Do not use it in a multi-route or iterative visual QA loop:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Prepare `
  -Route "/learning/assessments/new" `
  -ExpectText "Assessment" `
  -NoScreenshot
```

When iterating quickly, keep the prepared server only for the duration of the active validation run, then stop it once.

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
- `-HoverSelector ".some-row"`: move the real browser pointer over a visible element before the screenshot; use this for hover-state visual checks.
- `-ExpectText "Loaded"`: require visible page text.
- `-ExpectSelector ".modal.show"`: require a DOM selector that is visibly rendered.
- `-InspectSelector ".some-panel"`: include the rendered rectangle, client/scroll width and computed width in the JSON evidence; use it to verify transitions and column geometry.
- `-ExpectBadgeAtParentTopLeft "[data-sidebar-learning-pending-badge]"`: require at least one visible selected badge; its literal top-left corner must coincide (within half a CSS pixel) with the top-left corner of its `.gape-sidebar-badged-item` link, and it must not be clipped by an overflow ancestor.
- `-NetworkLatencyMs 1500`: deliberately add bounded CDP latency when a loading state must be captured; pair it with a short `-WaitAfterActionMs` and assert both the spinner and the static card content.
- `-RejectText "Exception"`: fail if text is present.
- `-NoScreenshot`: run faster when DOM/layout proof is enough.
- `-AllowNetworkErrors`, `-AllowConsoleErrors`, `-AllowBrokenImages`, `-AllowAccessibilityIssues`: temporary diagnostic opt-outs; do not use them for final evidence.

By default the helper fails on HTTP 4xx/5xx responses, failed resource loads, `console.error`, uncaught JavaScript errors, visible broken images, duplicate IDs, missing document language, visible images without `alt`, and visible controls/actions without an accessible name.

### Visual regression baseline

Create or intentionally update a baseline only after the DOM, network and accessibility checks pass:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/admin/organizations" `
  -Out "target\browser-screenshots\organizations-current.png" `
  -Baseline "docs\tests\browser-baselines\organizations-1920x1200.png" `
  -UpdateBaseline
```

Compare a later render against it:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/admin/organizations" `
  -Out "target\browser-screenshots\organizations-current.png" `
  -Baseline "docs\tests\browser-baselines\organizations-1920x1200.png" `
  -MaxPixelDifferencePercent 0.25 `
  -PixelColorTolerance 20
```

The baseline and current PNG must have identical dimensions. A changed baseline is review evidence and must be committed only when the visual change is intentional.

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

## Screenshots

When a screenshot is needed, use the local Brave/CDP scripts instead of the integrated Browser screenshot path:

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
- No failed HTTP/resource requests, browser log errors or console errors caused by the change.
- Menus and action buttons match the current user's permissions.
- Text does not overlap or overflow on mobile.
- User-facing dates and times use `DD-MM-AAAA` and `HH-MM-SS` in `Europe/Lisbon`; native input values and other ISO technical values are not display evidence.
- Images/profile assets load or use their intended fallback.
- Any server/browser process started for QA is stopped before finishing.

## Upload smoke validation

The upload helper uses valid PDF, image, M4A and MP4 fixtures with their real MIME types, verifies processed output, checks invalid PDF/image rejection, reuses a repository file, and removes the records/files it creates in a `finally` block. Cleanup is verified by probing every created content ID and checking that its storage directory is gone:

```powershell
.\docs\dev\scripts\browser-check-upload.ps1 -ClassGroupId 53
```

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
