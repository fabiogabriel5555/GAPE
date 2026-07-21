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

        .gape-subject-actions {
            min-width: 0;
        }

        .gape-subject-management-panel,
        .gape-subject-table-scroll {
            min-width: 0;
        }

        .gape-subject-table {
            min-width: 960px;
        }

        .gape-subject-table.is-subject-grouped {
            min-width: 860px;
        }

        .gape-subject-table.is-subject-grouped [data-subject-context-column] {
            display: none;
        }

        .gape-subject-table.is-subject-grouped-by-organization [data-subject-row] > td:first-child {
            padding-inline-start: 56px !important;
        }

        .gape-subject-table.is-subject-grouped-by-course [data-subject-row] > td:first-child {
            padding-inline-start: 36px !important;
        }

        [data-subject-association-column] {
            min-width: 94px;
            white-space: nowrap;
        }

        .gape-filter-toggle.is-active {
            background-color: var(--main-600) !important;
            border-color: var(--main-600) !important;
            color: #fff !important;
        }

        .gape-filter-toggle.is-active:hover,
        .gape-filter-toggle.is-active:focus-visible {
            background-color: var(--main-700) !important;
            border-color: var(--main-700) !important;
            color: #fff !important;
        }

        .gape-filter-toggle.is-active i {
            color: inherit;
        }

        .gape-sort-option,
        .gape-group-option {
            background: transparent;
            border: 0;
            text-align: start;
            width: 100%;
        }

        .gape-sort-option.is-active,
        .gape-group-option.is-active {
            background-color: var(--main-50);
            color: var(--main-600);
        }

        .gape-sort-arrows,
        .gape-group-arrows {
            min-width: 42px;
        }

        .gape-sort-arrow,
        .gape-group-arrow {
            color: #94a3b8;
            opacity: 0.55;
            transition: color 0.2s ease, opacity 0.2s ease;
        }

        .gape-sort-option[data-sort-state="normal"] .gape-sort-arrow--normal,
        .gape-sort-option[data-sort-state="reverse"] .gape-sort-arrow--reverse,
        .gape-group-option[data-group-state="normal"] .gape-group-arrow--normal,
        .gape-group-option[data-group-state="reverse"] .gape-group-arrow--reverse {
            color: var(--main-600);
            opacity: 1;
        }

        .gape-subject-organization-group-row td {
            background: #dcecff;
            border-bottom: 1px solid #b7d4f5;
            border-top: 18px solid #fff;
            padding-block: 18px !important;
        }

        .gape-subject-organization-group-row:first-child td {
            border-top-width: 0;
        }

        .gape-subject-unit-group-row td {
            background: #f5f9ff;
            border-bottom: 1px solid #d5e4f6;
            border-top: 8px solid #fff;
            padding-block: 12px !important;
        }

        .gape-subject-course-group-row td {
            background: #fff;
            border-bottom: 1px solid #e2e8f0;
            border-top: 6px solid #f8fbff;
            padding-block: 10px !important;
        }

        .gape-subject-group-heading,
        .gape-subject-unit-heading,
        .gape-subject-course-heading {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: space-between;
        }

        .gape-subject-group-title,
        .gape-subject-unit-title,
        .gape-subject-course-title {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .gape-subject-group-unit {
            background: #fff;
            border: 1px solid #cddff4;
            border-radius: 999px;
            color: #475569;
            display: inline-flex;
            padding: 4px 10px;
        }

        .gape-subject-group-toggle {
            align-items: center;
            background: transparent;
            border: 1px solid transparent;
            border-radius: 999px;
            color: #64748b;
            display: inline-flex;
            height: 28px;
            justify-content: center;
            padding: 0;
            transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
            width: 28px;
        }

        .gape-subject-group-toggle:hover,
        .gape-subject-group-toggle:focus-visible {
            background-color: #eef4fb;
            border-color: #d5e4f6;
            color: #334155;
        }

        .gape-subject-unit-heading {
            padding-inline-start: 0;
        }

        .gape-subject-course-heading {
            padding-inline-start: 20px;
        }

        .gape-subject-unit-marker,
        .gape-subject-course-marker {
            border-radius: 999px;
            display: inline-flex;
            height: 8px;
            width: 8px;
        }

        .gape-subject-unit-marker {
            background: var(--main-600);
        }

        .gape-subject-course-marker {
            background: #16a34a;
        }

        @media (max-width: 575.98px) {
            .gape-class-activities-panel {
                margin-inline-start: 0;
            }

            .gape-class-activities-panel::before {
                display: none;
            }

            .gape-subject-actions {
                width: 100%;
            }

            .gape-subject-management-panel {
                max-width: calc(100vw - 48px);
                overflow: hidden;
            }

            .gape-subject-table-scroll {
                max-width: 100%;
            }

            .gape-subject-actions > .dropdown {
                flex: 1 1 calc(50% - 6px);
                min-width: 0;
            }

            .gape-subject-actions > .dropdown > button,
            .gape-subject-actions > a {
                justify-content: center;
                text-align: center;
                white-space: nowrap;
                width: 100%;
            }

            .gape-subject-actions > a {
                flex: 1 1 100%;
            }

            .gape-subject-table.is-subject-grouped-by-organization [data-subject-row] > td:first-child {
                padding-inline-start: 44px !important;
            }

            .gape-subject-table.is-subject-grouped-by-course [data-subject-row] > td:first-child {
                padding-inline-start: 36px !important;
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
                            <span class="text-14 text-neutral-500">Inactive</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${inactiveSubjects}</h2>
                        </div>
                    </div>
                </div>

                <div class="gape-subject-management-panel bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Subject Management</h2>
                            <span class="text-14 text-neutral-500">Subjects, workload and coordinator assignment.</span>
                        </div>
                        <div class="gape-subject-actions d-flex align-items-center gap-12 flex-wrap">
                            <div class="dropdown">
                                <button type="button"
                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="outside"
                                        data-subject-sort-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-sort-ascending"></i>Sort by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-subject-sort-option
                                                data-sort-field="name"
                                                data-sort-normal="asc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>Name</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-subject-sort-option
                                                data-sort-field="date"
                                                data-sort-normal="desc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>Date</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-subject-sort-option
                                                data-sort-field="status"
                                                data-sort-normal="asc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>State</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <div class="dropdown">
                                <button type="button"
                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="outside"
                                        data-subject-group-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-stack"></i>Group by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-subject-group-option
                                                data-group-field="organization"
                                                data-group-normal="asc"
                                                data-group-state="none"
                                                aria-pressed="false">
                                            <span>Organization</span>
                                            <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-subject-group-option
                                                data-group-field="course"
                                                data-group-normal="asc"
                                                data-group-state="none"
                                                aria-pressed="false">
                                            <span>Courses</span>
                                            <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <c:if test="${canCreateSubjects}">
                                <a href="${pageContext.request.contextPath}${subjectBasePath}/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Subject
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="gape-subject-table-scroll overflow-x-auto">
                        <table class="table mb-0 gape-subject-table">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Subject</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-subject-context-column>Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">ECTS</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-subject-association-column>No. Associations</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-subject-list>
                            <%@ include file="/WEB-INF/fragments/subject-list-rows.jsp" %>
                            <c:if test="${false}">
                            <c:forEach var="subject" items="${subjects}" varStatus="subjectLoop">
                                <c:set var="canModifySubjectRow" value="${canModifySubjectById[subject.id]}" />
                                <c:set var="subjectCourses" value="${subjectCoursesBySubject[subject.id]}"/>
                                <c:set var="subjectClassGroups" value="${classGroupsBySubject[subject.id]}"/>
                                <c:set var="primarySubjectCourse" value="${empty subjectCourses ? null : subjectCourses[0]}"/>
                                <c:set var="subjectGroupCourseId" value="none"/>
                                <c:set var="subjectGroupCourseName" value="No course"/>
                                <c:set var="subjectGroupOrganicUnitId" value="none"/>
                                <c:set var="subjectGroupOrganicUnitName" value="No organic unit"/>
                                <c:if test="${not empty primarySubjectCourse}">
                                    <c:set var="subjectGroupCourseId" value="${primarySubjectCourse.courseId}"/>
                                    <c:set var="subjectGroupCourseName" value="${primarySubjectCourse.courseName}"/>
                                    <c:set var="subjectGroupOrganicUnitId" value="${empty primarySubjectCourse.course.organicUnitId ? 'none' : primarySubjectCourse.course.organicUnitId}"/>
                                    <c:set var="subjectGroupOrganicUnitName" value="${primarySubjectCourse.course.organicUnitLabel}"/>
                                </c:if>
                                <c:set var="subjectPhotoUrl" value=""/>
                                <c:if test="${subject.hasPhoto}">
                                    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
                                </c:if>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-subject-row
                                    data-sort-index="${subjectLoop.index}"
                                    data-sort-name="${fn:escapeXml(subject.name)}"
                                    data-sort-date="${subject.id}"
                                    data-sort-status="${fn:escapeXml(subject.stateLabel)}"
                                    data-group-organization-id="${subject.organizationId}"
                                    data-group-organization="${fn:escapeXml(subject.organizationName)}"
                                    data-group-organic-unit-id="${subjectGroupOrganicUnitId}"
                                    data-group-organic-unit="${fn:escapeXml(subjectGroupOrganicUnitName)}"
                                    data-group-course-id="${subjectGroupCourseId}"
                                    data-group-course="${fn:escapeXml(subjectGroupCourseName)}">
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
                                    <td class="py-20 px-20" data-subject-context-column>
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
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-subject-association-column><c:out value="${fn:length(subjectCourses)}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${subject.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canModifySubjectRow}">
                                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${canModifySubjectRow}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteSubject${subject.id}">
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
                            </c:forEach>
                            <c:if test="${empty subjects}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No managed subjects found.</td>
                                </tr>
                            </c:if>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${subjectCount > 10}">
                        <div class="d-flex justify-content-end mt-20" data-subject-pagination data-total="${subjectCount}" data-page-size="10">
                            <div class="gape-list-pagination">
                                <nav class="gape-list-pagination-pages" aria-label="Subject pages" data-subject-pagination-pages></nav>
                                <button type="button" class="gape-list-load-all" data-subject-load-all>Load All</button>
                            </div>
                        </div>
                    </c:if>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        function initializeSubjectList() {
        var list = document.querySelector('[data-subject-list]');
        if (!list) {
            return;
        }

        var table = list.closest('table');
        var rows = Array.prototype.slice.call(list.querySelectorAll('[data-subject-row]'));
        var contextColumns = table ? Array.prototype.slice.call(table.querySelectorAll('[data-subject-context-column]')) : [];
        var sortOptions = Array.prototype.slice.call(document.querySelectorAll('[data-subject-sort-option]'));
        var groupOptions = Array.prototype.slice.call(document.querySelectorAll('[data-subject-group-option]'));
        var sortToggle = document.querySelector('[data-subject-sort-toggle]');
        var groupToggle = document.querySelector('[data-subject-group-toggle]');
        var pagination = document.querySelector('[data-subject-pagination]');
        var paginationPages = document.querySelector('[data-subject-pagination-pages]');
        var loadAllButton = document.querySelector('[data-subject-load-all]');
        var initialLoadMoreMeta = list.querySelector('[data-subject-load-more-meta]');
        var currentPage = 1;
        var showingAll = false;
        var isLoadingPage = false;
        if (initialLoadMoreMeta) {
            currentPage = Number(initialLoadMoreMeta.dataset.currentPage) || 1;
            showingAll = initialLoadMoreMeta.dataset.loadAll === 'true';
            initialLoadMoreMeta.remove();
        }
        if (!rows.length) {
            return;
        }

        var rowGroups = [];
        function refreshRowGroups() {
            rowGroups = Array.prototype.slice.call(list.querySelectorAll('[data-subject-row]')).map(function (row) {
                return {
                    row: row
                };
            });
        }
        refreshRowGroups();
        var collator = new Intl.Collator(document.documentElement.lang || undefined, {
            numeric: true,
            sensitivity: 'base'
        });
        var activeField = null;
        var activeDirection = null;
        var activeGroupField = null;
        var activeGroupDirection = null;
        var collapsedOrganizationGroups = new Set();
        var collapsedUnitGroups = new Set();
        var collapsedCourseGroups = new Set();

        function oppositeDirection(direction) {
            return direction === 'asc' ? 'desc' : 'asc';
        }

        function datasetKey(field) {
            return 'sort' + field.charAt(0).toUpperCase() + field.slice(1);
        }

        function numericValue(row, field) {
            return Number(row.dataset[datasetKey(field)] || '0');
        }

        function textValue(row, field) {
            return row.dataset[datasetKey(field)] || '';
        }

        function originalIndex(group) {
            return numericValue(group.row, 'index');
        }

        function compareGroups(field, direction, first, second) {
            var result;
            if (field === 'date' || field === 'ects') {
                result = numericValue(first.row, field) - numericValue(second.row, field);
            } else {
                result = collator.compare(textValue(first.row, field), textValue(second.row, field));
            }

            if (result === 0) {
                result = originalIndex(first) - originalIndex(second);
            }

            return direction === 'desc' ? -result : result;
        }

        function sortedRowGroups() {
            var sorted = rowGroups.slice();
            if (activeField && activeDirection) {
                sorted.sort(function (first, second) {
                    return compareGroups(activeField, activeDirection, first, second);
                });
            } else {
                sorted.sort(function (first, second) {
                    return originalIndex(first) - originalIndex(second);
                });
            }
            return sorted;
        }

        function organizationKey(group) {
            return group.row.dataset.groupOrganizationId || group.row.dataset.groupOrganization || '';
        }

        function unitKey(group) {
            return [
                organizationKey(group),
                group.row.dataset.groupOrganicUnitId || 'none'
            ].join('::');
        }

        function courseKey(group) {
            return [
                unitKey(group),
                group.row.dataset.groupCourseId || 'none'
            ].join('::');
        }

        function standaloneCourseKey(group) {
            return [
                group.row.dataset.groupCourseId || 'none',
                group.row.dataset.groupCourse || ''
            ].join('::');
        }

        function directionMultiplier(direction) {
            return direction === 'desc' ? -1 : 1;
        }

        function compareGroupLabels(direction, first, second) {
            var result = collator.compare(first.label, second.label);
            if (result === 0) {
                result = collator.compare(first.key, second.key);
            }
            return result * directionMultiplier(direction);
        }

        function countLabel(count, singular, plural) {
            return count === 1 ? '1 ' + singular : count + ' ' + plural;
        }

        function clearCollapsedGroups() {
            collapsedOrganizationGroups.clear();
            collapsedUnitGroups.clear();
            collapsedCourseGroups.clear();
        }

        function visibleColumnCount() {
            return activeGroupField ? 5 : 6;
        }

        function updateGroupedLayout() {
            var grouped = activeGroupField !== null;
            if (table) {
                table.classList.toggle('is-subject-grouped', grouped);
                table.classList.toggle('is-subject-grouped-by-organization', activeGroupField === 'organization');
                table.classList.toggle('is-subject-grouped-by-course', activeGroupField === 'course');
            }
            contextColumns.forEach(function (column) {
                column.toggleAttribute('hidden', grouped);
            });
        }

        function updateDetailColumnCount(group) {
            if (!group.detail) {
                return;
            }
            var cell = group.detail.querySelector('td[colspan]');
            if (cell) {
                cell.colSpan = visibleColumnCount();
            }
        }

        function createGroupToggleButton(isExpanded, expandedLabel, collapsedLabel, onToggle) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'gape-subject-group-toggle text-18';
            button.setAttribute('aria-expanded', String(isExpanded));
            button.setAttribute('title', isExpanded ? expandedLabel : collapsedLabel);
            button.setAttribute('aria-label', isExpanded ? expandedLabel : collapsedLabel);

            var icon = document.createElement('i');
            icon.className = isExpanded ? 'ph ph-caret-up' : 'ph ph-caret-down';
            icon.setAttribute('aria-hidden', 'true');
            button.appendChild(icon);

            button.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                onToggle();
                renderRows();
            });
            return button;
        }

        function createSubjectOrganizationGroupHeader(bucket) {
            var header = document.createElement('tr');
            header.className = 'gape-subject-organization-group-row';
            header.setAttribute('data-subject-group-row', '');
            header.setAttribute('data-subject-organization-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-14';

            var heading = document.createElement('div');
            heading.className = 'gape-subject-group-heading';

            var title = document.createElement('div');
            title.className = 'gape-subject-group-title';

            var icon = document.createElement('i');
            icon.className = 'ph ph-buildings text-20 text-main-600';
            icon.setAttribute('aria-hidden', 'true');

            var organization = document.createElement('span');
            organization.className = 'text-14 fw-semibold text-neutral-700';
            organization.textContent = bucket.label;

            var unitCount = document.createElement('span');
            unitCount.className = 'gape-subject-group-unit text-12 fw-medium';
            unitCount.textContent = countLabel(bucket.units.length, 'department', 'departments');

            var courseCount = document.createElement('span');
            courseCount.className = 'gape-subject-group-unit text-12 fw-medium';
            courseCount.textContent = countLabel(bucket.courseCount, 'course', 'courses');

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.subjectCount, 'subject', 'subjects');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedOrganizationGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide organization subjects', 'Show organization subjects', function () {
                if (collapsedOrganizationGroups.has(bucket.key)) {
                    collapsedOrganizationGroups.delete(bucket.key);
                } else {
                    collapsedOrganizationGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-subject-organization-visibility-toggle', bucket.key);

            title.appendChild(icon);
            title.appendChild(organization);
            title.appendChild(unitCount);
            title.appendChild(courseCount);
            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createSubjectUnitGroupHeader(bucket) {
            var header = document.createElement('tr');
            header.className = 'gape-subject-unit-group-row';
            header.setAttribute('data-subject-group-row', '');
            header.setAttribute('data-subject-unit-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-12';

            var heading = document.createElement('div');
            heading.className = 'gape-subject-unit-heading';

            var title = document.createElement('div');
            title.className = 'gape-subject-unit-title';

            var marker = document.createElement('span');
            marker.className = 'gape-subject-unit-marker';
            marker.setAttribute('aria-hidden', 'true');

            var unit = document.createElement('span');
            unit.className = 'text-13 fw-semibold text-neutral-700';
            unit.textContent = bucket.label;

            var courseCount = document.createElement('span');
            courseCount.className = 'text-12 text-neutral-500';
            courseCount.textContent = countLabel(bucket.courses.length, 'course', 'courses');

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.subjectCount, 'subject', 'subjects');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedUnitGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide organic unit subjects', 'Show organic unit subjects', function () {
                if (collapsedUnitGroups.has(bucket.key)) {
                    collapsedUnitGroups.delete(bucket.key);
                } else {
                    collapsedUnitGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-subject-unit-visibility-toggle', bucket.key);

            title.appendChild(marker);
            title.appendChild(unit);
            actions.appendChild(courseCount);
            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createSubjectCourseGroupHeader(bucket, nested) {
            var header = document.createElement('tr');
            header.className = 'gape-subject-course-group-row';
            header.setAttribute('data-subject-group-row', '');
            header.setAttribute('data-subject-course-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-10';

            var heading = document.createElement('div');
            heading.className = nested ? 'gape-subject-course-heading' : 'gape-subject-group-heading';

            var title = document.createElement('div');
            title.className = 'gape-subject-course-title';

            var marker = document.createElement('span');
            marker.className = 'gape-subject-course-marker';
            marker.setAttribute('aria-hidden', 'true');

            var course = document.createElement('span');
            course.className = 'text-13 fw-semibold text-neutral-700';
            course.textContent = bucket.label;

            var context = document.createElement('span');
            context.className = 'text-12 text-neutral-500';
            context.textContent = bucket.context;

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.items.length, 'subject', 'subjects');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedCourseGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide course subjects', 'Show course subjects', function () {
                if (collapsedCourseGroups.has(bucket.key)) {
                    collapsedCourseGroups.delete(bucket.key);
                } else {
                    collapsedCourseGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-subject-course-visibility-toggle', bucket.key);

            title.appendChild(marker);
            title.appendChild(course);
            if (bucket.context) {
                title.appendChild(context);
            }
            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function removeSubjectGroupHeaders() {
            Array.prototype.slice.call(list.querySelectorAll('[data-subject-group-row]')).forEach(function (header) {
                header.remove();
            });
        }

        function detachRowGroups() {
            rowGroups.forEach(function (group) {
                group.row.remove();
            });
        }

        function appendGroup(group) {
            updateDetailColumnCount(group);
            list.appendChild(group.row);
        }

        function renderOrganizationGroups(sorted) {
            var organizations = [];
            var organizationByKey = new Map();
            sorted.forEach(function (group) {
                var organizationBucketKey = organizationKey(group);
                var unitBucketKey = unitKey(group);
                var courseBucketKey = courseKey(group);
                if (!organizationByKey.has(organizationBucketKey)) {
                    organizationByKey.set(organizationBucketKey, {
                        key: organizationBucketKey,
                        label: group.row.dataset.groupOrganization || 'Unknown organization',
                        units: [],
                        unitByKey: new Map(),
                        courseCount: 0,
                        subjectCount: 0
                    });
                    organizations.push(organizationByKey.get(organizationBucketKey));
                }

                var organizationBucket = organizationByKey.get(organizationBucketKey);
                if (!organizationBucket.unitByKey.has(unitBucketKey)) {
                    organizationBucket.unitByKey.set(unitBucketKey, {
                        key: unitBucketKey,
                        label: group.row.dataset.groupOrganicUnit || 'No organic unit',
                        courses: [],
                        courseByKey: new Map(),
                        subjectCount: 0
                    });
                    organizationBucket.units.push(organizationBucket.unitByKey.get(unitBucketKey));
                }

                var unitBucket = organizationBucket.unitByKey.get(unitBucketKey);
                if (!unitBucket.courseByKey.has(courseBucketKey)) {
                    unitBucket.courseByKey.set(courseBucketKey, {
                        key: courseBucketKey,
                        label: group.row.dataset.groupCourse || 'No course',
                        context: '',
                        items: []
                    });
                    unitBucket.courses.push(unitBucket.courseByKey.get(courseBucketKey));
                    organizationBucket.courseCount += 1;
                }

                unitBucket.courseByKey.get(courseBucketKey).items.push(group);
                unitBucket.subjectCount += 1;
                organizationBucket.subjectCount += 1;
            });

            organizations.sort(function (first, second) {
                return compareGroupLabels(activeGroupDirection, first, second);
            });

            organizations.forEach(function (organizationBucket) {
                organizationBucket.units.sort(function (first, second) {
                    return compareGroupLabels(activeGroupDirection, first, second);
                });
                list.appendChild(createSubjectOrganizationGroupHeader(organizationBucket));
                if (collapsedOrganizationGroups.has(organizationBucket.key)) {
                    return;
                }
                organizationBucket.units.forEach(function (unitBucket) {
                    unitBucket.courses.sort(function (first, second) {
                        return compareGroupLabels(activeGroupDirection, first, second);
                    });
                    list.appendChild(createSubjectUnitGroupHeader(unitBucket));
                    if (collapsedUnitGroups.has(unitBucket.key)) {
                        return;
                    }
                    unitBucket.courses.forEach(function (courseBucket) {
                        list.appendChild(createSubjectCourseGroupHeader(courseBucket, true));
                        if (collapsedCourseGroups.has(courseBucket.key)) {
                            return;
                        }
                        courseBucket.items.forEach(appendGroup);
                    });
                });
            });
        }

        function renderCourseGroups(sorted) {
            var courses = [];
            var courseByKey = new Map();
            sorted.forEach(function (group) {
                var bucketKey = standaloneCourseKey(group);
                if (!courseByKey.has(bucketKey)) {
                    courseByKey.set(bucketKey, {
                        key: bucketKey,
                        label: group.row.dataset.groupCourse || 'No course',
                        context: group.row.dataset.groupOrganicUnit
                            ? group.row.dataset.groupOrganicUnit + ' | ' + (group.row.dataset.groupOrganization || 'Unknown organization')
                            : group.row.dataset.groupOrganization || '',
                        items: []
                    });
                    courses.push(courseByKey.get(bucketKey));
                }
                courseByKey.get(bucketKey).items.push(group);
            });

            courses.sort(function (first, second) {
                return compareGroupLabels(activeGroupDirection, first, second);
            });

            courses.forEach(function (courseBucket) {
                list.appendChild(createSubjectCourseGroupHeader(courseBucket, false));
                if (collapsedCourseGroups.has(courseBucket.key)) {
                    return;
                }
                courseBucket.items.forEach(appendGroup);
            });
        }

        function renderRows() {
            refreshRowGroups();
            var sorted = sortedRowGroups();
            removeSubjectGroupHeaders();
            detachRowGroups();
            updateGroupedLayout();
            if (activeGroupField === 'organization') {
                renderOrganizationGroups(sorted);
                return;
            }
            if (activeGroupField === 'course') {
                renderCourseGroups(sorted);
                return;
            }
            sorted.forEach(function (group) {
                appendGroup(group);
            });
        }

        function subjectRowsUrl(offset, loadAll) {
            var target = new URL(window.location.href);
            target.searchParams.set('fragment', 'rows');
            target.searchParams.set('offset', String(offset));
            if (loadAll) {
                target.searchParams.set('loadAll', 'true');
            } else {
                target.searchParams.delete('loadAll');
            }
            return target.toString();
        }

        function paginationItems(totalPages) {
            if (totalPages <= 5) {
                return Array.from({ length: totalPages }, function (_, index) {
                    return index + 1;
                });
            }
            if (currentPage <= 3) {
                return [1, 2, 3, 'ellipsis', totalPages];
            }
            if (currentPage >= totalPages - 2) {
                return [1, 'ellipsis', totalPages - 2, totalPages - 1, totalPages];
            }
            return [1, 'ellipsis', currentPage - 1, currentPage, currentPage + 1, 'ellipsis', totalPages];
        }

        function requestPage(page, loadAll) {
            if (isLoadingPage || page < 1) {
                return;
            }
            isLoadingPage = true;
            list.setAttribute('aria-busy', 'true');
            renderPagination();
            fetch(subjectRowsUrl((page - 1) * Number(pagination.dataset.pageSize || '10'), loadAll), {
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (response) {
                if (!response.ok) {
                    throw new Error('Failed to load subjects.');
                }
                return response.text();
            }).then(function (html) {
                var holder = document.createElement('tbody');
                holder.innerHTML = html;
                var metadata = holder.querySelector('[data-subject-load-more-meta]');
                if (!metadata) {
                    throw new Error('Invalid subject list response.');
                }
                currentPage = Number(metadata.dataset.currentPage) || page;
                showingAll = metadata.dataset.loadAll === 'true';
                metadata.remove();
                list.replaceChildren();
                while (holder.firstChild) {
                    list.appendChild(holder.firstChild);
                }
                renderRows();
            }).catch(function () {
                // Keep the current rows visible when the request fails.
            }).finally(function () {
                isLoadingPage = false;
                list.removeAttribute('aria-busy');
                renderPagination();
            });
        }

        function paginationButton(page) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'gape-list-pagination-button';
            button.textContent = String(page);
            var active = !showingAll && page === currentPage;
            button.classList.toggle('is-active', active);
            button.disabled = isLoadingPage;
            if (active) {
                button.setAttribute('aria-current', 'page');
            }
            button.addEventListener('click', function () {
                requestPage(page, false);
            });
            return button;
        }

        function paginationArrow(page, icon, label, disabled) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'gape-list-pagination-button gape-list-pagination-arrow';
            button.disabled = disabled || isLoadingPage;
            button.setAttribute('aria-label', label);
            button.innerHTML = '<i class="ph ' + icon + '" aria-hidden="true"></i>';
            button.addEventListener('click', function () {
                requestPage(page, false);
            });
            return button;
        }

        function renderPagination() {
            if (!pagination || !paginationPages) {
                return;
            }
            var totalPages = Math.ceil(Number(pagination.dataset.total || '0') / Number(pagination.dataset.pageSize || '10'));
            paginationPages.replaceChildren();
            paginationPages.appendChild(paginationArrow(currentPage - 1, 'ph-caret-left', 'Previous page', showingAll || currentPage <= 1));
            paginationItems(totalPages).forEach(function (item) {
                if (item === 'ellipsis') {
                    var ellipsis = document.createElement('span');
                    ellipsis.className = 'gape-list-pagination-ellipsis';
                    ellipsis.textContent = '…';
                    paginationPages.appendChild(ellipsis);
                    return;
                }
                paginationPages.appendChild(paginationButton(item));
            });
            paginationPages.appendChild(paginationArrow(currentPage + 1, 'ph-caret-right', 'Next page', showingAll || currentPage >= totalPages));
            if (loadAllButton) {
                loadAllButton.disabled = isLoadingPage || showingAll;
                loadAllButton.classList.toggle('is-active', showingAll);
                loadAllButton.textContent = isLoadingPage ? 'Loading...' : 'Load All';
            }
        }

        if (loadAllButton) {
            loadAllButton.addEventListener('click', function () {
                requestPage(1, true);
            });
        }

        function updateOptionStates() {
            sortOptions.forEach(function (option) {
                var field = option.dataset.sortField;
                var normalDirection = option.dataset.sortNormal;
                var state = 'none';
                if (activeField === field) {
                    state = activeDirection === normalDirection ? 'normal' : 'reverse';
                }

                option.dataset.sortState = state;
                option.classList.toggle('is-active', state !== 'none');
                option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            if (sortToggle) {
                sortToggle.classList.toggle('is-active', activeField !== null);
            }
        }

        function updateGroupOptionStates() {
            groupOptions.forEach(function (option) {
                var field = option.dataset.groupField;
                var normalDirection = option.dataset.groupNormal;
                var state = 'none';
                if (activeGroupField === field) {
                    state = activeGroupDirection === normalDirection ? 'normal' : 'reverse';
                }

                option.dataset.groupState = state;
                option.classList.toggle('is-active', state !== 'none');
                option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            if (groupToggle) {
                groupToggle.classList.toggle('is-active', activeGroupField !== null);
            }
        }

        function closeDropdown(toggle) {
            if (!toggle) {
                return;
            }
            if (window.bootstrap && window.bootstrap.Dropdown) {
                window.bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
            }
        }

        if (sortToggle) {
            sortToggle.addEventListener('click', function (event) {
                if (!activeField) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                activeField = null;
                activeDirection = null;
                updateOptionStates();
                renderRows();
                closeDropdown(sortToggle);
            });
        }

        if (groupToggle) {
            groupToggle.addEventListener('click', function (event) {
                if (!activeGroupField) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                activeGroupField = null;
                activeGroupDirection = null;
                clearCollapsedGroups();
                updateGroupOptionStates();
                renderRows();
                closeDropdown(groupToggle);
            });
        }

        sortOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.sortField;
                var normalDirection = option.dataset.sortNormal;
                var currentState = option.dataset.sortState;

                if (activeField !== field || currentState === 'none') {
                    activeField = field;
                    activeDirection = normalDirection;
                } else if (currentState === 'normal') {
                    activeDirection = oppositeDirection(normalDirection);
                } else {
                    activeField = null;
                    activeDirection = null;
                }

                updateOptionStates();
                renderRows();
            });
        });

        groupOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.groupField;
                var normalDirection = option.dataset.groupNormal;
                var currentState = option.dataset.groupState;

                if (activeGroupField !== field || currentState === 'none') {
                    if (activeGroupField !== field) {
                        clearCollapsedGroups();
                    }
                    activeGroupField = field;
                    activeGroupDirection = normalDirection;
                } else if (currentState === 'normal') {
                    activeGroupDirection = oppositeDirection(normalDirection);
                } else {
                    activeGroupField = null;
                    activeGroupDirection = null;
                    clearCollapsedGroups();
                }

                updateGroupOptionStates();
                renderRows();
            });
        });

        updateOptionStates();
        updateGroupOptionStates();
        updateGroupedLayout();
        renderPagination();
        }

        initializeSubjectList();
    })();
</script>
</body>
</html>
