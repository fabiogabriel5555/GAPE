<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "organizations");
    }
%>
<c:set var="organizationPhotoUrl" value=""/>
<c:if test="${organization.hasPhoto}">
    <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/media/${organization.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Organization Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            min-height: 48px;
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

        .gape-action-archive {
            background-color: #f97316 !important;
            border-color: #f97316 !important;
            color: #fff !important;
        }

        .gape-action-edit {
            background-color: var(--success-600) !important;
            border-color: var(--success-600) !important;
            color: #fff !important;
        }

        .gape-hierarchy-node {
            border-inline-start: 3px solid var(--main-600);
        }

        .gape-organization-detail-photo {
            border-radius: 16px;
            height: 72px;
            object-fit: cover;
            width: 72px;
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
                        <div class="d-flex align-items-center gap-16">
                            <c:choose>
                                <c:when test="${organization.hasPhoto}">
                                    <img src="${organizationPhotoUrl}"
                                         alt=""
                                         class="gape-organization-detail-photo flex-shrink-0"
                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--organization-detail d-none" aria-label="No organization photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--organization-detail" aria-label="No organization photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${organization.name}"/></h2>
                                <span class="text-14 text-neutral-500">
                                    <c:out value="${organization.typeLabel}"/> |
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${organization.name}'/>"><c:out value="${organization.acronym}"/></span>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/admin/organizations" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${canModifyOrganization}">
                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/edit" class="gape-action-button gape-action-edit px-20 py-10 rounded-12 fw-semibold transition-03">Edit</a>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${organization.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${organization.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Type</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${organization.typeLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Organic Units</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${organization.organicUnitCount}"/></p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row gy-4 mb-24">
                    <div class="col-xl-7">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                                <div>
                                    <h3 class="text-18 fw-medium text-neutral-700 mb-4">Organic Unit Hierarchy</h3>
                                    <span class="text-14 text-neutral-500">Departments, schools, directions and sections.</span>
                                </div>
                                <c:if test="${canCreateOrganicUnits}">
                                    <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/new" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                        <i class="ph ph-plus-circle me-8"></i>New Unit
                                    </a>
                                </c:if>
                            </div>
                            <div class="d-flex flex-column gap-12">
                                <c:forEach var="unit" items="${organicUnits}">
                                    <c:set var="canModifyUnit" value="${canModifyOrganicUnitById[unit.id]}" />
                                    <div class="gape-hierarchy-node border border-neutral-30 rounded-12 px-20 py-16 bg-neutral-10" style="margin-left: ${unit.hierarchyIndent}px;">
                                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap">
                                            <div class="d-flex align-items-start gap-12">
                                                <span class="text-22 text-main-600 line-height-1"><i class="ph ph-tree-structure"></i></span>
                                                <div>
                                                    <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                        <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                                    </a>
                                                    <span class="d-block text-12 text-neutral-500">Parent: <c:out value="${unit.parentLabel}"/></span>
                                                </div>
                                            </div>
                                            <div class="d-flex align-items-center gap-12 flex-wrap">
                                                <span class="text-13 text-neutral-500"><c:out value="${unit.typeLabel}"/></span>
                                                <span class="${unit.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                    <c:out value="${unit.stateLabel}"/>
                                                </span>
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/${unit.id}" class="text-21 text-neutral-500 hover-text-main-600" title="Detail">
                                                    <i class="ph ph-eye"></i>
                                                </a>
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
                                    <div class="border border-neutral-30 rounded-12 px-20 py-24 text-center text-14 text-neutral-500">No organic units registered.</div>
                                </c:if>
                            </div>
                        </div>
                    </div>

                    <c:if test="${canAssignOrganizationAdministrators}">
                    <div class="col-xl-5">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Administrator Assignment</h3>
                            <span class="text-14 text-neutral-500 d-block mb-20">Assign an active administrator to this organization.</span>
                            <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/assign-admin" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <div class="mb-20 gape-select-field">
                                    <label for="administratorUserId" class="fw-medium text-base text-neutral-800 mb-12">Administrator</label>
                                    <select id="administratorUserId" name="administratorUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select administrator</option>
                                        <c:forEach var="administrator" items="${administratorOptions}">
                                            <option value="${administrator.id}"><c:out value="${administrator.name}"/> - <c:out value="${administrator.email}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="row gy-4">
                                    <div class="col-md-6">
                                        <label for="startDate" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                                        <input id="startDate" name="startDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="endDate" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                                        <input id="endDate" name="endDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                </div>
                                <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 mt-24">Assign Administrator</button>
                            </form>
                            <c:if test="${empty administratorOptions}">
                                <div class="alert alert-warning rounded-12 border-0 mt-20 mb-0" role="alert">No active administrators are available for assignment.</div>
                            </c:if>
                            <div class="border-top-dashed pt-24 mt-24">
                                <h4 class="text-16 fw-medium text-neutral-700 mb-16">Current Administrators</h4>
                                <div class="d-flex flex-column gap-12">
                                    <c:forEach var="assignment" items="${assignedAdministrators}">
                                        <div class="border border-neutral-30 rounded-12 px-16 py-14">
                                            <div class="d-flex align-items-start justify-content-between gap-12 flex-wrap">
                                                <div>
                                                    <span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${assignment.name}"/></span>
                                                    <span class="text-12 text-neutral-500"><c:out value="${assignment.email}"/></span>
                                                </div>
                                                <span class="${assignment.assignmentBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12">
                                                    <c:out value="${assignment.assignmentStateLabel}"/>
                                                </span>
                                            </div>
                                            <div class="row gy-2 mt-12 text-12 text-neutral-500">
                                                <div class="col-6">User: <c:out value="${assignment.userStateLabel}"/></div>
                                                <div class="col-6">Active now: <c:out value="${assignment.currentlyActive ? 'Yes' : 'No'}"/></div>
                                                <div class="col-6">Start: <c:out value="${assignment.startDateLabel}"/></div>
                                                <div class="col-6">End: <c:out value="${assignment.endDateLabel}"/></div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${empty assignedAdministrators}">
                                        <div class="border border-neutral-30 rounded-12 px-16 py-18 text-14 text-neutral-500">No administrators assigned.</div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>
                    </c:if>
                </div>

                <c:if test="${canModifyOrganization}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveOrganization">Deactivate</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteOrganization">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${canModifyOrganization}">
<div class="modal fade" id="archiveOrganization" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Deactivate Organization</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${organization.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Deactivate</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteOrganization" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Organization</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the record if the database allows it.</p>
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
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
