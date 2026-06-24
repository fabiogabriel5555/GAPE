<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "subjects");
    }
%>
<c:if test="${empty subjectBasePath}">
    <c:set var="subjectBasePath" value="/admin/subjects"/>
</c:if>
<c:if test="${empty subjectCourseBasePath}">
    <c:set var="subjectCourseBasePath" value="/admin/courses"/>
</c:if>
<c:set var="subjectPhotoUrl" value=""/>
<c:if test="${subject.hasPhoto}">
    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Subject Detail</title>
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

        .gape-learning-detail-photo {
            border-radius: 16px;
            height: 72px;
            object-fit: cover;
            width: 96px;
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
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div class="d-flex align-items-center gap-16">
                            <c:choose>
                                <c:when test="${subject.hasPhoto}">
                                    <img src="${subjectPhotoUrl}"
                                         alt=""
                                         class="gape-learning-detail-photo flex-shrink-0"
                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail d-none" aria-label="No subject photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail" aria-label="No subject photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${subject.name}"/></h2>
                                <span class="text-14 text-neutral-500">
                                    <c:out value="${subject.organizationContextHtml}" escapeXml="false"/> |
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}${subjectBasePath}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${not subject.archived and canModifySubject}">
                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Edit</a>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${subject.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">ECTS</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${subject.ectsLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Workload</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${subject.workloadHoursLabel}"/></p>
                            </div>
                        </div>
                    </div>
                    <p class="text-14 text-neutral-600 mt-20 mb-0"><c:out value="${subject.description}"/></p>
                </div>

                <div class="row gy-4 mb-24">
                    <c:if test="${canAssignSubjectCoordinators}">
                    <div class="col-xl-6">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                                <div>
                                    <h3 class="text-18 fw-medium text-neutral-700 mb-4">Coordinator Assignment</h3>
                                    <span class="text-14 text-neutral-500">Current coordinators and assignment controls for this subject.</span>
                                </div>
                                <span class="bg-main-50 text-main-600 px-14 py-6 rounded-pill text-13 fw-semibold">
                                    ${fn:length(coordinatorAssignments)} assigned
                                </span>
                            </div>
                            <div class="d-flex flex-column gap-12 mb-24">
                                <c:forEach var="assignment" items="${coordinatorAssignments}">
                                    <div class="border border-neutral-30 rounded-12 px-16 py-14">
                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                            <div>
                                                <span class="fw-medium text-14 text-neutral-700"><c:out value="${assignment.coordinatorName}"/></span>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${assignment.coordinatorEmail}"/></span>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${assignment.startDate}"/> to <c:out value="${assignment.endDate}"/></span>
                                            </div>
                                            <div class="d-flex align-items-center gap-10 flex-wrap">
                                                <span class="${assignment.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                    <c:out value="${assignment.stateLabel}"/>
                                                </span>
                                                <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Edit assignment" data-bs-toggle="modal" data-bs-target="#editCoordinatorAssignment${assignment.coordinatorUserId}">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="modal fade" id="editCoordinatorAssignment${assignment.coordinatorUserId}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog modal-dialog-centered">
                                            <div class="modal-content rounded-12 border-0 text-start">
                                                <div class="modal-header border-neutral-30">
                                                    <h5 class="modal-title text-18 fw-semibold">Edit Coordinator Assignment</h5>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                </div>
                                                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/coordinators/${assignment.coordinatorUserId}/update" method="post">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-20">
                                                            <strong><c:out value="${assignment.coordinatorName}"/></strong>
                                                            <span class="d-block text-12 text-neutral-500"><c:out value="${assignment.coordinatorEmail}"/></span>
                                                        </p>
                                                        <div class="row gy-3">
                                                            <div class="col-md-4 gape-select-field">
                                                                <label for="coordinatorAssignmentState${assignment.coordinatorUserId}" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                                                                <select id="coordinatorAssignmentState${assignment.coordinatorUserId}" name="state" required class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                                                    <option value="active" ${assignment.stateValue == 'active' ? 'selected' : ''}>Active</option>
                                                                    <option value="inactive" ${assignment.stateValue == 'inactive' ? 'selected' : ''}>Inactive</option>
                                                                    <option value="archived" ${assignment.stateValue == 'archived' ? 'selected' : ''}>Archived</option>
                                                                </select>
                                                            </div>
                                                            <div class="col-md-4">
                                                                <label for="coordinatorAssignmentStart${assignment.coordinatorUserId}" class="fw-medium text-base text-neutral-800 mb-12">Start</label>
                                                                <input id="coordinatorAssignmentStart${assignment.coordinatorUserId}" name="startDate" type="date" value="<c:out value='${assignment.startDateValue}'/>" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                                                            </div>
                                                            <div class="col-md-4">
                                                                <label for="coordinatorAssignmentEnd${assignment.coordinatorUserId}" class="fw-medium text-base text-neutral-800 mb-12">End</label>
                                                                <input id="coordinatorAssignmentEnd${assignment.coordinatorUserId}" name="endDate" type="date" value="<c:out value='${assignment.endDateValue}'/>" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <button type="submit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 border-0">Save</button>
                                                    </div>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty coordinatorAssignments}">
                                    <div class="border border-neutral-30 rounded-12 px-16 py-14 text-14 text-neutral-500">No coordinator assigned to this subject yet.</div>
                                </c:if>
                            </div>
                            <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/assign-coordinator" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <div class="mb-20 gape-select-field">
                                    <label for="coordinatorUserId" class="fw-medium text-base text-neutral-800 mb-12">Coordinator</label>
                                    <select id="coordinatorUserId" name="coordinatorUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select coordinator</option>
                                        <c:forEach var="coordinator" items="${coordinatorOptions}">
                                            <option value="${coordinator.id}"><c:out value="${coordinator.name}"/> - <c:out value="${coordinator.email}"/></option>
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
                                <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 mt-24">Assign New Coordinator</button>
                            </form>
                        </div>
                    </div>
                    </c:if>
                    <div class="col-xl-6">
                        <%@ include file="/WEB-INF/fragments/subject-course-associations-panel.jspf" %>
                    </div>
                </div>

                <%@ include file="/WEB-INF/fragments/subject-enrollment-management.jspf" %>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Subject Management</h3>
                            <span class="text-14 text-neutral-500">Subject structure with associated courses and class groups.</span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/learning/class-groups?subjectId=${subject.id}" class="border-main-600 border px-16 py-8 fw-semibold rounded-12 hover-bg-main-50 transition-03">Open Groups</a>
                            <c:if test="${not subject.archived and canCreateClassGroupsForSubject}">
                                <a href="${pageContext.request.contextPath}/learning/class-groups/new?subjectId=${subject.id}" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Group
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="gape-structure-panel">
                        <div class="gape-subject-node border border-neutral-30 rounded-8 px-16 py-14 bg-white">
                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                <div class="d-flex align-items-start gap-10">
                                    <span class="text-20 text-success-600 line-height-1"><i class="ph ph-book-open-text"></i></span>
                                    <div>
                                        <span class="fw-medium text-14 text-neutral-700">
                                            <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                            <span class="ms-4"><c:out value="${subject.name}"/></span>
                                        </span>
                                        <span class="gape-node-meta text-12">
                                            <span>${fn:length(subjectCourseAssociations)} courses</span>
                                            <span>${fn:length(classGroups)} class groups</span>
                                        </span>
                                    </div>
                                </div>
                                <div class="d-flex align-items-center gap-10 flex-wrap">
                                    <span class="${subject.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                        <c:out value="${subject.stateLabel}"/>
                                    </span>
                                    <button type="button"
                                            class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                            title="Show class groups"
                                            aria-label="Show class groups"
                                            aria-expanded="false"
                                            aria-controls="subjectDetailClassGroups${subject.id}"
                                            data-gape-tree-toggle="subjectDetailClassGroups${subject.id}"
                                            data-gape-open-title="Hide class groups"
                                            data-gape-closed-title="Show class groups">
                                        <i class="ph ph-caret-down"></i>
                                    </button>
                                </div>
                            </div>

                            <div id="subjectDetailClassGroups${subject.id}" class="gape-structure-panel d-none">
                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                    <div>
                                        <h3 class="text-16 fw-medium text-neutral-700 mb-4">Class Groups</h3>
                                        <span class="text-13 text-neutral-500">
                                            <c:out value="${subject.name}"/> &middot; ${fn:length(classGroups)} class groups
                                        </span>
                                    </div>
                                </div>
                                <div class="d-flex flex-column gap-10">
                                    <c:forEach var="classGroup" items="${classGroups}">
                                        <c:set var="canModifyClassGroup" value="${canModifyClassGroupById[classGroup.id]}" />
                                        <c:set var="canManageClassGroupStructure" value="${canManageClassGroupStructureById[classGroup.id]}" />
                                        <c:set var="activityPanelId" value="subjectDetailClassGroupActivities${subject.id}_${classGroup.id}" />
                                        <div class="gape-class-group-node border border-neutral-30 rounded-8 px-16 py-12 bg-white">
                                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                <div class="d-flex align-items-start gap-10">
                                                    <span class="text-20 text-warning-600 line-height-1"><i class="ph ph-users-three"></i></span>
                                                    <div>
                                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                            <c:out value="${classGroup.code}"/>
                                                        </a>
                                                        <span class="gape-node-meta text-12">
                                                            <span title="<c:out value='${classGroup.course.subjectManagementContextTitle}'/>">
                                                                <c:out value="${classGroup.course.subjectManagementContextHtml}" escapeXml="false"/>
                                                            </span>
                                                            <span><c:out value="${classGroup.modalityLabel}"/></span>
                                                            <span><c:out value="${classGroup.shift}"/></span>
                                                            <span><c:out value="${classGroup.dateRangeLabel}"/></span>
                                                            <span><c:out value="${classGroup.activeEnrollmentCountLabel}"/></span>
                                                            <span>${classGroup.blockCount} blocks</span>
                                                        </span>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-10 flex-wrap">
                                                    <span class="${classGroup.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                        <c:out value="${classGroup.stateLabel}"/>
                                                    </span>
                                                    <button type="button"
                                                            class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                            title="Show Activities"
                                                            aria-label="Show Activities"
                                                            aria-expanded="false"
                                                            aria-controls="${activityPanelId}"
                                                            data-gape-tree-toggle="${activityPanelId}"
                                                            data-gape-open-title="Hide Activities"
                                                            data-gape-closed-title="Show Activities">
                                                        <i class="ph ph-caret-down"></i>
                                                    </button>
                                                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail">
                                                        <i class="ph ph-eye"></i>
                                                    </a>
                                                    <c:if test="${not classGroup.archived and canModifyClassGroup}">
                                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit">
                                                            <i class="ph ph-pencil-simple-line"></i>
                                                        </a>
                                                    </c:if>
                                                    <c:if test="${not classGroup.archived and canManageClassGroupStructure}">
                                                        <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubjectDetailClassGroup${subject.id}_${classGroup.id}">
                                                            <i class="ph ph-trash"></i>
                                                        </button>
                                                    </c:if>
                                                </div>
                                            </div>
                                            <c:if test="${not classGroup.archived and canManageClassGroupStructure}">
                                            <div class="modal fade" id="deleteSubjectDetailClassGroup${subject.id}_${classGroup.id}" tabindex="-1" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content rounded-12 border-0">
                                                        <div class="modal-header border-neutral-30">
                                                            <h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <div class="modal-body">
                                                            <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${classGroup.code}"/></strong> if it has no dependencies.</p>
                                                        </div>
                                                        <div class="modal-footer border-neutral-30">
                                                            <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                            <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/delete" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                            </form>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            </c:if>
                                            <div id="${activityPanelId}" class="d-none">
                                                <%@ include file="/WEB-INF/fragments/class-group-activities-panel.jspf" %>
                                            </div>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${empty classGroups}">
                                        <div class="border border-neutral-30 rounded-8 px-16 py-14 bg-white text-14 text-neutral-500">No class groups created for this subject.</div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <c:if test="${not subject.archived and canModifySubject}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveSubject">Archive</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteSubject">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${not subject.archived and canModifySubject}">
<div class="modal fade" id="archiveSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Archive Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${subject.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the subject if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    document.querySelectorAll('[data-gape-tree-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            var target = document.getElementById(button.dataset.gapeTreeToggle);
            if (!target) {
                return;
            }
            var isHidden = target.classList.toggle('d-none');
            var isExpanded = !isHidden;
            var icon = button.querySelector('i');
            button.setAttribute('aria-expanded', String(isExpanded));
            button.setAttribute('title', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            button.setAttribute('aria-label', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            if (icon) {
                icon.classList.toggle('ph-caret-down', !isExpanded);
                icon.classList.toggle('ph-caret-up', isExpanded);
            }
        });
    });
</script>
</body>
</html>
