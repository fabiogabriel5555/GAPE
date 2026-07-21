# Local validation workflow

This file records the fastest known local setup for Maven and browser validation in this workspace.

Last confirmed locally: 21-07-2026.

## Browser driver rule

Do not use the integrated Codex Browser / in-app Browser (`iab`) for this workspace. It has consistently failed locally with `Browser is not available: iab`.

Always use the local Brave browser through Playwright or the CDP helper scripts in `docs/dev/scripts`. The helpers' historical 18080/browser-Tomcat defaults are not valid for this project; every invocation must pass `-Port 8080` explicitly. This is unrelated to the unavailable Codex in-app Browser.

## Responsive audit rule

Responsiveness is part of the acceptance criteria for every page, not a
one-time check of the developer's monitor. For a shared template/CSS change,
exercise every authenticated top-level route and at least one detail/form and
modal state for each actor (administrator, coordinator/instructor and
student). Repeat the checks at these viewport classes:

| Class | CSS viewport used by the helper | Required proof |
| --- | --- | --- |
| small phone | `-Width 320 -Height 568` | no page overflow, controls remain visible/actionable, long labels wrap/ellipsis |
| phone | `-Width 390 -Height 844` | same flow and settled mobile screenshot |
| tablet | `-Width 768 -Height 1024` | grids/forms reflow without overlap |
| laptop | `-Width 1366 -Height 768` | desktop navigation and tables remain usable |
| large desktop | `-Width 1920 -Height 1200` | no excessive stretching or clipped modal content |

Use the real deployment on port 8080 for every class; never substitute 18080.
For the fast pass, run `browser-check-flow.ps1 -Port 8080 -NoScreenshot` and
require `pageOverflowX: false`, zero network/console/broken-image/accessibility
errors and the expected route/state. Then capture settled screenshots for the
small-phone and large-desktop states, including the deepest relevant
accordion/tab/modal state (not only the page heading). Tables and calendars
may scroll inside their explicitly named wrapper; horizontal scrolling on the
document itself is a failure. After a responsive CSS/JSP correction, repeat
the complete route matrix, not just the page where the first defect was seen.

### Internal clipping audit (mandatory for responsive changes)

`pageOverflowX: false` only proves that the document itself did not become
wider; it does not detect a table header, action column, card or nested panel
being clipped inside an otherwise correctly sized parent. For every
responsive batch, add `-AuditInternalOverflow` to the helper invocation:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 -Route "/learning/lessons" `
  -Email "coord@gape.local" -Width 390 -Height 844 `
  -AuditInternalOverflow -NoScreenshot
```

The run is accepted only when both `pageOverflowX` is `false` and
`internalOverflowIssues` is an empty array, in addition to zero network,
console, broken-image and accessibility errors. Repeat this audit after
opening every relevant tab/accordion/modal and capture a settled screenshot;
checking only the initial heading or a global text match is insufficient.
The audit deliberately ignores explicit scroll wrappers and text-ellipsis
elements, but reports visible descendants whose content or action controls
extend beyond their parent bounds. This catches the “actions cut off at the
right edge” defect from `docs/fotos-exemplos/1.png`.

### Management Dashboard card flow

The management Dashboard is accepted only after the real 8080 flow has been
replayed for each management actor. As administrator, open `/dashboard`, click
the Dashboard and Logs cards in both directions, and capture the settled card
state. During each click assert that the clicked card keeps its fixed dimensions,
sets `aria-busy="true"`, and shows one centered animated spinner; after the
response settles assert the active card, visible content, URL and absence of
the old `The dashboard response was not valid.` message. Repeat after reload.
As coordinator and instructor, visit `/dashboard?tab=logs` directly and also
open `/dashboard` normally: the server must resolve to Dashboard, no Logs card
or log content may be rendered, and the page must remain usable at phone and
desktop widths. Run `-AuditInternalOverflow` for every state and retain the
JSON plus settled screenshots as evidence.

## Actor-owned page rule

The administrator, coordinator, instructor and student surfaces have separate
JSP namespaces. A coordinator or instructor must never be forwarded to an
`admin/...` page, and a student must never be forwarded to a management page.
The page names and directory shape stay parallel (`admin-courses.jsp`,
`coordinator-courses.jsp`, `instructor-courses.jsp`, `student-courses.jsp`,
with the same `detail`, `form`, `list` and feature suffixes); only the data
and authorization limits differ. Shared fragments are allowed for visual
components, but actor page wrappers and controller view constants must remain
actor-specific. Every actor-page change therefore requires static ownership
checks plus one real browser visit per actor, including list, detail/form and
one asynchronous/modal state where that actor is allowed to open it.

## Real application address is mandatory for every test

The address used by the user is the source of truth for browser behaviour and
visual appearance. The current GAPE deployment is:

```text
http://localhost:8080/GAPE/
```

Every browser test and visual validation must use that address: login,
route/card/tab selection, modal opening, dependent selections, submit/close
behaviour, final state, desktop/mobile evidence and cleanup. A check on another
port, a direct HTTP request, or a screenshot from an isolated deployment is
invalid evidence.

### Mandatory 8080 preflight

Before **every** browser-test or visual-validation batch, stop the existing
GAPE Tomcat on 8080 so it cannot interrupt the run or compete for the database.
Inspect the exact process before stopping it:

```powershell
$listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
$listeners | Select-Object LocalAddress,LocalPort,OwningProcess
$listeners | ForEach-Object { Get-CimInstance Win32_Process -Filter "ProcessId = $($_.OwningProcess)" | Select-Object ProcessId,CommandLine }
```

After confirming that the listed PID is the GAPE Tomcat, stop that exact
process (for example, `Stop-Process -Id <confirmedPid> -Force`), verify that
port 8080 is free,
then start/redeploy one clean application instance on 8080. Do not run the
browser helpers against 18080, and do not leave a second GAPE Tomcat running.
The final command shape is always:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
  -Route "/learning/lessons" `
  -Email "coord@gape.local" `
  -Width 1920 -Height 1200 `
  -Out "target\browser-screenshots\real-8080-lessons-desktop.png"

.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
  -Route "/learning/lessons" `
  -Email "coord@gape.local" `
  -Width 390 -Height 844 `
  -Out "target\browser-screenshots\real-8080-lessons-mobile.png"
```

If the clean 8080 deployment cannot be started, report the real-address
validation as blocked/unverified. Never silently substitute port 18080.
For a full Maven suite, apply the same preflight first, keep all GAPE Tomcats
stopped during the suite, and start the clean 8080 deployment again before the
post-suite browser validation.

### Real-address proof, not route proof

Passing a route check is never enough to accept a frontend change. The final
evidence must come from the real deployment at `http://localhost:8080/GAPE/`
and must replay the complete visible flow that the user reported. In
particular, when a feature partitions, filters or archives records, validate
both sides of the partition in the same session: open the parent view, perform
the real click that expands or selects the view, assert the complete expected
list and count in the primary view, assert the complete expected list and
count in the secondary/archive view, and assert that forbidden states are
absent from the correctly scoped container. A global text assertion is not
enough because the same text may be present in a hidden panel or a duplicated
card.

Before declaring the flow passed, reload the real route and repeat the final
assertions. Capture settled desktop and mobile screenshots showing the two
states, and keep the browser-check JSON with zero network, console, navigation,
resource and accessibility errors. This rule exists to prevent a false pass
where an archive looked correct while the same records were still duplicated
in the main list.

## Cleanup rule

Never leave a Tomcat, Maven/Surefire fork, Brave, Chrome, Edge or Playwright process open when it was started only for validation.

Before finishing a validation run:

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq 8080 }
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'mvn|surefire|browser-tomcat10|apache-tomcat-10.1.24|playwright|ms-playwright' } | Select-Object ProcessId,Name,CommandLine
```

The validation-owned Tomcat must be stopped after **every** run. Port 8080
must be completely free before the validation batch is reported as complete;
there is no user-owned/IDE-owned exception for this project. If a listener is
still present, inspect its PID and `catalina.base`, stop that exact GAPE
Tomcat, and verify the port again. Never leave a second GAPE Tomcat on another
port either.

Use this mandatory final check in `finally` (and fail the batch if it prints a
non-zero count):

```powershell
$listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
foreach ($listener in $listeners) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    $process | Select-Object ProcessId,Name,CommandLine
    if ($process.CommandLine -match 'catalina.base|tomcat') {
        Stop-Process -Id $listener.OwningProcess -Force
    }
}
Start-Sleep -Milliseconds 500
$remaining = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
if ($remaining.Count -ne 0) { throw "Port 8080 is not clean after validation." }
Write-Output "Port 8080 listeners after cleanup: 0"
```

## Maven-test Tomcat/database isolation (mandatory)

Before **any** Maven test invocation, focused or complete, stop every Tomcat
that can load the GAPE application against the `gape` schema. This always
includes the real 8080 Tomcat; also inspect IntelliJ IDEA Tomcat bases, old
manual Tomcat installations and arbitrary HTTP ports. Tests must not compete
with the live application. After Maven exits, start/redeploy one clean GAPE
instance again on 8080 before browser validation.

For a complete `mvn -q test` run, the same isolation is mandatory and the
database must additionally be disposable. Do not stop only port 8080 or only
one known QA process.

Use this preflight from the workspace root and inspect the output before
stopping anything:

```powershell
$javaServers = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^java(?:w)?\.exe$' -and
    $_.CommandLine -match 'org\.apache\.catalina\.startup\.Bootstrap|catalina\.base'
}

$gapeTomcats = foreach ($process in $javaServers) {
    $match = [regex]::Match([string]$process.CommandLine, '-Dcatalina\.base="([^"]+)"')
    if (-not $match.Success) { continue }
    $base = $match.Groups[1].Value
    $config = Join-Path $base "webapps\GAPE\WEB-INF\classes\config\db.properties"
    $usesGape = $false
    if (Test-Path -LiteralPath $config) {
        $usesGape = (Get-Content -LiteralPath $config -Raw) -match '(?im)^\s*db\.url\s*=.*(?:localhost|127\.0\.0\.1)[^\r\n]*/gape(?:\?|\s*$)'
    }
    if ($usesGape) {
        [pscustomobject]@{ ProcessId = $process.ProcessId; CatalinaBase = $base; CommandLine = $process.CommandLine }
    }
}

$gapeTomcats | Format-List
```

If the application was started with a `GAPE_DB_URL` environment or JVM
override, inspect that launcher as well; the deployed properties file alone is
not sufficient evidence. After checking the exact process IDs and bases, stop
only the listed processes and re-run the discovery block. It must return no
rows before the suite starts:

```powershell
$gapeTomcats | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Start-Sleep -Seconds 2
# Re-run the discovery block; it must return no rows.
```

The complete suite must use a disposable MySQL schema selected with
`GAPE_DB_URL`; it must not share the active Browser QA or user database. Close
competing Maven/Surefire runs and IDE test runners as well. Because this is the
slowest validation, run one invocation only after focused tests and visual
acceptance, under stable power/CPU/network conditions, and measure it:

```powershell
$started = Get-Date
mvn -q test
$exitCode = $LASTEXITCODE
$elapsed = (Get-Date) - $started
"full-suite exit=$exitCode elapsed=$($elapsed.ToString())"
if ($exitCode -ne 0) { exit $exitCode }
```

A deadlock, database-lock failure or connection-refused error caused by a still
running Tomcat/process is an invalid environment run, not a valid regression
result. Correct the isolation first, repeat the single final run, and record
both the invalid attempt and the clean result in the hand-off.

## Backend data-conformance rule

Every backend, schema, migration or domain-rule change must include a data-conformance audit. Historical data is not an exemption for invalid records: update the migration and seed/demo data so every persisted record satisfies the new rule, or make the record explicitly valid under the new lifecycle (for example, an association closed as `historical`). A historical state is valid only when it has its required lifecycle data (such as `ended_at`) and no longer breaks an active invariant; it is never a compatibility flag for legacy garbage.

Before calling backend work complete:

1. Identify every existing record affected by the new invariant.
2. Normalize persisted and seed/demo data; never leave legacy rows that only work because old code tolerated them. For a temporal or lifecycle rule, normalize the parent record, its persisted derived state, every dependent enrollment, assignment, assessment, grade sheet and historical event-recipient snapshot.
3. Add or update an automated conformance test that fails when the invariant is violated.
4. Rebuild/reseed the validation database when schema, migration or seed data changed, then perform the relevant visual validation.

## Fast visual QA loop for frontend changes

For small JSP/CSS/JS layout fixes, use this order. It is intentionally optimized to avoid the slow pattern of package, start Tomcat, capture, stop, edit, repeat.

### Choose the validation scope

The complete Maven suite is a regression gate, not a browser or visual test. It
must not be run automatically after every small frontend edit.

| Change scope | Required checks | Full `mvn -q test` |
| --- | --- | --- |
| Isolated JSP, JSPF, CSS, JS or static-asset change | Relevant focused structural/unit tests, one build when needed, and exact-state browser checks on desktop and mobile; take screenshots for frontend changes | Not required by default |
| Java method, servlet, service or DAO change confined to one feature | Relevant focused unit/integration tests, build/redeploy when the browser is affected, and browser checks when the UI is affected | Not required by default |
| Application-structure/runtime wiring change (`web.xml`, filters, listeners, bootstrap, shared templates/assets), shared backend contract, schema/migration/seed data, security boundary or cross-cutting infrastructure change | Relevant database/conformance tests and exact-state browser checks where applicable | Required once at the end |

When a request contains several related corrections, batch them and run the
complete suite only once after the focused tests and visual acceptance pass.
Never use the full suite to diagnose a layout defect: it is slow, can mutate the
test database and cannot prove that a modal, tab, spinner or alignment is
visually correct. An explicit user request for “all tests” or “no failures”
overrides the default scope, but still means one final full run, not one run per
individual edit.

### Evidence-first rule for visual defects

A screenshot, report or user observation is evidence of a **specific rendered
state**, not merely of a route. Before changing source, write down the exact
state to reproduce:

| Record before editing | Minimum evidence |
| --- | --- |
| Context | actor/account, route, entity ID, selected tab and any open accordion/modal/menu |
| Viewport | CSS width and height; record browser zoom or the assumption used when matching a supplied screenshot |
| Defect | the two or more elements that should align, overlap, resize or change state |
| Acceptance check | selectors/text to assert, plus the screenshot or DOM measurement that will prove the correction |

For a column-alignment defect, inspect the header and one rendered item at
**each visual depth** (for example, parent row, nested row and aggregate row).
Use `-InspectSelector` to compare their `rect.x` values. Unless an intentional
indent is documented, corresponding column anchors must be within 2 CSS pixels
and `pageOverflowX` must remain `false`. A generic route check, a successful
login or a screenshot of a different accordion state is not evidence that this
criterion passed.

Do this baseline measurement once before the first edit. After every failed
visual attempt, measure the same selectors before making another guess. Do not
run the full Maven suite, restart Tomcat or collect unrelated screenshots to
diagnose a CSS/JSP geometry issue.

For a small visual defect, timebox the first reproduce-and-measure pass to five
minutes and the first narrow correction to five minutes. If the exact state
cannot be reproduced in that time, stop the iteration and report the missing
fixture, route, permission or viewport instead of expanding into broad testing.

### Full-flow visual acceptance and proof standard

The fast loop is only fast because it avoids repeated setup; it is not a
shortcut around acceptance. If the requested behaviour is caused by a user
interaction, the final validation must replay the complete user journey from a
known fixture. A route smoke check or a direct POST proves only that an
endpoint responded; it does not prove that the UI is usable or that the
correct card, tab, modal or row was updated.

Treat a complete-flow validation as one evidence-bound transaction. It starts
with a recorded fixture and ends only after the visible result has settled and
survived a reload. Do not mark it passed because a page eventually rendered or
because an HTTP request returned 2xx. A refused connection, CSP/frame failure,
unexpected navigation, console/network error, unresolved spinner, missing
success/error feedback, or a result that is not visible after reload invalidates
the journey and requires a clean rerun after the cause is fixed.

For every affected journey, use one authenticated Brave/Playwright session and
perform the same visible actions a user performs:

1. Open the requested route and select the correct actor, entity, card/tab and
   expanded block. Record the URL and viewport before interacting.
2. Click the visible control that starts the journey (card, tab, menu, modal
   trigger or action button). Do not replace that action with a URL jump,
   `fetch`, form POST, or a script that changes `element.value`/class names.
3. Capture the important intermediate state before the choice, after the modal
   or menu opens, and after each required selection. Assert both appearance and
   behaviour: disabled controls must be visibly disabled and not actionable;
   enabled controls must be visibly enabled and actionable.
4. Fill and select every required field through the rendered controls, then
   click the rendered submit button. Record the request status, spinner/loading
   state, visible success/error message, final URL and selected card/tab.
5. Verify the result in the UI at the destination state: the new/updated row,
   ordering, status, count, badge, modal closure and absence of an unwanted
   redirect must all be asserted. If the task changes ordering, collect the
   complete ordered list and the IDs/text of the affected items, not only the
   last item.
6. Repeat the same acceptance journey at the required mobile viewport. The
   mobile run must use real clicks and prove that the relevant controls remain
   visible/actionable; a desktop screenshot alone is insufficient.

For asynchronous modal or iframe flows, wait on a meaningful settled
condition—not an arbitrary short delay: the frame document must load, the
loading indicator must disappear, the submit request must finish, the modal
must close (or remain open when that is the requirement), and the parent page
must show the expected updated state. Capture evidence before the click, while
the operation is loading, and after the final state. If any condition cannot be
observed, the flow is unverified rather than successful.

For a create flow opened in a modal/iframe, the minimum acceptance trace is:

1. Screenshot the parent page before opening it, including the trigger's
   dimensions and enabled state.
2. Click the trigger and prove that it changes only to the intended spinner
   state; wait for the frame to load and prove that the complete form, not an
   error page or a second dashboard, is visible.
3. Exercise the required-field rules with real controls, including at least
   one invalid/disabled submit attempt where applicable, then fill every field
   through visible input/select events.
4. Click the rendered submit button once, record the request/response and
   spinner transition, and wait for the success message and parent update.
5. Prove that the modal closes exactly when specified, does not reopen by
   itself, the parent remains on the requested card/tab, and exactly one
   expected row/count/state was added or changed.
6. Reload the parent page and repeat the final assertion. A row seen only in
   transient DOM or only inside the iframe is not persistence proof.

Use pointer/keyboard-capable browser actions for acceptance (`locator.click()`,
`locator.fill()`, `locator.selectOption()` or CDP input events). DOM evaluation
is allowed for read-only measurements and assertions, but must not be used to
silently perform the action being tested. The helper's `-ClickSelector` and
`-ClickText` options are suitable for quick smoke checks; for a workflow with
multiple dependent fields or a submit result, use a Playwright/CDP flow that
records each action and response.

The proof package for a completed visual journey must contain:

- a short click trace (actor, route, viewport, ordered actions and expected
  observation after each action);
- screenshots of the disputed state, including the open/selected modal or
  expanded layer, at desktop and mobile sizes;
- selector assertions or geometry measurements for every acceptance criterion
  (for example, disabled state, button state, row order, column `rect.x`,
  `pageOverflowX=false`);
- the browser-check JSON or equivalent log showing zero unexpected network,
  console, page, image and accessibility errors; and
- the final fixture/cleanup result, including any IDs created for QA and proof
  that they were removed or restored.

The following are **not** sufficient proof by themselves: a screenshot of a
different tab or collapsed block, a successful login, a 2xx response from a
direct request, an assertion based only on hidden inputs, a DOM mutation that
did not come from a visible action, or a screenshot taken before the final
state settled. When an interaction is the requirement, the evidence must show
the state before the click, the state immediately after the click/choice, and
the final persisted state.

For create/update/delete journeys, use a unique QA title or fixture marker,
record the precondition and resulting identifier/count, and clean it through
the UI after the screenshots whenever possible. If UI cleanup is impossible,
use a narrowly scoped database cleanup for the recorded identifiers only and
verify the rows and dependent records are gone. Never leave test-created data
in the user's database or leave the validation server running.

Before declaring the journey accepted, complete an evidence matrix like this
for **every acceptance criterion** (not only for the criterion that was easiest
to reproduce):

| Criterion | Initial state | Real action trace | Expected proof |
| --- | --- | --- | --- |
| Dependent control | Parent unselected; child visibly disabled | Click parent, choose an option, clear it again | screenshots/selector assertions for disabled -> enabled -> disabled |
| Required submit | Required field empty and then valid | Click submit in both states | disabled/no request or validation message, then spinner/request/success |
| Navigation | Source card/tab selected | Click the action and wait for completion | final URL/card/tab remains the requested one; no transient wrong destination |
| Ordering/persistence | Record the complete pre-action list | Create/update/delete through the UI | complete post-action list, IDs/counts, persisted state and screenshot |
| Responsive state | Same fixture at desktop and mobile widths | Repeat the visible clicks | equivalent behaviour, visible controls, no horizontal overflow |

If a branch cannot be exercised with a real visible click (for example, the
fixture is missing or the browser is unavailable), mark that criterion
**unverified/blocked** and report the missing proof. Do not replace it with a
direct API call and do not report the task as visually validated.

### Required order

1. Inspect the changed JSP/CSS first with `rg` and focused file reads.
2. Reproduce the exact visual state and record the evidence described above. Do not edit from a generic route render when the defect occurs in a nested, expanded, modal or selected state.
3. Run only the relevant structural/unit tests.
4. Before the test batch, stop the identified GAPE Tomcat on 8080, build once with `mvn -q -DskipTests package`, and start/redeploy one clean instance on 8080.
5. Run fast DOM checks first with `browser-check-flow.ps1 -Port 8080 -NoScreenshot`, including the exact-state selectors and viewport. For a frontend change, this step is mandatory even when the integrated browser is unavailable.
6. Fix issues while the server is still running:
   - for JSP/CSS/JS under `src/main/webapp`, sync only the edited files into the clean deployment served on 8080 and refresh/check again;
   - for Java, resources, servlet mappings or compiled classes, rebuild and redeploy once on 8080 after the changes are stable;
   - do not rebuild/reset the database unless schema, seed/demo data, migrations or the test scenario itself changed.
7. Take final screenshots only after the exact-state DOM checks pass. The screenshot must show the disputed state, not only the page header or a collapsed/default state.
8. If the scope table, task risk or user hand-off requires `mvn -q test`, record the fixture, stop **all Tomcats using the `gape` database** (including the user Tomcat on 8080, Browser Tomcat and IDE Tomcats on arbitrary ports), switch the suite to a disposable schema, and run it **once now**, after visual acceptance. The full suite is a final regression gate, not a diagnostic tool for a layout defect.
9. In `finally`, stop the validation-owned Tomcat on 8080 and verify that
   port 8080 has zero listeners. Also verify that no second GAPE Tomcat is
   listening elsewhere; a non-zero final listener count invalidates the batch.

Never run `mvn -q test` once per small edit or twice in the same validation pass.
A second run is justified only when source/test configuration changed, a failed
test was repaired, or the first run was invalidated by a known infrastructure
problem. If the suite alters the local database, restore the recorded fixture
and recheck the exact visual scenario afterwards rather than assuming the
previous screenshot still represents the current data.

### Fixture checkpoint before disruptive tests

Before a command that can recreate a schema, seed data or execute a test class
with database reset hooks, record the visual fixture being used: account, route,
entity IDs and the state needed to reveal the defect. Determine whether the
command changes that fixture and how it can be restored **before** running it.

- Do not use a database-resetting test merely to recover a visual scenario that
  was lost during an earlier test run.
- Prefer a disposable test database for the full suite. Do not run it against
  data that the active browser QA session needs to preserve.
- If a final suite legitimately changes the fixture, restore it through its
  documented seed/setup path and repeat the exact-state check once.

### Validation time record

For each frontend correction, include a compact timing record in the hand-off:

| Phase | Record |
| --- | --- |
| Baseline | exact route/state, viewport and elapsed time to reproduce/measure |
| Iterations | each source edit and the single DOM measurement that accepted or rejected it |
| Automated checks | command, exit code and elapsed time; distinguish focused tests from the full suite |
| Final evidence | screenshot path, selector measurements and cleanup/port result |

Use measured command durations where available. If a duration is estimated,
label it as such. This makes avoidable waits, repeated builds and duplicate full
suites visible instead of attributing them to a "simple visual change".

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

Do not use `-Prepare` or any 18080/Browser-Tomcat preparation command for this
project's evidence. After the mandatory 8080 preflight and clean deployment,
run multiple routes in the same batch with `browser-check-flow.ps1 -Port 8080`
without restarting between routes.

Recommended single-session command shape:

```powershell
$ErrorActionPreference = "Stop"
$hadRealTomcat = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).Count -gt 0
if ($hadRealTomcat) {
    throw "Port 8080 must be stopped and cleanly redeployed before validation."
}

try {
    mvn -q "-Dtest=TemplateStructureTest,TemplateAssetReferenceTest" test
    mvn -q -DskipTests package
    # Start/redeploy the clean real Tomcat on port 8080 here.

    # Fast checks first: no screenshots, no process restart.
    .\docs\dev\scripts\browser-check-flow.ps1 -Port 8080 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 1920 `
      -Height 1200 `
      -NoScreenshot

    .\docs\dev\scripts\browser-check-flow.ps1 -Port 8080 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 390 `
      -Height 900 `
      -NoScreenshot

    # Final evidence only after the checks above pass.
    .\docs\dev\scripts\browser-check-flow.ps1 -Port 8080 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 1920 `
      -Height 1200 `
      -Out "target\browser-screenshots\assessment-detail-desktop.png"

    .\docs\dev\scripts\browser-check-flow.ps1 -Port 8080 `
      -Route "/learning/assessments/98" `
      -ExpectText @("Complete Databases Assessment", "Builder", "Enrollments", "Attempts") `
      -Width 390 `
      -Height 1100 `
      -Out "target\browser-screenshots\assessment-detail-mobile.png"
} finally {
    # Stop the validation-owned Tomcat on 8080 and require a completely free port.
    $listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
        if ($process.CommandLine -match 'catalina.base|tomcat') {
            Stop-Process -Id $listener.OwningProcess -Force
        }
    }
    Start-Sleep -Milliseconds 500
    $remaining = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
    if ($remaining.Count -ne 0) { throw "Port 8080 is not clean after validation." }
    Write-Output "Port 8080 listeners after cleanup: 0"
}
```

### Hot sync for frontend-only edits

When the clean real Tomcat on 8080 is already running and the change is only
in `src/main/webapp`, do not rebuild/restart just to test a JSP, CSS or JS
tweak. Copy the edited file into the exploded webapp actually deployed on
8080 (or use the IDE's update-deployed-application action), preserving the
path below `src/main/webapp`.

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
    # Set this to the exploded GAPE webapp deployed by the real 8080 Tomcat.
    $realWebappRoot = "<real-8080-GAPE-webapp-root>"
    if (-not (Test-Path -LiteralPath $realWebappRoot)) {
        throw "Set realWebappRoot to the webapp deployed on port 8080."
    }
    $target = Join-Path $realWebappRoot $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
    Write-Output "Synced $relative"
}

Sync-WebappFile "src\main\webapp\WEB-INF\views\learning\assessment-builder.jsp"
Sync-WebappFile "src\main\webapp\assets\css\main.css"
```

After syncing, rerun the same `browser-check-flow.ps1 -Port 8080` command. Do
not start a second Tomcat or switch to 18080. For Java/resources/mappings,
rebuild and redeploy the same real 8080 application once after the changes are
stable.

### Java redeploy without database reset

Java changes need compiled classes to be rebuilt and the webapp redeployed, but that does not mean the database must be rebuilt.

Use the smallest valid action:

| Change made | Fast action | Do not do |
| --- | --- | --- |
| JSP, JSPF, CSS, JS, static assets | `Sync-WebappFile`, then rerun `browser-check-flow.ps1 -Port 8080` | Do not start another port, reset DB or interrupt the active 8080 batch |
| Java method body, Servlet logic, Service/DAO logic, auth policy | focused test if relevant, then `mvn -q -DskipTests package` and one clean redeploy on 8080 | Do not reset/reseed DB unless the change depends on new persisted data |
| New/renamed Java class, servlet mapping, filter, listener, dependency, resource on classpath | `mvn -q -DskipTests package` and one redeploy | Do not reset/reseed DB by default |
| `schema.sql`, `drop.sql`, trigger/constraint, seed data, demo data, data bootstrap code | run the relevant DB/bootstrap tests and rebuild/reseed only the affected validation database | Do not use the browser QA database reset as a reflex for unrelated Java/layout changes |
| Test fixture/data-only change | run the relevant focused tests; reload data only if that test requires it | Do not redeploy Tomcat unless browser runtime changed |

The clean 8080 redeploy does not recreate schema or seed data unless that is
explicitly requested. Treat database rebuild/reseed as a separate action and
keep the single real 8080 deployment as the only browser target.

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

Use the full suite command only when the scope table, risk assessment or user
request calls for a final regression gate:

```powershell
mvn -q test
```

Before running it, execute the mandatory Tomcat/database isolation preflight
above: stop **all** GAPE Tomcats, not only Browser Tomcat and the user Tomcat
listening on 8080. The suite and any application must not compete for the same
database. Record
the browser fixture first because database-resetting tests may remove or alter
the entities needed by the final visual check. After the suite, restore that
fixture through its documented seed/setup path and repeat the exact-state check
once. A transient deadlock or stale test state is reason to repair the test
environment and rerun the affected validation, not to start repeated full
suites while the cause is unknown.

Full-suite duration is deliberately unbounded: these tests are more complete
and may take a long time. Do **not** configure a timeout, interrupt the run
because it exceeds an expected duration, or replace it with a focused subset.
Measure and report the actual elapsed time after Maven exits. Do not treat the
Java/WebP native-access warning as a failure when Maven exits with code `0`.

If Maven has genuinely exited or an external infrastructure failure is
confirmed, check for stale Maven/Surefire/Java test forks before running it
again:

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

## Real Tomcat setup for browser validation

The browser helpers must target the real deployment on 8080. The old
`target/browser-tomcat10`/18080 setup is not an accepted test target for this
project and must not be used for test evidence.

- base URL: `http://localhost:8080/GAPE/`
- HTTP port: `8080`
- deployment: the clean Tomcat/IDE deployment opened by the user
- default account: `admin@gape.local` / `Password#2026`

Start/redeploy the real Tomcat deployment on 8080 using the project's IDE or
Tomcat run configuration after the mandatory preflight. Do not use
`browser-prepare.ps1`, `tomcat-install.ps1` or any other 18080 helper for test
evidence.

Build before redeploying when required:

```powershell
mvn -q -DskipTests package
```

Stop the validation-owned 8080 process when the validation is finished and
verify the port is free:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
```

If the real 8080 deployment cannot start because of local path quoting,
encoding or IDE configuration, fix that deployment or report the validation as
blocked; do not fall back to 18080.

## Fast browser checks

For a generic authenticated page check after the clean 8080 deployment is
running:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
  -Route "/learning/assessments/new" `
  -ExpectText "Assessment" `
  -Out "target\browser-screenshots\assessment-new.png"
```

Do not use `-Prepare`; it targets the retired isolated-server workflow. When
iterating quickly, keep the one clean 8080 deployment for the duration of the
active batch, then stop it once.

For mobile layout checks:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
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
- `-ProbeAnimationSelector ".spinner i"`: after each real click, record the
  computed animation name, duration, iteration count, play state and transform
  in the JSON evidence; use this for transient spinner checks together with a
  short `-ProbeAnimationDelayMs` and do not require the final modal selector in
  that same transient capture.
- `-FrameProbeAnimationSelector ".gape-modal-submit-spinner .ph-circle-notch"`:
  record the computed animation immediately after a submit click inside a
  modal iframe. Use it with `-FrameClickSelector` and bounded latency to prove
  that the submit button spinner starts before the server response closes the
  modal.
- `-RejectText "Exception"`: fail if text is present.
- `-NoScreenshot`: run faster when DOM/layout proof is enough.
- `-AllowNetworkErrors`, `-AllowConsoleErrors`, `-AllowBrokenImages`, `-AllowAccessibilityIssues`: temporary diagnostic opt-outs; do not use them for final evidence.

By default the helper fails on HTTP 4xx/5xx responses, failed resource loads, `console.error`, uncaught JavaScript errors, visible broken images, duplicate IDs, missing document language, visible images without `alt`, and visible controls/actions without an accessible name.

### Visual regression baseline

Create or intentionally update a baseline only after the DOM, network and accessibility checks pass:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
  -Route "/admin/organizations" `
  -Out "target\browser-screenshots\organizations-current.png" `
  -Baseline "docs\tests\browser-baselines\organizations-1920x1200.png" `
  -UpdateBaseline
```

Compare a later render against it:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Port 8080 `
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
.\docs\dev\scripts\browser-check-add-content.ps1 `
  -Port 8080 `
  -ClassGroupId 53 `
  -ContentType Video `
  -Out "target\browser-screenshots\add-content-modal-fast.png"
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
```

## Screenshots

When a screenshot is needed, use the local Brave/CDP scripts instead of the integrated Browser screenshot path:

```powershell
.\docs\dev\scripts\browser-screenshot.ps1 `
  -Port 8080 `
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
