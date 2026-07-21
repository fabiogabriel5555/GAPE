<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - <c:choose><c:when test="${attendanceOnly}">Attendance</c:when><c:otherwise>Enrollments &amp; Certificates</c:otherwise></c:choose></title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .aac-page {
            --aac-primary: #2563eb;
            --aac-border: #e6edf0;
            --aac-muted: #64748b;
            --aac-ink: #172033;
            min-width: 0;
        }

        /* Shared visual contract with the Lessons & Assessments mode cards. */
        .aac-page .la-mode-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(3, minmax(0, 1fr));
        }

        .aac-page .la-mode-card {
            align-items: flex-start;
            background: #fff;
            border: 1px solid var(--aac-border);
            border-radius: 8px;
            color: var(--aac-ink);
            cursor: pointer;
            display: flex;
            gap: 14px;
            min-height: 118px;
            padding: 20px;
            text-align: left;
            transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease, background .2s ease;
            width: 100%;
        }

        .aac-page .la-mode-card.is-active {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(20, 184, 166, 0.05)), #f7fbff;
            border-color: rgba(37, 99, 235, 0.7);
            box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12);
            transform: translateY(-1px);
        }

        /* Use the exact Class Group Management hover geometry: the card stays
           in place and only its border/shadow acknowledge the pointer. */
        .aac-page .la-mode-card:hover,
        .aac-page .la-mode-card:focus-visible {
            border-color: rgba(37, 99, 235, .28);
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
            color: #172033;
            transform: none;
        }

        .aac-page .la-mode-card.is-active .la-mode-icon {
            background: var(--aac-primary) !important;
            color: #fff !important;
        }

        .aac-page .la-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex: 0 0 48px;
            height: 48px;
            justify-content: center;
            width: 48px;
        }

        .aac-page .ad-mode-content,
        .aac-page .ad-mode-content span {
            min-width: 0;
            overflow-wrap: anywhere;
            white-space: normal !important;
        }

        .aac-page .ad-tab-panel[hidden] {
            display: none !important;
        }

        [data-attendance-decision][hidden] {
            display: none !important;
        }

        .aac-surface {
            background: #fff;
            border: 1px solid var(--aac-border);
            border-radius: 8px;
            box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
            min-width: 0;
        }

        .gape-management-archive-link {
            align-items: center;
            background: #fffafa;
            border: 1px solid #fecaca;
            border-radius: 8px;
            display: flex;
            flex-wrap: wrap;
            gap: 14px 18px;
            justify-content: space-between;
            padding: 16px 18px;
        }

        .gape-management-archive-link strong,
        .gape-management-archive-link span {
            display: block;
        }

        .gape-management-archive-link strong { color: #991b1b; font-size: 14px; }
        .gape-management-archive-link span { color: #7f1d1d; font-size: 12px; margin-top: 3px; }
        .gape-management-archive-link .gape-list-load-all { flex: 0 0 auto; width: auto; }

        /* Every management dialog uses a stable viewport-bound shell. */
        .aac-page .modal-dialog {
            margin: 16px auto;
            max-width: min(760px, calc(100vw - 32px));
            width: calc(100vw - 32px);
        }

        .aac-page .modal-dialog.modal-lg { max-width: min(900px, calc(100vw - 32px)); }
        .aac-page .modal-dialog.modal-xl { max-width: min(1180px, calc(100vw - 32px)); }
        .aac-page .modal-content { display: flex; flex-direction: column; max-height: calc(100vh - 32px); }
        .aac-page .modal-header,
        .aac-page .modal-footer { flex: 0 0 auto; }
        .aac-page .modal-body { min-height: 0; overflow: auto; }

        .aac-expanded-cell {
            background: #f8fbff !important;
            border-left: 4px solid #18a34a;
            padding: 14px 18px !important;
        }

        .aac-expanded-panel {
            background: #f8fbff;
            border: 1px solid #d9e4ef;
            border-radius: 8px;
            padding: 18px;
        }

        .aac-expanded-panel__header {
            align-items: flex-start;
            display: flex;
            gap: 12px;
            justify-content: space-between;
            margin-bottom: 14px;
        }

        .aac-expanded-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .aac-expanded-item {
            align-items: center;
            background: #fff;
            border: 1px solid #e5edf5;
            border-radius: 8px;
            display: flex;
            gap: 14px;
            justify-content: space-between;
            min-width: 0;
            padding: 14px 18px;
        }

        .aac-certificate-table {
            table-layout: fixed;
            width: 100%;
        }

        .aac-certificate-table th:nth-child(1),
        .aac-certificate-table td:nth-child(1) {
            width: 28%;
        }

        .aac-certificate-table th:nth-child(2),
        .aac-certificate-table td:nth-child(2) {
            width: 28%;
        }

        .aac-certificate-table th:nth-child(3),
        .aac-certificate-table td:nth-child(3) {
            width: 150px;
        }

        .aac-certificate-table th:nth-child(4),
        .aac-certificate-table td:nth-child(4) {
            width: 240px;
        }

        .aac-certificate-table th:nth-child(5),
        .aac-certificate-table td:nth-child(5) {
            width: 128px;
        }

        .aac-certificate-summary-row {
            cursor: default;
        }

        .aac-certificate-summary-row:focus-visible {
            outline: 2px solid rgba(37, 99, 235, .45);
            outline-offset: -2px;
        }

        .aac-certificate-table td {
            vertical-align: middle;
        }

        .aac-certificate-table__sheets {
            display: -webkit-box;
            line-height: 1.35;
            max-width: 100%;
            overflow: hidden;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 2;
        }

        .aac-attendance-table {
            table-layout: fixed;
            width: 100%;
        }

        .aac-attendance-table th,
        .aac-attendance-table td {
            vertical-align: middle;
        }

        .aac-attendance-table th {
            white-space: nowrap;
        }

        .aac-attendance-table th:nth-child(1),
        .aac-attendance-table td:nth-child(1) {
            width: 18%;
        }

        .aac-attendance-table th:nth-child(2),
        .aac-attendance-table td:nth-child(2) {
            width: 12%;
        }

        .aac-attendance-table th:nth-child(3),
        .aac-attendance-table td:nth-child(3) {
            width: 26%;
        }

        .aac-attendance-table th:nth-child(4),
        .aac-attendance-table td:nth-child(4) {
            width: 24%;
        }

        .aac-attendance-table th:nth-child(5),
        .aac-attendance-table td:nth-child(5) {
            width: 12%;
        }

        .aac-attendance-table th:nth-child(6),
        .aac-attendance-table td:nth-child(6) {
            width: 8%;
            white-space: nowrap;
        }

        .aac-attendance-table .aac-attendance-count {
            display: inline-block;
            max-width: 100%;
            white-space: nowrap;
        }

        .aac-page .gape-mobile-kv {
            grid-template-columns: minmax(82px, max-content) minmax(0, 1fr);
        }

        .aac-page .gape-mobile-kv strong {
            overflow-wrap: normal;
            white-space: nowrap;
            word-break: normal;
        }

        .aac-tree {
            display: flex;
            flex-direction: column;
            gap: 14px;
            min-width: 0;
        }

        .aac-tree-node {
            background: #fff;
            border: 1px solid #e5edf5;
            border-radius: 8px;
            min-width: 0;
            overflow: visible;
            transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
        }

        .aac-tree-node:hover {
            border-color: rgba(37, 99, 235, .28);
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
            transform: none;
        }

        .aac-page [data-deferred-management-root] tr.hover-bg-neutral-20:hover {
            background-color: #fff !important;
        }

        .aac-tree-row {
            align-items: center;
            display: flex;
            gap: 14px;
            justify-content: space-between;
            min-width: 0;
            padding: 14px 18px;
        }

        .aac-tree-main {
            align-items: center;
            display: flex;
            flex: 1 1 300px;
            gap: 12px;
            min-width: 0;
        }

        .aac-tree-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex: 0 0 40px;
            height: 40px;
            justify-content: center;
            width: 40px;
        }

        .aac-tree-title {
            color: #172033;
            display: block;
            font-size: 15px;
            font-weight: 600;
            line-height: 1.25;
            overflow-wrap: anywhere;
        }

        .aac-tree-meta {
            color: #64748b;
            display: block;
            font-size: 12px;
            line-height: 1.35;
            margin-top: 4px;
            overflow-wrap: anywhere;
        }

        .aac-tree-count,
        .aac-tree-status {
            flex: 0 0 126px;
            min-width: 110px;
        }

        .aac-tree-status {
            flex-basis: 180px;
        }

        .aac-tree-count-value {
            color: #172033;
            display: block;
            font-size: 14px;
            font-weight: 400;
            line-height: 1.2;
        }

        .aac-state-summary {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            min-width: 0;
        }

        .aac-state-summary__badge {
            white-space: nowrap;
        }

        .aac-state-empty {
            color: #94a3b8;
            font-size: 12px;
            line-height: 1.25;
        }

        .aac-tree-actions {
            align-items: center;
            display: inline-flex;
            flex: 0 0 auto;
            flex-wrap: nowrap;
            gap: 10px;
            justify-content: flex-end;
            min-width: max-content;
        }

        .aac-tree-children {
            background: #f8fbff;
            border-top: 1px solid #e5edf5;
            padding: 14px 18px 18px;
        }

        .aac-tree-children > .aac-tree-node + .aac-tree-node,
        .aac-tree-children > .aac-tree-empty + .aac-tree-node {
            margin-top: 12px;
        }

        .aac-tree-node--subject,
        .aac-tree-node--class,
        .aac-tree-node--assessment {
            background: #fff;
        }

        .aac-tree-empty {
            background: #fff;
            border: 1px dashed #d9e4ef;
            border-radius: 8px;
            color: #64748b;
            font-size: 13px;
            padding: 14px 16px;
        }

        .aac-icon-button {
            align-items: center;
            background: transparent !important;
            border: 0;
            border-radius: 8px;
            color: var(--neutral-500) !important;
            display: inline-flex;
            height: 32px;
            justify-content: center;
            padding: 0;
            transition: background-color .2s ease, color .2s ease;
            width: 32px;
        }

        .aac-icon-button i {
            font-size: 22px;
            line-height: 1;
        }

        .aac-icon-button:hover,
        .aac-icon-button:focus-visible {
            background-color: var(--main-50) !important;
            color: var(--main-600) !important;
            text-decoration: none;
        }

        .aac-icon-button:active {
            background-color: var(--main-50) !important;
            color: var(--main-600) !important;
        }

        .aac-settings-modal {
            background: #fff;
            box-shadow: 0 22px 60px rgba(15, 23, 42, .18);
        }

        .aac-settings-grid {
            display: grid;
            gap: 12px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .aac-settings-card {
            background: #fff;
            border: 1px solid #e6edf0;
            border-radius: 8px;
            min-width: 0;
            padding: 14px 16px;
        }

        .aac-settings-card--wide {
            grid-column: 1 / -1;
        }

        .aac-settings-decision {
            background: #fff;
            border: 1px solid #e6edf0;
            border-radius: 8px;
            padding: 16px;
        }

        .aac-settings-actions {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
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

        .aac-subgroup {
            background: #fff;
            border: 1px solid #e5edf5;
            border-radius: 8px;
            overflow: hidden;
        }

        .aac-subgroup__header {
            align-items: center;
            background: #fbfdff;
            border-bottom: 1px solid #e5edf5;
            display: flex;
            justify-content: space-between;
            padding: 14px 18px;
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
            overflow-wrap: anywhere;
            overflow: hidden;
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
            padding: 7px 8px;
            text-align: center;
            vertical-align: middle;
            overflow: hidden;
            overflow-wrap: anywhere;
            text-overflow: ellipsis;
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

        .aac-certificate-doc {
            background: #fff;
            color: #111;
            display: grid;
            font-family: Arial, sans-serif;
            gap: 34px;
            grid-template-columns: 48px minmax(0, 1fr);
            margin: 0 auto;
            max-width: 960px;
            min-height: 780px;
            padding: 26px 36px 38px 22px;
        }

        .aac-certificate-doc__rail {
            align-self: stretch;
            background: transparent;
            display: grid;
            gap: 28px;
            grid-template-rows: 172px minmax(380px, 1fr) 148px;
        }

        .aac-certificate-doc__rail span {
            background: #006070;
            display: block;
        }

        .aac-certificate-doc__body {
            min-width: 0;
        }

        .aac-certificate-doc h3 {
            color: #006070;
            font-size: 42px;
            font-weight: 800;
            line-height: 1.08;
            margin: 6px 0 66px;
            overflow-wrap: anywhere;
        }

        .aac-certificate-doc__identity {
            display: grid;
            gap: 16px;
            grid-template-columns: minmax(0, 1fr);
            margin-bottom: 34px;
        }

        .aac-certificate-doc__identity div {
            min-width: 0;
        }

        .aac-certificate-doc__identity span,
        .aac-certificate-doc__identity small {
            display: block;
            font-size: 17px;
            line-height: 1.35;
            margin-bottom: 7px;
        }

        .aac-certificate-doc__identity strong {
            display: block;
            font-size: 20px;
            line-height: 1.35;
            margin-bottom: 5px;
            overflow-wrap: anywhere;
        }

        .aac-certificate-doc__table {
            border-collapse: collapse;
            margin-bottom: 36px;
            table-layout: fixed;
            width: 100%;
        }

        .aac-certificate-doc__subject-col {
            width: 59%;
        }

        .aac-certificate-doc__ects-col {
            width: 17%;
        }

        .aac-certificate-doc__grade-col {
            width: 24%;
        }

        .aac-certificate-doc__table th,
        .aac-certificate-doc__table td {
            border: 1px solid #b7b7b7;
            font-size: 18px;
            line-height: 1.35;
            padding: 22px 22px;
            overflow-wrap: anywhere;
        }

        .aac-certificate-doc__table th {
            color: #006070;
            font-weight: 800;
            text-align: center;
        }

        .aac-certificate-doc__table td:nth-child(2),
        .aac-certificate-doc__table td:nth-child(3) {
            text-align: center;
        }

        .aac-certificate-doc__final td {
            font-weight: 800;
            text-align: center;
        }

        .aac-certificate-doc__statement {
            font-size: 18px;
            line-height: 1.62;
            margin: 0 0 58px;
        }

        .aac-certificate-doc__statement strong {
            font-weight: 800;
        }

        .aac-certificate-doc__signatures {
            display: grid;
            gap: 72px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            margin-top: 0;
        }

        .aac-certificate-doc__signatures span {
            display: block;
            font-size: 17px;
            line-height: 1.35;
        }

        .aac-certificate-doc__line {
            border-top: 1px solid #111;
            color: #111;
            font-size: 12px;
            margin-top: 42px;
            padding-top: 8px;
            text-align: center;
        }

        @media (max-width: 1199.98px) {
            .aac-page .la-mode-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
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
        }

        @media (max-width: 575.98px) {
            .aac-page .la-mode-grid {
                grid-template-columns: minmax(0, 1fr);
            }

            .aac-page .la-mode-card {
                padding: 16px;
            }

            .aac-settings-grid {
                grid-template-columns: 1fr;
            }

            .aac-expanded-panel {
                padding: 14px;
            }

            .aac-expanded-item {
                align-items: flex-start;
                flex-direction: column;
            }

            .aac-tree-row {
                align-items: flex-start;
                flex-direction: column;
            }

            .aac-tree-main {
                align-items: flex-start;
                flex: 0 1 auto;
                width: 100%;
            }

            .aac-tree-count,
            .aac-tree-status {
                flex: 1 1 auto;
                width: 100%;
            }

            .aac-tree-actions {
                flex-wrap: wrap;
                justify-content: flex-start;
                min-width: 0;
            }

            .aac-subgroup .gape-desktop-table {
                display: block !important;
                overflow-x: auto;
                scrollbar-width: none;
                -webkit-overflow-scrolling: touch;
            }

            .aac-subgroup .table {
                min-width: 760px;
            }

            .aac-grade-doc__head,
            .aac-certificate-doc,
            .aac-certificate-doc__identity,
            .aac-certificate-doc__signatures {
                grid-template-columns: minmax(0, 1fr);
            }

            .aac-certificate-doc__rail {
                display: none;
            }

            .aac-grade-doc__head {
                grid-template-areas:
                    "posted"
                    "institution"
                    "entity"
                    "title"
                    "period";
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

            .aac-certificate-doc h3 {
                font-size: 24px;
            }
        }

        /*
         * Subject Details > Structure is the reference for management rows:
         * one grid owns the columns, hover never changes a row's geometry and
         * every disclosure action occupies the same fixed slot.  Keeping this
         * contract below the legacy table declarations makes it the single
         * source of layout truth for Enrollments, Grades, Certificates and
         * Attendance without leaking into student or unrelated dashboard pages.
         */
        .aac-page .gape-structured-management-panel {
            --aac-structure-columns: minmax(250px, 1.5fr) minmax(190px, 1fr) minmax(106px, .45fr) minmax(136px, .62fr) minmax(120px, auto);
        }

        .aac-page .gape-structured-management-panel .gape-desktop-table > table > thead > tr,
        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr[data-gape-sort-row],
        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr.aac-attendance-summary-row,
        .aac-page .gape-deferred-management-archive-row {
            grid-template-columns: var(--aac-structure-columns);
        }

        .aac-page .gape-structured-management-panel .gape-desktop-table > table > thead > tr > th,
        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr[data-gape-sort-row] > td,
        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr.aac-attendance-summary-row > td {
            min-width: 0 !important;
            width: auto !important;
        }

        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr[data-gape-sort-row] > td:last-child,
        .aac-page .gape-structured-management-panel .gape-desktop-table > table > tbody > tr.aac-attendance-summary-row > td:last-child {
            align-items: center;
            display: flex;
            justify-content: flex-end;
        }

        .aac-page .gape-structured-management-panel tr.hover-bg-neutral-20:hover,
        .aac-page .aac-tree-node:hover {
            background-color: #fff !important;
            transform: none;
        }

        /* An inset marker keeps opened content visually connected without
           consuming horizontal space and displacing its child columns. */
        .aac-page .aac-expanded-cell {
            border-left: 0 !important;
            box-shadow: inset 4px 0 0 #18a34a;
        }

        .aac-page .aac-tree-row {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: minmax(0, 1.55fr) minmax(76px, .38fr) minmax(150px, .72fr) 120px;
            justify-content: normal;
        }

        .aac-page .aac-tree-main {
            flex: none;
            grid-column: 1;
            min-width: 0;
        }

        .aac-page .aac-tree-count {
            flex: none;
            grid-column: 2;
            min-width: 0;
        }

        .aac-page .aac-tree-status {
            flex: none;
            grid-column: 3;
            min-width: 0;
        }

        .aac-page .aac-tree-actions {
            display: flex;
            flex: none;
            flex-wrap: nowrap;
            gap: 10px;
            grid-column: 4;
            justify-content: flex-end;
            min-width: 0;
            width: 120px;
        }

        .aac-page .aac-icon-button {
            box-sizing: border-box;
            flex: 0 0 32px;
            line-height: 1;
            margin: 0 !important;
            min-height: 32px;
            min-width: 32px;
            transform: none !important;
        }

        .aac-page .aac-icon-button[data-bs-toggle="collapse"] i {
            transition: transform .2s ease;
        }

        .aac-page .aac-icon-button[data-bs-toggle="collapse"][aria-expanded="true"] i {
            transform: rotate(180deg);
        }

        .aac-page .aac-icon-button:active {
            background-color: var(--main-50) !important;
            color: var(--main-600) !important;
            transform: none !important;
        }

        @media (max-width: 575.98px) {
            .aac-page .aac-tree-row {
                align-items: flex-start;
                grid-template-columns: minmax(0, 1fr);
            }

            .aac-page .aac-tree-main,
            .aac-page .aac-tree-count,
            .aac-page .aac-tree-status,
            .aac-page .aac-tree-actions {
                grid-column: auto;
                width: 100%;
            }

            .aac-page .aac-tree-actions {
                justify-content: flex-start;
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

                <div class="aac-page" data-aac-tabs data-aac-default-panel="${attendanceOnly ? 'attendance' : 'enrollments'}">
                    <c:if test="${not attendanceOnly}">
                    <div class="la-mode-grid mb-20" role="tablist" aria-label="Enrollment, grade and certificate sections">
                        <button type="button" class="la-mode-card is-active" data-aac-tab="enrollments" role="tab" aria-selected="true" aria-controls="enrollments">
                            <span class="la-mode-icon bg-main-50 text-main-600 text-24"><i class="ph ph-student"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Enrollments</span>
                                <span class="text-13 text-main-600 fw-semibold">${enrollmentCount} enrollments | ${activeEnrollmentCount} active</span>
                            </span>
                        </button>
                        <button type="button" class="la-mode-card" data-aac-tab="grades" role="tab" aria-selected="false" aria-controls="grades">
                            <span class="la-mode-icon bg-warning-50 text-warning-600 text-24"><i class="ph ph-seal-check"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Grades</span>
                                <span class="text-13 text-main-600 fw-semibold">${gradeRecordCount} grades | ${gradeSheetCount} sheets</span>
                            </span>
                        </button>
                        <button type="button" class="la-mode-card" data-aac-tab="certificates" role="tab" aria-selected="false" aria-controls="certificates">
                            <span class="la-mode-icon bg-info-50 text-info-600 text-24"><i class="ph ph-certificate"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Certificates</span>
                                <span class="text-13 text-main-600 fw-semibold">${certificateCount} certificates | ${publishedGradeSheetCount} published sheets</span>
                            </span>
                        </button>
                    </div>
                    </c:if>

                <c:if test="${not attendanceOnly}">
                <section id="enrollments" class="ad-tab-panel" data-aac-panel="enrollments" role="tabpanel">
                <div class="aac-surface gape-structured-management-panel px-24 py-24 mb-24"
                     data-gape-sort-root
                     data-gape-group-item-label="student"
                     data-deferred-management-root
                     data-deferred-management-kind="enrollments"
                     data-deferred-management-endpoint="${pageContext.request.contextPath}${attendanceBasePath}"
                     data-deferred-management-page-parameter="enrollmentsPage"
                     data-deferred-management-scope-parameter="enrollmentsScope"
                     data-deferred-management-load-all-parameter="enrollmentsLoadAll"
                     data-deferred-management-active-scope="active"
                     data-deferred-management-archive-scope="completed"
                     data-deferred-management-current-page="${enrollmentManagementCurrentPage}"
                     data-deferred-management-total="${enrollmentManagementTotal}"
                     data-deferred-management-page-size="10"
                     data-deferred-management-showing-all="${enrollmentManagementLoadAll}"
                     data-deferred-management-archive-total="${enrollmentManagementCompletedTotal}">
                    <c:set var="enrollmentManagementIdPrefix" value="${enrollmentManagementScope eq 'completed' ? 'completed_' : 'active_'}"/>
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <c:choose>
                                <c:when test="${enrollmentManagementScope eq 'completed'}"><h2 class="text-18 fw-medium text-neutral-700 mb-4">Completed Enrollments</h2></c:when>
                                <c:otherwise><h2 class="text-18 fw-medium text-neutral-700 mb-4">Enrollments</h2></c:otherwise>
                            </c:choose>
                            <div class="gape-management-summary" aria-label="Enrollment summary">
                                <span>${enrollmentCount} enrollments</span>
                                <span>${activeEnrollmentCount} active</span>
                                <span>${pendingEnrollmentCount} pending</span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-enrollment-controls>
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-sort-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-sort-ascending"></i>Sort by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>Name</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-type="date" data-sort-state="none" aria-pressed="false">
                                                <span>Date</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>State</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                            <span class="gape-management-card__icon bg-main-50 text-main-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                                <i class="ph ph-student"></i>
                            </span>
                        </div>
                    </div>
                    <div data-deferred-management-active-content>
                    <div data-deferred-management-page-content>
                    <div class="gape-desktop-table">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Student</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Email</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Enrollments</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-gape-sort-list>
                            <c:forEach var="group" items="${enrollmentGroups}" varStatus="enrollmentLoop">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-gape-sort-row
                                    data-gape-detail-id="enrollmentGroup${enrollmentManagementIdPrefix}${group.studentUserId}"
                                    data-gape-mobile-id="enrollmentMobileGroup${enrollmentManagementIdPrefix}${group.studentUserId}"
                                    data-sort-index="${enrollmentLoop.index}"
                                    data-sort-name="${fn:escapeXml(group.studentDisplayLabel)}"
                                    data-sort-email="${fn:escapeXml(group.studentEmail)}"
                                    data-sort-date="${fn:escapeXml(group.primaryEnrollment.startDateSort)}"
                                    data-sort-status="${fn:escapeXml(group.primaryEnrollment.stateLabel)}"
                                    data-sort-course-count="${group.enrollmentCount}"
                                    data-sort-total-count="${group.enrollmentCount}">
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="fw-medium text-neutral-700"><c:out value="${group.studentDisplayLabel}"/></span>
                                        <span class="d-block text-12">${group.enrollmentCount} enrollments</span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${group.studentEmail}"/>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">${group.enrollmentCount}</td>
                                    <td class="py-20 px-20">
                                        <div class="aac-state-summary">
                                            <c:forEach var="summary" items="${group.stateSummaries}">
                                                <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                            </c:forEach>
                                            <c:if test="${empty group.stateSummaries}">
                                                <span class="aac-state-empty">No enrollments</span>
                                            </c:if>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show all enrollments" data-bs-toggle="collapse" data-bs-target="#enrollmentGroup${enrollmentManagementIdPrefix}${group.studentUserId}" aria-expanded="false">
                                            <i class="ph ph-caret-down"></i>
                                        </button>
                                    </td>
                                </tr>
                                <tr class="collapse" id="enrollmentGroup${enrollmentManagementIdPrefix}${group.studentUserId}">
                                    <td colspan="5" class="aac-expanded-cell">
                                        <div class="aac-expanded-panel">
                                            <div class="aac-expanded-panel__header">
                                                <div>
                                                    <h3 class="text-16 fw-semibold text-neutral-800 mb-4">Enrollments</h3>
                                                    <span class="text-13 text-neutral-600"><c:out value="${group.studentDisplayLabel}"/> | ${group.enrollmentCount} enrollment contexts</span>
                                                </div>
                                            </div>
                                            <div class="aac-tree">
                                                <c:forEach var="courseGroup" items="${group.courseGroups}">
                                                    <c:set var="item" value="${courseGroup.enrollment}"/>
                                                    <c:set var="enrollmentModalId" value="enrollment${enrollmentManagementIdPrefix}${item.modalKey}"/>
                                                    <article class="aac-tree-node aac-tree-node--course">
                                                        <div class="aac-tree-row">
                                                            <div class="aac-tree-main">
                                                                <span class="aac-tree-icon bg-main-50 text-main-600"><i class="ph ph-graduation-cap"></i></span>
                                                                <div class="min-w-0">
                                                                    <span class="aac-tree-title"><c:out value="${item.contextLabel}"/></span>
                                                                    <span class="aac-tree-meta" data-gape-datetime-display><c:out value="${item.periodLabel}"/></span>
                                                                </div>
                                                            </div>
                                                            <div class="aac-tree-count">
                                                                <span class="aac-tree-count-value">${courseGroup.classGroupEnrollmentCount}</span>
                                                            </div>
                                                            <div class="aac-tree-status">
                                                                <div class="aac-state-summary">
                                                                    <c:forEach var="summary" items="${courseGroup.classGroupStateSummaries}">
                                                                        <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                                                    </c:forEach>
                                                                    <c:if test="${empty courseGroup.classGroupStateSummaries}">
                                                                        <span class="${item.stateBadgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12"><c:out value="${item.stateLabel}"/></span>
                                                                    </c:if>
                                                                </div>
                                                            </div>
                                                            <div class="aac-tree-actions">
                                                                 <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show class group enrollments" data-bs-toggle="collapse" data-bs-target="#courseClasses${enrollmentManagementIdPrefix}${item.modalKey}" aria-expanded="false"><i class="ph ph-caret-down"></i></button>
                                                                 <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                 <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                            </div>
                                                        </div>
                                                        <div class="collapse aac-tree-children" id="courseClasses${enrollmentManagementIdPrefix}${item.modalKey}">
                                                            <c:forEach var="classGroup" items="${courseGroup.classGroups}">
                                                                <c:set var="item" value="${classGroup.enrollment}"/>
                                                                <c:set var="enrollmentModalId" value="enrollment${enrollmentManagementIdPrefix}${item.modalKey}"/>
                                                                <article class="aac-tree-node aac-tree-node--class">
                                                                    <div class="aac-tree-row">
                                                                        <div class="aac-tree-main">
                                                                            <span class="aac-tree-icon bg-warning-50 text-warning-600"><i class="ph ph-users-three"></i></span>
                                                                            <div class="min-w-0">
                                                                                <span class="aac-tree-title"><c:out value="${item.contextLabel}"/></span>
                                                                                <span class="aac-tree-meta"><c:out value="${item.contextDetail}"/></span>
                                                                            </div>
                                                                        </div>
                                                                        <div class="aac-tree-count">
                                                                            <span class="aac-tree-count-value">${classGroup.assessmentEnrollmentCount}</span>
                                                                        </div>
                                                                        <div class="aac-tree-status">
                                                                            <div class="aac-state-summary">
                                                                                <c:forEach var="summary" items="${classGroup.assessmentStateSummaries}">
                                                                                    <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                                                                </c:forEach>
                                                                                <c:if test="${empty classGroup.assessmentStateSummaries}">
                                                                                    <span class="${item.stateBadgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12"><c:out value="${item.stateLabel}"/></span>
                                                                                </c:if>
                                                                            </div>
                                                                        </div>
                                                                        <div class="aac-tree-actions">
                                                                            <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show assessment enrollments" data-bs-toggle="collapse" data-bs-target="#classAssessments${enrollmentManagementIdPrefix}${item.modalKey}" aria-expanded="false"><i class="ph ph-caret-down"></i></button>
                                                                            <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                            <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                                        </div>
                                                                    </div>
                                                                    <div class="collapse aac-tree-children" id="classAssessments${enrollmentManagementIdPrefix}${item.modalKey}">
                                                                        <c:forEach var="item" items="${classGroup.assessmentEnrollments}">
                                                                            <c:set var="enrollmentModalId" value="enrollment${enrollmentManagementIdPrefix}${item.modalKey}"/>
                                                                            <article class="aac-tree-node aac-tree-node--assessment">
                                                                                <div class="aac-tree-row">
                                                                                    <div class="aac-tree-main">
                                                                                        <span class="aac-tree-icon bg-info-50 text-info-600"><i class="ph ph-clipboard-text"></i></span>
                                                                                        <div class="min-w-0">
                                                                                            <span class="aac-tree-title"><c:out value="${item.contextLabel}"/></span>
                                                                                            <span class="aac-tree-meta"><c:out value="${item.contextDetail}"/></span>
                                                                                        </div>
                                                                                    </div>
                                                                                    <div class="aac-tree-count">
                                                                                        <span class="aac-tree-count-value">0</span>
                                                                                    </div>
                                                                                    <div class="aac-tree-status">
                                                                                        <div class="aac-state-summary">
                                                                                            <span class="${item.stateBadgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12"><c:out value="${item.stateLabel}"/></span>
                                                                                        </div>
                                                                                    </div>
                                                                                    <div class="aac-tree-actions">
                                                                                        <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                                        <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                                                    </div>
                                                                                </div>
                                                                            </article>
                                                                        </c:forEach>
                                                                        <c:if test="${empty classGroup.assessmentEnrollments}">
                                                                            <div class="aac-tree-empty">No assessment enrollments for this class group.</div>
                                                                        </c:if>
                                                                    </div>
                                                                </article>
                                                            </c:forEach>
                                                            <c:if test="${empty courseGroup.classGroups}">
                                                                <div class="aac-tree-empty">No class group enrollments for this course.</div>
                                                            </c:if>
                                                        </div>
                                                    </article>
                                                </c:forEach>
                                                <c:if test="${not empty group.unattachedEnrollments}">
                                                    <div class="aac-tree-empty">Other enrollment contexts</div>
                                                    <c:forEach var="item" items="${group.unattachedEnrollments}">
                                                        <c:set var="enrollmentModalId" value="enrollment${enrollmentManagementIdPrefix}${item.modalKey}"/>
                                                        <article class="aac-tree-node aac-tree-node--assessment">
                                                            <div class="aac-tree-row">
                                                                <div class="aac-tree-main">
                                                                    <c:choose>
                                                                        <c:when test="${item.classGroup}"><span class="aac-tree-icon bg-warning-50 text-warning-600"><i class="ph ph-users-three"></i></span></c:when>
                                                                        <c:when test="${item.assessment}"><span class="aac-tree-icon bg-info-50 text-info-600"><i class="ph ph-clipboard-text"></i></span></c:when>
                                                                        <c:otherwise><span class="aac-tree-icon bg-neutral-20 text-neutral-600"><i class="ph ph-link"></i></span></c:otherwise>
                                                                    </c:choose>
                                                                    <div class="min-w-0">
                                                                        <span class="aac-tree-title"><c:out value="${item.contextType}"/> — <c:out value="${item.contextLabel}"/></span>
                                                                        <span class="aac-tree-meta"><c:out value="${item.contextDetail}"/></span>
                                                                    </div>
                                                                </div>
                                                                <div class="aac-tree-count"><span class="aac-tree-count-value">1</span></div>
                                                                <div class="aac-tree-status"><span class="${item.stateBadgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12"><c:out value="${item.stateLabel}"/></span></div>
                                                                <div class="aac-tree-actions">
                                                                    <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                    <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                                </div>
                                                            </div>
                                                        </article>
                                                    </c:forEach>
                                                </c:if>
                                                <c:if test="${empty group.courseGroups and empty group.unattachedEnrollments}">
                                                    <div class="aac-tree-empty">No enrollment contexts for this student.</div>
                                                </c:if>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty enrollmentGroups}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No enrollments available.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <div class="gape-mobile-list" data-gape-mobile-list>
                        <c:forEach var="group" items="${enrollmentGroups}">
                            <article class="gape-mobile-row"
                                     data-gape-mobile-row
                                     data-gape-mobile-row-id="enrollmentMobileGroup${enrollmentManagementIdPrefix}${group.studentUserId}">
                                <div class="gape-mobile-row__header">
                                    <div>
                                        <div class="gape-mobile-title"><c:out value="${group.studentDisplayLabel}"/></div>
                                        <div class="gape-mobile-subtitle"><c:out value="${group.studentEmail}"/></div>
                                    </div>
                                    <span class="gape-mobile-badge px-12 py-7 border-neutral-30 border rounded-pill text-12">${group.enrollmentCount} enrollments</span>
                                </div>
                                <div class="gape-mobile-kv">
                                    <strong>Email</strong>
                                    <span><c:out value="${group.studentEmail}"/></span>
                                    <strong>Enrollments</strong>
                                    <span>${group.enrollmentCount}</span>
                                    <strong>State</strong>
                                    <span>
                                        <span class="aac-state-summary">
                                            <c:forEach var="summary" items="${group.stateSummaries}">
                                                <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                            </c:forEach>
                                            <c:if test="${empty group.stateSummaries}">
                                                <span class="aac-state-empty">No enrollments</span>
                                            </c:if>
                                        </span>
                                    </span>
                                    <strong>Total enrollments</strong>
                                    <span>${group.enrollmentCount}</span>
                                </div>
                            </article>
                        </c:forEach>
                        <c:if test="${empty enrollmentGroups}">
                            <div class="gape-mobile-empty">No enrollments available.</div>
                        </c:if>
                    </div>
                    <c:forEach var="group" items="${enrollmentGroups}">
                        <c:forEach var="item" items="${group.enrollments}">
                            <c:set var="enrollmentModalId" value="enrollment${enrollmentManagementIdPrefix}${item.modalKey}"/>
                            <div class="modal fade" id="${enrollmentModalId}Detail" tabindex="-1" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content rounded-8 border-0">
                                        <div class="modal-header border-neutral-30">
                                            <h5 class="modal-title text-18 fw-semibold">Enrollment detail</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body">
                                            <p class="text-14 text-neutral-600 mb-12"><strong><c:out value="${item.studentName}"/></strong><span class="d-block text-12"><c:out value="${item.studentEmail}"/></span></p>
                                            <div class="row gy-3">
                                                <div class="col-md-6"><span class="text-12 text-neutral-500 d-block mb-6">Type</span><strong class="text-14 text-neutral-800"><c:out value="${item.contextType}"/></strong></div>
                                                <div class="col-md-6"><span class="text-12 text-neutral-500 d-block mb-6">Context</span><strong class="text-14 text-neutral-800"><c:out value="${item.contextLabel}"/></strong></div>
                                                <div class="col-md-6"><span class="text-12 text-neutral-500 d-block mb-6">State</span><span class="${item.stateBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12"><c:out value="${item.stateLabel}"/></span></div>
                                                <div class="col-md-6"><span class="text-12 text-neutral-500 d-block mb-6">Occurrence period (read-only)</span><strong class="text-14 text-neutral-800" data-gape-datetime-display><c:out value="${item.periodLabel}"/></strong></div>
                                                <div class="col-12"><span class="text-12 text-neutral-500 d-block mb-6">Details</span><span class="text-14 text-neutral-700"><c:out value="${item.contextDetail}"/></span></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="modal fade" id="${enrollmentModalId}Delete" tabindex="-1" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content rounded-8 border-0">
                                        <div class="modal-header border-neutral-30">
                                            <h5 class="modal-title text-18 fw-semibold">Delete enrollment</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body">
                                            <p class="text-14 text-neutral-600 mb-0">Delete <strong><c:out value="${item.contextType}"/> - <c:out value="${item.contextLabel}"/></strong> for <strong><c:out value="${item.studentName}"/></strong>?</p>
                                        </div>
                                        <div class="modal-footer border-neutral-30">
                                            <button type="button" class="border border-neutral-30 text-neutral-600 bg-white px-20 py-10 rounded-8 fw-semibold" data-bs-dismiss="modal">Cancel</button>
                                            <form action="${pageContext.request.contextPath}${item.deleteAction}" method="post" class="m-0">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <input type="hidden" name="returnTo" value="${pageContext.request.contextPath}/learning/attendance#enrollments">
                                                <button type="submit" class="bg-danger-600 px-20 py-10 rounded-8 fw-semibold text-white border-0">Delete</button>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </c:forEach>
                    </div>
                    </div>
                    <div class="gape-learning-management-load-state mt-20" data-deferred-management-load-state>
                        <span><strong>${enrollmentManagementTotal}</strong> current enrollment groups load in pages of 10.</span>
                        <c:if test="${enrollmentManagementCompletedTotal > 0}">
                            <span><strong>${enrollmentManagementCompletedTotal}</strong> completed enrollments load only when their section is opened.</span>
                        </c:if>
                    </div>
                    <div class="d-flex justify-content-end mt-12" data-deferred-management-pagination data-total="${enrollmentManagementTotal}" data-page-size="10" ${enrollmentManagementTotal > 10 ? '' : 'hidden'}>
                        <div class="gape-list-pagination">
                            <nav class="gape-list-pagination-pages" aria-label="Enrollment pages" data-deferred-management-pagination-pages></nav>
                            <button type="button" class="gape-list-load-all" data-deferred-management-load-all>Load All</button>
                        </div>
                    </div>
                    <c:if test="${enrollmentManagementCompletedTotal > 0}">
                        <section class="gape-learning-management-completed-wrapper mt-20" data-deferred-management-archive>
                            <div class="gape-learning-management-completed-divider">Completed Enrollments</div>
                            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                                <div class="gape-deferred-management-archive-row">
                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                        <span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span>
                                        <div class="min-w-0">
                                            <span class="fw-medium text-14 text-neutral-700 d-block">Completed Enrollments</span>
                                            <span class="gape-node-meta text-12"><span>Past course-occurrence enrollments</span></span>
                                        </div>
                                    </div>
                                    <div class="gape-deferred-management-archive-spacer"></div>
                                    <div><span class="cd-element-count">${enrollmentManagementCompletedTotal}</span></div>
                                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-deferred-management-archive-toggle aria-expanded="false" title="Show completed enrollments" aria-label="Show completed enrollments"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                                </div>
                                <div class="gape-learning-management-completed-panel d-none" data-deferred-management-archive-panel>
                                    <div class="gape-learning-management-completed-content d-flex flex-column gap-12" data-deferred-management-archive-content>
                                        <div class="gape-learning-management-deferred-copy text-13 text-neutral-500">Open this section to load ${enrollmentManagementCompletedTotal} completed enrollments.</div>
                                    </div>
                                </div>
                            </article>
                        </section>
                    </c:if>

                </div>
                </section>

                <section id="grades" class="ad-tab-panel" data-aac-panel="grades" role="tabpanel" hidden>
                <div class="aac-surface gape-structured-management-panel px-24 py-24 mb-24"
                     data-gape-sort-root
                     data-gape-group-item-label="grade sheet"
                     data-deferred-management-root
                     data-deferred-management-kind="grades"
                     data-deferred-management-endpoint="${pageContext.request.contextPath}${attendanceBasePath}"
                     data-deferred-management-page-parameter="gradesPage"
                     data-deferred-management-scope-parameter="gradesScope"
                     data-deferred-management-load-all-parameter="gradesLoadAll"
                     data-deferred-management-active-scope="active"
                     data-deferred-management-archive-scope="published"
                     data-deferred-management-current-page="${gradeManagementCurrentPage}"
                     data-deferred-management-total="${gradeManagementTotal}"
                     data-deferred-management-page-size="10"
                     data-deferred-management-showing-all="${gradeManagementLoadAll}"
                     data-deferred-management-archive-total="${gradeManagementPublishedTotal}">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <c:choose>
                                <c:when test="${gradeManagementScope eq 'published'}"><h2 class="text-18 fw-medium text-neutral-700 mb-4">Published Grades</h2></c:when>
                                <c:otherwise><h2 class="text-18 fw-medium text-neutral-700 mb-4">Grades</h2></c:otherwise>
                            </c:choose>
                            <div class="gape-management-summary" aria-label="Grade summary">
                                <span>${gradeRecordCount} grades</span>
                                <span>${gradeSheetCount} grade sheets</span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-grade-controls>
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-sort-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-sort-ascending"></i>Sort by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>Name</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-type="date" data-sort-state="none" aria-pressed="false">
                                                <span>Date</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>State</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-group-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-stack"></i>Group by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="organization" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Organization</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="course" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Courses</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="subject" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Subjects</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="classGroup" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Class Groups</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                            <span class="gape-management-card__icon bg-warning-50 text-warning-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                                <i class="ph ph-seal-check"></i>
                            </span>
                        </div>
                    </div>
                    <div data-deferred-management-active-content>
                    <div data-deferred-management-page-content>
                    <%@ include file="/WEB-INF/fragments/learning-grades-certificates-content.jspf" %>
                    </div>
                    </div>

                    <div class="gape-learning-management-load-state mt-20" data-deferred-management-load-state>
                        <span><strong>${gradeManagementTotal}</strong> current grade groups load in pages of 10.</span>
                        <c:if test="${gradeManagementPublishedTotal > 0}">
                            <span><strong>${gradeManagementPublishedTotal}</strong> published grade groups load only when their section is opened.</span>
                        </c:if>
                    </div>
                    <div class="d-flex justify-content-end mt-12" data-deferred-management-pagination data-total="${gradeManagementTotal}" data-page-size="10" ${gradeManagementTotal > 10 ? '' : 'hidden'}>
                        <div class="gape-list-pagination">
                            <nav class="gape-list-pagination-pages" aria-label="Grade pages" data-deferred-management-pagination-pages></nav>
                            <button type="button" class="gape-list-load-all" data-deferred-management-load-all>Load All</button>
                        </div>
                    </div>
                    <c:if test="${gradeManagementPublishedTotal > 0}">
                        <section class="gape-learning-management-completed-wrapper mt-20" data-deferred-management-archive>
                            <div class="gape-learning-management-completed-divider">Published Grades</div>
                            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                                <div class="gape-deferred-management-archive-row">
                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                        <span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span>
                                        <div class="min-w-0">
                                            <span class="fw-medium text-14 text-neutral-700 d-block">Published Grades</span>
                                            <span class="gape-node-meta text-12"><span>Released occurrence grade sheets</span></span>
                                        </div>
                                    </div>
                                    <div class="gape-deferred-management-archive-spacer"></div>
                                    <div><span class="cd-element-count">${gradeManagementPublishedTotal}</span></div>
                                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Published</span></div>
                                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-deferred-management-archive-toggle aria-expanded="false" title="Show published grades" aria-label="Show published grades"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                                </div>
                                <div class="gape-learning-management-completed-panel d-none" data-deferred-management-archive-panel>
                                    <div class="gape-learning-management-completed-content d-flex flex-column gap-12" data-deferred-management-archive-content>
                                        <div class="gape-learning-management-deferred-copy text-13 text-neutral-500">Open this section to load ${gradeManagementPublishedTotal} published grade groups.</div>
                                    </div>
                                </div>
                            </article>
                        </section>
                    </c:if>
                </div>

                </section>

                <section id="certificates" class="ad-tab-panel" data-aac-panel="certificates" role="tabpanel" hidden>
                <div class="aac-surface gape-structured-management-panel px-24 py-24 mb-24"
                     data-gape-sort-root
                     data-gape-group-item-label="student"
                     data-deferred-management-root
                     data-deferred-management-kind="certificates"
                     data-deferred-management-endpoint="${pageContext.request.contextPath}${attendanceBasePath}"
                     data-deferred-management-page-parameter="certificatesPage"
                     data-deferred-management-scope-parameter="certificatesScope"
                     data-deferred-management-load-all-parameter="certificatesLoadAll"
                     data-deferred-management-active-scope="active"
                     data-deferred-management-archive-scope="published"
                     data-deferred-management-current-page="${certificateManagementCurrentPage}"
                     data-deferred-management-total="${certificateManagementTotal}"
                     data-deferred-management-page-size="10"
                     data-deferred-management-showing-all="${certificateManagementLoadAll}"
                     data-deferred-management-archive-total="${certificateManagementPublishedTotal}">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <c:choose>
                                <c:when test="${certificateManagementScope eq 'published'}"><h2 class="text-18 fw-medium text-neutral-700 mb-4">Published Certificates</h2></c:when>
                                <c:otherwise><h2 class="text-18 fw-medium text-neutral-700 mb-4">Certificates</h2></c:otherwise>
                            </c:choose>
                            <div class="gape-management-summary" aria-label="Certificate summary">
                                <span>${certificateCount} certificates</span>
                                <span>${publishedGradeSheetCount} published sheets</span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-certificate-controls>
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-sort-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-sort-ascending"></i>Sort by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>Name</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-type="date" data-sort-state="none" aria-pressed="false">
                                                <span>Date</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>State</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                            <span class="gape-management-card__icon bg-info-50 text-info-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                                <i class="ph ph-certificate"></i>
                            </span>
                        </div>
                    </div>
                    <div data-deferred-management-active-content>
                    <div data-deferred-management-page-content>
                    <%@ include file="/WEB-INF/fragments/learning-certificates-content.jspf" %>
                    </div>
                    </div>

                    <div class="gape-learning-management-load-state mt-20" data-deferred-management-load-state>
                        <span><strong>${certificateManagementTotal}</strong> current certificate groups load in pages of 10.</span>
                        <c:if test="${certificateManagementPublishedTotal > 0}">
                            <span><strong>${certificateManagementPublishedTotal}</strong> published certificates load only when their section is opened.</span>
                        </c:if>
                    </div>
                    <div class="d-flex justify-content-end mt-12" data-deferred-management-pagination data-total="${certificateManagementTotal}" data-page-size="10" ${certificateManagementTotal > 10 ? '' : 'hidden'}>
                        <div class="gape-list-pagination">
                            <nav class="gape-list-pagination-pages" aria-label="Certificate pages" data-deferred-management-pagination-pages></nav>
                            <button type="button" class="gape-list-load-all" data-deferred-management-load-all>Load All</button>
                        </div>
                    </div>
                    <c:if test="${certificateManagementPublishedTotal > 0}">
                        <section class="gape-learning-management-completed-wrapper mt-20" data-deferred-management-archive>
                            <div class="gape-learning-management-completed-divider">Published Certificates</div>
                            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                                <div class="gape-deferred-management-archive-row">
                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                        <span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span>
                                        <div class="min-w-0">
                                            <span class="fw-medium text-14 text-neutral-700 d-block">Published Certificates</span>
                                            <span class="gape-node-meta text-12"><span>Issued student certificates</span></span>
                                        </div>
                                    </div>
                                    <div class="gape-deferred-management-archive-spacer"></div>
                                    <div><span class="cd-element-count">${certificateManagementPublishedTotal}</span></div>
                                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Published</span></div>
                                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-deferred-management-archive-toggle aria-expanded="false" title="Show published certificates" aria-label="Show published certificates"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                                </div>
                                <div class="gape-learning-management-completed-panel d-none" data-deferred-management-archive-panel>
                                    <div class="gape-learning-management-completed-content d-flex flex-column gap-12" data-deferred-management-archive-content>
                                        <div class="gape-learning-management-deferred-copy text-13 text-neutral-500">Open this section to load ${certificateManagementPublishedTotal} published certificates.</div>
                                    </div>
                                </div>
                            </article>
                        </section>
                    </c:if>
                </div>
                </section>
                </c:if>

                <section id="attendance" class="ad-tab-panel" data-aac-panel="attendance" role="tabpanel" hidden>
                <div class="aac-surface gape-structured-management-panel px-24 py-24 mb-24"
                     data-deferred-management-root
                     data-deferred-management-kind="attendance"
                     data-deferred-management-endpoint="${pageContext.request.contextPath}${attendanceBasePath}"
                     data-deferred-management-page-parameter="attendancePage"
                     data-deferred-management-scope-parameter="attendanceScope"
                     data-deferred-management-load-all-parameter="attendanceLoadAll"
                     data-deferred-management-active-scope="active"
                     data-deferred-management-current-page="${attendanceManagementCurrentPage}"
                     data-deferred-management-total="${attendanceManagementTotal}"
                     data-deferred-management-page-size="10"
                     data-deferred-management-showing-all="${attendanceManagementLoadAll}"
                     data-deferred-management-filter-name="attendanceState"
                     data-deferred-management-filter-value="${selectedAttendanceState}">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Attendance</h2>
                            <div class="gape-management-summary" aria-label="Attendance summary">
                                <span data-attendance-total-summary>${attendanceCount} records</span>
                                <span data-attendance-absence-summary>${absenceCount} absences</span>
                                <span data-attendance-pending-summary>${pendingJustificationCount} pending justifications</span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <form action="${pageContext.request.contextPath}${attendanceBasePath}" method="get" class="d-flex align-items-center gap-10 flex-wrap mb-0">
                                <select name="attendanceState" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 190px; min-height: 44px;" aria-label="Attendance state filter" onchange="this.form.submit()">
                                    <option value="">All states</option>
                                    <c:forEach var="option" items="${attendanceStateOptions}">
                                        <option value="${option.value}" ${option.selected ? 'selected' : ''}>
                                            <c:out value="${option.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </form>
                            <button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center" style="min-height: 44px;" data-bs-toggle="modal" data-bs-target="#registerAttendanceModal">
                                <i class="ph ph-plus-circle me-8"></i>New Attendance
                            </button>
                        </div>
                    </div>

                    <div class="modal fade" id="registerAttendanceModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-lg modal-dialog-centered">
                            <div class="modal-content rounded-8 border-0">
                                <div class="modal-header border-neutral-30">
                                    <div>
                                        <h5 class="modal-title text-18 fw-semibold mb-4">New Attendance</h5>
                                        <span class="text-13 text-neutral-500">Create a manual attendance record.</span>
                                    </div>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <form action="${pageContext.request.contextPath}${attendanceBasePath}" method="post">
                                    <div class="modal-body">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <input type="hidden" name="returnTo" value="${attendanceReturnTo}">
                                        <div class="row gy-3">
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-lesson-id">Lesson ID</label>
                                                <input type="number" id="attendance-lesson-id" name="lessonId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                            </div>
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-student-id">Student ID</label>
                                                <input type="number" id="attendance-student-id" name="studentUserId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                            </div>
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-status">State</label>
                                                <select id="attendance-status" name="status" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                                    <c:forEach var="option" items="${attendanceCreateStateOptions}">
                                                        <option value="${option.value}"><c:out value="${option.label}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-source">Source</label>
                                                <select id="attendance-source" name="source" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                                    <c:forEach var="option" items="${attendanceCreateSourceOptions}">
                                                        <option value="${option.value}"><c:out value="${option.label}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-check-in">Check-in</label>
                                                <input type="datetime-local" id="attendance-check-in" name="checkIn" value="${defaultCheckIn}" data-default-value="${defaultCheckIn}" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                            </div>
                                            <div class="col-md-6">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-check-out">Check-out</label>
                                                <input type="datetime-local" id="attendance-check-out" name="checkOut" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                            </div>
                                            <div class="col-12">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-notes">Notes</label>
                                                <input type="text" id="attendance-notes" name="notes" maxlength="500" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                            </div>
                                        </div>
                                    </div>
                                    <div class="modal-footer border-neutral-30">
                                        <button type="button" class="border border-neutral-30 bg-white text-neutral-600 px-18 py-10 rounded-8 fw-semibold" data-bs-dismiss="modal">Cancel</button>
                                        <button type="submit" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center">
                                            <i class="ph ph-check-circle me-8"></i>Save
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>

                    <div data-deferred-management-active-content>
                    <div data-deferred-management-page-content>
                    <div class="gape-desktop-table">
                        <table class="table mb-0 aac-attendance-table">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Student</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Activity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Attendance</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="group" items="${attendanceStudentGroups}">
                                <tr class="aac-attendance-summary-row hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="fw-medium text-neutral-700"><c:out value="${group.studentLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="fw-medium text-neutral-700 aac-attendance-count"><c:out value="${group.activityCountLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="d-block" data-gape-datetime-display><c:out value="${group.timeLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="aac-state-summary" data-attendance-group-summary="${group.studentUserId}">
                                            <c:forEach var="summary" items="${group.stateSummaries}">
                                                <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                            </c:forEach>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show all attendance activities" data-bs-toggle="collapse" data-bs-target="#attendanceGroup${group.studentUserId}" aria-expanded="false" aria-controls="attendanceGroup${group.studentUserId}">
                                            <i class="ph ph-caret-down"></i>
                                        </button>
                                    </td>
                                </tr>
                                <tr class="collapse" id="attendanceGroup${group.studentUserId}">
                                    <td colspan="5" class="aac-expanded-cell">
                                        <div class="aac-expanded-panel">
                                            <div class="aac-tree">
                                                <c:forEach var="activity" items="${group.activities}">
                                                    <article class="aac-tree-node aac-tree-node--class">
                                                        <div class="aac-tree-row">
                                                            <div class="aac-tree-main">
                                                                <span class="aac-tree-icon bg-main-two-50 text-main-two-600"><i class="ph ph-calendar-check"></i></span>
                                                                <div class="min-w-0">
                                                                    <span class="aac-tree-title"><c:out value="${activity.activityLabel}"/></span>
                                                                    <span class="aac-tree-meta" data-gape-datetime-display><c:out value="${activity.activityMeta}"/> | <c:out value="${activity.activityScheduleLabel}"/> | <c:out value="${activity.timeLabel}"/> | <c:out value="${activity.permanenceLabel}"/></span>
                                                                </div>
                                                            </div>
                                                            <div class="aac-tree-status">
                                                                <span class="${activity.stateBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12" data-attendance-activity-badge="${activity.id}" data-student-id="${group.studentUserId}" data-state-value="${activity.stateValue}">
                                                                    <c:out value="${activity.stateLabel}"/>
                                                                </span>
                                                            </div>
                                                            <div class="aac-tree-actions">
                                                                <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View attendance detail" data-bs-toggle="modal" data-bs-target="#${activity.detailModalId}">
                                                                    <i class="ph ph-eye"></i>
                                                                </button>
                                                                <c:if test="${activity.hasSettings}">
                                                                    <button type="button" class="aac-icon-button bg-info-50 text-info-600" title="Settings" aria-label="Configure attendance justification" data-bs-toggle="modal" data-bs-target="#${activity.settingsModalId}">
                                                                        <i class="ph ph-gear-six"></i>
                                                                    </button>
                                                                </c:if>
                                                            </div>
                                                        </div>
                                                    </article>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty attendanceStudentGroups}">
                                <tr>
                                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No attendance records available.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <div class="gape-mobile-list">
                        <c:forEach var="group" items="${attendanceStudentGroups}">
                            <article class="gape-mobile-row aac-attendance-summary-row">
                                <div class="gape-mobile-row__header">
                                    <div>
                                        <div class="gape-mobile-title"><c:out value="${group.studentLabel}"/></div>
                                    </div>
                                    <span class="gape-mobile-badge px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${group.activityCountLabel}"/></span>
                                </div>
                                <div class="gape-mobile-kv">
                                    <strong>State</strong>
                                    <span class="aac-state-summary" data-attendance-group-summary="${group.studentUserId}">
                                        <c:forEach var="summary" items="${group.stateSummaries}">
                                            <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                        </c:forEach>
                                    </span>
                                    <strong>Time</strong>
                                    <span data-gape-datetime-display><c:out value="${group.timeLabel}"/></span>
                                    <strong>Permanence</strong>
                                    <span data-gape-datetime-display><c:out value="${group.permanenceLabel}"/></span>
                                </div>
                                <div class="gape-mobile-actions">
                                    <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show all attendance activities" data-bs-toggle="collapse" data-bs-target="#attendanceMobileGroup${group.studentUserId}" aria-expanded="false" aria-controls="attendanceMobileGroup${group.studentUserId}">
                                        <i class="ph ph-caret-down"></i>
                                    </button>
                                </div>
                            </article>
                            <div class="collapse" id="attendanceMobileGroup${group.studentUserId}">
                                <div class="aac-expanded-panel mt-12 mb-12">
                                    <div class="aac-tree">
                                        <c:forEach var="activity" items="${group.activities}">
                                            <article class="aac-tree-node aac-tree-node--class">
                                                <div class="aac-tree-row">
                                                    <div class="aac-tree-main">
                                                        <span class="aac-tree-icon bg-main-two-50 text-main-two-600"><i class="ph ph-calendar-check"></i></span>
                                                        <div class="min-w-0">
                                                            <span class="aac-tree-title"><c:out value="${activity.activityLabel}"/></span>
                                                            <span class="aac-tree-meta" data-gape-datetime-display><c:out value="${activity.activityMeta}"/> | <c:out value="${activity.activityScheduleLabel}"/> | <c:out value="${activity.timeLabel}"/> | <c:out value="${activity.permanenceLabel}"/></span>
                                                        </div>
                                                    </div>
                                                    <div class="aac-tree-status">
                                                        <span class="${activity.stateBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12" data-attendance-activity-badge="${activity.id}" data-student-id="${group.studentUserId}" data-state-value="${activity.stateValue}">
                                                            <c:out value="${activity.stateLabel}"/>
                                                        </span>
                                                    </div>
                                                    <div class="aac-tree-actions">
                                                        <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View attendance detail" data-bs-toggle="modal" data-bs-target="#${activity.detailModalId}">
                                                            <i class="ph ph-eye"></i>
                                                        </button>
                                                        <c:if test="${activity.hasSettings}">
                                                            <button type="button" class="aac-icon-button bg-info-50 text-info-600" title="Settings" aria-label="Configure attendance justification" data-bs-toggle="modal" data-bs-target="#${activity.settingsModalId}">
                                                                <i class="ph ph-gear-six"></i>
                                                            </button>
                                                        </c:if>
                                                    </div>
                                                </div>
                                            </article>
                                        </c:forEach>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                        <c:if test="${empty attendanceStudentGroups}">
                            <div class="gape-mobile-empty">No attendance records available.</div>
                        </c:if>
                    </div>

                    <c:forEach var="activity" items="${attendanceActivities}">
                        <div class="modal fade" id="${activity.detailModalId}" tabindex="-1" aria-hidden="true">
                            <div class="modal-dialog modal-lg modal-dialog-centered">
                                <div class="modal-content rounded-8 border-0">
                                    <div class="modal-header border-neutral-30">
                                        <div>
                                            <h5 class="modal-title text-18 fw-semibold mb-4">Attendance detail</h5>
                                            <span class="text-13 text-neutral-500"><c:out value="${activity.activityLabel}"/></span>
                                        </div>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                    </div>
                                    <div class="modal-body">
                                        <div class="row gy-3">
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Student</span>
                                                <span class="text-14 fw-medium text-neutral-800"><c:out value="${activity.studentLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">State</span>
                                                <span class="${activity.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12" data-attendance-detail-state="${activity.id}" data-state-value="${activity.stateValue}"><c:out value="${activity.stateLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Activity</span>
                                                <span class="text-14 text-neutral-700"><c:out value="${activity.activityMeta}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Schedule</span>
                                                <span class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${activity.activityScheduleLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Time</span>
                                                <span class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${activity.timeLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Permanence</span>
                                                <span class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${activity.permanenceLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Source</span>
                                                <span class="text-14 text-neutral-700"><c:out value="${activity.sourceLabel}"/></span>
                                            </div>
                                            <div class="col-md-6">
                                                <span class="text-12 text-neutral-500 d-block mb-4">Notes</span>
                                                <span class="text-14 text-neutral-700"><c:out value="${activity.notes}"/></span>
                                            </div>
                                            <c:if test="${activity.hasJustification}">
                                                <div class="col-12">
                                                    <div class="border border-neutral-30 rounded-8 p-16 bg-neutral-20">
                                                        <span class="text-12 text-neutral-500 d-block mb-6">Justification</span>
                                                        <span class="text-14 text-neutral-700 d-block"><c:out value="${activity.justification.reason}"/></span>
                                                        <span class="text-12 text-neutral-500 d-block mt-6">Attachment: <c:out value="${activity.justification.attachmentLabel}"/></span>
                                                        <span class="text-12 text-neutral-500 d-block" data-gape-datetime-display>Submitted: <c:out value="${activity.justification.submittedAt}"/></span>
                                                        <span class="text-12 text-neutral-500 d-block" data-gape-datetime-display>Processed: <c:out value="${activity.justification.processedAt}"/></span>
                                                    </div>
                                                </div>
                                            </c:if>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <c:if test="${activity.hasSettings}">
                            <div class="modal fade" id="${activity.settingsModalId}" tabindex="-1" aria-hidden="true" data-attendance-settings-modal data-activity-id="${activity.id}">
                                <div class="modal-dialog modal-lg modal-dialog-centered">
                                    <div class="modal-content rounded-8 border-0 bg-white aac-settings-modal">
                                        <div class="modal-header border-neutral-30 bg-white">
                                            <div>
                                                <h5 class="modal-title text-18 fw-semibold mb-4">Attendance settings</h5>
                                                <span class="text-13 text-neutral-500"><c:out value="${activity.studentLabel}"/> | <c:out value="${activity.activityLabel}"/></span>
                                            </div>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body bg-white">
                                            <div class="aac-settings-grid">
                                                <section class="aac-settings-card">
                                                    <span class="text-12 text-neutral-500 d-block mb-8">Current state</span>
                                                    <span class="${activity.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12" data-attendance-settings-state data-state-value="${activity.stateValue}"><c:out value="${activity.stateLabel}"/></span>
                                                </section>
                                                <section class="aac-settings-card">
                                                    <span class="text-12 text-neutral-500 d-block mb-8">Submitted</span>
                                                    <span class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${activity.justification.submittedAt}"/></span>
                                                </section>
                                                <section class="aac-settings-card aac-settings-card--wide">
                                                    <span class="text-12 text-neutral-500 d-block mb-8">Reason</span>
                                                    <span class="text-14 text-neutral-700"><c:out value="${activity.justification.reason}"/></span>
                                                </section>
                                                <section class="aac-settings-card">
                                                    <span class="text-12 text-neutral-500 d-block mb-8">Attachment</span>
                                                    <span class="text-14 text-neutral-700"><c:out value="${activity.justification.attachmentLabel}"/></span>
                                                </section>
                                                <section class="aac-settings-card">
                                                    <span class="text-12 text-neutral-500 d-block mb-8">Processed</span>
                                                    <span class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${activity.justification.processedAt}"/></span>
                                                </section>
                                            </div>
                                            <form class="aac-settings-decision mt-18" data-attendance-settings-form data-activity-id="${activity.id}">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <input type="hidden" name="returnTo" value="${attendanceReturnTo}">
                                                <label class="text-13 text-neutral-600 mb-6 d-block" for="decision-notes-${activity.id}">Decision notes</label>
                                                <textarea id="decision-notes-${activity.id}" name="decisionNotes" maxlength="500" rows="3" class="form-control px-12 py-10 text-13 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Optional internal note"></textarea>
                                                <div class="aac-settings-actions mt-14">
                                                    <button type="button" class="border-0 bg-success-50 text-success-600 px-16 py-10 rounded-8 fw-semibold d-inline-flex align-items-center" data-attendance-decision="approve" data-action-url="${pageContext.request.contextPath}${attendanceBasePath}/justifications/${activity.justification.id}/approve" onclick="return window.handleAttendanceDecision ? window.handleAttendanceDecision(this) : false;" ${activity.canApprove ? '' : 'hidden'}>
                                                        <i class="ph ph-check me-8"></i>Accept
                                                    </button>
                                                    <button type="button" class="border-0 bg-danger-50 text-danger-600 px-16 py-10 rounded-8 fw-semibold d-inline-flex align-items-center" data-attendance-decision="reject" data-action-url="${pageContext.request.contextPath}${attendanceBasePath}/justifications/${activity.justification.id}/reject" onclick="return window.handleAttendanceDecision ? window.handleAttendanceDecision(this) : false;" ${activity.canReject ? '' : 'hidden'}>
                                                        <i class="ph ph-x me-8"></i>Reject
                                                    </button>
                                                    <span class="text-13 text-neutral-500" data-attendance-settings-message></span>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </c:if>
                    </c:forEach>
                    </div>
                    </div>
                    <div class="gape-learning-management-load-state mt-20" data-deferred-management-load-state>
                        <span><strong>${attendanceManagementTotal}</strong> attendance students load in pages of 10.</span>
                    </div>
                    <div class="d-flex justify-content-end mt-12" data-deferred-management-pagination data-total="${attendanceManagementTotal}" data-page-size="10" ${attendanceManagementTotal > 10 ? '' : 'hidden'}>
                        <div class="gape-list-pagination">
                            <nav class="gape-list-pagination-pages" aria-label="Attendance pages" data-deferred-management-pagination-pages></nav>
                            <button type="button" class="gape-list-load-all" data-deferred-management-load-all>Load All</button>
                        </div>
                    </div>
                </div>
                </section>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<script>
    (function () {
        var root = document.querySelector('[data-aac-tabs]');
        if (!root) {
            return;
        }
        var tabs = Array.prototype.slice.call(root.querySelectorAll('[data-aac-tab]'));
        var panels = Array.prototype.slice.call(root.querySelectorAll('[data-aac-panel]'));
        var defaultPanel = root.getAttribute('data-aac-default-panel') || 'enrollments';
        var aliases = {
            'grade-sheets': 'grades',
            'grade-records': 'grades',
            'certificates-list': 'certificates',
            justifications: 'attendance'
        };

        function hashValue() {
            return (window.location.hash || '').replace(/^#/, '');
        }

        function panelForTarget(hash) {
            if (!hash) {
                return defaultPanel;
            }
            if (aliases[hash]) {
                return aliases[hash];
            }
            if (hash.indexOf('gradeSheetDetail') === 0
                    || hash.indexOf('gradeCourseSheetDetail') === 0
                    || hash.indexOf('gradeSubjectSheetDetail') === 0
                    || hash.indexOf('gradeSheetSetup') === 0) {
                return 'grades';
            }
            if (hash.indexOf('certificateDetail') === 0) {
                return 'certificates';
            }
            if (hash.indexOf('attendanceDetail') === 0
                    || hash.indexOf('attendanceAssessmentDetail') === 0
                    || hash.indexOf('attendanceSettings') === 0) {
                return 'attendance';
            }
            if (hash.indexOf('enrollment') === 0) {
                return 'enrollments';
            }
            return hash;
        }

        function panelForHash() {
            return panelForTarget(hashValue());
        }

        function activate(panelName, updateHash) {
            var activePanel = panels.find(function (panel) {
                return panel.getAttribute('data-aac-panel') === panelName;
            });
            if (!activePanel) {
                panelName = defaultPanel;
            }
            tabs.forEach(function (tab) {
                var active = tab.getAttribute('data-aac-tab') === panelName;
                tab.classList.toggle('is-active', active);
                tab.setAttribute('aria-selected', active ? 'true' : 'false');
            });
            panels.forEach(function (panel) {
                panel.hidden = panel.getAttribute('data-aac-panel') !== panelName;
            });
            if (updateHash && window.history && window.history.replaceState) {
                window.history.replaceState(
                        null,
                        '',
                        window.location.pathname + window.location.search + '#' + panelName
                );
            }
        }

        function showHashModal(attempt) {
            var hash = hashValue();
            if (!hash) {
                return;
            }
            var target = document.getElementById(hash);
            if (!target || !target.classList.contains('modal')) {
                return;
            }
            window.setTimeout(function () {
                if (window.bootstrap && window.bootstrap.Modal) {
                    window.bootstrap.Modal.getOrCreateInstance(target).show();
                    return;
                }
                if ((attempt || 0) < 40) {
                    showHashModal((attempt || 0) + 1);
                }
            }, 50);
        }

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                activate(tab.getAttribute('data-aac-tab'), true);
            });
        });

        activate(panelForHash(), false);
        showHashModal();
        window.addEventListener('hashchange', function () {
            activate(panelForHash(), false);
            showHashModal();
        });

        var rawHash = hashValue();
        if (aliases[rawHash]) {
            window.setTimeout(function () {
                var anchor = document.getElementById(rawHash);
                if (anchor) {
                    anchor.scrollIntoView({block: 'start'});
                }
            }, 0);
        }
    }());

    (function () {
        var buttons = Array.prototype.slice.call(document.querySelectorAll('.aac-page [data-bs-toggle="collapse"][data-bs-target]'));

        function targetFor(button) {
            var selector = button.getAttribute('data-bs-target');
            if (!selector || selector.charAt(0) !== '#') {
                return null;
            }
            return document.getElementById(selector.slice(1));
        }

        function syncButton(button, expanded) {
            var icon = button.querySelector('i');
            button.classList.toggle('collapsed', !expanded);
            button.setAttribute('aria-expanded', expanded ? 'true' : 'false');
            if (!icon) {
                return;
            }
            icon.classList.toggle('ph-caret-down', !expanded);
            icon.classList.toggle('ph-caret-up', expanded);
        }

        document.addEventListener('click', function (event) {
            var button = event.target.closest('[data-bs-toggle="collapse"][data-bs-target]');
            if (!button || !button.closest('.aac-page')) {
                return;
            }
            syncButton(button, button.getAttribute('aria-expanded') !== 'true');
        }, true);

        buttons.forEach(function (button) {
            var target = targetFor(button);
            if (!target) {
                return;
            }
            syncButton(button, target.classList.contains('show'));
            target.addEventListener('shown.bs.collapse', function (event) {
                if (event.target === target) {
                    syncButton(button, true);
                }
            });
            target.addEventListener('hidden.bs.collapse', function (event) {
                if (event.target === target) {
                    syncButton(button, false);
                }
            });
        });
    }());

    (function () {
        var attendanceStateMeta = {
            present: {label: 'Present', badgeClass: 'bg-success-50 text-success-600'},
            absent: {label: 'Absent', badgeClass: 'bg-danger-50 text-danger-600'},
            requested: {label: 'Requested', badgeClass: 'bg-warning-50 text-warning-600'},
            justified: {label: 'Justified', badgeClass: 'bg-main-50 text-main-600'},
            rejected: {label: 'Rejected', badgeClass: 'bg-danger-50 text-danger-600'}
        };
        var attendanceStateOrder = ['present', 'absent', 'requested', 'justified', 'rejected'];
        var setText = function (selector, value) {
            var element = document.querySelector(selector);
            if (element) {
                element.textContent = value;
            }
        };
        var plural = function (count, singular, pluralText) {
            return count + ' ' + (count === 1 ? singular : pluralText);
        };
        var uniqueAttendanceBadges = function (selector) {
            var seen = {};
            return Array.prototype.slice.call(document.querySelectorAll(selector || '[data-attendance-activity-badge]'))
                    .filter(function (badge) {
                        var key = badge.getAttribute('data-attendance-activity-badge');
                        if (!key || seen[key]) {
                            return false;
                        }
                        seen[key] = true;
                        return true;
                    });
        };
        var refreshAttendanceTotals = function () {
            var badges = uniqueAttendanceBadges();
            var absenceCount = badges.filter(function (badge) {
                return ['absent', 'requested', 'rejected'].indexOf(badge.getAttribute('data-state-value')) >= 0;
            }).length;
            var pendingCount = badges.filter(function (badge) {
                return badge.getAttribute('data-state-value') === 'requested';
            }).length;
            setText('[data-attendance-total-summary]', plural(badges.length, 'record', 'records'));
            setText('[data-attendance-absence-summary]', plural(absenceCount, 'absence', 'absences'));
            setText('[data-attendance-pending-summary]', pendingCount + ' pending justifications');
        };
        var refreshAttendanceGroupSummary = function (studentId) {
            var summaries = Array.prototype.slice.call(document.querySelectorAll('[data-attendance-group-summary="' + studentId + '"]'));
            if (!summaries.length) {
                return;
            }
            var counts = {};
            uniqueAttendanceBadges('[data-attendance-activity-badge][data-student-id="' + studentId + '"]')
                    .forEach(function (badge) {
                        var state = badge.getAttribute('data-state-value');
                        counts[state] = (counts[state] || 0) + 1;
                    });
            summaries.forEach(function (summary) {
                summary.innerHTML = '';
                attendanceStateOrder.forEach(function (state) {
                    if (!counts[state]) {
                        return;
                    }
                    var meta = attendanceStateMeta[state];
                    var item = document.createElement('span');
                    item.className = meta.badgeClass + ' aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12';
                    item.textContent = counts[state] + ' ' + meta.label;
                    summary.appendChild(item);
                });
            });
        };
        var updateAttendanceDecisionButtons = function (modal, data) {
            var approve = modal.querySelector('[data-attendance-decision="approve"]');
            var reject = modal.querySelector('[data-attendance-decision="reject"]');
            if (approve) {
                approve.hidden = !data.canApprove;
            }
            if (reject) {
                reject.hidden = !data.canReject;
            }
        };
        var applyAttendanceDecision = function (data) {
            var activityId = String(data.activityId);
            Array.prototype.slice.call(document.querySelectorAll('[data-attendance-activity-badge="' + activityId + '"]'))
                    .forEach(function (badge) {
                        badge.className = data.badgeClass + ' px-12 py-6 border-neutral-30 border rounded-pill text-12';
                        badge.textContent = data.stateLabel;
                        badge.setAttribute('data-state-value', data.stateValue);
                        refreshAttendanceGroupSummary(badge.getAttribute('data-student-id'));
                    });
            Array.prototype.slice.call(document.querySelectorAll('[data-attendance-detail-state="' + activityId + '"]'))
                    .forEach(function (stateBadge) {
                        stateBadge.className = data.badgeClass + ' px-12 py-7 border-neutral-30 border rounded-pill text-12';
                        stateBadge.textContent = data.stateLabel;
                        stateBadge.setAttribute('data-state-value', data.stateValue);
                    });
            var modal = document.querySelector('[data-attendance-settings-modal][data-activity-id="' + activityId + '"]');
            if (modal) {
                var stateBadge = modal.querySelector('[data-attendance-settings-state]');
                if (stateBadge) {
                    stateBadge.className = data.badgeClass + ' px-12 py-7 border-neutral-30 border rounded-pill text-12';
                    stateBadge.textContent = data.stateLabel;
                    stateBadge.setAttribute('data-state-value', data.stateValue);
                }
                updateAttendanceDecisionButtons(modal, data);
            }
            refreshAttendanceTotals();
        };
        var closeAttendanceModal = function (modalElement) {
            if (!modalElement) {
                return;
            }
            if (window.bootstrap) {
                var modal = window.bootstrap.Modal.getInstance(modalElement)
                        || new window.bootstrap.Modal(modalElement);
                modal.hide();
            }
            window.setTimeout(function () {
                modalElement.classList.remove('show');
                modalElement.setAttribute('aria-hidden', 'true');
                modalElement.removeAttribute('aria-modal');
                modalElement.style.display = 'none';
                document.body.classList.remove('modal-open');
                document.body.style.removeProperty('overflow');
                document.body.style.removeProperty('padding-right');
                Array.prototype.slice.call(document.querySelectorAll('.modal-backdrop'))
                        .forEach(function (backdrop) {
                            backdrop.remove();
                        });
            }, 150);
        };
        window.handleAttendanceDecision = function (button) {
            if (!button || button.hidden) {
                return false;
            }
            if (button.getAttribute('data-attendance-busy') === 'true') {
                return false;
            }
            button.setAttribute('data-attendance-busy', 'true');
            var form = button.closest('[data-attendance-settings-form]');
            if (!form) {
                button.removeAttribute('data-attendance-busy');
                return false;
            }
            var message = form.querySelector('[data-attendance-settings-message]');
            var actionUrl = button.getAttribute('data-action-url');
            var buttons = Array.prototype.slice.call(form.querySelectorAll('[data-attendance-decision]'));
            buttons.forEach(function (item) {
                item.disabled = true;
            });
            if (message) {
                message.textContent = 'Saving...';
            }
            fetch(actionUrl, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams(new FormData(form))
            })
                    .then(function (response) {
                        return response.json().then(function (payload) {
                            if (!response.ok || !payload.success) {
                                throw new Error(payload.message || 'Attendance settings could not be saved.');
                            }
                            return payload;
                        });
                    })
                    .then(function (payload) {
                        applyAttendanceDecision(payload);
                        if (message) {
                            message.textContent = '';
                        }
                        closeAttendanceModal(button.closest('.modal'));
                    })
                    .catch(function (error) {
                        if (message) {
                            message.textContent = error.message;
                        }
                    })
                    .finally(function () {
                        button.removeAttribute('data-attendance-busy');
                        buttons.forEach(function (item) {
                            item.disabled = false;
                        });
                    });
            return false;
        };
        document.addEventListener('click', function (event) {
            var button = event.target.closest('[data-attendance-decision]');
            if (!button) {
                return;
            }
            event.preventDefault();
            window.handleAttendanceDecision(button);
        });
    }());

    (function () {
        var status = document.getElementById('attendance-status');
        var checkIn = document.getElementById('attendance-check-in');
        var checkOut = document.getElementById('attendance-check-out');
        if (!status || !checkIn || !checkOut) {
            return;
        }
        var syncTimestamps = function () {
            var absent = status.value === 'absent';
            checkIn.disabled = absent;
            checkOut.disabled = absent;
            if (absent) {
                checkIn.value = '';
                checkOut.value = '';
                return;
            }
            if (!checkIn.value) {
                checkIn.value = checkIn.getAttribute('data-default-value') || '';
            }
        };
        status.addEventListener('change', syncTimestamps);
        syncTimestamps();
    }());
</script>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-deferred-management-list.js?v=20260715-deferred-management-1"></script>
</body>
</html>
