<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "users");
    }
%>
<c:set var="userHasPhoto" value="${not empty user.photo}"/>
<c:set var="detailPhotoUrl" value=""/>
<c:if test="${userHasPhoto}">
    <c:set var="detailPhotoUrl" value="${pageContext.request.contextPath}/media/${user.photo}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - User Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-user-detail-photo {
            border: 1px solid var(--neutral-30);
            border-radius: 50%;
            height: 88px;
            object-fit: cover;
            width: 88px;
        }

        .gape-critical-action {
            min-height: 48px;
        }

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

        .gape-action-button:active {
            filter: brightness(.92);
            transform: translateY(0);
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-action-delete:hover,
        .gape-action-delete:focus-visible {
            background-color: #b91c1c !important;
            border-color: #b91c1c !important;
            color: #fff !important;
        }

        .gape-action-delete:active {
            background-color: #991b1b !important;
            border-color: #991b1b !important;
        }

        .gape-action-block {
            background-color: #f97316 !important;
            border-color: #f97316 !important;
            color: #fff !important;
        }

        .gape-action-block:hover,
        .gape-action-block:focus-visible {
            background-color: #ea580c !important;
            border-color: #ea580c !important;
            color: #fff !important;
        }

        .gape-action-block:active {
            background-color: #c2410c !important;
            border-color: #c2410c !important;
        }

        .gape-action-inactivate {
            background-color: #facc15 !important;
            border-color: #facc15 !important;
            color: #1f2937 !important;
        }

        .gape-action-inactivate:hover,
        .gape-action-inactivate:focus-visible {
            background-color: #eab308 !important;
            border-color: #eab308 !important;
            color: #1f2937 !important;
        }

        .gape-action-inactivate:active {
            background-color: #ca8a04 !important;
            border-color: #ca8a04 !important;
        }

        .gape-action-activate,
        .gape-action-edit {
            background-color: var(--success-600) !important;
            border-color: var(--success-600) !important;
            color: #fff !important;
        }

        .gape-action-activate:hover,
        .gape-action-activate:focus-visible,
        .gape-action-edit:hover,
        .gape-action-edit:focus-visible {
            background-color: #15803d !important;
            border-color: #15803d !important;
            color: #fff !important;
        }

        .gape-action-activate:active,
        .gape-action-edit:active {
            background-color: #166534 !important;
            border-color: #166534 !important;
        }

        .gape-action-audit {
            background-color: var(--main-600) !important;
            border-color: var(--main-600) !important;
            color: #fff !important;
        }

        .gape-action-audit:hover,
        .gape-action-audit:focus-visible {
            background-color: var(--main-700) !important;
            border-color: var(--main-700) !important;
            color: #fff !important;
        }

        .gape-action-audit:active {
            background-color: var(--main-800) !important;
            border-color: var(--main-800) !important;
        }

        .gape-user-assignment-card {
            min-height: 96px;
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
                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${user.name}"/></h2>
                            <span class="text-14 text-neutral-500"><c:out value="${user.email}"/></span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/admin/users" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${user.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${user.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Profiles</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${user.profileSummary}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Document</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${user.documentLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Created At</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${user.createdAt}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Language</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${user.language}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Photo</span>
                                <div class="mt-8">
                                    <c:choose>
                                        <c:when test="${userHasPhoto}">
                                            <img src="${detailPhotoUrl}"
                                                 alt="Profile photo"
                                                 class="gape-user-detail-photo"
                                                 onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                            <span class="gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--user-detail d-none" aria-label="No profile photo">
                                                <i class="ph ph-user-circle"></i>
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="gape-photo-placeholder gape-photo-placeholder--user gape-photo-placeholder--user-detail" aria-label="No profile photo">
                                                <i class="ph ph-user-circle"></i>
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="border-bottom-dashed pb-20 mb-20">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-4">Access and Permissions</h3>
                        <span class="text-14 text-neutral-500">Profiles, administrator permissions and profile contexts assigned to this user.</span>
                    </div>

                    <c:if test="${not empty accessProfileDetails}">
                        <h4 class="text-16 fw-medium text-neutral-700 mb-12">Access Profiles</h4>
                        <div class="row gy-4 mb-24">
                            <c:forEach var="item" items="${accessProfileDetails}">
                                <div class="col-lg-3 col-md-6">
                                    <div class="gape-user-assignment-card border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${item.label}"/></span>
                                        <span class="d-block text-12 text-neutral-500 mt-6">
                                            <c:choose>
                                                <c:when test="${item.hasDetailHtml}"><c:out value="${item.detailHtml}" escapeXml="false"/></c:when>
                                                <c:otherwise><c:out value="${item.detail}"/></c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:if>

                    <c:if test="${not empty administratorPermissionDetails}">
                        <h4 class="text-16 fw-medium text-neutral-700 mb-12">Administrator Permissions</h4>
                        <div class="row gy-4 mb-24">
                            <c:forEach var="item" items="${administratorPermissionDetails}">
                                <div class="col-lg-3 col-md-6">
                                    <div class="gape-user-assignment-card border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${item.label}"/></span>
                                        <span class="d-block text-12 text-neutral-500 mt-6">
                                            <c:choose>
                                                <c:when test="${item.hasDetailHtml}"><c:out value="${item.detailHtml}" escapeXml="false"/></c:when>
                                                <c:otherwise><c:out value="${item.detail}"/></c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:if>

                    <c:if test="${not empty profileContextDetails}">
                        <h4 class="text-16 fw-medium text-neutral-700 mb-12">Profile Contexts</h4>
                        <div class="row gy-4">
                            <c:forEach var="item" items="${profileContextDetails}">
                                <div class="col-lg-4 col-md-6">
                                    <div class="gape-user-assignment-card border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${item.label}"/></span>
                                        <span class="d-block text-12 text-neutral-500 mt-6">
                                            <c:choose>
                                                <c:when test="${item.hasDetailHtml}"><c:out value="${item.detailHtml}" escapeXml="false"/></c:when>
                                                <c:otherwise><c:out value="${item.detail}"/></c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:if>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                    <div class="d-flex align-items-center gap-16 flex-wrap">
                        <button type="button" class="gape-action-button gape-critical-action gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteUser">Delete</button>
                        <c:choose>
                            <c:when test="${user.blocked}">
                                <button type="button" class="gape-action-button gape-critical-action gape-action-activate px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#activateUser">Activate</button>
                                <button type="button" class="gape-action-button gape-critical-action gape-action-inactivate px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#inactivateUser">Inactivate</button>
                            </c:when>
                            <c:when test="${user.inactive}">
                                <button type="button" class="gape-action-button gape-critical-action gape-action-block px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#blockUser">Block</button>
                                <button type="button" class="gape-action-button gape-critical-action gape-action-activate px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#activateUser">Activate</button>
                            </c:when>
                            <c:otherwise>
                                <button type="button" class="gape-action-button gape-critical-action gape-action-block px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#blockUser">Block</button>
                                <button type="button" class="gape-action-button gape-critical-action gape-action-inactivate px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#inactivateUser">Inactivate</button>
                            </c:otherwise>
                        </c:choose>
                        <a href="${pageContext.request.contextPath}/admin/users/${user.id}/edit" class="gape-action-button gape-critical-action gape-action-edit px-24 py-12 rounded-12 fw-semibold transition-03">Edit</a>
                        <a href="${pageContext.request.contextPath}/admin/activity-log?userId=${user.id}" class="gape-action-button gape-critical-action gape-action-audit px-24 py-12 rounded-12 fw-semibold transition-03">View Audit</a>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<div class="modal fade" id="blockUser" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Block User</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm blocking <strong><c:out value="${user.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/block" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" name="returnTo" value="/admin/users/${user.id}">
                    <button type="submit" class="gape-action-button gape-action-block px-20 py-10 rounded-12 fw-semibold transition-03">Block</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="activateUser" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Activate User</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm activating <strong><c:out value="${user.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/activate" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" name="returnTo" value="/admin/users/${user.id}">
                    <button type="submit" class="gape-action-button gape-action-activate px-20 py-10 rounded-12 fw-semibold transition-03">Activate</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="inactivateUser" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Inactivate User</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm inactivating <strong><c:out value="${user.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/inactivate" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" name="returnTo" value="/admin/users/${user.id}">
                    <button type="submit" class="gape-action-button gape-action-inactivate px-20 py-10 rounded-12 fw-semibold transition-03">Inactivate</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteUser" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete User</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the record if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/users/${user.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
