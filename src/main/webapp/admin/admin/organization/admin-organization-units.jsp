<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "organizations");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Organic Units</title>
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

                <div class="bg-white rounded-10 px-32 py-28 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
                        <div>
                            <h2 class="text-20 fw-medium text-neutral-700 mb-6">Organic Units</h2>
                            <span class="text-14 text-neutral-500">
                                <c:out value="${organization.name}"/> · <c:out value="${organization.typeLabel}"/>
                            </span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">
                                <i class="ph ph-eye me-8"></i>Detail
                            </a>
                            <c:if test="${canCreateOrganicUnits}">
                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/new" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Unit
                                </a>
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Organic Unit Hierarchy</h3>
                            <span class="text-14 text-neutral-500">Add or modify the units that belong to this organization.</span>
                        </div>
                        <span class="bg-main-50 text-main-600 px-16 py-8 rounded-pill text-14 fw-medium">
                            <c:out value="${organization.organicUnitCount}"/> units
                        </span>
                    </div>

                    <div class="d-flex flex-column gap-12">
                        <c:forEach var="unit" items="${organicUnits}">
                            <c:set var="canModifyUnit" value="${canModifyOrganicUnitById[unit.id]}" />
                            <div class="gape-hierarchy-node border border-neutral-30 rounded-12 px-20 py-16 bg-neutral-10" style="margin-left: ${unit.hierarchyIndent}px;">
                                <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
                                    <div class="d-flex align-items-start gap-12">
                                        <span class="text-22 text-main-600 line-height-1"><i class="ph ph-tree-structure"></i></span>
                                        <div>
                                            <c:choose>
                                                <c:when test="${unit.archived or not canModifyUnit}">
                                                    <span class="fw-medium text-14 text-neutral-700">
                                                        <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}/edit" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                        <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>
                                            <span class="d-block text-12 text-neutral-500">Parent: <c:out value="${unit.parentLabel}"/></span>
                                        </div>
                                    </div>
                                    <div class="d-flex align-items-center gap-12 flex-wrap">
                                        <span class="text-13 text-neutral-500"><c:out value="${unit.typeLabel}"/></span>
                                        <span class="${unit.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${unit.stateLabel}"/>
                                        </span>
                                        <c:if test="${not unit.archived and canModifyUnit}">
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}/edit" class="text-21 text-neutral-500 hover-text-main-600" title="Edit">
                                                <i class="ph ph-pencil-simple-line"></i>
                                            </a>
                                            <button type="button" class="text-21 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteUnit${unit.id}">
                                                <i class="ph ph-trash"></i>
                                            </button>
                                        </c:if>
                                    </div>
                                </div>
                            </div>

                            <c:if test="${not unit.archived and canModifyUnit}">
                                <div class="modal fade" id="deleteUnit${unit.id}" tabindex="-1" aria-hidden="true">
                                    <div class="modal-dialog modal-dialog-centered">
                                        <div class="modal-content rounded-12 border-0">
                                            <div class="modal-header border-neutral-30">
                                                <h5 class="modal-title text-18 fw-semibold">Delete Organic Unit</h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${unit.name}"/></strong> if it has no dependencies.</p>
                                            </div>
                                            <div class="modal-footer border-neutral-30">
                                                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}/delete" method="post" class="m-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </c:if>
                        </c:forEach>
                        <c:if test="${empty organicUnits}">
                            <div class="border border-neutral-30 rounded-12 px-20 py-24 text-center text-14 text-neutral-500">
                                No organic units registered.
                            </div>
                        </c:if>
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
