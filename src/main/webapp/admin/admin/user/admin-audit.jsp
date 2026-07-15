<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "audit");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Audit</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>
<div class="dashbord bg-main-25 w-100 overflow-hidden">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-vh-100">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>
                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">
                                <c:choose>
                                    <c:when test="${not empty filteredUserId}">Audit Log for User ${filteredUserId}</c:when>
                                    <c:otherwise>Audit Log</c:otherwise>
                                </c:choose>
                            </h2>
                    <span class="text-14 text-neutral-500">Critical operations over personal data and deletion.</span>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Date</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Operation</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Actor</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Entity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Outcome</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">IP</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="log" items="${logs}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display><c:out value="${log.occurredAt}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${log.operationType}"/></span>
                                        <span class="d-block text-12 text-neutral-500">Session: <c:out value="${empty log.sessionId ? '-' : log.sessionId}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${empty log.userId ? '-' : log.userId}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${log.affectedEntityType}"/> / <c:out value="${log.affectedEntityIdentifier}"/>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="bg-neutral-40 text-neutral-600 px-16 py-8 border-neutral-30 border rounded-pill text-14"><c:out value="${log.outcome}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${empty log.sourceIp ? '-' : log.sourceIp}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty logs}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No audit records.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
