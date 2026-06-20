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
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Subjects</title>
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

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
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

        .gape-course-node {
            border-inline-start: 3px solid #2563eb;
        }

        .gape-class-group-node {
            border-inline-start: 3px solid #7c3aed;
        }

        .gape-node-meta {
            color: #64748b;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
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
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Total</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${subjectCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeSubjects}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Archived</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${archivedSubjects}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Subject Management</h2>
                            <span class="text-14 text-neutral-500">Subjects, workload and coordinator assignment.</span>
                        </div>
                        <c:if test="${canCreateSubjects}">
                            <a href="${pageContext.request.contextPath}${subjectBasePath}/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>New Subject
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
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Workload</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="subject" items="${subjects}">
                                <c:set var="canModifySubjectRow" value="${canModifySubjectById[subject.id]}" />
                                <c:set var="canManageSubjectAssociationsRow" value="${canManageSubjectAssociationsById[subject.id]}" />
                                <c:set var="subjectCourses" value="${subjectCoursesBySubject[subject.id]}"/>
                                <c:set var="subjectPhotoUrl" value=""/>
                                <c:if test="${subject.hasPhoto}">
                                    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${subject.hasPhoto}">
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
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${subject.name}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                                </span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-8 flex-wrap">
                                            <c:forEach var="association" items="${subjectCourses}">
                                                <span class="px-12 py-6 border-neutral-30 border rounded-8 text-12 text-neutral-600" title="<c:out value='${association.courseContextTitle}'/>">
                                                    <c:out value="${association.courseContextHtml}" escapeXml="false"/>
                                                </span>
                                            </c:forEach>
                                            <c:if test="${empty subjectCourses}">
                                                <span class="text-14 text-neutral-500"><c:out value="${subject.organizationContextHtml}" escapeXml="false"/></span>
                                            </c:if>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${subject.ectsLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${subject.workloadHoursLabel}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${subject.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show courses"
                                                    aria-label="Show courses"
                                                    aria-expanded="false"
                                                    aria-controls="subjectCourses${subject.id}"
                                                    data-gape-tree-toggle="subjectCourses${subject.id}"
                                                    data-gape-open-title="Hide courses"
                                                    data-gape-closed-title="Show courses">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${not subject.archived and canModifySubjectRow}">
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${not subject.archived and canManageSubjectAssociationsRow}">
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/courses" class="text-22 text-neutral-500 hover-text-main-600" title="Associate courses">
                                                    <i class="ph ph-link-simple"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${not subject.archived and canModifySubjectRow}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubject${subject.id}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <c:if test="${canModifySubjectRow}">
                                        <div class="modal fade" id="deleteSubject${subject.id}" tabindex="-1" aria-hidden="true">
                                            <div class="modal-dialog modal-dialog-centered">
                                                <div class="modal-content rounded-12 border-0">
                                                    <div class="modal-header border-neutral-30">
                                                        <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${subject.name}"/></strong> if it has no dependencies.</p>
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
                                    </td>
                                </tr>
                                <tr id="subjectCourses${subject.id}" class="d-none">
                                    <td colspan="6" class="py-0 px-20 bg-neutral-20">
                                        <div class="gape-structure-panel">
                                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                <div>
                                                    <h3 class="text-16 fw-medium text-neutral-700 mb-4">Courses</h3>
                                                    <span class="text-13 text-neutral-500">
                                                        <c:out value="${subject.name}"/> &middot; <c:out value="${fn:length(subjectCourses)}"/> courses
                                                    </span>
                                                </div>
                                            </div>
                                            <div class="d-flex flex-column gap-10">
                                                <c:forEach var="association" items="${subjectCourses}">
                                                    <c:set var="course" value="${association.course}" />
                                                    <c:set var="classGroups" value="${subjectCourseClassGroupsBySubjectAndCourse[subject.id][course.id]}" />
                                                    <c:set var="canModifyCourse" value="${canModifyCourseById[course.id]}" />
                                                    <c:set var="canManageCourseChildren" value="${canManageCourseChildrenById[course.id]}" />
                                                    <div class="gape-course-node border border-neutral-30 rounded-8 px-16 py-14 bg-white">
                                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                            <div class="d-flex align-items-start gap-10">
                                                                <span class="text-20 text-main-600 line-height-1"><i class="ph ph-graduation-cap"></i></span>
                                                                <div>
                                                                    <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                        <span class="gape-acronym-token" tabindex="0" title="<c:out value='${course.name}'/>"><c:out value="${course.acronym}"/></span>
                                                                        <span class="ms-4"><c:out value="${course.name}"/></span>
                                                                    </a>
                                                                    <span class="gape-node-meta text-12">
                                                                        <span><c:out value="${course.typeLabel}"/></span>
                                                                        <span><c:out value="${association.curricularPositionLabel}"/></span>
                                                                        <span><c:out value="${association.mandatoryLabel}"/></span>
                                                                        <span><c:out value="${fn:length(classGroups)}"/> class groups</span>
                                                                    </span>
                                                                </div>
                                                            </div>
                                                            <div class="d-flex align-items-center gap-10 flex-wrap">
                                                                <span class="${association.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                    <c:out value="${association.stateLabel}"/>
                                                                </span>
                                                                <button type="button"
                                                                        class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                        title="Show class groups"
                                                                        aria-label="Show class groups"
                                                                        aria-expanded="false"
                                                                        aria-controls="subjectCourseClassGroups${subject.id}_${course.id}"
                                                                        data-gape-tree-toggle="subjectCourseClassGroups${subject.id}_${course.id}"
                                                                        data-gape-open-title="Hide class groups"
                                                                        data-gape-closed-title="Show class groups">
                                                                    <i class="ph ph-caret-down"></i>
                                                                </button>
                                                                <a href="${pageContext.request.contextPath}/admin/courses/${course.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail">
                                                                    <i class="ph ph-eye"></i>
                                                                </a>
                                                                <c:if test="${not course.archived and canModifyCourse}">
                                                                    <a href="${pageContext.request.contextPath}/admin/courses/${course.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit">
                                                                        <i class="ph ph-pencil-simple-line"></i>
                                                                    </a>
                                                                </c:if>
                                                                <c:if test="${not course.archived and canManageCourseChildren}">
                                                                    <a href="${pageContext.request.contextPath}/admin/courses/${course.id}/subjects/new" class="text-20 text-neutral-500 hover-text-main-600" title="Associate subject">
                                                                        <i class="ph ph-link-simple"></i>
                                                                    </a>
                                                                </c:if>
                                                                <c:if test="${not course.archived and canModifyCourse}">
                                                                    <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubjectPanelCourse${subject.id}_${course.id}">
                                                                        <i class="ph ph-trash"></i>
                                                                    </button>
                                                                </c:if>
                                                                <c:if test="${course.archived and canModifyCourse}">
                                                                    <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/unarchive" method="post" class="m-0">
                                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                        <button type="submit" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Unarchive">
                                                                            <i class="ph ph-arrow-u-up-left"></i>
                                                                        </button>
                                                                    </form>
                                                                </c:if>
                                                            </div>
                                                        </div>

                                                        <c:if test="${not course.archived and canModifyCourse}">
                                                        <div class="modal fade" id="deleteSubjectPanelCourse${subject.id}_${course.id}" tabindex="-1" aria-hidden="true">
                                                            <div class="modal-dialog modal-dialog-centered">
                                                                <div class="modal-content rounded-12 border-0">
                                                                    <div class="modal-header border-neutral-30">
                                                                        <h5 class="modal-title text-18 fw-semibold">Delete Course</h5>
                                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                                    </div>
                                                                    <div class="modal-body">
                                                                        <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${course.name}"/></strong> if it has no dependencies.</p>
                                                                    </div>
                                                                    <div class="modal-footer border-neutral-30">
                                                                        <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                                        <form action="${pageContext.request.contextPath}/admin/courses/${course.id}/delete" method="post" class="m-0">
                                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                                        </form>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        </c:if>

                                                        <div id="subjectCourseClassGroups${subject.id}_${course.id}" class="gape-structure-panel d-none">
                                                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                                <div>
                                                                    <h4 class="text-14 fw-medium text-neutral-700 mb-2">Class Groups</h4>
                                                                    <span class="text-12 text-neutral-500">
                                                                        <c:out value="${course.name}"/> &middot; <c:out value="${fn:length(classGroups)}"/> class groups
                                                                    </span>
                                                                </div>
                                                            </div>
                                                            <div class="d-flex flex-column gap-10">
                                                                <c:forEach var="classGroup" items="${classGroups}">
                                                                    <c:set var="canModifyClassGroup" value="${canModifyClassGroupById[classGroup.id]}" />
                                                                    <c:set var="canManageClassGroupStructure" value="${canManageClassGroupStructureById[classGroup.id]}" />
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
                                                                                    </span>
                                                                                </div>
                                                                            </div>
                                                                            <div class="d-flex align-items-center gap-10 flex-wrap">
                                                                                <span class="${classGroup.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                                    <c:out value="${classGroup.stateLabel}"/>
                                                                                </span>
                                                                                <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail">
                                                                                    <i class="ph ph-eye"></i>
                                                                                </a>
                                                                                <c:if test="${not classGroup.archived and canModifyClassGroup}">
                                                                                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit">
                                                                                        <i class="ph ph-pencil-simple-line"></i>
                                                                                    </a>
                                                                                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/new" class="text-20 text-neutral-500 hover-text-main-600" title="New content block">
                                                                                        <i class="ph ph-stack-plus"></i>
                                                                                    </a>
                                                                                </c:if>
                                                                                <c:if test="${not classGroup.archived and canManageClassGroupStructure}">
                                                                                    <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubjectPanelClassGroup${subject.id}_${course.id}_${classGroup.id}">
                                                                                        <i class="ph ph-trash"></i>
                                                                                    </button>
                                                                                </c:if>
                                                                            </div>
                                                                        </div>

                                                                        <c:if test="${not classGroup.archived and canManageClassGroupStructure}">
                                                                        <div class="modal fade" id="deleteSubjectPanelClassGroup${subject.id}_${course.id}_${classGroup.id}" tabindex="-1" aria-hidden="true">
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
                                                                    </div>
                                                                </c:forEach>
                                                                <c:if test="${empty classGroups}">
                                                                    <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                                        No class groups registered for this subject in this course.
                                                                    </div>
                                                                </c:if>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </c:forEach>
                                                <c:if test="${empty subjectCourses}">
                                                    <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                        No courses associated with this subject.
                                                    </div>
                                                </c:if>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty subjects}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No managed subjects found.</td>
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
