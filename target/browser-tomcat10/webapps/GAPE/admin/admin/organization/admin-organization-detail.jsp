<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
    <title>GAPE - Organization Details</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        :root,
        .og-shell {
            --og-primary: #2563eb;
            --og-primary-dark: #1d4ed8;
            --og-primary-soft: #eff6ff;
            --og-border: #e6edf0;
            --og-muted: #64748b;
            --og-ink: #172033;
        }

        .og-surface {
            background: #fff;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
        }

        .og-hero {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(14, 165, 233, 0.06)), #fff;
        }

        .og-mode-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .og-mode-card {
            background: #fff;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            color: var(--og-ink);
            cursor: pointer;
            min-height: 132px;
            padding: 20px;
            text-align: left;
            transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
            width: 100%;
        }

        .og-mode-card.is-active {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(14, 165, 233, 0.06)), #f8fbff;
            border-color: rgba(37, 99, 235, 0.65);
            box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12);
            transform: translateY(-1px);
        }

        .og-mode-card.is-active .og-mode-icon {
            background: var(--og-primary) !important;
            color: #fff !important;
        }

        .og-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex-shrink: 0;
            height: 44px;
            justify-content: center;
            width: 44px;
        }

        .og-tab-panel[hidden] {
            display: none !important;
        }

        .og-primary-button,
        .og-outline-button,
        .og-danger-button,
        .og-warning-button {
            align-items: center;
            border-radius: 8px;
            cursor: pointer;
            display: inline-flex;
            font-weight: 600;
            justify-content: center;
            line-height: 1.2;
            min-height: 42px;
            padding: 10px 16px;
            text-decoration: none;
            transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
        }

        .og-primary-button {
            background: var(--og-primary);
            border: 1px solid var(--og-primary);
            color: #fff;
        }

        .og-primary-button:hover {
            background: var(--og-primary-dark);
            border-color: var(--og-primary-dark);
            color: #fff;
            transform: translateY(-1px);
        }

        .og-outline-button {
            background: #fff;
            border: 1px solid var(--og-border);
            color: var(--og-ink);
        }

        .og-outline-button:hover {
            border-color: rgba(37, 99, 235, 0.45);
            color: var(--og-primary-dark);
            text-decoration: none;
        }

        .og-danger-button {
            background: #dc2626;
            border: 1px solid #dc2626;
            color: #fff;
        }

        .og-warning-button {
            background: #fff7ed;
            border: 1px solid #fed7aa;
            color: #c2410c;
        }

        .og-warning-button:hover {
            background: #ffedd5;
            border-color: #fdba74;
            color: #9a3412;
            transform: translateY(-1px);
        }

        .og-danger-button:hover {
            background: #b91c1c;
            border-color: #b91c1c;
            color: #fff;
            transform: translateY(-1px);
        }

        .og-soft-badge {
            background: var(--og-primary-soft);
            color: var(--og-primary);
        }

        .og-count-text {
            color: var(--og-primary);
        }

        .og-table-wrap {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        .og-data-table {
            min-width: 760px;
        }

        .og-info-grid {
            display: grid;
            gap: 14px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
        }

        .og-info-cell {
            background: #f8fbfb;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            min-height: 88px;
            padding: 16px;
        }

        .gape-organization-detail-photo {
            border-radius: 12px;
            height: 76px;
            object-fit: cover;
            width: 76px;
        }

        .gape-learning-table-photo {
            border-radius: 12px;
            height: 44px;
            object-fit: cover;
            width: 44px;
        }

        .gape-hierarchy-node {
            border-inline-start: 3px solid var(--og-primary);
        }

        .gape-tree-toggle {
            align-items: center;
            background: transparent;
            border: 0;
            border-radius: 8px;
            cursor: pointer;
            display: inline-flex;
            height: 32px;
            justify-content: center;
            padding: 0;
            width: 32px;
        }

        .gape-tree-toggle:hover,
        .gape-tree-toggle:focus-visible {
            background-color: var(--og-primary-soft);
            color: var(--og-primary) !important;
            text-decoration: none;
        }

        .og-shell .hover-text-main-600:hover {
            color: var(--og-primary) !important;
        }

        .gape-organization-units-panel {
            background: transparent;
            border: 0;
            border-radius: 0;
            padding: 0;
        }

        .gape-organization-units-panel > .px-18.py-18 {
            padding: 0 !important;
        }

        .gape-structure-panel {
            background-color: #f8fbff;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            margin-top: 14px;
            padding: 14px;
        }

        .gape-course-node {
            border-inline-start: 3px solid #2563eb;
        }

        .gape-subject-node {
            border-inline-start: 3px solid #0ea5e9;
        }

        .gape-class-group-node {
            border-inline-start: 3px solid #7c3aed;
        }

        .gape-node-meta {
            color: var(--og-muted);
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .gape-structure-list-header,
        .gape-structure-row {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: minmax(280px, 1.7fr) minmax(120px, 0.55fr) minmax(120px, 0.55fr) minmax(120px, 0.55fr) minmax(150px, auto);
        }

        .gape-structure-list-header {
            color: #475569;
            font-size: 13px;
            font-weight: 600;
            padding: 0 16px 12px;
        }

        .gape-structure-node {
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .gape-structure-node:hover {
            border-color: rgba(37, 99, 235, 0.28) !important;
            box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
        }

        .og-element-count {
            color: var(--og-ink);
            font-size: 13px;
            font-weight: 500;
        }

        .og-unit-name-button {
            background: transparent;
            border: 0;
            color: inherit;
            padding: 0;
            text-align: left;
        }

        .og-unit-name-button:hover,
        .og-unit-name-button:focus-visible {
            color: var(--og-primary) !important;
            text-decoration: none;
        }

        .og-unit-modal-form .select2-container {
            width: 100% !important;
        }

        .og-unit-modal .modal-dialog {
            --bs-modal-margin: 1rem;
            margin-left: auto;
            margin-right: auto;
            max-height: calc(100vh - 2rem);
        }

        .og-unit-modal.show .modal-dialog {
            transform: none !important;
        }

        .og-unit-modal .modal-dialog-centered {
            min-height: calc(100% - 2rem);
        }

        .og-unit-modal .modal-content {
            display: flex;
            max-height: calc(100vh - 2rem);
        }

        .og-unit-modal .modal-body {
            overflow-y: auto;
        }

        .og-unit-modal .modal-footer {
            align-items: center;
            background: #fff;
            bottom: 0;
            display: flex;
            flex-shrink: 0;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: flex-end;
            padding: 16px 20px;
            position: sticky;
            z-index: 3;
        }

        .og-unit-modal .modal-footer .og-outline-button,
        .og-unit-modal .modal-footer .og-primary-button {
            min-width: 132px;
        }

        .og-unit-modal .modal-footer .og-primary-button {
            background: var(--og-primary) !important;
            border-color: var(--og-primary) !important;
            box-shadow: 0 10px 18px rgba(37, 99, 235, 0.18);
            color: #fff !important;
        }

        .og-unit-modal .modal-footer .og-primary-button:hover {
            background: var(--og-primary-dark) !important;
            border-color: var(--og-primary-dark) !important;
        }

        .og-unit-modal .modal-footer .og-primary-button:disabled {
            opacity: 0.86;
            transform: none;
        }

        .og-button-spinner {
            animation: og-spin 0.75s linear infinite;
            border: 2px solid rgba(255, 255, 255, 0.42);
            border-radius: 50%;
            border-top-color: #fff;
            display: inline-block;
            flex: 0 0 auto;
            height: 16px;
            margin-right: 8px;
            width: 16px;
        }

        @keyframes og-spin {
            to {
                transform: rotate(360deg);
            }
        }

        .og-readonly-input[readonly] {
            cursor: default;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .og-critical-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .og-critical-card {
            background: #fff;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            padding: 20px;
        }

        .og-critical-card--archive {
            border-color: #fed7aa;
        }

        .og-critical-card--delete {
            border-color: #fecaca;
        }

        .og-critical-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            height: 44px;
            justify-content: center;
            width: 44px;
        }

        .og-critical-icon--archive {
            background: #fff7ed;
            color: #c2410c;
        }

        .og-critical-icon--delete {
            background: #fef2f2;
            color: #dc2626;
        }

        .og-activity-panel {
            background: #f8fafc;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            margin-top: 14px;
            padding: 16px;
        }

        .og-activity-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .og-activity-column {
            background: #fff;
            border: 1px solid var(--og-border);
            border-radius: 8px;
            min-width: 0;
            padding: 14px;
        }

        .og-activity-column-header {
            align-items: center;
            border-bottom: 1px dashed var(--og-border);
            display: flex;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 12px;
            padding-bottom: 12px;
        }

        .og-activity-heading {
            color: var(--og-ink);
            font-size: 14px;
            font-weight: 700;
        }

        .og-activity-count {
            background: var(--og-primary-soft);
            border-radius: 999px;
            color: var(--og-primary);
            font-size: 12px;
            font-weight: 700;
            padding: 5px 9px;
        }

        .og-activity-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .og-activity-card {
            border: 1px solid var(--og-border);
            border-radius: 8px;
            padding: 12px;
        }

        .og-activity-card--lesson {
            border-left: 3px solid #2563eb;
        }

        .og-activity-card--assessment {
            border-left: 3px solid #7c3aed;
        }

        .og-activity-action {
            align-items: center;
            background: var(--og-primary-soft);
            border-radius: 7px;
            color: var(--og-primary);
            display: inline-flex;
            height: 30px;
            justify-content: center;
            width: 30px;
        }

        .og-activity-action:hover {
            background: var(--og-primary);
            color: #fff;
        }

        .og-activity-empty {
            background: #f8fafc;
            border: 1px dashed var(--og-border);
            border-radius: 8px;
            color: var(--og-muted);
            font-size: 13px;
            padding: 18px;
            text-align: center;
        }

        @media (max-width: 991.98px) {
            .og-info-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }

            .og-activity-grid,
            .og-critical-grid {
                grid-template-columns: 1fr;
            }
        }

        @media (max-width: 767.98px) {
            .og-mode-grid {
                grid-template-columns: 1fr;
            }

            .gape-structure-list-header {
                display: none;
            }

            .gape-structure-row {
                grid-template-columns: 1fr;
            }
        }

        @media (max-width: 575.98px) {
            .og-info-grid {
                grid-template-columns: 1fr;
            }

            .og-unit-modal .modal-dialog {
                margin: 8px;
                max-height: calc(100vh - 16px);
            }

            .og-unit-modal .modal-content {
                max-height: calc(100vh - 16px);
            }

            .og-unit-modal .modal-footer {
                justify-content: stretch;
            }

            .og-unit-modal .modal-footer .og-outline-button,
            .og-unit-modal .modal-footer .og-primary-button {
                flex: 1 1 140px;
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
            <div class="og-shell px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <section class="og-surface og-hero px-24 py-24 mb-20">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-center gap-16 min-w-0">
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
                            <div class="min-w-0">
                                <div class="d-flex align-items-center gap-10 flex-wrap mb-10">
                                    <span class="${organization.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                                        <c:out value="${organization.stateLabel}"/>
                                    </span>
                                    <span class="og-soft-badge px-14 py-7 rounded-pill text-13">
                                        <i class="ph ph-buildings me-6"></i><c:out value="${organization.typeLabel}"/>
                                    </span>
                                </div>
                                <h2 class="text-28 fw-semibold text-neutral-800 mb-8"><c:out value="${organization.name}"/></h2>
                                <span class="text-14 text-neutral-500">
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${organization.name}'/>"><c:out value="${organization.acronym}"/></span>
                                    &middot; <c:out value="${organization.organicUnitCount}"/> organic units
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${pageContext.request.contextPath}/admin/organizations" class="og-outline-button">
                                <i class="ph ph-arrow-left me-8"></i>Back
                            </a>
                            <button type="button" class="og-outline-button" data-bs-toggle="modal" data-bs-target="#organizationSetupModal">
                                <i class="ph ph-sliders-horizontal me-8"></i>Setup
                            </button>
                            <c:if test="${canModifyOrganization}">
                                <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}/edit" class="og-outline-button">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                        </div>
                    </div>
                </section>

                <div class="og-mode-grid mb-20" role="tablist" aria-label="Organization detail views">
                    <button type="button" class="og-mode-card is-active" data-organization-tab="organic-units" aria-selected="true">
                        <span class="og-mode-icon og-soft-badge text-24 mb-14"><i class="ph ph-tree-structure"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Organic Units</span>
                        <span class="text-14 text-neutral-500 d-block mb-12">Complete hierarchy, courses, subjects and class groups.</span>
                        <span class="text-13 og-count-text fw-semibold"><c:out value="${organization.organicUnitCount}"/> organic units</span>
                    </button>
                    <button type="button" class="og-mode-card" data-organization-tab="administrators" aria-selected="false">
                        <span class="og-mode-icon bg-info-50 text-info-600 text-24 mb-14"><i class="ph ph-user-gear"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Administrators</span>
                        <span class="text-14 text-neutral-500 d-block mb-12">Organization-level administrator assignments.</span>
                        <span class="text-13 og-count-text fw-semibold">${fn:length(assignedAdministrators)} assigned</span>
                    </button>
                </div>

                <main class="min-w-0">
                    <div class="og-tab-panel" data-organization-panel="organic-units">
                        <section class="og-surface px-22 py-22 mb-20">
                            <%@ include file="/WEB-INF/fragments/organization-structure-tree.jspf" %>
                        </section>
                    </div>

                    <div class="og-tab-panel" data-organization-panel="administrators" hidden>
                        <section class="og-surface px-22 py-22 mb-20">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                                <div>
                                    <h3 class="text-18 fw-semibold text-neutral-800 mb-6">Administrators</h3>
                                    <span class="text-14 text-neutral-500">Assign and review organization administrators.</span>
                                </div>
                                <span class="og-soft-badge px-14 py-8 rounded-pill text-13">
                                    ${fn:length(assignedAdministrators)} assigned
                                </span>
                            </div>

                            <c:if test="${canAssignOrganizationAdministrators}">
                                <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/assign-admin" method="post" class="border border-neutral-30 rounded-8 px-18 py-18 mb-20">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <div class="row gy-3 align-items-start">
                                        <div class="col-lg-6 gape-select-field">
                                            <label for="administratorUserId" class="fw-medium text-base text-neutral-800 mb-12">Administrator</label>
                                            <select id="administratorUserId" name="administratorUserId" required class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                                <option value="">Select administrator</option>
                                                <c:forEach var="administrator" items="${administratorOptions}">
                                                    <option value="${administrator.id}">#<c:out value="${administrator.id}"/> - <c:out value="${administrator.name}"/> - <c:out value="${administrator.email}"/></option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <div class="col-md-3">
                                            <label for="administratorStartDate" class="fw-medium text-base text-neutral-800 mb-12">Start</label>
                                            <input id="administratorStartDate" name="startDate" type="date" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                                        </div>
                                        <div class="col-md-3">
                                            <label for="administratorEndDate" class="fw-medium text-base text-neutral-800 mb-12">End</label>
                                            <input id="administratorEndDate" name="endDate" type="date" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                                        </div>
                                        <div class="col-12">
                                            <button type="submit" class="og-primary-button">Assign Administrator</button>
                                        </div>
                                    </div>
                                </form>
                            </c:if>
                            <c:if test="${canAssignOrganizationAdministrators and empty administratorOptions}">
                                <div class="alert alert-warning rounded-8 border-0 mb-20" role="alert">No active administrators are available for assignment.</div>
                            </c:if>

                            <div class="og-table-wrap">
                                <table class="table mb-0 og-data-table">
                                    <thead>
                                    <tr>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Administrator</th>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Period</th>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">User</th>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Assignment</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="assignment" items="${assignedAdministrators}">
                                        <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                            <td class="py-16 px-16">
                                                <span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${assignment.name}"/></span>
                                                <span class="text-12 text-neutral-500"><c:out value="${assignment.email}"/></span>
                                            </td>
                                            <td class="py-16 px-16 text-14 text-neutral-500" data-gape-datetime-display>
                                                <c:out value="${assignment.startDateLabel}"/> to <c:out value="${assignment.endDateLabel}"/>
                                            </td>
                                            <td class="py-16 px-16 text-14 text-neutral-500"><c:out value="${assignment.userStateLabel}"/></td>
                                            <td class="py-16 px-16">
                                                <div class="d-flex align-items-center gap-8 flex-wrap">
                                                    <span class="${assignment.assignmentBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12">
                                                        <c:out value="${assignment.assignmentStateLabel}"/>
                                                    </span>
                                                    <span class="text-12 text-neutral-500">
                                                        Active now: <c:out value="${assignment.currentlyActive ? 'Yes' : 'No'}"/>
                                                    </span>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty assignedAdministrators}">
                                        <tr>
                                            <td colspan="4" class="py-32 px-16 text-center text-14 text-neutral-500">No administrators assigned.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>

                </main>

                <c:if test="${canModifyOrganization}">
                    <section class="og-surface px-22 py-22 mb-20">
                        <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                            <div>
                                <h3 class="text-20 fw-semibold text-neutral-800 mb-6">Critical Actions</h3>
                                <span class="text-14 text-neutral-500">Restricted lifecycle operations for this organization.</span>
                            </div>
                            <span class="bg-warning-50 text-warning-600 px-14 py-8 rounded-pill text-13">
                                <i class="ph ph-lock-key me-6"></i>Requires confirmation
                            </span>
                        </div>
                        <div class="og-critical-grid">
                            <article class="og-critical-card og-critical-card--archive">
                                <div class="d-flex align-items-start gap-14 mb-16">
                                    <span class="og-critical-icon og-critical-icon--archive text-22"><i class="ph ph-archive"></i></span>
                                    <div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Deactivate Organization</h4>
                                        <p class="text-14 text-neutral-500 mb-0">Temporarily disables this organization while preserving its structure.</p>
                                    </div>
                                </div>
                                <button type="button" class="og-warning-button" data-bs-toggle="modal" data-bs-target="#archiveOrganization">Deactivate</button>
                            </article>
                            <article class="og-critical-card og-critical-card--delete">
                                <div class="d-flex align-items-start gap-14 mb-16">
                                    <span class="og-critical-icon og-critical-icon--delete text-22"><i class="ph ph-trash"></i></span>
                                    <div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Delete Organization</h4>
                                        <p class="text-14 text-neutral-500 mb-0">Permanently removes the record when the database allows it.</p>
                                    </div>
                                </div>
                                <button type="button" class="og-danger-button" data-bs-toggle="modal" data-bs-target="#deleteOrganization">Delete</button>
                            </article>
                        </div>
                    </section>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<div class="modal fade" id="organizationSetupModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <div>
                    <h5 class="modal-title text-18 fw-semibold mb-4">Setup</h5>
                    <span class="text-13 text-neutral-500">Organization identity, state and structure summary.</span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="d-flex align-items-center gap-10 flex-wrap mb-18">
                    <span class="${organization.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                        <c:out value="${organization.stateLabel}"/>
                    </span>
                    <span class="og-soft-badge px-14 py-7 rounded-pill text-13">
                        <i class="ph ph-buildings me-6"></i><c:out value="${organization.typeLabel}"/>
                    </span>
                </div>
                <div class="og-info-grid">
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Name</span><strong class="text-14 text-neutral-800"><c:out value="${organization.name}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Acronym</span><strong class="text-14 text-neutral-800"><c:out value="${organization.acronym}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Type</span><strong class="text-14 text-neutral-800"><c:out value="${organization.typeLabel}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">State</span><strong class="text-14 text-neutral-800"><c:out value="${organization.stateLabel}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Organic Units</span><strong class="text-14 text-neutral-800"><c:out value="${organization.organicUnitCount}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Administrators</span><strong class="text-14 text-neutral-800">${fn:length(assignedAdministrators)}</strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Photo</span><strong class="text-14 text-neutral-800"><c:out value="${organization.hasPhoto ? 'Available' : 'Missing'}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Identifier</span><strong class="text-14 text-neutral-800">#<c:out value="${organization.id}"/></strong></div>
                </div>
            </div>
        </div>
    </div>
</div>

<select id="organicUnitParentLookup" class="d-none" aria-hidden="true" tabindex="-1">
    <option value="" data-acronym="Root">Root</option>
    <c:forEach var="parent" items="${organization.organicUnits}">
        <option value="${parent.id}" data-acronym="<c:out value='${parent.acronym}'/>">
            <c:out value="${parent.code}"/> - <c:out value="${parent.name}"/>
        </option>
    </c:forEach>
</select>

<div class="modal fade og-unit-modal" id="organicUnitCreateModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/units" method="post" class="og-unit-modal-form" data-unit-dynamic-form>
                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                <input type="hidden" name="returnTo" value="/admin/organizations/${organization.id}">
                <div class="modal-header border-neutral-30">
                    <div>
                        <h5 class="modal-title text-18 fw-semibold mb-4">Create Organic Unit</h5>
                        <span class="text-13 text-neutral-500" data-create-unit-parent-context>Parent: Root</span>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none rounded-8 border-0 mb-16" role="alert" data-unit-form-error></div>
                    <div class="row gy-4">
                        <div class="col-lg-8">
                            <label for="createUnitName" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="createUnitName" name="name" type="text" required pattern="[^|]*" title="Names cannot contain |" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4">
                            <label for="createUnitAcronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="createUnitAcronym" name="acronym" type="text" required pattern="[^|]*" title="Acronyms cannot contain |" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4">
                            <label for="createUnitType" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="createUnitType" name="type" class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="SCHOOL">School</option>
                                <option value="FACULTY">Faculty</option>
                                <option value="DEPARTMENT" selected>Department</option>
                                <option value="CENTER">Center</option>
                                <option value="OFFICE">Office</option>
                                <option value="SERVICE">Service</option>
                                <option value="SECTION">Section</option>
                                <option value="DIRECTION">Direction</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="createUnitState" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="createUnitState" name="state" class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="ACTIVE" selected>Active</option>
                                <option value="INACTIVE">Inactive</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="createUnitParentOrganicUnitDisplay" class="fw-medium text-base text-neutral-800 mb-12">Parent Unit</label>
                            <input id="createUnitParentOrganicUnitId" name="parentOrganicUnitId" type="hidden">
                            <input id="createUnitParentOrganicUnitDisplay" type="text" readonly
                                   class="form-control og-readonly-input px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8"
                                   data-parent-display value="Root">
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-neutral-30">
                    <button type="button" class="og-outline-button" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="og-primary-button">Save Unit</button>
                </div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade og-unit-modal" id="organicUnitDetailModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <div>
                    <h5 class="modal-title text-18 fw-semibold mb-4" data-unit-detail-title>Organic Unit Detail</h5>
                    <span class="text-13 text-neutral-500" data-unit-detail-subtitle></span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="d-flex align-items-center gap-10 flex-wrap mb-18">
                    <span data-unit-detail-state class="px-14 py-7 border-neutral-30 border rounded-pill text-13"></span>
                    <span class="og-soft-badge px-14 py-7 rounded-pill text-13">
                        <i class="ph ph-tree-structure me-6"></i><span data-unit-detail-type></span>
                    </span>
                </div>
                <div class="og-info-grid">
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Identifier</span><strong class="text-14 text-neutral-800">#<span data-unit-detail-id></span></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Generated Code</span><strong class="text-14 text-neutral-800" data-unit-detail-code></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Acronym</span><strong class="text-14 text-neutral-800" data-unit-detail-acronym></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Parent</span><strong class="text-14 text-neutral-800" data-unit-detail-parent></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Courses</span><strong class="text-14 text-neutral-800" data-unit-detail-courses></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Visible Elements</span><strong class="text-14 text-neutral-800" data-unit-detail-elements></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Organization</span><strong class="text-14 text-neutral-800"><c:out value="${organization.name}"/></strong></div>
                    <div class="og-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">State</span><strong class="text-14 text-neutral-800" data-unit-detail-state-label></strong></div>
                </div>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="og-outline-button" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="og-primary-button d-none" data-unit-detail-edit-button>
                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                </button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade og-unit-modal" id="organicUnitEditModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <form method="post" class="og-unit-modal-form" data-unit-edit-form data-unit-dynamic-form>
                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                <input type="hidden" name="returnTo" value="/admin/organizations/${organization.id}">
                <div class="modal-header border-neutral-30">
                    <div>
                        <h5 class="modal-title text-18 fw-semibold mb-4">Edit Organic Unit</h5>
                        <span class="text-13 text-neutral-500" data-unit-edit-subtitle></span>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="alert alert-danger d-none rounded-8 border-0 mb-16" role="alert" data-unit-form-error></div>
                    <div class="row gy-4">
                        <div class="col-lg-3">
                            <span class="fw-medium text-base text-neutral-800 mb-12 d-block">Generated Code</span>
                            <div class="px-20 py-12 text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8" data-unit-edit-code></div>
                        </div>
                        <div class="col-lg-6">
                            <label for="editUnitName" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="editUnitName" name="name" type="text" required pattern="[^|]*" title="Names cannot contain |" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="editUnitAcronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="editUnitAcronym" name="acronym" type="text" required pattern="[^|]*" title="Acronyms cannot contain |" class="form-control px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4">
                            <label for="editUnitType" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="editUnitType" name="type" class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="SCHOOL">School</option>
                                <option value="FACULTY">Faculty</option>
                                <option value="DEPARTMENT">Department</option>
                                <option value="CENTER">Center</option>
                                <option value="OFFICE">Office</option>
                                <option value="SERVICE">Service</option>
                                <option value="SECTION">Section</option>
                                <option value="DIRECTION">Direction</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="editUnitState" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="editUnitState" name="state" class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="ACTIVE">Active</option>
                                <option value="INACTIVE">Inactive</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="editUnitParentOrganicUnitDisplay" class="fw-medium text-base text-neutral-800 mb-12">Parent Unit</label>
                            <input id="editUnitParentOrganicUnitId" name="parentOrganicUnitId" type="hidden">
                            <input id="editUnitParentOrganicUnitDisplay" type="text" readonly
                                   class="form-control og-readonly-input px-20 py-12 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-8"
                                   data-parent-display value="Root">
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-neutral-30">
                    <button type="button" class="og-outline-button" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="og-primary-button">Save Unit</button>
                </div>
            </form>
        </div>
    </div>
</div>

<c:if test="${canModifyOrganization}">
<div class="modal fade" id="archiveOrganization" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Deactivate Organization</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm deactivation of <strong><c:out value="${organization.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="og-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="og-warning-button">Deactivate</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteOrganization" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Organization</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the record if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="og-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/admin/organizations/${organization.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="og-danger-button">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    function bindTreeToggles(root) {
        (root || document).querySelectorAll('[data-gape-tree-toggle]').forEach(function (button) {
            if (button.dataset.gapeTreeBound === 'true') {
                return;
            }
            button.dataset.gapeTreeBound = 'true';
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
    }

    function bindOrganizationTabs() {
        document.querySelectorAll('[data-organization-tab]').forEach(function (tab) {
            if (tab.dataset.organizationTabBound === 'true') {
                return;
            }
            tab.dataset.organizationTabBound = 'true';
            tab.addEventListener('click', function () {
                var target = tab.getAttribute('data-organization-tab');
                document.querySelectorAll('[data-organization-tab]').forEach(function (item) {
                    var active = item === tab;
                    item.classList.toggle('is-active', active);
                    item.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                document.querySelectorAll('[data-organization-panel]').forEach(function (panel) {
                    panel.hidden = panel.getAttribute('data-organization-panel') !== target;
                });
            });
        });
    }

    function activateOrganizationPanel(name) {
        var tab = document.querySelector('[data-organization-tab="' + name + '"]');
        if (tab) {
            tab.click();
        }
    }

    function showBootstrapModal(selector) {
        var modal = document.querySelector(selector);
        if (!modal || !window.bootstrap) {
            return;
        }
        window.bootstrap.Modal.getOrCreateInstance(modal).show();
    }

    function setText(selector, value) {
        var element = document.querySelector(selector);
        if (element) {
            element.textContent = value || '';
        }
    }

    function selectedValue(select, value) {
        if (!select) {
            return;
        }
        select.value = value || '';
        select.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function triggerForUnit(unitId, selector) {
        if (!unitId) {
            return null;
        }
        return document.querySelector(selector + '[data-unit-id="' + unitId + '"]');
    }

    function unitData(trigger) {
        return trigger ? trigger.dataset : {};
    }

    function parentInfo(parentId, fallbackLabel, fallbackAcronym) {
        var normalizedId = parentId ? String(parentId) : '';
        var label = fallbackLabel || '';
        var acronym = fallbackAcronym || '';
        var lookup = document.getElementById('organicUnitParentLookup');
        if (lookup) {
            var option = Array.prototype.find.call(lookup.options, function (item) {
                return item.value === normalizedId;
            });
            if (option) {
                label = option.textContent.trim() || label;
                acronym = option.dataset.acronym || acronym;
            }
        }
        label = label || 'Root';
        acronym = acronym || label;
        return { id: normalizedId, label: label, acronym: acronym };
    }

    function fitParentDisplay(input) {
        if (!input) {
            return;
        }
        input.value = input.dataset.fullLabel || 'Root';
        window.requestAnimationFrame(function () {
            if (input.scrollWidth > input.clientWidth && input.dataset.acronym) {
                input.value = input.dataset.acronym;
            }
        });
    }

    function fitAllParentDisplays(root) {
        (root || document).querySelectorAll('[data-parent-display]').forEach(fitParentDisplay);
    }

    function setReadonlyParent(hiddenId, displayId, parentId, parentLabel, parentAcronym) {
        var info = parentInfo(parentId, parentLabel, parentAcronym);
        var hidden = document.getElementById(hiddenId);
        var display = document.getElementById(displayId);
        if (hidden) {
            hidden.value = info.id;
        }
        if (display) {
            display.dataset.fullLabel = info.label;
            display.dataset.acronym = info.acronym;
            display.setAttribute('title', info.label);
            fitParentDisplay(display);
        }
        return info;
    }

    function setCreateParent(parentId, parentLabel, parentAcronym) {
        var info = setReadonlyParent(
            'createUnitParentOrganicUnitId',
            'createUnitParentOrganicUnitDisplay',
            parentId || '',
            parentLabel,
            parentAcronym
        );
        setText('[data-create-unit-parent-context]', 'Parent: ' + info.label);
    }

    function hideUnitFormError(form) {
        var alert = form ? form.querySelector('[data-unit-form-error]') : null;
        if (alert) {
            alert.textContent = '';
            alert.classList.add('d-none');
        }
    }

    function showUnitFormError(form, message) {
        var alert = form ? form.querySelector('[data-unit-form-error]') : null;
        if (alert) {
            alert.textContent = message || 'The unit could not be saved.';
            alert.classList.remove('d-none');
        }
    }

    function configureCreateModal(trigger) {
        var data = trigger ? trigger.dataset : {};
        var form = document.querySelector('#organicUnitCreateModal form');
        if (form) {
            form.reset();
            hideUnitFormError(form);
            selectedValue(document.getElementById('createUnitType'), 'DEPARTMENT');
            selectedValue(document.getElementById('createUnitState'), 'ACTIVE');
        }
        setCreateParent(data.parentId, data.parentLabel, data.parentAcronym);
    }

    function configureDetailModal(trigger) {
        var data = unitData(trigger);
        setText('[data-unit-detail-title]', (data.unitCode || '') + ' - ' + (data.unitName || ''));
        setText('[data-unit-detail-subtitle]', 'Parent: ' + (data.unitParentLabel || 'Root'));
        setText('[data-unit-detail-id]', data.unitId);
        setText('[data-unit-detail-code]', data.unitCode);
        setText('[data-unit-detail-acronym]', data.unitAcronym);
        setText('[data-unit-detail-parent]', data.unitParentLabel || 'Root');
        setText('[data-unit-detail-courses]', data.unitCourseCount || '0');
        setText('[data-unit-detail-elements]', data.unitElementCount || '0');
        setText('[data-unit-detail-type]', data.unitTypeLabel);
        setText('[data-unit-detail-state-label]', data.unitStateLabel);
        var state = document.querySelector('[data-unit-detail-state]');
        if (state) {
            state.className = (data.unitStateClass || '') + ' px-14 py-7 border-neutral-30 border rounded-pill text-13';
            state.textContent = data.unitStateLabel || '';
        }
        var editButton = document.querySelector('[data-unit-detail-edit-button]');
        if (editButton) {
            editButton.classList.toggle('d-none', data.unitCanModify !== 'true');
            editButton.onclick = function () {
                var detailModal = document.getElementById('organicUnitDetailModal');
                if (detailModal && window.bootstrap) {
                    window.bootstrap.Modal.getOrCreateInstance(detailModal).hide();
                }
                configureEditModal(trigger);
                showBootstrapModal('#organicUnitEditModal');
            };
        }
    }

    function configureEditModal(trigger) {
        var data = unitData(trigger);
        var form = document.querySelector('[data-unit-edit-form]');
        if (form) {
            form.action = '${pageContext.request.contextPath}/admin/organizations/${organization.id}/units/' + data.unitId;
        }
        setText('[data-unit-edit-subtitle]', (data.unitCode || '') + ' - ' + (data.unitName || ''));
        setText('[data-unit-edit-code]', data.unitCode);
        var name = document.getElementById('editUnitName');
        var acronym = document.getElementById('editUnitAcronym');
        if (name) {
            name.value = data.unitName || '';
        }
        if (acronym) {
            acronym.value = data.unitAcronym || '';
        }
        selectedValue(document.getElementById('editUnitType'), data.unitType);
        selectedValue(document.getElementById('editUnitState'), data.unitState);
        var editForm = document.querySelector('[data-unit-edit-form]');
        if (editForm) {
            hideUnitFormError(editForm);
        }
        setReadonlyParent(
            'editUnitParentOrganicUnitId',
            'editUnitParentOrganicUnitDisplay',
            data.unitParentId || '',
            data.unitParentLabel,
            data.unitParentAcronym
        );
    }

    function bindUnitModalTriggers(root) {
        (root || document).querySelectorAll('[data-unit-create-trigger]').forEach(function (trigger) {
            if (trigger.dataset.unitCreateBound === 'true') {
                return;
            }
            trigger.dataset.unitCreateBound = 'true';
            trigger.addEventListener('click', function () {
                configureCreateModal(trigger);
            });
        });
        (root || document).querySelectorAll('[data-unit-detail-trigger]').forEach(function (trigger) {
            if (trigger.dataset.unitDetailBound === 'true') {
                return;
            }
            trigger.dataset.unitDetailBound = 'true';
            trigger.addEventListener('click', function () {
                configureDetailModal(trigger);
            });
        });
        (root || document).querySelectorAll('[data-unit-edit-trigger]').forEach(function (trigger) {
            if (trigger.dataset.unitEditBound === 'true') {
                return;
            }
            trigger.dataset.unitEditBound = 'true';
            trigger.addEventListener('click', function () {
                configureEditModal(trigger);
            });
        });
    }

    function closeUnitModals() {
        document.querySelectorAll('.og-unit-modal.show').forEach(function (modal) {
            if (window.bootstrap) {
                window.bootstrap.Modal.getOrCreateInstance(modal).hide();
            }
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden', 'true');
            modal.style.display = 'none';
        });
        document.querySelectorAll('.modal-backdrop').forEach(function (backdrop) {
            backdrop.remove();
        });
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('padding-right');
    }

    function errorMessageFromHtml(htmlText) {
        var doc = new DOMParser().parseFromString(htmlText, 'text/html');
        var alert = doc.querySelector('.alert-danger');
        return alert ? alert.textContent.trim() : '';
    }

    function replaceUnitSurface(htmlText) {
        var doc = new DOMParser().parseFromString(htmlText, 'text/html');
        var nextPanel = doc.querySelector('[data-organization-panel="organic-units"]');
        var currentPanel = document.querySelector('[data-organization-panel="organic-units"]');
        if (nextPanel && currentPanel) {
            currentPanel.innerHTML = nextPanel.innerHTML;
        }
        var nextTab = doc.querySelector('[data-organization-tab="organic-units"]');
        var currentTab = document.querySelector('[data-organization-tab="organic-units"]');
        if (nextTab && currentTab) {
            currentTab.innerHTML = nextTab.innerHTML;
        }
        ['organicUnitParentLookup', 'organicUnitCreateModal', 'organicUnitDetailModal', 'organicUnitEditModal'].forEach(function (id) {
            var current = document.getElementById(id);
            var next = doc.getElementById(id);
            if (current && next) {
                current.outerHTML = next.outerHTML;
            }
        });
        closeUnitModals();
        bindOrganizationDetailInteractions(document);
        activateOrganizationPanel('organic-units');
    }

    function setSubmitLoading(submit, loading) {
        if (!submit) {
            return;
        }
        if (loading) {
            submit.dataset.originalHtml = submit.innerHTML;
            submit.disabled = true;
            submit.classList.add('is-loading');
            submit.innerHTML = '<span class="og-button-spinner" aria-hidden="true"></span><span>Saving...</span>';
            return;
        }
        submit.disabled = false;
        submit.classList.remove('is-loading');
        if (submit.dataset.originalHtml) {
            submit.innerHTML = submit.dataset.originalHtml;
            delete submit.dataset.originalHtml;
        }
    }

    function waitForMinimumElapsed(startedAt, minimumMs) {
        var remaining = minimumMs - (Date.now() - startedAt);
        if (remaining <= 0) {
            return Promise.resolve();
        }
        return new Promise(function (resolve) {
            window.setTimeout(resolve, remaining);
        });
    }

    function bindDynamicUnitForms(root) {
        (root || document).querySelectorAll('[data-unit-dynamic-form]').forEach(function (form) {
            if (form.dataset.unitDynamicBound === 'true') {
                return;
            }
            form.dataset.unitDynamicBound = 'true';
            form.addEventListener('submit', function (event) {
                if (!window.fetch || !window.FormData || !window.DOMParser) {
                    return;
                }
                event.preventDefault();
                hideUnitFormError(form);
                var submit = form.querySelector('button[type="submit"]');
                var startedAt = Date.now();
                setSubmitLoading(submit, true);
                fetch(form.action, {
                    method: 'POST',
                    body: new FormData(form),
                    credentials: 'same-origin',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                }).then(function (response) {
                    return response.text().then(function (htmlText) {
                        return { response: response, htmlText: htmlText };
                    });
                }).then(function (result) {
                    if (!result.response.ok) {
                        showUnitFormError(form, errorMessageFromHtml(result.htmlText));
                        return;
                    }
                    return waitForMinimumElapsed(startedAt, 360).then(function () {
                        replaceUnitSurface(result.htmlText);
                    });
                }).catch(function () {
                    showUnitFormError(form, 'The unit could not be saved without refreshing the page.');
                }).finally(function () {
                    setSubmitLoading(submit, false);
                });
            });
        });
    }

    function bindUnitModalLifecycle(root) {
        (root || document).querySelectorAll('.og-unit-modal').forEach(function (modal) {
            if (modal.dataset.unitModalLifecycleBound === 'true') {
                return;
            }
            modal.dataset.unitModalLifecycleBound = 'true';
            modal.addEventListener('shown.bs.modal', function () {
                fitAllParentDisplays(modal);
            });
        });
    }

    function bindOrganizationDetailInteractions(root) {
        bindTreeToggles(root);
        bindOrganizationTabs();
        bindUnitModalTriggers(root);
        bindDynamicUnitForms(root);
        bindUnitModalLifecycle(root);
        fitAllParentDisplays(root);
    }

    window.addEventListener('resize', function () {
        fitAllParentDisplays(document);
    });

    bindOrganizationDetailInteractions(document);

    (function openRequestedUnitModal() {
        var params = new URLSearchParams(window.location.search);
        var createRequested = params.get('unitModal') === 'create';
        var detailUnitId = params.get('unitDetail');
        var editUnitId = params.get('unitEdit');
        if (!createRequested && !detailUnitId && !editUnitId) {
            return;
        }
        activateOrganizationPanel('organic-units');
        var panel = document.querySelector('[data-organization-panel="organic-units"]');
        if (panel) {
            panel.scrollIntoView({ block: 'start' });
        }
        if (createRequested) {
            setCreateParent(params.get('parentUnitId'), params.get('parentLabel') || 'Root');
            showBootstrapModal('#organicUnitCreateModal');
            return;
        }
        if (detailUnitId) {
            var detailTrigger = triggerForUnit(detailUnitId, '[data-unit-detail-trigger]');
            if (detailTrigger) {
                configureDetailModal(detailTrigger);
                showBootstrapModal('#organicUnitDetailModal');
            }
            return;
        }
        if (editUnitId) {
            var editTrigger = triggerForUnit(editUnitId, '[data-unit-edit-trigger]');
            if (editTrigger) {
                configureEditModal(editTrigger);
                showBootstrapModal('#organicUnitEditModal');
            }
        }
    })();
</script>
</body>
</html>
