<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "subjects");
    }
%>
<c:if test="${empty subjectBasePath}">
    <c:set var="subjectBasePath" value="/coordinator/subjects"/>
</c:if>
<c:if test="${empty subjectCourseBasePath}">
    <c:set var="subjectCourseBasePath" value="/courses"/>
</c:if>
<c:set var="subjectPhotoUrl" value=""/>
<c:if test="${subject.hasPhoto}">
    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Subject Details</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        :root,
        .cd-shell {
            --cd-primary: #2563eb;
            --cd-primary-dark: #1d4ed8;
            --cd-primary-soft: #eff6ff;
            --cd-primary-surface: #f8fbff;
            --cd-primary-rgb: 37, 99, 235;
            --cd-primary-highlight-rgb: 14, 165, 233;
            --cd-border: #e6edf0;
            --cd-muted: #64748b;
            --cd-ink: #172033;
            --og-primary: #2563eb;
            --og-primary-dark: #1d4ed8;
            --og-primary-soft: #eff6ff;
            --og-border: #e6edf0;
            --og-muted: #64748b;
            --og-ink: #172033;
        }

        .cd-shell--inactive {
            --cd-primary: #dc2626;
            --cd-primary-dark: #b91c1c;
            --cd-primary-soft: #fef2f2;
            --cd-primary-surface: #fffafa;
            --cd-primary-rgb: 220, 38, 38;
            --cd-primary-highlight-rgb: 248, 113, 113;
            --og-primary: #dc2626;
            --og-primary-dark: #b91c1c;
            --og-primary-soft: #fef2f2;
        }

        .cd-surface {
            background: #fff;
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
        }

        .cd-hero {
            background: linear-gradient(135deg, rgba(var(--cd-primary-rgb), 0.12), rgba(var(--cd-primary-highlight-rgb), 0.06)), #fff;
        }

        .cd-mode-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
        }

        .cd-mode-card {
            background: #fff;
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            color: var(--cd-ink);
            cursor: pointer;
            min-height: 144px;
            padding: 20px;
            text-align: left;
            transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
            width: 100%;
        }

        .cd-mode-card.is-active {
            background: linear-gradient(135deg, rgba(var(--cd-primary-rgb), 0.14), rgba(var(--cd-primary-highlight-rgb), 0.06)), var(--cd-primary-surface);
            border-color: rgba(var(--cd-primary-rgb), 0.65);
            box-shadow: 0 16px 36px rgba(var(--cd-primary-rgb), 0.12);
            transform: translateY(-1px);
        }

        .cd-mode-card.is-active .cd-mode-icon {
            background: var(--cd-primary) !important;
            color: #fff !important;
        }

        .gape-subject-loading-spinner {
            align-items: center;
            display: inline-flex;
            justify-content: center;
            min-height: 1.2em;
            min-width: 1.2em;
        }

        .gape-subject-loading-spinner > i {
            animation: gape-subject-loading-spin 0.8s linear infinite;
            display: inline-block;
        }

        @keyframes gape-subject-loading-spin {
            to {
                transform: rotate(360deg);
            }
        }

        .cd-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex-shrink: 0;
            height: 44px;
            justify-content: center;
            width: 44px;
        }

        .cd-tab-panel[hidden] {
            display: none !important;
        }

        .cd-primary-button,
        .cd-outline-button,
        .cd-danger-button,
        .cd-warning-button {
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

        .cd-primary-button {
            background: var(--cd-primary);
            border: 1px solid var(--cd-primary);
            color: #fff;
        }

        .cd-primary-button:hover {
            background: var(--cd-primary-dark);
            border-color: var(--cd-primary-dark);
            color: #fff;
            transform: translateY(-1px);
        }

        .cd-section-create-button {
            min-height: 44px;
            min-width: 184px;
            padding: 10px 18px;
            white-space: nowrap;
        }

        .cd-disabled-action-wrapper {
            cursor: not-allowed;
            display: inline-flex;
        }

        .cd-disabled-action-wrapper .cd-primary-button[disabled] {
            cursor: not-allowed;
            opacity: 0.58;
            pointer-events: none;
        }

        .cd-outline-button {
            background: #fff;
            border: 1px solid var(--cd-border);
            color: var(--cd-ink);
        }

        .cd-outline-button:hover {
            border-color: rgba(var(--cd-primary-rgb), 0.45);
            color: var(--cd-primary-dark);
            text-decoration: none;
        }

        .cd-danger-button {
            background: #dc2626;
            border: 1px solid #dc2626;
            color: #fff;
        }

        .cd-danger-button:hover {
            background: #b91c1c;
            border-color: #b91c1c;
            color: #fff;
            transform: translateY(-1px);
        }

        .cd-warning-button {
            background: #fff7ed;
            border: 1px solid #fed7aa;
            color: #c2410c;
        }

        .cd-warning-button:hover {
            background: #ffedd5;
            border-color: #fdba74;
            color: #9a3412;
            transform: translateY(-1px);
        }

        .cd-soft-badge {
            background: var(--cd-primary-soft);
            color: var(--cd-primary);
        }

        .cd-count-text {
            color: var(--cd-primary);
        }

        .cd-info-grid {
            display: grid;
            gap: 14px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
        }

        .cd-info-cell {
            background: #f8fbfb;
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            min-height: 88px;
            padding: 16px;
        }

        .cd-table-wrap {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        .cd-data-table {
            min-width: 760px;
        }

        .sd-enrollment-table {
            min-width: 0;
            table-layout: fixed;
            width: 100%;
        }

        .sd-enrollment-table th,
        .sd-enrollment-table td {
            overflow-wrap: anywhere;
            padding-left: 12px !important;
            padding-right: 12px !important;
            white-space: normal;
        }

        .sd-enrollment-table--pending th:nth-child(1),
        .sd-enrollment-table--pending td:nth-child(1) {
            width: 42%;
        }

        .sd-enrollment-table--pending th:nth-child(2),
        .sd-enrollment-table--pending td:nth-child(2) {
            width: 28%;
        }

        .sd-enrollment-table--pending th:nth-child(3),
        .sd-enrollment-table--pending td:nth-child(3) {
            overflow-wrap: normal;
            white-space: nowrap;
            width: 30%;
        }

        .sd-enrollment-table--active th:nth-child(1),
        .sd-enrollment-table--active td:nth-child(1),
        .sd-enrollment-table--audit th:nth-child(1),
        .sd-enrollment-table--audit td:nth-child(1) {
            width: 34%;
        }

        .sd-enrollment-table--active th:nth-child(2),
        .sd-enrollment-table--active td:nth-child(2),
        .sd-enrollment-table--audit th:nth-child(2),
        .sd-enrollment-table--audit td:nth-child(2) {
            width: 26%;
        }

        .sd-enrollment-table--active th:nth-child(3),
        .sd-enrollment-table--active td:nth-child(3),
        .sd-enrollment-table--audit th:nth-child(3),
        .sd-enrollment-table--audit td:nth-child(3) {
            width: 16%;
        }

        .sd-enrollment-table--active th:nth-child(4),
        .sd-enrollment-table--active td:nth-child(4),
        .sd-enrollment-table--audit th:nth-child(4),
        .sd-enrollment-table--audit td:nth-child(4) {
            overflow-wrap: normal;
            padding-left: 8px !important;
            padding-right: 8px !important;
            white-space: nowrap;
            width: 24%;
        }

        .sd-enrollment-table .sd-enrollment-actions {
            flex-wrap: nowrap !important;
        }

        .sd-live-status {
            min-height: 20px;
        }

        .sd-modal-entity,
        .sd-modal-entity strong,
        .sd-modal-entity span,
        .sd-truncate-line {
            display: block;
            max-width: 100%;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .cd-shell .select2-container--default .select2-selection--single .select2-selection__rendered {
            max-width: 100%;
            overflow: hidden;
            padding-right: 34px;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .sd-parallel-grid {
            align-items: start;
            display: grid;
            gap: 20px;
            grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        }

        .sd-stack {
            display: flex;
            flex-direction: column;
            gap: 20px;
            min-width: 0;
        }

        .gape-subject-structure-panel {
            background: transparent;
            border: 0;
            border-radius: 0;
            padding: 0;
        }

        .gape-subject-detail-panel {
            background: transparent;
            border: 0;
            border-radius: 0;
            padding: 0;
        }

        .gape-subject-panel-content {
            padding: 0 !important;
        }

        .gape-subject-panel-header {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: space-between;
            margin-bottom: 16px;
        }

        .gape-subject-panel-heading {
            min-width: 0;
        }

        .gape-subject-panel-actions {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
        }

        .gape-subject-form-modal {
            max-width: 640px;
            width: calc(100% - 2rem);
        }

        .gape-subject-form-modal .modal-content {
            min-height: 0;
        }

        .gape-subject-form-modal .modal-body {
            min-width: 0;
            overflow-x: hidden;
        }

        .gape-subject-form-modal .select2-container {
            max-width: 100%;
            width: 100% !important;
        }

        .modal.gape-subject-form-modal-root {
            overflow: hidden;
        }

        .gape-subject-form-modal-root .gape-subject-select2-modal-portal {
            inset: 0;
            overflow: visible;
            pointer-events: none;
            position: fixed;
            z-index: 1;
        }

        .gape-subject-form-modal-root .gape-subject-select2-modal-portal > .select2-container {
            pointer-events: auto;
        }

        /* Select2 leaves an empty dropdown wrapper in the selection container.
           Keep its spacing in the field, rather than in the container that
           Select2 uses to calculate the dropdown's vertical origin. */
        .gape-subject-form-modal-root .gape-select-field {
            padding-bottom: 24px;
        }

        .gape-subject-form-modal-root .gape-select-field .select2-container > .dropdown-wrapper {
            display: none;
        }

        /* The creation dialogs are centered around their real content.  A
           maximum viewport height preserves access on short screens without
           imposing an artificial fixed frame. */
        .modal-dialog.gape-subject-form-modal--coordinator {
            max-width: 560px;
        }

        .modal-dialog.gape-subject-form-modal--association {
            max-width: 680px;
        }

        .modal-dialog.gape-subject-form-modal--coordinator .modal-content,
        .modal-dialog.gape-subject-form-modal--association .modal-content {
            display: flex;
            flex-direction: column;
            height: var(--gape-subject-modal-open-height, auto);
            max-height: calc(100vh - 2rem);
        }

        .modal-dialog.gape-subject-form-modal--coordinator .modal-content > form,
        .modal-dialog.gape-subject-form-modal--association .modal-content > form {
            display: flex;
            flex: 1 1 auto;
            flex-direction: column;
            min-height: 0;
            overflow: hidden;
        }

        .modal-dialog.gape-subject-form-modal--coordinator .modal-body,
        .modal-dialog.gape-subject-form-modal--association .modal-body {
            flex: 1 1 auto;
            min-height: 0;
            overflow-x: hidden;
            overflow-y: auto;
            scrollbar-gutter: stable;
        }

        .modal-dialog.gape-subject-form-modal--coordinator .modal-footer,
        .modal-dialog.gape-subject-form-modal--coordinator .modal-header,
        .modal-dialog.gape-subject-form-modal--association .modal-footer,
        .modal-dialog.gape-subject-form-modal--association .modal-header {
            flex: 0 0 auto;
        }

        .gape-subject-association-fields > [class*="col"] {
            min-width: 0;
        }

        .gape-subject-association-fields .gape-course-year-field .select2-selection__rendered {
            box-sizing: border-box;
            display: block !important;
            line-height: 1.4 !important;
            min-width: 0;
            overflow: hidden;
            padding: 0 48px 0 18px !important;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-subject-form-modal-root select[data-course-term-select]:disabled + .select2-container {
            cursor: not-allowed;
            pointer-events: none;
        }

        .gape-subject-form-modal-root select[data-course-term-select]:disabled + .select2-container .select2-selection--single {
            background-color: #f3f4f6 !important;
            border-color: #e5e7eb !important;
        }

        .gape-subject-form-modal-root select[data-course-term-select]:disabled + .select2-container .select2-selection__rendered,
        .gape-subject-form-modal-root select[data-course-term-select]:disabled + .select2-container .select2-selection__placeholder {
            color: #9ca3af !important;
        }

        .gape-subject-form-modal-root select[data-course-term-select]:disabled + .select2-container .select2-selection__arrow {
            opacity: .45;
        }

        .gape-subject-form-modal-root select[data-course-term-select]:not(:disabled) + .select2-container .select2-selection__placeholder {
            color: #334155 !important;
            opacity: 1;
        }

        .gape-subject-form-modal-root button[data-course-term-submit][disabled] {
            cursor: not-allowed;
            opacity: .58;
            pointer-events: none;
        }

        .gape-subject-required-toggle {
            min-height: 50px;
        }

        .gape-subject-structure-panel > .px-18.py-18,
        .gape-subject-detail-panel > .gape-subject-panel-content {
            padding: 0 !important;
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

        /* Coordinator Assignments and Subject Associations each expose four
           columns. Without this explicit four-column grid, they inherit the
           five-column Structure/Grade Sheets layout and leave its last column
           empty instead of using the full panel width. */
        .gape-enrollment-list-header,
        .gape-enrollment-row {
            grid-template-columns: minmax(250px, 1.25fr) minmax(220px, 1.05fr) minmax(135px, 0.55fr) minmax(92px, auto);
        }

        /* Structure has one shared column map at every depth. The third
           track is the intentional visual buffer that centres State between
           Class Groups and Actions without changing any layer's alignment. */
        #subject-structure {
            --gape-subject-structure-columns: minmax(260px, 1.15fr) minmax(130px, 0.38fr) minmax(110px, 0.28fr) minmax(0, 0.65fr) minmax(122px, auto);
        }

        #subject-structure .gape-enrollment-list-header,
        #subject-structure .gape-enrollment-row {
            grid-template-columns: var(--gape-subject-structure-columns);
        }

        @media (min-width: 1200px) {
            #subject-structure .gape-enrollment-list-header > :nth-child(3),
            #subject-structure .gape-enrollment-row > :nth-child(3) {
                grid-column: 4;
            }
        }

        /* Grade Sheets has longer course names than its numerical/state
           fields. Giving both context columns proportional space prevents the
           State-to-Actions void present in the inherited generic grid. */
        #subject-grade-sheets .gape-subject-grade-sheet-list-header,
        #subject-grade-sheets .gape-subject-grade-sheet-row,
        #subject-grade-sheets .gape-published-grade-sheets-node > .gape-structure-row {
            grid-template-columns: minmax(250px, 1.35fr) minmax(220px, 1.15fr) minmax(92px, .34fr) minmax(118px, .46fr) minmax(88px, auto);
        }

        #subject-grade-sheets .gape-subject-grade-sheet-actions {
            min-width: 88px;
        }

        #subject-grade-sheets .gape-subject-grade-sheet-actions {
            min-height: 34px;
        }

        #subject-grade-sheets .gape-subject-grade-sheet-actions > a,
        #subject-grade-sheets .gape-subject-grade-sheet-actions > button {
            align-items: center;
            border: 1px solid transparent !important;
            border-radius: 8px;
            display: inline-flex;
            height: 34px;
            justify-content: center;
            line-height: 1;
            width: 34px;
        }

        #subject-grade-sheets .gape-subject-grade-sheet-actions > a:hover,
        #subject-grade-sheets .gape-subject-grade-sheet-actions > button:hover {
            background: #eff6ff;
            border-color: rgba(var(--cd-primary-rgb), .28) !important;
        }

        .gape-structure-node {
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .gape-structure-node:hover {
            border-color: rgba(var(--cd-primary-rgb), 0.28) !important;
            box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
        }

        .gape-subject-occurrence-groups-panel {
            border-top: 1px solid var(--cd-border);
            margin-top: 16px;
            padding-top: 14px;
        }

        .gape-subject-occurrence-groups-panel__heading {
            align-items: center;
            display: flex;
            justify-content: space-between;
            padding: 0 0 8px;
        }

        .gape-subject-occurrence-groups-content {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .gape-subject-class-group-card {
            background: #fff !important;
            border: 1px solid var(--cd-border) !important;
            border-radius: 8px !important;
            padding: 12px 16px !important;
        }

        @media (min-width: 768px) {
            .gape-subject-class-group-card .gape-enrollment-row {
                /* Keep the child card visibly inset, exactly as the concrete
                   enrollment occurrence card in Course Details. */
                /* The card itself stays inset like the Course Details enrollment
                   card.  Extend only its grid tracks to the Structure header's
                   exact column lines, then restore the visible content inset. */
                margin-inline: -16px;
            }

            .gape-subject-class-group-card .gape-enrollment-row > :first-child {
                padding-left: 16px;
            }

            .gape-subject-class-group-card .gape-enrollment-row > :last-child {
                /* Keep the action icons inside the inset card without adding
                   layout width to the shared Actions grid track. */
                transform: translateX(-16px);
            }

            #subject-structure .gape-enrollment-list-header > :last-child,
            #subject-structure .gape-enrollment-row > :last-child {
                grid-column: 5;
            }

            /* Completed occurrence cards are visually nested, but their
               Class Groups, State and Actions tracks must land on the same
               column lines as the completed aggregate. */
            #subject-structure .gape-completed-class-groups-content > [data-subject-detail-occurrence-group] {
                margin-inline: -32px;
            }

            #subject-structure .gape-completed-class-groups-content > [data-subject-detail-occurrence-group] > .gape-enrollment-row > :first-child {
                padding-left: 32px;
            }

        }

        @media (min-width: 768px) and (max-width: 1199.98px) {
            /* A medium viewport cannot accommodate the desktop spacer without
               wrapping State badges. Keep the same four data columns aligned
               and remove only that spacer at this width. */
            #subject-structure {
                --gape-subject-structure-columns: minmax(200px, 1fr) minmax(95px, auto) minmax(110px, auto) minmax(122px, auto);
            }

            #subject-structure .gape-enrollment-list-header > :last-child,
            #subject-structure .gape-enrollment-row > :last-child {
                grid-column: 4;
            }
        }

        .gape-completed-class-groups-divider,
        .gape-published-grade-sheets-divider {
            align-items: center;
            color: #b91c1c;
            display: flex;
            font-size: 12px;
            font-weight: 600;
            gap: 10px;
            letter-spacing: 0.02em;
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
            margin-left: -18px;
            margin-right: -18px;
            margin-top: 16px;
        }

        .gape-completed-class-groups-content {
            padding: 16px 32px 0;
        }

        .gape-published-grade-sheets-divider {
            color: #15803d;
        }

        .gape-published-grade-sheets-divider::before,
        .gape-published-grade-sheets-divider::after {
            background: #bbf7d0;
            content: "";
            flex: 1;
            height: 1px;
        }

        .gape-published-grade-sheets-node {
            border-color: #bbf7d0 !important;
        }

        .gape-published-grade-sheets-panel {
            border-top: 1px solid #bbf7d0;
            margin-left: -18px;
            margin-right: -18px;
            margin-top: 16px;
        }

        .gape-published-grade-sheets-content {
            /* Published rows use the exact Grade Sheets grid, not a narrower
               indented area that shifts their columns to the left. */
            padding: 16px 0 0;
        }

        .cd-element-count {
            color: var(--cd-ink);
            font-size: 13px;
            font-weight: 500;
        }

        .cd-critical-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .cd-critical-card {
            background: #fff;
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            padding: 20px;
        }

        .cd-critical-card--archive {
            border-color: #fed7aa;
        }

        .cd-critical-card--delete {
            border-color: #fecaca;
        }

        .cd-critical-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            height: 44px;
            justify-content: center;
            width: 44px;
        }

        .cd-critical-icon--archive {
            background: #fff7ed;
            color: #c2410c;
        }

        .cd-critical-icon--delete {
            background: #fef2f2;
            color: #dc2626;
        }

        .aac-icon-button {
            align-items: center;
            background: transparent !important;
            border: 0;
            border-radius: 8px;
            color: var(--neutral-500) !important;
            display: inline-flex;
            height: 36px;
            justify-content: center;
            padding: 0;
            transition: background-color .2s ease, color .2s ease;
            width: 36px;
        }

        .aac-icon-button i {
            font-size: 22px;
            line-height: 1;
        }

        .aac-icon-button:hover,
        .aac-icon-button:focus-visible {
            background-color: var(--cd-primary-soft) !important;
            color: var(--cd-primary) !important;
            text-decoration: none;
        }

        .aac-download-menu {
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 18px 42px rgba(15, 23, 42, .12);
            min-width: 150px;
            padding: 8px;
            z-index: 1080;
        }

        .aac-download-menu .dropdown-item {
            align-items: center;
            border-radius: 7px;
            display: flex;
            font-size: 13px;
            font-weight: 600;
            gap: 8px;
            padding: 9px 10px;
        }

        .aac-grade-doc {
            background: #fff;
            color: #000;
            font-family: Arial, sans-serif;
            max-width: 100%;
            min-width: 0;
            overflow: hidden;
            padding: 0;
            width: 100%;
        }

        .cd-grade-sheet-doc {
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            padding: 16px;
        }

        .aac-grade-doc__head {
            border: 1px solid #000;
            display: grid;
            gap: 0 18px;
            grid-template-areas:
                "posted institution institution"
                "blank institution institution"
                "entity title period";
            grid-template-columns: 310px minmax(330px, 1fr) 300px;
            margin-bottom: 12px;
            min-height: 184px;
            padding: 12px;
            position: relative;
        }

        .aac-grade-doc__posted {
            align-self: start;
            font-size: 18px;
            font-weight: 700;
            grid-area: posted;
            line-height: 1.2;
            text-align: left;
            white-space: nowrap;
        }

        .aac-grade-doc__institution {
            align-items: flex-start;
            display: flex;
            grid-area: institution;
            justify-content: flex-start;
            min-width: 0;
        }

        .aac-grade-doc__media {
            align-items: flex-start;
            display: flex;
            flex: 0 0 150px;
            justify-content: center;
            min-width: 0;
        }

        .aac-grade-doc__image,
        .aac-grade-doc__placeholder {
            border-radius: 8px;
            height: 58px;
            width: 58px;
        }

        .aac-grade-doc__image {
            display: block;
            object-fit: cover;
        }

        .aac-grade-doc__placeholder {
            align-items: center;
            background: #eef2f7;
            border: 1px solid #d8e0ea;
            color: #1f3b57;
            display: inline-flex;
            flex-shrink: 0;
            font-size: 24px;
            justify-content: center;
        }

        .aac-grade-doc__brand {
            align-self: start;
            border-left: 2px solid #000;
            display: block;
            flex: 1 1 auto;
            font-size: 11px;
            font-weight: 400;
            line-height: 1.25;
            min-height: 72px;
            overflow: hidden;
            overflow-wrap: anywhere;
            padding-left: 20px;
            text-align: left;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .aac-grade-doc__brand strong {
            display: inline;
            font-size: 12px;
            font-weight: 800;
            line-height: 1.2;
            text-transform: uppercase;
        }

        .aac-grade-doc__brand strong::after {
            content: " | ";
            font-weight: 400;
        }

        .aac-grade-doc__brand span {
            display: inline;
        }

        .aac-grade-doc .gape-acronym-token {
            border-bottom: 0 !important;
            color: inherit !important;
            cursor: default !important;
            display: inline-block;
            max-width: 100%;
            overflow: hidden;
            padding: 0 !important;
            text-decoration: none !important;
            text-overflow: ellipsis;
            vertical-align: bottom;
            white-space: nowrap;
        }

        .aac-grade-doc__entity {
            align-self: end;
            border-bottom: 1px solid #000;
            font-size: 16px;
            grid-area: entity;
            line-height: 1.25;
            padding-bottom: 5px;
            text-align: left;
            white-space: nowrap;
        }

        .aac-grade-doc__head h3 {
            align-self: end;
            color: #000;
            font-size: 30px;
            font-weight: 800;
            grid-area: title;
            line-height: 1.1;
            margin: 0;
            text-align: center;
        }

        .aac-grade-doc__period {
            align-self: end;
            font-size: 16px;
            font-weight: 700;
            grid-area: period;
            line-height: 1.25;
            text-align: right;
        }

        .aac-grade-doc__table-wrap {
            max-width: 100%;
            min-width: 0;
            overflow-x: auto;
            overflow-y: hidden;
            padding-bottom: 8px;
            scrollbar-color: #94a3b8 #eef2f7;
            scrollbar-gutter: stable;
            scrollbar-width: thin;
        }

        .aac-grade-doc__table-wrap::-webkit-scrollbar {
            display: block;
            height: 8px;
        }

        .aac-grade-doc__table-wrap::-webkit-scrollbar-track {
            background: #eef2f7;
            border-radius: 999px;
        }

        .aac-grade-doc__table-wrap::-webkit-scrollbar-thumb {
            background: #94a3b8;
            border-radius: 999px;
        }

        .aac-grade-doc__table {
            border-collapse: collapse;
            margin: 0;
            min-width: var(--aac-grade-doc-min-width, 720px);
            table-layout: fixed;
            width: max(100%, var(--aac-grade-doc-min-width, 720px));
        }

        .aac-grade-doc__number-col {
            width: 70px;
        }

        .aac-grade-doc__name-col {
            width: 300px;
        }

        .aac-grade-doc__assessment-col {
            width: 118px;
        }

        .aac-grade-doc__final-col {
            width: 116px;
        }

        .aac-grade-doc__table th,
        .aac-grade-doc__table td {
            border: 1px solid #000;
            color: #000;
            font-size: 15px;
            line-height: 1.25;
            overflow: hidden;
            overflow-wrap: anywhere;
            padding: 7px 8px;
            text-align: center;
            text-overflow: ellipsis;
            vertical-align: middle;
            white-space: nowrap;
        }

        .aac-grade-doc__table th:nth-child(2),
        .aac-grade-doc__table td:nth-child(2) {
            min-width: 300px;
            text-align: left;
            white-space: normal;
        }

        .aac-grade-doc__table th {
            background: #fff;
            font-size: 15px;
            font-weight: 700;
            white-space: normal;
        }

        .aac-grade-doc__table th .gape-acronym-token {
            display: block;
            overflow-wrap: anywhere;
            text-overflow: clip;
            white-space: normal;
        }

        .aac-grade-doc__grade,
        .aac-grade-doc__final {
            font-weight: 700;
            white-space: nowrap;
        }

        .aac-grade-doc__alert {
            color: #6b4e00;
            font-size: 13px;
            margin-top: 10px;
        }

        .aac-grade-doc__footer {
            display: flex;
            justify-content: flex-end;
            margin-top: 34px;
            min-height: 84px;
        }

        .aac-grade-doc__footer > div:not(.aac-grade-doc__signature) {
            display: none;
        }

        .aac-grade-doc__signature {
            font-size: 18px;
            text-align: center;
            width: 300px;
        }

        .aac-grade-doc__signature em {
            border-bottom: 2px solid #000;
            display: block;
            height: 34px;
            margin-top: 10px;
            width: 100%;
        }

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
            border-radius: 12px;
            height: 76px;
            object-fit: cover;
            width: 96px;
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
            background-color: var(--cd-primary-soft);
            color: var(--cd-primary) !important;
            text-decoration: none;
        }

        .gape-structure-panel {
            background-color: var(--cd-primary-surface);
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            margin-top: 14px;
            padding: 14px;
        }

        .gape-subject-node {
            border-inline-start: 3px solid #0ea5e9;
        }

        .gape-class-group-node {
            border-inline-start: 3px solid #7c3aed;
        }

        .gape-lesson-node {
            border-inline-start: 3px solid var(--cd-primary);
        }

        .gape-room-node {
            border-inline-start: 3px solid #16a34a;
        }

        .gape-node-meta {
            color: var(--cd-muted);
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .cd-shell .hover-text-main-600:hover {
            color: var(--cd-primary) !important;
        }

        .cd-shell .hover-bg-main-50:hover,
        .cd-shell .bg-main-50 {
            background-color: var(--cd-primary-soft) !important;
        }

        .cd-shell .text-main-600 {
            color: var(--cd-primary) !important;
        }

        .og-activity-panel {
            background: #f8fafc;
            border: 1px solid var(--cd-border);
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
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            min-width: 0;
            padding: 14px;
        }

        .og-activity-column-header {
            align-items: center;
            border-bottom: 1px dashed var(--cd-border);
            display: flex;
            gap: 12px;
            justify-content: space-between;
            margin-bottom: 12px;
            padding-bottom: 12px;
        }

        .og-activity-heading {
            color: var(--cd-ink);
            font-size: 14px;
            font-weight: 700;
        }

        .og-activity-count {
            background: var(--cd-primary-soft);
            border-radius: 999px;
            color: var(--cd-primary);
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
            border: 1px solid var(--cd-border);
            border-radius: 8px;
            padding: 12px;
        }

        .og-activity-card--lesson {
            border-left: 3px solid var(--cd-primary);
        }

        .og-activity-card--assessment {
            border-left: 3px solid #7c3aed;
        }

        .og-activity-action {
            align-items: center;
            background: var(--cd-primary-soft);
            border-radius: 7px;
            color: var(--cd-primary);
            display: inline-flex;
            height: 30px;
            justify-content: center;
            width: 30px;
        }

        .og-activity-action:hover {
            background: var(--cd-primary);
            color: #fff;
        }

        .og-activity-empty {
            background: #f8fafc;
            border: 1px dashed var(--cd-border);
            border-radius: 8px;
            color: var(--cd-muted);
            font-size: 13px;
            padding: 18px;
            text-align: center;
        }

        @media (max-width: 1199.98px) {
            .cd-mode-grid,
            .sd-parallel-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }
        }

        @media (max-width: 991.98px) {
            .cd-info-grid,
            .sd-parallel-grid {
                grid-template-columns: 1fr;
            }

            .og-activity-grid,
            .cd-critical-grid {
                grid-template-columns: 1fr;
            }
        }

        @media (max-width: 767.98px) {
            .cd-mode-grid {
                grid-template-columns: 1fr;
            }

            .aac-grade-doc__head {
                grid-template-areas:
                    "posted"
                    "institution"
                    "entity"
                    "title"
                    "period";
                grid-template-columns: minmax(0, 1fr);
                row-gap: 12px;
            }

            .aac-grade-doc__institution {
                flex-direction: column;
                gap: 12px;
            }

            .aac-grade-doc__media {
                flex-basis: auto;
                justify-content: flex-start;
            }

            .aac-grade-doc__brand {
                border-left: 0;
                border-top: 2px solid #000;
                padding-left: 0;
                padding-top: 12px;
            }

            .aac-grade-doc__period {
                text-align: left;
            }

            .gape-structure-list-header {
                display: none;
            }

            .gape-structure-row {
                grid-template-columns: 1fr;
            }

            /* Override the desktop-only, scoped column grammars above.  Without
               this explicit scope, their higher specificity keeps nested rows
               in five columns on phones and clips the context cells. */
            #subject-structure .gape-structure-row,
            #subject-grade-sheets .gape-subject-grade-sheet-row,
            #subject-grade-sheets .gape-published-grade-sheets-node > .gape-structure-row {
                grid-template-columns: minmax(0, 1fr);
            }

            #subject-structure .gape-structure-row > *,
            #subject-grade-sheets .gape-subject-grade-sheet-row > *,
            #subject-grade-sheets .gape-published-grade-sheets-node > .gape-structure-row > * {
                min-width: 0;
            }
        }

        @media (max-width: 575.98px) {
            .cd-info-grid {
                grid-template-columns: 1fr;
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
            <div class="cd-shell ${subject.inactive ? 'cd-shell--inactive' : ''} px-24 py-24 flex-grow-1" id="subjectDetailLiveRoot">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <section class="cd-surface cd-hero px-24 py-24 mb-20">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-center gap-16 min-w-0">
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
                            <div class="min-w-0">
                                <div class="d-flex align-items-center gap-10 flex-wrap mb-10">
                                    <span class="${subject.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                                        <c:out value="${subject.stateLabel}"/>
                                    </span>
                                    <span class="cd-soft-badge px-14 py-7 rounded-pill text-13">
                                        <i class="ph ph-book-open-text me-6"></i><c:out value="${subject.ectsLabel}"/>
                                    </span>
                                </div>
                                <h2 class="text-28 fw-semibold text-neutral-800 mb-8"><c:out value="${subject.name}"/></h2>
                                <span class="text-14 text-neutral-500">
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                    &middot; <c:out value="${subject.organizationContextHtml}" escapeXml="false"/>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${pageContext.request.contextPath}${subjectBasePath}" class="cd-outline-button">
                                <i class="ph ph-arrow-left me-8"></i>Back
                            </a>
                            <button type="button" class="cd-outline-button" data-bs-toggle="modal" data-bs-target="#subjectSetupModal">
                                <i class="ph ph-sliders-horizontal me-8"></i>Setup
                            </button>
                            <c:if test="${canModifySubject}">
                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="cd-outline-button">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                            <c:if test="${subject.inactive and canModifySubject}">
                                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/unarchive" method="post" class="m-0" data-subject-live-form data-subject-live-panel="structure">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="cd-primary-button border-0">Activate</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </section>

                <div class="cd-mode-grid mb-20" role="tablist" aria-label="Subject detail views">
                    <button type="button" class="cd-mode-card is-active" data-subject-tab="structure" aria-selected="true">
                        <span class="cd-mode-icon cd-soft-badge text-24 mb-14"><i class="ph ph-tree-structure"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Class Groups</span>
                        <span class="text-13 cd-count-text fw-semibold">${activeClassGroupCount} active class groups</span>
                    </button>
                    <button type="button" class="cd-mode-card" data-subject-tab="allocations" aria-selected="false">
                        <span class="cd-mode-icon bg-info-50 text-info-600 text-24 mb-14"><i class="ph ph-user-switch"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Coordinators</span>
                        <span class="text-13 cd-count-text fw-semibold">${fn:length(coordinatorAssignments)} coordinators</span>
                    </button>
                    <button type="button" class="cd-mode-card" data-subject-tab="associations" aria-selected="false">
                        <span class="cd-mode-icon bg-success-50 text-success-600 text-24 mb-14"><i class="ph ph-link-simple"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Associations</span>
                        <span class="text-13 cd-count-text fw-semibold">${fn:length(subjectCourseAssociations)} courses</span>
                    </button>
                    <button type="button" class="cd-mode-card" data-subject-tab="grade-sheet" aria-selected="false">
                        <span class="cd-mode-icon bg-warning-50 text-warning-600 text-24 mb-14"><i class="ph ph-seal-check"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Grade Sheets</span>
                        <span class="text-13 cd-count-text fw-semibold">${subjectGradeSheetSheetCount} grade sheets</span>
                    </button>
                </div>

                <main class="min-w-0">
                    <div class="cd-tab-panel" data-subject-panel="structure" id="subject-structure">
                        <section class="cd-surface px-22 py-22 mb-20 gape-subject-detail-surface">
                            <div class="gape-subject-structure-panel gape-subject-detail-panel gape-course-structure-panel">
                                <div class="px-18 py-18 gape-subject-panel-content" data-subject-detail-sort-root>
                                    <div class="gape-subject-panel-header">
                                        <div class="gape-subject-panel-heading">
                                            <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Structure</h3>
                                            <span class="text-13 text-neutral-500">Class groups grouped by course occurrence and their activities.</span>
                                        </div>
                                        <div class="gape-subject-panel-actions">
                                            <div class="dropdown">
                                                <button type="button"
                                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                                        data-bs-toggle="dropdown"
                                                        data-bs-auto-close="outside"
                                                        data-subject-detail-sort-toggle
                                                        aria-expanded="false">
                                                    <i class="ph ph-sort-ascending"></i>Sort by
                                                </button>
                                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>Name</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false">
                                                            <span>Date</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>State</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                </ul>
                                            </div>
                                            <c:if test="${not subject.inactive}">
                                                <c:choose>
                                                    <c:when test="${canCreateClassGroupsForSubject}">
                                                        <a href="${pageContext.request.contextPath}/learning/class-groups/new?subjectId=${subject.id}&amp;subjectContextId=${subject.id}" class="cd-primary-button cd-section-create-button">
                                                            <i class="ph ph-plus-circle me-8"></i>New Group
                                                        </a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="cd-disabled-action-wrapper"
                                                              tabindex="0"
                                                              data-bs-toggle="tooltip"
                                                              data-bs-placement="top"
                                                              title="<c:out value='${classGroupCreationUnavailableReason}'/>">
                                                            <button type="button" class="cd-primary-button cd-section-create-button" disabled aria-disabled="true">
                                                                <i class="ph ph-plus-circle me-8"></i>New Group
                                                            </button>
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                        </div>
                                    </div>

                                            <div class="gape-structure-list-header gape-enrollment-list-header">
                                                <span>Course Occurrence</span>
                                                <span>Class Groups</span>
                                                <span>State</span>
                                                <span class="text-end">Actions</span>
                                    </div>

                                    <div class="d-flex flex-column gap-12">
                                        <c:forEach var="occurrenceGroup" items="${activeClassGroupOccurrenceGroups}">
                                            <c:set var="occurrenceGroupPanelPrefix" value="subjectDetailActiveOccurrenceGroups" />
                                            <c:set var="occurrenceGroupIconClass" value="bg-success-50 text-success-600" />
                                            <c:set var="occurrenceGroupIcon" value="ph ph-calendar-dots" />
                                            <c:set var="occurrenceGroupStartsOpen" value="true" />
                                            <%@ include file="/WEB-INF/fragments/subject-class-group-occurrence-node.jspf" %>
                                        </c:forEach>

                                        <c:if test="${completedClassGroupCount gt 0}">
                                            <div class="gape-completed-class-groups-divider" aria-hidden="true"><span>Completed Class Groups</span></div>
                                            <article class="gape-structure-node gape-completed-class-groups-node border rounded-8 px-18 py-16 bg-white">
                                                <div class="gape-structure-row gape-enrollment-row">
                                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                                        <span class="bg-danger-50 text-danger-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-archive" aria-hidden="true"></i></span>
                                                        <div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block">Completed Class Groups</span><span class="gape-node-meta text-12"><span>Past class groups</span></span></div>
                                                    </div>
                                                    <div><span class="cd-element-count"><c:out value="${completedClassGroupCount}"/></span></div>
                                                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                                                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-gape-tree-toggle="completedSubjectClassGroups${subject.id}" data-gape-open-title="Hide completed class groups" data-gape-closed-title="Show completed class groups" aria-expanded="false" aria-controls="completedSubjectClassGroups${subject.id}" aria-label="Show completed class groups" title="Show completed class groups"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                                                </div>
                                                <div id="completedSubjectClassGroups${subject.id}" class="gape-completed-class-groups-panel d-none">
                                                    <div class="gape-completed-class-groups-content d-flex flex-column gap-10">
                                                        <c:forEach var="occurrenceGroup" items="${completedClassGroupOccurrenceGroups}">
                                                            <c:set var="occurrenceGroupPanelPrefix" value="subjectDetailCompletedOccurrenceGroups" />
                                                            <c:set var="occurrenceGroupIconClass" value="bg-neutral-20 text-neutral-600" />
                                                            <c:set var="occurrenceGroupIcon" value="ph ph-calendar-check" />
                                                            <c:set var="occurrenceGroupStartsOpen" value="true" />
                                                            <%@ include file="/WEB-INF/fragments/subject-class-group-occurrence-node.jspf" %>
                                                        </c:forEach>
                                                    </div>
                                                </div>
                                            </article>
                                        </c:if>

                                        <c:if test="${empty classGroups}">
                                            <div class="border border-neutral-30 rounded-8 px-20 py-28 text-center text-14 text-neutral-500 bg-white">
                                                No class groups created for this subject.
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </section>
                    </div>

                    <div class="cd-tab-panel" data-subject-panel="allocations" hidden>
                        <div data-subject-lazy-panel="allocations"
                             data-subject-lazy-url="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/allocations"
                             aria-busy="false">
                            <div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Open this tab to load coordinator assignments.</div>
                        </div>
                    </div>

                    <div class="cd-tab-panel" data-subject-panel="associations" hidden>
                        <!-- subject-course-associations-panel.jspf is loaded on demand by the fragment endpoint. -->
                        <div data-subject-lazy-panel="associations"
                             data-subject-lazy-url="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/associations"
                             aria-busy="false">
                            <div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Open this tab to load course associations.</div>
                        </div>
                    </div>

                    <div class="cd-tab-panel" data-subject-panel="grade-sheet" id="subject-grade-sheet" hidden>
                        <div data-subject-lazy-panel="grade-sheet"
                             data-subject-lazy-url="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/grade-sheet"
                             aria-busy="false">
                            <c:if test="${not subjectGradeSheetContentLoaded}">
                                <div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Open this tab to load grade sheets.</div>
                            </c:if>
                        <c:if test="${subjectGradeSheetContentLoaded}">
                        <c:choose>
                            <c:when test="${subjectGradeSheetAvailable}">
                                <c:forEach var="courseGroup" items="${subjectGradeSheetCourseGroups}">
                                    <c:forEach var="subjectGradeGroup" items="${courseGroup.subjects}">
                                        <section class="cd-surface px-22 py-22 mb-20">
                                            <c:set var="doc" value="${subjectGradeGroup.subjectDocument}"/>
                                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                                                <div>
                                                    <h3 class="text-20 fw-semibold text-neutral-800 mb-6">Grade sheet - <c:out value="${subjectGradeGroup.occurrenceLabel}"/></h3>
                                                    <span class="text-14 text-neutral-500" data-gape-datetime-display>
                                                        <c:out value="${courseGroup.courseName}"/> &middot;
                                                        <c:out value="${subjectGradeGroup.occurrenceDateRangeLabel}"/> &middot;
                                                        <c:out value="${subjectGradeGroup.sheetCount}"/> class sheets &middot;
                                                        <c:out value="${fn:length(doc.rows)}"/> students
                                                    </span>
                                                </div>
                                                <div class="d-flex align-items-center gap-10 flex-wrap">
                                                    <span class="${subjectGradeGroup.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                        <c:out value="${subjectGradeGroup.stateLabel}"/>
                                                    </span>
                                                    <div class="dropdown">
                                                        <button type="button"
                                                                class="aac-icon-button bg-main-50 text-main-600"
                                                                title="Download"
                                                                aria-label="Download subject grade sheet"
                                                                data-bs-toggle="dropdown"
                                                                aria-expanded="false">
                                                            <i class="ph ph-download-simple"></i>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end aac-download-menu">
                                                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/learning/grades/courses/${courseGroup.courseId}/occurrences/${subjectGradeGroup.occurrenceId}/subjects/${subjectGradeGroup.subjectId}/download?format=pdf"><i class="ph ph-file-pdf"></i>PDF</a></li>
                                                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/learning/grades/courses/${courseGroup.courseId}/occurrences/${subjectGradeGroup.occurrenceId}/subjects/${subjectGradeGroup.subjectId}/download?format=excel"><i class="ph ph-file-xls"></i>Excel</a></li>
                                                        </ul>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="aac-grade-doc cd-grade-sheet-doc">
                                                <div class="aac-grade-doc__head">
                                                    <div class="aac-grade-doc__posted">Published on: ___ / ___ / _____</div>
                                                    <div class="aac-grade-doc__institution">
                                                        <div class="aac-grade-doc__media">
                                                            <c:choose>
                                                                <c:when test="${doc.hasImage}">
                                                                    <img src="${pageContext.request.contextPath}/media/${doc.imagePath}?v=${mediaCacheVersion}" alt="" class="aac-grade-doc__image" onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                                                    <span class="aac-grade-doc__placeholder d-none"><i class="${doc.fallbackIconClass}"></i></span>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <span class="aac-grade-doc__placeholder"><i class="${doc.fallbackIconClass}"></i></span>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </div>
                                                        <div class="aac-grade-doc__brand" title="<c:out value='${doc.contextTitle}'/>"><c:out value="${doc.contextHtml}" escapeXml="false"/></div>
                                                    </div>
                                                    <div class="aac-grade-doc__entity"><strong><c:out value="${doc.entityLabel}"/>:</strong> <c:out value="${doc.entityName}"/></div>
                                                    <h3><c:out value="${doc.title}"/></h3>
                                                    <div class="aac-grade-doc__period" data-gape-datetime-display>Academic period: <c:out value="${doc.periodLabel}"/></div>
                                                </div>
                                                <div class="aac-grade-doc__table-wrap" style="--aac-grade-doc-columns: ${doc.variableColumnCount}; --aac-grade-doc-min-width: ${doc.tableMinWidthPx}px;">
                                                    <table class="aac-grade-doc__table">
                                                        <colgroup>
                                                            <col class="aac-grade-doc__number-col">
                                                            <col class="aac-grade-doc__name-col">
                                                            <c:choose>
                                                                <c:when test="${empty doc.columns}">
                                                                    <col class="aac-grade-doc__assessment-col">
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:forEach var="column" items="${doc.columns}">
                                                                        <col class="aac-grade-doc__assessment-col">
                                                                    </c:forEach>
                                                                </c:otherwise>
                                                            </c:choose>
                                                            <col class="aac-grade-doc__final-col">
                                                        </colgroup>
                                                        <thead>
                                                        <tr>
                                                            <th>ID</th>
                                                            <th>Student name</th>
                                                            <c:choose>
                                                                <c:when test="${empty doc.columns}">
                                                                    <th>Assessments</th>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:forEach var="column" items="${doc.columns}">
                                                                        <th title="<c:out value='${column.headerTitle}'/>"><span class="gape-acronym-token"><c:out value="${column.headerLabel}"/></span></th>
                                                                    </c:forEach>
                                                                </c:otherwise>
                                                            </c:choose>
                                                            <th>Final grade</th>
                                                        </tr>
                                                        </thead>
                                                        <tbody>
                                                        <c:forEach var="row" items="${doc.rows}">
                                                            <tr>
                                                                <td><c:out value="${row.studentId}"/></td>
                                                                <td class="text-start"><c:out value="${row.studentName}"/></td>
                                                                <c:choose>
                                                                    <c:when test="${empty doc.columns}">
                                                                        <td class="aac-grade-doc__grade">-</td>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <c:forEach var="value" items="${row.values}">
                                                                            <td class="aac-grade-doc__grade"><c:out value="${value}"/></td>
                                                                        </c:forEach>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                                <td class="aac-grade-doc__final"><c:out value="${row.finalGrade}"/></td>
                                                            </tr>
                                                        </c:forEach>
                                                        <c:if test="${empty doc.rows}">
                                                            <tr>
                                                                <td colspan="${doc.documentColumnCount}" class="py-24 text-center text-neutral-500"><c:out value="${doc.emptyMessage}"/></td>
                                                            </tr>
                                                        </c:if>
                                                        </tbody>
                                                    </table>
                                                </div>
                                                <c:if test="${doc.hasAlert}">
                                                    <div class="aac-grade-doc__alert"><c:out value="${doc.alert}"/></div>
                                                </c:if>
                                                <div class="aac-grade-doc__footer">
                                                    <div class="aac-grade-doc__signature">
                                                        <span>Responsible person</span>
                                                        <em></em>
                                                    </div>
                                                </div>
                                            </div>
                                        </section>
                                    </c:forEach>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <section class="cd-surface px-22 py-22 mb-20">
                                    <div class="border border-neutral-30 rounded-8 px-20 py-32 text-center text-14 text-neutral-500 bg-white">
                                        No subject grade sheet is available for this subject.
                                    </div>
                                </section>
                            </c:otherwise>
                        </c:choose>
                        </c:if>
                        </div>
                    </div>
                </main>

                <c:if test="${canModifySubject}">
                    <section class="cd-surface px-22 py-22 mb-20">
                        <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                            <div>
                                <h3 class="text-20 fw-semibold text-neutral-800 mb-6">Critical Actions</h3>
                                <span class="text-14 text-neutral-500">Restricted lifecycle operations for this subject.</span>
                            </div>
                            <span class="bg-warning-50 text-warning-600 px-14 py-8 rounded-pill text-13">
                                <i class="ph ph-lock-key me-6"></i>Requires confirmation
                            </span>
                        </div>
                        <div class="cd-critical-grid">
                            <c:choose>
                                <c:when test="${subject.inactive}">
                                    <article class="cd-critical-card cd-critical-card--archive">
                                        <div class="d-flex align-items-start gap-14 mb-16">
                                            <span class="cd-critical-icon cd-critical-icon--archive text-22"><i class="ph ph-arrow-clockwise"></i></span>
                                            <div>
                                                <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Activate Subject</h4>
                                                <p class="text-14 text-neutral-500 mb-0">Restores this item to the active academic catalogue.</p>
                                            </div>
                                        </div>
                                        <button type="button" class="cd-primary-button" data-bs-toggle="modal" data-bs-target="#activateSubject">Activate</button>
                                    </article>
                                </c:when>
                                <c:otherwise>
                                    <article class="cd-critical-card cd-critical-card--archive">
                                        <div class="d-flex align-items-start gap-14 mb-16">
                                            <span class="cd-critical-icon cd-critical-icon--archive text-22"><i class="ph ph-archive"></i></span>
                                            <div>
                                                <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Deactivate Subject</h4>
                                                <p class="text-14 text-neutral-500 mb-0">Temporarily disabled this item while preserving its data.</p>
                                            </div>
                                        </div>
                                        <button type="button" class="cd-warning-button" data-bs-toggle="modal" data-bs-target="#archiveSubject">Deactivate</button>
                                    </article>
                                </c:otherwise>
                            </c:choose>
                            <article class="cd-critical-card cd-critical-card--delete">
                                <div class="d-flex align-items-start gap-14 mb-16">
                                    <span class="cd-critical-icon cd-critical-icon--delete text-22"><i class="ph ph-trash"></i></span>
                                    <div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Delete Subject</h4>
                                        <p class="text-14 text-neutral-500 mb-0">Permanently removes this item when the database allows it.</p>
                                    </div>
                                </div>
                                <button type="button" class="cd-danger-button" data-bs-toggle="modal" data-bs-target="#deleteSubject">Delete</button>
                            </article>
                        </div>
                    </section>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<div class="modal fade" id="subjectSetupModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <div>
                    <h5 class="modal-title text-18 fw-semibold mb-4">Setup</h5>
                    <span class="text-13 text-neutral-500">Subject identity, context and academic summary.</span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="d-flex align-items-center gap-10 flex-wrap mb-18">
                    <span class="${subject.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                        <c:out value="${subject.stateLabel}"/>
                    </span>
                    <span class="cd-soft-badge px-14 py-7 rounded-pill text-13">
                        <i class="ph ph-book-open-text me-6"></i><c:out value="${subject.ectsLabel}"/>
                    </span>
                </div>
                <div class="cd-info-grid">
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Name</span><strong class="text-14 text-neutral-800"><c:out value="${subject.name}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Acronym</span><strong class="text-14 text-neutral-800"><c:out value="${subject.acronym}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">State</span><strong class="text-14 text-neutral-800"><c:out value="${subject.stateLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">ECTS</span><strong class="text-14 text-neutral-800"><c:out value="${subject.ectsLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Max final grade</span><strong class="text-14 text-neutral-800"><c:out value="${subject.finalGradeMaxLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Workload</span><strong class="text-14 text-neutral-800"><c:out value="${subject.workloadHoursLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Organization</span><strong class="text-14 text-neutral-800"><c:out value="${subject.organizationContextHtml}" escapeXml="false"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Courses</span><strong class="text-14 text-neutral-800">${fn:length(subjectCourseAssociations)}</strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Class groups</span><strong class="text-14 text-neutral-800">${fn:length(classGroups)}</strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Coordinators</span><strong class="text-14 text-neutral-800">${fn:length(coordinatorAssignments)}</strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Grade sheets</span><strong class="text-14 text-neutral-800">${subjectGradeSheetSheetCount}</strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Photo</span><strong class="text-14 text-neutral-800"><c:out value="${subject.hasPhoto ? 'Available' : 'Missing'}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Identifier</span><strong class="text-14 text-neutral-800">#<c:out value="${subject.id}"/></strong></div>
                </div>
            </div>
        </div>
    </div>
</div>

<c:if test="${canModifySubject}">
<c:if test="${subject.inactive}">
<div class="modal fade" id="activateSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Activate Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm activation of <strong><c:out value="${subject.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/unarchive" method="post" class="m-0" data-subject-live-form data-subject-live-panel="structure" data-subject-lifecycle-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="cd-primary-button">Activate</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<c:if test="${not subject.inactive}">
<div class="modal fade" id="archiveSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Deactivate Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm deactivation of <strong><c:out value="${subject.name}"/></strong>? Existing associations and historical records remain available.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/archive" method="post" class="m-0" data-subject-live-form data-subject-live-panel="structure" data-subject-lifecycle-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="cd-warning-button">Deactivate</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<div class="modal fade" id="deleteSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the subject if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="cd-danger-button">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    window.GapeSubjectModalSelect2Portal = {
        dropdownParent: function (modal) {
            if (!modal || !modal.classList.contains('gape-subject-form-modal-root')) {
                return null;
            }
            var portal = modal.querySelector('[data-gape-subject-select2-modal-portal]');
            if (!portal) {
                portal = document.createElement('div');
                portal.className = 'gape-subject-select2-modal-portal';
                portal.setAttribute('data-gape-subject-select2-modal-portal', '');
                modal.appendChild(portal);
            }
            return portal;
        }
    };
</script>
<script src="${pageContext.request.contextPath}/assets/js/gape-subject-course-select.js?v=20260717-standard-selection-1"></script>
<script src="${pageContext.request.contextPath}/assets/js/gape-course-year-select.js?v=20260717-course-year-dependency-2"></script>
<script src="${pageContext.request.contextPath}/assets/js/gape-course-term-select.js?v=20260717-course-year-dependency-4"></script>
<script>
    (function () {
        var hashTarget = {
            '#subject-coordinators': 'allocations',
            '#subject-allocations': 'allocations',
            '#course-associations': 'associations',
            '#subject-associations': 'associations',
            '#subject-class-groups': 'structure',
            '#subject-structure': 'structure',
            '#subject-grade-sheet': 'grade-sheet'
        };

        var subjectPanelHash = {
            allocations: '#subject-coordinators',
            associations: '#subject-associations',
            structure: '#subject-structure',
            'grade-sheet': '#subject-grade-sheet'
        };

        function subjectRoot() {
            return document.getElementById('subjectDetailLiveRoot');
        }

        function subjectTabs() {
            var root = subjectRoot();
            return root ? Array.prototype.slice.call(root.querySelectorAll('[data-subject-tab]')) : [];
        }

        function subjectPanels() {
            var root = subjectRoot();
            return root ? Array.prototype.slice.call(root.querySelectorAll('[data-subject-panel]')) : [];
        }

        function currentSubjectPanel() {
            var root = subjectRoot();
            var active = root ? root.querySelector('[data-subject-tab].is-active') : null;
            return active ? active.getAttribute('data-subject-tab') : 'structure';
        }

        function loadingSpinnerMarkup() {
            return '<span class="gape-subject-loading-spinner" role="status" aria-label="Loading">'
                    + '<i class="ph ph-spinner-gap" aria-hidden="true"></i>'
                    + '<span class="visually-hidden">Loading</span>'
                    + '</span>';
        }

        function setSubjectControlLoading(control, busy) {
            if (!control || control.tagName !== 'BUTTON') {
                return;
            }
            if (busy) {
                if (control.dataset.subjectLoadingOriginalHtml === undefined) {
                    control.dataset.subjectLoadingOriginalHtml = control.innerHTML;
                    control.dataset.subjectLoadingOriginalTitle = control.getAttribute('title') || '';
                    control.dataset.subjectLoadingOriginalAriaLabel = control.getAttribute('aria-label') || '';
                }
                control.innerHTML = loadingSpinnerMarkup();
                control.setAttribute('title', 'Loading');
                control.setAttribute('aria-label', 'Loading');
                control.setAttribute('aria-busy', 'true');
                control.disabled = true;
                return;
            }
            if (control.dataset.subjectLoadingOriginalHtml === undefined) {
                return;
            }
            control.innerHTML = control.dataset.subjectLoadingOriginalHtml;
            if (control.dataset.subjectLoadingOriginalTitle) {
                control.setAttribute('title', control.dataset.subjectLoadingOriginalTitle);
            } else {
                control.removeAttribute('title');
            }
            if (control.dataset.subjectLoadingOriginalAriaLabel) {
                control.setAttribute('aria-label', control.dataset.subjectLoadingOriginalAriaLabel);
            } else {
                control.removeAttribute('aria-label');
            }
            control.removeAttribute('aria-busy');
            if (control.dataset.subjectPreviousDisabled !== 'true') {
                control.disabled = false;
            }
            delete control.dataset.subjectLoadingOriginalHtml;
            delete control.dataset.subjectLoadingOriginalTitle;
            delete control.dataset.subjectLoadingOriginalAriaLabel;
        }

        function setSubjectTabLoading(name, busy) {
            var root = subjectRoot();
            var tab = root ? root.querySelector('[data-subject-tab="' + name + '"]') : null;
            var title = tab ? tab.querySelector('.cd-mode-card__title') : null;
            if (!tab || !title) {
                return;
            }
            if (busy) {
                if (title.dataset.subjectLoadingOriginalHtml === undefined) {
                    title.dataset.subjectLoadingOriginalHtml = title.innerHTML;
                }
                title.innerHTML = loadingSpinnerMarkup();
                tab.setAttribute('aria-busy', 'true');
                tab.disabled = true;
                return;
            }
            if (title.dataset.subjectLoadingOriginalHtml !== undefined) {
                title.innerHTML = title.dataset.subjectLoadingOriginalHtml;
                delete title.dataset.subjectLoadingOriginalHtml;
            }
            tab.removeAttribute('aria-busy');
            tab.disabled = false;
        }

        function revealSubjectPanel(target, activeTab) {
            var tabs = subjectTabs();
            var panels = subjectPanels();
            if (!activeTab) {
                activeTab = tabs.find(function (tab) {
                    return tab.getAttribute('data-subject-tab') === target;
                });
            }
            tabs.forEach(function (item) {
                var active = item === activeTab;
                item.classList.toggle('is-active', active);
                item.setAttribute('aria-selected', active ? 'true' : 'false');
            });
            panels.forEach(function (panel) {
                panel.hidden = panel.getAttribute('data-subject-panel') !== target;
            });
        }

        async function activateSubjectPanel(target, activeTab) {
            var root = subjectRoot();
            var lazyPanel = root ? root.querySelector('[data-subject-lazy-panel="' + target + '"]') : null;
            if (lazyPanel && lazyPanel.dataset.subjectLazyLoaded !== 'true') {
                var loaded = await loadSubjectLazyPanel(target);
                if (!loaded) {
                    return;
                }
            }
            revealSubjectPanel(target, activeTab);
        }

        function initializeSubjectFragment(scope) {
            configureSubjectDetailSorting(scope);
            initSubjectSelects(scope);
            if (window.GapeSubjectCourseSelect) {
                window.GapeSubjectCourseSelect.init(scope);
            }
            if (window.GapeCourseYearSelect) {
                window.GapeCourseYearSelect.init(scope);
            }
            if (window.GapeCourseTermSelect) {
                window.GapeCourseTermSelect.init(scope);
            }
        }

        async function loadSubjectLazyPanel(name) {
            var root = subjectRoot();
            var panel = root ? root.querySelector('[data-subject-lazy-panel="' + name + '"]') : null;
            if (!panel || panel.dataset.subjectLazyLoaded === 'true') {
                return true;
            }
            if (panel.dataset.subjectLazyLoading === 'true') {
                return false;
            }
            var url = panel.dataset.subjectLazyUrl;
            if (!url) {
                return;
            }
            panel.dataset.subjectLazyLoading = 'true';
            panel.setAttribute('aria-busy', 'true');
            setSubjectTabLoading(name, true);
            panel.innerHTML = '<div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Loadingâ€¦</div>';
            try {
                var response = await fetch(withCurrentSession(url), {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'text/html',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                if (!response.ok) {
                    throw new Error('The panel could not be loaded.');
                }
                panel.innerHTML = await response.text();
                panel.dataset.subjectLazyLoaded = 'true';
                initializeSubjectFragment(panel);
                return true;
            } catch (error) {
                panel.innerHTML = '<div class="cd-surface px-22 py-22 text-center text-14 text-danger-600">Unable to load this section. Please try again.</div>';
                return false;
            } finally {
                delete panel.dataset.subjectLazyLoading;
                panel.setAttribute('aria-busy', 'false');
                setSubjectTabLoading(name, false);
            }
        }

        async function loadClassGroupActivities(panel, trigger) {
            if (!panel || panel.dataset.subjectActivitiesLoaded === 'true') {
                return true;
            }
            if (panel.dataset.subjectActivitiesLoading === 'true') {
                return false;
            }
            var url = panel.dataset.subjectActivityUrl;
            if (!url) {
                return;
            }
            panel.dataset.subjectActivitiesLoading = 'true';
            panel.setAttribute('aria-busy', 'true');
            setSubjectControlLoading(trigger, true);
            panel.innerHTML = '<div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-neutral-500">Loading activitiesâ€¦</div>';
            try {
                var response = await fetch(withCurrentSession(url), {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'text/html',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                if (!response.ok) {
                    throw new Error('The activities could not be loaded.');
                }
                panel.innerHTML = await response.text();
                panel.dataset.subjectActivitiesLoaded = 'true';
                return true;
            } catch (error) {
                panel.innerHTML = '<div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-danger-600">Unable to load activities. Please try again.</div>';
                return false;
            } finally {
                delete panel.dataset.subjectActivitiesLoading;
                panel.setAttribute('aria-busy', 'false');
                setSubjectControlLoading(trigger, false);
            }
        }

        function setClassGroupActivitiesExpanded(button, target, expanded) {
            var icon = button.querySelector('i');
            var label = button.querySelector('[data-gape-toggle-label]');
            target.classList.toggle('d-none', !expanded);
            button.setAttribute('aria-expanded', String(expanded));
            button.setAttribute('title', expanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            button.setAttribute('aria-label', expanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            if (icon) {
                icon.classList.toggle('ph-caret-down', !expanded);
                icon.classList.toggle('ph-caret-up', expanded);
            }
            if (label) {
                label.textContent = expanded ? 'Hide' : 'Show';
            }
        }

        function initSubjectSelects(scope) {
            if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
                return;
            }
            window.jQuery(scope).find('.js-example-basic-single').each(function () {
                var select = window.jQuery(this);
                if (select.data('select2')) {
                    return;
                }
                if (select.hasClass('gape-eduall-select')) {
                    var modalParent = this.closest('.modal');
                    var options = {
                        width: '100%',
                        selectionCssClass: 'gape-eduall-selection',
                        dropdownCssClass: 'gape-eduall-select-dropdown'
                    };
                    if (modalParent) {
                        var portal = window.GapeSubjectModalSelect2Portal
                                ? window.GapeSubjectModalSelect2Portal.dropdownParent(modalParent)
                                : null;
                        options.dropdownParent = window.jQuery(portal || modalParent);
                    }
                    select.select2(options);
                    return;
                }
                select.select2();
            });
        }

        function subjectFormModalContent(modal) {
            if (!modal || !modal.classList.contains('gape-subject-form-modal-root')) {
                return null;
            }
            return modal.querySelector('.modal-content');
        }

        function lockSubjectFormModalGeometry(modal) {
            var content = subjectFormModalContent(modal);
            if (!content) {
                return;
            }
            var height = Math.round(content.getBoundingClientRect().height);
            if (height > 0) {
                content.style.setProperty('--gape-subject-modal-open-height', height + 'px');
            }
        }

        function unlockSubjectFormModalGeometry(modal) {
            var content = subjectFormModalContent(modal);
            if (content) {
                content.style.removeProperty('--gape-subject-modal-open-height');
            }
        }

        function configureSubjectDetailSorting(scope) {
            var roots = Array.prototype.slice.call((scope || document).querySelectorAll('[data-subject-detail-sort-root]'));
            if (!roots.length) {
                return;
            }

            function datasetKey(field) {
                return 'sort' + field.charAt(0).toUpperCase() + field.slice(1);
            }

            function oppositeDirection(direction) {
                return direction === 'asc' ? 'desc' : 'asc';
            }

            roots.forEach(function (root) {
                var lists = Array.prototype.slice.call(root.querySelectorAll('[data-subject-detail-sort-list]'));
                var sortOptions = Array.prototype.slice.call(root.querySelectorAll('[data-subject-detail-sort-option]'));
                var sortToggle = root.querySelector('[data-subject-detail-sort-toggle]');
                if (!lists.length || !sortOptions.length) {
                    return;
                }

                var listGroups = lists.map(function (list) {
                    var rows = Array.prototype.slice.call(list.children).filter(function (child) {
                        return child.hasAttribute('data-subject-detail-sort-row');
                    });
                    return {
                        list: list,
                        rows: rows.map(function (row, index) {
                            if (!row.dataset.sortIndex) {
                                row.dataset.sortIndex = String(index);
                            }
                            return { row: row, index: index };
                        })
                    };
                });
                var collator = new Intl.Collator(document.documentElement.lang || undefined, {
                    numeric: true,
                    sensitivity: 'base'
                });
                var activeField = null;
                var activeDirection = null;

                function originalIndex(group) {
                    return Number(group.row.dataset.sortIndex || group.index || '0');
                }

                function sortValue(group, field) {
                    return group.row.dataset[datasetKey(field)] || '';
                }

                function compareGroups(first, second) {
                    if (!activeField || !activeDirection) {
                        return originalIndex(first) - originalIndex(second);
                    }
                    var firstValue = sortValue(first, activeField);
                    var secondValue = sortValue(second, activeField);
                    var result = activeField === 'date'
                        ? Number(firstValue || '0') - Number(secondValue || '0')
                        : collator.compare(firstValue, secondValue);
                    if (result === 0) {
                        result = originalIndex(first) - originalIndex(second);
                    }
                    return activeDirection === 'desc' ? -result : result;
                }

                function renderRows() {
                    listGroups.forEach(function (listGroup) {
                        var sorted = listGroup.rows.slice().sort(compareGroups);
                        listGroup.rows.forEach(function (group) {
                            group.row.remove();
                        });
                        sorted.forEach(function (group) {
                            listGroup.list.appendChild(group.row);
                        });
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

                function closeDropdown(toggle) {
                    if (toggle && window.bootstrap && window.bootstrap.Dropdown) {
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

                updateOptionStates();
            });
        }

        function initSubjectDetail(preferredPanel) {
            var root = subjectRoot();
            if (!root) {
                return;
            }
            var target = preferredPanel || hashTarget[window.location.hash] || currentSubjectPanel() || 'structure';
            if (preferredPanel || hashTarget[window.location.hash]) {
                /* Keep the requested card visible while its lazy content loads,
                   including after a normal-navigation fallback. */
                revealSubjectPanel(target);
            }
            activateSubjectPanel(target);
            initializeSubjectFragment(root);
        }

        function cleanupSubjectModals() {
            document.querySelectorAll('.modal.show').forEach(function (modal) {
                if (window.bootstrap && window.bootstrap.Modal) {
                    var instance = window.bootstrap.Modal.getInstance(modal);
                    if (instance) {
                        instance.hide();
                    }
                }
                modal.classList.remove('show');
                modal.setAttribute('aria-hidden', 'true');
                modal.removeAttribute('aria-modal');
                modal.style.display = 'none';
            });
            document.querySelectorAll('.modal-backdrop').forEach(function (backdrop) {
                backdrop.remove();
            });
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('padding-right');
        }

        function setFormBusy(form, busy) {
            form.querySelectorAll('button, input, select, textarea').forEach(function (control) {
                if (busy) {
                    control.dataset.subjectPreviousDisabled = control.disabled ? 'true' : 'false';
                    control.disabled = true;
                } else if (control.dataset.subjectPreviousDisabled !== 'true') {
                    control.disabled = false;
                    delete control.dataset.subjectPreviousDisabled;
                }
            });
        }

        function withCurrentSession(url) {
            var target = new URL(url, window.location.href);
            var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
            if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1 && target.pathname.indexOf(match[1] + '/') === 0) {
                target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
            }
            return target.toString();
        }

        function replaceSubjectRootFrom(html, panel) {
            var parser = new DOMParser();
            var doc = parser.parseFromString(html, 'text/html');
            var nextRoot = doc.getElementById('subjectDetailLiveRoot');
            var root = subjectRoot();
            if (!nextRoot || !root) {
                return false;
            }
            cleanupSubjectModals();
            ['subjectSetupModal', 'activateSubject', 'archiveSubject', 'deleteSubject'].forEach(function (id) {
                var currentModal = document.getElementById(id);
                var nextModal = doc.getElementById(id);
                if (currentModal) {
                    currentModal.remove();
                }
                if (nextModal) {
                    document.body.appendChild(nextModal);
                }
            });
            root.replaceWith(nextRoot);
            initSubjectDetail(panel);
            return true;
        }

        function isSubjectLiveFormInScope(form, root) {
            return !!form && !!root && (root.contains(form) || form.hasAttribute('data-subject-lifecycle-form'));
        }

        async function reloadSubjectRoot(panel) {
            var response = await fetch(withCurrentSession(window.location.href.split('#')[0]), {
                method: 'GET',
                credentials: 'same-origin',
                headers: {
                    'Accept': 'text/html',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            var html = await response.text();
            return replaceSubjectRootFrom(html, panel);
        }

        function subjectPanelUrl(url, panel) {
            var target = new URL(url, window.location.href);
            var hash = subjectPanelHash[panel];
            if (hash) {
                target.hash = hash;
            }
            return withCurrentSession(target.toString());
        }

        async function submitSubjectLiveForm(form, trigger) {
            if (form.dataset.subjectSubmitting === 'true') {
                return;
            }
            if (form.reportValidity && !form.reportValidity()) {
                return;
            }
            form.dataset.subjectSubmitting = 'true';
            var panel = form.getAttribute('data-subject-live-panel') || currentSubjectPanel();
            var formData = new FormData(form);
            setFormBusy(form, true);
            setSubjectControlLoading(trigger || form.querySelector('button[type="submit"]'), true);
            try {
                var response = await fetch(withCurrentSession(form.action), {
                    method: (form.method || 'POST').toUpperCase(),
                    body: formData,
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'text/html',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                var html = await response.text();
                if (!replaceSubjectRootFrom(html, panel) && !await reloadSubjectRoot(panel)) {
                    window.location.assign(subjectPanelUrl(response.url || form.action, panel));
                }
            } catch (error) {
                try {
                    if (await reloadSubjectRoot(panel)) {
                        return;
                    }
                } catch (ignored) {
                    // Fall back to a normal navigation if the live refresh cannot recover.
                }
                window.location.assign(subjectPanelUrl(window.location.href, panel));
            } finally {
                setSubjectControlLoading(trigger || form.querySelector('button[type="submit"]'), false);
                setFormBusy(form, false);
                delete form.dataset.subjectSubmitting;
            }
        }

        document.addEventListener('shown.bs.modal', function (event) {
            lockSubjectFormModalGeometry(event.target);
        });

        document.addEventListener('hidden.bs.modal', function (event) {
            unlockSubjectFormModalGeometry(event.target);
        });

        document.addEventListener('click', function (event) {
            var tab = event.target.closest('[data-subject-tab]');
            var root = subjectRoot();
            if (tab && root && root.contains(tab)) {
                activateSubjectPanel(tab.getAttribute('data-subject-tab'), tab);
                return;
            }

            var submitControl = event.target.closest('form[data-subject-live-form] button[type="submit"], form[data-subject-live-form] input[type="submit"]');
            if (submitControl) {
                var form = submitControl.form || submitControl.closest('form[data-subject-live-form]');
                if (isSubjectLiveFormInScope(form, root)) {
                    event.preventDefault();
                    if (form.reportValidity && !form.reportValidity()) {
                        return;
                    }
                    submitSubjectLiveForm(form, submitControl);
                    return;
                }
            }

            var button = event.target.closest('[data-gape-tree-toggle]');
            if (!button || !root || !root.contains(button)) {
                return;
            }
            var target = document.getElementById(button.dataset.gapeTreeToggle);
            if (!target) {
                return;
            }
            var isExpanded = !target.classList.contains('d-none');
            if (isExpanded) {
                setClassGroupActivitiesExpanded(button, target, false);
                return;
            }
            if (target.matches('[data-subject-class-group-activities]')
                    && target.dataset.subjectActivitiesLoaded !== 'true') {
                loadClassGroupActivities(target, button).then(function (loaded) {
                    if (loaded) {
                        setClassGroupActivitiesExpanded(button, target, true);
                    }
                });
                return;
            }
            setClassGroupActivitiesExpanded(button, target, true);
        });

        document.addEventListener('submit', function (event) {
            var form = event.target.closest('form[data-subject-live-form]');
            var root = subjectRoot();
            if (!isSubjectLiveFormInScope(form, root)) {
                return;
            }
            event.preventDefault();
            submitSubjectLiveForm(form, event.submitter);
        });

        initSubjectDetail();
    })();
</script>
</body>
</html>
