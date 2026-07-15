<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div data-messages-older-payload
     data-has-older-messages="${communicationPage.hasOlderMessages}"
     data-next-before-message-id="${communicationPage.oldestMessageId}">
    <%@ include file="/WEB-INF/fragments/messages-chat-items.jspf" %>
</div>
