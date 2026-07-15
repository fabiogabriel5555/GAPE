<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "class-groups");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Class Groups</title>
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
            border-radius: 8px !important;
            color: #fff !important;
            font-weight: 600;
            line-height: 1.2;
            min-height: 42px;
            padding: 10px 16px !important;
            transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
        }

        .gape-action-delete:hover,
        .gape-action-delete:focus-visible {
            background-color: #b91c1c !important;
            border-color: #b91c1c !important;
            color: #fff !important;
            transform: translateY(-1px);
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

        .gape-class-group-actions {
            min-width: 0;
        }

        .gape-class-group-management-panel,
        .gape-class-group-table-scroll {
            min-width: 0;
        }

        .gape-class-group-structure-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-top: 20px;
        }

        .gape-class-group-structure-list-header,
        .gape-class-group-structure-row {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: minmax(260px, 1.45fr) minmax(180px, .95fr) minmax(105px, .45fr) minmax(115px, .5fr) minmax(110px, .45fr);
        }

        .gape-class-group-structure-list-header {
            border-bottom: 1px solid var(--neutral-30);
            color: #475569;
            font-size: 13px;
            font-weight: 600;
            padding: 16px 16px 12px;
        }

        .gape-class-group-occurrence-group {
            border-bottom: 1px solid var(--neutral-30);
            padding: 0 16px 12px;
        }

        .gape-completed-class-groups-divider {
            align-items: center;
            color: #b91c1c;
            display: flex;
            font-size: 12px;
            font-weight: 600;
            gap: 12px;
            letter-spacing: .02em;
            margin: 6px 0 0;
            text-transform: uppercase;
        }

        .gape-completed-class-groups-divider::before,
        .gape-completed-class-groups-divider::after {
            background: #fecaca;
            content: "";
            flex: 1;
            height: 1px;
        }

        .gape-completed-class-groups-node {
            border-color: #fecaca !important;
        }

        .gape-completed-class-groups-panel {
            border-top: 1px solid #fecaca;
            margin: 16px -18px 0;
        }

        .gape-completed-class-groups-content {
            padding: 16px 16px 0 36px;
        }

        .gape-class-group-occurrence-heading {
            padding-block: 4px 12px;
        }

        .gape-class-group-occurrence-icon {
            flex-shrink: 0;
        }

        .gape-class-group-event-count-anchor {
            display: inline-flex;
            flex: 0 0 auto;
            position: relative;
        }

        .gape-class-group-event-count-badge {
            align-items: center;
            background: #dc2626;
            border: 2px solid #fff;
            border-radius: 999px;
            color: #fff;
            display: inline-flex;
            font-size: 10px;
            font-weight: 700;
            justify-content: center;
            left: -8px;
            line-height: 1;
            min-height: 22px;
            min-width: 22px;
            padding: 3px 6px;
            position: absolute;
            top: -8px;
            z-index: 2;
        }

        .gape-class-group-occurrence-children {
            border-top: 1px solid var(--neutral-30);
            margin-top: 8px;
            padding-top: 12px;
        }

        .gape-class-group-occurrence-content {
            display: flex;
            flex-direction: column;
            gap: 12px;
            padding: 0 0 4px 20px;
        }

        .gape-class-group-node {
            transition: border-color .2s ease, box-shadow .2s ease;
        }

        .gape-class-group-node:hover {
            border-color: rgba(37, 99, 235, .28) !important;
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
        }

        .gape-class-group-table {
            min-width: 960px;
        }

        .gape-class-group-table.is-class-group-grouped {
            min-width: 860px;
        }

        .gape-class-group-table.is-class-group-grouped [data-class-group-context-column] {
            display: none;
        }

        .gape-class-group-table:not(.is-class-group-grouped) [data-class-group-row] > td:first-child {
            padding-inline-start: 48px !important;
        }

        .gape-class-group-table.is-class-group-grouped-by-organization [data-class-group-row] > td:first-child {
            padding-inline-start: 76px !important;
        }

        .gape-class-group-table.is-class-group-grouped-by-course [data-class-group-row] > td:first-child {
            padding-inline-start: 64px !important;
        }

        .gape-class-group-table.is-class-group-grouped-by-subject [data-class-group-row] > td:first-child {
            padding-inline-start: 32px !important;
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

        .gape-class-group-organization-group-row td {
            background: #dcecff;
            border-bottom: 1px solid #b7d4f5;
            border-top: 18px solid #fff;
            padding-block: 18px !important;
        }

        .gape-class-group-organization-group-row:first-child td {
            border-top-width: 0;
        }

        .gape-class-group-unit-group-row td {
            background: #f5f9ff;
            border-bottom: 1px solid #d5e4f6;
            border-top: 8px solid #fff;
            padding-block: 12px !important;
        }

        .gape-class-group-course-group-row td {
            background: #fff;
            border-bottom: 1px solid #e2e8f0;
            border-top: 6px solid #f8fbff;
            padding-block: 10px !important;
        }

        .gape-class-group-subject-group-row td {
            background: #fbfdff;
            border-bottom: 1px solid #e2e8f0;
            border-top: 4px solid #f8fbff;
            padding-block: 10px !important;
        }

        .gape-class-group-heading,
        .gape-class-group-unit-heading,
        .gape-class-group-course-heading,
        .gape-class-group-subject-heading {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: space-between;
        }

        .gape-class-group-title,
        .gape-class-group-unit-title,
        .gape-class-group-course-title,
        .gape-class-group-subject-title {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .gape-class-group-unit-chip {
            background: #fff;
            border: 1px solid #cddff4;
            border-radius: 999px;
            color: #475569;
            display: inline-flex;
            padding: 4px 10px;
        }

        .gape-class-group-toggle {
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

        .gape-class-group-toggle:hover,
        .gape-class-group-toggle:focus-visible {
            background-color: #eef4fb;
            border-color: #d5e4f6;
            color: #334155;
        }

        .gape-class-group-unit-heading {
            padding-inline-start: 0;
        }

        .gape-class-group-course-heading.is-nested-under-organization {
            padding-inline-start: 20px;
        }

        .gape-class-group-subject-heading.is-nested-under-organization {
            padding-inline-start: 40px;
        }

        .gape-class-group-subject-heading.is-nested-under-course {
            padding-inline-start: 28px;
        }

        .gape-class-group-unit-marker,
        .gape-class-group-course-marker,
        .gape-class-group-subject-marker {
            border-radius: 999px;
            display: inline-flex;
            height: 8px;
            width: 8px;
        }

        .gape-class-group-unit-marker {
            background: var(--main-600);
        }

        .gape-class-group-course-marker {
            background: #16a34a;
        }

        .gape-class-group-subject-marker {
            background: #7c3aed;
        }

        @media (max-width: 575.98px) {
            .gape-class-activities-panel {
                margin-inline-start: 0;
            }

            .gape-class-activities-panel::before {
                display: none;
            }

            .gape-class-group-actions {
                width: 100%;
            }

            .gape-class-group-management-panel {
                max-width: calc(100vw - 48px);
                overflow: hidden;
            }

            .gape-class-group-table-scroll {
                max-width: 100%;
            }

            .gape-class-group-actions > .dropdown {
                flex: 1 1 calc(50% - 6px);
                min-width: 0;
            }

            .gape-class-group-actions > .dropdown > button,
            .gape-class-group-actions > a {
                justify-content: center;
                text-align: center;
                white-space: nowrap;
                width: 100%;
            }

            .gape-class-group-actions > a {
                flex: 1 1 100%;
            }

            .gape-class-group-structure-list-header {
                display: none;
            }

            .gape-class-group-structure-list {
                margin-top: 0;
            }

            .gape-class-group-structure-row {
                grid-template-columns: 1fr;
            }

            .gape-class-group-occurrence-content {
                padding-left: 0;
            }

            .gape-completed-class-groups-content {
                padding-left: 16px;
            }

            .gape-class-group-table.is-class-group-grouped-by-organization [data-class-group-row] > td:first-child {
                padding-inline-start: 36px !important;
            }

            .gape-class-group-table.is-class-group-grouped-by-course [data-class-group-row] > td:first-child {
                padding-inline-start: 40px !important;
            }

            .gape-class-group-table.is-class-group-grouped-by-subject [data-class-group-row] > td:first-child {
                padding-inline-start: 28px !important;
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
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${classGroupCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeClassGroups}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Completed</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${completedClassGroups}</h2>
                        </div>
                    </div>
                </div>

                <div class="gape-class-group-management-panel bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Class Group Management</h2>
                            <span class="text-14 text-neutral-500">Class groups grouped by course occurrence.</span>
                        </div>
                        <div class="gape-class-group-actions d-flex align-items-center gap-12 flex-wrap">
                            <div class="dropdown">
                                <button type="button"
                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="outside"
                                        data-class-group-sort-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-sort-ascending"></i>Sort by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-class-group-sort-option
                                                data-sort-field="code"
                                                data-sort-normal="asc"
                                                data-sort-state="none"
                                                aria-pressed="false">
                                            <span>Code</span>
                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-class-group-sort-option
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
                                                data-class-group-sort-option
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
                                        data-class-group-group-toggle
                                        aria-expanded="false">
                                    <i class="ph ph-stack"></i>Group by
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-class-group-group-option
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
                                                data-class-group-group-option
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
                                    <li>
                                        <button type="button"
                                                class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10"
                                                data-class-group-group-option
                                                data-group-field="subject"
                                                data-group-normal="asc"
                                                data-group-state="none"
                                                aria-pressed="false">
                                            <span>Subjects</span>
                                            <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true">
                                                <i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i>
                                                <i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i>
                                            </span>
                                        </button>
                                    </li>
                                </ul>
                            </div>
                            <c:if test="${canCreateClassGroups}">
                                <a href="${pageContext.request.contextPath}/learning/class-groups/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Class Group
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="gape-class-group-table-scroll overflow-x-auto">
                        <table hidden class="table mb-0 gape-class-group-table">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Class Group</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-class-group-context-column>Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Blocks</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-class-group-legacy-list>
                            <c:if test="${false}">
                            <c:forEach var="occurrenceGroup" items="${classGroupOccurrenceGroups}">
                                <tr class="bg-neutral-20 border-bottom" data-class-group-group-row data-class-group-occurrence-row>
                                    <td colspan="6" class="py-14 px-20">
                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                            <div>
                                                <span class="fw-semibold text-14 text-neutral-800"><c:out value="${occurrenceGroup.occurrenceLabel}"/></span>
                                                <span class="d-block text-12 text-neutral-500" data-gape-datetime-display><c:out value="${occurrenceGroup.occurrenceDateRangeLabel}"/> &middot; ${occurrenceGroup.classGroupCount} class groups</span>
                                            </div>
                                            <span class="${occurrenceGroup.occurrenceStateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                <c:out value="${occurrenceGroup.occurrenceStateLabel}"/>
                                            </span>
                                        </div>
                                    </td>
                                </tr>
                            <c:forEach var="classGroup" items="${occurrenceGroup.classGroups}" varStatus="classGroupLoop">
                                <c:set var="canModifyClassGroupRow" value="${canModifyClassGroupById[classGroup.id]}" />
                                <c:set var="canManageClassGroupStructureRow" value="${canManageClassGroupStructureById[classGroup.id]}" />
                                <c:set var="classGroupLessons" value="${classGroupLessonsByClassGroup[classGroup.id]}" />
                                <c:set var="classGroupRooms" value="${classGroupRoomsByClassGroup[classGroup.id]}" />
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-class-group-row
                                    data-class-group-detail-id="classGroupStructure${classGroup.id}"
                                    data-sort-index="${classGroupLoop.index}"
                                    data-sort-code="${fn:escapeXml(classGroup.code)}"
                                    data-sort-date="${classGroup.id}"
                                    data-sort-status="${fn:escapeXml(classGroup.stateLabel)}"
                                    data-group-organization-id="${classGroup.course.organizationId}"
                                    data-group-organization="${fn:escapeXml(classGroup.course.organizationName)}"
                                    data-group-organic-unit-id="${classGroup.course.organicUnitId}"
                                    data-group-organic-unit="${fn:escapeXml(classGroup.course.organicUnitLabel)}"
                                    data-group-course-id="${classGroup.courseId}"
                                    data-group-course="${fn:escapeXml(classGroup.courseName)}"
                                    data-group-subject-id="${classGroup.subjectId}"
                                    data-group-subject="${fn:escapeXml(classGroup.subjectName)}"
                                    data-group-occurrence-id="${classGroup.courseOccurrenceId}"
                                    data-group-occurrence="${fn:escapeXml(classGroup.occurrenceLabel)}"
                                    data-group-occurrence-range="${fn:escapeXml(classGroup.occurrenceDateRangeLabel)}"
                                    data-group-occurrence-state="${fn:escapeXml(classGroup.occurrenceStateLabel)}"
                                    data-group-occurrence-state-class="${fn:escapeXml(classGroup.occurrenceStateBadgeClass)}">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${classGroup.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/> | <c:out value="${classGroup.shift}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${classGroup.contextTitle}'/>" data-class-group-context-column><c:out value="${classGroup.contextHtml}" escapeXml="false"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${classGroup.activeEnrollmentCount}"/> enrolled
                                        <span class="d-block text-12 text-neutral-500">Min/Max: <c:out value="${classGroup.capacityLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.blockCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${classGroup.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${classGroup.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show Activities"
                                                    aria-label="Show Activities"
                                                    aria-expanded="false"
                                                    aria-controls="classGroupStructure${classGroup.id}"
                                                    data-gape-tree-toggle="classGroupStructure${classGroup.id}"
                                                    data-gape-open-title="Hide Activities"
                                                    data-gape-closed-title="Show Activities">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canModifyClassGroupRow}">
                                                <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${canManageClassGroupStructureRow}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteClassGroup${classGroup.id}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <c:if test="${canManageClassGroupStructureRow}">
                                            <div class="modal fade" id="deleteClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true">
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
                                    </td>
                                </tr>
                                <tr id="classGroupStructure${classGroup.id}" class="d-none" data-class-group-detail-row>
                                    <td colspan="6" class="py-0 px-20 bg-white">
                                        <c:set var="canModifyClassGroup" value="${canModifyClassGroupRow}" />
                                        <%@ include file="/WEB-INF/fragments/class-group-activities-panel.jspf" %>
                                    </td>
                                </tr>
                            </c:forEach>
                            </c:forEach>
                            <c:if test="${empty classGroups}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No class groups available in your context.</td>
                                </tr>
                            </c:if>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <div class="gape-class-group-structure-list-wrapper">
                        <div class="gape-class-group-structure-list-header">
                            <span>Course Occurrence</span>
                            <span>Context</span>
                            <span>No. Blocks</span>
                            <span>State</span>
                            <span class="text-end">Actions</span>
                        </div>
                        <div class="gape-class-group-structure-list" data-class-group-list aria-live="polite">
                            <%@ include file="/WEB-INF/fragments/class-group-list-rows.jsp" %>
                        </div>
                    </div>
                    <c:if test="${classGroupCount > 10}">
                        <div class="d-flex justify-content-end mt-20" data-class-group-pagination data-total="${classGroupCount}" data-page-size="10">
                            <div class="gape-list-pagination">
                                <nav class="gape-list-pagination-pages" aria-label="Class group pages" data-class-group-pagination-pages></nav>
                                <button type="button" class="gape-list-load-all" data-class-group-load-all>Load All</button>
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
<script src="${pageContext.request.contextPath}/assets/js/gape-class-group-list.js?v=20260715-class-group-structure-4"></script>
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

    (function () {
        if (window.GapeClassGroupList) {
            window.GapeClassGroupList.initialize();
            return;
        }
        var list = document.querySelector('[data-class-group-list]');
        if (!list) {
            return;
        }

        var table = list.closest('table');
        var rows = Array.prototype.slice.call(list.querySelectorAll('[data-class-group-row]'));
        var contextColumns = table ? Array.prototype.slice.call(table.querySelectorAll('[data-class-group-context-column]')) : [];
        var sortOptions = Array.prototype.slice.call(document.querySelectorAll('[data-class-group-sort-option]'));
        var groupOptions = Array.prototype.slice.call(document.querySelectorAll('[data-class-group-group-option]'));
        var sortToggle = document.querySelector('[data-class-group-sort-toggle]');
        var groupToggle = document.querySelector('[data-class-group-group-toggle]');
        var pagination = document.querySelector('[data-class-group-pagination]');
        var paginationPages = document.querySelector('[data-class-group-pagination-pages]');
        var loadAllButton = document.querySelector('[data-class-group-load-all]');
        var initialLoadMoreMeta = list.querySelector('[data-class-group-load-more-meta]');
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
            rowGroups = Array.prototype.slice.call(list.querySelectorAll('[data-class-group-row]')).map(function (row) {
                return { row: row, detail: document.getElementById(row.dataset.classGroupDetailId) };
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
        var collapsedSubjectGroups = new Set();
        var collapsedOccurrenceGroups = new Set();
        var completedClassGroupsCollapsed = true;

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
            if (field === 'date') {
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

        function subjectKey(group) {
            return [
                courseKey(group),
                group.row.dataset.groupSubjectId || 'none'
            ].join('::');
        }

        function occurrenceKey(group) {
            return [
                group.row.dataset.groupOccurrenceId || 'none',
                group.row.dataset.groupSubjectId || 'none',
                group.row.dataset.groupOccurrence || ''
            ].join('::');
        }

        function standaloneCourseKey(group) {
            return [
                group.row.dataset.groupCourseId || 'none',
                group.row.dataset.groupCourse || ''
            ].join('::');
        }

        function courseSubjectKey(group) {
            return [
                standaloneCourseKey(group),
                group.row.dataset.groupSubjectId || 'none'
            ].join('::');
        }

        function standaloneSubjectKey(group) {
            return [
                group.row.dataset.groupSubjectId || 'none',
                group.row.dataset.groupSubject || ''
            ].join('::');
        }

        function courseContextLabel(group) {
            var unit = group.row.dataset.groupOrganicUnit || '';
            var organization = group.row.dataset.groupOrganization || '';
            if (unit && organization) {
                return unit + ' | ' + organization;
            }
            return organization;
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
            collapsedSubjectGroups.clear();
            collapsedOccurrenceGroups.clear();
            completedClassGroupsCollapsed = true;
        }

        function visibleColumnCount() {
            return activeGroupField ? 5 : 6;
        }

        function updateGroupedLayout() {
            var grouped = activeGroupField !== null;
            if (table) {
                table.classList.toggle('is-class-group-grouped', grouped);
                table.classList.toggle('is-class-group-grouped-by-organization', activeGroupField === 'organization');
                table.classList.toggle('is-class-group-grouped-by-course', activeGroupField === 'course');
                table.classList.toggle('is-class-group-grouped-by-subject', activeGroupField === 'subject');
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
            button.className = 'gape-class-group-toggle text-18';
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

        function createClassGroupOrganizationHeader(bucket) {
            var header = document.createElement('tr');
            header.className = 'gape-class-group-organization-group-row';
            header.setAttribute('data-class-group-group-row', '');
            header.setAttribute('data-class-group-organization-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-14';

            var heading = document.createElement('div');
            heading.className = 'gape-class-group-heading';

            var title = document.createElement('div');
            title.className = 'gape-class-group-title';

            var icon = document.createElement('i');
            icon.className = 'ph ph-buildings text-20 text-main-600';
            icon.setAttribute('aria-hidden', 'true');

            var organization = document.createElement('span');
            organization.className = 'text-14 fw-semibold text-neutral-700';
            organization.textContent = bucket.label;

            var unitCount = document.createElement('span');
            unitCount.className = 'gape-class-group-unit-chip text-12 fw-medium';
            unitCount.textContent = countLabel(bucket.units.length, 'department', 'departments');

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.classGroupCount, 'class group', 'class groups');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedOrganizationGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide organization class groups', 'Show organization class groups', function () {
                if (collapsedOrganizationGroups.has(bucket.key)) {
                    collapsedOrganizationGroups.delete(bucket.key);
                } else {
                    collapsedOrganizationGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-class-group-organization-visibility-toggle', bucket.key);

            title.appendChild(icon);
            title.appendChild(organization);
            title.appendChild(unitCount);
            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createClassGroupUnitHeader(bucket) {
            var header = document.createElement('tr');
            header.className = 'gape-class-group-unit-group-row';
            header.setAttribute('data-class-group-group-row', '');
            header.setAttribute('data-class-group-unit-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-12';

            var heading = document.createElement('div');
            heading.className = 'gape-class-group-unit-heading';

            var title = document.createElement('div');
            title.className = 'gape-class-group-unit-title';

            var marker = document.createElement('span');
            marker.className = 'gape-class-group-unit-marker';
            marker.setAttribute('aria-hidden', 'true');

            var unit = document.createElement('span');
            unit.className = 'text-13 fw-semibold text-neutral-700';
            unit.textContent = bucket.label;

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.classGroupCount, 'class group', 'class groups');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedUnitGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide organic unit class groups', 'Show organic unit class groups', function () {
                if (collapsedUnitGroups.has(bucket.key)) {
                    collapsedUnitGroups.delete(bucket.key);
                } else {
                    collapsedUnitGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-class-group-unit-visibility-toggle', bucket.key);

            title.appendChild(marker);
            title.appendChild(unit);
            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createClassGroupCourseHeader(bucket, nestedMode) {
            var header = document.createElement('tr');
            header.className = 'gape-class-group-course-group-row';
            header.setAttribute('data-class-group-group-row', '');
            header.setAttribute('data-class-group-course-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-10';

            var heading = document.createElement('div');
            heading.className = 'gape-class-group-course-heading';
            if (nestedMode === 'organization') {
                heading.classList.add('is-nested-under-organization');
            }

            var title = document.createElement('div');
            title.className = 'gape-class-group-course-title';

            var marker = document.createElement('span');
            marker.className = 'gape-class-group-course-marker';
            marker.setAttribute('aria-hidden', 'true');

            var course = document.createElement('span');
            course.className = 'text-13 fw-semibold text-neutral-700';
            course.textContent = bucket.label;

            title.appendChild(marker);
            title.appendChild(course);
            if (bucket.context) {
                var context = document.createElement('span');
                context.className = 'text-12 text-neutral-500';
                context.textContent = bucket.context;
                title.appendChild(context);
            }

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.classGroupCount, 'class group', 'class groups');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedCourseGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide course class groups', 'Show course class groups', function () {
                if (collapsedCourseGroups.has(bucket.key)) {
                    collapsedCourseGroups.delete(bucket.key);
                } else {
                    collapsedCourseGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-class-group-course-visibility-toggle', bucket.key);

            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createClassGroupSubjectHeader(bucket, nestedMode) {
            var header = document.createElement('tr');
            header.className = 'gape-class-group-subject-group-row';
            header.setAttribute('data-class-group-group-row', '');
            header.setAttribute('data-class-group-subject-group-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-10';

            var heading = document.createElement('div');
            heading.className = 'gape-class-group-subject-heading';
            if (nestedMode === 'organization') {
                heading.classList.add('is-nested-under-organization');
            } else if (nestedMode === 'course') {
                heading.classList.add('is-nested-under-course');
            }

            var title = document.createElement('div');
            title.className = 'gape-class-group-subject-title';

            var marker = document.createElement('span');
            marker.className = 'gape-class-group-subject-marker';
            marker.setAttribute('aria-hidden', 'true');

            var subject = document.createElement('span');
            subject.className = 'text-13 fw-semibold text-neutral-700';
            subject.textContent = bucket.label;

            title.appendChild(marker);
            title.appendChild(subject);
            if (bucket.courseCount && bucket.courseCount > 1) {
                var courseCount = document.createElement('span');
                courseCount.className = 'gape-class-group-unit-chip text-12 fw-medium';
                courseCount.textContent = countLabel(bucket.courseCount, 'course', 'courses');
                title.appendChild(courseCount);
            }

            var count = document.createElement('span');
            count.className = 'text-12 text-neutral-500';
            count.textContent = countLabel(bucket.items.length, 'class group', 'class groups');

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';

            var isExpanded = !collapsedSubjectGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide subject class groups', 'Show subject class groups', function () {
                if (collapsedSubjectGroups.has(bucket.key)) {
                    collapsedSubjectGroups.delete(bucket.key);
                } else {
                    collapsedSubjectGroups.add(bucket.key);
                }
            });
            toggle.setAttribute('data-class-group-subject-visibility-toggle', bucket.key);

            actions.appendChild(count);
            actions.appendChild(toggle);
            heading.appendChild(title);
            heading.appendChild(actions);
            cell.appendChild(heading);
            header.appendChild(cell);
            return header;
        }

        function createClassGroupOccurrenceHeader(bucket) {
            var header = document.createElement('tr');
            header.className = 'bg-neutral-20 border-bottom';
            header.setAttribute('data-class-group-group-row', '');
            header.setAttribute('data-class-group-occurrence-row', '');

            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'py-14 px-20';

            var wrapper = document.createElement('div');
            wrapper.className = 'd-flex align-items-center justify-content-between gap-12 flex-wrap';

            var title = document.createElement('div');
            var label = document.createElement('span');
            label.className = 'fw-semibold text-14 text-neutral-800';
            label.textContent = bucket.label;
            var meta = document.createElement('span');
            meta.className = 'd-block text-12 text-neutral-500';
            meta.textContent = bucket.range + ' \u00b7 ' + countLabel(bucket.items.length, 'class group', 'class groups');
            title.appendChild(label);
            title.appendChild(meta);

            var badge = document.createElement('span');
            badge.className = (bucket.stateClass || 'bg-neutral-30 text-neutral-600') + ' px-14 py-6 border-neutral-30 border rounded-pill text-13';
            badge.textContent = bucket.state || '-';

            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';
            var isExpanded = !collapsedOccurrenceGroups.has(bucket.key);
            var toggle = createGroupToggleButton(isExpanded, 'Hide class groups', 'Show class groups', function () {
                if (collapsedOccurrenceGroups.has(bucket.key)) {
                    collapsedOccurrenceGroups.delete(bucket.key);
                } else {
                    collapsedOccurrenceGroups.add(bucket.key);
                }
            });
            actions.appendChild(badge);
            actions.appendChild(toggle);
            wrapper.appendChild(title);
            wrapper.appendChild(actions);
            cell.appendChild(wrapper);
            header.appendChild(cell);
            return header;
        }

        function createCompletedClassGroupsHeader(count) {
            var header = document.createElement('tr');
            header.className = 'gape-completed-class-groups-node border-bottom';
            header.setAttribute('data-class-group-group-row', '');
            var cell = document.createElement('td');
            cell.colSpan = visibleColumnCount();
            cell.className = 'px-20 py-14 bg-white';
            var wrapper = document.createElement('div');
            wrapper.className = 'd-flex align-items-center justify-content-between gap-12';
            var title = document.createElement('div');
            title.className = 'd-flex align-items-center gap-12';
            title.innerHTML = '<span class="bg-danger-50 text-danger-600 rounded-circle p-8"><i class="ph ph-archive" aria-hidden="true"></i></span><span><span class="fw-semibold text-14 text-neutral-800 d-block">Completed Class Groups</span><span class="text-12 text-neutral-500">Past class groups</span></span>';
            var actions = document.createElement('div');
            actions.className = 'd-flex align-items-center gap-10';
            var countLabelNode = document.createElement('span');
            countLabelNode.className = 'text-12 text-neutral-500';
            countLabelNode.textContent = countLabel(count, 'class group', 'class groups');
            var toggle = createGroupToggleButton(!completedClassGroupsCollapsed, 'Hide completed class groups', 'Show completed class groups', function () {
                completedClassGroupsCollapsed = !completedClassGroupsCollapsed;
            });
            actions.appendChild(countLabelNode);
            actions.appendChild(toggle);
            wrapper.appendChild(title);
            wrapper.appendChild(actions);
            cell.appendChild(wrapper);
            header.appendChild(cell);
            return header;
        }

        function removeClassGroupHeaders() {
            Array.prototype.slice.call(list.querySelectorAll('[data-class-group-group-row]')).forEach(function (header) {
                header.remove();
            });
        }

        function detachRowGroups() {
            rowGroups.forEach(function (group) {
                group.row.remove();
                if (group.detail) {
                    group.detail.remove();
                }
            });
        }

        function appendGroup(group) {
            updateDetailColumnCount(group);
            list.appendChild(group.row);
            if (group.detail) {
                list.appendChild(group.detail);
            }
        }

        function renderOrganizationGroups(sorted) {
            var organizations = [];
            var organizationByKey = new Map();
            sorted.forEach(function (group) {
                var organizationBucketKey = organizationKey(group);
                var unitBucketKey = unitKey(group);
                var courseBucketKey = courseKey(group);
                var subjectBucketKey = subjectKey(group);
                if (!organizationByKey.has(organizationBucketKey)) {
                    organizationByKey.set(organizationBucketKey, {
                        key: organizationBucketKey,
                        label: group.row.dataset.groupOrganization || 'Unknown organization',
                        units: [],
                        unitByKey: new Map(),
                        classGroupCount: 0
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
                        classGroupCount: 0
                    });
                    organizationBucket.units.push(organizationBucket.unitByKey.get(unitBucketKey));
                }

                var unitBucket = organizationBucket.unitByKey.get(unitBucketKey);
                if (!unitBucket.courseByKey.has(courseBucketKey)) {
                    unitBucket.courseByKey.set(courseBucketKey, {
                        key: courseBucketKey,
                        label: group.row.dataset.groupCourse || 'No course',
                        subjects: [],
                        subjectByKey: new Map(),
                        classGroupCount: 0
                    });
                    unitBucket.courses.push(unitBucket.courseByKey.get(courseBucketKey));
                }

                var courseBucket = unitBucket.courseByKey.get(courseBucketKey);
                if (!courseBucket.subjectByKey.has(subjectBucketKey)) {
                    courseBucket.subjectByKey.set(subjectBucketKey, {
                        key: subjectBucketKey,
                        label: group.row.dataset.groupSubject || 'No subject',
                        items: []
                    });
                    courseBucket.subjects.push(courseBucket.subjectByKey.get(subjectBucketKey));
                }

                courseBucket.subjectByKey.get(subjectBucketKey).items.push(group);
                courseBucket.classGroupCount += 1;
                unitBucket.classGroupCount += 1;
                organizationBucket.classGroupCount += 1;
            });

            organizations.sort(function (first, second) {
                return compareGroupLabels(activeGroupDirection, first, second);
            });

            organizations.forEach(function (organizationBucket) {
                organizationBucket.units.sort(function (first, second) {
                    return compareGroupLabels(activeGroupDirection, first, second);
                });
                list.appendChild(createClassGroupOrganizationHeader(organizationBucket));
                if (collapsedOrganizationGroups.has(organizationBucket.key)) {
                    return;
                }
                organizationBucket.units.forEach(function (unitBucket) {
                    unitBucket.courses.sort(function (first, second) {
                        return compareGroupLabels(activeGroupDirection, first, second);
                    });
                    list.appendChild(createClassGroupUnitHeader(unitBucket));
                    if (collapsedUnitGroups.has(unitBucket.key)) {
                        return;
                    }
                    unitBucket.courses.forEach(function (courseBucket) {
                        courseBucket.subjects.sort(function (first, second) {
                            return compareGroupLabels(activeGroupDirection, first, second);
                        });
                        list.appendChild(createClassGroupCourseHeader(courseBucket, 'organization'));
                        if (collapsedCourseGroups.has(courseBucket.key)) {
                            return;
                        }
                        courseBucket.subjects.forEach(function (subjectBucket) {
                            list.appendChild(createClassGroupSubjectHeader(subjectBucket, 'organization'));
                            if (collapsedSubjectGroups.has(subjectBucket.key)) {
                                return;
                            }
                            subjectBucket.items.forEach(appendGroup);
                        });
                    });
                });
            });
        }

        function renderCourseGroups(sorted) {
            var courses = [];
            var courseByKey = new Map();
            sorted.forEach(function (group) {
                var bucketKey = standaloneCourseKey(group);
                var subjectBucketKey = courseSubjectKey(group);
                if (!courseByKey.has(bucketKey)) {
                    courseByKey.set(bucketKey, {
                        key: bucketKey,
                        label: group.row.dataset.groupCourse || 'No course',
                        context: courseContextLabel(group),
                        subjects: [],
                        subjectByKey: new Map(),
                        classGroupCount: 0
                    });
                    courses.push(courseByKey.get(bucketKey));
                }

                var courseBucket = courseByKey.get(bucketKey);
                if (!courseBucket.subjectByKey.has(subjectBucketKey)) {
                    courseBucket.subjectByKey.set(subjectBucketKey, {
                        key: subjectBucketKey,
                        label: group.row.dataset.groupSubject || 'No subject',
                        items: []
                    });
                    courseBucket.subjects.push(courseBucket.subjectByKey.get(subjectBucketKey));
                }
                courseBucket.subjectByKey.get(subjectBucketKey).items.push(group);
                courseBucket.classGroupCount += 1;
            });

            courses.sort(function (first, second) {
                return compareGroupLabels(activeGroupDirection, first, second);
            });

            courses.forEach(function (courseBucket) {
                courseBucket.subjects.sort(function (first, second) {
                    return compareGroupLabels(activeGroupDirection, first, second);
                });
                list.appendChild(createClassGroupCourseHeader(courseBucket, 'course'));
                if (collapsedCourseGroups.has(courseBucket.key)) {
                    return;
                }
                courseBucket.subjects.forEach(function (subjectBucket) {
                    list.appendChild(createClassGroupSubjectHeader(subjectBucket, 'course'));
                    if (collapsedSubjectGroups.has(subjectBucket.key)) {
                        return;
                    }
                    subjectBucket.items.forEach(appendGroup);
                });
            });
        }

        function renderSubjectGroups(sorted) {
            var subjects = [];
            var subjectByKey = new Map();
            sorted.forEach(function (group) {
                var bucketKey = standaloneSubjectKey(group);
                if (!subjectByKey.has(bucketKey)) {
                    subjectByKey.set(bucketKey, {
                        key: bucketKey,
                        label: group.row.dataset.groupSubject || 'No subject',
                        courseKeys: new Set(),
                        items: []
                    });
                    subjects.push(subjectByKey.get(bucketKey));
                }
                var subjectBucket = subjectByKey.get(bucketKey);
                subjectBucket.courseKeys.add(group.row.dataset.groupCourseId || 'none');
                subjectBucket.items.push(group);
            });

            subjects.forEach(function (subjectBucket) {
                subjectBucket.courseCount = subjectBucket.courseKeys.size;
            });
            subjects.sort(function (first, second) {
                return compareGroupLabels(activeGroupDirection, first, second);
            });

            subjects.forEach(function (subjectBucket) {
                list.appendChild(createClassGroupSubjectHeader(subjectBucket, 'subject'));
                if (collapsedSubjectGroups.has(subjectBucket.key)) {
                    return;
                }
                subjectBucket.items.forEach(appendGroup);
            });
        }

        function renderOccurrenceGroups(sorted) {
            function renderOccurrenceCollection(items) {
            var occurrences = [];
            var occurrenceByKey = new Map();
            items.forEach(function (group) {
                var bucketKey = occurrenceKey(group);
                if (!occurrenceByKey.has(bucketKey)) {
                    occurrenceByKey.set(bucketKey, {
                        key: bucketKey,
                        label: group.row.dataset.groupOccurrence || 'Occurrence',
                        range: group.row.dataset.groupOccurrenceRange || '-',
                        state: group.row.dataset.groupOccurrenceState || '-',
                        stateClass: group.row.dataset.groupOccurrenceStateClass || '',
                        items: []
                    });
                    occurrences.push(occurrenceByKey.get(bucketKey));
                }
                occurrenceByKey.get(bucketKey).items.push(group);
            });
            occurrences.forEach(function (bucket) {
                list.appendChild(createClassGroupOccurrenceHeader(bucket));
                if (!collapsedOccurrenceGroups.has(bucket.key)) {
                    bucket.items.forEach(appendGroup);
                }
            });
            }
            var active = sorted.filter(function (group) { return group.row.dataset.classGroupCompleted !== 'true'; });
            var completed = sorted.filter(function (group) { return group.row.dataset.classGroupCompleted === 'true'; });
            renderOccurrenceCollection(active);
            if (completed.length) {
                list.appendChild(createCompletedClassGroupsHeader(completed.length));
                if (!completedClassGroupsCollapsed) {
                    renderOccurrenceCollection(completed);
                }
            }
        }

        function renderRows() {
            refreshRowGroups();
            var sorted = sortedRowGroups();
            removeClassGroupHeaders();
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
            if (activeGroupField === 'subject') {
                renderSubjectGroups(sorted);
                return;
            }
            renderOccurrenceGroups(sorted);
        }

        function bindClassGroupActivityToggles() {
            Array.prototype.slice.call(list.querySelectorAll('[data-class-group-activity-toggle]')).forEach(function (button) {
                if (button.dataset.classGroupActivityBound === 'true') {
                    return;
                }
                button.dataset.classGroupActivityBound = 'true';
                button.addEventListener('click', function () {
                    var target = document.getElementById(button.getAttribute('aria-controls'));
                    if (!target) {
                        return;
                    }
                    var isHidden = target.classList.contains('d-none');
                    function setExpanded(expanded) {
                        target.classList.toggle('d-none', !expanded);
                        button.setAttribute('aria-expanded', String(expanded));
                        button.setAttribute('title', expanded ? 'Hide Activities' : 'Show Activities');
                        button.setAttribute('aria-label', expanded ? 'Hide Activities' : 'Show Activities');
                        var icon = button.querySelector('i');
                        if (icon) { icon.classList.toggle('ph-caret-up', expanded); icon.classList.toggle('ph-caret-down', !expanded); }
                    }
                    if (!isHidden) { setExpanded(false); return; }
                    var panel = target.querySelector('[data-class-group-activity-panel]');
                    if (target.dataset.classGroupActivityLoaded === 'true') { setExpanded(true); return; }
                    if (panel) { panel.textContent = 'Loading class activities…'; }
                    fetch(classGroupActivityUrl(button.dataset.classGroupActivityUrl), { credentials: 'same-origin', headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' } })
                        .then(function (response) { if (!response.ok) { throw new Error('Failed to load activities.'); } return response.text(); })
                        .then(function (html) { if (panel) { panel.outerHTML = html; } target.dataset.classGroupActivityLoaded = 'true'; setExpanded(true); })
                        .catch(function () { if (panel) { panel.textContent = 'Unable to load class activities. Please try again.'; } });
                });
            });
        }

        function classGroupRowsUrl(offset, loadAll) {
            var target = new URL(window.location.href);
            target.searchParams.set('fragment', 'rows');
            target.searchParams.set('offset', String(offset));
            if (loadAll) { target.searchParams.set('loadAll', 'true'); } else { target.searchParams.delete('loadAll'); }
            return target.toString();
        }

        /* The validation server can propagate authentication through a
           rewritten URL instead of a cookie.  Preserve it for row requests. */
        function classGroupActivityUrl(rawUrl) {
            var target = new URL(rawUrl, window.location.href);
            var match = window.location.pathname.match(/^([^;]+;jsessionid=[^/]+)/i);
            if (!match) { return target.toString(); }
            var sessionContext = match[1];
            var contextPath = sessionContext.substring(0, sessionContext.toLowerCase().indexOf(';jsessionid='));
            if (target.pathname === contextPath || target.pathname.indexOf(contextPath + '/') === 0) {
                target.pathname = sessionContext + target.pathname.substring(contextPath.length);
            }
            return target.toString();
        }

        function paginationItems(totalPages) {
            if (totalPages <= 5) { return Array.from({ length: totalPages }, function (_, index) { return index + 1; }); }
            if (currentPage <= 3) { return [1, 2, 3, 'ellipsis', totalPages]; }
            if (currentPage >= totalPages - 2) { return [1, 'ellipsis', totalPages - 2, totalPages - 1, totalPages]; }
            return [1, 'ellipsis', currentPage - 1, currentPage, currentPage + 1, 'ellipsis', totalPages];
        }

        function requestPage(page, loadAll) {
            if (isLoadingPage || page < 1) { return; }
            isLoadingPage = true;
            list.setAttribute('aria-busy', 'true');
            renderPagination();
            fetch(classGroupRowsUrl((page - 1) * Number(pagination.dataset.pageSize || '10'), loadAll), { credentials: 'same-origin', headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (response) { if (!response.ok) { throw new Error('Failed to load class groups.'); } return response.text(); })
                .then(function (html) {
                    var holder = document.createElement('tbody'); holder.innerHTML = html;
                    var metadata = holder.querySelector('[data-class-group-load-more-meta]');
                    if (!metadata) { throw new Error('Invalid class group list response.'); }
                    currentPage = Number(metadata.dataset.currentPage) || page; showingAll = metadata.dataset.loadAll === 'true'; metadata.remove();
                    list.replaceChildren(); while (holder.firstChild) { list.appendChild(holder.firstChild); }
                    bindClassGroupActivityToggles(); renderRows();
                }).catch(function () {
                    // Keep the visible page when a transient request fails.
                }).finally(function () { isLoadingPage = false; list.removeAttribute('aria-busy'); renderPagination(); });
        }

        function paginationButton(page) {
            var button = document.createElement('button'); button.type = 'button'; button.className = 'gape-list-pagination-button'; button.textContent = String(page);
            var active = !showingAll && page === currentPage; button.classList.toggle('is-active', active); button.disabled = isLoadingPage;
            if (active) { button.setAttribute('aria-current', 'page'); }
            button.addEventListener('click', function () { requestPage(page, false); }); return button;
        }

        function paginationArrow(page, icon, label, disabled) {
            var button = document.createElement('button'); button.type = 'button'; button.className = 'gape-list-pagination-button gape-list-pagination-arrow'; button.disabled = disabled || isLoadingPage; button.setAttribute('aria-label', label); button.innerHTML = '<i class="ph ' + icon + '" aria-hidden="true"></i>';
            button.addEventListener('click', function () { requestPage(page, false); }); return button;
        }

        function renderPagination() {
            if (!pagination || !paginationPages) { return; }
            var totalPages = Math.ceil(Number(pagination.dataset.total || '0') / Number(pagination.dataset.pageSize || '10'));
            paginationPages.replaceChildren(); paginationPages.appendChild(paginationArrow(currentPage - 1, 'ph-caret-left', 'Previous page', showingAll || currentPage <= 1));
            paginationItems(totalPages).forEach(function (item) { if (item === 'ellipsis') { var ellipsis = document.createElement('span'); ellipsis.className = 'gape-list-pagination-ellipsis'; ellipsis.textContent = '…'; paginationPages.appendChild(ellipsis); } else { paginationPages.appendChild(paginationButton(item)); } });
            paginationPages.appendChild(paginationArrow(currentPage + 1, 'ph-caret-right', 'Next page', showingAll || currentPage >= totalPages));
            if (loadAllButton) { loadAllButton.disabled = isLoadingPage || showingAll; loadAllButton.classList.toggle('is-active', showingAll); loadAllButton.textContent = isLoadingPage ? 'Loading...' : 'Load All'; }
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
        bindClassGroupActivityToggles();
        renderRows();
        if (loadAllButton) { loadAllButton.addEventListener('click', function () { requestPage(1, true); }); }
        renderPagination();
    })();
</script>
</body>
</html>
