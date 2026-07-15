package pt.isel.gape.web.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommunicationTemplateTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path JAVA = Path.of("src/main/java");

    @Test
    void messagesViewFocusesOnUserMessagesWithoutChannelManagement() throws IOException {
        String jsp = Files.readString(WEBAPP.resolve("WEB-INF/views/transversal/messages.jsp"));
        String threadFragment = Files.readString(WEBAPP.resolve("WEB-INF/fragments/messages-thread-panel.jspf"));
        String contactListFragment = Files.readString(WEBAPP.resolve("WEB-INF/fragments/messages-contact-list.jspf"));
        String messageRowsFragment = Files.readString(WEBAPP.resolve("WEB-INF/fragments/messages-chat-items.jspf"));
        String threadJsp = Files.readString(WEBAPP.resolve("WEB-INF/views/transversal/messages-thread.jsp"));
        String olderMessagesJsp = Files.readString(WEBAPP.resolve("WEB-INF/views/transversal/messages-thread-messages.jsp"));

        assertTrue(contactListFragment.contains("communicationPage.conversations"));
        assertTrue(threadFragment.contains("communicationPage.messages"));
        assertTrue(jsp.contains("Conversations"));
        assertTrue(jsp.contains("New message"));
        assertTrue(jsp.contains("studentMessagesPage"));
        assertTrue(jsp.contains("messages-student-hero-band"));
        assertTrue(jsp.contains("messages-student-hero-heading"));
        assertTrue(threadFragment.contains("aria-label=\"Attach files\""));
        assertFalse(jsp.contains("active chats"));
        assertTrue(messageRowsFragment.contains("message-bubble"));
        assertTrue(messageRowsFragment.contains("message-bubble__text"));
        assertTrue(messageRowsFragment.indexOf("message.hasAttachment") < messageRowsFragment.indexOf("not empty message.body"));
        assertTrue(jsp.contains("messages-contact-panel"));
        assertTrue(jsp.contains("flex: 0 0 336px"));
        assertTrue(jsp.contains("max-height: calc(105vh - 474.6px)"));
        assertTrue(jsp.contains("min-height: 273px"));
        assertTrue(jsp.contains("border-right-dashed"));
        assertTrue(threadFragment.contains("messages-chat-scroll"));
        assertTrue(contactListFragment.contains("conversation.previewLabel"));
        assertTrue(jsp.contains("messages-contact-list.jspf"));
        assertTrue(jsp.contains("replaceConversationList(html)"));
        assertTrue(threadJsp.contains("data-messages-contact-list-payload"));
        assertTrue(threadJsp.contains("messages-contact-list.jspf"));
        assertTrue(jsp.contains("data-user-search-input"));
        assertTrue(jsp.contains("data-user-search=\"${recipient.userId}"));
        assertTrue(jsp.contains("fetch(requestUrl"));
        assertTrue(jsp.contains("threadUrl(url)"));
        assertTrue(jsp.contains("threadCache"));
        assertTrue(jsp.contains("setActiveConversation(peerUserId)"));
        assertTrue(jsp.contains("loadOlderMessages(button)"));
        assertTrue(jsp.contains("data-read-conversation-url"));
        assertTrue(jsp.contains("markSelectedConversationRead"));
        assertTrue(jsp.contains("pagehide"));
        assertTrue(jsp.contains("markConversationRead(leavingPeerUserId"));
        assertFalse(jsp.contains("scrollConversationToBottom();\n        markSelectedConversationRead();"));
        assertTrue(threadFragment.contains("data-load-older-messages"));
        assertFalse(threadFragment.contains("communicationPage.loadedMessageCount"));
        assertFalse(threadFragment.contains("communicationPage.messageCount"));
        assertFalse(threadFragment.contains("data-thread-unread-pill"));
        assertTrue(threadFragment.contains("data-total-unread-count"));
        assertTrue(threadFragment.contains("recipientUserId"));
        assertTrue(threadFragment.contains("/messages/send"));
        assertTrue(threadFragment.contains("enctype=\"multipart/form-data\""));
        assertTrue(threadFragment.contains("name=\"attachmentFile\""));
        assertTrue(threadFragment.contains("multiple"));
        assertTrue(threadFragment.contains("data-message-send-form"));
        assertTrue(threadFragment.contains("<textarea"));
        assertTrue(threadFragment.contains("data-message-body-input"));
        assertTrue(messageRowsFragment.contains("/messages/attachments/"));
        assertTrue(messageRowsFragment.contains("?inline=1"));
        assertTrue(messageRowsFragment.contains("<img"));
        assertTrue(messageRowsFragment.contains("<video"));
        assertFalse(messageRowsFragment.contains("Mark read"));
        assertFalse(messageRowsFragment.contains("data-message-read-form"));
        assertTrue(messageRowsFragment.contains("message-bubble--sent"));
        assertTrue(messageRowsFragment.contains("message-bubble--received"));
        assertTrue(jsp.contains("submitMessageForm(form)"));
        assertTrue(jsp.contains("new FormData(form)"));
        assertTrue(jsp.contains("Maximum 5 files per message."));
        assertFalse(jsp.contains("Direct conversations with users."));
        assertFalse(jsp.contains("${communicationPage.conversationCount} conversations"));
        assertFalse(jsp.contains("Message sent."));
        assertTrue(threadJsp.contains("messages-thread-panel.jspf"));
        assertTrue(olderMessagesJsp.contains("data-messages-older-payload"));
        assertFalse(jsp.contains("<h1 class=\"text-24 fw-semibold text-neutral-700"));
        assertFalse(jsp.contains("replaceSection(documentFragment, '[data-messages-contact-list]'"));
        assertFalse(jsp.contains("<select id=\"new-message-recipient\""));
        assertFalse(jsp.contains("Write a message"));
        assertFalse(jsp.contains("communicationPage.channels"));
        assertFalse(jsp.contains("communicationPage.selectedChannel"));
        assertFalse(jsp.contains("/messages/channels"));
        assertFalse(jsp.contains("/messages/participants"));
        assertFalse(jsp.contains("/messages/notifications"));
        assertFalse(jsp.contains("name=\"channelId\""));
        assertFalse(jsp.contains("System notification"));
        assertFalse(jsp.contains("Create channel"));
    }

    @Test
    void dashboardMenusUseDynamicMessagesRouteAfterTopbarRemoval() throws IOException {
        String topbar = Files.readString(WEBAPP.resolve("WEB-INF/fragments/dashboard-topbar.jspf"));
        String dropdown = Files.readString(WEBAPP.resolve("WEB-INF/fragments/dashboard-notification-dropdown.jspf"));
        String sidebar = Files.readString(WEBAPP.resolve("WEB-INF/fragments/dashboard-sidebar.jspf"));
        String studentSidebar = Files.readString(WEBAPP.resolve("WEB-INF/fragments/student-dashboard-sidebar.jspf"));

        assertTrue(topbar.contains("gape-dashboard-mobile-menu-toggle"));
        assertTrue(!topbar.contains("dashboard-notification-dropdown.jspf"));
        assertTrue(dropdown.contains("notificationUnreadCount"));
        assertTrue(dropdown.contains("/messages?messageId="));
        assertFalse(dropdown.contains("channelId="));
        assertTrue(sidebar.contains("value=\"/messages\""));
        assertTrue(sidebar.contains("data-sidebar-message-badge"));
        assertTrue(sidebar.contains("data-sidebar-event-badge"));
        assertTrue(sidebar.contains("data-sidebar-learning-pending-badge"));
        assertTrue(sidebar.contains("gape-sidebar-event-badge"));
        assertTrue(sidebar.contains("sidebarMessageUnreadCount > 0"));
        assertTrue(sidebar.contains("sidebarEventUnreadCount > 0"));
        assertTrue(sidebar.contains("sidebarLearningPendingWorkCount > 0"));
        assertTrue(sidebar.contains("Messages"));
        assertFalse(sidebar.contains(">Message\n"));
        assertTrue(studentSidebar.contains("${pageContext.request.contextPath}/messages"));
        assertTrue(studentSidebar.contains("data-sidebar-message-badge"));
        assertTrue(studentSidebar.contains("data-sidebar-event-badge"));
        assertTrue(studentSidebar.contains("gape-sidebar-event-badge"));
        assertTrue(studentSidebar.contains("sidebarMessageUnreadCount > 0"));
        assertTrue(studentSidebar.contains("sidebarEventUnreadCount > 0"));
        assertTrue(studentSidebar.contains("Messages"));
        assertFalse(studentSidebar.contains(">Message\n"));
    }

    @Test
    void legacyMessagePagesForwardToServlet() throws IOException {
        assertForward("admin/admin-message.jsp");
        assertForward("messages.jsp");
        assertForward("student/student-message.jsp");
        assertForward("student/student/message/student-message.jsp");
        assertForward("instructor/instructor-message.jsp");
        assertForward("coordinator/coordinator-message.jsp");
    }

    @Test
    void servletAndFiltersProtectMessagesEndpoint() throws IOException {
        String servlet = Files.readString(JAVA.resolve("pt/isel/gape/web/controller/CommunicationServlet.java"));
        String authPolicy = Files.readString(JAVA.resolve("pt/isel/gape/security/authorization/AuthorizationPolicy.java"));
        String csrfFilter = Files.readString(JAVA.resolve("pt/isel/gape/web/filter/CsrfFilter.java"));

        assertTrue(servlet.contains("urlPatterns = {\"/messages\", \"/messages/*\"}"));
        assertTrue(servlet.contains("\"thread\""));
        assertTrue(servlet.contains("\"read-conversation\""));
        assertTrue(servlet.contains("\"attachments\""));
        assertTrue(servlet.contains("@MultipartConfig"));
        assertTrue(servlet.contains("loadThread"));
        assertTrue(servlet.contains("loadOlderThreadMessages"));
        assertTrue(servlet.contains("MAX_ATTACHMENTS_PER_MESSAGE"));
        assertTrue(servlet.contains("saveContentFile(format, attachmentPart)"));
        assertTrue(servlet.contains("uploadedFiles.size() == 1"));
        assertTrue(servlet.contains("DirectMessageContent.attachment("));
        assertTrue(servlet.contains("directMessageService.sendDirectMessages("));
        assertTrue(servlet.contains("messageService.markDirectConversationRead"));
        assertTrue(servlet.contains("private void renderThread("));
        assertTrue(servlet.contains("renderConversationUpdate(request, response"));
        assertTrue(servlet.contains("readService.loadSnapshot(currentUser.userId(), recipientUserId, messageId)"));
        assertTrue(servlet.contains("\"inline\""));
        assertTrue(authPolicy.contains("isPathOrChild(path, \"/messages\")"));
        assertTrue(csrfFilter.contains("SAFE_METHODS")
                && csrfFilter.contains("sessionManager.isValidCsrfToken"));
    }

    private static void assertForward(String relativePath) throws IOException {
        String jsp = Files.readString(WEBAPP.resolve(relativePath));
        assertTrue(jsp.contains("<jsp:forward page=\"/messages\" />"), relativePath);
    }
}
