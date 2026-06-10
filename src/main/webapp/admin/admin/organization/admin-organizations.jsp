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
    <title>GAPE - Organizations</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            text-decoration: none;
        }

        .gape-action-button:hover,
        .gape-action-button:focus-visible {
            text-decoration: none;
            transform: translateY(-1px);
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-organization-table-photo {
            border-radius: 12px;
            height: 44px;
            object-fit: cover;
            width: 44px;
        }
    </style>
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
                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Total</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${organizationCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeOrganizations}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Inactive</span>
                            <h2 class="text-32 fw-semibold text-warning-600 mb-0">${inactiveOrganizations}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Organic Units</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${unitTotal}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Organization Management</h2>
                            <span class="text-14 text-neutral-500">Organizations, institutions, hierarchy and administrator context.</span>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/organizations/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                            <i class="ph ph-plus-circle me-8"></i>New Organization
                        </a>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Organization</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Type</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Units</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="organization" items="${organizations}">
                                <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/assets/images/thumbs/student-dashbord-profile-photo-img1.png"/>
                                <c:if test="${organization.hasPhoto}">
                                    <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/media/${organization.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12">
                                            <img src="${organizationPhotoUrl}"
                                                 alt=""
                                                 class="gape-organization-table-photo flex-shrink-0"
                                                 onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/thumbs/student-dashbord-profile-photo-img1.png';">
                                            <div>
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${organization.name}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${organization.acronym}"/></span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${organization.typeLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${organization.organicUnitCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${organization.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${organization.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                <i class="ph ph-pencil-simple-line"></i>
                                            </a>
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/new" class="text-22 text-neutral-500 hover-text-main-600" title="New unit">
                                                <i class="ph ph-tree-structure"></i>
                                            </a>
                                            <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteOrganization${organization.id}">
                                                <i class="ph ph-trash"></i>
                                            </button>
                                        </div>

                                        <div class="modal fade" id="deleteOrganization${organization.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Organization</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${organization.name}"/></strong> if it has no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty organizations}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No managed organizations found.</td>
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
