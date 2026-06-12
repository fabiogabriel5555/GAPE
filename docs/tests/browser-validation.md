# Browser validation workflow

Use this workflow after frontend changes. It keeps Browser validation repeatable and avoids losing time on port discovery, manual Tomcat setup and repeated UI login.

## Fast setup

1. Build the app when code changed:

```powershell
mvn -q -DskipTests package
```

2. Start/redeploy the Browser Tomcat on the fixed port:

```powershell
.\scripts\browser-prepare.ps1 -SkipPackage
```

The script always targets `http://localhost:18080/GAPE`, redeploys `target/gape`, logs in with the seed admin account and prints authenticated URLs using `;jsessionid=...`.

3. Open the printed `Session base` URL in the in-app Browser, then navigate from the printed recommended URLs.

4. Stop the local Browser Tomcat when finished:

```powershell
.\scripts\browser-stop.ps1
```

## Browser plugin defaults

Use longer timeouts for local JSP pages and screenshots:

- Navigation/actions: at least `120000` ms.
- Screenshots: at least `180000` ms.
- Prefer direct authenticated URLs from `browser-prepare.ps1` over typing credentials into the UI for every run.
- If the Browser screenshot call times out but navigation worked, still inspect DOM, console errors, route transitions and visible page content. Record the screenshot timeout explicitly instead of treating the Browser validation as complete.

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
- `admin.learning@gape.local`: course/subject learning administrator.
- `admin.enroll@gape.local`: class-group enrollment administrator.
- `coord@gape.local`: coordinator.
- `teacher@gape.local`: teacher.
- `student@gape.local`: student.
- `multi@gape.local`: coordinator, teacher and student profiles.
