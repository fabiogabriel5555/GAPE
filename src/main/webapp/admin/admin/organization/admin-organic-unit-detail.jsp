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
    <title>GAPE - Organic Unit Detail</title>
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

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div class="d-flex align-items-start gap-14">
                            <span class="text-28 text-main-600 line-height-1"><i class="ph ph-tree-structure"></i></span>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4">
                                    <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                </h2>
                                <span class="text-14 text-neutral-500">
                                    <c:out value="${organization.name}"/> | Parent: <c:out value="${unit.parentLabel}"/>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/admin/organizations" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${canModifyUnit}">
                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                        </div>
                    </div>

                    <div class="row gy-4">
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Generated Code</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${unit.code}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Acronym</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${unit.acronym}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Type</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${unit.typeLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${unit.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${unit.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <%@ include file="/WEB-INF/fragments/organic-unit-administrators.jspf" %>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
