<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "courses");
    }
%>
<c:set var="coursePhotoUrl" value=""/>
<c:if test="${course.hasPhoto}">
    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Course Detail</title>
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

        .gape-learning-table-photo {
            border-radius: 12px;
            height: 44px;
            object-fit: cover;
            width: 44px;
        }

        .gape-tree-toggle {
            align-items: center;
            background: transparent;
            border: 0;
            border-radius: 8px;
            display: inline-flex;
            height: 32px;
            justify-content: center;
            padding: 0;
            width: 32px;
        }

        .gape-tree-toggle:hover,
        .gape-tree-toggle:focus-visible {
            background-color: var(--main-50);
            text-decoration: none;
        }

        .gape-structure-panel {
            background-color: #f8fbff;
            border: 1px solid #d9e2ef;
            border-radius: 8px;
            margin-block: 14px;
            padding: 14px;
        }

        .gape-class-activities-panel {
            margin-inline-start: 28px;
            position: relative;
        }

        .gape-class-activities-panel::before {
            background-color: #d9e2ef;
            bottom: 12px;
            content: "";
            left: -16px;
            position: absolute;
            top: 12px;
            width: 2px;
        }

        .gape-subject-node {
            border-inline-start: 3px solid #16a34a;
        }

        .gape-class-group-node {
            border-inline-start: 3px solid #7c3aed;
        }

        .gape-lesson-node {
            border-inline-start: 3px solid #2563eb;
        }

        .gape-room-node {
            border-inline-start: 3px solid #16a34a;
        }

        .gape-node-meta {
            color: #64748b;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        @media (max-width: 575.98px) {
            .gape-class-activities-panel {
                margin-inline-start: 0;
            }

            .gape-class-activities-panel::before {
                display: none;
            }
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

                <%@ include file="/WEB-INF/fragments/course-enrollment-management.jspf" %>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div class="d-flex align-items-center gap-16">
                            <c:choose>
                                <c:when test="${course.hasPhoto}">
                                    <img src="${coursePhotoUrl}"
                                         alt=""
                                         class="gape-learning-detail-photo flex-shrink-0"
                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail d-none" aria-label="No course photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail" aria-label="No course photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${course.name}"/></h2>
                                <span class="text-14 text-neutral-500" title="<c:out value='${course.courseManagementContextTitle}'/>"><c:out value="${course.courseManagementContextHtml}" escapeXml="false"/></span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}${courseBasePath}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${not course.archived and canModifyCourse}">
                                <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Edit</a>
                            </c:if>
                            <c:if test="${course.archived and canModifyCourse}">
                                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/unarchive" method="post" class="m-0">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 border-0">Unarchive</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Type</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.typeLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${course.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${course.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">ECTS</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.ectsLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Duration</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${course.durationLabel}"/></p>
                            </div>
                        </div>
                    </div>
                    <p class="text-14 text-neutral-600 mt-20 mb-0"><c:out value="${course.description}"/></p>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Subjects</h3>
                            <span class="text-14 text-neutral-500">Subjects and class group structure for this course.</span>
                        </div>
                        <c:if test="${not course.archived and canManageCourseChildren}">
                            <a href="${pageContext.request.contextPath}${subjectBasePath}/new?courseId=${course.id}" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>Create Subject
                            </a>
                        </c:if>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subject</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">ECTS</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="association" items="${courseSubjects}">
                                <c:set var="subjectClassGroups" value="${classGroupsBySubject[association.subjectId]}"/>
                                <c:set var="canModifySubject" value="${canModifySubjectById[association.subjectId]}"/>
                                <c:set var="canManageSubjectAssociations" value="${canManageSubjectAssociationsById[association.subjectId]}"/>
                                <c:set var="subjectPhotoUrl" value=""/>
                                <c:if test="${association.subject.hasPhoto}">
                                    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${association.subject.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${association.subject.hasPhoto}">
                                                    <img src="${subjectPhotoUrl}"
                                                         alt=""
                                                         class="gape-learning-table-photo flex-shrink-0"
                                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No subject photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No subject photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                            <div>
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${association.subjectName}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${association.subjectName}'/>"><c:out value="${association.subjectAcronym}"/></span>
                                                </span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-8 flex-wrap">
                                            <span class="px-12 py-6 border-neutral-30 border rounded-8 text-12 text-neutral-600" title="<c:out value='${course.courseManagementContextTitle}'/>">
                                                <c:out value="${course.courseManagementContextHtml}" escapeXml="false"/>
                                            </span>
                                            <span class="px-12 py-6 border-neutral-30 border rounded-8 text-12 text-neutral-600">
                                                <c:out value="${association.curricularPositionLabel}"/>
                                            </span>
                                            <span class="px-12 py-6 border-neutral-30 border rounded-8 text-12 text-neutral-600">
                                                <c:out value="${association.mandatoryLabel}"/>
                                            </span>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${association.subjectEctsLabel}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${association.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${association.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show class groups"
                                                    aria-label="Show class groups"
                                                    aria-expanded="false"
                                                    aria-controls="courseDetailSubjectGroups${association.subjectId}"
                                                    data-gape-tree-toggle="courseDetailSubjectGroups${association.subjectId}"
                                                    data-gape-open-title="Hide class groups"
                                                    data-gape-closed-title="Show class groups">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${not association.subject.archived and canModifySubject}">
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${not association.archived and canManageSubjectAssociations}">
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/edit#course-associations" class="text-22 text-neutral-500 hover-text-main-600" title="Course Associations">
                                                    <i class="ph ph-link-simple"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${not association.subject.archived and canModifySubject}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourseDetailSubject${course.id}_${association.subjectId}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <c:if test="${canModifySubject}">
                                        <div class="modal fade" id="deleteCourseDetailSubject${course.id}_${association.subjectId}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${association.subjectName}"/></strong> if it has no dependencies.</p>
                                                    </div>
                                                    <div class="modal-footer border-neutral-30">
                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                        <form action="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        </c:if>
                                    </td>
                                </tr>
                                <tr id="courseDetailSubjectGroups${association.subjectId}" class="d-none">
                                    <td colspan="5" class="py-0 px-20 bg-neutral-20">
                                        <div class="gape-subject-node border border-neutral-30 rounded-8 px-16 py-14 bg-white my-14">
                                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                <div class="d-flex align-items-start gap-10">
                                                    <span class="text-20 text-success-600 line-height-1"><i class="ph ph-book-open-text"></i></span>
                                                    <div>
                                                        <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                            <span class="gape-acronym-token" tabindex="0" title="<c:out value='${association.subjectName}'/>"><c:out value="${association.subjectAcronym}"/></span>
                                                            <span class="ms-4"><c:out value="${association.subjectName}"/></span>
                                                        </a>
                                                        <span class="gape-node-meta text-12">
                                                            <span><c:out value="${association.curricularPositionLabel}"/></span>
                                                            <span><c:out value="${association.mandatoryLabel}"/></span>
                                                            <span><c:out value="${fn:length(subjectClassGroups)}"/> class groups</span>
                                                        </span>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-10 flex-wrap">
                                                    <span class="${association.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                        <c:out value="${association.stateLabel}"/>
                                                    </span>
                                                    <c:if test="${not course.archived and canManageCourseChildren}">
                                                        <a href="${pageContext.request.contextPath}/learning/class-groups/new?courseId=${course.id}&subjectId=${association.subjectId}" class="text-20 text-neutral-500 hover-text-main-600" title="New Group">
                                                            <i class="ph ph-plus-circle"></i>
                                                        </a>
                                                    </c:if>
                                                    <c:if test="${not association.subject.archived and canModifySubject}">
                                                        <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit">
                                                            <i class="ph ph-pencil-simple-line"></i>
                                                        </a>
                                                    </c:if>
                                                    <c:if test="${not association.archived and canManageSubjectAssociations}">
                                                        <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/edit#course-associations" class="text-20 text-neutral-500 hover-text-main-600" title="Course Associations">
                                                            <i class="ph ph-link-simple"></i>
                                                        </a>
                                                    </c:if>
                                                </div>
                                            </div>

                                            <div class="gape-structure-panel">
                                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                <div>
                                                    <h3 class="text-16 fw-medium text-neutral-700 mb-4">Class Groups</h3>
                                                    <span class="text-13 text-neutral-500">
                                                        <c:out value="${association.subjectName}"/> &middot; ${fn:length(subjectClassGroups)} class groups
                                                    </span>
                                                </div>
                                            </div>
                                            <div class="d-flex flex-column gap-10">
                                                <c:forEach var="classGroup" items="${subjectClassGroups}">
                                                    <c:set var="canModifyClassGroup" value="${canModifyClassGroupById[classGroup.id]}" />
                                                    <c:set var="canManageClassGroupStructure" value="${canManageClassGroupStructureById[classGroup.id]}" />
                                                    <c:set var="activityPanelId" value="courseDetailClassGroupActivities${association.subjectId}_${classGroup.id}" />
                                                    <div class="gape-class-group-node border border-neutral-30 rounded-8 px-16 py-12 bg-white">
                                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                            <div class="d-flex align-items-start gap-10">
                                                                <span class="text-20 text-warning-600 line-height-1"><i class="ph ph-users-three"></i></span>
                                                                <div>
                                                                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                        <c:out value="${classGroup.code}"/>
                                                                    </a>
                                                                    <span class="gape-node-meta text-12">
                                                                        <span><c:out value="${classGroup.modalityLabel}"/></span>
                                                                        <span><c:out value="${classGroup.shift}"/></span>
                                                                        <span><c:out value="${classGroup.dateRangeLabel}"/></span>
                                                                        <span><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled</span>
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
                                                            </div>
                                                        </div>
                                                        <div id="${activityPanelId}" class="d-none">
                                                            <%@ include file="/WEB-INF/fragments/class-group-activities-panel.jspf" %>
                                                        </div>
                                                    </div>
                                                </c:forEach>
                                                <c:if test="${empty subjectClassGroups}">
                                                    <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                        No class groups registered for this subject in this course.
                                                    </div>
                                                </c:if>
                                            </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty courseSubjects}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No subjects associated with this course.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <c:if test="${not course.archived and canModifyCourse}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveCourse">Archive</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteCourse">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${not course.archived and canModifyCourse}">
<div class="modal fade" id="archiveCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Archive Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${course.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the course if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/delete" method="post" class="m-0">
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
            var label = button.querySelector('[data-gape-toggle-label]');
            button.setAttribute('aria-expanded', String(isExpanded));
            button.setAttribute('title', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            button.setAttribute('aria-label', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            if (icon) {
                icon.classList.toggle('ph-caret-down', !isExpanded);
                icon.classList.toggle('ph-caret-up', isExpanded);
            }
            if (label) {
                label.textContent = isExpanded ? 'Hide' : 'Show';
            }
        });
    });
</script>
</body>
</html>
