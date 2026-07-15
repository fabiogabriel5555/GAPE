<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - ${calendarMode ? 'Events' : 'Lessons & Assessments'}</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-class-group-picker {
            position: relative;
        }

        .gape-class-group-picker__summary {
            min-height: 44px;
            cursor: pointer;
            list-style: none;
        }

        .gape-class-group-picker__summary::-webkit-details-marker {
            display: none;
        }

        .gape-class-group-picker__panel {
            position: absolute;
            inset-block-start: calc(100% + 6px);
            inset-inline-start: 0;
            z-index: 30;
            width: min(420px, 100vw - 48px);
            max-height: 220px;
            box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
        }

        .gape-notifications-table {
            min-width: 840px;
        }

        .gape-notifications-table th:last-child,
        .gape-notifications-table td:last-child {
            min-width: 78px;
            white-space: nowrap;
        }

        .gape-learning-actions {
            min-width: 0;
        }

        .gape-learning-table-scroll {
            min-width: 0;
        }

        .gape-learning-table {
            min-width: 900px;
        }

        .gape-learning-table [data-learning-context-column] {
            max-width: 220px;
            width: 18%;
        }

        .gape-learning-table.is-learning-grouped {
            min-width: 820px;
        }

        .gape-learning-table.is-learning-grouped [data-learning-context-column] {
            display: none;
        }

        .gape-learning-table.is-learning-grouped-by-organization [data-learning-row] > td:first-child {
            padding-inline-start: 96px !important;
        }

        .gape-learning-table.is-learning-grouped-by-course [data-learning-row] > td:first-child {
            padding-inline-start: 76px !important;
        }

        .gape-learning-table.is-learning-grouped-by-subject [data-learning-row] > td:first-child {
            padding-inline-start: 56px !important;
        }

        .gape-learning-table.is-learning-grouped-by-class-group [data-learning-row] > td:first-child {
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

        .gape-learning-organization-group-row td {
            background: #dcecff;
            border-bottom: 1px solid #b7d4f5;
            border-top: 18px solid #fff;
            padding-block: 18px !important;
        }

        .gape-learning-organization-group-row:first-child td {
            border-top-width: 0;
        }

        .gape-learning-unit-group-row td {
            background: #f5f9ff;
            border-bottom: 1px solid #d5e4f6;
            border-top: 8px solid #fff;
            padding-block: 12px !important;
        }

        .gape-learning-course-group-row td {
            background: #fff;
            border-bottom: 1px solid #e2e8f0;
            border-top: 6px solid #f8fbff;
            padding-block: 10px !important;
        }

        .gape-learning-subject-group-row td,
        .gape-learning-class-group-group-row td {
            background: #fbfdff;
            border-bottom: 1px solid #e2e8f0;
            border-top: 4px solid #f8fbff;
            padding-block: 10px !important;
        }

        .gape-learning-heading,
        .gape-learning-unit-heading,
        .gape-learning-course-heading,
        .gape-learning-subject-heading,
        .gape-learning-class-group-heading {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: space-between;
        }

        .gape-learning-title,
        .gape-learning-unit-title,
        .gape-learning-course-title,
        .gape-learning-subject-title,
        .gape-learning-class-group-title {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .gape-learning-chip {
            background: #fff;
            border: 1px solid #cddff4;
            border-radius: 999px;
            color: #475569;
            display: inline-flex;
            padding: 4px 10px;
        }

        .gape-learning-group-toggle {
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

        .gape-learning-group-toggle:hover,
        .gape-learning-group-toggle:focus-visible {
            background-color: #eef4fb;
            border-color: #d5e4f6;
            color: #334155;
        }

        .gape-learning-unit-heading {
            padding-inline-start: 0;
        }

        .gape-learning-course-heading.is-nested-under-organization {
            padding-inline-start: 20px;
        }

        .gape-learning-subject-heading.is-nested-under-organization {
            padding-inline-start: 40px;
        }

        .gape-learning-class-group-heading.is-nested-under-organization {
            padding-inline-start: 60px;
        }

        .gape-learning-subject-heading.is-nested-under-course {
            padding-inline-start: 20px;
        }

        .gape-learning-class-group-heading.is-nested-under-course {
            padding-inline-start: 40px;
        }

        .gape-learning-class-group-heading.is-nested-under-subject {
            padding-inline-start: 20px;
        }

        .gape-learning-unit-marker,
        .gape-learning-course-marker,
        .gape-learning-subject-marker,
        .gape-learning-class-group-marker {
            border-radius: 999px;
            display: inline-flex;
            height: 8px;
            width: 8px;
        }

        .gape-learning-unit-marker {
            background: var(--main-600);
        }

        .gape-learning-course-marker {
            background: #16a34a;
        }

        .gape-learning-subject-marker {
            background: #7c3aed;
        }

        .gape-learning-class-group-marker {
            background: #f59e0b;
        }

        .gape-event-icon {
            position: relative;
        }

        .gape-event-unread-marker {
            border: 2px solid #fff;
            height: 12px;
            inset-block-start: -4px;
            inset-inline-end: -4px;
            position: absolute;
            width: 12px;
        }

        .gape-event-rules-table {
            min-width: 860px;
        }

        .gape-event-rules-table th,
        .gape-event-rules-table td {
            vertical-align: top;
        }

        .gape-calendar-shell {
            min-width: 0;
        }

        .gape-calendar-toolbar {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: space-between;
        }

        .gape-calendar-nav {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            justify-content: flex-end;
        }

        .gape-calendar-scroll {
            min-width: 0;
            overflow-x: auto;
            padding-bottom: 4px;
        }

        .gape-calendar-grid {
            background: #e6edf0;
            border: 1px solid #d9e4ef;
            border-radius: 8px;
            display: grid;
            gap: 1px;
            grid-template-columns: repeat(7, minmax(132px, 1fr));
            min-width: 924px;
            overflow: hidden;
        }

        .gape-calendar-weekday {
            background: #506b59;
            color: #fff;
            font-size: 14px;
            font-weight: 600;
            line-height: 1.25;
            padding: 10px 12px;
            text-align: center;
        }

        .gape-calendar-day {
            background: #fff;
            min-height: 154px;
            min-width: 0;
            padding: 10px;
        }

        .gape-calendar-day.is-muted {
            background: #f5f7fa;
        }

        .gape-calendar-day.is-today {
            box-shadow: inset 0 0 0 2px rgba(37, 99, 235, 0.45);
        }

        .gape-calendar-day__number {
            align-items: center;
            color: #526074;
            display: flex;
            font-size: 16px;
            font-weight: 600;
            justify-content: space-between;
            line-height: 1;
            margin-bottom: 8px;
        }

        .gape-calendar-day.is-muted .gape-calendar-day__number {
            color: #a3adba;
        }

        .gape-calendar-count {
            background: #eaf2ff;
            border-radius: 999px;
            color: #2563eb;
            font-size: 11px;
            font-weight: 700;
            line-height: 1;
            padding: 5px 8px;
        }

        .gape-calendar-events {
            display: flex;
            flex-direction: column;
            gap: 7px;
        }

        .gape-calendar-event {
            align-items: flex-start;
            background: #f8fbff;
            border: 1px solid #e1eaf4;
            border-radius: 8px;
            color: #172033;
            display: flex;
            gap: 7px;
            min-width: 0;
            padding: 7px 8px;
            width: 100%;
        }

        .gape-calendar-event:hover {
            border-color: rgba(37, 99, 235, 0.4);
            color: #2563eb;
        }

        .gape-calendar-event__icon {
            align-items: center;
            border-radius: 6px;
            display: inline-flex;
            flex: 0 0 24px;
            height: 24px;
            justify-content: center;
            width: 24px;
        }

        .gape-calendar-event > span:last-child {
            flex: 1 1 auto;
            min-width: 0;
        }

        .gape-calendar-event__title {
            display: block;
            font-size: 12px;
            font-weight: 600;
            line-height: 1.25;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-calendar-show-all {
            cursor: pointer;
            font: inherit;
            text-align: left;
        }

        .gape-calendar-show-all:hover {
            color: #2563eb;
        }

        .gape-calendar-modal-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .gape-calendar-modal-item {
            align-items: flex-start;
            background: #fff;
            border: 1px solid #e1eaf4;
            border-radius: 8px;
            display: flex;
            gap: 10px;
            min-width: 0;
            padding: 12px;
        }

        .gape-calendar-event__meta {
            color: #64748b;
            display: block;
            font-size: 11px;
            line-height: 1.25;
            margin-top: 2px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .dashbord-body {
            min-width: 0;
        }

        .dashbord-body h1 {
            overflow-wrap: normal;
            word-break: normal;
        }

        .la-mode-grid {
            display: grid;
            gap: 16px;
            grid-template-columns: repeat(3, minmax(0, 1fr));
        }

        .la-mode-grid--events {
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .la-mode-card {
            align-items: flex-start;
            background: #fff;
            border: 1px solid #e6edf0;
            border-radius: 8px;
            color: #172033;
            cursor: pointer;
            display: flex;
            gap: 14px;
            min-height: 118px;
            padding: 20px;
            position: relative;
            text-align: left;
            transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease, background .2s ease;
            width: 100%;
        }

        .la-mode-card.is-active {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(20, 184, 166, 0.05)), #f7fbff;
            border-color: rgba(37, 99, 235, 0.7);
            box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12);
            color: #172033;
            transform: translateY(-1px);
        }

        /* Match the Class Group Management hover contract: no lift or
           coloured fill, only the same bounded border and shadow. */
        .la-mode-card:hover,
        .la-mode-card:focus-visible {
            border-color: rgba(37, 99, 235, .28);
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
            color: #172033;
            transform: none;
        }

        .la-mode-card.is-active .la-mode-icon {
            background: #2563eb !important;
            color: #fff !important;
        }

        .la-pending-corner-badge { align-items: center; background: #dc2626; border: 2px solid #fff; border-radius: 999px; color: #fff; display: inline-flex; font-size: 10px; font-weight: 700; justify-content: center; left: -8px; line-height: 1; min-height: 22px; min-width: 22px; padding: 3px 6px; position: absolute; top: -8px; z-index: 2; }

        .la-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex: 0 0 48px;
            height: 48px;
            justify-content: center;
            width: 48px;
        }

        [data-la-panel][hidden] {
            display: none !important;
        }

        [data-attendance-decision][hidden] {
            display: none !important;
        }

        /* Class Group Management list contract, reused by Lessons and Assessments. */
        .gape-learning-management-panel { min-width: 0; }

        .gape-learning-management-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-top: 20px;
        }

        .gape-learning-management-list-header,
        .gape-learning-management-row {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: minmax(260px, 1.45fr) minmax(180px, .95fr) minmax(105px, .45fr) minmax(115px, .5fr) minmax(110px, .45fr);
        }

        .gape-learning-management-list-header {
            border-bottom: 1px solid var(--neutral-30);
            color: #475569;
            font-size: 13px;
            font-weight: 600;
            padding: 16px 16px 12px;
        }

        .gape-learning-management-row > :nth-child(3) {
            justify-self: start;
        }

        .gape-learning-management-node { transition: border-color .2s ease, box-shadow .2s ease; }
        .gape-learning-management-node:hover { border-color: rgba(37, 99, 235, .28) !important; box-shadow: 0 10px 26px rgba(15, 23, 42, .05); }

        .gape-learning-management-icon,
        .gape-learning-management-group-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex: 0 0 40px;
            height: 40px;
            justify-content: center;
            width: 40px;
        }

        .gape-learning-management-icon-wrap { display: inline-flex; flex: 0 0 40px; position: relative; }
        .gape-learning-pending-corner-badge { align-items: center; background: #dc2626; border: 2px solid #fff; border-radius: 999px; color: #fff; display: inline-flex; font-size: 10px; font-weight: 700; justify-content: center; left: -9px; line-height: 1; min-height: 22px; min-width: 22px; padding: 3px 5px; position: absolute; top: -9px; z-index: 2; }
        .gape-learning-management-event-count-anchor { display: inline-flex; flex: 0 0 auto; position: relative; }

        .gape-learning-management-group {
            border-bottom: 1px solid var(--neutral-30);
            padding: 0 16px 12px;
        }

        .gape-learning-management-group-heading { padding-block: 4px 12px; }

        .gape-learning-management-children {
            border-top: 1px solid var(--neutral-30);
            margin-top: 8px;
            padding-top: 12px;
        }

        .gape-learning-management-content {
            display: flex;
            flex-direction: column;
            gap: 12px;
            padding: 0 0 4px 20px;
        }

        .gape-learning-management-completed-divider {
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

        .gape-learning-management-completed-divider::before,
        .gape-learning-management-completed-divider::after { background: #fecaca; content: ""; flex: 1; height: 1px; }
        .gape-learning-management-completed-node { border-color: #fecaca !important; }
        .gape-learning-management-completed-node.gape-learning-management-completed-node { padding-inline: 16px !important; }
        .gape-learning-management-completed-panel { border-top: 1px solid #fecaca; margin: 16px -16px 0; }
        .gape-learning-management-completed-content { padding: 16px 16px 0 36px; }

        .gape-learning-management-load-state {
            align-items: center;
            color: #64748b;
            display: flex;
            flex-wrap: wrap;
            font-size: 12px;
            gap: 8px 16px;
        }

        .gape-learning-management-load-state span + span::before {
            background: #cbd5e1;
            border-radius: 999px;
            content: "";
            display: inline-block;
            height: 4px;
            margin-right: 16px;
            vertical-align: middle;
            width: 4px;
        }

        .gape-learning-management-deferred-copy {
            border: 1px dashed #fecaca;
            border-radius: 8px;
            background: #fffafa;
            padding: 12px 14px;
        }

        .gape-learning-management-actions { min-width: 0; }
        .gape-node-meta { color: #64748b; display: flex; flex-wrap: wrap; gap: 8px; }

        @media (max-width: 575.98px) {
            .gape-learning-management-panel { max-width: calc(100vw - 48px); overflow: hidden; }
            .gape-learning-management-list-header { display: none; }
            .gape-learning-management-list { margin-top: 0; }
            .gape-learning-management-row { grid-template-columns: 1fr; }
            .gape-learning-management-content { padding-left: 0; }
            .gape-learning-management-completed-content { padding-left: 16px; }
            .gape-learning-management-actions { width: 100%; }
            .gape-learning-management-actions > .dropdown,
            .gape-learning-management-actions > a { flex: 1 1 calc(50% - 6px); min-width: 0; }
            .gape-learning-management-actions > .dropdown > button,
            .gape-learning-management-actions > a { justify-content: center; text-align: center; width: 100%; }
        }

        .aac-page {
            --aac-border: #e6edf0;
            --aac-ink: #172033;
            --aac-muted: #64748b;
            min-width: 0;
        }

        .aac-surface {
            background: #fff;
            border: 1px solid var(--aac-border);
            border-radius: 8px;
            box-shadow: 0 12px 28px rgba(15, 23, 42, 0.04);
            min-width: 0;
        }

        .aac-attendance-table {
            min-width: 980px;
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

        .aac-attendance-count {
            display: inline-block;
            max-width: 100%;
            white-space: nowrap;
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
            transition: border-color .2s ease, box-shadow .2s ease;
        }

        .aac-tree-node:hover {
            border-color: rgba(37, 99, 235, .28);
            box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
        }

        #attendance-panel [data-deferred-management-root] tr.hover-bg-neutral-20:hover {
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
            color: var(--aac-ink);
            display: block;
            font-size: 15px;
            font-weight: 600;
            line-height: 1.25;
            overflow-wrap: anywhere;
        }

        .aac-tree-meta {
            color: var(--aac-muted);
            display: block;
            font-size: 12px;
            line-height: 1.35;
            margin-top: 4px;
            overflow-wrap: anywhere;
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

        .aac-tree-actions {
            align-items: center;
            display: inline-flex;
            flex: 0 0 auto;
            flex-wrap: nowrap;
            gap: 10px;
            justify-content: flex-end;
            min-width: max-content;
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

        @media (max-width: 767px) {
            .la-mode-grid {
                grid-template-columns: 1fr;
            }

            .gape-learning-actions {
                width: 100%;
            }

            .gape-learning-actions > .dropdown {
                flex: 1 1 calc(50% - 6px);
                min-width: 0;
            }

            .gape-learning-actions > .dropdown > button,
            .gape-learning-actions > a,
            .gape-learning-actions > button {
                justify-content: center;
                text-align: center;
                white-space: nowrap;
                width: 100%;
            }

            .gape-learning-table.is-learning-grouped-by-organization [data-learning-row] > td:first-child {
                padding-inline-start: 64px !important;
            }

            .gape-learning-table.is-learning-grouped-by-course [data-learning-row] > td:first-child {
                padding-inline-start: 52px !important;
            }

            .gape-learning-table.is-learning-grouped-by-subject [data-learning-row] > td:first-child {
                padding-inline-start: 44px !important;
            }

            .gape-learning-table.is-learning-grouped-by-class-group [data-learning-row] > td:first-child {
                padding-inline-start: 36px !important;
            }

            .aac-settings-grid {
                grid-template-columns: 1fr;
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

            .aac-tree-actions {
                flex-wrap: wrap;
                justify-content: flex-start;
                min-width: 0;
            }

            .gape-event-rules-table {
                min-width: 0;
            }

            .gape-event-rules-table thead {
                display: none;
            }

            .gape-event-rules-table tbody,
            .gape-event-rules-table tr,
            .gape-event-rules-table td {
                display: block;
                width: 100%;
            }

            .gape-event-rules-table tr {
                border-bottom: 1px solid #e6edf0;
                padding: 12px 0;
            }

            .gape-event-rules-table td {
                align-items: start;
                border: 0;
                display: grid;
                gap: 12px;
                grid-template-columns: 86px minmax(0, 1fr);
                padding: 5px 0 !important;
            }

            .gape-event-rules-table td::before {
                color: #64748b;
                content: "";
                font-size: 12px;
                font-weight: 600;
                line-height: 1.4;
            }

            .gape-event-rules-table td:nth-child(1)::before {
                content: "Element";
            }

            .gape-event-rules-table td:nth-child(2)::before {
                content: "Dates";
            }

            .gape-event-rules-table td:nth-child(3)::before {
                content: "States";
            }

            .gape-event-rules-table td:nth-child(4)::before {
                content: "Events";
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
            <c:if test="${calendarMode}">
                <c:set var="pageTitle" value="Events" scope="request" />
            </c:if>
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <c:if test="${not calendarMode}">
                    <div class="la-mode-grid mb-24" data-la-tabs role="tablist" aria-label="Lessons and assessments views">
                        <button type="button" class="la-mode-card is-active" data-la-tab="lessons" role="tab" aria-selected="true" aria-controls="lessons-panel">
                            <span class="la-mode-icon bg-main-50 text-main-600 text-24"><i class="ph ph-chalkboard"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Lessons</span>
                                <span class="text-13 text-main-600 fw-semibold"><c:out value="${lessonCount}"/> lessons | <c:out value="${scheduledLessonCount}"/> scheduled | <c:out value="${activeLessonCount}"/> active</span>
                            </span>
                        </button>
                        <button type="button" class="la-mode-card" data-la-tab="assessments" role="tab" aria-selected="false" aria-controls="assessments-panel">
                            <c:if test="${pendingAssessmentEnrollmentCount + pendingCorrectionCount > 0}"><span class="la-pending-corner-badge" title="${pendingAssessmentEnrollmentCount} pending enrollment request(s) and ${pendingCorrectionCount} pending correction(s)"><c:out value="${pendingAssessmentEnrollmentCount + pendingCorrectionCount}"/></span></c:if>
                            <span class="la-mode-icon bg-info-50 text-info-600 text-24"><i class="ph ph-seal-question"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Assessments</span>
                                <span class="text-13 text-main-600 fw-semibold"><c:out value="${assessmentCount}"/> assessments | <c:out value="${testCount}"/> tests | <c:out value="${examCount}"/> exams</span>
                            </span>
                        </button>
                        <button type="button" class="la-mode-card" data-la-tab="attendance" role="tab" aria-selected="false" aria-controls="attendance-panel">
                            <span class="la-mode-icon bg-success-50 text-success-600 text-24"><i class="ph ph-user-check"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Attendance</span>
                                <span class="text-13 text-main-600 fw-semibold">Attendance records and justifications</span>
                            </span>
                        </button>
                    </div>
                </c:if>

                <c:if test="${calendarMode}">
                    <div class="la-mode-grid la-mode-grid--events mb-24" data-la-tabs role="tablist" aria-label="Events views">
                        <button type="button" class="la-mode-card is-active" data-la-tab="event-list" role="tab" aria-selected="true" aria-controls="event-list-panel">
                            <span class="la-mode-icon bg-main-50 text-main-600 text-24"><i class="ph ph-list-bullets"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">List</span>
                                <span class="text-13 text-main-600 fw-semibold"><c:out value="${eventTotalCount}"/> events | <c:out value="${eventListUnreadCount}"/> unread</span>
                            </span>
                        </button>
                        <button type="button" class="la-mode-card" data-la-tab="event-calendar" role="tab" aria-selected="false" aria-controls="event-calendar-panel">
                            <span class="la-mode-icon bg-success-50 text-success-600 text-24"><i class="ph ph-calendar-dots"></i></span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Calendar</span>
                                <span class="text-13 text-main-600 fw-semibold" data-event-calendar-card-summary><c:out value="${eventCalendar.visibleMonthEventCount}"/> events in <c:out value="${eventCalendar.title}"/></span>
                            </span>
                        </button>
                    </div>
                </c:if>

                <c:if test="${not calendarMode}">
                    <div id="lessons-panel" data-la-panel="lessons">
                        <c:set var="learningManagementKind" value="lessons"/>
                        <c:set var="learningManagementTotal" value="${lessonManagementPageTotal}"/>
                        <c:set var="learningManagementOffset" value="${lessonManagementOffset}"/>
                        <c:set var="learningManagementCurrentPage" value="${lessonManagementCurrentPage}"/>
                        <c:set var="learningManagementLoadAll" value="${lessonManagementLoadAll}"/>
                        <c:set var="learningManagementCompletedCount" value="${lessonManagementCompletedTotal}"/>
                        <c:set var="learningManagementHasMore" value="${lessonManagementHasMore}"/>
                        <c:set var="learningManagementNextOffset" value="${lessonManagementNextOffset}"/>
                        <%@ include file="/WEB-INF/fragments/learning-management-panel.jspf" %>
                    </div>
                </c:if>

                <c:if test="${calendarMode}">
                <div id="event-list-panel" class="bg-white rounded-10 px-24 py-24" data-la-panel="event-list" data-gape-sort-root data-gape-group-item-label="event">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4 d-flex align-items-center gap-8">
                                ${calendarMode ? 'Events' : 'Lessons'}
                            </h2>
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Operational events generated from existing records, ordered by date.' : 'Online, presential and hybrid lessons ordered by date.'}</span>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <c:if test="${calendarMode}">
                                <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-event-controls>
                                    <span class="visually-hidden">Events filter</span>
                                    <span class="visually-hidden">Events class group filter</span>
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
                            </c:if>
                            <c:if test="${not calendarMode}">
                                <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-learning-controls="lessons">
                                    <div class="dropdown">
                                        <button type="button"
                                                class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                                data-bs-toggle="dropdown"
                                                data-bs-auto-close="outside"
                                                data-learning-sort-toggle
                                                aria-expanded="false">
                                            <i class="ph ph-sort-ascending"></i>Sort by
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                            <li>
                                                <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                    <span>Name</span>
                                                    <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                            <li>
                                                <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false">
                                                    <span>Date</span>
                                                    <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                            <li>
                                                <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
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
                                                data-learning-group-toggle
                                                aria-expanded="false">
                                            <i class="ph ph-stack"></i>Group by
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                            <li>
                                                <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="organization" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                    <span>Organization</span>
                                                    <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                            <li>
                                                <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="course" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                    <span>Courses</span>
                                                    <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                            <li>
                                                <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="subject" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                    <span>Subjects</span>
                                                    <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                            <li>
                                                <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="classGroup" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                    <span>Class Groups</span>
                                                    <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                                </button>
                                            </li>
                                        </ul>
                                    </div>
                                    <a href="${pageContext.request.contextPath}/learning/lessons/new?returnTo=/learning/lessons" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center" style="min-height: 44px;" data-lesson-modal-url="${pageContext.request.contextPath}/learning/lessons/new?returnTo=/learning/lessons" data-lesson-modal-title="Create Lesson">
                                        <i class="ph ph-plus-circle me-8"></i>New Lesson
                                    </a>
                                </div>
                            </c:if>
                            <c:if test="${not empty topActionHref}">
                                <a href="${pageContext.request.contextPath}${topActionHref}" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 d-inline-flex align-items-center" style="min-height: 44px;">
                                    <i class="ph ph-plus-circle me-8"></i>${topActionLabel}
                                </a>
                            </c:if>
                        </div>
                    </div>

                    <c:if test="${calendarMode}">
                        <div class="bg-warning-50 text-warning-700 rounded-10 px-18 py-14 mb-20 d-flex align-items-center justify-content-between gap-12 border border-warning-100 flex-wrap">
                            <div class="d-flex align-items-start gap-10">
                                <i class="ph ph-warning-circle text-22 mt-2 flex-shrink-0"></i>
                                <span class="text-14 fw-medium">Only selected project elements are treated as events. Draft records are never shown.</span>
                            </div>
                            <button type="button" class="border-0 bg-white text-warning-700 px-14 py-8 rounded-8 fw-semibold d-inline-flex align-items-center gap-6" data-bs-toggle="modal" data-bs-target="#eventRulesModal">
                                <i class="ph ph-list-checks"></i>Show more
                            </button>
                        </div>

                        <div class="modal fade" id="eventRulesModal" tabindex="-1" aria-hidden="true">
                            <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
                                <div class="modal-content rounded-8 border-0 bg-white">
                                    <div class="modal-header border-neutral-30">
                                        <div>
                                            <h5 class="modal-title text-18 fw-semibold mb-4">Event rules</h5>
                                            <span class="text-13 text-neutral-500">A new event is created whenever one of the selected elements changes state. Draft state is ignored.</span>
                                        </div>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                    </div>
                                    <div class="modal-body">
                                        <div class="overflow-x-auto">
                                            <table class="table mb-0 gape-event-rules-table">
                                                <thead>
                                                <tr>
                                                    <th class="py-12 px-14 text-13 fw-medium text-neutral-600">Element</th>
                                                    <th class="py-12 px-14 text-13 fw-medium text-neutral-600">Dates</th>
                                                    <th class="py-12 px-14 text-13 fw-medium text-neutral-600">States</th>
                                                    <th class="py-12 px-14 text-13 fw-medium text-neutral-600">Events</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                <tr><td class="py-12 px-14">Lesson</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Draft, Scheduled, Active, Completed, Cancelled</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Assessment</td><td class="py-12 px-14">Available from, available until</td><td class="py-12 px-14">Draft, Scheduled, Active, Completed</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Attendance</td><td class="py-12 px-14">Check-in, check-out</td><td class="py-12 px-14">Active, Corrected, Cancelled; Present, Absent, Justified, Late, Partial</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Absence justification</td><td class="py-12 px-14">Submitted, processed</td><td class="py-12 px-14">Submitted, Under review, Approved, Rejected, Cancelled</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Grade sheet</td><td class="py-12 px-14">Released</td><td class="py-12 px-14">Draft, Published, Closed, Inactive</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Certificate</td><td class="py-12 px-14">Issued</td><td class="py-12 px-14">Draft, Active, Issued</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Class group enrollment request</td><td class="py-12 px-14">Request period</td><td class="py-12 px-14">Pending approval only</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event while pending</span></td></tr>
                                                <tr><td class="py-12 px-14">Assessment enrollment request</td><td class="py-12 px-14">Assessment availability</td><td class="py-12 px-14">Pending approval only</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event while pending</span></td></tr>
                                                <tr><td class="py-12 px-14">Course enrollment</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Active, Inactive, Completed, Withdrawn</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Class group</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Draft, Scheduled, Active, Completed</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Assessment correction</td><td class="py-12 px-14">Submitted</td><td class="py-12 px-14">Pending correction only</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event while pending</span></td></tr>
                                                <tr><td class="py-12 px-14">Teacher assignment</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Active, Inactive</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Subject coordination</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Active, Inactive</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Organization management</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Active, Inactive</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Deletion request</td><td class="py-12 px-14">Submitted, processed</td><td class="py-12 px-14">Submitted, Under review, Approved, Rejected, Completed</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">User account</td><td class="py-12 px-14">Created</td><td class="py-12 px-14">Active, Inactive, Blocked</td><td class="py-12 px-14"><span class="bg-success-50 text-success-600 px-10 py-6 rounded-pill text-12">Event</span></td></tr>
                                                <tr><td class="py-12 px-14">Grade record</td><td class="py-12 px-14">Recorded</td><td class="py-12 px-14">Draft, Published, Corrected, Inactive</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Pedagogical block</td><td class="py-12 px-14">Created, updated</td><td class="py-12 px-14">No state</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Pedagogical content</td><td class="py-12 px-14">Created, updated</td><td class="py-12 px-14">Draft, Active, Inactive</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Content file</td><td class="py-12 px-14">Created, processed</td><td class="py-12 px-14">No state</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Channel</td><td class="py-12 px-14">Created</td><td class="py-12 px-14">Active, Inactive</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Channel participation</td><td class="py-12 px-14">Joined</td><td class="py-12 px-14">Active, Inactive, Blocked</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Message</td><td class="py-12 px-14">Created, updated, scheduled, sent</td><td class="py-12 px-14">Draft, Scheduled, Sent, Active, Edited, Deleted, Cancelled</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Message receipt</td><td class="py-12 px-14">Delivered, read</td><td class="py-12 px-14">Pending, Delivered, Read, Failed</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Assessment response</td><td class="py-12 px-14">Answered</td><td class="py-12 px-14">No state</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">User session</td><td class="py-12 px-14">Start, last activity, end</td><td class="py-12 px-14">Active, Expired, Closed</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Audit log</td><td class="py-12 px-14">Occurred</td><td class="py-12 px-14">No state</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                <tr><td class="py-12 px-14">Learning event record</td><td class="py-12 px-14">Occurred, created, updated</td><td class="py-12 px-14">Visible, Hidden</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not source</span></td></tr>
                                                <tr><td class="py-12 px-14">Legacy schedule event</td><td class="py-12 px-14">Start, end</td><td class="py-12 px-14">Draft, Active, Inactive, Cancelled, Completed</td><td class="py-12 px-14"><span class="bg-neutral-50 text-neutral-600 px-10 py-6 rounded-pill text-12">Not event</span></td></tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <c:set var="notificationRows" value="${learningNotifications}" />
                        <c:if test="${empty notificationRows and not empty eventNotifications}">
                            <c:set var="notificationRows" value="${eventNotifications}" />
                        </c:if>
                        <div class="overflow-x-auto">
                            <table class="table mb-0 gape-notifications-table">
                                <thead>
                                <tr>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Event</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-gape-group-hide-when-grouped>Context</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Date</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Action</th>
                                </tr>
                                </thead>
                                <tbody data-gape-sort-list>
                                <c:forEach var="notification" items="${notificationRows}" varStatus="notificationLoop">
                                    <c:set var="notificationClassGroup" value="${not empty notification.classGroupId ? classGroupById[notification.classGroupId] : null}"/>
                                    <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                        data-gape-sort-row
                                        data-sort-index="${notificationLoop.index}"
                                        data-sort-name="${fn:escapeXml(notification.title)}"
                                        data-sort-date="${notification.dateValue}"
                                        data-sort-status="${fn:escapeXml(notification.stateLabel)}"
                                        data-sort-type="${fn:escapeXml(notification.categoryLabel)}"
                                        data-group-organization-id="${not empty notificationClassGroup ? notificationClassGroup.course.organizationId : ''}"
                                        data-group-organization="${not empty notificationClassGroup ? fn:escapeXml(notificationClassGroup.course.organizationName) : 'Unknown organization'}"
                                        data-group-organic-unit-id="${not empty notificationClassGroup ? notificationClassGroup.course.organicUnitId : ''}"
                                        data-group-organic-unit="${not empty notificationClassGroup ? fn:escapeXml(notificationClassGroup.course.organicUnitLabel) : 'No organic unit'}"
                                        data-group-course-id="${not empty notificationClassGroup ? notificationClassGroup.courseId : ''}"
                                        data-group-course="${not empty notificationClassGroup ? fn:escapeXml(notificationClassGroup.courseName) : 'No course'}"
                                        data-group-subject-id="${not empty notificationClassGroup ? notificationClassGroup.subjectId : ''}"
                                        data-group-subject="${not empty notificationClassGroup ? fn:escapeXml(notificationClassGroup.subjectName) : 'No subject'}"
                                        data-group-class-group-id="${not empty notificationClassGroup ? notificationClassGroup.id : notification.classGroupId}"
                                        data-group-class-group="${not empty notificationClassGroup ? fn:escapeXml(notificationClassGroup.code) : 'No class group'}">
                                        <td class="py-20 px-20">
                                            <div class="d-flex align-items-start gap-12">
                                                <span class="${notification.badgeClass} gape-event-icon w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                    <i class="${notification.iconClass}"></i>
                                                    <c:if test="${notification.unread}">
                                                        <span class="gape-event-unread-marker bg-danger-600 rounded-circle" aria-label="Unread event"></span>
                                                    </c:if>
                                                </span>
                                                <div>
                                                    <a href="${pageContext.request.contextPath}${notification.actionHref}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                        <c:out value="${notification.title}"/>
                                                    </a>
                                                    <span class="d-block text-12 text-neutral-500"><c:out value="${notification.categoryLabel}"/> | <c:out value="${notification.description}"/></span>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${notification.contextTitle}'/>" data-gape-group-hide-when-grouped>
                                            <c:out value="${notification.contextHtml}" escapeXml="false"/>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display><c:out value="${notification.dateLabel}"/></td>
                                        <td class="py-20 px-20">
                                            <span class="${notification.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                <c:out value="${notification.stateLabel}"/>
                                            </span>
                                        </td>
                                        <td class="py-20 px-20 text-end">
                                            <a href="${pageContext.request.contextPath}${notification.actionHref}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty notificationRows}">
                                    <tr>
                                        <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No events available in your context.</td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                        <c:if test="${eventTotalCount > 0}">
                            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mt-20">
                                <span class="text-14 text-neutral-500">
                                    Showing ${eventPageStart}-${eventPageEnd} of ${eventTotalCount} events
                                </span>
                                <c:if test="${eventPageCount > 1}">
                                    <div class="d-flex align-items-center gap-8">
                                        <c:url var="eventsPreviousHref" value="/learning/events">
                                            <c:param name="contentType" value="${selectedCalendarFilter}" />
                                            <c:if test="${not empty selectedClassGroupId}">
                                                <c:param name="classGroupId" value="${selectedClassGroupId}" />
                                            </c:if>
                                            <c:param name="page" value="${eventPreviousPage}" />
                                        </c:url>
                                        <c:url var="eventsNextHref" value="/learning/events">
                                            <c:param name="contentType" value="${selectedCalendarFilter}" />
                                            <c:if test="${not empty selectedClassGroupId}">
                                                <c:param name="classGroupId" value="${selectedClassGroupId}" />
                                            </c:if>
                                            <c:param name="page" value="${eventNextPage}" />
                                        </c:url>
                                        <c:choose>
                                            <c:when test="${eventHasPreviousPage}">
                                                <a href="${eventsPreviousHref}" class="px-14 py-8 rounded-8 border border-neutral-30 text-14 text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center gap-6">
                                                    <i class="ph ph-caret-left"></i>Previous
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="px-14 py-8 rounded-8 border border-neutral-30 text-14 text-neutral-300 d-inline-flex align-items-center gap-6" aria-disabled="true">
                                                    <i class="ph ph-caret-left"></i>Previous
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                        <span class="text-14 text-neutral-500">Page ${eventCurrentPage} of ${eventPageCount}</span>
                                        <c:choose>
                                            <c:when test="${eventHasNextPage}">
                                                <a href="${eventsNextHref}" class="px-14 py-8 rounded-8 border border-neutral-30 text-14 text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center gap-6">
                                                    Next<i class="ph ph-caret-right"></i>
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="px-14 py-8 rounded-8 border border-neutral-30 text-14 text-neutral-300 d-inline-flex align-items-center gap-6" aria-disabled="true">
                                                    Next<i class="ph ph-caret-right"></i>
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </c:if>
                            </div>
                        </c:if>
                    </c:if>

                    <c:if test="${not calendarMode}">
                    <div class="gape-learning-table-scroll overflow-x-auto">
                        <table class="table mb-0 gape-learning-table" data-learning-table="lessons">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Lesson</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-learning-context-column>Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Date</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Access</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-learning-list="lessons">
                            <c:forEach var="lesson" items="${lessons}" varStatus="lessonLoop">
                                <c:set var="classGroup" value="${classGroupById[lesson.classGroupId]}"/>
                                <c:set var="canManageLessonRow" value="${canManageClassGroupById[lesson.classGroupId]}"/>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-learning-row
                                    data-sort-index="${lessonLoop.index}"
                                    data-sort-name="${fn:escapeXml(lesson.title)}"
                                    data-sort-date="${lesson.startsAtRaw}"
                                    data-sort-status="${fn:escapeXml(lesson.stateLabel)}"
                                    data-sort-type="${fn:escapeXml(lesson.typeLabel)}"
                                    data-group-organization-id="${not empty classGroup ? classGroup.course.organizationId : ''}"
                                    data-group-organization="${not empty classGroup ? fn:escapeXml(classGroup.course.organizationName) : 'Unknown organization'}"
                                    data-group-organic-unit-id="${not empty classGroup ? classGroup.course.organicUnitId : ''}"
                                    data-group-organic-unit="${not empty classGroup ? fn:escapeXml(classGroup.course.organicUnitLabel) : 'No organic unit'}"
                                    data-group-course-id="${not empty classGroup ? classGroup.courseId : ''}"
                                    data-group-course="${not empty classGroup ? fn:escapeXml(classGroup.courseName) : 'No course'}"
                                    data-group-subject-id="${not empty classGroup ? classGroup.subjectId : ''}"
                                    data-group-subject="${not empty classGroup ? fn:escapeXml(classGroup.subjectName) : 'No subject'}"
                                    data-group-class-group-id="${lesson.classGroupId}"
                                    data-group-class-group="${not empty classGroup ? fn:escapeXml(classGroup.code) : 'Class group'}">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-start gap-12">
                                            <span class="${lesson.typeBadgeClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                <i class="${lesson.typeIconClass}"></i>
                                            </span>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" class="fw-medium text-14 text-neutral-700 hover-text-main-600" data-lesson-modal-url="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" data-lesson-modal-title="Lesson Details">
                                                    <c:out value="${lesson.title}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${lesson.typeLabel}"/> | <c:out value="${lesson.durationLabel}"/></span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-learning-context-column>
                                        <c:choose>
                                            <c:when test="${not empty classGroup}">
                                                <c:out value="${classGroup.contextHtml}" escapeXml="false"/>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display>
                                        <c:out value="${lesson.startsAt}"/>
                                        <span class="d-block text-12 text-neutral-500">to <c:out value="${lesson.endsAt}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:choose>
                                            <c:when test="${lesson.hasRoom}">
                                                <span class="bg-main-two-50 text-main-two-600 px-12 py-7 rounded-pill text-12">
                                                    <i class="ph ph-door me-6"></i><c:out value="${lesson.physicalRoomCode}"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${lesson.hasMeetingLink}">
                                                <span class="bg-main-50 text-main-600 px-12 py-7 rounded-pill text-12">
                                                    <i class="ph ph-video-camera me-6"></i>Meeting link
                                                </span>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="${lesson.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${lesson.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail Lesson" data-lesson-modal-url="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" data-lesson-modal-title="Lesson Details">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canManageLessonRow}">
                                                <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/edit?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Edit Lesson" data-lesson-modal-url="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/edit?returnTo=${currentReturnToParam}" data-lesson-modal-title="Edit Lesson">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                                <form action="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/delete" method="post" class="m-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <input type="hidden" name="returnTo" value="/learning/lessons">
                                                    <button type="submit" class="text-22 text-neutral-500 hover-text-danger-600 bg-transparent border-0 p-0" title="Delete" aria-label="Delete lesson">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </form>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty lessons}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No lessons available in your context.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    </c:if>
                </div>

                </c:if>

                <c:if test="${calendarMode}">
                    <div id="event-calendar-panel" class="bg-white rounded-10 px-24 py-24 mt-24" data-la-panel="event-calendar" hidden>
                        <div class="gape-calendar-shell" data-event-calendar-root data-current-month="${eventCalendar.monthValue}" data-total-events="${eventCalendar.totalEventCount}">
                            <div class="gape-calendar-toolbar mb-20">
                                <div>
                                    <h2 class="text-18 fw-medium text-neutral-700 mb-4 d-flex align-items-center gap-8">
                                        Calendar
                                    </h2>
                                    <span class="text-14 text-neutral-500">
                                        Monthly calendar for all filtered events. Showing <span data-event-calendar-month-count><c:out value="${eventCalendar.visibleMonthEventCount}"/></span> of <span data-event-calendar-total-count><c:out value="${eventCalendar.totalEventCount}"/></span> events in this month.
                                    </span>
                                </div>
                                <div class="gape-calendar-nav">
                                    <button type="button" class="w-40 h-40 rounded-8 border border-neutral-30 bg-white text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center justify-content-center" title="Previous year" aria-label="Previous year" data-event-calendar-unit="year" data-event-calendar-step="-1">
                                        <i class="ph ph-caret-double-left"></i>
                                    </button>
                                    <button type="button" class="w-40 h-40 rounded-8 border border-neutral-30 bg-white text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center justify-content-center" title="Previous month" aria-label="Previous month" data-event-calendar-unit="month" data-event-calendar-step="-1">
                                        <i class="ph ph-caret-left"></i>
                                    </button>
                                    <span class="px-16 py-10 rounded-8 bg-neutral-20 text-neutral-700 fw-semibold text-14" data-event-calendar-title>
                                        <c:out value="${eventCalendar.title}"/>
                                    </span>
                                    <button type="button" class="w-40 h-40 rounded-8 border border-neutral-30 bg-white text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center justify-content-center" title="Next month" aria-label="Next month" data-event-calendar-unit="month" data-event-calendar-step="1">
                                        <i class="ph ph-caret-right"></i>
                                    </button>
                                    <button type="button" class="w-40 h-40 rounded-8 border border-neutral-30 bg-white text-neutral-600 hover-bg-neutral-20 d-inline-flex align-items-center justify-content-center" title="Next year" aria-label="Next year" data-event-calendar-unit="year" data-event-calendar-step="1">
                                        <i class="ph ph-caret-double-right"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="gape-calendar-scroll">
                                <div class="gape-calendar-grid" role="grid" aria-label="Events calendar" data-event-calendar-grid>
                                    <div class="gape-calendar-weekday" role="columnheader">Monday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Tuesday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Wednesday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Thursday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Friday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Saturday</div>
                                    <div class="gape-calendar-weekday" role="columnheader">Sunday</div>
                                    <c:forEach var="day" items="${eventCalendar.days}">
                                        <div class="gape-calendar-day ${day.currentMonth ? '' : 'is-muted'} ${day.today ? 'is-today' : ''}" role="gridcell">
                                            <div class="gape-calendar-day__number">
                                                <span><c:out value="${day.dayLabel}"/></span>
                                                <c:if test="${day.hasEvents}">
                                                    <span class="gape-calendar-count"><c:out value="${day.eventCount}"/></span>
                                                </c:if>
                                            </div>
                                            <div class="gape-calendar-events">
                                                <c:forEach var="event" items="${day.events}" begin="0" end="0">
                                                    <a href="${pageContext.request.contextPath}${event.actionHref}" class="gape-calendar-event" title="<c:out value='${event.title}'/>">
                                                        <span class="${event.badgeClass} gape-calendar-event__icon text-13">
                                                            <i class="${event.iconClass}"></i>
                                                        </span>
                                                        <span class="min-w-0">
                                                            <span class="gape-calendar-event__title"><c:out value="${event.title}"/></span>
                                                            <span class="gape-calendar-event__meta"><c:out value="${event.categoryLabel}"/> | <c:out value="${event.stateLabel}"/></span>
                                                        </span>
                                                    </a>
                                                </c:forEach>
                                                <c:if test="${day.eventCount > 1}">
                                                    <button type="button" class="gape-calendar-event gape-calendar-show-all" data-event-calendar-show-all data-event-date="${day.date}">
                                                        <span class="bg-main-50 text-main-600 gape-calendar-event__icon text-13">
                                                            <i class="ph ph-list-bullets"></i>
                                                        </span>
                                                        <span class="min-w-0">
                                                            <span class="gape-calendar-event__title">Show all</span>
                                                            <span class="gape-calendar-event__meta"><c:out value="${day.eventCount}"/> events</span>
                                                        </span>
                                                    </button>
                                                </c:if>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <div data-event-calendar-source hidden>
                                <c:forEach var="event" items="${eventCalendarNotifications}">
                                    <span data-event-calendar-source-item
                                          data-event-date="<c:out value='${event.dateValue}'/>"
                                          data-event-time="<c:out value='${event.timeLabel}'/>"
                                          data-event-date-label="<c:out value='${event.dateLabel}'/>"
                                          data-event-title="<c:out value='${event.title}'/>"
                                          data-event-category="<c:out value='${event.categoryLabel}'/>"
                                          data-event-state="<c:out value='${event.stateLabel}'/>"
                                          data-event-context="<c:out value='${event.contextLabel}'/>"
                                          data-event-href="<c:out value='${pageContext.request.contextPath}${event.actionHref}'/>"
                                          data-event-icon-class="<c:out value='${event.iconClass}'/>"
                                          data-event-badge-class="<c:out value='${event.badgeClass}'/>"
                                          data-event-state-badge-class="<c:out value='${event.stateBadgeClass}'/>"></span>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                    <div class="modal fade" id="eventCalendarEventsModal" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
                            <div class="modal-content rounded-8 border-0 bg-white">
                                <div class="modal-header border-neutral-30">
                                    <div>
                                        <h5 class="modal-title text-18 fw-semibold mb-4" data-event-calendar-modal-title>Events</h5>
                                        <span class="text-13 text-neutral-500" data-event-calendar-modal-subtitle>All events in the selected date.</span>
                                    </div>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="gape-calendar-modal-list" data-event-calendar-modal-body></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>

                <c:if test="${not calendarMode}">
                    <div id="assessments-panel" data-la-panel="assessments" hidden>
                        <c:set var="learningManagementKind" value="assessments"/>
                        <c:set var="learningManagementTotal" value="${assessmentManagementPageTotal}"/>
                        <c:set var="learningManagementOffset" value="${assessmentManagementOffset}"/>
                        <c:set var="learningManagementCurrentPage" value="${assessmentManagementCurrentPage}"/>
                        <c:set var="learningManagementLoadAll" value="${assessmentManagementLoadAll}"/>
                        <c:set var="learningManagementCompletedCount" value="${assessmentManagementCompletedTotal}"/>
                        <c:set var="learningManagementHasMore" value="${assessmentManagementHasMore}"/>
                        <c:set var="learningManagementNextOffset" value="${assessmentManagementNextOffset}"/>
                        <%@ include file="/WEB-INF/fragments/learning-management-panel.jspf" %>
                    </div>
                </c:if>

                <c:if test="${false}">
                    <div class="bg-white rounded-10 px-24 py-24" data-la-panel="legacy-assessments" hidden>
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                            <div>
                                <h2 class="text-18 fw-medium text-neutral-700 mb-4">Assessments</h2>
                                <span class="text-14 text-neutral-500">Questions, attempts, corrections and downloadable assessment documents.</span>
                            </div>
                            <div class="gape-learning-actions d-flex align-items-center gap-12 flex-wrap" data-learning-controls="assessments">
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-learning-sort-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-sort-ascending"></i>Sort by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>Name</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false">
                                                <span>Date</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
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
                                            data-learning-group-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-stack"></i>Group by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="organization" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Organization</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="course" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Courses</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="subject" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Subjects</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-learning-group-option data-group-field="classGroup" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Class Groups</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                                <a href="${pageContext.request.contextPath}/learning/assessments/new" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center" style="min-height: 44px;">
                                    <i class="ph ph-plus-circle me-8"></i>New Assessment
                                </a>
                            </div>
                        </div>

                        <div class="gape-learning-table-scroll overflow-x-auto">
                            <table class="table mb-0 gape-learning-table" data-learning-table="assessments">
                                <thead>
                                <tr>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Assessment</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-learning-context-column>Context</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Availability</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                                </tr>
                                </thead>
                                <tbody data-learning-list="assessments">
                                <c:forEach var="assessment" items="${assessments}" varStatus="assessmentLoop">
                                    <c:set var="assessmentClassGroup" value="${classGroupById[assessment.classGroupId]}"/>
                                    <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                        data-learning-row
                                        data-sort-index="${assessmentLoop.index}"
                                        data-sort-name="${fn:escapeXml(assessment.title)}"
                                        data-sort-date="${assessment.availableFromRaw}"
                                        data-sort-status="${fn:escapeXml(assessment.stateLabel)}"
                                        data-sort-type="${fn:escapeXml(assessment.typeLabel)}"
                                        data-group-organization-id="${not empty assessmentClassGroup ? assessmentClassGroup.course.organizationId : ''}"
                                        data-group-organization="${not empty assessmentClassGroup ? fn:escapeXml(assessmentClassGroup.course.organizationName) : 'Unknown organization'}"
                                        data-group-organic-unit-id="${not empty assessmentClassGroup ? assessmentClassGroup.course.organicUnitId : ''}"
                                        data-group-organic-unit="${not empty assessmentClassGroup ? fn:escapeXml(assessmentClassGroup.course.organicUnitLabel) : 'No organic unit'}"
                                        data-group-course-id="${not empty assessmentClassGroup ? assessmentClassGroup.courseId : ''}"
                                        data-group-course="${not empty assessmentClassGroup ? fn:escapeXml(assessmentClassGroup.courseName) : 'No course'}"
                                        data-group-subject-id="${not empty assessmentClassGroup ? assessmentClassGroup.subjectId : assessment.subjectId}"
                                        data-group-subject="${fn:escapeXml(assessment.subjectName)}"
                                        data-group-class-group-id="${not empty assessmentClassGroup ? assessmentClassGroup.id : assessment.classGroupId}"
                                        data-group-class-group="${not empty assessmentClassGroup ? fn:escapeXml(assessmentClassGroup.code) : (not empty assessment.classGroupCode ? fn:escapeXml(assessment.classGroupCode) : 'No class group')}">
                                        <td class="py-20 px-20">
                                            <div class="d-flex align-items-start gap-12">
                                                <span class="${assessment.softClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                    <i class="${assessment.iconClass}"></i>
                                                </span>
                                                <div class="min-w-0">
                                                    <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                        <c:out value="${assessment.title}"/>
                                                    </a>
                                                    <span class="d-block text-12 text-neutral-500">
                                                        <c:out value="${assessment.typeLabel}"/> | <c:out value="${assessment.modeLabel}"/> | <c:out value="${assessment.correctionModeLabel}"/>
                                                    </span>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-learning-context-column>
                                            <c:out value="${assessment.contextLabel}"/>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display>
                                            <c:choose>
                                                <c:when test="${empty assessment.availabilityLabel}">Always available</c:when>
                                                <c:otherwise><c:out value="${assessment.availabilityLabel}"/></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="py-20 px-20">
                                            <span class="${assessment.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                <c:out value="${assessment.stateLabel}"/>
                                            </span>
                                        </td>
                                        <td class="py-20 px-20 text-end">
                                            <div class="d-flex align-items-center gap-12 justify-content-end">
                                                <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail">
                                                    <i class="ph ph-eye"></i>
                                                </a>
                                                <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                                <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/pdf" class="text-22 text-neutral-500 hover-text-main-600" title="Download" aria-label="Download">
                                                    <i class="ph ph-download-simple"></i>
                                                </a>
                                                <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/delete" method="post" class="m-0">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                    <input type="hidden" name="returnTo" value="/learning/lessons#assessments">
                                                    <button type="submit" class="text-22 text-neutral-500 hover-text-danger-600 bg-transparent border-0 p-0" title="Delete" aria-label="Delete">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty assessments}">
                                    <tr>
                                        <td colspan="5" class="py-40 px-20 text-center text-14 text-neutral-500">No assessments available in your management context.</td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:if>

                <c:if test="${not calendarMode}">
                    <div id="attendance-panel" class="aac-page" data-la-panel="attendance" hidden>
                        <c:set var="lessonAssessmentAttendanceControls" value="true" scope="request"/>
                        <%@ include file="/WEB-INF/fragments/learning-attendance-content.jspf" %>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<script>
    (function () {
        var pickers = document.querySelectorAll('[data-class-group-picker]');
        pickers.forEach(function (picker) {
            var label = picker.querySelector('[data-class-group-picker-label]');
            var inputs = picker.querySelectorAll('[data-class-group-picker-input]');
            var updateLabel = function () {
                var selected = Array.prototype.filter.call(inputs, function (input) {
                    return input.checked;
                }).length;
                if (label) {
                    label.textContent = selected === 0
                            ? 'Select class groups'
                            : selected + (selected === 1 ? ' class group selected' : ' class groups selected');
                }
            };
            inputs.forEach(function (input) {
                input.addEventListener('change', updateLabel);
            });
            updateLabel();
        });

        document.addEventListener('click', function (event) {
            pickers.forEach(function (picker) {
                if (!picker.contains(event.target)) {
                    picker.removeAttribute('open');
                }
            });
        });

        var initializeLearningLists = function () {
        Array.prototype.slice.call(document.querySelectorAll('[data-learning-list]')).forEach(function (list) {
            if (list.dataset.learningListReady === 'true') {
                return;
            }
            list.dataset.learningListReady = 'true';
            var surface = list.getAttribute('data-learning-list');
            var table = list.closest('[data-learning-table]');
            var controls = document.querySelector('[data-learning-controls="' + surface + '"]');
            var rows = Array.prototype.slice.call(list.querySelectorAll('[data-learning-row]'));
            if (!table || !controls || !rows.length) {
                return;
            }

            var sortOptions = Array.prototype.slice.call(controls.querySelectorAll('[data-learning-sort-option]'));
            var groupOptions = Array.prototype.slice.call(controls.querySelectorAll('[data-learning-group-option]'));
            var sortToggle = controls.querySelector('[data-learning-sort-toggle]');
            var groupToggle = controls.querySelector('[data-learning-group-toggle]');
            var contextColumns = Array.prototype.slice.call(table.querySelectorAll('[data-learning-context-column]'));
            var rowGroups = rows.map(function (row) {
                return {
                    row: row,
                    detail: row.dataset.learningDetailId ? document.getElementById(row.dataset.learningDetailId) : null
                };
            });
            var itemSingular = surface === 'lessons' ? 'lesson' : surface === 'assessments' ? 'assessment' : 'record';
            var itemPlural = surface === 'lessons' ? 'lessons' : surface === 'assessments' ? 'assessments' : 'records';
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
            var collapsedClassGroupGroups = new Set();

            function oppositeDirection(direction) {
                return direction === 'asc' ? 'desc' : 'asc';
            }

            function datasetKey(prefix, field) {
                return prefix + field.charAt(0).toUpperCase() + field.slice(1);
            }

            function textValue(row, field) {
                return row.dataset[datasetKey('sort', field)] || '';
            }

            function dateValue(row) {
                var value = textValue(row, 'date');
                if (!value) {
                    return 0;
                }
                if (/^-?\d+(\.\d+)?$/.test(value)) {
                    return Number(value);
                }
                var parsed = Date.parse(value);
                return Number.isNaN(parsed) ? 0 : parsed;
            }

            function originalIndex(group) {
                return Number(group.row.dataset.sortIndex || '0');
            }

            function compareRows(field, direction, first, second) {
                var result = field === 'date'
                        ? dateValue(first.row) - dateValue(second.row)
                        : collator.compare(textValue(first.row, field), textValue(second.row, field));
                if (result === 0) {
                    result = originalIndex(first) - originalIndex(second);
                }
                return direction === 'desc' ? -result : result;
            }

            function sortedRowGroups() {
                var sorted = rowGroups.slice();
                if (activeField && activeDirection) {
                    sorted.sort(function (first, second) {
                        return compareRows(activeField, activeDirection, first, second);
                    });
                } else {
                    sorted.sort(function (first, second) {
                        return originalIndex(first) - originalIndex(second);
                    });
                }
                return sorted;
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
                collapsedClassGroupGroups.clear();
            }

            function visibleColumnCount() {
                return Array.prototype.slice.call(table.querySelectorAll('thead th'))
                        .filter(function (column) {
                            return !column.hidden;
                        }).length || 1;
            }

            function updateGroupedLayout() {
                var grouped = activeGroupField !== null;
                table.classList.toggle('is-learning-grouped', grouped);
                table.classList.toggle('is-learning-grouped-by-organization', activeGroupField === 'organization');
                table.classList.toggle('is-learning-grouped-by-course', activeGroupField === 'course');
                table.classList.toggle('is-learning-grouped-by-subject', activeGroupField === 'subject');
                table.classList.toggle('is-learning-grouped-by-class-group', activeGroupField === 'classGroup');
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

            function groupText(row, name, fallback) {
                return row.dataset[datasetKey('group', name)] || fallback;
            }

            function organizationKey(group) {
                return group.row.dataset.groupOrganizationId || group.row.dataset.groupOrganization || 'unknown';
            }

            function unitKey(group) {
                return [organizationKey(group), group.row.dataset.groupOrganicUnitId || 'none'].join('::');
            }

            function courseKey(group) {
                return [unitKey(group), group.row.dataset.groupCourseId || 'none'].join('::');
            }

            function subjectKey(group) {
                return [courseKey(group), group.row.dataset.groupSubjectId || 'none'].join('::');
            }

            function classGroupKey(group) {
                return [subjectKey(group), group.row.dataset.groupClassGroupId || 'none'].join('::');
            }

            function standaloneCourseKey(group) {
                return [group.row.dataset.groupCourseId || 'none', group.row.dataset.groupCourse || ''].join('::');
            }

            function courseSubjectKey(group) {
                return [standaloneCourseKey(group), group.row.dataset.groupSubjectId || 'none'].join('::');
            }

            function courseSubjectClassGroupKey(group) {
                return [courseSubjectKey(group), group.row.dataset.groupClassGroupId || 'none'].join('::');
            }

            function standaloneSubjectKey(group) {
                return [group.row.dataset.groupSubjectId || 'none', group.row.dataset.groupSubject || ''].join('::');
            }

            function subjectClassGroupKey(group) {
                return [standaloneSubjectKey(group), group.row.dataset.groupClassGroupId || 'none'].join('::');
            }

            function standaloneClassGroupKey(group) {
                return [group.row.dataset.groupClassGroupId || 'none', group.row.dataset.groupClassGroup || ''].join('::');
            }

            function courseContextLabel(group) {
                var unit = group.row.dataset.groupOrganicUnit || '';
                var organization = group.row.dataset.groupOrganization || '';
                return unit && organization ? unit + ' | ' + organization : organization;
            }

            function createGroupToggleButton(isExpanded, expandedLabel, collapsedLabel, onToggle) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = 'gape-learning-group-toggle text-18';
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

            function createHeader(kind, bucket, nestedMode) {
                var header = document.createElement('tr');
                var cssKind = kind === 'classGroup' ? 'class-group' : kind;
                var rowClass = 'gape-learning-' + cssKind + '-group-row';
                var headingClass = kind === 'organization' ? 'gape-learning-heading' : 'gape-learning-' + cssKind + '-heading';
                var titleClass = kind === 'organization' ? 'gape-learning-title' : 'gape-learning-' + cssKind + '-title';
                var markerClass = kind === 'organization' ? '' : 'gape-learning-' + cssKind + '-marker';
                var collapseSet = {
                    organization: collapsedOrganizationGroups,
                    unit: collapsedUnitGroups,
                    course: collapsedCourseGroups,
                    subject: collapsedSubjectGroups,
                    classGroup: collapsedClassGroupGroups
                }[kind];
                var labels = {
                    organization: ['Hide organization ' + itemPlural, 'Show organization ' + itemPlural],
                    unit: ['Hide organic unit ' + itemPlural, 'Show organic unit ' + itemPlural],
                    course: ['Hide course ' + itemPlural, 'Show course ' + itemPlural],
                    subject: ['Hide subject ' + itemPlural, 'Show subject ' + itemPlural],
                    classGroup: ['Hide class group ' + itemPlural, 'Show class group ' + itemPlural]
                }[kind];

                header.className = rowClass;
                header.setAttribute('data-learning-group-row', '');

                var cell = document.createElement('td');
                cell.colSpan = visibleColumnCount();
                cell.className = kind === 'organization' ? 'px-20 py-14' : 'px-20 py-10';

                var heading = document.createElement('div');
                heading.className = headingClass;
                if (nestedMode) {
                    heading.classList.add('is-nested-under-' + nestedMode);
                }

                var title = document.createElement('div');
                title.className = titleClass;

                if (kind === 'organization') {
                    var icon = document.createElement('i');
                    icon.className = 'ph ph-buildings text-20 text-main-600';
                    icon.setAttribute('aria-hidden', 'true');
                    title.appendChild(icon);
                } else {
                    var marker = document.createElement('span');
                    marker.className = markerClass;
                    marker.setAttribute('aria-hidden', 'true');
                    title.appendChild(marker);
                }

                var name = document.createElement('span');
                name.className = kind === 'organization' ? 'text-14 fw-semibold text-neutral-700' : 'text-13 fw-semibold text-neutral-700';
                name.textContent = bucket.label;
                title.appendChild(name);

                if (kind === 'organization') {
                    var unitCount = document.createElement('span');
                    unitCount.className = 'gape-learning-chip text-12 fw-medium';
                    unitCount.textContent = countLabel(bucket.units.length, 'department', 'departments');
                    title.appendChild(unitCount);
                }
                if (bucket.context) {
                    var context = document.createElement('span');
                    context.className = 'text-12 text-neutral-500';
                    context.textContent = bucket.context;
                    title.appendChild(context);
                }

                var count = document.createElement('span');
                count.className = 'text-12 text-neutral-500';
                count.textContent = countLabel(bucket.itemCount || bucket.items.length, itemSingular, itemPlural);

                var actions = document.createElement('div');
                actions.className = 'd-flex align-items-center gap-10';

                var isExpanded = !collapseSet.has(bucket.key);
                var toggle = createGroupToggleButton(isExpanded, labels[0], labels[1], function () {
                    if (collapseSet.has(bucket.key)) {
                        collapseSet.delete(bucket.key);
                    } else {
                        collapseSet.add(bucket.key);
                    }
                });

                actions.appendChild(count);
                actions.appendChild(toggle);
                heading.appendChild(title);
                heading.appendChild(actions);
                cell.appendChild(heading);
                header.appendChild(cell);
                return header;
            }

            function removeHeaders() {
                Array.prototype.slice.call(list.querySelectorAll('[data-learning-group-row]')).forEach(function (header) {
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

            function sortBuckets(buckets) {
                buckets.sort(function (first, second) {
                    return compareGroupLabels(activeGroupDirection, first, second);
                });
            }

            function renderOrganizationGroups(sorted) {
                var organizations = [];
                var organizationByKey = new Map();
                sorted.forEach(function (group) {
                    var organizationBucketKey = organizationKey(group);
                    var unitBucketKey = unitKey(group);
                    var courseBucketKey = courseKey(group);
                    var subjectBucketKey = subjectKey(group);
                    var classGroupBucketKey = classGroupKey(group);
                    if (!organizationByKey.has(organizationBucketKey)) {
                        organizationByKey.set(organizationBucketKey, {
                            key: organizationBucketKey,
                            label: groupText(group.row, 'organization', 'Unknown organization'),
                            units: [],
                            unitByKey: new Map(),
                            itemCount: 0
                        });
                        organizations.push(organizationByKey.get(organizationBucketKey));
                    }
                    var organizationBucket = organizationByKey.get(organizationBucketKey);
                    if (!organizationBucket.unitByKey.has(unitBucketKey)) {
                        organizationBucket.unitByKey.set(unitBucketKey, {
                            key: unitBucketKey,
                            label: groupText(group.row, 'organicUnit', 'No organic unit'),
                            courses: [],
                            courseByKey: new Map(),
                            itemCount: 0
                        });
                        organizationBucket.units.push(organizationBucket.unitByKey.get(unitBucketKey));
                    }
                    var unitBucket = organizationBucket.unitByKey.get(unitBucketKey);
                    if (!unitBucket.courseByKey.has(courseBucketKey)) {
                        unitBucket.courseByKey.set(courseBucketKey, {
                            key: courseBucketKey,
                            label: groupText(group.row, 'course', 'No course'),
                            subjects: [],
                            subjectByKey: new Map(),
                            itemCount: 0
                        });
                        unitBucket.courses.push(unitBucket.courseByKey.get(courseBucketKey));
                    }
                    var courseBucket = unitBucket.courseByKey.get(courseBucketKey);
                    if (!courseBucket.subjectByKey.has(subjectBucketKey)) {
                        courseBucket.subjectByKey.set(subjectBucketKey, {
                            key: subjectBucketKey,
                            label: groupText(group.row, 'subject', 'No subject'),
                            classGroups: [],
                            classGroupByKey: new Map(),
                            itemCount: 0
                        });
                        courseBucket.subjects.push(courseBucket.subjectByKey.get(subjectBucketKey));
                    }
                    var subjectBucket = courseBucket.subjectByKey.get(subjectBucketKey);
                    if (!subjectBucket.classGroupByKey.has(classGroupBucketKey)) {
                        subjectBucket.classGroupByKey.set(classGroupBucketKey, {
                            key: classGroupBucketKey,
                            label: groupText(group.row, 'classGroup', 'No class group'),
                            items: []
                        });
                        subjectBucket.classGroups.push(subjectBucket.classGroupByKey.get(classGroupBucketKey));
                    }
                    subjectBucket.classGroupByKey.get(classGroupBucketKey).items.push(group);
                    subjectBucket.itemCount += 1;
                    courseBucket.itemCount += 1;
                    unitBucket.itemCount += 1;
                    organizationBucket.itemCount += 1;
                });

                sortBuckets(organizations);
                organizations.forEach(function (organizationBucket) {
                    sortBuckets(organizationBucket.units);
                    list.appendChild(createHeader('organization', organizationBucket));
                    if (collapsedOrganizationGroups.has(organizationBucket.key)) {
                        return;
                    }
                    organizationBucket.units.forEach(function (unitBucket) {
                        sortBuckets(unitBucket.courses);
                        list.appendChild(createHeader('unit', unitBucket));
                        if (collapsedUnitGroups.has(unitBucket.key)) {
                            return;
                        }
                        unitBucket.courses.forEach(function (courseBucket) {
                            sortBuckets(courseBucket.subjects);
                            list.appendChild(createHeader('course', courseBucket, 'organization'));
                            if (collapsedCourseGroups.has(courseBucket.key)) {
                                return;
                            }
                            courseBucket.subjects.forEach(function (subjectBucket) {
                                sortBuckets(subjectBucket.classGroups);
                                list.appendChild(createHeader('subject', subjectBucket, 'organization'));
                                if (collapsedSubjectGroups.has(subjectBucket.key)) {
                                    return;
                                }
                                subjectBucket.classGroups.forEach(function (classGroupBucket) {
                                    list.appendChild(createHeader('classGroup', classGroupBucket, 'organization'));
                                    if (collapsedClassGroupGroups.has(classGroupBucket.key)) {
                                        return;
                                    }
                                    classGroupBucket.items.forEach(appendGroup);
                                });
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
                    var classGroupBucketKey = courseSubjectClassGroupKey(group);
                    if (!courseByKey.has(bucketKey)) {
                        courseByKey.set(bucketKey, {
                            key: bucketKey,
                            label: groupText(group.row, 'course', 'No course'),
                            context: courseContextLabel(group),
                            subjects: [],
                            subjectByKey: new Map(),
                            itemCount: 0
                        });
                        courses.push(courseByKey.get(bucketKey));
                    }
                    var courseBucket = courseByKey.get(bucketKey);
                    if (!courseBucket.subjectByKey.has(subjectBucketKey)) {
                        courseBucket.subjectByKey.set(subjectBucketKey, {
                            key: subjectBucketKey,
                            label: groupText(group.row, 'subject', 'No subject'),
                            classGroups: [],
                            classGroupByKey: new Map(),
                            itemCount: 0
                        });
                        courseBucket.subjects.push(courseBucket.subjectByKey.get(subjectBucketKey));
                    }
                    var subjectBucket = courseBucket.subjectByKey.get(subjectBucketKey);
                    if (!subjectBucket.classGroupByKey.has(classGroupBucketKey)) {
                        subjectBucket.classGroupByKey.set(classGroupBucketKey, {
                            key: classGroupBucketKey,
                            label: groupText(group.row, 'classGroup', 'No class group'),
                            items: []
                        });
                        subjectBucket.classGroups.push(subjectBucket.classGroupByKey.get(classGroupBucketKey));
                    }
                    subjectBucket.classGroupByKey.get(classGroupBucketKey).items.push(group);
                    subjectBucket.itemCount += 1;
                    courseBucket.itemCount += 1;
                });

                sortBuckets(courses);
                courses.forEach(function (courseBucket) {
                    sortBuckets(courseBucket.subjects);
                    list.appendChild(createHeader('course', courseBucket, 'course'));
                    if (collapsedCourseGroups.has(courseBucket.key)) {
                        return;
                    }
                    courseBucket.subjects.forEach(function (subjectBucket) {
                        sortBuckets(subjectBucket.classGroups);
                        list.appendChild(createHeader('subject', subjectBucket, 'course'));
                        if (collapsedSubjectGroups.has(subjectBucket.key)) {
                            return;
                        }
                        subjectBucket.classGroups.forEach(function (classGroupBucket) {
                            list.appendChild(createHeader('classGroup', classGroupBucket, 'course'));
                            if (collapsedClassGroupGroups.has(classGroupBucket.key)) {
                                return;
                            }
                            classGroupBucket.items.forEach(appendGroup);
                        });
                    });
                });
            }

            function renderSubjectGroups(sorted) {
                var subjects = [];
                var subjectByKey = new Map();
                sorted.forEach(function (group) {
                    var bucketKey = standaloneSubjectKey(group);
                    var classGroupBucketKey = subjectClassGroupKey(group);
                    if (!subjectByKey.has(bucketKey)) {
                        subjectByKey.set(bucketKey, {
                            key: bucketKey,
                            label: groupText(group.row, 'subject', 'No subject'),
                            classGroups: [],
                            classGroupByKey: new Map(),
                            itemCount: 0
                        });
                        subjects.push(subjectByKey.get(bucketKey));
                    }
                    var subjectBucket = subjectByKey.get(bucketKey);
                    if (!subjectBucket.classGroupByKey.has(classGroupBucketKey)) {
                        subjectBucket.classGroupByKey.set(classGroupBucketKey, {
                            key: classGroupBucketKey,
                            label: groupText(group.row, 'classGroup', 'No class group'),
                            items: []
                        });
                        subjectBucket.classGroups.push(subjectBucket.classGroupByKey.get(classGroupBucketKey));
                    }
                    subjectBucket.classGroupByKey.get(classGroupBucketKey).items.push(group);
                    subjectBucket.itemCount += 1;
                });

                sortBuckets(subjects);
                subjects.forEach(function (subjectBucket) {
                    sortBuckets(subjectBucket.classGroups);
                    list.appendChild(createHeader('subject', subjectBucket, 'subject'));
                    if (collapsedSubjectGroups.has(subjectBucket.key)) {
                        return;
                    }
                    subjectBucket.classGroups.forEach(function (classGroupBucket) {
                        list.appendChild(createHeader('classGroup', classGroupBucket, 'subject'));
                        if (collapsedClassGroupGroups.has(classGroupBucket.key)) {
                            return;
                        }
                        classGroupBucket.items.forEach(appendGroup);
                    });
                });
            }

            function renderClassGroupGroups(sorted) {
                var classGroups = [];
                var classGroupByKey = new Map();
                sorted.forEach(function (group) {
                    var bucketKey = standaloneClassGroupKey(group);
                    if (!classGroupByKey.has(bucketKey)) {
                        classGroupByKey.set(bucketKey, {
                            key: bucketKey,
                            label: groupText(group.row, 'classGroup', 'No class group'),
                            context: [group.row.dataset.groupSubject, group.row.dataset.groupCourse].filter(Boolean).join(' | '),
                            items: []
                        });
                        classGroups.push(classGroupByKey.get(bucketKey));
                    }
                    classGroupByKey.get(bucketKey).items.push(group);
                });

                sortBuckets(classGroups);
                classGroups.forEach(function (classGroupBucket) {
                    list.appendChild(createHeader('classGroup', classGroupBucket, 'classGroup'));
                    if (collapsedClassGroupGroups.has(classGroupBucket.key)) {
                        return;
                    }
                    classGroupBucket.items.forEach(appendGroup);
                });
            }

            function renderRows() {
                var sorted = sortedRowGroups();
                removeHeaders();
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
                if (activeGroupField === 'classGroup') {
                    renderClassGroupGroups(sorted);
                    return;
                }
                sorted.forEach(appendGroup);
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
                sortToggle.classList.toggle('is-active', activeField !== null);
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
                groupToggle.classList.toggle('is-active', activeGroupField !== null);
            }

            function closeDropdown(toggle) {
                if (window.bootstrap && window.bootstrap.Dropdown) {
                    window.bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
                }
            }

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
        });
        };
        window.GapeLessonListControls = { initialize: initializeLearningLists };
        initializeLearningLists();

        var lessonAssessmentTabs = Array.prototype.slice.call(document.querySelectorAll('[data-la-tab]'));
        var lessonAssessmentPanels = Array.prototype.slice.call(document.querySelectorAll('[data-la-panel]'));
        var initialLessonAssessmentPanel = '<c:out value="${initialLessonAssessmentPanel}"/>';
        var lessonAssessmentHashValue = function () {
            return (window.location.hash || '').replace(/^#/, '');
        };
        var panelFromLessonAssessmentHash = function (name) {
            var hasEventPanels = !!document.querySelector('[data-la-panel="event-list"]');
            if (!name) {
                return initialLessonAssessmentPanel || (hasEventPanels ? 'event-list' : 'lessons');
            }
            if (hasEventPanels) {
                if (name === 'event-calendar' || name === 'calendar') {
                    return 'event-calendar';
                }
                if (name === 'event-list' || name === 'list' || name === 'events') {
                    return 'event-list';
                }
                return 'event-list';
            }
            if (name === 'assessments' || name.indexOf('assessment') === 0) {
                return 'assessments';
            }
            if (name === 'attendance'
                    || name.indexOf('attendanceDetail') === 0
                    || name.indexOf('attendanceAssessmentDetail') === 0
                    || name.indexOf('attendanceSettings') === 0) {
                return 'attendance';
            }
            return 'lessons';
        };
        var activateLessonAssessmentPanel = function (name, updateHash) {
            if (!lessonAssessmentTabs.length || !lessonAssessmentPanels.length) {
                return;
            }
            var panelName = panelFromLessonAssessmentHash(name);
            lessonAssessmentTabs.forEach(function (tab) {
                var active = tab.getAttribute('data-la-tab') === panelName;
                tab.classList.toggle('is-active', active);
                tab.setAttribute('aria-selected', active ? 'true' : 'false');
            });
            lessonAssessmentPanels.forEach(function (panel) {
                if (panel.getAttribute('data-la-panel') === 'events') {
                    return;
                }
                panel.hidden = panel.getAttribute('data-la-panel') !== panelName;
            });
            if (updateHash && window.history && window.history.replaceState) {
                window.history.replaceState(null, '', window.location.pathname + window.location.search + '#' + panelName);
            }
        };
        var showLessonAssessmentHashModal = function (attempt) {
            var hash = lessonAssessmentHashValue();
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
                    showLessonAssessmentHashModal((attempt || 0) + 1);
                }
            }, 50);
        };
        lessonAssessmentTabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                activateLessonAssessmentPanel(tab.getAttribute('data-la-tab'), true);
            });
        });
        if (lessonAssessmentTabs.length) {
            activateLessonAssessmentPanel(lessonAssessmentHashValue() || initialLessonAssessmentPanel, false);
            showLessonAssessmentHashModal();
            window.addEventListener('hashchange', function () {
                activateLessonAssessmentPanel(lessonAssessmentHashValue(), false);
                showLessonAssessmentHashModal();
            });
        }

        var calendarRoot = document.querySelector('[data-event-calendar-root]');
        if (calendarRoot) {
            var calendarGrid = calendarRoot.querySelector('[data-event-calendar-grid]');
            var calendarTitle = calendarRoot.querySelector('[data-event-calendar-title]');
            var calendarMonthCount = calendarRoot.querySelector('[data-event-calendar-month-count]');
            var calendarTotalCount = calendarRoot.querySelector('[data-event-calendar-total-count]');
            var calendarCardSummary = document.querySelector('[data-event-calendar-card-summary]');
            var calendarModalElement = document.getElementById('eventCalendarEventsModal');
            var calendarModalTitle = calendarModalElement ? calendarModalElement.querySelector('[data-event-calendar-modal-title]') : null;
            var calendarModalSubtitle = calendarModalElement ? calendarModalElement.querySelector('[data-event-calendar-modal-subtitle]') : null;
            var calendarModalBody = calendarModalElement ? calendarModalElement.querySelector('[data-event-calendar-modal-body]') : null;
            var eventSources = Array.prototype.slice.call(calendarRoot.querySelectorAll('[data-event-calendar-source-item]'));
            var calendarEvents = eventSources.map(function (item, index) {
                var timeLabel = item.getAttribute('data-event-time') || '';
                return {
                    index: index,
                    date: item.getAttribute('data-event-date') || '',
                    time: window.GapeDateTimeFormat
                            ? window.GapeDateTimeFormat.formatDisplayText(timeLabel)
                            : timeLabel,
                    dateLabel: item.getAttribute('data-event-date-label') || '',
                    title: item.getAttribute('data-event-title') || '',
                    category: item.getAttribute('data-event-category') || '',
                    state: item.getAttribute('data-event-state') || '',
                    context: item.getAttribute('data-event-context') || '',
                    href: item.getAttribute('data-event-href') || '#',
                    iconClass: item.getAttribute('data-event-icon-class') || 'ph ph-calendar',
                    badgeClass: item.getAttribute('data-event-badge-class') || 'bg-main-50 text-main-600',
                    stateBadgeClass: item.getAttribute('data-event-state-badge-class') || 'bg-neutral-50 text-neutral-600'
                };
            });
            var initialMonthValue = calendarRoot.getAttribute('data-current-month') || '';
            var initialMonthParts = initialMonthValue.split('-');
            var activeCalendarDate = initialMonthParts.length === 2
                    ? new Date(Number(initialMonthParts[0]), Number(initialMonthParts[1]) - 1, 1)
                    : new Date();
            activeCalendarDate.setDate(1);

            var pad = function (value) {
                return String(value).padStart(2, '0');
            };
            var monthKey = function (date) {
                return date.getFullYear() + '-' + pad(date.getMonth() + 1);
            };
            var dayKey = function (date) {
                return monthKey(date) + '-' + pad(date.getDate());
            };
            var addDays = function (date, days) {
                var next = new Date(date.getFullYear(), date.getMonth(), date.getDate());
                next.setDate(next.getDate() + days);
                return next;
            };
            var monthTitle = function (date) {
                var monthReference = new Date(Date.UTC(date.getFullYear(), date.getMonth(), 15, 12));
                return new Intl.DateTimeFormat('pt-PT', {
                    timeZone: 'Europe/Lisbon',
                    month: 'long',
                    year: 'numeric'
                }).format(monthReference);
            };
            var fullDateTitle = function (dateValue) {
                if (window.GapeDateTimeFormat) {
                    return window.GapeDateTimeFormat.formatDisplayText(dateValue);
                }
                var parts = dateValue.split('-').map(Number);
                if (parts.length !== 3) {
                    return dateValue;
                }
                return pad(parts[2]) + '-' + pad(parts[1]) + '-' + parts[0];
            };
            var eventsForDate = function (dateValue) {
                return calendarEvents.filter(function (eventItem) {
                    return eventItem.date === dateValue;
                });
            };
            var eventsForMonth = function (date) {
                var key = monthKey(date);
                return calendarEvents.filter(function (eventItem) {
                    return eventItem.date.indexOf(key) === 0;
                });
            };
            var createEventLink = function (eventItem) {
                var link = document.createElement('a');
                link.href = eventItem.href;
                link.className = 'gape-calendar-event';
                link.title = eventItem.title;

                var iconWrap = document.createElement('span');
                iconWrap.className = eventItem.badgeClass + ' gape-calendar-event__icon text-13';
                var icon = document.createElement('i');
                icon.className = eventItem.iconClass;
                iconWrap.appendChild(icon);

                var textWrap = document.createElement('span');
                textWrap.className = 'min-w-0';
                var title = document.createElement('span');
                title.className = 'gape-calendar-event__title';
                title.textContent = eventItem.title;
                var meta = document.createElement('span');
                meta.className = 'gape-calendar-event__meta';
                meta.textContent = eventItem.category + ' | ' + eventItem.state;

                textWrap.appendChild(title);
                textWrap.appendChild(meta);
                link.appendChild(iconWrap);
                link.appendChild(textWrap);
                return link;
            };
            var createShowAllButton = function (dateValue, count) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = 'gape-calendar-event gape-calendar-show-all';
                button.setAttribute('data-event-calendar-show-all', '');
                button.setAttribute('data-event-date', dateValue);

                var iconWrap = document.createElement('span');
                iconWrap.className = 'bg-main-50 text-main-600 gape-calendar-event__icon text-13';
                var icon = document.createElement('i');
                icon.className = 'ph ph-list-bullets';
                iconWrap.appendChild(icon);

                var textWrap = document.createElement('span');
                textWrap.className = 'min-w-0';
                var title = document.createElement('span');
                title.className = 'gape-calendar-event__title';
                title.textContent = 'Show all';
                var meta = document.createElement('span');
                meta.className = 'gape-calendar-event__meta';
                meta.textContent = count + (count === 1 ? ' event' : ' events');

                textWrap.appendChild(title);
                textWrap.appendChild(meta);
                button.appendChild(iconWrap);
                button.appendChild(textWrap);
                return button;
            };
            var createModalEventItem = function (eventItem) {
                var link = document.createElement('a');
                link.href = eventItem.href;
                link.className = 'gape-calendar-modal-item text-neutral-700 hover-text-main-600';

                var iconWrap = document.createElement('span');
                iconWrap.className = eventItem.badgeClass + ' w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0';
                var icon = document.createElement('i');
                icon.className = eventItem.iconClass;
                iconWrap.appendChild(icon);

                var content = document.createElement('span');
                content.className = 'min-w-0 flex-grow-1';
                var title = document.createElement('span');
                title.className = 'fw-medium text-14 text-neutral-700 d-block';
                title.textContent = eventItem.title;
                var meta = document.createElement('span');
                meta.className = 'd-block text-12 text-neutral-500 mt-2';
                meta.textContent = eventItem.time + ' | ' + eventItem.category + ' | ' + eventItem.context;

                var state = document.createElement('span');
                state.className = eventItem.stateBadgeClass + ' px-12 py-7 border-neutral-30 border rounded-pill text-12 flex-shrink-0';
                state.textContent = eventItem.state;

                content.appendChild(title);
                content.appendChild(meta);
                link.appendChild(iconWrap);
                link.appendChild(content);
                link.appendChild(state);
                return link;
            };
            var showCalendarEventsModal = function (dateValue, dayEvents) {
                if (!calendarModalElement || !calendarModalTitle || !calendarModalBody) {
                    return;
                }
                calendarModalTitle.textContent = 'Events on ' + fullDateTitle(dateValue);
                if (calendarModalSubtitle) {
                    calendarModalSubtitle.textContent = dayEvents.length + (dayEvents.length === 1 ? ' event' : ' events') + ' in this date.';
                }
                calendarModalBody.innerHTML = '';
                dayEvents.forEach(function (eventItem) {
                    calendarModalBody.appendChild(createModalEventItem(eventItem));
                });
                if (window.bootstrap && window.bootstrap.Modal) {
                    window.bootstrap.Modal.getOrCreateInstance(calendarModalElement).show();
                }
            };
            var renderEventCalendar = function () {
                if (!calendarGrid) {
                    return;
                }
                Array.prototype.slice.call(calendarGrid.querySelectorAll('.gape-calendar-day')).forEach(function (day) {
                    day.remove();
                });
                var monthStart = new Date(activeCalendarDate.getFullYear(), activeCalendarDate.getMonth(), 1);
                var monthEnd = new Date(activeCalendarDate.getFullYear(), activeCalendarDate.getMonth() + 1, 0);
                var startOffset = (monthStart.getDay() + 6) % 7;
                var endOffset = 6 - ((monthEnd.getDay() + 6) % 7);
                var cursor = addDays(monthStart, -startOffset);
                var gridEnd = addDays(monthEnd, endOffset);
                var visibleMonthEvents = eventsForMonth(activeCalendarDate);
                var cells = 0;

                if (calendarTitle) {
                    calendarTitle.textContent = monthTitle(activeCalendarDate);
                }
                if (calendarMonthCount) {
                    calendarMonthCount.textContent = visibleMonthEvents.length;
                }
                if (calendarTotalCount) {
                    calendarTotalCount.textContent = calendarEvents.length;
                }
                if (calendarCardSummary) {
                    calendarCardSummary.textContent = visibleMonthEvents.length + ' events in ' + monthTitle(activeCalendarDate);
                }

                while (cursor <= gridEnd || cells < 35) {
                    var current = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate());
                    var dateValue = dayKey(current);
                    var dayEvents = eventsForDate(dateValue);
                    var currentMonth = current.getMonth() === activeCalendarDate.getMonth()
                            && current.getFullYear() === activeCalendarDate.getFullYear();
                    var todayValue = window.GapeDateTimeFormat
                            ? window.GapeDateTimeFormat.currentLisbonMinuteValue().substring(0, 10)
                            : dayKey(new Date());
                    var today = dateValue === todayValue;
                    var cell = document.createElement('div');
                    cell.className = 'gape-calendar-day' + (currentMonth ? '' : ' is-muted') + (today ? ' is-today' : '');
                    cell.setAttribute('role', 'gridcell');

                    var number = document.createElement('div');
                    number.className = 'gape-calendar-day__number';
                    var numberText = document.createElement('span');
                    numberText.textContent = current.getDate();
                    number.appendChild(numberText);
                    if (dayEvents.length > 0) {
                        var count = document.createElement('span');
                        count.className = 'gape-calendar-count';
                        count.textContent = dayEvents.length;
                        number.appendChild(count);
                    }
                    cell.appendChild(number);

                    var eventsWrap = document.createElement('div');
                    eventsWrap.className = 'gape-calendar-events';
                    if (dayEvents.length > 0) {
                        eventsWrap.appendChild(createEventLink(dayEvents[0]));
                        if (dayEvents.length > 1) {
                            eventsWrap.appendChild(createShowAllButton(dateValue, dayEvents.length));
                        }
                    }
                    cell.appendChild(eventsWrap);
                    calendarGrid.appendChild(cell);
                    cursor = addDays(cursor, 1);
                    cells += 1;
                }
            };

            calendarRoot.addEventListener('click', function (event) {
                var moveButton = event.target.closest('[data-event-calendar-step]');
                if (moveButton) {
                    event.preventDefault();
                    var step = Number(moveButton.getAttribute('data-event-calendar-step') || '0');
                    var unit = moveButton.getAttribute('data-event-calendar-unit') || 'month';
                    if (unit === 'year') {
                        activeCalendarDate = new Date(activeCalendarDate.getFullYear() + step, activeCalendarDate.getMonth(), 1);
                    } else {
                        activeCalendarDate = new Date(activeCalendarDate.getFullYear(), activeCalendarDate.getMonth() + step, 1);
                    }
                    renderEventCalendar();
                    if (window.history && window.history.replaceState) {
                        var nextUrl = new URL(window.location.href);
                        nextUrl.searchParams.set('month', monthKey(activeCalendarDate));
                        nextUrl.hash = 'event-calendar';
                        window.history.replaceState(null, '', nextUrl.toString());
                    }
                    return;
                }

                var showAllButton = event.target.closest('[data-event-calendar-show-all]');
                if (showAllButton) {
                    event.preventDefault();
                    var selectedDate = showAllButton.getAttribute('data-event-date');
                    showCalendarEventsModal(selectedDate, eventsForDate(selectedDate));
                }
            });
            renderEventCalendar();
        }

        var status = document.getElementById('attendance-status');
        var checkIn = document.getElementById('attendance-check-in');
        var checkOut = document.getElementById('attendance-check-out');
        if (status && checkIn && checkOut) {
            var syncAttendanceTimestamps = function () {
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
            status.addEventListener('change', syncAttendanceTimestamps);
            syncAttendanceTimestamps();
        }

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
            Array.prototype.slice.call(document.querySelectorAll('[data-attendance-detail-state="' + activityId + '"]'))
                    .forEach(function (stateBadge) {
                        stateBadge.className = data.badgeClass + ' px-12 py-7 border-neutral-30 border rounded-pill text-12';
                        stateBadge.textContent = data.stateLabel;
                        stateBadge.setAttribute('data-state-value', data.stateValue);
                    });
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
                        var modalElement = button.closest('.modal');
                        closeAttendanceModal(modalElement);
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

        var eventType = document.getElementById('schedule-event-type');
        var lessonReference = document.querySelector('[data-schedule-reference="lesson"]');
        var assessmentReference = document.querySelector('[data-schedule-reference="assessment"]');
        var lessonInput = document.getElementById('schedule-event-lesson-id');
        var assessmentInput = document.getElementById('schedule-event-assessment-id');
        var syncReferences = function () {
            if (!eventType) {
                return;
            }
            var lessonSelected = eventType.value === 'lesson';
            var assessmentSelected = eventType.value === 'assessment';
            if (lessonReference) {
                lessonReference.hidden = !lessonSelected;
            }
            if (assessmentReference) {
                assessmentReference.hidden = !assessmentSelected;
            }
            if (lessonInput) {
                lessonInput.required = lessonSelected;
                if (!lessonSelected) {
                    lessonInput.value = '';
                }
            }
            if (assessmentInput) {
                assessmentInput.required = assessmentSelected;
                if (!assessmentSelected) {
                    assessmentInput.value = '';
                }
            }
        };
        if (eventType) {
            eventType.addEventListener('change', syncReferences);
            syncReferences();
        }
    }());
</script>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-learning-management-list.js?v=20260715-learning-management-3"></script>
<script src="${pageContext.request.contextPath}/assets/js/gape-deferred-management-list.js?v=20260715-deferred-management-1"></script>
</body>
</html>
