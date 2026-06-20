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

        .gape-hierarchy-node {
            border-inline-start: 3px solid var(--main-600);
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

        .gape-organization-card {
            background-color: #fff;
            border: 1px solid #d9e2ef;
            border-radius: 10px;
            overflow: hidden;
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .gape-organization-card:hover {
            background-color: #f8fbff;
            border-color: #b8c7dc;
        }

        .gape-organization-summary {
            display: grid;
            gap: 16px;
            grid-template-columns: minmax(280px, 1.4fr) minmax(150px, 0.7fr) minmax(90px, 0.4fr) minmax(120px, 0.5fr) minmax(150px, auto);
            padding: 20px;
        }

        .gape-organization-list-header {
            display: grid;
            gap: 16px;
            grid-template-columns: minmax(280px, 1.4fr) minmax(150px, 0.7fr) minmax(90px, 0.4fr) minmax(120px, 0.5fr) minmax(150px, auto);
            padding: 0 20px 12px;
        }

        .gape-organization-units-panel {
            background-color: #f8fbff;
            border-top: 1px solid #d9e2ef;
            padding: 20px;
        }

        .gape-structure-panel {
            background-color: #f8fbff;
            border: 1px solid #d9e2ef;
            border-radius: 8px;
            margin-top: 14px;
            padding: 14px;
        }

        .gape-course-node {
            border-inline-start: 3px solid #2563eb;
        }

        .gape-subject-node {
            border-inline-start: 3px solid #16a34a;
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

        @media (max-width: 1199.98px) {
            .gape-organization-summary,
            .gape-organization-list-header {
                grid-template-columns: 1fr;
            }

            .gape-organization-list-header {
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
                        <c:if test="${canCreateOrganizations}">
                            <a href="${pageContext.request.contextPath}/admin/organizations/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>New Organization
                            </a>
                        </c:if>
                    </div>
                    <div class="gape-organization-list">
                        <div class="gape-organization-list-header">
                            <span class="text-14 fw-medium text-neutral-600">Organization</span>
                            <span class="text-14 fw-medium text-neutral-600">Type</span>
                            <span class="text-14 fw-medium text-neutral-600">Units</span>
                            <span class="text-14 fw-medium text-neutral-600">State</span>
                            <span class="text-14 fw-medium text-neutral-600 text-end">Actions</span>
                        </div>
                        <div class="d-flex flex-column gap-14">
                            <c:forEach var="organization" items="${organizations}">
                                <c:set var="canCreateOrganicUnits" value="${canCreateOrganicUnitsByOrganizationId[organization.id]}" />
                                <c:set var="organizationPhotoUrl" value=""/>
                                <c:if test="${organization.hasPhoto}">
                                    <c:set var="organizationPhotoUrl" value="${pageContext.request.contextPath}/media/${organization.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <section class="gape-organization-card">
                                    <div class="gape-organization-summary">
                                        <div class="d-flex align-items-center gap-12">
                                            <c:choose>
                                                <c:when test="${organization.hasPhoto}">
                                                    <img src="${organizationPhotoUrl}"
                                                         alt=""
                                                         class="gape-organization-table-photo flex-shrink-0"
                                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No organization photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No organization photo">
                                                        <i class="ph ph-image"></i>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${organization.name}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${organization.name}'/>"><c:out value="${organization.acronym}"/></span>
                                                </span>
                                            </div>
                                        </div>
                                        <div class="text-14 text-neutral-500 d-flex align-items-center"><c:out value="${organization.typeLabel}"/></div>
                                        <div class="text-14 text-neutral-500 d-flex align-items-center"><c:out value="${organization.organicUnitCount}"/></div>
                                        <div class="d-flex align-items-center">
                                            <span class="${organization.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                                <c:out value="${organization.stateLabel}"/>
                                            </span>
                                        </div>
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show organic units"
                                                    aria-label="Show organic units"
                                                    aria-expanded="false"
                                                    aria-controls="organizationUnits${organization.id}"
                                                    data-gape-tree-toggle="organizationUnits${organization.id}"
                                                    data-gape-open-title="Hide organic units"
                                                    data-gape-closed-title="Show organic units">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canModifyOrganizations}">
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${canModifyOrganizations}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteOrganization${organization.id}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>
                                    </div>

                                    <div id="organizationUnits${organization.id}" class="gape-organization-units-panel d-none">
                                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-16">
                                            <div>
                                                <h3 class="text-16 fw-medium text-neutral-700 mb-4">Organic Units</h3>
                                                <span class="text-13 text-neutral-500">
                                                    <c:out value="${organization.name}"/> &middot; <c:out value="${organization.organicUnitCount}"/> units
                                                </span>
                                            </div>
                                            <c:if test="${canCreateOrganicUnits}">
                                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/new" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                                    <i class="ph ph-plus-circle me-8"></i>New Unit
                                                </a>
                                            </c:if>
                                        </div>

                                        <div class="d-flex flex-column gap-12">
                                            <c:forEach var="unit" items="${organization.organicUnits}">
                                                <c:set var="canModifyUnit" value="${canModifyOrganicUnitById[unit.id]}" />
                                                <div class="gape-hierarchy-node border border-neutral-30 rounded-12 px-20 py-16 bg-white" style="margin-left: ${unit.hierarchyIndent}px;">
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
                                                            <button type="button"
                                                                    class="gape-tree-toggle text-21 text-neutral-500 hover-text-main-600"
                                                                    title="Show courses"
                                                                    aria-label="Show courses"
                                                                    aria-expanded="false"
                                                                    aria-controls="unitCourses${unit.id}"
                                                                    data-gape-tree-toggle="unitCourses${unit.id}"
                                                                    data-gape-open-title="Hide courses"
                                                                    data-gape-closed-title="Show courses">
                                                                <i class="ph ph-caret-down"></i>
                                                            </button>
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

                                                    <div id="unitCourses${unit.id}" class="gape-structure-panel d-none">
                                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                            <div>
                                                                <h3 class="text-16 fw-medium text-neutral-700 mb-4">Courses</h3>
                                                                <span class="text-13 text-neutral-500">
                                                                    <c:out value="${unit.code}"/> &middot; <c:out value="${unit.courseCount}"/> courses
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <div class="d-flex flex-column gap-10">
                                                            <c:forEach var="course" items="${unit.courses}">
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
                                                                                    <span><c:out value="${course.subjectCount}"/> subjects</span>
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                        <div class="d-flex align-items-center gap-10 flex-wrap">
                                                                            <span class="${course.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                                <c:out value="${course.stateLabel}"/>
                                                                            </span>
                                                                            <button type="button"
                                                                                    class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                                    title="Show subjects"
                                                                                    aria-label="Show subjects"
                                                                                    aria-expanded="false"
                                                                                    aria-controls="courseSubjects${course.id}"
                                                                                    data-gape-tree-toggle="courseSubjects${course.id}"
                                                                                    data-gape-open-title="Hide subjects"
                                                                                    data-gape-closed-title="Show subjects">
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
                                                                                <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteTreeCourse${course.id}">
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
                                                                        <div class="modal fade" id="deleteTreeCourse${course.id}" tabindex="-1" aria-hidden="true">
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

                                                                    <div id="courseSubjects${course.id}" class="gape-structure-panel d-none">
                                                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                                            <div>
                                                                                <h5 class="text-14 fw-medium text-neutral-700 mb-2">Subjects</h5>
                                                                                <span class="text-12 text-neutral-500">
                                                                                    <c:out value="${course.name}"/> &middot; <c:out value="${course.subjectCount}"/> subjects
                                                                                </span>
                                                                            </div>
                                                                        </div>
                                                                        <div class="d-flex flex-column gap-10">
                                                                            <c:forEach var="subject" items="${course.subjects}">
                                                                                <c:set var="canModifySubject" value="${canModifySubjectById[subject.subjectId]}" />
                                                                                <c:set var="canManageSubjectAssociations" value="${canManageSubjectAssociationsById[subject.subjectId]}" />
                                                                                <div class="gape-subject-node border border-neutral-30 rounded-8 px-16 py-14 bg-white">
                                                                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                                                        <div class="d-flex align-items-start gap-10">
                                                                                            <span class="text-20 text-success-600 line-height-1"><i class="ph ph-book-open-text"></i></span>
                                                                                            <div>
                                                                                                <a href="${pageContext.request.contextPath}/admin/subjects/${subject.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                                                                                    <span class="ms-4"><c:out value="${subject.name}"/></span>
                                                                                                </a>
                                                                                                <span class="gape-node-meta text-12">
                                                                                                    <span><c:out value="${subject.curricularPositionLabel}"/></span>
                                                                                                    <span><c:out value="${subject.mandatoryLabel}"/></span>
                                                                                                    <span><c:out value="${subject.classGroupCount}"/> class groups</span>
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
                                                                                                    aria-controls="subjectClassGroups${course.id}_${subject.subjectId}"
                                                                                                    data-gape-tree-toggle="subjectClassGroups${course.id}_${subject.subjectId}"
                                                                                                    data-gape-open-title="Hide class groups"
                                                                                                    data-gape-closed-title="Show class groups">
                                                                                                <i class="ph ph-caret-down"></i>
                                                                                            </button>
                                                                                            <a href="${pageContext.request.contextPath}/admin/subjects/${subject.subjectId}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail">
                                                                                                <i class="ph ph-eye"></i>
                                                                                            </a>
                                                                                            <c:if test="${not subject.archived and canModifySubject}">
                                                                                                <a href="${pageContext.request.contextPath}/admin/subjects/${subject.subjectId}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit">
                                                                                                    <i class="ph ph-pencil-simple-line"></i>
                                                                                                </a>
                                                                                            </c:if>
                                                                                            <c:if test="${not subject.archived and canManageSubjectAssociations}">
                                                                                                <a href="${pageContext.request.contextPath}/admin/subjects/${subject.subjectId}/courses" class="text-20 text-neutral-500 hover-text-main-600" title="Associate courses">
                                                                                                    <i class="ph ph-link-simple"></i>
                                                                                                </a>
                                                                                            </c:if>
                                                                                            <c:if test="${not subject.archived and canModifySubject}">
                                                                                                <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteTreeSubject${course.id}_${subject.subjectId}">
                                                                                                    <i class="ph ph-trash"></i>
                                                                                                </button>
                                                                                            </c:if>
                                                                                        </div>
                                                                                    </div>

                                                                                    <c:if test="${not subject.archived and canModifySubject}">
                                                                                        <div class="modal fade" id="deleteTreeSubject${course.id}_${subject.subjectId}" tabindex="-1" aria-hidden="true">
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
                                                                                                        <form action="${pageContext.request.contextPath}/admin/subjects/${subject.subjectId}/delete" method="post" class="m-0">
                                                                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                                                            <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                                                                        </form>
                                                                                                    </div>
                                                                                                </div>
                                                                                            </div>
                                                                                        </div>
                                                                                    </c:if>

                                                                                    <div id="subjectClassGroups${course.id}_${subject.subjectId}" class="gape-structure-panel d-none">
                                                                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                                                            <div>
                                                                                                <h6 class="text-14 fw-medium text-neutral-700 mb-2">Class Groups</h6>
                                                                                                <span class="text-12 text-neutral-500">
                                                                                                    <c:out value="${subject.name}"/> &middot; <c:out value="${subject.classGroupCount}"/> class groups
                                                                                                </span>
                                                                                            </div>
                                                                                        </div>
                                                                                        <div class="d-flex flex-column gap-10">
                                                                                            <c:forEach var="classGroup" items="${subject.classGroups}">
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
                                                                                                                <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteTreeClassGroup${classGroup.id}">
                                                                                                                    <i class="ph ph-trash"></i>
                                                                                                                </button>
                                                                                                            </c:if>
                                                                                                        </div>
                                                                                                    </div>

                                                                                                    <c:if test="${not classGroup.archived and canManageClassGroupStructure}">
                                                                                                        <div class="modal fade" id="deleteTreeClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true">
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
                                                                                            <c:if test="${empty subject.classGroups}">
                                                                                                <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                                                                    No class groups registered for this subject in this course.
                                                                                                </div>
                                                                                            </c:if>
                                                                                        </div>
                                                                                    </div>
                                                                                </div>
                                                                            </c:forEach>
                                                                            <c:if test="${empty course.subjects}">
                                                                                <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                                                    No subjects associated with this course.
                                                                                </div>
                                                                            </c:if>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </c:forEach>
                                                            <c:if test="${empty unit.courses}">
                                                                <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                                    No courses registered for this organic unit.
                                                                </div>
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
                                            <c:if test="${empty organization.organicUnits}">
                                                <div class="border border-neutral-30 rounded-12 px-20 py-24 text-center text-14 text-neutral-500 bg-white">
                                                    No organic units registered.
                                                </div>
                                            </c:if>
                                        </div>
                                    </div>

                                    <c:if test="${canModifyOrganizations}">
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
                                    </c:if>
                                </section>
                            </c:forEach>
                            <c:if test="${empty organizations}">
                                <div class="border border-neutral-30 rounded-12 px-20 py-32 text-center text-14 text-neutral-500 bg-white">
                                    No managed organizations found.
                                </div>
                            </c:if>
                        </div>
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
