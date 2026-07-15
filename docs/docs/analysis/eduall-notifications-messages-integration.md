# EduAll notifications and messages integration

Date: 02-07-2026

## Scope analysed

The EduAll template was reviewed across the dashboard, role pages and reusable JSP fragments. The relevant existing pieces were:

- `admin/admin-message.jsp`: original EduAll inbox/chat mockup with sidebar, notification dropdown, conversation list, thread and composer.
- `student/student/message/student-message.jsp`: student placeholder for messages.
- `instructor/instructor-message.jsp` and `coordinator/coordinator-message.jsp`: role aliases that included the admin mock page.
- `messages.jsp`, `forms.jsp`, `tables.jsp`: legacy EduAll pages with duplicated sidebar/header/message snippets.
- `WEB-INF/fragments/dashboard-sidebar.jspf`: shared dashboard menu and message link selection.
- `WEB-INF/fragments/dashboard-topbar.jspf`: shared dashboard header where notification badges belong.
- `WEB-INF/fragments/student-dashboard-sidebar.jspf` and `student-template-header.jspf`: student menu entry points.
- Dashboard pages with mock notification or notice board sections, especially `admin/admin-dashbord.jsp`.
- Static comment/review areas in `content.jsp` and `lesson-details.jsp`.
- Dynamic learning views, especially `WEB-INF/views/learning/lesson-list.jsp`, which already marks forums/comments as future contextual work.

## Integration implemented

The final UI entry point is the authenticated servlet route `/messages`.

The old EduAll message JSPs now forward to `/messages`, preserving legacy URLs without keeping mock data alive:

- `admin/admin-message.jsp`
- `student/student-message.jsp`
- `student/student/message/student-message.jsp`
- `instructor/instructor-message.jsp`
- `coordinator/coordinator-message.jsp`
- `messages.jsp`

The shared dashboard menus now point directly to `/messages`, and the dashboard topbar includes `dashboard-notification-dropdown.jspf` for unread badges and recent notifications.

## Backend connection

The JSP does not query the database directly. It receives data prepared by:

- `CommunicationServlet`
- `CommunicationReadService`
- `CommunicationDAO`
- `CommunicationViewFactory`
- `Communication*View` classes under `pt.isel.gape.web.view`

Mutating operations use the tested backend services:

- `ChannelService` for channel creation.
- `ChannelParticipationService` for adding participants.
- `MessageService` for human messages, replies, scheduling, attachment validation and read receipts.
- `NotificationService` for system notifications delivered inside the application.

`AuthorizationPolicy` protects `/messages`, and `CsrfFilter` protects `/messages/*` POST actions.

## UI coverage

The page supports:

- notification badge and dropdown in the dynamic dashboard header;
- inbox with active channels for the authenticated participant;
- channel detail and participants;
- message thread;
- message sending;
- replies inside the same channel;
- scheduled delivery input;
- attachment message input;
- forum/comment channel display and comment defaulting for forum-like channels;
- channel creation for message, forum, comments, announcement, system and context channel types;
- participant addition;
- flash success/error messages from backend validations.

## Important constraints

Static EduAll comments in `content.jsp` and `lesson-details.jsp` were not wired directly. The backend communication model supports comments/forums through `ChannelType.COMMENTS` and `ChannelType.FORUM` associated with class groups, content blocks or assessments. A lesson-specific comment wall would need either a lesson-channel association or a clear mapping from lessons to an existing content block/class group channel.

The UI exposes class group, block and assessment context IDs when creating channels. The backend validates the structural chain, including block and assessment consistency with class groups.

All message and notification delivery is internal to the application. User email addresses remain identity/profile data and are not used as a communication channel.
