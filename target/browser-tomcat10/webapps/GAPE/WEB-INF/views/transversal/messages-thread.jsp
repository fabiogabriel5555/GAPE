<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/fragments/messages-thread-panel.jspf" %>
<c:if test="${communicationPage.hasConversations}">
    <div data-messages-contact-list-payload hidden>
        <%@ include file="/WEB-INF/fragments/messages-contact-list.jspf" %>
    </div>
</c:if>
