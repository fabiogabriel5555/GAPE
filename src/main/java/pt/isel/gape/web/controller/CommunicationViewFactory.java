package pt.isel.gape.web.controller;

import java.util.List;

import pt.isel.gape.transversal.model.CommunicationSnapshot;
import pt.isel.gape.transversal.model.DirectConversationSummary;
import pt.isel.gape.transversal.model.MessageRecipientSummary;
import pt.isel.gape.transversal.model.MessageSummary;
import pt.isel.gape.web.view.CommunicationConversationView;
import pt.isel.gape.web.view.CommunicationMessageView;
import pt.isel.gape.web.view.CommunicationPageData;
import pt.isel.gape.web.view.CommunicationRecipientView;

final class CommunicationViewFactory {

    CommunicationPageData pageData(CommunicationSnapshot snapshot, long currentUserId) {
        return new CommunicationPageData(
                conversationViews(snapshot.conversations(), currentUserId),
                snapshot.selectedConversation() == null
                        ? null
                        : new CommunicationConversationView(snapshot.selectedConversation(), currentUserId),
                messageViews(snapshot.messages(), currentUserId),
                snapshot.selectedMessage() == null ? null : new CommunicationMessageView(snapshot.selectedMessage(), currentUserId),
                messageViews(snapshot.notifications(), currentUserId),
                recipientViews(snapshot.recipients()),
                snapshot.unreadCount(),
                snapshot.hasOlderMessages()
        );
    }

    List<CommunicationMessageView> notificationViews(CommunicationSnapshot snapshot, long currentUserId) {
        return messageViews(snapshot.notifications(), currentUserId);
    }

    private static List<CommunicationMessageView> messageViews(List<MessageSummary> messages, long currentUserId) {
        return messages.stream()
                .map(message -> new CommunicationMessageView(message, currentUserId))
                .toList();
    }

    private static List<CommunicationConversationView> conversationViews(
            List<DirectConversationSummary> conversations,
            long currentUserId
    ) {
        return conversations.stream()
                .map(conversation -> new CommunicationConversationView(conversation, currentUserId))
                .toList();
    }

    private static List<CommunicationRecipientView> recipientViews(List<MessageRecipientSummary> recipients) {
        return recipients.stream()
                .map(CommunicationRecipientView::new)
                .toList();
    }
}
