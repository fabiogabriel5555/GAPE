<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "users");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Users</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-state-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            min-height: 44px;
            text-decoration: none;
        }

        .gape-state-action-button:hover,
        .gape-state-action-button:focus-visible {
            text-decoration: none;
            transform: translateY(-1px);
        }

        .gape-state-action-button:active {
            filter: brightness(.92);
            transform: translateY(0);
        }

        .gape-state-action-activate {
            background-color: var(--success-600) !important;
            border-color: var(--success-600) !important;
            color: #fff !important;
        }

        .gape-state-action-activate:hover,
        .gape-state-action-activate:focus-visible {
            background-color: #15803d !important;
            border-color: #15803d !important;
            color: #fff !important;
        }

        .gape-state-action-activate:active {
            background-color: #166534 !important;
            border-color: #166534 !important;
        }

        .gape-state-action-inactivate {
            background-color: #facc15 !important;
            border-color: #facc15 !important;
            color: #1f2937 !important;
        }

        .gape-state-action-inactivate:hover,
        .gape-state-action-inactivate:focus-visible {
            background-color: #eab308 !important;
            border-color: #eab308 !important;
            color: #1f2937 !important;
        }

        .gape-state-action-inactivate:active {
            background-color: #ca8a04 !important;
            border-color: #ca8a04 !important;
        }

        .gape-state-action-block {
            background-color: #f97316 !important;
            border-color: #f97316 !important;
            color: #fff !important;
        }

        .gape-state-action-block:hover,
        .gape-state-action-block:focus-visible {
            background-color: #ea580c !important;
            border-color: #ea580c !important;
            color: #fff !important;
        }

        .gape-state-action-block:active {
            background-color: #c2410c !important;
            border-color: #c2410c !important;
        }

        .gape-user-table-photo {
            border-radius: 50%;
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
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${userCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeUsers}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Inactive</span>
                            <h2 class="text-32 fw-semibold text-warning-600 mb-0">${inactiveUsers}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Blocked</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${blockedUsers}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24" data-gape-sort-root data-gape-group-item-label="user">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">User Management</h2>
                    <span class="text-14 text-neutral-500">Personal data, profiles, and access state.</span>
                        </div>
                        <div class="gape-management-actions d-flex align-items-center gap-12 flex-wrap">
                            <div class="dropdown">
                                <button type="button"
                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="outside"
                                        data-gape-sort-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-sort-ascending"></i>Sort by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                            <span>Name</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-type="date" data-sort-state="none" aria-pressed="false">
                                            <span>Date</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                            <span>State</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <a href="${pageContext.request.contextPath}/admin/users/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-user-plus me-8"></i>New User
                            </a>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">User</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Profiles</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Document</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-gape-sort-list>
                            <c:forEach var="user" items="${users}" varStatus="userLoop">
                                <c:set var="userPhotoUrl" value=""/>
                                <c:if test="${user.hasPhoto}">
                                    <c:set var="userPhotoUrl" value="${pageContext.request.contextPath}/media/${user.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-gape-sort-row
                                    data-sort-index="${userLoop.index}"
                                    data-sort-name="${fn:escapeXml(user.name)}"
                                    data-sort-profile="${fn:escapeXml(user.profileSummary)}"
                                    data-sort-status="${fn:escapeXml(user.stateLabel)}"
                                    data-sort-date="${fn:escapeXml(user.createdAtSort)}"
                                    data-sort-created="${fn:escapeXml(user.createdAtSort)}">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${user.hasPhoto}">
                                                    <img src="${userPhotoUrl}"
                                                         alt=""
                                                         class="gape-user-table-photo flex-shrink-0"
                                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--table d-none" aria-label="No profile photo">
                                                        <i class="ph ph-user-circle"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--table" aria-label="No profile photo">
                                                        <i class="ph ph-user-circle"></i>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/admin/users/${user.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${user.name}"/>
                                                </a>
                                                <c:choose>
                                                    <c:when test="${sessionScope['gape.auth.canViewPersonalData']}">
                                                        <span class="d-block text-12 text-neutral-500"><c:out value="${user.email}"/></span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="d-block text-12 text-neutral-500">Personal data restricted</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${user.profileSummary}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:choose>
                                            <c:when test="${sessionScope['gape.auth.canViewPersonalData']}">
                                                <c:out value="${user.documentLabel}"/>
                                            </c:when>
                                            <c:otherwise>Restricted</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="${user.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${user.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/admin/users/${user.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <a href="${pageContext.request.contextPath}/admin/users/${user.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                <i class="ph ph-pencil-simple-line"></i>
                                            </a>
                                            <button type="button" class="text-22 text-neutral-500 hover-text-main-600" title="State Actions" data-bs-toggle="modal" data-bs-target="#stateUser${user.id}">
                                                <i class="ph ph-lock"></i>
                                            </button>
                                            <c:if test="${sessionScope['gape.auth.canProcessDeletionRequests']}">
                                                <a href="${pageContext.request.contextPath}/admin/deletion-requests?userId=${user.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Deletion">
                                                    <i class="ph ph-archive-box"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${sessionScope['gape.auth.canViewReports']}">
                                                <a href="${pageContext.request.contextPath}/admin/activity-log?userId=${user.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Audit">
                                                    <i class="ph ph-list-checks"></i>
                                                </a>
                                            </c:if>
                                             <button type="button" class="text-22 text-neutral-500 hover-text-main-600" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteUser${user.id}">
                                                <i class="ph ph-trash"></i>
                                            </button>
                                        </div>

                                        <div class="modal fade" id="stateUser${user.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">State Actions</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">Choose how to update <strong><c:out value="${user.name}"/></strong>.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30 gap-8">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <c:choose>
                                                            <c:when test="${user.blocked}">
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/activate" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-activate px-20 py-10 rounded-12 fw-semibold transition-03">Activate</button>
                                                                </form>
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/inactivate" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-inactivate px-20 py-10 rounded-12 fw-semibold transition-03">Inactivate</button>
                                                                </form>
                                                            </c:when>
                                                            <c:when test="${user.inactive}">
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/activate" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-activate px-20 py-10 rounded-12 fw-semibold transition-03">Activate</button>
                                                                </form>
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/block" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-block px-20 py-10 rounded-12 fw-semibold transition-03">Block</button>
                                                                </form>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/inactivate" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-inactivate px-20 py-10 rounded-12 fw-semibold transition-03">Inactivate</button>
                                                                </form>
                                                                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/block" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="gape-state-action-button gape-state-action-block px-20 py-10 rounded-12 fw-semibold transition-03">Block</button>
                                                                </form>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="modal fade" id="deleteUser${user.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete User</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${user.name}"/></strong> if there are no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}/admin/users/${user.id}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="bg-danger-600 px-20 py-10 rounded-12 fw-semibold text-white transition-03">Delete</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty users}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No users registered.</td>
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
