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
    <title>GAPE - Course Details</title>
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

        .gape-course-loading-spinner {
            align-items: center;
            display: inline-flex;
            justify-content: center;
            min-height: 1.2em;
            min-width: 1.2em;
        }

        .gape-course-loading-spinner > i {
            animation: gape-course-loading-spin 0.8s linear infinite;
            display: inline-block;
        }

        @keyframes gape-course-loading-spin {
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

        .gape-course-modal-root .select2-container--default .select2-selection--single .select2-selection__rendered {
            max-width: 100%;
            overflow: hidden;
            padding-right: 34px;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-course-modal-root .gape-select-field.is-disabled select,
        .gape-course-modal-root .gape-select-field.is-disabled .select2-container {
            cursor: not-allowed;
            pointer-events: none;
        }

        .gape-course-modal-root .gape-select-field.is-disabled .select2-selection--single {
            background-color: #f3f4f6 !important;
            border-color: #e5e7eb !important;
        }

        .gape-course-modal-root .gape-select-field.is-disabled .select2-selection__rendered {
            color: #9ca3af !important;
        }

        .gape-course-modal-root .gape-select-field.is-disabled .select2-selection__arrow {
            opacity: 0.45;
        }

        .gape-course-modal-root .gape-select-field.is-available .select2-selection__rendered {
            color: #1f2937 !important;
        }

        .gape-course-modal-root .gape-select-field.is-available .select2-selection__placeholder {
            color: #1f2937 !important;
            opacity: 1;
        }

        .gape-course-modal-root [data-course-occurrence-submit]:disabled {
            background-color: #e5e7eb !important;
            border-color: #e5e7eb !important;
            box-shadow: none !important;
            color: #9ca3af !important;
            cursor: not-allowed;
            opacity: 1;
            pointer-events: none;
        }

        /* Keep every Course Details dialog fixed around its opening geometry.
           Select2 menus are rendered in a portal, outside the scrollable body,
           so opening or choosing an option never changes the dialog position. */
        .modal.gape-course-modal-root {
            overflow: hidden;
        }

        .gape-course-modal-root .gape-course-select2-modal-portal {
            inset: 0;
            overflow: visible;
            pointer-events: none;
            position: fixed;
            z-index: 1;
        }

        .gape-course-modal-root .gape-course-select2-modal-portal > .select2-container {
            pointer-events: auto;
        }

        /* Keep Select2's helper spacing in the field, never in the element
           that it uses as the dropdown placement reference. */
        .gape-course-modal-root .gape-select-field {
            padding-bottom: 24px;
        }

        .gape-course-modal-root .gape-select-field .select2-container > .dropdown-wrapper {
            display: none;
        }

        .modal-dialog.gape-course-modal-dialog {
            max-height: calc(100vh - 2rem);
        }

        .modal-dialog.gape-course-modal-dialog--association {
            max-width: 680px;
        }

        .modal-dialog.gape-course-modal-dialog .modal-content {
            display: flex;
            flex-direction: column;
            height: var(--gape-course-modal-open-height, auto);
            max-height: calc(100vh - 2rem);
        }

        .modal-dialog.gape-course-modal-dialog .modal-content > form {
            display: flex;
            flex: 1 1 auto;
            flex-direction: column;
            min-height: 0;
            overflow: hidden;
        }

        .modal-dialog.gape-course-modal-dialog .modal-body {
            flex: 1 1 auto;
            min-height: 0;
            overflow-x: hidden;
            overflow-y: auto;
            scrollbar-gutter: stable;
        }

        .modal-dialog.gape-course-modal-dialog .modal-footer,
        .modal-dialog.gape-course-modal-dialog .modal-header {
            flex: 0 0 auto;
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

        .cd-associate-button {
            background: #8b5cf6;
            border-color: #8b5cf6;
        }

        .cd-associate-button:hover {
            background: #7c3aed;
            border-color: #7c3aed;
        }

        .cd-section-create-button {
            min-height: 44px;
            min-width: 184px;
            padding: 10px 18px;
            white-space: nowrap;
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

        .gape-course-structure-panel {
            background: transparent;
            border: 0;
            border-radius: 0;
            padding: 0;
        }

        .gape-course-structure-panel > .px-18.py-18 {
            padding: 0 !important;
        }

        .gape-structure-list-header,
        .gape-structure-row {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: minmax(280px, 1.7fr) minmax(120px, 0.55fr) minmax(120px, 0.55fr) minmax(120px, 0.55fr) minmax(150px, auto);
        }

        .gape-occurrence-list-header,
        .gape-occurrence-row {
            grid-template-columns: minmax(260px, 1.65fr) minmax(120px, 0.5fr) minmax(135px, 0.58fr) minmax(70px, auto);
        }

        .gape-enrollment-list-header,
        .gape-enrollment-row {
            grid-template-columns: minmax(250px, 1.25fr) minmax(220px, 1.05fr) minmax(135px, 0.55fr) minmax(92px, auto);
        }

        .gape-course-enrollment-state-summaries {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .gape-course-enrollment-show {
            /* Kept only as a semantic hook.  Enrollment toggles use the
               same compact caret-only control as Occurrences. */
        }

        .gape-course-enrollment-student-panel {
            border-top: 1px solid var(--cd-border);
            margin-top: 16px;
            padding-top: 14px;
        }

        .gape-course-enrollment-student-panel__heading {
            align-items: center;
            display: flex;
            justify-content: space-between;
            padding: 0 0 8px;
        }

        .gape-course-enrollment-occurrence-card {
            /* Match the concrete class-group layer in Subject Details: every
               enrollment occurrence is a clearly bounded child record. */
            background: #fff !important;
            border: 1px solid var(--cd-border) !important;
            border-radius: 8px !important;
            padding: 12px 16px !important;
        }

        @media (min-width: 768px) {
            /* The concrete occurrence is nested in the student's card.  Let
               its grid use the same horizontal span as the student row, so
               the period and state share the exact Enrollment and State
               columns while retaining the card's visual inset. */
            .gape-course-enrollment-occurrence-card .gape-enrollment-row {
                margin-inline: -17px;
            }

            .gape-course-enrollment-occurrence-card .gape-enrollment-row > :first-child {
                padding-left: 17px;
            }

            .gape-course-enrollment-occurrence-card .gape-enrollment-row > :last-child {
                padding-right: 17px;
            }
        }

        .gape-occurrence-periods-panel {
            border-top: 1px solid var(--cd-border);
            margin-top: 16px;
            padding-top: 16px;
        }

        .gape-occurrence-period-card {
            padding: 12px 0;
        }

        .gape-occurrence-period-card + .gape-occurrence-period-card {
            border-top: 1px solid var(--cd-border);
        }

        .gape-completed-occurrences-divider {
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

        .gape-completed-occurrences-divider::before,
        .gape-completed-occurrences-divider::after {
            background: #fecaca;
            content: "";
            flex: 1;
            height: 1px;
        }

        .gape-completed-occurrences-node {
            border-color: #fecaca !important;
        }

        .gape-completed-occurrences-panel {
            border-top: 1px solid #fecaca;
            margin-left: -18px;
            margin-right: -18px;
            margin-top: 16px;
        }

        .gape-completed-occurrences-content {
            padding: 16px 32px 0;
        }

        .gape-academic-year-picker {
            background: #f8fafc;
            border: 1px solid #dbe3ee;
            border-radius: 10px;
            padding: 14px;
        }

        .gape-academic-year-picker.is-invalid {
            border-color: #dc3545;
            box-shadow: 0 0 0 0.15rem rgba(220, 53, 69, 0.1);
        }

        .gape-academic-year-picker__header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 12px;
        }

        .gape-academic-year-picker__grid {
            display: grid;
            gap: 8px;
            grid-template-columns: repeat(3, minmax(0, 1fr));
        }

        .gape-academic-year-option {
            background: #fff;
            border: 1px solid #dbe3ee;
            border-radius: 8px;
            color: #334155;
            font-size: 13px;
            font-weight: 600;
            min-height: 42px;
            padding: 8px 6px;
            transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
        }

        .gape-academic-year-option:hover:not(:disabled),
        .gape-academic-year-option:focus-visible:not(:disabled) {
            border-color: var(--cd-primary);
            color: var(--cd-primary-dark);
            outline: 0;
        }

        .gape-academic-year-option.is-selected {
            background: var(--cd-primary);
            border-color: var(--cd-primary);
            color: #fff;
        }

        .gape-academic-year-option:disabled {
            background: #f1f5f9;
            border-color: #e2e8f0;
            color: #94a3b8;
            cursor: not-allowed;
        }

        .gape-academic-year-selection {
            background: #f8fafc;
            border: 1px solid #dbe3ee;
            border-radius: 8px;
            min-height: 44px;
            padding: 11px 14px;
        }

        .gape-structure-list-header {
            color: #475569;
            font-size: 13px;
            font-weight: 600;
            /* The header and every tree depth deliberately share the same
               inline reference.  This keeps counters, states and actions
               vertically aligned when a subject is expanded. */
            padding: 0 18px 12px;
        }

        .gape-structure-node {
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }

        .gape-structure-node:hover {
            border-color: rgba(var(--cd-primary-rgb), 0.28) !important;
            box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
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

        .gape-structure-panel .gape-structure-row {
            /* Nested cards retain their border and hierarchy, while their
               grid columns use the full parent width just like the subject. */
            margin-left: -16px;
            /* Keep the nested action slot visually inside its column. */
            margin-right: -8px;
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

        @media (max-width: 991.98px) {
            .cd-info-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
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

            .gape-academic-year-picker__grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
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
            <div class="cd-shell ${course.inactive ? 'cd-shell--inactive' : ''} px-24 py-24 flex-grow-1" id="courseDetailLiveRoot">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <section class="cd-surface cd-hero px-24 py-24 mb-20">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-center gap-16 min-w-0">
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
                            <div class="min-w-0">
                                <div class="d-flex align-items-center gap-10 flex-wrap mb-10">
                                    <span class="${course.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                                        <c:out value="${course.stateLabel}"/>
                                    </span>
                                    <span class="cd-soft-badge px-14 py-7 rounded-pill text-13">
                                        <i class="ph ph-books me-6"></i><c:out value="${course.typeLabel}"/>
                                    </span>
                                </div>
                                <h2 class="text-28 fw-semibold text-neutral-800 mb-8"><c:out value="${course.name}"/></h2>
                                <span class="text-14 text-neutral-500" title="<c:out value='${course.courseManagementContextTitle}'/>">
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${course.name}'/>"><c:out value="${course.acronym}"/></span>
                                    &middot; <c:out value="${course.courseManagementContextHtml}" escapeXml="false"/>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${pageContext.request.contextPath}${courseBasePath}" class="cd-outline-button">
                                <i class="ph ph-arrow-left me-8"></i>Back
                            </a>
                            <button type="button" class="cd-outline-button" data-bs-toggle="modal" data-bs-target="#courseSetupModal">
                                <i class="ph ph-sliders-horizontal me-8"></i>Setup
                            </button>
                            <c:if test="${canModifyCourse}">
                                <a href="${pageContext.request.contextPath}${courseBasePath}/${course.id}/edit" class="cd-outline-button">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                            <c:if test="${course.inactive and canModifyCourse}">
                                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/unarchive" method="post" class="m-0" data-course-live-form data-course-live-panel="structure">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <input type="hidden" name="returnTo" value="${currentReturnTo}">
                                    <button type="submit" class="cd-primary-button border-0">Activate</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </section>

                <c:if test="${not courseEctsConsistent}">
                    <div class="alert alert-warning rounded-8 border-0 mb-20" role="alert">
                        The sum of the subject ECTS in this course is <strong><c:out value="${courseSubjectEctsTotalLabel}"/></strong>, but the course requires <strong><c:out value="${courseEctsTargetLabel}"/></strong>. Certificates for this course cannot be completed or receive a final grade until these values match.
                    </div>
                </c:if>

                <div class="cd-mode-grid mb-20" role="tablist" aria-label="Course detail views">
                    <button type="button" class="cd-mode-card is-active" data-course-tab="structure" aria-selected="true">
                        <span class="cd-mode-icon cd-soft-badge text-24 mb-14"><i class="ph ph-tree-structure"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Subjects</span>
                        <span class="text-13 cd-count-text fw-semibold"><c:out value="${fn:length(courseSubjects)}"/> subjects</span>
                    </button>
                    <button type="button" class="cd-mode-card" data-course-tab="occurrences" aria-selected="false">
                        <span class="cd-mode-icon bg-success-50 text-success-600 text-24 mb-14"><i class="ph ph-calendar-dots"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Occurrences</span>
                        <span class="text-13 cd-count-text fw-semibold">${activeCourseOccurrenceCount} active occurrences</span>
                    </button>
                    <button type="button" class="cd-mode-card" data-course-tab="enrollments" aria-selected="false">
                        <span class="cd-mode-icon bg-info-50 text-info-600 text-24 mb-14"><i class="ph ph-student"></i></span>
                        <span class="text-20 fw-semibold text-neutral-800 d-block mb-12 cd-mode-card__title">Enrollments</span>
                        <span class="text-13 cd-count-text fw-semibold">${activeCourseEnrollmentCount} active students</span>
                    </button>
                </div>

                <main class="min-w-0">
                    <div class="cd-tab-panel" data-course-panel="structure">
                        <section class="cd-surface px-22 py-22 mb-20">
                            <div class="gape-course-structure-panel">
                                <div class="px-18 py-18" data-course-detail-sort-root>
                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-16">
                                        <div>
                                            <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Structure</h3>
                                            <span class="text-13 text-neutral-500">Subjects associated with this course, their curricular position and class groups.</span>
                                        </div>
                                        <div class="d-flex align-items-center gap-12 flex-wrap">
                                            <div class="dropdown">
                                                <button type="button"
                                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                                        data-bs-toggle="dropdown"
                                                        data-bs-auto-close="outside"
                                                        data-course-detail-sort-toggle
                                                        aria-expanded="false">
                                                    <i class="ph ph-sort-ascending"></i>Sort by
                                                </button>
                                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>Name</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false">
                                                            <span>Date</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>State</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                </ul>
                                            </div>
                                        <c:if test="${canManageCourseChildren}">
                                            <button type="button" class="cd-primary-button cd-section-create-button cd-associate-button" data-course-lazy-modal="associate-subject" data-course-lazy-modal-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/associate-subject">
                                                <i class="ph ph-link-simple me-8"></i>Associate Subject
                                            </button>
                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/new?courseId=${course.id}" class="cd-primary-button cd-section-create-button">
                                                <i class="ph ph-plus-circle me-8"></i>Create Subject
                                            </a>
                                        </c:if>
                                        </div>
                                    </div>

                                    <div class="gape-structure-list-header">
                                        <span>Subject</span>
                                        <span>ECTS</span>
                                        <span>Active Class Groups</span>
                                        <span>Subject State</span>
                                        <span class="text-end">Actions</span>
                                    </div>

                                    <div class="d-flex flex-column gap-12" data-course-detail-sort-list>
                                        <c:forEach var="association" items="${courseSubjects}" varStatus="associationLoop">
                                            <c:set var="subjectClassGroups" value="${classGroupsBySubject[association.subjectId]}"/>
                                            <c:set var="canModifySubject" value="${canModifySubjectById[association.subjectId]}"/>
                                            <c:set var="subjectPhotoUrl" value=""/>
                                            <c:if test="${association.subject.hasPhoto}">
                                                <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${association.subject.photo}?v=${mediaCacheVersion}"/>
                                            </c:if>
                                            <article class="gape-structure-node gape-subject-node border border-neutral-30 rounded-8 px-18 py-16 bg-white"
                                                     data-course-detail-sort-row
                                                     data-sort-index="${associationLoop.index}"
                                                     data-sort-name="<c:out value='${association.subjectName}'/>"
                                                     data-sort-date="${association.subjectId}"
                                                     data-sort-status="<c:out value='${association.subject.stateLabel}'/>">
                                                <div class="gape-structure-row">
                                                    <div class="d-flex align-items-start gap-12 min-w-0">
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
                                                        <div class="min-w-0">
                                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                <span class="gape-acronym-token" tabindex="0" title="<c:out value='${association.subjectName}'/>"><c:out value="${association.subjectAcronym}"/></span>
                                                                <span class="ms-4"><c:out value="${association.subjectName}"/></span>
                                                            </a>
                                                            <span class="gape-node-meta text-12">
                                                                <span><c:out value="${association.curricularPositionLabel}"/></span>
                                                                <span><c:out value="${association.mandatoryLabel}"/></span>
                                                            </span>
                                                        </div>
                                                    </div>
                                                    <div class="text-13 text-neutral-500"><c:out value="${association.subjectEctsLabel}"/></div>
                                                    <div><span class="cd-element-count"><c:out value="${activeClassGroupCountBySubject[association.subjectId]}"/></span></div>
                                                    <div>
                                                        <span class="${association.subject.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                            <c:out value="${association.subject.stateLabel}"/>
                                                        </span>
                                                    </div>
                                                    <div class="d-flex align-items-center gap-10 flex-wrap justify-content-end">
                                                        <button type="button"
                                                                class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                title="Show class groups"
                                                                aria-label="Show class groups"
                                                                aria-expanded="false"
                                                                aria-controls="courseDetailSubjectGroups${association.subjectId}"
                                                                data-gape-tree-toggle="courseDetailSubjectGroups${association.subjectId}"
                                                                data-gape-open-title="Hide class groups"
                                                                data-gape-closed-title="Show class groups">
                                                            <i class="ph ph-caret-down"></i>
                                                        </button>
                                                        <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail">
                                                            <i class="ph ph-eye"></i>
                                                        </a>
                                                        <c:if test="${canModifySubject}">
                                                            <a href="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit">
                                                                <i class="ph ph-pencil-simple-line"></i>
                                                            </a>
                                                            <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourseDetailSubject${course.id}_${association.subjectId}">
                                                                <i class="ph ph-trash"></i>
                                                            </button>
                                                        </c:if>
                                                    </div>
                                                </div>

                                                <div id="courseDetailSubjectGroups${association.subjectId}"
                                                     class="gape-structure-panel d-none"
                                                     data-course-subject-groups
                                                     data-course-subject-groups-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects/${association.subjectId}/class-groups"
                                                     aria-busy="false">
                                                    <div class="text-13 text-neutral-500">Open class groups to load their details.</div>
                                                </div>

                                                <c:if test="${courseDetailEagerPanels}">
                                                <div id="courseDetailEagerSubjectGroups${association.subjectId}" class="gape-structure-panel d-none">
                                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
                                                        <div>
                                                            <h4 class="text-16 fw-medium text-neutral-700 mb-4">Class Groups</h4>
                                                            <span class="text-13 text-neutral-500">
                                                                <c:out value="${association.subjectName}"/> &middot; <c:out value="${fn:length(subjectClassGroups)}"/> class groups
                                                            </span>
                                                        </div>
                                                        <c:if test="${canManageCourseChildren}">
                                                            <a href="${pageContext.request.contextPath}/learning/class-groups/new?courseId=${course.id}&subjectId=${association.subjectId}" class="text-20 text-neutral-500 hover-text-main-600" title="New Group" aria-label="New Group">
                                                                <i class="ph ph-plus-circle"></i>
                                                            </a>
                                                        </c:if>
                                                    </div>
                                                    <div class="d-flex flex-column gap-10">
                                                        <c:forEach var="classGroup" items="${subjectClassGroups}">
                                                            <c:set var="canModifyClassGroup" value="${canModifyClassGroupById[classGroup.id]}" />
                                                            <c:set var="canManageClassGroupStructure" value="${canManageClassGroupStructureById[classGroup.id]}" />
                                                            <c:set var="activityPanelId" value="courseDetailClassGroupActivities${association.subjectId}_${classGroup.id}" />
                                                            <article class="gape-structure-node gape-class-group-node border border-neutral-30 rounded-8 px-16 py-12 bg-white">
                                                                <div class="gape-structure-row">
                                                                    <div class="d-flex align-items-start gap-10 min-w-0">
                                                                        <span class="bg-warning-50 text-warning-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-users-three"></i></span>
                                                                        <div class="min-w-0">
                                                                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                                                <c:out value="${classGroup.code}"/>
                                                                            </a>
                                                                            <span class="gape-node-meta text-12">
                                                                                <span><c:out value="${classGroup.modalityLabel}"/></span>
                                                                                <span><c:out value="${classGroup.shift}"/></span>
                                                                                <span data-gape-datetime-display><c:out value="${classGroup.dateRangeLabel}"/></span>
                                                                                <span><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled</span>
                                                                            </span>
                                                                        </div>
                                                                    </div>
                                                                    <div class="text-13 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/></div>
                                                                    <div><span class="cd-element-count">${classGroupActivityCountByClassGroup[classGroup.id]}</span></div>
                                                                    <div>
                                                                        <span class="${classGroup.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                            <c:out value="${classGroup.stateLabel}"/>
                                                                        </span>
                                                                    </div>
                                                                    <div class="d-flex align-items-center gap-10 flex-wrap justify-content-end">
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
                                                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail">
                                                                            <i class="ph ph-eye"></i>
                                                                        </a>
                                                                        <c:if test="${canModifyClassGroup}">
                                                                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit">
                                                                                <i class="ph ph-pencil-simple-line"></i>
                                                                            </a>
                                                                        </c:if>
                                                                        <c:if test="${canManageClassGroupStructure}">
                                                                            <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourseDetailClassGroup${classGroup.id}">
                                                                                <i class="ph ph-trash"></i>
                                                                            </button>
                                                                        </c:if>
                                                                    </div>
                                                                </div>
                                                                <div id="${activityPanelId}"
                                                                     class="d-none"
                                                                     data-course-class-group-activities
                                                                     data-course-activity-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/class-groups/${classGroup.id}/activities"
                                                                     aria-busy="false">
                                                                    <div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-neutral-500">Open activities to load their details.</div>
                                                                </div>
                                                            </article>

                                                            <c:if test="${canManageClassGroupStructure}">
                                                                <div class="modal fade" id="deleteCourseDetailClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true">
                                                                    <div class="modal-dialog modal-dialog-centered">
                                                                        <div class="modal-content rounded-8 border-0">
                                                                            <div class="modal-header border-neutral-30">
                                                                                <h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5>
                                                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                                            </div>
                                                                            <div class="modal-body">
                                                                                <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${classGroup.code}"/></strong> if it has no dependencies.</p>
                                                                            </div>
                                                                            <div class="modal-footer border-neutral-30">
                                                                                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                                                                                <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/delete" method="post" class="m-0" data-course-live-form data-course-live-panel="structure">
                                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                                    <button type="submit" class="cd-danger-button">Delete</button>
                                                                                </form>
                                                                            </div>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </c:if>
                                                        </c:forEach>
                                                        <c:if test="${empty subjectClassGroups}">
                                                            <div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">
                                                                No class groups registered for this subject in this course.
                                                            </div>
                                                        </c:if>
                                                    </div>
                                                </div>
                                                </c:if>
                                            </article>

                                            <c:if test="${canModifySubject}">
                                                <div class="modal fade" id="deleteCourseDetailSubject${course.id}_${association.subjectId}" tabindex="-1" aria-hidden="true">
                                                    <div class="modal-dialog modal-dialog-centered">
                                                        <div class="modal-content rounded-8 border-0">
                                                            <div class="modal-header border-neutral-30">
                                                                <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                            </div>
                                                            <div class="modal-body">
                                                                <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${association.subjectName}"/></strong> if it has no dependencies.</p>
                                                            </div>
                                                            <div class="modal-footer border-neutral-30">
                                                                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                                                                <form action="${pageContext.request.contextPath}${subjectBasePath}/${association.subjectId}/delete" method="post" class="m-0">
                                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                    <button type="submit" class="cd-danger-button">Delete</button>
                                                                </form>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </c:if>
                                        </c:forEach>
                                        <c:if test="${empty courseSubjects}">
                                            <div class="border border-neutral-30 rounded-8 px-20 py-28 text-center text-14 text-neutral-500 bg-white">
                                                No subjects associated with this course.
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <c:if test="${courseDetailEagerPanels and canManageCourseChildren}">
                            <div class="modal fade" id="associateCourseSubjectModal" tabindex="-1" aria-labelledby="associateCourseSubjectModalLabel" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content rounded-12 border-0">
                                        <div class="modal-header border-neutral-30">
                                            <h5 class="modal-title text-18 fw-semibold" id="associateCourseSubjectModalLabel">Associate Subject</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects" method="post" data-course-live-form data-course-live-panel="structure" data-course-associate-subject-form>
                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                            <input type="hidden" name="returnTo" value="${currentReturnTo}#course-structure">
                                            <div class="modal-body">
                                                <p class="text-14 text-neutral-600 mb-20">Select an existing subject and its curricular position in this course.</p>
                                                <div class="mb-20 gape-select-field">
                                                    <label for="courseDetailSubjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                    <select id="courseDetailSubjectId" name="subjectId" required data-course-associate-subject class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                                        <option value="">Select subject</option>
                                                        <c:forEach var="subject" items="${availableSubjectOptions}">
                                                            <option value="${subject.id}"><c:out value="${subject.name}"/></option>
                                                        </c:forEach>
                                                    </select>
                                                    <c:if test="${empty availableSubjectOptions}">
                                                        <span class="d-block text-12 text-neutral-500 mt-8">There are no available subjects to associate.</span>
                                                    </c:if>
                                                </div>
                                                <div class="row gy-3">
                                                    <div class="col-sm-6 gape-select-field">
                                                        <label for="courseDetailSubjectYear" class="fw-medium text-base text-neutral-800 mb-12">Course year</label>
                                                        <select id="courseDetailSubjectYear" name="curricularYear" required disabled data-course-year-select data-fixed-duration-years="${course.durationYears}" data-placeholder="Select course year" data-course-associate-year class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                                            <option value="">Select course year</option>
                                                            <c:forEach var="yearOption" items="${course.durationYearOptions}">
                                                                <option value="${yearOption.value}"><c:out value="${yearOption.label}"/></option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                    <div class="col-sm-6 gape-select-field">
                                                        <label for="courseDetailSubjectTerm" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                                                        <select id="courseDetailSubjectTerm" name="term" required disabled data-course-associate-period class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                                            <option value="">Select period</option>
                                                            <c:forEach var="period" items="${coursePeriodTemplates}">
                                                                <option value="${period.term}" data-course-associate-period-year="${period.curricularYear}"><c:out value="${period.termLabel}"/></option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                    <div class="col-12">
                                                        <div class="form-check common-check mb-0">
                                                            <input class="form-check-input" type="checkbox" id="courseDetailSubjectMandatory" name="mandatory" value="true">
                                                            <label class="form-check-label fw-medium" for="courseDetailSubjectMandatory">Mandatory subject</label>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="modal-footer border-neutral-30">
                                                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                                                <button type="submit" class="cd-primary-button border-0">Associate Subject</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:if>
                    </div>

                    <div class="cd-tab-panel" data-course-panel="enrollments" hidden>
                        <div data-course-lazy-panel="enrollments"
                             data-course-lazy-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/enrollments"
                             aria-busy="false">
                            <div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Open this tab to load course enrollments.</div>
                        </div>
                    </div>

                    <div class="cd-tab-panel" data-course-panel="occurrences" id="course-occurrences" hidden>
                        <div data-course-lazy-panel="occurrences"
                             data-course-lazy-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/occurrences"
                             aria-busy="false">
                            <div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Open this tab to load course occurrences.</div>
                        </div>
                    </div>

                    <c:if test="${courseDetailEagerPanels}">
                    <div class="cd-tab-panel" data-course-panel="enrollments" hidden>
                        <c:set var="courseEnrollmentEmbedded" value="${true}" />
                        <%@ include file="/WEB-INF/fragments/course-enrollment-management.jspf" %>
                    </div>

                    <div class="cd-tab-panel" data-course-panel="occurrences" id="course-occurrences" hidden>
                        <section class="cd-surface px-22 py-22 mb-20">
                            <div class="gape-course-structure-panel">
                                <div class="px-18 py-18" data-course-detail-sort-root>
                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-16">
                                        <div>
                                            <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Occurrences</h3>
                                            <span class="text-13 text-neutral-500">Academic years are calculated from the configured course periods.</span>
                                        </div>
                                        <div class="d-flex align-items-center gap-12 flex-wrap">
                                            <div class="dropdown">
                                                <button type="button"
                                                        class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                                        data-bs-toggle="dropdown"
                                                        data-bs-auto-close="outside"
                                                        data-course-detail-sort-toggle
                                                        aria-expanded="false">
                                                    <i class="ph ph-sort-ascending"></i>Sort by
                                                </button>
                                                <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>Name</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false">
                                                            <span>Date</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                    <li>
                                                        <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                            <span>State</span>
                                                            <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                        </button>
                                                    </li>
                                                </ul>
                                            </div>
                                        <c:if test="${canManageCourseChildren}">
                                            <button type="button" class="cd-primary-button cd-section-create-button" data-bs-toggle="modal" data-bs-target="#newCourseOccurrenceModal">
                                                <i class="ph ph-plus-circle me-8"></i>New Occurrence
                                            </button>
                                        </c:if>
                                        </div>
                                    </div>

                                    <div class="gape-structure-list-header gape-occurrence-list-header">
                                        <span>Academic year</span>
                                        <span>Periods</span>
                                        <span>State</span>
                                        <span class="text-end">Actions</span>
                                    </div>

                                    <c:set var="completedOccurrenceCount" value="0"/>
                                    <c:forEach var="occurrence" items="${courseOccurrences}">
                                        <c:if test="${occurrence.stateValue eq 'completed'}">
                                            <c:set var="completedOccurrenceCount" value="${completedOccurrenceCount + 1}"/>
                                        </c:if>
                                    </c:forEach>

                                    <div class="d-flex flex-column gap-12">
                                        <div class="d-flex flex-column gap-12" data-course-detail-sort-list>
                                        <c:forEach var="occurrence" items="${courseOccurrences}" varStatus="occurrenceLoop">
                                            <c:if test="${occurrence.stateValue ne 'completed'}">
                                                <article class="gape-structure-node border border-neutral-30 rounded-8 px-18 py-16 bg-white"
                                                         data-course-detail-sort-row
                                                         data-sort-index="${occurrenceLoop.index}"
                                                         data-sort-name="<c:out value='${occurrence.academicYearLabel}'/>"
                                                         data-sort-date="${fn:replace(occurrence.startsAtValue, '-', '')}"
                                                         data-sort-status="<c:out value='${occurrence.stateLabel}'/>">
                                                    <div class="gape-structure-row gape-occurrence-row">
                                                        <div class="d-flex align-items-center gap-12 min-w-0">
                                                            <span class="bg-success-50 text-success-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-calendar-dots"></i></span>
                                                            <div class="min-w-0">
                                                                <span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${occurrence.academicYearLabel}"/></span>
                                                                <span class="gape-node-meta text-12"><span>Course occurrence</span></span>
                                                            </div>
                                                        </div>
                                                        <div><span class="cd-element-count"><c:out value="${occurrence.periodCount}"/></span></div>
                                                        <div>
                                                            <span class="${occurrence.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13">
                                                                <c:out value="${occurrence.stateLabel}"/>
                                                            </span>
                                                        </div>
                                                        <div class="d-flex justify-content-end">
                                                            <button type="button"
                                                                    class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                    data-gape-tree-toggle="occurrencePeriods${occurrence.id}"
                                                                    data-gape-open-title="Hide periods"
                                                                    data-gape-closed-title="Show periods"
                                                                    aria-expanded="false"
                                                                    aria-controls="occurrencePeriods${occurrence.id}"
                                                                    aria-label="Show periods"
                                                                    title="Show periods"><i class="ph ph-caret-down" aria-hidden="true"></i></button>
                                                        </div>
                                                    </div>
                                                    <div id="occurrencePeriods${occurrence.id}" class="gape-occurrence-periods-panel d-none">
                                                        <div class="d-flex flex-column">
                                                            <c:forEach var="period" items="${occurrence.periods}">
                                                                <article class="gape-occurrence-period-card">
                                                                    <div class="gape-structure-row gape-occurrence-row gape-occurrence-period-row">
                                                                        <div class="fw-medium text-14 text-neutral-700"><c:out value="${period.label}"/></div>
                                                                        <div class="text-13 text-neutral-500" data-gape-datetime-display><c:out value="${period.dateRangeLabel}"/></div>
                                                                        <div><span class="${period.stateBadgeClass} px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${period.stateLabel}"/></span></div>
                                                                        <div aria-hidden="true"></div>
                                                                    </div>
                                                                </article>
                                                            </c:forEach>
                                                        </div>
                                                    </div>
                                                </article>
                                            </c:if>
                                        </c:forEach>
                                        </div>

                                        <c:if test="${completedOccurrenceCount gt 0}">
                                            <div class="gape-completed-occurrences-divider" aria-hidden="true"><span>Completed occurrences</span></div>
                                            <article class="gape-structure-node gape-completed-occurrences-node border rounded-8 px-18 py-16 bg-white">
                                                <div class="gape-structure-row gape-occurrence-row">
                                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                                        <span class="bg-danger-50 text-danger-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-archive" aria-hidden="true"></i></span>
                                                        <div class="min-w-0">
                                                            <span class="fw-medium text-14 text-neutral-700 d-block">Completed occurrences</span>
                                                            <span class="gape-node-meta text-12"><span>Past academic years</span></span>
                                                        </div>
                                                    </div>
                                                    <div><span class="cd-element-count"><c:out value="${completedOccurrenceCount}"/></span></div>
                                                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                                                    <div class="d-flex justify-content-end">
                                                        <button type="button"
                                                                class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                data-gape-tree-toggle="completedCourseOccurrences"
                                                                data-gape-open-title="Hide completed occurrences"
                                                                data-gape-closed-title="Show completed occurrences"
                                                                aria-expanded="false"
                                                                aria-controls="completedCourseOccurrences"
                                                                aria-label="Show completed occurrences"
                                                                title="Show completed occurrences"><i class="ph ph-caret-down" aria-hidden="true"></i></button>
                                                    </div>
                                                </div>
                                                <div id="completedCourseOccurrences" class="gape-completed-occurrences-panel d-none">
                                                    <div class="gape-completed-occurrences-content d-flex flex-column gap-10" data-course-detail-sort-list>
                                                        <c:forEach var="occurrence" items="${courseOccurrences}" varStatus="completedOccurrenceLoop">
                                                            <c:if test="${occurrence.stateValue eq 'completed'}">
                                                                <article class="gape-structure-node border border-neutral-30 rounded-8 px-16 py-12 bg-white"
                                                                         data-course-detail-sort-row
                                                                         data-sort-index="${completedOccurrenceLoop.index}"
                                                                         data-sort-name="<c:out value='${occurrence.academicYearLabel}'/>"
                                                                         data-sort-date="${fn:replace(occurrence.startsAtValue, '-', '')}"
                                                                         data-sort-status="<c:out value='${occurrence.stateLabel}'/>">
                                                                    <div class="gape-structure-row gape-occurrence-row">
                                                                        <div class="d-flex align-items-center gap-10 min-w-0">
                                                                            <span class="bg-neutral-20 text-neutral-600 cd-mode-icon text-18 line-height-1"><i class="ph ph-calendar-check" aria-hidden="true"></i></span>
                                                                            <div class="min-w-0">
                                                                                <span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${occurrence.academicYearLabel}"/></span>
                                                                                <span class="gape-node-meta text-12"><span>Course occurrence</span></span>
                                                                            </div>
                                                                        </div>
                                                                        <div><span class="cd-element-count"><c:out value="${occurrence.periodCount}"/></span></div>
                                                                        <div><span class="${occurrence.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13"><c:out value="${occurrence.stateLabel}"/></span></div>
                                                                        <div class="d-flex justify-content-end">
                                                                            <button type="button"
                                                                                    class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600"
                                                                                    data-gape-tree-toggle="occurrencePeriods${occurrence.id}"
                                                                                    data-gape-open-title="Hide periods"
                                                                                    data-gape-closed-title="Show periods"
                                                                                    aria-expanded="false"
                                                                                    aria-controls="occurrencePeriods${occurrence.id}"
                                                                                    aria-label="Show periods"
                                                                                    title="Show periods"><i class="ph ph-caret-down" aria-hidden="true"></i></button>
                                                                        </div>
                                                                    </div>
                                                                    <div id="occurrencePeriods${occurrence.id}" class="gape-occurrence-periods-panel d-none">
                                                                        <div class="d-flex flex-column">
                                                                            <c:forEach var="period" items="${occurrence.periods}">
                                                                                <article class="gape-occurrence-period-card">
                                                                                    <div class="gape-structure-row gape-occurrence-row gape-occurrence-period-row">
                                                                                        <div class="fw-medium text-14 text-neutral-700"><c:out value="${period.label}"/></div>
                                                                                        <div class="text-13 text-neutral-500" data-gape-datetime-display><c:out value="${period.dateRangeLabel}"/></div>
                                                                                        <div><span class="${period.stateBadgeClass} px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${period.stateLabel}"/></span></div>
                                                                                        <div aria-hidden="true"></div>
                                                                                    </div>
                                                                                </article>
                                                                            </c:forEach>
                                                                        </div>
                                                                    </div>
                                                                </article>
                                                            </c:if>
                                                        </c:forEach>
                                                    </div>
                                                </div>
                                                </c:if>
                                            </article>

                                        <c:if test="${empty courseOccurrences}">
                                            <div class="border border-neutral-30 rounded-8 px-18 py-28 text-center text-14 text-neutral-500">No occurrences are available for this course yet.</div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <c:if test="${canManageCourseChildren}">
                            <div class="modal fade" id="newCourseOccurrenceModal" tabindex="-1" aria-labelledby="newCourseOccurrenceModalLabel" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content rounded-12 border-0">
                                        <div class="modal-header border-neutral-30">
                                            <h5 class="modal-title text-18 fw-semibold" id="newCourseOccurrenceModalLabel">New Occurrence</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form id="courseOccurrenceForm" action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/occurrences" method="post" novalidate data-course-live-form data-course-live-panel="occurrences">
                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                            <input type="hidden" name="returnTo" value="${currentReturnTo}#course-occurrences">
                                            <div class="modal-body">
                                                <p class="text-14 text-neutral-600 mb-20">Choose the academic year. Its year span, dates and state are calculated automatically from the configured course periods.</p>
                                                <c:forEach var="template" items="${coursePeriodTemplates}">
                                                    <input type="hidden"
                                                           data-course-period-template
                                                           data-start-month="${template.startsMonth}"
                                                           data-start-day="${template.startsDay}"
                                                           data-end-month="${template.endsMonth}"
                                                           data-end-day="${template.endsDay}">
                                                </c:forEach>
                                                <c:forEach var="occurrence" items="${courseOccurrences}">
                                                    <span hidden
                                                          data-existing-course-occurrence
                                                          data-reference-year="${occurrence.referenceYear}"
                                                          data-start-date="${occurrence.startsAtValue}"
                                                          data-end-date="${occurrence.endsAtValue}"></span>
                                                </c:forEach>
                                                <input id="courseOccurrenceReferenceYear" name="referenceYear" type="hidden" required>
                                                <div class="mb-20">
                                                    <span class="fw-medium text-base text-neutral-800 d-block mb-12" id="courseOccurrenceAcademicYearLabel">Academic year</span>
                                                    <div class="gape-academic-year-picker" id="courseOccurrenceAcademicYearPicker" aria-labelledby="courseOccurrenceAcademicYearLabel">
                                                        <div class="gape-academic-year-picker__header">
                                                            <button type="button" class="aac-icon-button" data-academic-year-previous aria-label="Show previous academic years" title="Show previous academic years"><i class="ph ph-caret-left" aria-hidden="true"></i></button>
                                                            <span class="fw-semibold text-neutral-700" id="courseOccurrenceAcademicYearPickerTitle">Academic years</span>
                                                            <button type="button" class="aac-icon-button" data-academic-year-next aria-label="Show next academic years" title="Show next academic years"><i class="ph ph-caret-right" aria-hidden="true"></i></button>
                                                        </div>
                                                        <div class="gape-academic-year-picker__grid" data-academic-year-grid role="listbox" aria-label="Available academic years"></div>
                                                    </div>
                                                </div>
                                                <div>
                                                    <span class="fw-medium text-base text-neutral-800 d-block mb-12">Selected academic year</span>
                                                    <output id="courseOccurrenceCodePreview" class="gape-academic-year-selection d-block text-14 text-neutral-600">Select an academic year</output>
                                                </div>
                                                <div id="courseOccurrenceDateValidation" class="gape-course-occurrence-validation mt-16" role="alert" hidden></div>
                                            </div>
                                            <div class="modal-footer border-neutral-30">
                                                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                                                <button type="submit" class="cd-primary-button border-0" data-course-occurrence-submit disabled aria-disabled="true"><i class="ph ph-plus-circle me-8"></i>Create Occurrence</button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:if>
                    </div>
                    </c:if>

                </main>

                <div data-course-lazy-modal-container></div>

                <c:if test="${canModifyCourse}">
                    <section class="cd-surface px-22 py-22 mb-20">
                        <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                            <div>
                                <h3 class="text-20 fw-semibold text-neutral-800 mb-6">Critical Actions</h3>
                                <span class="text-14 text-neutral-500">Restricted lifecycle operations for this course.</span>
                            </div>
                            <span class="bg-warning-50 text-warning-600 px-14 py-8 rounded-pill text-13">
                                <i class="ph ph-lock-key me-6"></i>Requires confirmation
                            </span>
                        </div>
                        <div class="cd-critical-grid">
                            <c:choose>
                                <c:when test="${course.inactive}">
                                    <article class="cd-critical-card cd-critical-card--archive">
                                        <div class="d-flex align-items-start gap-14 mb-16">
                                            <span class="cd-critical-icon cd-critical-icon--archive text-22"><i class="ph ph-arrow-clockwise"></i></span>
                                            <div>
                                                <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Activate Course</h4>
                                                <p class="text-14 text-neutral-500 mb-0">Restores this item to the active academic catalogue.</p>
                                            </div>
                                        </div>
                                        <button type="button" class="cd-primary-button" data-bs-toggle="modal" data-bs-target="#activateCourse">Activate</button>
                                    </article>
                                </c:when>
                                <c:otherwise>
                                    <article class="cd-critical-card cd-critical-card--archive">
                                        <div class="d-flex align-items-start gap-14 mb-16">
                                            <span class="cd-critical-icon cd-critical-icon--archive text-22"><i class="ph ph-archive"></i></span>
                                            <div>
                                                <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Deactivate Course</h4>
                                                <p class="text-14 text-neutral-500 mb-0">Temporarily disabled this item while preserving its data.</p>
                                            </div>
                                        </div>
                                        <button type="button" class="cd-warning-button" data-bs-toggle="modal" data-bs-target="#archiveCourse">Deactivate</button>
                                    </article>
                                </c:otherwise>
                            </c:choose>
                            <article class="cd-critical-card cd-critical-card--delete">
                                <div class="d-flex align-items-start gap-14 mb-16">
                                    <span class="cd-critical-icon cd-critical-icon--delete text-22"><i class="ph ph-trash"></i></span>
                                    <div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6">Delete Course</h4>
                                        <p class="text-14 text-neutral-500 mb-0">Permanently removes this item when the database allows it.</p>
                                    </div>
                                </div>
                                <button type="button" class="cd-danger-button" data-bs-toggle="modal" data-bs-target="#deleteCourse">Delete</button>
                            </article>
                        </div>
                    </section>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<div class="modal fade" id="courseSetupModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <div>
                    <h5 class="modal-title text-18 fw-semibold mb-4">Setup</h5>
                    <span class="text-13 text-neutral-500">Course identity, context and academic summary.</span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="d-flex align-items-center gap-10 flex-wrap mb-18">
                    <span class="${course.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                        <c:out value="${course.stateLabel}"/>
                    </span>
                    <span class="cd-soft-badge px-14 py-7 rounded-pill text-13">
                        <i class="ph ph-books me-6"></i><c:out value="${course.typeLabel}"/>
                    </span>
                </div>
                <div class="cd-info-grid">
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Name</span><strong class="text-14 text-neutral-800"><c:out value="${course.name}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Acronym</span><strong class="text-14 text-neutral-800"><c:out value="${course.acronym}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Type</span><strong class="text-14 text-neutral-800"><c:out value="${course.typeLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">State</span><strong class="text-14 text-neutral-800"><c:out value="${course.stateLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">ECTS</span><strong class="text-14 text-neutral-800"><c:out value="${course.ectsLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Subject ECTS</span><strong class="text-14 text-neutral-800"><c:out value="${courseSubjectEctsTotalLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Certificate max grade</span><strong class="text-14 text-neutral-800"><c:out value="${course.certificateMaxGradeLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Duration</span><strong class="text-14 text-neutral-800"><c:out value="${course.durationLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Organization</span><strong class="text-14 text-neutral-800"><c:out value="${course.organizationContextHtml}" escapeXml="false"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Organic Unit</span><strong class="text-14 text-neutral-800"><c:out value="${course.organicUnitLabel}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Subjects</span><strong class="text-14 text-neutral-800"><c:out value="${fn:length(courseSubjects)}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Active enrollments</span><strong class="text-14 text-neutral-800">${activeCourseEnrollmentCount}</strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Photo</span><strong class="text-14 text-neutral-800"><c:out value="${course.hasPhoto ? 'Available' : 'Missing'}"/></strong></div>
                    <div class="cd-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Identifier</span><strong class="text-14 text-neutral-800">#<c:out value="${course.id}"/></strong></div>
                </div>
            </div>
        </div>
    </div>
</div>

<c:if test="${canModifyCourse}">
<c:if test="${course.inactive}">
<div class="modal fade" id="activateCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Activate Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm activation of <strong><c:out value="${course.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/unarchive" method="post" class="m-0" data-course-live-form data-course-live-panel="structure" data-course-lifecycle-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" name="returnTo" value="${currentReturnTo}">
                    <button type="submit" class="cd-primary-button">Activate</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>
<c:if test="${not course.inactive}">
<div class="modal fade" id="archiveCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Deactivate Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm deactivation of <strong><c:out value="${course.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/archive" method="post" class="m-0" data-course-live-form data-course-live-panel="structure" data-course-lifecycle-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="cd-warning-button">Deactivate</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<div class="modal fade" id="deleteCourse" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Course</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the course if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/delete" method="post" class="m-0">
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
    window.GapeCourseModalSelect2Portal = {
        dropdownParent: function (modal) {
            if (!modal || !modal.classList.contains('gape-course-modal-root')) {
                return null;
            }
            var portal = modal.querySelector('[data-gape-course-select2-modal-portal]');
            if (!portal) {
                portal = document.createElement('div');
                portal.className = 'gape-course-select2-modal-portal';
                portal.setAttribute('data-gape-course-select2-modal-portal', '');
                modal.appendChild(portal);
            }
            return portal;
        }
    };
</script>
<script src="${pageContext.request.contextPath}/assets/js/gape-course-year-select.js?v=20260713-course-years-stable-1"></script>
<script>
    (function () {
        var courseHashTarget = {
            '#course-occurrences': 'occurrences',
            '#course-enrollments': 'enrollments',
            '#course-students': 'enrollments'
        };

    var coursePanelHash = {
        enrollments: '#course-enrollments',
        occurrences: '#course-occurrences',
        structure: '#course-structure'
    };

    function courseRoot() {
        return document.getElementById('courseDetailLiveRoot');
    }

    function courseTabs() {
        var root = courseRoot();
        return root ? Array.prototype.slice.call(root.querySelectorAll('[data-course-tab]')) : [];
    }

    function coursePanels() {
        var root = courseRoot();
        return root ? Array.prototype.slice.call(root.querySelectorAll('[data-course-panel]')) : [];
    }

    function currentCoursePanel() {
        var panel = coursePanels().find(function (item) {
            return !item.hidden;
        });
        return panel ? panel.getAttribute('data-course-panel') : 'structure';
    }

    function loadingSpinnerMarkup() {
        return '<span class="gape-course-loading-spinner" role="status" aria-label="Loading">'
                + '<i class="ph ph-spinner-gap" aria-hidden="true"></i>'
                + '<span class="visually-hidden">Loading</span>'
                + '</span>';
    }

    function withCurrentCourseSession(url) {
        var target = new URL(url, window.location.href);
        var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
        if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1 && target.pathname.indexOf(match[1] + '/') === 0) {
            target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
        }
        return target.toString();
    }

    function setCourseControlLoading(control, busy) {
        if (!control || control.tagName !== 'BUTTON') {
            return;
        }
        if (busy) {
            if (control.dataset.courseLoadingOriginalHtml === undefined) {
                control.dataset.courseLoadingOriginalHtml = control.innerHTML;
                control.dataset.courseLoadingOriginalTitle = control.getAttribute('title') || '';
                control.dataset.courseLoadingOriginalAriaLabel = control.getAttribute('aria-label') || '';
            }
            control.innerHTML = loadingSpinnerMarkup();
            control.setAttribute('title', 'Loading');
            control.setAttribute('aria-label', 'Loading');
            control.setAttribute('aria-busy', 'true');
            control.disabled = true;
            return;
        }
        if (control.dataset.courseLoadingOriginalHtml === undefined) {
            return;
        }
        control.innerHTML = control.dataset.courseLoadingOriginalHtml;
        if (control.dataset.courseLoadingOriginalTitle) {
            control.setAttribute('title', control.dataset.courseLoadingOriginalTitle);
        } else {
            control.removeAttribute('title');
        }
        if (control.dataset.courseLoadingOriginalAriaLabel) {
            control.setAttribute('aria-label', control.dataset.courseLoadingOriginalAriaLabel);
        } else {
            control.removeAttribute('aria-label');
        }
        control.removeAttribute('aria-busy');
        control.disabled = false;
        delete control.dataset.courseLoadingOriginalHtml;
        delete control.dataset.courseLoadingOriginalTitle;
        delete control.dataset.courseLoadingOriginalAriaLabel;
    }

    function setCourseTabLoading(name, busy) {
        var root = courseRoot();
        var tab = root ? root.querySelector('[data-course-tab="' + name + '"]') : null;
        var title = tab ? tab.querySelector('.cd-mode-card__title') : null;
        if (!tab || !title) {
            return;
        }
        if (busy) {
            if (title.dataset.courseLoadingOriginalHtml === undefined) {
                title.dataset.courseLoadingOriginalHtml = title.innerHTML;
            }
            title.innerHTML = loadingSpinnerMarkup();
            tab.setAttribute('aria-busy', 'true');
            tab.disabled = true;
            return;
        }
        if (title.dataset.courseLoadingOriginalHtml !== undefined) {
            title.innerHTML = title.dataset.courseLoadingOriginalHtml;
            delete title.dataset.courseLoadingOriginalHtml;
        }
        tab.removeAttribute('aria-busy');
        tab.disabled = false;
    }

    function revealCoursePanel(target, activeTab) {
        var tabs = courseTabs();
        var panels = coursePanels();
        if (!activeTab) {
            activeTab = tabs.find(function (tab) {
                return tab.getAttribute('data-course-tab') === target;
            });
        }
        tabs.forEach(function (item) {
            var active = activeTab ? item === activeTab : item.getAttribute('data-course-tab') === target;
            item.classList.toggle('is-active', active);
            item.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        panels.forEach(function (panel) {
            panel.hidden = panel.getAttribute('data-course-panel') !== target;
        });
    }

    async function activateCoursePanel(target, activeTab) {
        var root = courseRoot();
        var lazyPanel = root ? root.querySelector('[data-course-lazy-panel="' + target + '"]') : null;
        if (lazyPanel && lazyPanel.dataset.courseLazyLoaded !== 'true') {
            var loaded = await loadCourseLazyPanel(target);
            if (!loaded) {
                return;
            }
        }
        revealCoursePanel(target, activeTab);
    }

    function bindCourseTreeToggles(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('[data-gape-tree-toggle]')).forEach(function (button) {
            if (button.dataset.courseTreeBound === 'true') {
                return;
            }
            button.dataset.courseTreeBound = 'true';
        button.addEventListener('click', function () {
            var target = document.getElementById(button.dataset.gapeTreeToggle);
            if (!target) {
                return;
            }
            if (target.matches('[data-course-subject-groups]')
                    && target.dataset.courseSubjectGroupsLoaded !== 'true'
                    && target.classList.contains('d-none')) {
                loadCourseSubjectGroups(target, button).then(function (loaded) {
                    if (loaded) {
                        setCourseTreeExpanded(button, target, true);
                    }
                });
                return;
            }
            if (target.matches('[data-course-class-group-activities]')
                    && target.dataset.courseActivitiesLoaded !== 'true'
                    && target.classList.contains('d-none')) {
                loadCourseClassGroupActivities(target, button).then(function (loaded) {
                    if (loaded) {
                        setCourseTreeExpanded(button, target, true);
                    }
                });
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
    }

    function setCourseTreeExpanded(button, target, expanded) {
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

    function initCourseSelects(scope) {
        if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        prepareCourseDetailModals(scope);
        window.jQuery(scope).find('.js-example-basic-single').each(function () {
            var select = window.jQuery(this);
            if (select.data('select2')) {
                return;
            }
            var modalParent = this.closest('.modal');
            var options = {
                width: '100%',
                selectionCssClass: 'gape-eduall-selection',
                dropdownCssClass: 'gape-eduall-select-dropdown'
            };
            if (modalParent) {
                var portal = window.GapeCourseModalSelect2Portal
                        ? window.GapeCourseModalSelect2Portal.dropdownParent(modalParent)
                        : null;
                options.dropdownParent = window.jQuery(portal || modalParent);
            }
            select.select2(options);
        });
    }

    function setCourseDependentSelectDisabled(select, disabled) {
        if (!select) {
            return;
        }
        select.disabled = disabled;
        select.setAttribute('aria-disabled', String(disabled));
        var field = select.closest('.gape-select-field');
        if (field) {
            field.classList.toggle('is-disabled', disabled);
            field.classList.toggle('is-available', !disabled);
            field.dataset.courseDependentState = disabled ? 'disabled' : 'available';
        }
        var container = select.nextElementSibling;
        if (container && container.classList.contains('select2-container')) {
            container.classList.toggle('select2-container--disabled', disabled);
            var selection = container.querySelector('.select2-selection');
            if (selection) {
                selection.setAttribute('aria-disabled', String(disabled));
            }
        }
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2
                && window.jQuery(select).data('select2')) {
            window.jQuery(select).prop('disabled', disabled).trigger('change.select2');
        }
    }

    function clearCourseDependentSelect(select) {
        if (!select || !select.value) {
            return;
        }
        select.value = '';
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2
                && window.jQuery(select).data('select2')) {
            window.jQuery(select).val('').trigger('change.select2');
        }
    }

    function bindCourseDependentSelectEvents(select, handler) {
        select.addEventListener('change', handler);
        if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2
                || !window.jQuery(select).data('select2')) {
            return;
        }
        window.jQuery(select).on('select2:select.gapeCourseDependency select2:clear.gapeCourseDependency', function () {
            window.setTimeout(handler, 0);
        });
    }

    function configureCourseAssociateSubjectForms(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('[data-course-associate-subject-form]')).forEach(function (form) {
            if (form.dataset.courseAssociateSubjectBound === 'true') {
                return;
            }
            var subject = form.querySelector('[data-course-associate-subject]');
            var year = form.querySelector('[data-course-associate-year]');
            var period = form.querySelector('[data-course-associate-period]');
            if (!subject || !year || !period) {
                return;
            }
            form.dataset.courseAssociateSubjectBound = 'true';

            function syncPeriodOptions() {
                var selectedYear = year.value;
                Array.prototype.slice.call(period.options).forEach(function (option) {
                    var optionYear = option.dataset.courseAssociatePeriodYear;
                    if (!optionYear) {
                        return;
                    }
                    var available = optionYear === selectedYear;
                    option.hidden = !available;
                    option.disabled = !available;
                });
            }

            function syncYear() {
                var hasSubject = Boolean(subject.value);
                if (!hasSubject) {
                    clearCourseDependentSelect(year);
                    clearCourseDependentSelect(period);
                }
                setCourseDependentSelectDisabled(year, !hasSubject);
                syncPeriod();
            }

            function syncPeriod() {
                var hasYear = Boolean(year.value) && !year.disabled;
                if (!hasYear) {
                    clearCourseDependentSelect(period);
                }
                syncPeriodOptions();
                setCourseDependentSelectDisabled(period, !hasYear);
            }

            bindCourseDependentSelectEvents(subject, syncYear);
            bindCourseDependentSelectEvents(year, function () {
                clearCourseDependentSelect(period);
                syncPeriod();
            });
            syncYear();
        });
    }

    function configureCourseEnrollmentForms(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('[data-course-enrollment-form]')).forEach(function (form) {
            if (form.dataset.courseEnrollmentBound === 'true') {
                return;
            }
            var student = form.querySelector('[data-course-enrollment-student]');
            var occurrence = form.querySelector('[data-course-enrollment-occurrence]');
            if (!student || !occurrence) {
                return;
            }
            form.dataset.courseEnrollmentBound = 'true';
            var syncOccurrence = function () {
                var hasStudent = Boolean(student.value);
                if (!hasStudent) {
                    clearCourseDependentSelect(occurrence);
                }
                setCourseDependentSelectDisabled(occurrence, !hasStudent);
            };
            bindCourseDependentSelectEvents(student, syncOccurrence);
            syncOccurrence();
        });
    }

    function prepareCourseDetailModals(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('.modal')).forEach(function (modal) {
            modal.classList.add('gape-course-modal-root');
            var dialog = modal.querySelector('.modal-dialog');
            if (!dialog) {
                return;
            }
            dialog.classList.add('gape-course-modal-dialog');
            if (modal.id === 'associateCourseSubjectModal') {
                dialog.classList.add('gape-course-modal-dialog--association');
            }
        });
    }

    function courseModalContent(modal) {
        if (!modal || !modal.classList.contains('gape-course-modal-root')) {
            return null;
        }
        return modal.querySelector('.modal-content');
    }

    function lockCourseModalGeometry(modal) {
        var content = courseModalContent(modal);
        if (!content) {
            return;
        }
        var height = Math.round(content.getBoundingClientRect().height);
        if (height > 0) {
            content.style.setProperty('--gape-course-modal-open-height', height + 'px');
        }
    }

    function unlockCourseModalGeometry(modal) {
        var content = courseModalContent(modal);
        if (content) {
            content.style.removeProperty('--gape-course-modal-open-height');
        }
    }

    function initializeCourseFragment(scope) {
        prepareCourseDetailModals(scope);
        bindCourseTreeToggles(scope);
        configureCourseDetailSorting(scope);
        configureCourseOccurrenceForm(scope);
        initCourseSelects(scope);
        configureCourseAssociateSubjectForms(scope);
        configureCourseEnrollmentForms(scope);
    }

    async function loadCourseLazyPanel(name) {
        var root = courseRoot();
        var panel = root ? root.querySelector('[data-course-lazy-panel="' + name + '"]') : null;
        if (!panel || panel.dataset.courseLazyLoaded === 'true') {
            return true;
        }
        if (panel.dataset.courseLazyLoading === 'true') {
            return false;
        }
        var url = panel.dataset.courseLazyUrl;
        if (!url) {
            return false;
        }
        panel.dataset.courseLazyLoading = 'true';
        panel.setAttribute('aria-busy', 'true');
        setCourseTabLoading(name, true);
        panel.innerHTML = '<div class="cd-surface px-22 py-22 text-center text-14 text-neutral-500">Loadingâ€¦</div>';
        try {
            var response = await fetch(withCurrentCourseSession(url), {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!response.ok) {
                throw new Error('The panel could not be loaded.');
            }
            panel.innerHTML = await response.text();
            panel.dataset.courseLazyLoaded = 'true';
            initializeCourseFragment(panel);
            return true;
        } catch (error) {
            panel.innerHTML = '<div class="cd-surface px-22 py-22 text-center text-14 text-danger-600">Unable to load this section. Please try again.</div>';
            return false;
        } finally {
            delete panel.dataset.courseLazyLoading;
            panel.setAttribute('aria-busy', 'false');
            setCourseTabLoading(name, false);
        }
    }

    async function loadCourseClassGroupActivities(panel, trigger) {
        if (!panel || panel.dataset.courseActivitiesLoaded === 'true') {
            return true;
        }
        if (panel.dataset.courseActivitiesLoading === 'true') {
            return false;
        }
        var url = panel.dataset.courseActivityUrl;
        if (!url) {
            return false;
        }
        panel.dataset.courseActivitiesLoading = 'true';
        panel.setAttribute('aria-busy', 'true');
        setCourseControlLoading(trigger, true);
        panel.innerHTML = '<div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-neutral-500">Loading activitiesâ€¦</div>';
        try {
            var response = await fetch(withCurrentCourseSession(url), {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!response.ok) {
                throw new Error('The activities could not be loaded.');
            }
            panel.innerHTML = await response.text();
            panel.dataset.courseActivitiesLoaded = 'true';
            return true;
        } catch (error) {
            panel.innerHTML = '<div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-danger-600">Unable to load activities. Please try again.</div>';
            return false;
        } finally {
            delete panel.dataset.courseActivitiesLoading;
            panel.setAttribute('aria-busy', 'false');
            setCourseControlLoading(trigger, false);
        }
    }

    async function loadCourseSubjectGroups(panel, trigger) {
        if (!panel || panel.dataset.courseSubjectGroupsLoaded === 'true') {
            return true;
        }
        if (panel.dataset.courseSubjectGroupsLoading === 'true') {
            return false;
        }
        var url = panel.dataset.courseSubjectGroupsUrl;
        if (!url) {
            return false;
        }
        panel.dataset.courseSubjectGroupsLoading = 'true';
        panel.setAttribute('aria-busy', 'true');
        setCourseControlLoading(trigger, true);
        panel.innerHTML = '<div class="text-13 text-neutral-500">Loading class groupsâ€¦</div>';
        try {
            var response = await fetch(withCurrentCourseSession(url), {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!response.ok) {
                throw new Error('The class groups could not be loaded.');
            }
            panel.innerHTML = await response.text();
            panel.dataset.courseSubjectGroupsLoaded = 'true';
            initializeCourseFragment(panel);
            return true;
        } catch (error) {
            panel.innerHTML = '<div class="text-13 text-danger-600">Unable to load class groups. Please try again.</div>';
            return false;
        } finally {
            delete panel.dataset.courseSubjectGroupsLoading;
            panel.setAttribute('aria-busy', 'false');
            setCourseControlLoading(trigger, false);
        }
    }

    async function showCourseLazyModal(button) {
        var root = courseRoot();
        var container = root ? root.querySelector('[data-course-lazy-modal-container]') : null;
        var url = button ? button.dataset.courseLazyModalUrl : null;
        if (!container || !url) {
            return;
        }
        var modal = container.querySelector('#associateCourseSubjectModal');
        if (!modal) {
            setCourseControlLoading(button, true);
            try {
                var response = await fetch(withCurrentCourseSession(url), {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
                });
                if (!response.ok) {
                    throw new Error('The modal could not be loaded.');
                }
                container.innerHTML = await response.text();
                modal = container.querySelector('#associateCourseSubjectModal');
                initializeCourseFragment(container);
            } finally {
                setCourseControlLoading(button, false);
            }
        }
        if (modal && window.bootstrap && window.bootstrap.Modal) {
            window.bootstrap.Modal.getOrCreateInstance(modal).show();
        }
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('[data-course-lazy-modal="associate-subject"]');
        var root = courseRoot();
        if (!button || !root || !root.contains(button)) {
            return;
        }
        event.preventDefault();
        showCourseLazyModal(button).catch(function () {
            // The control is restored by showCourseLazyModal; keep the current page stable on a failed fetch.
        });
    });

    function configureCourseDetailSorting(scope) {
        var roots = Array.prototype.slice.call((scope || document).querySelectorAll('[data-course-detail-sort-root]'));
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
            var lists = Array.prototype.slice.call(root.querySelectorAll('[data-course-detail-sort-list]'));
            var sortOptions = Array.prototype.slice.call(root.querySelectorAll('[data-course-detail-sort-option]'));
            var sortToggle = root.querySelector('[data-course-detail-sort-toggle]');
            if (!lists.length || !sortOptions.length) {
                return;
            }

            var listGroups = lists.map(function (list) {
                var rows = Array.prototype.slice.call(list.children).filter(function (child) {
                    return child.hasAttribute('data-course-detail-sort-row');
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

    function configureCourseOccurrenceForm(scope) {
        var form = (scope || document).querySelector('#courseOccurrenceForm');
        if (!form) {
            return;
        }
        if (form.dataset.courseOccurrenceBound === 'true') {
            return;
        }
        form.dataset.courseOccurrenceBound = 'true';
        var referenceYearInput = document.getElementById('courseOccurrenceReferenceYear');
        var codePreview = document.getElementById('courseOccurrenceCodePreview');
        var validation = document.getElementById('courseOccurrenceDateValidation');
        var picker = document.getElementById('courseOccurrenceAcademicYearPicker');
        var pickerTitle = document.getElementById('courseOccurrenceAcademicYearPickerTitle');
        var pickerGrid = form.querySelector('[data-academic-year-grid]');
        var previousButton = form.querySelector('[data-academic-year-previous]');
        var nextButton = form.querySelector('[data-academic-year-next]');
        var submitButton = form.querySelector('[data-course-occurrence-submit]');
        var templateRows = Array.prototype.slice.call(form.querySelectorAll('[data-course-period-template]'));
        var existingOccurrences = Array.prototype.slice.call(form.querySelectorAll('[data-existing-course-occurrence]')).map(function (element) {
            return {
                referenceYear: Number(element.dataset.referenceYear),
                startsAt: new Date(element.dataset.startDate + 'T00:00:00'),
                endsAt: new Date(element.dataset.endDate + 'T00:00:00')
            };
        });
        var pageStart = Math.floor(new Date().getFullYear() / 12) * 12;

        function compareDates(left, right) {
            return left.getTime() - right.getTime();
        }

        function academicYearLabel(startsAt, endsAt) {
            if (!startsAt || !endsAt) {
                return '';
            }
            var years = [];
            for (var year = startsAt.getFullYear(); year <= endsAt.getFullYear(); year++) {
                years.push(String(year));
            }
            return years.join('-');
        }

        function dateFor(year, month, day) {
            var lastDay = new Date(year, month, 0).getDate();
            return new Date(year, month - 1, Math.min(day, lastDay));
        }

        function monthDayBefore(month, day, otherMonth, otherDay) {
            return month < otherMonth || (month === otherMonth && day < otherDay);
        }

        function configuredPeriods(referenceYear) {
            if (!referenceYear || !templateRows.length) {
                return [];
            }
            var anchor = templateRows[0];
            var anchorMonth = Number(anchor.dataset.startMonth);
            var anchorDay = Number(anchor.dataset.startDay);
            return templateRows.map(function (row) {
                var startMonth = Number(row.dataset.startMonth);
                var startDay = Number(row.dataset.startDay);
                var startsAt = dateFor(referenceYear, startMonth, startDay);
                if (monthDayBefore(startMonth, startDay, anchorMonth, anchorDay)) {
                    startsAt = dateFor(referenceYear + 1, startMonth, startDay);
                }
                var endsAt = dateFor(startsAt.getFullYear(), Number(row.dataset.endMonth), Number(row.dataset.endDay));
                if (compareDates(endsAt, startsAt) < 0) {
                    endsAt = dateFor(endsAt.getFullYear() + 1, Number(row.dataset.endMonth), Number(row.dataset.endDay));
                }
                return {
                    label: row.dataset.periodLabel,
                    startsAt: startsAt,
                    endsAt: endsAt
                };
            });
        }

        function occurrenceRange(referenceYear) {
            var periods = configuredPeriods(referenceYear);
            if (!periods.length) {
                return null;
            }
            return periods.reduce(function (range, period) {
                return {
                    startsAt: compareDates(period.startsAt, range.startsAt) < 0 ? period.startsAt : range.startsAt,
                    endsAt: compareDates(period.endsAt, range.endsAt) > 0 ? period.endsAt : range.endsAt
                };
            }, {
                startsAt: periods[0].startsAt,
                endsAt: periods[0].endsAt
            });
        }

        function displayDate(date) {
            return String(date.getDate()).padStart(2, '0') + '-'
                + String(date.getMonth() + 1).padStart(2, '0') + '-'
                + date.getFullYear();
        }

        function availabilityFor(referenceYear) {
            if (!Number.isInteger(referenceYear) || referenceYear < 1900 || referenceYear > 9998) {
                return { available: false, message: 'This academic year is outside the supported range.' };
            }
            if (!templateRows.length) {
                return { available: false, message: 'Configure the recurring course periods before creating an occurrence.' };
            }
            var range = occurrenceRange(referenceYear);
            var duplicate = existingOccurrences.some(function (occurrence) {
                return occurrence.referenceYear === referenceYear;
            });
            if (duplicate) {
                return { available: false, message: 'An occurrence already exists for this academic year.', range: range };
            }
            var overlapping = existingOccurrences.find(function (occurrence) {
                return compareDates(range.startsAt, occurrence.endsAt) <= 0 && compareDates(range.endsAt, occurrence.startsAt) >= 0;
            });
            if (overlapping) {
                return {
                    available: false,
                    message: 'This academic year overlaps the existing ' + academicYearLabel(overlapping.startsAt, overlapping.endsAt) + ' occurrence.',
                    range: range
                };
            }
            var latestEnd = existingOccurrences.reduce(function (latest, occurrence) {
                return !latest || compareDates(occurrence.endsAt, latest) > 0 ? occurrence.endsAt : latest;
            }, null);
            if (latestEnd && compareDates(range.startsAt, latestEnd) <= 0) {
                return {
                    available: false,
                    message: 'A new occurrence must start after ' + displayDate(latestEnd) + ', when the latest existing occurrence ends.',
                    range: range
                };
            }
            return { available: true, message: '', range: range };
        }

        function renderAcademicYears() {
            if (!pickerGrid) {
                return;
            }
            pickerGrid.innerHTML = '';
            if (pickerTitle) {
                pickerTitle.textContent = pageStart + ' â€“ ' + (pageStart + 11);
            }
            var selectedReferenceYear = Number(referenceYearInput.value);
            for (var offset = 0; offset < 12; offset++) {
                var referenceYear = pageStart + offset;
                var availability = availabilityFor(referenceYear);
                var label = availability.range
                    ? academicYearLabel(availability.range.startsAt, availability.range.endsAt)
                    : String(referenceYear);
                var option = document.createElement('button');
                option.type = 'button';
                option.className = 'gape-academic-year-option';
                option.dataset.academicYearOption = String(referenceYear);
                option.setAttribute('role', 'option');
                option.setAttribute('aria-selected', String(selectedReferenceYear === referenceYear));
                option.disabled = !availability.available;
                option.title = availability.available ? 'Select ' + label : availability.message;
                option.textContent = label;
                if (selectedReferenceYear === referenceYear) {
                    option.classList.add('is-selected');
                }
                pickerGrid.appendChild(option);
            }
        }

        function validateOccurrence(showRequiredErrors) {
            var referenceYear = Number(referenceYearInput.value);
            var messages = [];
            var hasReferenceYear = Boolean(referenceYearInput.value);
            var validReferenceYear = Number.isInteger(referenceYear) && referenceYear >= 1900 && referenceYear <= 9998;
            if (!validReferenceYear) {
                if (showRequiredErrors || hasReferenceYear) {
                    messages.push('Select an academic year.');
                }
            }
            if (!templateRows.length) {
                messages.push('Configure the recurring course periods before creating an occurrence.');
            }
            var availability = validReferenceYear && templateRows.length ? availabilityFor(referenceYear) : null;
            if (availability && !availability.available) {
                messages.push(availability.message);
            }

            var hasErrors = messages.length > 0;
            if (picker) {
                picker.classList.toggle('is-invalid', hasErrors && (hasReferenceYear || showRequiredErrors));
                picker.title = messages.join(' ');
                picker.setAttribute('aria-invalid', String(hasErrors));
            }
            referenceYearInput.setCustomValidity(messages.length ? messages[0] : '');
            validation.hidden = !hasErrors;
            validation.textContent = messages.join(' ');

            if (codePreview) {
                codePreview.textContent = availability && availability.available
                    ? academicYearLabel(availability.range.startsAt, availability.range.endsAt)
                    : 'Select an academic year';
            }

            var valid = validReferenceYear && templateRows.length > 0 && availability && availability.available;
            if (submitButton) {
                submitButton.disabled = !valid;
                submitButton.setAttribute('aria-disabled', String(!valid));
            }
            return valid;
        }

        pickerGrid.addEventListener('click', function (event) {
            var option = event.target.closest('[data-academic-year-option]');
            if (!option || option.disabled) {
                return;
            }
            referenceYearInput.value = option.dataset.academicYearOption;
            validateOccurrence(false);
            renderAcademicYears();
        });
        previousButton.addEventListener('click', function () {
            pageStart -= 12;
            renderAcademicYears();
        });
        nextButton.addEventListener('click', function () {
            pageStart += 12;
            renderAcademicYears();
        });
        form.addEventListener('submit', function (event) {
            if (!validateOccurrence(true)) {
                event.preventDefault();
                picker.focus();
            }
        });
        renderAcademicYears();
        validateOccurrence(false);
    }

    function cleanupCourseModals() {
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

    function setCourseFormBusy(form, busy) {
        form.querySelectorAll('button, input, select, textarea').forEach(function (control) {
            if (busy) {
                control.dataset.coursePreviousDisabled = control.disabled ? 'true' : 'false';
                control.disabled = true;
            } else if (control.dataset.coursePreviousDisabled !== 'true') {
                control.disabled = false;
                delete control.dataset.coursePreviousDisabled;
            }
        });
    }

    function replaceCourseRootFrom(html, panel) {
        var parser = new DOMParser();
        var doc = parser.parseFromString(html, 'text/html');
        var nextRoot = doc.getElementById('courseDetailLiveRoot');
        var root = courseRoot();
        if (!nextRoot || !root) {
            return false;
        }
        cleanupCourseModals();
        ['courseSetupModal', 'activateCourse', 'archiveCourse', 'deleteCourse'].forEach(function (id) {
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
        initCourseDetail(panel);
        return true;
    }

    function isCourseLiveFormInScope(form, root) {
        return !!form && !!root && (root.contains(form) || form.hasAttribute('data-course-lifecycle-form'));
    }

    async function reloadCourseRoot(panel) {
        var response = await fetch(withCurrentCourseSession(window.location.href.split('#')[0]), {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'text/html',
                'X-Requested-With': 'XMLHttpRequest'
            }
        });
        var html = await response.text();
        return replaceCourseRootFrom(html, panel);
    }

    function coursePanelUrl(url, panel) {
        var target = new URL(url, window.location.href);
        var hash = coursePanelHash[panel];
        if (hash) {
            target.hash = hash;
        }
        return withCurrentCourseSession(target.toString());
    }

    async function submitCourseLiveForm(form, trigger) {
        if (form.dataset.courseSubmitting === 'true') {
            return;
        }
        if (form.reportValidity && !form.reportValidity()) {
            return;
        }
        form.dataset.courseSubmitting = 'true';
        var panel = form.getAttribute('data-course-live-panel') || currentCoursePanel();
        var formData = new FormData(form);
        setCourseFormBusy(form, true);
        setCourseControlLoading(trigger || form.querySelector('button[type="submit"]'), true);
        try {
            var response = await fetch(withCurrentCourseSession(form.action), {
                method: (form.method || 'POST').toUpperCase(),
                body: formData,
                credentials: 'same-origin',
                headers: {
                    'Accept': 'text/html',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            var html = await response.text();
            if (!replaceCourseRootFrom(html, panel) && !await reloadCourseRoot(panel)) {
                window.location.assign(coursePanelUrl(response.url || form.action, panel));
            }
        } catch (error) {
            try {
                if (await reloadCourseRoot(panel)) {
                    return;
                }
            } catch (ignored) {
                // Fall back to a normal navigation if the partial refresh cannot recover.
            }
            window.location.assign(coursePanelUrl(window.location.href, panel));
        } finally {
            setCourseControlLoading(trigger || form.querySelector('button[type="submit"]'), false);
            setCourseFormBusy(form, false);
            delete form.dataset.courseSubmitting;
        }
    }

    function initCourseDetail(preferredPanel) {
        var root = courseRoot();
        if (!root) {
            return;
        }
        prepareCourseDetailModals(document);
        var target = preferredPanel || courseHashTarget[window.location.hash] || currentCoursePanel() || 'structure';
        if (preferredPanel || courseHashTarget[window.location.hash]) {
            /* Keep the requested card visible while its lazy content loads,
               including after a normal-navigation fallback. */
            revealCoursePanel(target);
        }
        activateCoursePanel(target);
        initializeCourseFragment(root);
    }

    document.addEventListener('shown.bs.modal', function (event) {
        lockCourseModalGeometry(event.target);
    });

    document.addEventListener('hidden.bs.modal', function (event) {
        unlockCourseModalGeometry(event.target);
    });

    document.addEventListener('click', function (event) {
        var root = courseRoot();
        var tab = event.target.closest('[data-course-tab]');
        if (tab && root && root.contains(tab)) {
            event.preventDefault();
            activateCoursePanel(tab.getAttribute('data-course-tab'), tab);
            return;
        }

        var submitControl = event.target.closest('form[data-course-live-form] button[type="submit"], form[data-course-live-form] input[type="submit"]');
        if (!submitControl) {
            return;
        }
        var form = submitControl.form || submitControl.closest('form[data-course-live-form]');
        if (!isCourseLiveFormInScope(form, root)) {
            return;
        }
        event.preventDefault();
        if (form.reportValidity && !form.reportValidity()) {
            return;
        }
        submitCourseLiveForm(form, submitControl);
    });

    document.addEventListener('submit', function (event) {
        var form = event.target.closest('form[data-course-live-form]');
        var root = courseRoot();
        if (!isCourseLiveFormInScope(form, root)) {
            return;
        }
        event.preventDefault();
        submitCourseLiveForm(form, event.submitter);
    });

    initCourseDetail();
    })();
</script>
</body>
</html>
