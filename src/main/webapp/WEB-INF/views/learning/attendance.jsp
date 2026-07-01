<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Enrollments &amp; Certificates</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .aac-page {
            --aac-primary: #2563eb;
            --aac-border: #e6edf0;
            --aac-muted: #64748b;
            --aac-ink: #172033;
            min-width: 0;
        }

        .aac-page .ad-mode-grid {
            display: grid;
            align-items: stretch;
            gap: 14px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            max-width: 100%;
            min-width: 0;
        }

        .aac-page .ad-mode-card {
            background: #fff;
            border: 1px solid var(--aac-border);
            border-radius: 8px;
            color: var(--aac-ink);
            cursor: pointer;
            min-width: 0;
            padding: 18px;
            text-align: left;
            transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease, background .2s ease;
            width: 100%;
        }

        .aac-page .ad-mode-card > .d-flex {
            align-items: stretch !important;
            height: 100%;
            max-width: 100%;
            min-width: 0;
        }

        .aac-page .ad-mode-card.is-active {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(20, 184, 166, 0.05)), #f7fbff;
            border-color: rgba(37, 99, 235, 0.7);
            box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12);
            transform: translateY(-1px);
        }

        .aac-page .ad-mode-card.is-active .ad-mode-icon {
            background: var(--aac-primary) !important;
            color: #fff !important;
        }

        .aac-page .ad-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex: 0 0 44px;
            height: 44px;
            justify-content: center;
            width: 44px;
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

        .aac-surface {
            background: #fff;
            border: 1px solid var(--aac-border);
            border-radius: 8px;
            box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
            min-width: 0;
        }

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
            border-color: #d5e3ef;
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
            transform: translateY(-1px);
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
            min-width: 0;
            padding: 0;
            width: 100%;
        }

        .aac-grade-doc__table-wrap {
            border: 1px solid #000;
            max-width: 100%;
            overflow-x: auto;
            overflow-y: hidden;
            -webkit-overflow-scrolling: touch;
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
            margin: -1px;
            min-width: var(--aac-grade-doc-min-width, 604px);
            table-layout: fixed;
            width: 100%;
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
            color: #111827;
            display: grid;
            font-family: Arial, sans-serif;
            gap: 26px;
            grid-template-columns: 44px minmax(0, 1fr);
            margin: 0 auto;
            max-width: 920px;
            padding: 30px 34px;
        }

        .aac-certificate-doc__rail {
            align-self: stretch;
            background: transparent;
            display: grid;
            gap: 28px;
            grid-template-rows: 150px minmax(300px, 1fr) 132px 34px;
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
            font-size: 32px;
            font-weight: 800;
            line-height: 1.16;
            margin-bottom: 34px;
            overflow-wrap: anywhere;
        }

        .aac-certificate-doc__identity {
            display: grid;
            gap: 26px 34px;
            grid-template-columns: 1.45fr .8fr .85fr;
            margin-bottom: 32px;
        }

        .aac-certificate-doc__identity div {
            min-width: 0;
        }

        .aac-certificate-doc__identity span,
        .aac-certificate-doc__identity small {
            display: block;
            font-size: 14px;
            line-height: 1.35;
            margin-bottom: 8px;
        }

        .aac-certificate-doc__identity strong {
            display: block;
            font-size: 16px;
            line-height: 1.35;
            overflow-wrap: anywhere;
        }

        .aac-certificate-doc__identity div:nth-child(4) {
            grid-column: 1 / -1;
        }

        .aac-certificate-doc__table {
            border-collapse: collapse;
            margin-bottom: 26px;
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
            font-size: 15px;
            line-height: 1.35;
            padding: 18px 16px;
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
            font-size: 16px;
            line-height: 1.65;
            margin: 0 0 28px;
        }

        .aac-certificate-doc__date {
            font-size: 15px;
            margin-bottom: 28px;
        }

        .aac-certificate-doc__signatures {
            display: grid;
            gap: 36px;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            margin-top: 0;
        }

        .aac-certificate-doc__line {
            border-top: 1px solid #111827;
            color: #111827;
            font-size: 12px;
            margin-top: 34px;
            padding-top: 8px;
        }

        .aac-certificate-doc__legal {
            align-items: center;
            color: #006070;
            display: grid;
            font-size: 14px;
            font-weight: 700;
            gap: 6px 10px;
            grid-template-columns: auto 1fr;
            margin-top: 32px;
        }

        .aac-certificate-doc__legal span:last-child {
            grid-column: 1 / -1;
        }

        .aac-certificate-doc__page {
            font-size: 13px;
            margin-top: 18px;
            text-align: right;
        }

        @media (max-width: 1199.98px) {
            .aac-page .ad-mode-grid {
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
            .aac-page .ad-mode-grid {
                grid-template-columns: minmax(0, 1fr);
            }

            .aac-page .ad-mode-card {
                padding: 16px;
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

                <div class="aac-page" data-aac-tabs>
                    <div class="ad-mode-grid mb-20" role="tablist" aria-label="Enrollment, grade, certificate and attendance sections">
                        <button type="button" class="ad-mode-card is-active" data-aac-tab="enrollments" role="tab" aria-selected="true" aria-controls="enrollments">
                            <span class="d-flex align-items-start gap-14">
                                <span class="ad-mode-icon bg-main-50 text-main-600 text-22"><i class="ph ph-student"></i></span>
                                <span class="min-w-0 ad-mode-content">
                                    <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Enrollments</span>
                                    <span class="d-block text-13 text-main-600 fw-semibold">${enrollmentCount} enrollments | ${activeEnrollmentCount} active</span>
                                </span>
                            </span>
                        </button>
                        <button type="button" class="ad-mode-card" data-aac-tab="grades" role="tab" aria-selected="false" aria-controls="grades">
                            <span class="d-flex align-items-start gap-14">
                                <span class="ad-mode-icon bg-warning-50 text-warning-600 text-22"><i class="ph ph-seal-check"></i></span>
                                <span class="min-w-0 ad-mode-content">
                                    <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Grades</span>
                                    <span class="d-block text-13 text-main-600 fw-semibold">${gradeRecordCount} grades | ${gradeSheetCount} sheets</span>
                                </span>
                            </span>
                        </button>
                        <button type="button" class="ad-mode-card" data-aac-tab="certificates" role="tab" aria-selected="false" aria-controls="certificates">
                            <span class="d-flex align-items-start gap-14">
                                <span class="ad-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-certificate"></i></span>
                                <span class="min-w-0 ad-mode-content">
                                    <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Certificates</span>
                                    <span class="d-block text-13 text-main-600 fw-semibold">${certificateCount} certificates | ${publishedGradeSheetCount} published sheets</span>
                                </span>
                            </span>
                        </button>
                        <button type="button" class="ad-mode-card" data-aac-tab="attendance" role="tab" aria-selected="false" aria-controls="attendance">
                            <span class="d-flex align-items-start gap-14">
                                <span class="ad-mode-icon bg-success-50 text-success-600 text-22"><i class="ph ph-user-check"></i></span>
                                <span class="min-w-0 ad-mode-content">
                                    <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Attendance</span>
                                    <span class="d-block text-13 text-main-600 fw-semibold">${attendanceCount} records | ${absenceCount} absences</span>
                                </span>
                            </span>
                        </button>
                    </div>

                <section id="enrollments" class="ad-tab-panel" data-aac-panel="enrollments" role="tabpanel">
                <div class="aac-surface px-24 py-24 mb-24">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Enrollments</h2>
                            <div class="gape-management-summary" aria-label="Enrollment summary">
                                <span>${enrollmentCount} enrollments</span>
                                <span>${activeEnrollmentCount} active</span>
                                <span>${pendingEnrollmentCount} pending</span>
                            </div>
                        </div>
                        <span class="gape-management-card__icon bg-main-50 text-main-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                            <i class="ph ph-student"></i>
                        </span>
                    </div>
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
                            <tbody>
                            <c:forEach var="group" items="${enrollmentGroups}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="fw-medium text-neutral-700"><c:out value="${group.studentDisplayLabel}"/></span>
                                        <span class="d-block text-12">${group.enrollmentCount} enrollments</span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${group.studentEmail}"/>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">${group.courseEnrollmentCount}</td>
                                    <td class="py-20 px-20">
                                        <div class="aac-state-summary">
                                            <c:forEach var="summary" items="${group.courseStateSummaries}">
                                                <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                            </c:forEach>
                                            <c:if test="${empty group.courseStateSummaries}">
                                                <span class="aac-state-empty">No enrollments</span>
                                            </c:if>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show all enrollments" data-bs-toggle="collapse" data-bs-target="#enrollmentGroup${group.studentUserId}" aria-expanded="false">
                                            <i class="ph ph-caret-down"></i>
                                        </button>
                                    </td>
                                </tr>
                                <tr class="collapse" id="enrollmentGroup${group.studentUserId}">
                                    <td colspan="5" class="aac-expanded-cell">
                                        <div class="aac-expanded-panel">
                                            <div class="aac-expanded-panel__header">
                                                <div>
                                                    <h3 class="text-16 fw-semibold text-neutral-800 mb-4">Enrollments</h3>
                                                    <span class="text-13 text-neutral-600"><c:out value="${group.studentDisplayLabel}"/> | ${group.courseEnrollmentCount} enrollments | ${group.enrollmentCount} total</span>
                                                </div>
                                            </div>
                                            <div class="aac-tree">
                                                <c:forEach var="courseGroup" items="${group.courseGroups}">
                                                    <c:set var="item" value="${courseGroup.enrollment}"/>
                                                    <c:set var="enrollmentModalId" value="enrollment${item.modalKey}"/>
                                                    <article class="aac-tree-node aac-tree-node--course">
                                                        <div class="aac-tree-row">
                                                            <div class="aac-tree-main">
                                                                <span class="aac-tree-icon bg-main-50 text-main-600"><i class="ph ph-graduation-cap"></i></span>
                                                                <div class="min-w-0">
                                                                    <span class="aac-tree-title"><c:out value="${item.contextLabel}"/></span>
                                                                    <span class="aac-tree-meta"><c:out value="${item.periodLabel}"/></span>
                                                                </div>
                                                            </div>
                                                            <div class="aac-tree-count">
                                                                <span class="aac-tree-count-value">${courseGroup.subjectEnrollmentCount}</span>
                                                            </div>
                                                            <div class="aac-tree-status">
                                                                <div class="aac-state-summary">
                                                                    <c:forEach var="summary" items="${courseGroup.subjectStateSummaries}">
                                                                        <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                                                    </c:forEach>
                                                                    <c:if test="${empty courseGroup.subjectStateSummaries}">
                                                                        <span class="aac-state-empty">No enrollments</span>
                                                                    </c:if>
                                                                </div>
                                                            </div>
                                                            <div class="aac-tree-actions">
                                                                <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show subject enrollments" data-bs-toggle="collapse" data-bs-target="#courseSubjects${item.modalKey}" aria-expanded="false"><i class="ph ph-caret-down"></i></button>
                                                                <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                <button type="button" class="aac-icon-button bg-info-50 text-info-600" title="Edit" aria-label="Edit enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Edit"><i class="ph ph-pencil-simple-line"></i></button>
                                                                <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                            </div>
                                                        </div>
                                                        <div class="collapse aac-tree-children" id="courseSubjects${item.modalKey}">
                                                            <c:forEach var="subjectGroup" items="${courseGroup.subjectGroups}">
                                                                <c:set var="item" value="${subjectGroup.enrollment}"/>
                                                                <c:set var="enrollmentModalId" value="enrollment${item.modalKey}"/>
                                                                <article class="aac-tree-node aac-tree-node--subject">
                                                                    <div class="aac-tree-row">
                                                                        <div class="aac-tree-main">
                                                                            <span class="aac-tree-icon bg-success-50 text-success-600"><i class="ph ph-book-open"></i></span>
                                                                            <div class="min-w-0">
                                                                                <span class="aac-tree-title"><c:out value="${item.contextLabel}"/></span>
                                                                                <span class="aac-tree-meta"><c:out value="${item.periodLabel}"/></span>
                                                                            </div>
                                                                        </div>
                                                                        <div class="aac-tree-count">
                                                                            <span class="aac-tree-count-value">${subjectGroup.classGroupEnrollmentCount}</span>
                                                                        </div>
                                                                        <div class="aac-tree-status">
                                                                            <div class="aac-state-summary">
                                                                                <c:forEach var="summary" items="${subjectGroup.classGroupStateSummaries}">
                                                                                    <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                                                                </c:forEach>
                                                                                <c:if test="${empty subjectGroup.classGroupStateSummaries}">
                                                                                    <span class="aac-state-empty">No enrollments</span>
                                                                                </c:if>
                                                                            </div>
                                                                        </div>
                                                                        <div class="aac-tree-actions">
                                                                            <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show class group enrollments" data-bs-toggle="collapse" data-bs-target="#subjectClasses${item.modalKey}" aria-expanded="false"><i class="ph ph-caret-down"></i></button>
                                                                            <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                            <button type="button" class="aac-icon-button bg-info-50 text-info-600" title="Edit" aria-label="Edit enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Edit"><i class="ph ph-pencil-simple-line"></i></button>
                                                                            <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                                        </div>
                                                                    </div>
                                                                    <div class="collapse aac-tree-children" id="subjectClasses${item.modalKey}">
                                                                        <c:forEach var="classGroup" items="${subjectGroup.classGroups}">
                                                                            <c:set var="item" value="${classGroup.enrollment}"/>
                                                                            <c:set var="enrollmentModalId" value="enrollment${item.modalKey}"/>
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
                                                                                                <span class="aac-state-empty">No enrollments</span>
                                                                                            </c:if>
                                                                                        </div>
                                                                                    </div>
                                                                                    <div class="aac-tree-actions">
                                                                                        <button type="button" class="aac-icon-button bg-main-50 text-main-600 collapsed" title="Show all" aria-label="Show assessment enrollments" data-bs-toggle="collapse" data-bs-target="#classAssessments${item.modalKey}" aria-expanded="false"><i class="ph ph-caret-down"></i></button>
                                                                                        <button type="button" class="aac-icon-button bg-neutral-20 text-neutral-600" title="Detail" aria-label="View enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Detail"><i class="ph ph-eye"></i></button>
                                                                                        <button type="button" class="aac-icon-button bg-danger-50 text-danger-600" title="Delete" aria-label="Delete enrollment" data-bs-toggle="modal" data-bs-target="#${enrollmentModalId}Delete"><i class="ph ph-trash"></i></button>
                                                                                    </div>
                                                                                </div>
                                                                                <div class="collapse aac-tree-children" id="classAssessments${item.modalKey}">
                                                                                    <c:forEach var="item" items="${classGroup.assessmentEnrollments}">
                                                                                        <c:set var="enrollmentModalId" value="enrollment${item.modalKey}"/>
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
                                                                        <c:if test="${empty subjectGroup.classGroups}">
                                                                            <div class="aac-tree-empty">No class group enrollments for this subject.</div>
                                                                        </c:if>
                                                                    </div>
                                                                </article>
                                                            </c:forEach>
                                                            <c:if test="${empty courseGroup.subjectGroups}">
                                                                <div class="aac-tree-empty">No subject enrollments for this course.</div>
                                                            </c:if>
                                                        </div>
                                                    </article>
                                                </c:forEach>
                                                <c:if test="${empty group.courseGroups}">
                                                    <div class="aac-tree-empty">No course enrollments for this student.</div>
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
                    <div class="gape-mobile-list">
                        <c:forEach var="group" items="${enrollmentGroups}">
                            <article class="gape-mobile-row">
                                <div class="gape-mobile-row__header">
                                    <div>
                                        <div class="gape-mobile-title"><c:out value="${group.studentDisplayLabel}"/></div>
                                        <div class="gape-mobile-subtitle"><c:out value="${group.studentEmail}"/></div>
                                    </div>
                                    <span class="gape-mobile-badge px-12 py-7 border-neutral-30 border rounded-pill text-12">${group.courseEnrollmentCount} enrollments</span>
                                </div>
                                <div class="gape-mobile-kv">
                                    <strong>Email</strong>
                                    <span><c:out value="${group.studentEmail}"/></span>
                                    <strong>Enrollments</strong>
                                    <span>${group.courseEnrollmentCount}</span>
                                    <strong>State</strong>
                                    <span>
                                        <span class="aac-state-summary">
                                            <c:forEach var="summary" items="${group.courseStateSummaries}">
                                                <span class="${summary.badgeClass} aac-state-summary__badge px-12 py-6 border-neutral-30 border rounded-pill text-12">${summary.count} <c:out value="${summary.label}"/></span>
                                            </c:forEach>
                                            <c:if test="${empty group.courseStateSummaries}">
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
                            <c:set var="enrollmentModalId" value="enrollment${item.modalKey}"/>
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
                                                <div class="col-md-6"><span class="text-12 text-neutral-500 d-block mb-6">Period</span><strong class="text-14 text-neutral-800"><c:out value="${item.periodLabel}"/></strong></div>
                                                <div class="col-12"><span class="text-12 text-neutral-500 d-block mb-6">Details</span><span class="text-14 text-neutral-700"><c:out value="${item.contextDetail}"/></span></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="modal fade" id="${enrollmentModalId}Edit" tabindex="-1" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content rounded-8 border-0">
                                        <div class="modal-header border-neutral-30">
                                            <h5 class="modal-title text-18 fw-semibold">Edit enrollment</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}${item.updateAction}" method="post">
                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                            <input type="hidden" name="returnTo" value="${pageContext.request.contextPath}/learning/attendance#enrollments">
                                            <div class="modal-body">
                                                <div class="row gy-3">
                                                    <div class="col-md-4">
                                                        <label class="text-13 text-neutral-600 mb-6 d-block">State</label>
                                                        <select name="state" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                                            <option value="active" ${item.stateValue == 'active' ? 'selected' : ''}>Active</option>
                                                            <option value="inactive" ${item.stateValue == 'inactive' ? 'selected' : ''}>Inactive</option>
                                                            <option value="completed" ${item.stateValue == 'completed' ? 'selected' : ''}>Completed</option>
                                                            <option value="withdrawn" ${item.stateValue == 'withdrawn' ? 'selected' : ''}>Withdrawn</option>
                                                            <option value="pending" ${item.stateValue == 'pending' ? 'selected' : ''}>Pending</option>
                                                            <option value="rejected" ${item.stateValue == 'rejected' ? 'selected' : ''}>Rejected</option>
                                                        </select>
                                                    </div>
                                                    <div class="col-md-4">
                                                        <label class="text-13 text-neutral-600 mb-6 d-block">Start</label>
                                                        <input type="date" name="startDate" value="<c:out value='${item.startDateValue}'/>" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                                    </div>
                                                    <div class="col-md-4">
                                                        <label class="text-13 text-neutral-600 mb-6 d-block">End</label>
                                                        <input type="date" name="endDate" value="<c:out value='${item.endDateValue}'/>" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="modal-footer border-neutral-30">
                                                <button type="button" class="border border-neutral-30 text-neutral-600 bg-white px-20 py-10 rounded-8 fw-semibold" data-bs-dismiss="modal">Cancel</button>
                                                <button type="submit" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white border-0">Save</button>
                                            </div>
                                        </form>
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

                </section>

                <section id="grades" class="ad-tab-panel" data-aac-panel="grades" role="tabpanel" hidden>
                <div class="aac-surface px-24 py-24 mb-24">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Grades</h2>
                            <div class="gape-management-summary" aria-label="Grade summary">
                                <span>${gradeRecordCount} grades</span>
                                <span>${gradeSheetCount} grade sheets</span>
                            </div>
                        </div>
                        <span class="gape-management-card__icon bg-warning-50 text-warning-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                            <i class="ph ph-seal-check"></i>
                        </span>
                    </div>
                    <%@ include file="/WEB-INF/fragments/learning-grades-certificates-content.jspf" %>
                </div>

                </section>

                <section id="certificates" class="ad-tab-panel" data-aac-panel="certificates" role="tabpanel" hidden>
                <div class="aac-surface px-24 py-24 mb-24">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Certificates</h2>
                            <div class="gape-management-summary" aria-label="Certificate summary">
                                <span>${certificateCount} certificates</span>
                                <span>${publishedGradeSheetCount} published sheets</span>
                            </div>
                        </div>
                        <span class="gape-management-card__icon bg-info-50 text-info-600 w-44 h-44 rounded-8 d-inline-flex align-items-center justify-content-center text-22">
                            <i class="ph ph-certificate"></i>
                        </span>
                    </div>
                    <%@ include file="/WEB-INF/fragments/learning-certificates-content.jspf" %>
                </div>
                </section>

                <section id="attendance" class="ad-tab-panel" data-aac-panel="attendance" role="tabpanel" hidden>
                <div class="aac-surface px-24 py-24 mb-24">
                    <div class="gape-management-card__header d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Attendance</h2>
                            <div class="gape-management-summary" aria-label="Attendance summary">
                                <span>${attendanceCount} records</span>
                                <span>${absenceCount} absences</span>
                                <span>${pendingJustificationCount} pending justifications</span>
                            </div>
                        </div>
                        <form action="${pageContext.request.contextPath}/learning/attendance" method="get" class="d-flex align-items-center gap-10 flex-wrap mb-0">
                            <select name="status" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 190px; min-height: 44px;" aria-label="Attendance status filter" onchange="this.form.submit()">
                                <option value="">All statuses</option>
                                <c:forEach var="option" items="${attendanceStatusOptions}">
                                    <option value="${option.value}" ${option.selected ? 'selected' : ''}>
                                        <c:out value="${option.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <select name="justificationState" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 220px; min-height: 44px;" aria-label="Justification state filter" onchange="this.form.submit()">
                                <option value="">All justifications</option>
                                <c:forEach var="option" items="${justificationStateOptions}">
                                    <option value="${option.value}" ${option.selected ? 'selected' : ''}>
                                        <c:out value="${option.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </form>
                    </div>

                    <div class="mb-24 pb-24 border-bottom border-neutral-30">
                        <h3 class="text-16 fw-semibold text-neutral-700 mb-14">Register attendance</h3>
                        <form action="${pageContext.request.contextPath}/learning/attendance" method="post" class="row gy-3 align-items-end">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-lesson-id">Lesson ID</label>
                                <input type="number" id="attendance-lesson-id" name="lessonId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-student-id">Student ID</label>
                                <input type="number" id="attendance-student-id" name="studentUserId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-status">Status</label>
                                <select id="attendance-status" name="status" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                    <c:forEach var="option" items="${attendanceCreateStatusOptions}">
                                        <option value="${option.value}"><c:out value="${option.label}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-source">Source</label>
                                <select id="attendance-source" name="source" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <c:forEach var="option" items="${attendanceCreateSourceOptions}">
                                        <option value="${option.value}"><c:out value="${option.label}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-check-in">Check-in</label>
                                <input type="datetime-local" id="attendance-check-in" name="checkIn" value="${defaultCheckIn}" data-default-value="${defaultCheckIn}" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                            <div class="col-xl-2 col-lg-3 col-md-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-check-out">Check-out</label>
                                <input type="datetime-local" id="attendance-check-out" name="checkOut" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                            <div class="col-xl-4 col-lg-6">
                                <label class="text-13 text-neutral-600 mb-6 d-block" for="attendance-notes">Notes</label>
                                <input type="text" id="attendance-notes" name="notes" maxlength="500" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                            <div class="col-xl-2 col-lg-3">
                                <button type="submit" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center" style="min-height: 44px;">
                                    <i class="ph ph-check-circle me-8"></i>Save
                                </button>
                            </div>
                        </form>
                    </div>

                    <div class="gape-desktop-table">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Lesson</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Student</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Status</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Time</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Permanence</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Source</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="record" items="${attendanceRecords}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20 text-14 text-neutral-700">
                                        <span class="fw-medium"><c:out value="${record.lessonTitle}"/></span>
                                        <span class="d-block text-12 text-neutral-500">#<c:out value="${record.lessonId}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="fw-medium text-neutral-700"><c:out value="${record.studentName}"/></span>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${record.studentEmail}"/></span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="${record.statusBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${record.statusLabel}"/>
                                        </span>
                                        <span class="${record.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12 ms-6">
                                            <c:out value="${record.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="d-block">In: <c:out value="${record.checkIn}"/></span>
                                        <span class="d-block text-12">Out: <c:out value="${record.checkOut}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${record.permanenceLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${record.sourceLabel}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty attendanceRecords}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No attendance records available.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <div class="gape-mobile-list">
                        <c:forEach var="record" items="${attendanceRecords}">
                            <article class="gape-mobile-row">
                                <div class="gape-mobile-row__header">
                                    <div>
                                        <div class="gape-mobile-title"><c:out value="${record.lessonTitle}"/></div>
                                        <div class="gape-mobile-subtitle">#<c:out value="${record.lessonId}"/> | <c:out value="${record.studentName}"/></div>
                                    </div>
                                    <span class="${record.statusBadgeClass} gape-mobile-badge px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                        <c:out value="${record.statusLabel}"/>
                                    </span>
                                </div>
                                <div class="gape-mobile-kv">
                                    <strong>Student</strong>
                                    <span><c:out value="${record.studentEmail}"/></span>
                                    <strong>State</strong>
                                    <span><c:out value="${record.stateLabel}"/></span>
                                    <strong>Time</strong>
                                    <span>In: <c:out value="${record.checkIn}"/> | Out: <c:out value="${record.checkOut}"/></span>
                                    <strong>Duration</strong>
                                    <span><c:out value="${record.permanenceLabel}"/></span>
                                    <strong>Source</strong>
                                    <span><c:out value="${record.sourceLabel}"/></span>
                                </div>
                            </article>
                        </c:forEach>
                        <c:if test="${empty attendanceRecords}">
                            <div class="gape-mobile-empty">No attendance records available.</div>
                        </c:if>
                    </div>

                    <div class="mt-28 pt-24 border-top border-neutral-30">
                        <div class="mb-20">
                            <h3 class="text-16 fw-semibold text-neutral-700 mb-4">Justifications</h3>
                            <span class="text-14 text-neutral-500">Submitted absence, late and partial-presence justifications.</span>
                        </div>
                        <div class="gape-desktop-table">
                            <table class="table mb-0">
                                <thead>
                                <tr>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Student</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Lesson</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Reason</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Submitted</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Process</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="justification" items="${justifications}">
                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-20 px-20 text-14 text-neutral-500">
                                            <span class="fw-medium text-neutral-700"><c:out value="${justification.studentName}"/></span>
                                            <span class="d-block text-12"><c:out value="${justification.studentEmail}"/></span>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500">
                                            <span class="fw-medium text-neutral-700"><c:out value="${justification.lessonTitle}"/></span>
                                            <span class="d-block text-12">Record #<c:out value="${justification.attendanceRecordId}"/></span>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500">
                                            <c:out value="${justification.reason}"/>
                                            <span class="d-block text-12">Attachment: <c:out value="${justification.attachmentLabel}"/></span>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${justification.submittedAt}"/></td>
                                        <td class="py-20 px-20">
                                            <span class="${justification.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                <c:out value="${justification.stateLabel}"/>
                                            </span>
                                        </td>
                                        <td class="py-20 px-20 text-end">
                                            <c:choose>
                                                <c:when test="${justification.submitted}">
                                                    <div class="d-flex justify-content-end gap-8 flex-wrap">
                                                        <form action="${pageContext.request.contextPath}/learning/attendance/justifications/${justification.id}/approve" method="post" class="d-flex align-items-center gap-8 mb-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <input type="text" name="decisionNotes" maxlength="500" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Notes">
                                                            <button type="submit" class="border-0 bg-success-50 text-success-600 w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center" title="Approve" aria-label="Approve justification">
                                                                <i class="ph ph-check"></i>
                                                            </button>
                                                        </form>
                                                        <form action="${pageContext.request.contextPath}/learning/attendance/justifications/${justification.id}/reject" method="post" class="d-flex align-items-center gap-8 mb-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <input type="text" name="decisionNotes" maxlength="500" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Notes">
                                                            <button type="submit" class="border-0 bg-danger-50 text-danger-600 w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center" title="Reject" aria-label="Reject justification">
                                                                <i class="ph ph-x"></i>
                                                            </button>
                                                        </form>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-13 text-neutral-500">Processed <c:out value="${justification.processedAt}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty justifications}">
                                    <tr>
                                        <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No justifications available.</td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                        <div class="gape-mobile-list">
                            <c:forEach var="justification" items="${justifications}">
                                <article class="gape-mobile-row">
                                    <div class="gape-mobile-row__header">
                                        <div>
                                            <div class="gape-mobile-title"><c:out value="${justification.studentName}"/></div>
                                            <div class="gape-mobile-subtitle"><c:out value="${justification.studentEmail}"/></div>
                                        </div>
                                        <span class="${justification.stateBadgeClass} gape-mobile-badge px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                            <c:out value="${justification.stateLabel}"/>
                                        </span>
                                    </div>
                                    <div class="gape-mobile-kv">
                                        <strong>Lesson</strong>
                                        <span><c:out value="${justification.lessonTitle}"/> | Record #<c:out value="${justification.attendanceRecordId}"/></span>
                                        <strong>Reason</strong>
                                        <span><c:out value="${justification.reason}"/></span>
                                        <strong>Attachment</strong>
                                        <span><c:out value="${justification.attachmentLabel}"/></span>
                                        <strong>Submitted</strong>
                                        <span><c:out value="${justification.submittedAt}"/></span>
                                    </div>
                                    <c:choose>
                                        <c:when test="${justification.submitted}">
                                            <div class="gape-mobile-actions">
                                                <form action="${pageContext.request.contextPath}/learning/attendance/justifications/${justification.id}/approve" method="post" class="d-flex align-items-center gap-8 mb-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <input type="text" name="decisionNotes" maxlength="500" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Notes">
                                                    <button type="submit" class="border-0 bg-success-50 text-success-600 w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center flex-shrink-0" title="Approve" aria-label="Approve justification">
                                                        <i class="ph ph-check"></i>
                                                    </button>
                                                </form>
                                                <form action="${pageContext.request.contextPath}/learning/attendance/justifications/${justification.id}/reject" method="post" class="d-flex align-items-center gap-8 mb-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <input type="text" name="decisionNotes" maxlength="500" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Notes">
                                                    <button type="submit" class="border-0 bg-danger-50 text-danger-600 w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center flex-shrink-0" title="Reject" aria-label="Reject justification">
                                                        <i class="ph ph-x"></i>
                                                    </button>
                                                </form>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="gape-mobile-processed">Processed <c:out value="${justification.processedAt}"/></div>
                                        </c:otherwise>
                                    </c:choose>
                                </article>
                            </c:forEach>
                            <c:if test="${empty justifications}">
                                <div class="gape-mobile-empty">No justifications available.</div>
                            </c:if>
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
        var aliases = {
            'grade-sheets': 'grades',
            'grade-records': 'grades',
            'certificates-list': 'certificates',
            justifications: 'attendance'
        };

        function panelForHash() {
            var hash = (window.location.hash || '').replace(/^#/, '');
            return aliases[hash] || hash || 'enrollments';
        }

        function activate(panelName, updateHash) {
            var activePanel = panels.find(function (panel) {
                return panel.getAttribute('data-aac-panel') === panelName;
            });
            if (!activePanel) {
                panelName = 'enrollments';
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

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                activate(tab.getAttribute('data-aac-tab'), true);
            });
        });

        activate(panelForHash(), false);
        window.addEventListener('hashchange', function () {
            activate(panelForHash(), false);
        });

        var rawHash = (window.location.hash || '').replace(/^#/, '');
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
</body>
</html>
