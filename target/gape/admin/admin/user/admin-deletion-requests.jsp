<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "deletion-admin");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Deletion Processing</title>
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
                                    <c:when test="${not empty filteredUserId}">Deletion Requests for User ${filteredUserId}</c:when>
                                    <c:otherwise>Deletion Requests</c:otherwise>
                                </c:choose>
                            </h2>
                            <span class="text-14 text-neutral-500">Final states require a processing date.</span>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Request</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">User</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Submitted</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Processed</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="requestItem" items="${requests}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 text-14 text-neutral-700">#${requestItem.id}</td>
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/admin/users/${requestItem.submitterUserId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">User ${requestItem.submitterUserId}</a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${empty requestItem.reason ? 'No reason provided' : requestItem.reason}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display><c:out value="${requestItem.submittedAt}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display><c:out value="${requestItem.processedAt}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${requestItem.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${requestItem.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <c:choose>
                                            <c:when test="${requestItem.finalState}">
                                                <span class="text-14 text-neutral-400">Closed</span>
                                            </c:when>
                                            <c:otherwise>
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600" data-bs-toggle="modal" data-bs-target="#processRequest${requestItem.id}" title="Process">
                                                    <i class="ph ph-check-square"></i>
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="modal fade" id="processRequest${requestItem.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0 text-start">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Process Request #${requestItem.id}</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <form action="${pageContext.request.contextPath}/admin/deletion-requests/${requestItem.id}/process" method="post">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <input type="hidden" name="returnTo" value="${adminReturnTo}">
                                                        <div class="modal-body">
                                                            <label class="fw-medium text-base text-neutral-800 mb-12" for="state${requestItem.id}">State</label>
                                                            <select id="state${requestItem.id}" name="state" class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-12 mb-16">
                                                                <option value="UNDER_REVIEW">Under review</option>
                                                                <option value="APPROVED">Approved</option>
                                                                <option value="REJECTED">Rejected</option>
                                                                <option value="COMPLETED">Completed</option>
                                                            </select>
                                                            <label class="fw-medium text-base text-neutral-800 mb-12" for="processedAt${requestItem.id}">Processing Date</label>
                                                            <input id="processedAt${requestItem.id}" name="processedAt" type="datetime-local" min="${requestItem.submittedAtInputMinimum}" class="form-control px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-12">
                                                        </div>
                                                        <div class="modal-footer border-neutral-30">
                                                            <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                            <button type="submit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Process</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty requests}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No deletion requests.</td>
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
