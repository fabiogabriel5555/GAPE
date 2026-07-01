<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Assessment Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .ad-shell {
            --ad-primary: #2563eb;
            --ad-primary-dark: #1d4ed8;
            --ad-border: #e6edf0;
            --ad-muted: #64748b;
            --ad-ink: #172033;
            max-width: 100%;
            min-width: 0;
        }

        .dashbord-body {
            max-width: 100%;
            min-width: 0;
        }

        .ad-surface {
            background: #fff;
            border: 1px solid var(--ad-border);
            border-radius: 8px;
            max-width: 100%;
            min-width: 0;
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
        }

        .ad-hero {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.13), rgba(14, 165, 233, 0.07)), #fff;
        }

        .ad-hero h2,
        .ad-hero p,
        .ad-hero-meta {
            max-width: 100%;
            min-width: 0;
            overflow-wrap: anywhere;
            white-space: normal;
        }

        .ad-hero h2 {
            flex: 1 1 240px;
        }

        .ad-hero .flex-wrap {
            max-width: 100%;
            min-width: 0;
        }

        .ad-hero-meta > span {
            min-width: 0;
            overflow-wrap: anywhere;
            white-space: normal;
        }

        .ad-hero > .d-flex > .d-flex:first-child {
            flex: 1 1 360px;
            max-width: 100%;
            min-width: 0;
        }

        .ad-hero > .d-flex > .d-flex:first-child > .min-w-0 {
            flex: 1 1 0;
            max-width: 100%;
            min-width: 0;
        }

        .ad-mode-grid {
            display: grid;
            align-items: stretch;
            gap: 14px;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            max-width: 100%;
            min-width: 0;
        }

        .ad-mode-card {
            background: #fff;
            border: 1px solid var(--ad-border);
            border-radius: 8px;
            color: var(--ad-ink);
            cursor: pointer;
            height: 100%;
            min-height: 126px;
            min-width: 0;
            padding: 16px;
            text-align: left;
            transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
            white-space: normal !important;
            width: 100%;
        }

        .ad-mode-card > .d-flex {
            align-items: stretch !important;
            height: 100%;
            max-width: 100%;
            min-width: 0;
        }

        .ad-mode-content {
            display: flex;
            flex-direction: column;
            min-height: 100%;
        }

        .ad-mode-description {
            min-height: 40px;
        }

        .ad-mode-metric {
            margin-top: auto;
        }

        .ad-mode-card .min-w-0 {
            max-width: 100%;
            min-width: 0;
        }

        .ad-mode-card .text-13,
        .ad-mode-card .text-14 {
            line-height: 1.4;
            white-space: normal !important;
        }

        .ad-mode-card .flex-wrap > span {
            min-width: 0;
            overflow-wrap: anywhere;
            white-space: normal !important;
        }

        .ad-mode-card.is-active {
            background: linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(14, 165, 233, 0.06)), #f4f8ff;
            border-color: rgba(37, 99, 235, 0.7);
            box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12);
            transform: translateY(-1px);
        }

        .ad-mode-card.is-active .ad-mode-icon {
            background: var(--ad-primary) !important;
            color: #fff !important;
        }

        .ad-mode-icon {
            align-items: center;
            border-radius: 8px;
            display: inline-flex;
            flex-shrink: 0;
            height: 44px;
            justify-content: center;
            width: 44px;
        }

        .ad-tab-panel[hidden] {
            display: none !important;
        }

        .ad-subtab-list {
            align-items: center;
            background: #f8fafc;
            border: 1px solid var(--ad-border);
            border-radius: 8px;
            display: inline-flex;
            gap: 4px;
            padding: 4px;
        }

        .ad-subtab-button {
            align-items: center;
            background: transparent;
            border: 0;
            border-radius: 6px;
            color: var(--ad-muted);
            cursor: pointer;
            display: inline-flex;
            font-size: 14px;
            font-weight: 600;
            gap: 6px;
            min-height: 38px;
            padding: 8px 12px;
        }

        .ad-subtab-button.is-active {
            background: #fff;
            box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
            color: var(--ad-primary-dark);
        }

        .ad-enrollment-subpanel[hidden] {
            display: none !important;
        }

        .ad-tab-panel,
        .ad-enrollment-policy-form,
        .ad-enrollment-create-form,
        .ad-enrollment-create-form .row,
        .ad-enrollment-create-form [class*="col-"] {
            max-width: 100%;
            min-width: 0;
        }

        .ad-enrollment-create-form .form-control,
        .ad-enrollment-create-form .form-select,
        .ad-enrollment-create-form .select2-container {
            max-width: 100%;
            min-width: 0;
        }

        .ad-enrollment-create-grid {
            align-items: start;
            display: grid;
            gap: 12px;
            grid-template-columns: minmax(0, 1fr) 48px;
        }

        .ad-enrollment-icon-button {
            align-items: center;
            border: 1px solid transparent;
            border-radius: 8px;
            cursor: pointer;
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 20px;
            height: 42px;
            justify-content: center;
            line-height: 1;
            padding: 0;
            transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
            width: 42px;
        }

        .ad-enrollment-icon-button:hover {
            transform: translateY(-1px);
        }

        .ad-enrollment-icon-button--primary {
            background: var(--ad-primary);
            border-color: var(--ad-primary);
            color: #fff;
        }

        .ad-enrollment-icon-button--primary:hover {
            background: var(--ad-primary-dark);
            border-color: var(--ad-primary-dark);
            color: #fff;
        }

        .ad-enrollment-icon-button--success {
            background: #ecfdf3;
            border-color: #bbf7d0;
            color: #16a34a;
        }

        .ad-enrollment-icon-button--success:hover {
            background: #dcfce7;
            color: #15803d;
        }

        .ad-enrollment-icon-button--danger {
            background: #fef2f2;
            border-color: #fecaca;
            color: #dc2626;
        }

        .ad-enrollment-icon-button--danger:hover {
            background: #fee2e2;
            color: #b91c1c;
        }

        .ad-enrollment-icon-button--delete {
            background: #fff1f2;
            border-color: #fecdd3;
            color: #be123c;
        }

        .ad-enrollment-icon-button--delete:hover {
            background: #ffe4e6;
            color: #9f1239;
        }

        .ad-enrollment-icon-button--warning {
            background: #fff7ed;
            border-color: #fed7aa;
            color: #ea580c;
        }

        .ad-enrollment-icon-button--warning:hover {
            background: #ffedd5;
            color: #c2410c;
        }

        .ad-enrollment-icon-button--neutral {
            background: #fff;
            border-color: var(--ad-border);
            color: var(--ad-ink);
        }

        .ad-enrollment-icon-button--neutral:hover {
            border-color: rgba(37, 99, 235, 0.45);
            color: var(--ad-primary-dark);
        }

        .ad-enrollment-create-submit {
            align-self: start;
            height: 52px;
            margin-top: 31px;
            width: 52px;
        }

        .ad-correction-dialog {
            max-width: min(1120px, calc(100vw - 32px));
        }

        .ad-correction-modal {
            --ad-primary: #2563eb;
            --ad-primary-dark: #1d4ed8;
            --ad-border: #e6edf0;
            --ad-muted: #64748b;
            --ad-ink: #172033;
        }

        .ad-correction-modal .modal-content {
            background: #fff;
            border: 0;
            border-radius: 8px;
            overflow: hidden;
        }

        .ad-correction-modal .modal-header {
            align-items: flex-start;
            background: #fff;
            border-bottom: 1px solid var(--ad-border);
            padding: 20px 24px;
        }

        .ad-correction-modal .modal-body {
            background: #fff;
            padding: 22px 24px;
        }

        .ad-correction-modal .modal-footer {
            background: #fff;
            border-top: 1px solid var(--ad-border);
            padding: 16px 24px;
        }

        .ad-correction-footer-actions {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
            justify-content: flex-end;
            width: 100%;
        }

        .ad-correction-footer-actions .ad-outline-button,
        .ad-correction-footer-actions .ad-primary-button {
            min-height: 42px;
        }

        .ad-correction-summary-grid {
            display: grid;
            gap: 12px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            margin-bottom: 18px;
        }

        .ad-correction-summary-card {
            background: #fff;
            border: 1px solid var(--ad-border);
            border-radius: 8px;
            min-width: 0;
            padding: 14px;
        }

        .ad-correction-summary-card span,
        .ad-correction-summary-card strong {
            display: block;
            min-width: 0;
            overflow-wrap: anywhere;
        }

        .ad-correction-question-list {
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .ad-correction-question-card {
            background: #fff;
            border: 1px solid #d5e0e7;
            border-radius: 8px;
            box-shadow: none;
            padding: 20px;
        }

        .ad-correction-question-header {
            align-items: center;
            display: flex;
            gap: 16px;
            justify-content: space-between;
            margin-bottom: 16px;
        }

        .ad-correction-question-heading {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 10px 18px;
            min-width: 0;
        }

        .ad-correction-question-label {
            color: #009688;
            display: inline-flex;
            font-size: 15px;
            font-weight: 700;
            line-height: 1.2;
        }

        .ad-correction-question-meta {
            color: #64748b;
            font-size: 15px;
            line-height: 1.2;
        }

        .ad-correction-required-pill {
            align-items: center;
            border: 1px solid #009688;
            border-radius: 999px;
            color: #00796f;
            display: inline-flex;
            font-size: 12px;
            font-weight: 700;
            line-height: 1;
            min-height: 24px;
            padding: 5px 13px;
        }

        .ad-correction-required-pill.is-optional {
            border-color: #cbd5e1;
            color: #64748b;
        }

        .ad-correction-score-pill {
            align-self: flex-start;
        }

        .ad-correction-statement {
            color: var(--ad-ink);
            font-size: 17px;
            font-weight: 700;
            line-height: 1.45;
            margin-bottom: 22px;
        }

        .ad-correction-answer {
            background: #fff;
            border: 0;
            padding: 0;
        }

        .ad-correction-answer-block {
            min-width: 0;
        }

        .ad-correction-answer-label {
            color: var(--ad-muted);
            display: block;
            font-size: 12px;
            font-weight: 700;
            margin-bottom: 6px;
            text-transform: uppercase;
        }

        .ad-correction-expected-toggle {
            margin-top: 16px;
        }

        .ad-correction-expected-answer {
            background: #fff;
            border-left: 3px solid #94a3b8;
            border-radius: 0;
            margin-top: 14px;
            padding: 2px 0 0 16px;
        }

        .ad-correction-expected-answer[hidden] {
            display: none !important;
        }

        .ad-correction-expected-answer .ad-correction-answer-label {
            color: #334155;
            margin-bottom: 10px;
        }

        .ad-correction-choice-list {
            display: grid;
            gap: 12px;
            max-width: none;
            width: 100%;
        }

        .ad-correction-choice {
            align-items: center;
            display: grid;
            gap: 14px;
            grid-template-columns: 26px minmax(0, 1fr);
            min-width: 0;
        }

        .ad-correction-choice-control {
            background: #fff;
            border: 2px solid #536273;
            display: inline-flex;
            flex-shrink: 0;
            height: 22px;
            position: relative;
            width: 22px;
        }

        .ad-correction-choice-control--radio {
            border-radius: 50%;
        }

        .ad-correction-choice-control--checkbox {
            border-radius: 4px;
        }

        .ad-correction-choice.is-selected .ad-correction-choice-control {
            background: #475569;
            border-color: #334155;
        }

        .ad-correction-choice.is-correct .ad-correction-choice-control {
            background: #22c55e;
            border-color: #15803d;
        }

        .ad-correction-choice.is-incorrect .ad-correction-choice-control {
            background: #ef4444;
            border-color: #b91c1c;
        }

        .ad-correction-choice.is-missed .ad-correction-choice-control {
            background: #fff;
            border-color: #dc2626;
        }

        .ad-correction-choice.is-expected .ad-correction-choice-control {
            background: #64748b;
            border-color: #475569;
        }

        .ad-correction-choice.is-selected .ad-correction-choice-control--checkbox::after,
        .ad-correction-choice.is-correct .ad-correction-choice-control--checkbox::after,
        .ad-correction-choice.is-incorrect .ad-correction-choice-control--checkbox::after,
        .ad-correction-choice.is-expected .ad-correction-choice-control--checkbox::after {
            border-bottom: 2px solid #fff;
            border-right: 2px solid #fff;
            content: "";
            height: 11px;
            left: 6px;
            position: absolute;
            top: 2px;
            transform: rotate(45deg);
            width: 6px;
        }

        .ad-correction-choice-text {
            align-items: center;
            background: #fff;
            border: 2px solid #536273;
            border-radius: 10px;
            color: #465568;
            display: flex;
            font-size: 15px;
            font-weight: 700;
            line-height: 1.25;
            min-height: 46px;
            min-width: 0;
            overflow-wrap: anywhere;
            padding: 11px 22px;
            max-width: 100%;
            width: 100%;
        }

        .ad-correction-choice.is-selected .ad-correction-choice-text {
            background: #f8fafc;
            border-color: #334155;
            color: #172033;
        }

        .ad-correction-choice.is-correct .ad-correction-choice-text {
            background: #ecfdf5;
            border-color: #16a34a;
            color: #166534;
        }

        .ad-correction-choice.is-incorrect .ad-correction-choice-text {
            background: #fef2f2;
            border-color: #dc2626;
            color: #991b1b;
        }

        .ad-correction-choice.is-missed .ad-correction-choice-text {
            background: #fff;
            border-color: #dc2626;
            color: #991b1b;
        }

        .ad-correction-choice.is-expected .ad-correction-choice-text {
            background: #fff;
            border-color: #94a3b8;
            color: #334155;
        }

        .ad-correction-text-answer {
            border: 2px solid #536273;
            border-radius: 10px;
            color: #465568;
            font-size: 15px;
            font-weight: 700;
            line-height: 1.45;
            max-width: none;
            min-height: 54px;
            overflow-wrap: anywhere;
            padding: 15px 22px;
            white-space: pre-wrap;
            width: 100%;
        }

        .ad-correction-text-answer--expected {
            background: #fff;
            border-color: #cbd5e1;
            color: #172033;
        }

        .ad-correction-text-answer--long {
            min-height: 120px;
        }

        .ad-correction-upload-row {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
        }

        .ad-correction-upload-answer {
            align-items: center;
            background: #45c7bd;
            border: 2px solid #278d84;
            border-radius: 10px;
            color: #fff;
            display: inline-flex;
            font-size: 15px;
            font-weight: 700;
            gap: 8px;
            min-height: 46px;
            max-width: 100%;
            overflow-wrap: anywhere;
            padding: 11px 22px;
            text-decoration: none;
        }

        .ad-correction-upload-answer:hover {
            background: #38b7ad;
            color: #fff;
        }

        .ad-correction-upload-file {
            color: #465568;
            font-size: 15px;
            font-weight: 700;
            overflow-wrap: anywhere;
        }

        .ad-correction-rating {
            align-items: center;
            color: #334155;
            display: inline-flex;
            flex-wrap: wrap;
            font-size: 26px;
            gap: 4px;
            line-height: 1;
        }

        .ad-correction-rating.is-correct {
            color: #16a34a;
        }

        .ad-correction-rating.is-incorrect {
            color: #dc2626;
        }

        .ad-correction-rating.is-expected {
            color: #475569;
        }

        .ad-correction-rating.is-neutral {
            color: #334155;
        }

        .ad-correction-controls {
            align-items: flex-end;
            display: flex;
            gap: 12px;
            justify-content: space-between;
            margin-top: 16px;
        }

        .ad-correction-score-field {
            max-width: 170px;
            min-width: 150px;
        }

        .ad-correction-score-input.is-invalid {
            border-color: #dc2626 !important;
            box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
        }

        .ad-correction-score-error {
            color: #dc2626;
            display: block;
            font-size: 12px;
            line-height: 1.3;
            margin-top: 4px;
            min-height: 16px;
        }

        .ad-correction-score-error:empty {
            margin-top: 0;
            min-height: 0;
        }

        .ad-correction-auto-button {
            background: #eff6ff;
            border-color: #bfdbfe;
            color: var(--ad-primary-dark);
            min-height: 42px;
            white-space: nowrap;
        }

        .ad-correction-auto-button:hover {
            background: #dbeafe;
            color: var(--ad-primary-dark);
        }

        .ad-correction-auto-button[disabled],
        .ad-correction-submit-button[disabled] {
            cursor: not-allowed;
            opacity: .65 !important;
        }

        .ad-correction-feedback {
            color: var(--ad-muted);
            font-size: 13px;
            margin-right: auto;
            min-height: 20px;
        }

        .ad-correction-feedback.is-error {
            color: #dc2626;
        }

        .ad-correction-feedback.is-success {
            color: #15803d;
        }

        .ad-correction-submit-button {
            background: var(--ad-primary) !important;
            border-color: var(--ad-primary) !important;
            color: #fff !important;
            display: inline-flex !important;
            min-width: 160px;
            opacity: 1 !important;
            visibility: visible !important;
        }

        .ad-correction-submit-button:hover {
            background: var(--ad-primary-dark) !important;
            border-color: var(--ad-primary-dark) !important;
            color: #fff !important;
        }

        .ad-outline-button,
        .ad-primary-button {
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

        .ad-outline-button {
            background: #fff;
            border: 1px solid var(--ad-border);
            color: var(--ad-ink);
        }

        .ad-outline-button:hover {
            border-color: rgba(37, 99, 235, 0.45);
            color: var(--ad-primary-dark);
        }

        .ad-primary-button {
            background: var(--ad-primary);
            border: 1px solid var(--ad-primary);
            color: #fff;
        }

        .ad-primary-button:hover {
            background: var(--ad-primary-dark);
            border-color: var(--ad-primary-dark);
            color: #fff;
            transform: translateY(-1px);
        }

        .ad-info-grid {
            display: grid;
            gap: 14px;
            grid-template-columns: repeat(4, minmax(0, 1fr));
        }

        .ad-info-cell {
            background: #f8fbfb;
            border: 1px solid var(--ad-border);
            border-radius: 8px;
            min-height: 88px;
            padding: 16px;
        }

        .ad-info-cell--wide {
            grid-column: 1 / -1;
            min-height: 0;
        }

        .ad-table-wrap {
            max-width: 100%;
            min-width: 0;
            overflow-x: auto;
            width: 100%;
            -webkit-overflow-scrolling: touch;
        }

        .ad-data-table {
            min-width: 820px;
        }

        .gape-assessment-builder {
            display: block;
        }

        .gape-question-card,
        .gape-question-type-card {
            background: #fff;
            border: 1px solid var(--neutral-30);
            border-radius: 10px;
        }

        .gape-question-type-card:hover {
            border-color: var(--main-600);
            color: var(--main-600);
        }

        .gape-question-card {
            scroll-margin-top: 96px;
        }

        .gape-question-card.is-dragging {
            opacity: .58;
        }

        .gape-question-drag-handle {
            color: var(--neutral-500);
            cursor: grab;
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 18px;
            margin-top: 12px;
            touch-action: none;
        }

        .gape-question-answer-area {
            border: 1px solid var(--neutral-30);
            border-radius: 8px;
            padding: 16px;
        }

        .gape-question-statement-editor {
            background: #fff !important;
            border: 1px solid var(--neutral-30);
            border-left: 4px solid var(--main-600);
            border-radius: 8px;
            box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
            font-size: 16px;
            font-weight: 600;
            min-height: 92px;
            padding: 14px 16px;
            resize: vertical;
        }

        .gape-question-statement-field {
            align-items: center;
            display: flex;
            gap: 6px;
            margin-bottom: 14px;
        }

        .gape-question-statement-field__icon {
            align-items: center;
            background: var(--main-50);
            border-radius: 8px;
            color: var(--main-600);
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 18px;
            height: 42px;
            justify-content: center;
            width: 42px;
        }

        .gape-question-statement-editor:focus {
            border-color: var(--main-600);
            box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
        }

        .gape-question-header-actions {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            justify-content: flex-end;
        }

        .gape-question-score-control {
            align-items: center;
            display: flex;
            gap: 8px;
        }

        .gape-question-score-control input {
            width: 96px;
        }

        .gape-question-type-control select {
            background-position: right 14px center;
            max-width: min(100%, 320px);
            min-width: 280px;
            padding-right: 44px !important;
            text-overflow: ellipsis;
            width: 280px;
        }

        .gape-question-format-menu {
            position: relative;
        }

        .gape-question-format-menu summary {
            cursor: pointer;
            list-style: none;
            min-height: 40px;
        }

        .gape-question-format-menu summary::-webkit-details-marker {
            display: none;
        }

        .gape-question-format-menu__panel {
            background: #fff;
            border: 1px solid var(--neutral-30);
            border-radius: 8px;
            box-shadow: 0 14px 34px rgba(15, 23, 42, 0.12);
            min-width: 180px;
            padding: 10px;
            position: absolute;
            right: 0;
            top: calc(100% + 8px);
            z-index: 20;
        }

        .gape-question-required-control {
            min-height: 40px;
        }

        .gape-question-answer-area.is-single-choice {
            background: #fbfcfd;
            border-left: 4px solid #45c7bd;
        }

        .gape-question-answer-area.is-multiple-choice {
            background: #fbfcfd;
            border-left: 4px solid #278d84;
        }

        .gape-question-answer-area.is-text-answer,
        .gape-question-answer-area.is-rating,
        .gape-question-answer-area.is-file-upload {
            background: #fbfcfd;
        }

        .gape-question-answer-area.is-text-answer {
            border-left: 4px solid #45c7bd;
        }

        .gape-question-answer-area.is-rating {
            border-left: 4px solid #f0bc18;
        }

        .gape-question-answer-area.is-file-upload {
            border-left: 4px solid #278d84;
        }

        .gape-question-answer-area.is-paragraph {
            background: #f8fafb;
            border-left: 4px solid #51606f;
        }

        .gape-expected-answer-row {
            align-items: stretch;
            display: flex;
            flex-direction: column;
            gap: 8px;
            margin-top: 14px;
        }

        .gape-expected-answer-label {
            align-items: center;
            color: var(--neutral-700);
            display: inline-flex;
            font-size: 13px;
            font-weight: 700;
            gap: 8px;
            line-height: 1.2;
            min-height: 0;
            padding-top: 0;
        }

        .gape-short-expected-input,
        .gape-paragraph-expected-editor {
            margin-top: 0 !important;
        }

        .gape-paragraph-expected-editor {
            background-color: #fff !important;
            background-image: linear-gradient(to bottom, transparent 31px, rgba(81, 96, 111, 0.12) 32px);
            background-size: 100% 32px;
            line-height: 32px;
            min-height: 156px;
            resize: vertical;
        }

        .gape-option-row {
            align-items: center;
            background: #fff;
            border: 1px solid var(--neutral-30);
            border-radius: 8px;
            display: grid;
            gap: 12px;
            grid-template-columns: minmax(0, 1fr) max-content;
            padding: 12px;
        }

        .gape-option-row.is-dragging {
            opacity: .52;
        }

        .gape-option-main {
            align-items: center;
            display: flex;
            gap: 10px;
            min-width: 0;
            width: 100%;
        }

        .gape-option-main .form-control {
            flex: 1 1 auto;
            min-width: 0;
        }

        .gape-option-drag-handle {
            color: var(--neutral-500);
            cursor: grab;
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 18px;
            touch-action: none;
        }

        .gape-option-choice-marker {
            align-items: center;
            border: 2px solid #51606f;
            border-radius: 50%;
            color: #fff;
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 10px;
            height: 18px;
            justify-content: center;
            width: 18px;
        }

        .gape-option-choice-marker.is-checkbox {
            border-radius: 4px;
        }

        .gape-option-choice-marker.is-radio {
            background: #fff;
            border-color: #278d84;
        }

        .gape-option-choice-marker.is-checkbox {
            background: #fff;
            border-color: #278d84;
        }

        .gape-option-choice-marker.is-correct {
            background: #45c7bd;
            border-color: #278d84;
        }

        .gape-add-option-shell {
            position: relative;
        }

        .gape-add-option-feedback {
            background: #162234;
            border-radius: 6px;
            bottom: calc(100% + 7px);
            box-shadow: 0 8px 20px rgba(15, 23, 42, 0.18);
            color: #fff;
            display: none;
            font-size: 12px;
            font-weight: 700;
            line-height: 1.25;
            max-width: min(260px, 72vw);
            padding: 7px 9px;
            position: absolute;
            right: 0;
            z-index: 25;
        }

        .gape-add-option-feedback::after {
            border: 6px solid transparent;
            border-top-color: #162234;
            content: "";
            position: absolute;
            right: 18px;
            top: 100%;
        }

        .gape-add-option-shell.is-invalid .gape-add-option-feedback {
            display: block;
        }

        .gape-add-option-button.is-blocked {
            cursor: not-allowed;
        }

        .gape-new-option-input.is-invalid {
            border-color: #e34848 !important;
            box-shadow: 0 0 0 3px rgba(227, 72, 72, 0.11);
        }

        .gape-option-correct-stack {
            align-items: center;
            display: inline-flex;
            flex: 0 0 86px;
            height: 44px;
            justify-content: center;
            min-height: 44px;
            position: relative;
            width: 86px;
        }

        .gape-option-actions {
            align-items: center;
            display: flex;
            flex-wrap: nowrap;
            gap: 10px;
            justify-content: flex-end;
            min-width: max-content;
        }

        .gape-add-option-button {
            min-height: 44px;
            min-width: 124px;
            white-space: nowrap;
        }

        [data-delete-option-button]:disabled {
            cursor: not-allowed;
            opacity: .48;
            pointer-events: none;
        }

        .gape-correct-answer-label,
        .gape-rating-correct-label {
            background: transparent;
            border: 0;
            border-radius: 0;
            color: #278d84;
            display: none;
            font-size: 8px;
            font-weight: 700;
            line-height: 1;
            opacity: .72;
            padding: 0;
            white-space: nowrap;
        }

        .gape-option-correct-stack.is-correct-primary .gape-correct-answer-label {
            display: inline-flex;
            left: 50%;
            position: absolute;
            top: -10px;
            transform: translateX(-50%);
        }

        .gape-rating-builder {
            background: #fff;
            border: 1px solid var(--neutral-30);
            border-radius: 8px;
            padding: 14px;
        }

        .gape-rating-config-grid {
            display: grid;
            gap: 12px;
            grid-template-columns: 1fr;
        }

        .gape-rating-picker {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .gape-rating-unit {
            align-items: center;
            background: transparent;
            border: 0;
            color: #c6ccd3;
            display: inline-flex;
            font-size: 24px;
            height: 34px;
            justify-content: center;
            padding: 0;
            position: relative;
            width: 34px;
        }

        .gape-rating-unit__base,
        .gape-rating-unit__fill {
            align-items: center;
            display: inline-flex;
            inset: 0;
            justify-content: center;
            position: absolute;
        }

        .gape-rating-unit__fill {
            color: #f0bc18;
            justify-content: flex-start;
            overflow: hidden;
            width: 0;
        }

        .gape-rating-unit__fill i {
            align-items: center;
            display: inline-flex;
            flex: 0 0 34px;
            justify-content: center;
            width: 34px;
        }

        [data-rating-style="circles"] .gape-rating-unit__fill {
            color: #278d84;
        }

        [data-rating-style="hearts"] .gape-rating-unit__fill {
            color: #ef5a76;
        }

        .gape-rating-unit.is-half .gape-rating-unit__fill {
            width: 50%;
        }

        .gape-rating-unit.is-full .gape-rating-unit__fill {
            width: 100%;
        }

        .gape-rating-unit.has-correct-label .gape-rating-correct-label {
            display: inline-flex;
            left: 50%;
            position: absolute;
            top: -24px;
            transform: translateX(-50%);
        }

        .gape-rating-readout {
            color: var(--neutral-600);
            font-size: 13px;
            font-weight: 600;
            min-width: 76px;
        }

        .gape-option-correct-control {
            align-items: center;
            border: 1px solid var(--neutral-30);
            border-radius: 8px;
            color: var(--neutral-600);
            display: inline-flex;
            height: 36px;
            justify-content: center;
            width: 36px;
        }

        .gape-option-correct-control input {
            accent-color: var(--main-600);
            cursor: pointer;
            height: 18px;
            width: 18px;
        }

        .gape-option-correct-control:has(input:checked) {
            background: rgba(37, 99, 235, 0.1);
            border-color: rgba(37, 99, 235, 0.35);
        }

        .gape-file-upload-answer {
            align-items: center;
            border: 1px dashed #278d84;
            border-radius: 8px;
            display: flex;
            gap: 12px;
            padding: 14px;
        }

        .gape-question-type-grid {
            display: grid;
            gap: 14px;
            grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
        }

        .gape-question-type-card {
            min-height: 176px;
            overflow: hidden;
            text-align: start;
            transition: border-color .2s ease, color .2s ease, transform .2s ease;
        }

        .gape-question-type-card:hover {
            transform: translateY(-2px);
        }

        .gape-add-question-dialog {
            max-width: 1440px;
            width: calc(100vw - 80px);
            height: min(820px, calc(100vh - 72px));
            min-height: 0;
        }

        .gape-add-question-modal {
            display: flex;
            flex-direction: column;
            height: 100%;
            max-height: 100%;
        }

        .gape-add-question-modal .modal-body {
            flex: 1 1 auto;
            min-height: 0;
            overflow: hidden;
            padding: 0;
        }

        .gape-add-question-shell {
            display: grid;
            grid-template-columns: 270px minmax(0, 1fr);
            height: 100%;
            min-height: 0;
        }

        .gape-add-question-side {
            border-right: 1px solid var(--neutral-30);
            display: flex;
            flex-direction: column;
            min-height: 0;
            padding: 18px;
        }

        .gape-add-question-categories {
            min-height: 0;
            overflow-y: auto;
        }

        .gape-question-category {
            align-items: center;
            background: #fff;
            border: 1px solid transparent;
            border-radius: 8px;
            color: var(--neutral-600);
            display: flex;
            font-size: 14px;
            font-weight: 700;
            gap: 10px;
            min-height: 42px;
            padding: 10px 12px;
            text-align: left;
            width: 100%;
        }

        .gape-question-category:hover,
        .gape-question-category.is-active {
            background: rgba(37, 99, 235, 0.09);
            border-color: rgba(37, 99, 235, 0.18);
            color: var(--main-600);
        }

        .gape-add-question-main {
            height: 100%;
            min-height: 0;
            overflow-y: auto;
            padding: 18px 20px 22px;
        }

        .gape-question-type-section {
            margin-bottom: 24px;
        }

        .gape-question-type-section:last-child {
            margin-bottom: 0;
        }

        .gape-question-type-section-title {
            align-items: center;
            color: var(--neutral-700);
            display: flex;
            font-size: 12px;
            font-weight: 800;
            gap: 10px;
            letter-spacing: 0;
            margin-bottom: 10px;
            text-transform: uppercase;
        }

        .gape-question-type-section-title::after {
            background: var(--neutral-30);
            content: "";
            flex: 1 1 auto;
            height: 1px;
        }

        .gape-question-type-card__preview {
            align-items: center;
            background: #f8fafb;
            display: flex;
            height: 98px;
            justify-content: center;
            margin: -18px -18px 14px;
        }

        .gape-question-type-card__body {
            display: flex;
            flex-direction: column;
            min-height: 44px;
        }

        .gape-preview-choice {
            display: grid;
            gap: 7px;
            width: 92px;
        }

        .gape-preview-choice__row {
            align-items: center;
            display: grid;
            gap: 6px;
            grid-template-columns: 14px 1fr;
        }

        .gape-preview-choice__mark {
            align-items: center;
            border: 2px solid #51606f;
            border-radius: 50%;
            color: #fff;
            display: inline-flex;
            font-size: 9px;
            height: 14px;
            justify-content: center;
            width: 14px;
        }

        .gape-preview-choice__mark.is-check {
            border-radius: 3px;
        }

        .gape-preview-choice__mark.is-active {
            background: #45c7bd;
            border-color: #278d84;
        }

        .gape-preview-choice__line {
            border: 2px solid #51606f;
            border-radius: 999px;
            height: 14px;
        }

        .gape-preview-choice__line.is-active {
            background: #45c7bd;
            border-color: #278d84;
        }

        .gape-preview-input {
            align-items: center;
            border: 2px solid #51606f;
            border-radius: 5px;
            color: #51606f;
            display: flex;
            font-size: 10px;
            font-weight: 800;
            height: 34px;
            padding: 0 12px;
            width: 118px;
        }

        .gape-preview-input.is-textarea {
            align-items: flex-start;
            height: 56px;
            padding-top: 10px;
        }

        .gape-preview-upload {
            align-items: center;
            background: #45c7bd;
            border: 2px solid #278d84;
            border-radius: 5px;
            color: #fff;
            display: inline-flex;
            font-size: 10px;
            font-weight: 800;
            gap: 6px;
            min-height: 34px;
            padding: 0 12px;
        }

        .gape-preview-rating {
            color: #f0bc18;
            display: flex;
            font-size: 22px;
            gap: 2px;
        }

        @media (max-width: 991px) {
            .ad-mode-grid,
            .ad-info-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }

            .ad-correction-summary-grid {
                grid-template-columns: repeat(2, minmax(0, 1fr));
            }

            #enrollments .ad-data-table {
                min-width: 0;
                width: 100%;
            }

            #enrollments .ad-table-wrap {
                overflow-x: hidden;
                width: 100%;
            }

            #enrollments .ad-data-table thead {
                display: none;
            }

            #enrollments .ad-data-table,
            #enrollments .ad-data-table tbody,
            #enrollments .ad-data-table tr,
            #enrollments .ad-data-table td {
                display: block;
                width: 100%;
            }

            #enrollments .ad-data-table tr {
                border: 1px solid var(--ad-border);
                border-radius: 8px;
                margin-bottom: 12px;
                padding: 12px;
            }

            #enrollments .ad-data-table td {
                border-bottom: 0 !important;
                padding: 6px 0 !important;
                text-align: left !important;
            }

            #enrollments .ad-data-table td[data-label]::before {
                color: var(--neutral-500);
                content: attr(data-label);
                display: block;
                font-size: 12px;
                font-weight: 600;
                margin-bottom: 4px;
            }

            #enrollments .ad-data-table td form,
            #enrollments .ad-data-table td .d-inline-flex {
                justify-content: flex-start !important;
                width: auto;
            }

            .gape-add-question-dialog {
                width: calc(100vw - 24px);
            }

            .gape-add-question-shell {
                grid-template-columns: 1fr;
            }

            .gape-add-question-side {
                border-bottom: 1px solid var(--neutral-30);
                border-right: 0;
                max-height: 240px;
            }
        }

        @media (max-width: 575px) {
            html,
            body,
            .dashbord,
            .dashbord > .d-flex,
            .dashbord-body {
                max-width: 100%;
                overflow-x: hidden;
            }

            .ad-shell {
                padding-left: 14px !important;
                padding-right: 14px !important;
            }

            .ad-hero {
                padding: 18px 16px !important;
            }

            .ad-hero > .d-flex,
            .ad-hero > .d-flex > .d-flex:first-child {
                width: 100%;
            }

            .ad-hero > .d-flex > .d-flex:first-child {
                flex-wrap: wrap;
            }

            .ad-hero > .d-flex > .d-flex:first-child > .min-w-0 {
                flex-basis: 100%;
                width: 100%;
            }

            .ad-hero h2 {
                flex-basis: 100%;
                font-size: 20px;
                line-height: 1.25;
            }

            .ad-hero p {
                line-height: 1.45;
            }

            .ad-hero-meta,
            .ad-mode-card .flex-wrap {
                align-items: flex-start !important;
                flex-direction: column;
                gap: 4px;
                max-width: 100%;
            }

            .ad-hero-meta > span:nth-child(even),
            .ad-mode-card .flex-wrap > span:nth-child(even) {
                display: none;
            }

            .ad-mode-grid,
            .ad-info-grid {
                grid-template-columns: 1fr;
            }

            .ad-mode-card {
                min-height: 0;
                padding: 16px;
            }

            .ad-mode-card > .d-flex {
                flex-wrap: wrap;
            }

            .ad-mode-card > .d-flex > .min-w-0,
            .ad-mode-card .flex-wrap {
                width: 100%;
            }

            .ad-mode-card,
            .ad-mode-card span {
                white-space: normal !important;
            }

            .ad-tab-panel .ad-surface {
                overflow-x: hidden;
                padding-left: 16px !important;
                padding-right: 16px !important;
                width: 100%;
            }

            #enrollments,
            #enrollments * {
                box-sizing: border-box;
            }

            #enrollments .ad-outline-button,
            #enrollments .ad-primary-button {
                max-width: 100%;
            }

            #enrollments .ad-enrollment-icon-button {
                max-width: none;
            }

            .ad-enrollment-policy-form {
                align-items: flex-start !important;
                flex-direction: column;
                overflow-x: hidden;
                width: 100%;
            }

            .ad-enrollment-policy-form .form-select,
            .ad-enrollment-policy-form .ad-outline-button {
                max-width: 100% !important;
                width: 100%;
            }

            .ad-enrollment-policy-form .ad-enrollment-icon-button {
                width: 42px !important;
            }

            .ad-enrollment-create-form {
                overflow: hidden;
                padding: 16px !important;
                width: 100%;
            }

            .ad-enrollment-create-form .row {
                margin-left: 0;
                margin-right: 0;
            }

            .ad-enrollment-create-form .row > [class*="col"] {
                flex: 0 0 100%;
                max-width: 100%;
                padding-left: 0;
                padding-right: 0;
                width: 100%;
            }

            .ad-enrollment-create-grid {
                grid-template-columns: minmax(0, 1fr) 52px;
            }

            .ad-enrollment-create-form .form-control,
            .ad-enrollment-create-form .form-select,
            .ad-enrollment-create-form .select2-container {
                width: 100% !important;
            }

            .ad-correction-dialog {
                max-width: calc(100vw - 20px);
            }

            .ad-correction-modal .modal-header,
            .ad-correction-modal .modal-body,
            .ad-correction-modal .modal-footer {
                padding-left: 16px;
                padding-right: 16px;
            }

            .ad-correction-summary-grid {
                grid-template-columns: 1fr;
            }

            .ad-correction-question-header,
            .ad-correction-controls {
                align-items: stretch;
                flex-direction: column;
            }

            .ad-correction-score-field {
                max-width: none;
                min-width: 0;
                width: 100%;
            }

            .ad-correction-auto-button {
                width: 100%;
            }

            .ad-correction-footer-actions {
                flex-direction: column;
                width: 100%;
            }

            .ad-correction-footer-actions form,
            .ad-correction-footer-actions .ad-outline-button,
            .ad-correction-footer-actions .ad-primary-button {
                width: 100%;
            }

            .ad-correction-feedback {
                margin-right: 0;
                order: 10;
                text-align: center;
                width: 100%;
            }

            .ad-data-table {
                min-width: 0;
                width: 100%;
            }

            .ad-table-wrap {
                overflow-x: hidden;
                width: 100%;
            }

            .ad-data-table thead {
                display: none;
            }

            .ad-data-table,
            .ad-data-table tbody,
            .ad-data-table tr,
            .ad-data-table td {
                display: block;
                width: 100%;
            }

            .ad-data-table tr {
                border: 1px solid var(--ad-border);
                border-radius: 8px;
                margin-bottom: 12px;
                padding: 12px;
            }

            .ad-data-table td {
                border-bottom: 0 !important;
                padding: 6px 0 !important;
                text-align: left !important;
            }

            .ad-data-table td[data-label]::before {
                color: var(--neutral-500);
                content: attr(data-label);
                display: block;
                font-size: 12px;
                font-weight: 600;
                margin-bottom: 4px;
            }

            .ad-data-table td form,
            .ad-data-table td .d-inline-flex {
                justify-content: flex-start !important;
                width: 100%;
            }

            .ad-data-table td input[type="date"] {
                min-width: 0 !important;
                width: 100%;
            }

            .gape-option-row {
                grid-template-columns: 1fr;
            }

            .gape-option-actions {
                justify-content: flex-end;
                width: 100%;
            }

            .gape-question-type-control,
            .gape-question-type-control select {
                min-width: 0;
                width: 100%;
            }

            .gape-expected-answer-label {
                min-height: 0;
                padding-top: 0;
            }

            .gape-question-type-grid {
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
            <div class="ad-shell px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>
                <c:url var="assessmentListUrl" value="/learning/assessments"/>
                <c:url var="assessmentEditUrl" value="/learning/assessments/${assessment.id}/edit"/>
                <c:url var="assessmentPdfUrl" value="/learning/assessments/${assessment.id}/pdf"/>

                <section class="ad-surface ad-hero px-24 py-24 mb-20">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-start gap-14 min-w-0">
                            <span class="bg-info-50 text-info-600 w-52 h-52 rounded-8 d-inline-flex align-items-center justify-content-center text-26 flex-shrink-0">
                                <i class="ph ph-eye"></i>
                            </span>
                            <div class="min-w-0">
                                <div class="d-flex align-items-center gap-8 flex-wrap mb-6">
                                    <h2 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${assessment.title}"/></h2>
                                    <span class="${assessment.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                        <c:out value="${assessment.stateLabel}"/>
                                    </span>
                                </div>
                                <div class="ad-hero-meta d-flex align-items-center gap-8 flex-wrap text-14 text-neutral-500 mb-0">
                                    <span><c:out value="${assessment.typeLabel}"/></span>
                                    <span>|</span>
                                    <span><c:out value="${assessment.modeLabel}"/></span>
                                    <span>|</span>
                                    <span><c:out value="${assessment.correctionModeLabel}"/></span>
                                    <span>|</span>
                                    <span><c:out value="${assessment.enrollmentModeLabel}"/></span>
                                    <span>|</span>
                                    <span><c:out value="${assessment.finalGradeWeight}"/>%</span>
                                    <span>|</span>
                                    <span><c:out value="${assessment.contextLabel}"/></span>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${assessmentListUrl}" class="ad-outline-button">
                                <i class="ph ph-arrow-left me-8"></i>Back
                            </a>
                            <a href="${assessmentPdfUrl}" class="ad-outline-button">
                                <i class="ph ph-download-simple me-8"></i>Download
                            </a>
                            <button type="button" class="ad-outline-button" data-bs-toggle="modal" data-bs-target="#assessmentSetupModal">
                                <i class="ph ph-sliders-horizontal me-8"></i>Setup
                            </button>
                            <a href="${assessmentEditUrl}" class="ad-primary-button">
                                <i class="ph ph-pencil-simple me-8"></i>Edit
                            </a>
                        </div>
                    </div>
                </section>

                <div class="ad-mode-grid mb-20" role="tablist" aria-label="Assessment detail sections">
                    <button type="button" class="ad-mode-card is-active" data-ad-tab="builder" role="tab" aria-selected="true">
                        <span class="d-flex align-items-start gap-14">
                            <span class="ad-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-pencil-ruler"></i></span>
                            <span class="min-w-0 ad-mode-content">
                                <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Builder</span>
                                <span class="d-block text-14 text-neutral-500 mb-10 ad-mode-description">Questions, options and scoring.</span>
                                <span class="d-block text-13 text-main-600 fw-semibold ad-mode-metric"><c:out value="${assessment.questionCountLabel}"/></span>
                            </span>
                        </span>
                    </button>
                    <button type="button" class="ad-mode-card" data-ad-tab="enrollments" role="tab" aria-selected="false">
                        <span class="d-flex align-items-start gap-14">
                            <span class="ad-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-users-three"></i></span>
                            <span class="min-w-0 ad-mode-content">
                                <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Enrollments</span>
                                <span class="d-block text-14 text-neutral-500 mb-10 ad-mode-description">Requests, policy and student access.</span>
                                <span class="d-block text-13 text-main-600 fw-semibold ad-mode-metric">
                                    ${fn:length(activeAssessmentEnrollments)} active<c:if test="${not empty pendingAssessmentEnrollments}"> | ${fn:length(pendingAssessmentEnrollments)} pending</c:if>
                                </span>
                            </span>
                        </span>
                    </button>
                    <button type="button" class="ad-mode-card" data-ad-tab="attempts" role="tab" aria-selected="false">
                        <span class="d-flex align-items-start gap-14">
                            <span class="ad-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-checks"></i></span>
                            <span class="min-w-0 ad-mode-content">
                                <span class="d-block text-17 fw-semibold text-neutral-800 mb-5">Attempts</span>
                                <span class="d-block text-14 text-neutral-500 mb-10 ad-mode-description">Submissions, answers and correction results.</span>
                                <span class="d-block text-13 text-main-600 fw-semibold ad-mode-metric"><c:out value="${assessment.attemptCountLabel}"/></span>
                            </span>
                        </span>
                    </button>
                </div>

                <section class="ad-tab-panel" id="builder" data-ad-panel="builder" role="tabpanel">
                    <div class="ad-surface px-24 py-24">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                            <div>
                                <h3 class="text-18 fw-medium text-neutral-700 mb-4">Builder</h3>
                                <span class="text-14 text-neutral-500"><c:out value="${assessment.questionCountLabel}"/> configured for this assessment.</span>
                            </div>
                            <c:if test="${canEditAssessmentStructure}">
                                <button type="button" class="ad-primary-button border-0" data-bs-toggle="modal" data-bs-target="#addQuestionModal">
                                    <i class="ph ph-plus-circle me-8"></i>Add Question
                                </button>
                            </c:if>
                        </div>

                        <c:if test="${not empty errorMessage}">
                            <div class="alert alert-danger mb-20" role="alert">
                                <c:out value="${errorMessage}"/>
                            </div>
                        </c:if>
                        <c:if test="${not empty assessmentStructureLockMessage}">
                            <div class="alert alert-warning mb-20" role="alert" data-assessment-structure-lock>
                                <i class="ph ph-lock-key me-8"></i><c:out value="${assessmentStructureLockMessage}"/>
                            </div>
                        </c:if>
                        <c:if test="${canEditAssessmentStructure}">
                            <div class="alert alert-warning mb-20 d-none" role="alert" data-score-balance-warning>
                                <i class="ph ph-warning-circle me-8"></i>
                                Scores total <strong data-score-total>0</strong> / <strong data-score-maximum><c:out value="${assessment.maxGrade}"/></strong>. Difference will be distributed automatically.
                            </div>
                            <div class="alert alert-danger mb-20 d-none" role="alert" data-autosave-error></div>
                        </c:if>

                        <div class="gape-assessment-builder"
                             data-assessment-builder-root
                             data-can-edit="${canEditAssessmentStructure}"
                             data-max-grade="${assessment.maxGrade}"
                             data-rebalance-url="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/rebalance"
                             data-question-reorder-url="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/reorder"
                             data-csrf-token="${sessionScope['gape.auth.csrfToken']}">
                            <main class="d-flex flex-column gap-18" data-question-list>
                                <c:forEach var="question" items="${questions}" varStatus="questionLoop">
                            <article id="question-${question.id}" class="gape-question-card px-22 py-22" data-question-card data-question-id="${question.id}">
                                <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap border-bottom-dashed pb-18 mb-18">
                                    <div class="d-flex align-items-start gap-12 min-w-0">
                                        <c:if test="${canEditAssessmentStructure}">
                                            <span class="gape-question-drag-handle" draggable="true" title="Drag to reorder" aria-label="Drag to reorder"><i class="ph ph-dots-six-vertical"></i></span>
                                        </c:if>
                                        <span class="w-44 h-44 rounded-8 bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                                            <i class="${question.iconClass}"></i>
                                        </span>
                                        <div class="min-w-0">
                                            <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                                <h3 class="text-17 fw-semibold text-neutral-800 mb-0"><span data-question-number>Question ${questionLoop.count}</span></h3>
                                            </div>
                                            <span class="text-13 text-neutral-500">
                                                <c:out value="${question.typeLabel}"/><c:if test="${question.allowsOptions}"> | <span data-option-count-summary data-question-id="${question.id}">${question.optionCount} ${question.optionCount == 1 ? 'option' : 'options'}</span></c:if>
                                            </span>
                                        </div>
                                    </div>
                                    <div class="gape-question-header-actions">
                                        <label class="gape-question-score-control text-13 text-neutral-700 mb-0">
                                            Score
                                            <input form="questionForm${question.id}" id="questionScore${question.id}" name="score" type="number" step="0.01" min="0.1" max="${assessment.maxGrade}" required value="<c:out value='${question.score}'/>" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" data-question-score-input ${canEditAssessmentStructure ? '' : 'disabled'}>
                                        </label>
                                        <c:if test="${question.typeValue == 'rating'}">
                                            <label class="gape-question-type-control mb-0">
                                                <select form="questionForm${question.id}" name="ratingDesign" class="form-select px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" data-rating-design-select data-question-id="${question.id}" ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                    <option value="stars_integer" ${question.ratingDesignValue == 'stars_integer' ? 'selected' : ''}>Design: Stars - Integer</option>
                                                    <option value="stars_half" ${question.ratingDesignValue == 'stars_half' ? 'selected' : ''}>Design: Stars - Fractional</option>
                                                    <option value="circles_integer" ${question.ratingDesignValue == 'circles_integer' ? 'selected' : ''}>Design: Circles - Integer</option>
                                                    <option value="circles_half" ${question.ratingDesignValue == 'circles_half' ? 'selected' : ''}>Design: Circles - Fractional</option>
                                                    <option value="hearts_integer" ${question.ratingDesignValue == 'hearts_integer' ? 'selected' : ''}>Design: Hearts - Integer</option>
                                                    <option value="hearts_half" ${question.ratingDesignValue == 'hearts_half' ? 'selected' : ''}>Design: Hearts - Fractional</option>
                                                </select>
                                            </label>
                                            <label class="gape-question-score-control text-13 text-neutral-700 mb-0">
                                                Max
                                                <input form="questionForm${question.id}" name="ratingMax" type="number" min="1" max="100" step="1" required value="${question.ratingMax}" class="form-control px-12 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" data-rating-max-input data-question-id="${question.id}" ${canEditAssessmentStructure ? '' : 'disabled'}>
                                            </label>
                                        </c:if>
                                        <c:if test="${question.typeValue == 'file_upload'}">
                                            <details class="gape-question-format-menu">
                                                <summary class="d-inline-flex align-items-center gap-8 px-12 py-8 border border-neutral-30 rounded-8 bg-neutral-20 text-13 text-neutral-700">
                                                    Accepted files <i class="ph ph-caret-down"></i>
                                                </summary>
                                                <div class="gape-question-format-menu__panel">
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-8">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="pdf" ${question.acceptsPdf ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> PDF
                                                    </label>
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-8">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="text" ${question.acceptsText ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> Text
                                                    </label>
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-8">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="image" ${question.acceptsImage ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> Image
                                                    </label>
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-8">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="video" ${question.acceptsVideo ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> Video
                                                    </label>
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-0">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="audio" ${question.acceptsAudio ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> Audio
                                                    </label>
                                                    <label class="d-flex align-items-center gap-8 text-13 text-neutral-700 mb-0 mt-8">
                                                        <input form="questionForm${question.id}" type="checkbox" name="acceptedFormat" value="archive" ${question.acceptsArchive ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}> Archive
                                                    </label>
                                                </div>
                                            </details>
                                        </c:if>
                                        <label class="gape-question-required-control d-inline-flex align-items-center gap-8 px-12 py-8 border border-neutral-30 rounded-8 bg-neutral-20 text-13 text-neutral-700 mb-0">
                                            <input form="questionForm${question.id}" type="checkbox" name="required" value="true" ${question.required ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}>
                                            Required
                                        </label>
                                        <c:if test="${canEditAssessmentStructure}">
                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/${question.id}/delete" method="post" class="m-0">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <button type="submit" class="border border-danger-200 bg-danger-50 text-danger-600 w-40 h-40 rounded-8 hover-bg-danger-100 transition-03 text-18 d-inline-flex align-items-center justify-content-center" title="Delete question" aria-label="Delete question" data-delete-question-button>
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                </div>

                                <form id="questionForm${question.id}" action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/${question.id}" method="post" class="m-0" data-question-form data-question-id="${question.id}" data-question-type="${question.typeValue}">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <c:choose>
                                        <c:when test="${question.allowsOptions}">
                                            <input type="hidden" name="expectedAnswer" value="<c:out value='${question.expectedAnswer}'/>">
                                            <div class="gape-question-answer-area ${question.typeValue == 'multiple_choice' ? 'is-multiple-choice' : 'is-single-choice'}" data-question-answer-design="${question.typeValue}" data-question-id="${question.id}">
                                                <div class="gape-question-statement-field">
                                                    <span class="gape-question-statement-field__icon" aria-hidden="true"><i class="ph ph-text-aa"></i></span>
                                                    <textarea id="questionStatement${question.id}" name="statement" maxlength="2000" rows="2" required class="form-control gape-question-statement-editor text-neutral-800" placeholder="Write the question here" ${canEditAssessmentStructure ? '' : 'disabled'}><c:out value="${question.statement}"/></textarea>
                                                </div>
                                                <c:if test="${question.optionCount == 0}">
                                                    <div class="alert alert-warning mb-12 mt-14" role="alert" data-empty-options-warning>
                                                        At least one option is required.
                                                    </div>
                                                </c:if>
                                                <div class="d-flex flex-column gap-10 mt-14" data-option-list>
                                                            <c:forEach var="option" items="${question.options}" varStatus="optionLoop">
                                                                <div class="gape-option-row" draggable="${canEditAssessmentStructure}" data-option-row data-option-id="${option.id}">
                                                                    <input type="hidden" name="optionOrder_${option.id}" value="${optionLoop.count}" data-option-order-input>
                                                                    <div class="gape-option-main">
                                                                        <c:if test="${canEditAssessmentStructure}">
                                                                            <span class="gape-option-drag-handle" title="Drag to reorder" aria-label="Drag to reorder"><i class="ph ph-dots-six-vertical"></i></span>
                                                                        </c:if>
                                                                        <span class="gape-option-choice-marker ${question.typeValue == 'multiple_choice' ? 'is-checkbox' : 'is-radio'}" aria-hidden="true"></span>
                                                                        <input id="optionText${option.id}" name="optionText_${option.id}" maxlength="300" value="<c:out value='${option.text}'/>" class="form-control px-12 py-9 text-13 bg-white border-neutral-30 border rounded-8" ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                                    </div>
                                                                    <div class="gape-option-actions">
                                                                        <div class="gape-option-correct-stack" data-correct-answer-stack>
                                                                            <span class="gape-correct-answer-label" data-correct-answer-label>Correct Answer</span>
                                                                            <label class="gape-option-correct-control mb-0" title="Correct answer">
                                                                                <c:choose>
                                                                                    <c:when test="${question.typeValue == 'multiple_choice'}">
                                                                                        <input type="checkbox" name="optionCorrect_${option.id}" value="true" aria-label="Correct answer" ${option.correct ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                                                    </c:when>
                                                                                    <c:otherwise>
                                                                                        <input type="radio" name="singleCorrect_${question.id}" value="${option.id}" aria-label="Correct answer" ${option.correct ? 'checked' : ''} ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                                                    </c:otherwise>
                                                                                </c:choose>
                                                                            </label>
                                                                        </div>
                                                                        <c:if test="${canEditAssessmentStructure}">
                                                                            <button type="submit" formaction="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/${question.id}/options/${option.id}/archive" formnovalidate class="border border-danger-200 bg-danger-50 text-danger-600 w-36 h-36 rounded-8 hover-bg-danger-100 transition-03 text-16 d-inline-flex align-items-center justify-content-center" title="Delete option" aria-label="Delete option" data-delete-option-button>
                                                                                <i class="ph ph-trash"></i>
                                                                            </button>
                                                                        </c:if>
                                                                    </div>
                                                                </div>
                                                            </c:forEach>
                                                </div>
                                                <c:if test="${canEditAssessmentStructure}">
                                                    <div class="gape-option-row gape-add-option-shell border-main-200 bg-main-25 mt-12" data-add-option-row>
                                                        <div class="gape-option-main">
                                                            <input id="newOptionText${question.id}" name="text" maxlength="300" class="form-control gape-new-option-input px-12 py-9 text-13 bg-white border-neutral-30 border rounded-8" placeholder="Option text" data-new-option-input>
                                                        </div>
                                                        <div class="gape-option-actions">
                                                            <div class="gape-option-correct-stack" data-correct-answer-stack>
                                                                <span class="gape-correct-answer-label" data-correct-answer-label>Correct Answer</span>
                                                                <label class="gape-option-correct-control mb-0" title="Correct answer">
                                                                    <c:choose>
                                                                        <c:when test="${question.typeValue == 'multiple_choice'}">
                                                                            <input type="checkbox" name="correct" value="true" aria-label="Correct answer" ${question.optionCount == 0 ? 'checked' : ''}>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <input type="radio" name="singleCorrect_${question.id}" value="new" aria-label="Correct answer" ${question.optionCount == 0 ? 'checked' : ''}>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </label>
                                                            </div>
                                                            <button type="button" formaction="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions/${question.id}/options" class="gape-add-option-button bg-main-600 px-14 py-9 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 text-13 border-0" data-add-option-button>Add Option</button>
                                                            <span class="gape-add-option-feedback" role="status" aria-live="polite" data-add-option-feedback>Question option text is required</span>
                                                        </div>
                                                    </div>
                                                </c:if>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="gape-question-answer-area ${question.typeValue == 'paragraph' ? 'is-paragraph' : question.typeValue == 'file_upload' ? 'is-file-upload' : question.typeValue == 'rating' ? 'is-rating' : 'is-text-answer'}" data-question-answer-design="${question.typeValue}">
                                                <div class="gape-question-statement-field">
                                                    <span class="gape-question-statement-field__icon" aria-hidden="true"><i class="ph ph-text-aa"></i></span>
                                                    <textarea id="questionStatement${question.id}" name="statement" maxlength="2000" rows="2" required class="form-control gape-question-statement-editor text-neutral-800" placeholder="Write the question here" ${canEditAssessmentStructure ? '' : 'disabled'}><c:out value="${question.statement}"/></textarea>
                                                </div>
                                                <c:choose>
                                                    <c:when test="${question.typeValue == 'paragraph'}">
                                                        <div class="gape-expected-answer-row">
                                                            <label for="expectedAnswer${question.id}" class="gape-expected-answer-label mb-0">
                                                                <i class="ph ph-target"></i>Expected answer
                                                            </label>
                                                            <textarea id="expectedAnswer${question.id}" name="expectedAnswer" maxlength="500" rows="5" class="form-control gape-paragraph-expected-editor px-14 py-10 text-14 bg-white border-neutral-30 border rounded-8" placeholder="Expected answer text" ${canEditAssessmentStructure ? '' : 'disabled'}><c:out value="${question.expectedAnswer}"/></textarea>
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${question.typeValue == 'file_upload'}">
                                                        <div class="gape-file-upload-answer mt-14">
                                                            <span class="w-40 h-40 rounded-8 bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                                <i class="ph ph-upload-simple"></i>
                                                            </span>
                                                            <span class="text-14 text-neutral-600">Student file upload. Accepted file types are configured next to Score.</span>
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${question.typeValue == 'rating'}">
                                                        <div class="gape-expected-answer-row">
                                                            <span class="gape-expected-answer-label">
                                                                <i class="ph ph-target"></i>Expected answer
                                                            </span>
                                                            <div class="gape-rating-builder"
                                                                 data-rating-builder
                                                                 data-rating-style="${question.ratingStyle}"
                                                                 data-rating-step="${question.ratingStep}"
                                                                 data-rating-max="${question.ratingMax}">
                                                                <input id="expectedRatingValue${question.id}"
                                                                       form="questionForm${question.id}"
                                                                       name="expectedRatingValue"
                                                                       type="hidden"
                                                                       value="${question.ratingExpectedValue}"
                                                                       data-rating-expected-input
                                                                       data-question-id="${question.id}"
                                                                       ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                                <div class="gape-rating-config-grid">
                                                                    <div class="d-flex align-items-end gap-10 flex-wrap">
                                                                        <div class="gape-rating-picker"
                                                                             data-rating-picker
                                                                             data-rating-style="${question.ratingStyle}"
                                                                             data-rating-step="${question.ratingStep}"
                                                                             data-rating-value="${question.ratingExpectedValue}"
                                                                             data-rating-max="${question.ratingMax}"
                                                                             data-question-id="${question.id}"
                                                                             data-rating-readonly="${canEditAssessmentStructure ? 'false' : 'true'}"
                                                                             aria-label="Expected rating value"></div>
                                                                        <span class="gape-rating-readout" data-rating-readout>${question.ratingExpectedValue} / ${question.ratingMax}</span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="gape-expected-answer-row">
                                                            <label for="expectedAnswer${question.id}" class="gape-expected-answer-label mb-0">
                                                                <i class="ph ph-target"></i>Expected answer
                                                            </label>
                                                            <input id="expectedAnswer${question.id}" name="expectedAnswer" maxlength="500" value="<c:out value='${question.expectedAnswer}'/>" class="form-control gape-short-expected-input px-14 py-10 text-14 bg-white border-neutral-30 border rounded-8" placeholder="Expected answer text" ${canEditAssessmentStructure ? '' : 'disabled'}>
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                            </article>
                        </c:forEach>

                        <c:if test="${empty questions}">
                            <section class="gape-question-card px-32 py-40 text-center">
                                <span class="d-inline-flex w-60 h-60 rounded-8 bg-main-50 text-main-600 align-items-center justify-content-center text-30 mb-16">
                                    <i class="ph ph-seal-question"></i>
                                </span>
                                <h3 class="text-20 fw-semibold text-neutral-800 mb-8">No questions yet</h3>
                                <p class="text-14 text-neutral-500 mb-18">Add one of the supported question types to start building the assessment.</p>
                                <c:if test="${canEditAssessmentStructure}">
                                    <button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03" data-bs-toggle="modal" data-bs-target="#addQuestionModal">
                                        <i class="ph ph-plus-circle me-8"></i>Add Question
                                    </button>
                                </c:if>
                            </section>
                        </c:if>
                    </main>
                </div>
                    </div>
                </section>

                <section class="ad-tab-panel" id="enrollments" data-ad-panel="enrollments" role="tabpanel" hidden>
                    <div class="ad-surface px-24 py-24 mb-20">
                        <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                            <div>
                                <h3 class="text-18 fw-medium text-neutral-700 mb-4">Enrollments</h3>
                                <span class="text-14 text-neutral-500">Students eligible through the assessment class group or subject.</span>
                            </div>
                            <div class="d-flex align-items-center gap-8 flex-wrap">
                                <span class="bg-success-50 text-success-600 px-14 py-8 rounded-pill text-13">${fn:length(activeAssessmentEnrollments)} active</span>
                                <c:if test="${not empty pendingAssessmentEnrollments}">
                                    <span class="bg-warning-30 text-warning-600 px-14 py-8 rounded-pill text-13">
                                        <i class="ph ph-bell-ringing me-6"></i>${fn:length(pendingAssessmentEnrollments)} new
                                    </span>
                                </c:if>
                            </div>
                        </div>

                        <c:if test="${canManageAssessmentEnrollments}">
                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollment-policy" method="post" class="ad-enrollment-policy-form d-flex align-items-center gap-10 flex-wrap mb-20">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                <span class="text-14 text-neutral-600 fw-medium">Enrollment policy</span>
                                <select name="approvalMode" class="form-select px-14 py-8 text-13 bg-neutral-20 border-neutral-30 border rounded-8" style="max-width: 210px;">
                                    <option value="auto_approve" <c:if test="${assessment.enrollmentModeValue eq 'auto_approve'}">selected</c:if>>Automatic enrollment</option>
                                    <option value="manual" <c:if test="${assessment.enrollmentModeValue eq 'manual'}">selected</c:if>>Manual enrollment</option>
                                </select>
                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--neutral" title="Save policy" aria-label="Save enrollment policy">
                                    <i class="ph ph-floppy-disk"></i>
                                </button>
                            </form>
                        </c:if>

                        <c:if test="${showAssessmentEnrollmentRequests}">
                            <div class="ad-subtab-list mb-18" role="tablist" aria-label="Enrollment sections">
                                <button type="button" class="ad-subtab-button is-active" data-ad-enrollment-tab="requests" role="tab" aria-selected="true">
                                    <i class="ph ph-bell-ringing"></i>Requests
                                </button>
                                <button type="button" class="ad-subtab-button" data-ad-enrollment-tab="enrollments" role="tab" aria-selected="false">
                                    <i class="ph ph-users-three"></i>Enrollments
                                </button>
                            </div>

                            <div class="ad-enrollment-subpanel" data-ad-enrollment-panel="requests" role="tabpanel">
                                <div class="alert alert-warning mb-20" role="alert">
                                    <i class="ph ph-warning-circle me-8"></i>There are pending enrollment requests waiting for approval.
                                </div>
                                <div class="ad-table-wrap">
                                    <table class="table mb-0 ad-data-table">
                                        <thead>
                                        <tr>
                                            <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Student</th>
                                            <th class="py-14 px-16 text-14 fw-medium text-neutral-600">State</th>
                                            <th class="py-14 px-16 text-14 fw-medium text-neutral-600 text-end">Management</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <c:forEach var="enrollment" items="${pendingAssessmentEnrollments}">
                                            <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                                <td class="py-16 px-16" data-label="Student">
                                                    <span class="fw-medium text-14 text-neutral-700"><c:out value="${enrollment.studentName}"/></span>
                                                    <span class="d-block text-12 text-neutral-500"><c:out value="${enrollment.studentEmail}"/></span>
                                                </td>
                                                <td class="py-16 px-16" data-label="State">
                                                    <span class="${enrollment.stateBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12">
                                                        <c:out value="${enrollment.stateLabel}"/>
                                                    </span>
                                                </td>
                                                <td class="py-16 px-16 text-end" data-label="Management">
                                                    <c:if test="${canManageAssessmentEnrollments}">
                                                        <div class="d-inline-flex align-items-center gap-8 flex-wrap justify-content-end">
                                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/approve" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--success" title="Approve enrollment" aria-label="Approve enrollment">
                                                                    <i class="ph ph-check"></i>
                                                                </button>
                                                            </form>
                                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/reject" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--danger" title="Reject enrollment" aria-label="Reject enrollment">
                                                                    <i class="ph ph-x"></i>
                                                                </button>
                                                            </form>
                                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/delete" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--delete" title="Delete enrollment" aria-label="Delete enrollment">
                                                                    <i class="ph ph-trash"></i>
                                                                </button>
                                                            </form>
                                                        </div>
                                                    </c:if>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </c:if>

                        <div class="ad-enrollment-subpanel" data-ad-enrollment-panel="enrollments" role="tabpanel" <c:if test="${showAssessmentEnrollmentRequests}">hidden</c:if>>
                            <c:if test="${canManageAssessmentEnrollments and showAssessmentEnrollmentCreate}">
                                <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments" method="post" class="ad-enrollment-create-form border border-neutral-30 rounded-8 px-18 py-18 mb-20">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                    <div class="ad-enrollment-create-grid">
                                        <div class="gape-select-field">
                                            <label for="assessmentStudentUserId" class="fw-medium text-base text-neutral-800 mb-12">Student</label>
                                            <select id="assessmentStudentUserId" name="studentUserId" required class="form-select px-20 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                                <option value="">Select student</option>
                                                <c:forEach var="student" items="${assessmentStudentOptions}">
                                                    <option value="${student.id}"><c:out value="${student.name}"/> - <c:out value="${student.email}"/></option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--primary ad-enrollment-create-submit" title="Enroll student" aria-label="Enroll student">
                                            <i class="ph ph-user-plus"></i>
                                        </button>
                                    </div>
                                </form>
                            </c:if>
                            <div class="ad-table-wrap">
                                <table class="table mb-0 ad-data-table">
                                    <thead>
                                    <tr>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">Student</th>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600">State</th>
                                        <th class="py-14 px-16 text-14 fw-medium text-neutral-600 text-end">Management</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="enrollment" items="${managedAssessmentEnrollments}">
                                        <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                            <td class="py-16 px-16" data-label="Student">
                                                <span class="fw-medium text-14 text-neutral-700"><c:out value="${enrollment.studentName}"/></span>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${enrollment.studentEmail}"/></span>
                                            </td>
                                            <td class="py-16 px-16" data-label="State">
                                                <span class="${enrollment.stateBadgeClass} px-12 py-6 border-neutral-30 border rounded-pill text-12">
                                                    <c:out value="${enrollment.stateLabel}"/>
                                                </span>
                                            </td>
                                            <td class="py-16 px-16 text-end" data-label="Management">
                                                <c:if test="${canManageAssessmentEnrollments}">
                                                    <div class="d-inline-flex align-items-center gap-8 flex-wrap justify-content-end">
                                                        <c:if test="${enrollment.active}">
                                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/withdraw" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--warning" title="Deactivate enrollment" aria-label="Deactivate enrollment">
                                                                    <i class="ph ph-user-minus"></i>
                                                                </button>
                                                            </form>
                                                        </c:if>
                                                        <c:if test="${enrollment.reactivateAvailable}">
                                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/update" method="post" class="m-0">
                                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                                <input type="hidden" name="state" value="active">
                                                                <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--success" title="Reactivate enrollment" aria-label="Reactivate enrollment">
                                                                    <i class="ph ph-user-plus"></i>
                                                                </button>
                                                            </form>
                                                        </c:if>
                                                        <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/enrollments/${enrollment.studentUserId}/delete" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#enrollments">
                                                            <button type="submit" class="ad-enrollment-icon-button ad-enrollment-icon-button--delete" title="Delete enrollment" aria-label="Delete enrollment">
                                                                <i class="ph ph-trash"></i>
                                                            </button>
                                                        </form>
                                                    </div>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty managedAssessmentEnrollments}">
                                        <tr>
                                            <td colspan="3" class="py-32 px-16 text-center text-14 text-neutral-500">No students enrolled in this assessment yet.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="ad-tab-panel" id="attempts" data-ad-panel="attempts" role="tabpanel" hidden>
                    <div class="ad-surface px-24 py-24">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                            <div>
                                <h3 class="text-18 fw-medium text-neutral-700 mb-4">Attempts</h3>
                                <span class="text-14 text-neutral-500"><c:out value="${assessment.attemptCountLabel}"/> for this assessment.</span>
                            </div>
                        </div>
                        <div class="ad-table-wrap">
                            <table class="table mb-0 ad-data-table">
                                <thead>
                                <tr>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Student</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Attempt</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Started</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Submitted</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Score</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                    <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="attempt" items="${attempts}">
                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-20 px-20" data-label="Student">
                                            <span class="d-block text-14 fw-semibold text-neutral-700"><c:out value="${attempt.studentName}"/></span>
                                            <span class="d-block text-12 text-neutral-500"><c:out value="${attempt.studentEmail}"/></span>
                                        </td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-label="Attempt">#${attempt.attemptNumber}</td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-label="Started"><c:out value="${attempt.startedAt}"/></td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-label="Submitted"><c:out value="${attempt.submittedAt}"/></td>
                                        <td class="py-20 px-20 text-14 text-neutral-500" data-label="Score" data-ad-attempt-score="${attempt.id}"><c:out value="${attempt.scoreOverMaxLabel}"/></td>
                                        <td class="py-20 px-20" data-label="State">
                                            <span class="${attempt.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13" data-ad-attempt-state="${attempt.id}">
                                                <c:out value="${attempt.stateLabel}"/>
                                            </span>
                                        </td>
                                        <td class="py-20 px-20 text-end" data-label="Actions">
                                            <c:choose>
                                                <c:when test="${attempt.correctionOpen}">
                                                    <div class="d-inline-flex align-items-center gap-8 justify-content-end">
                                                        <button type="button" class="ad-enrollment-icon-button ad-enrollment-icon-button--neutral" title="Review answers" aria-label="Review answers" data-bs-toggle="modal" data-bs-target="#correctAttemptModal${attempt.id}">
                                                            <i class="ph ph-eye"></i>
                                                        </button>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-13 text-neutral-400">Waiting submission</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty attempts}">
                                    <tr>
                                        <td colspan="7" class="py-40 px-20 text-center text-14 text-neutral-500">No attempts have been started yet.</td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<div class="modal fade" id="assessmentSetupModal" tabindex="-1" aria-labelledby="assessmentSetupTitle" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content rounded-8 border-0">
            <div class="modal-header border-neutral-30">
                <div>
                    <h5 class="modal-title text-18 fw-semibold mb-4" id="assessmentSetupTitle">Setup</h5>
                    <span class="text-13 text-neutral-500">Type, correction, enrolment, grading and availability settings.</span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="d-flex align-items-center gap-10 flex-wrap mb-18">
                    <span class="${assessment.stateBadgeClass} px-14 py-7 border-neutral-30 border rounded-pill text-13">
                        <c:out value="${assessment.stateLabel}"/>
                    </span>
                    <span class="bg-main-50 text-main-600 px-14 py-7 rounded-pill text-13">
                        <i class="ph ph-clock me-6"></i><c:out value="${assessment.availabilityLabel}"/>
                    </span>
                </div>
                <div class="ad-info-grid">
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Type</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.typeLabel}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Mode</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.modeLabel}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Correction</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.correctionModeLabel}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Enrollment</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.enrollmentModeLabel}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Max grade</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.maxGrade}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Passing grade</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.passingGrade}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Final weight</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.finalGradeWeight}"/>%</strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Attempts</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.attemptsLimitLabel}"/></strong></div>
                    <div class="ad-info-cell"><span class="text-12 text-neutral-500 d-block mb-6">Questions</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.questionCountLabel}"/></strong></div>
                    <div class="ad-info-cell ad-info-cell--wide"><span class="text-12 text-neutral-500 d-block mb-6">Context</span><strong class="text-14 text-neutral-800"><c:out value="${assessment.contextLabel}"/></strong></div>
                </div>
            </div>
        </div>
    </div>
</div>

<c:forEach var="attempt" items="${attempts}">
    <c:if test="${attempt.correctionOpen}">
        <c:set var="attemptResponses" value="${responsesByAttemptId[attempt.id]}"/>
        <div class="modal fade ad-correction-modal" id="correctAttemptModal${attempt.id}" tabindex="-1" aria-labelledby="correctAttemptTitle${attempt.id}" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable ad-correction-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <div class="min-w-0">
                            <h5 class="modal-title text-20 fw-semibold text-neutral-800 mb-4" id="correctAttemptTitle${attempt.id}">Correct Attempt</h5>
                            <span class="text-13 text-neutral-500">
                                Attempt #${attempt.attemptNumber} by <c:out value="${attempt.studentName}"/>
                            </span>
                        </div>
                        <button type="button" class="btn-close flex-shrink-0" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="ad-correction-summary-grid">
                            <div class="ad-correction-summary-card">
                                <span class="text-12 text-neutral-500 mb-4">Student</span>
                                <strong class="text-14 text-neutral-800"><c:out value="${attempt.studentName}"/></strong>
                            </div>
                            <div class="ad-correction-summary-card">
                                <span class="text-12 text-neutral-500 mb-4">State</span>
                                <span class="${attempt.stateBadgeClass} px-10 py-6 rounded-pill text-12 d-inline-block mt-2" data-ad-attempt-state="${attempt.id}"><c:out value="${attempt.stateLabel}"/></span>
                            </div>
                            <div class="ad-correction-summary-card">
                                <span class="text-12 text-neutral-500 mb-4">Score</span>
                                <strong class="text-14 text-neutral-800" data-ad-attempt-score="${attempt.id}"><c:out value="${attempt.scoreOverMaxLabel}"/></strong>
                            </div>
                            <div class="ad-correction-summary-card">
                                <span class="text-12 text-neutral-500 mb-4">Correction</span>
                                <strong class="text-14 text-neutral-800"><c:out value="${assessment.correctionModeLabel}"/></strong>
                            </div>
                        </div>

                        <form id="manualCorrectionForm${attempt.id}" action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/manual-correct" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#attempts">
                        </form>

                        <c:choose>
                            <c:when test="${empty attemptResponses}">
                                <div class="bg-white rounded-8 px-24 py-40 border border-neutral-30 text-center text-14 text-neutral-500">
                                    No responses were saved for this attempt.
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="ad-correction-question-list">
                                    <c:forEach var="response" items="${attemptResponses}" varStatus="responseStatus">
                                        <article class="ad-correction-question-card">
                                            <div class="ad-correction-question-header">
                                                <div class="ad-correction-question-heading">
                                                    <span class="ad-correction-question-label">Question ${responseStatus.count}</span>
                                                    <span class="ad-correction-question-meta"><c:out value="${response.question.typeLabel}"/> | <c:out value="${response.question.score}"/> pts</span>
                                                    <span class="ad-correction-required-pill ${response.question.required ? '' : 'is-optional'}"><c:out value="${response.question.requiredLabel}"/></span>
                                                </div>
                                                <span class="ad-correction-score-pill ${response.scored ? 'bg-success-50 text-success-600' : 'bg-warning-30 text-warning-600'} px-12 py-7 rounded-pill text-12 flex-shrink-0">
                                                    <c:out value="${response.scoreLabel}"/>
                                                </span>
                                            </div>

                                            <p class="ad-correction-statement"><c:out value="${response.question.statement}"/></p>

                                            <div class="ad-correction-answer">
                                                <div class="ad-correction-answer-block">
                                                    <span class="ad-correction-answer-label">Student answer</span>
                                                    <c:choose>
                                                        <c:when test="${response.question.allowsOptions}">
                                                            <div class="ad-correction-choice-list">
                                                                <c:forEach var="option" items="${response.question.activeOptions}">
                                                                    <c:set var="optionSelected" value="${fn:contains(response.selectedOptionTokens, option.idToken)}"/>
                                                                    <c:set var="optionExpected" value="${fn:contains(response.expectedOptionTokens, option.idToken)}"/>
                                                                    <c:choose>
                                                                        <c:when test="${response.objectiveWithExpectedAnswer and optionSelected and optionExpected}">
                                                                            <c:set var="choiceTone" value="is-correct"/>
                                                                        </c:when>
                                                                        <c:when test="${response.objectiveWithExpectedAnswer and optionSelected}">
                                                                            <c:set var="choiceTone" value="is-incorrect"/>
                                                                        </c:when>
                                                                        <c:when test="${response.objectiveWithExpectedAnswer and optionExpected}">
                                                                            <c:set var="choiceTone" value="is-missed"/>
                                                                        </c:when>
                                                                        <c:when test="${optionSelected}">
                                                                            <c:set var="choiceTone" value="is-selected"/>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <c:set var="choiceTone" value=""/>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                    <div class="ad-correction-choice ${choiceTone}">
                                                                        <span class="ad-correction-choice-control ${response.question.singleSelectedOption ? 'ad-correction-choice-control--radio' : 'ad-correction-choice-control--checkbox'}" aria-hidden="true"></span>
                                                                        <span class="ad-correction-choice-text"><c:out value="${option.text}"/></span>
                                                                    </div>
                                                                </c:forEach>
                                                            </div>
                                                        </c:when>
                                                        <c:when test="${response.question.textAnswer}">
                                                            <div class="ad-correction-text-answer ${response.question.paragraph ? 'ad-correction-text-answer--long' : ''}"><c:out value="${response.displayAnswer}"/></div>
                                                        </c:when>
                                                        <c:when test="${response.question.fileUpload and response.hasAttachment}">
                                                            <div class="ad-correction-upload-row">
                                                                <a class="ad-correction-upload-answer" href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/responses/${response.id}/attachment">
                                                                    <i class="ph ph-upload-simple"></i>Upload
                                                                </a>
                                                                <span class="ad-correction-upload-file"><c:out value="${response.attachmentFileName}"/></span>
                                                            </div>
                                                        </c:when>
                                                        <c:when test="${response.question.fileUpload}">
                                                            <div class="ad-correction-text-answer">No file uploaded.</div>
                                                        </c:when>
                                                        <c:when test="${response.question.typeValue == 'rating'}">
                                                            <div class="ad-correction-rating ${response.objectiveAnswerToneClass}" aria-label="${fn:escapeXml(response.displayAnswer)}">
                                                                <c:forEach var="ratingUnit" begin="1" end="${response.question.ratingDisplayMax}">
                                                                    <i class="${ratingUnit <= response.ratingFilledUnits ? 'ph-fill ph-star' : 'ph ph-star'}"></i>
                                                                </c:forEach>
                                                            </div>
                                                            <span class="text-13 text-neutral-500 d-block mt-8"><c:out value="${response.displayAnswer}"/> / ${response.question.ratingMax}</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <div class="ad-correction-text-answer"><c:out value="${response.displayAnswer}"/></div>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    <c:if test="${not empty response.answeredAt}">
                                                        <span class="text-12 text-neutral-500 d-block mt-8">Answered at <c:out value="${response.answeredAt}"/></span>
                                                    </c:if>
                                                </div>
                                                <c:if test="${response.hasExpectedAnswer}">
                                                    <button type="button" class="ad-outline-button ad-correction-expected-toggle" data-ad-expected-toggle aria-expanded="false" aria-controls="expectedAnswer${response.id}">
                                                        <i class="ph ph-eye me-8" data-ad-expected-toggle-icon></i><span data-ad-expected-toggle-label>Show expected answer</span>
                                                    </button>
                                                    <div class="ad-correction-expected-answer" id="expectedAnswer${response.id}" data-ad-expected-panel hidden>
                                                        <span class="ad-correction-answer-label">Expected answer</span>
                                                        <c:choose>
                                                            <c:when test="${response.question.allowsOptions}">
                                                                <div class="ad-correction-choice-list">
                                                                    <c:forEach var="option" items="${response.question.activeOptions}">
                                                                        <c:if test="${fn:contains(response.expectedOptionTokens, option.idToken)}">
                                                                            <div class="ad-correction-choice is-expected">
                                                                                <span class="ad-correction-choice-control ${response.question.singleSelectedOption ? 'ad-correction-choice-control--radio' : 'ad-correction-choice-control--checkbox'}" aria-hidden="true"></span>
                                                                                <span class="ad-correction-choice-text"><c:out value="${option.text}"/></span>
                                                                            </div>
                                                                        </c:if>
                                                                    </c:forEach>
                                                                </div>
                                                            </c:when>
                                                            <c:when test="${response.question.typeValue == 'rating'}">
                                                                <div class="ad-correction-rating is-expected" aria-label="${fn:escapeXml(response.expectedDisplayAnswer)}">
                                                                    <c:forEach var="ratingUnit" begin="1" end="${response.question.ratingDisplayMax}">
                                                                        <i class="${ratingUnit <= response.expectedRatingFilledUnits ? 'ph-fill ph-star' : 'ph ph-star'}"></i>
                                                                    </c:forEach>
                                                                </div>
                                                                <span class="text-13 text-neutral-600 d-block mt-8"><c:out value="${response.expectedDisplayAnswer}"/></span>
                                                            </c:when>
                                                            <c:when test="${response.question.paragraph}">
                                                                <div class="ad-correction-text-answer ad-correction-text-answer--expected ad-correction-text-answer--long"><c:out value="${response.expectedDisplayAnswer}"/></div>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <div class="ad-correction-text-answer ad-correction-text-answer--expected"><c:out value="${response.expectedDisplayAnswer}"/></div>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </c:if>
                                            </div>

                                            <div class="ad-correction-controls">
                                                <div class="d-flex align-items-center gap-10 flex-wrap">
                                                    <c:if test="${response.objectiveWithExpectedAnswer}">
                                                        <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/responses/${response.id}/auto-correct" method="post" class="m-0" data-ad-auto-correct-form data-ad-auto-correct-scope="response" data-ad-attempt-id="${attempt.id}">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#attempts">
                                                            <button type="submit" class="ad-outline-button ad-correction-auto-button" aria-label="Auto correct response" data-ad-auto-correct-button>
                                                                <i class="ph ph-magic-wand me-8"></i>Auto Correct
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                                <div class="ad-correction-score-field">
                                                    <label for="responseScore${response.id}" class="text-13 fw-medium text-neutral-700 mb-8">Manual score</label>
                                                    <input id="responseScore${response.id}" name="score_${response.id}" form="manualCorrectionForm${attempt.id}" type="text" inputmode="decimal" autocomplete="off" required value="<c:out value='${response.score}'/>" class="form-control px-14 py-10 text-14 bg-white border-neutral-30 border rounded-8 ad-correction-score-input" data-ad-response-score-input="${response.id}" data-ad-score-max="${response.question.score}" aria-describedby="responseScoreStatus${response.id} responseScoreError${response.id}">
                                                    <span id="responseScoreStatus${response.id}" class="${response.scored ? 'text-success-600' : 'text-warning-600'} text-12 d-block mt-6" data-ad-response-score-label="${response.id}"><c:out value="${response.scoreLabel}"/></span>
                                                    <span id="responseScoreError${response.id}" class="ad-correction-score-error" data-ad-response-score-error="${response.id}" aria-live="polite"></span>
                                                </div>
                                            </div>
                                        </article>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="modal-footer">
                        <div class="ad-correction-footer-actions">
                            <span class="ad-correction-feedback" data-ad-correction-feedback="${attempt.id}" aria-live="polite"></span>
                            <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/pdf" class="ad-outline-button ad-correction-download-button" title="Download" aria-label="Download attempt PDF">
                                <i class="ph ph-download-simple me-8"></i>Download
                            </a>
                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/auto-correct-eligible" method="post" class="m-0" data-ad-auto-correct-form data-ad-auto-correct-scope="attempt" data-ad-attempt-id="${attempt.id}">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="/learning/assessments/${assessment.id}#attempts">
                                <button type="submit" class="ad-outline-button ad-correction-auto-button" <c:if test="${empty attemptResponses}">disabled</c:if> data-ad-auto-correct-button>
                                    <i class="ph ph-magic-wand me-8"></i>Auto all legible
                                </button>
                            </form>
                            <button type="submit" form="manualCorrectionForm${attempt.id}" class="ad-primary-button ad-correction-submit-button" <c:if test="${empty attemptResponses}">disabled</c:if> data-ad-correction-submit-button>Submit Correction</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
</c:forEach>

<c:if test="${canEditAssessmentStructure}">
<div class="modal fade" id="addQuestionModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered gape-add-question-dialog">
        <div class="modal-content rounded-10 border-0 gape-add-question-modal">
            <div class="modal-header border-bottom-dashed">
                <div>
                    <h5 class="modal-title text-18 fw-semibold text-neutral-800">Add Question</h5>
                    <span class="text-13 text-neutral-500">Choose a supported question type.</span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="gape-add-question-shell">
                    <aside class="gape-add-question-side">
                        <input type="search" class="form-control px-14 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 mb-14" placeholder="Search type" data-question-type-search>
                        <div class="gape-add-question-categories d-flex flex-column gap-8" data-question-category-list>
                            <button type="button" class="gape-question-category is-active" data-question-category="all"><i class="ph ph-squares-four"></i>All types</button>
                            <button type="button" class="gape-question-category" data-question-category="choice"><i class="ph ph-check-square"></i>Multiple choice</button>
                            <button type="button" class="gape-question-category" data-question-category="text"><i class="ph ph-text-aa"></i>Text</button>
                            <button type="button" class="gape-question-category" data-question-category="file"><i class="ph ph-upload-simple"></i>File upload</button>
                            <button type="button" class="gape-question-category" data-question-category="rating"><i class="ph ph-star"></i>Rating</button>
                        </div>
                    </aside>
                    <section class="gape-add-question-main">
                        <div class="gape-question-type-grid" data-question-type-grid>
                            <c:set var="questionTypeSpecs" value="single_choice|Single choice|ph ph-radio-button|choice|single|One correct answer;multiple_choice|Multiple choice|ph ph-check-square|choice|multi|Several correct answers;short_text|Short text|ph ph-text-aa|text|short|Short typed answer;paragraph|Paragraph|ph ph-text-align-left|text|paragraph|Long typed answer;file_upload|File upload|ph ph-upload-simple|file|file|Attach a file;rating|Rating|ph ph-star|rating|rating|Rating scale with expected value"/>
                            <c:forEach var="spec" items="${fn:split(questionTypeSpecs, ';')}">
                                <c:set var="parts" value="${fn:split(spec, '|')}"/>
                                <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/questions" method="post" data-question-type-card data-question-category="${parts[3]}" data-question-type-text="${parts[1]}">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <input type="hidden" name="type" value="${parts[0]}">
                                    <input type="hidden" name="required" value="true">
                                    <input type="hidden" name="score" value="1.00">
                                    <c:if test="${parts[0] == 'file_upload'}">
                                        <input type="hidden" name="acceptedFormat" value="pdf">
                                        <input type="hidden" name="acceptedFormat" value="archive">
                                    </c:if>
                                    <c:if test="${parts[0] == 'rating'}">
                                        <input type="hidden" name="ratingDesign" value="stars_integer">
                                        <input type="hidden" name="ratingMax" value="5">
                                        <input type="hidden" name="expectedRatingValue" value="5">
                                    </c:if>
                                    <button type="submit" class="gape-question-type-card px-18 py-18 w-100 bg-white">
                                        <span class="gape-question-type-card__preview" aria-hidden="true">
                                            <c:choose>
                                                <c:when test="${parts[4] == 'single'}">
                                                    <span class="gape-preview-choice">
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark is-active"></span><span class="gape-preview-choice__line is-active"></span></span>
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark"></span><span class="gape-preview-choice__line"></span></span>
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark"></span><span class="gape-preview-choice__line"></span></span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${parts[4] == 'multi'}">
                                                    <span class="gape-preview-choice">
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark is-check is-active"><i class="ph ph-check"></i></span><span class="gape-preview-choice__line is-active"></span></span>
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark is-check"></span><span class="gape-preview-choice__line"></span></span>
                                                        <span class="gape-preview-choice__row"><span class="gape-preview-choice__mark is-check is-active"><i class="ph ph-check"></i></span><span class="gape-preview-choice__line is-active"></span></span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${parts[4] == 'short'}">
                                                    <span class="gape-preview-input">Type here</span>
                                                </c:when>
                                                <c:when test="${parts[4] == 'paragraph'}">
                                                    <span class="gape-preview-input is-textarea">Type here</span>
                                                </c:when>
                                                <c:when test="${parts[4] == 'file'}">
                                                    <span class="gape-preview-upload"><i class="ph ph-upload-simple"></i>Upload</span>
                                                </c:when>
                                                <c:when test="${parts[4] == 'rating'}">
                                                    <span class="gape-preview-rating">
                                                        <i class="ph-fill ph-star"></i><i class="ph-fill ph-star"></i><i class="ph-fill ph-star"></i><i class="ph-fill ph-star"></i><i class="ph ph-star"></i>
                                                    </span>
                                                </c:when>
                                            </c:choose>
                                        </span>
                                        <span class="gape-question-type-card__body">
                                            <span class="d-block text-15 fw-semibold text-neutral-800 mb-5"><c:out value="${parts[1]}"/></span>
                                            <span class="d-block text-12 text-neutral-500"><c:out value="${parts[5]}"/></span>
                                        </span>
                                    </button>
                                </form>
                            </c:forEach>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        function ready(callback) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', callback);
                return;
            }
            callback();
        }

        function normalize(value) {
            return (value || '').toLowerCase();
        }

        ready(function () {
            var tabs = Array.prototype.slice.call(document.querySelectorAll('[data-ad-tab]'));
            var panels = Array.prototype.slice.call(document.querySelectorAll('[data-ad-panel]'));
            if (!tabs.length || !panels.length) {
                return;
            }

            function panelFromHash(hash) {
                var value = (hash || '').replace(/^#/, '');
                if (value === 'enrollments' || value === 'enrollments-attempts' || value === 'assessment-settings') {
                    return 'enrollments';
                }
                if (value === 'attempts' || value === 'assessment-attempts' || value === 'settings-attempts') {
                    return 'attempts';
                }
                return value === 'builder' ? 'builder' : 'builder';
            }

            function activatePanel(name, updateHash) {
                tabs.forEach(function (tab) {
                    var active = tab.dataset.adTab === name;
                    tab.classList.toggle('is-active', active);
                    tab.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                panels.forEach(function (panel) {
                    panel.hidden = panel.dataset.adPanel !== name;
                });
                if (updateHash && window.history && window.history.replaceState) {
                    window.history.replaceState(null, '', window.location.pathname + window.location.search + '#' + name);
                }
            }

            tabs.forEach(function (tab) {
                tab.addEventListener('click', function () {
                    activatePanel(tab.dataset.adTab || 'builder', true);
                });
            });
            window.addEventListener('hashchange', function () {
                activatePanel(panelFromHash(window.location.hash), false);
            });
            var initialPanel = window.location.hash ? panelFromHash(window.location.hash) : 'builder';
            activatePanel(initialPanel, false);
        });

        ready(function () {
            var tabs = Array.prototype.slice.call(document.querySelectorAll('[data-ad-enrollment-tab]'));
            var panels = Array.prototype.slice.call(document.querySelectorAll('[data-ad-enrollment-panel]'));
            if (!tabs.length || !panels.length) {
                return;
            }

            function activateEnrollmentPanel(name) {
                tabs.forEach(function (tab) {
                    var active = tab.dataset.adEnrollmentTab === name;
                    tab.classList.toggle('is-active', active);
                    tab.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                panels.forEach(function (panel) {
                    panel.hidden = panel.dataset.adEnrollmentPanel !== name;
                });
            }

            tabs.forEach(function (tab) {
                tab.addEventListener('click', function () {
                    activateEnrollmentPanel(tab.dataset.adEnrollmentTab || 'enrollments');
                });
            });
            activateEnrollmentPanel(tabs[0].dataset.adEnrollmentTab || 'enrollments');
        });

        ready(function () {
            Array.prototype.slice.call(document.querySelectorAll('[data-ad-expected-toggle]')).forEach(function (button) {
                var panel = document.getElementById(button.getAttribute('aria-controls') || '');
                var label = button.querySelector('[data-ad-expected-toggle-label]');
                var icon = button.querySelector('[data-ad-expected-toggle-icon]');
                if (!panel || !label) {
                    return;
                }

                function setExpectedAnswerVisible(visible) {
                    panel.hidden = !visible;
                    button.setAttribute('aria-expanded', visible ? 'true' : 'false');
                    label.textContent = visible ? 'Hide expected answer' : 'Show expected answer';
                    if (icon) {
                        icon.className = visible ? 'ph ph-eye-slash me-8' : 'ph ph-eye me-8';
                    }
                }

                setExpectedAnswerVisible(false);
                button.addEventListener('click', function () {
                    setExpectedAnswerVisible(button.getAttribute('aria-expanded') !== 'true');
                });
            });
        });

        ready(function () {
            var inputs = Array.prototype.slice.call(document.querySelectorAll('[data-ad-response-score-input]'));
            var submitButtons = Array.prototype.slice.call(document.querySelectorAll('[data-ad-correction-submit-button]'));
            if (!inputs.length) {
                return;
            }

            submitButtons.forEach(function (button) {
                if (button.disabled) {
                    button.dataset.adInitialDisabled = 'true';
                }
            });

            function parseScore(value) {
                var raw = (value || '').trim();
                if (!raw) {
                    return { valid: false, message: 'Manual score is required.' };
                }
                if (raw.indexOf('-') !== -1) {
                    return { valid: false, message: 'Manual score cannot be negative.' };
                }
                if (!/^\d+(?:[\.,]\d+)?$/.test(raw)) {
                    return { valid: false, message: 'Use a non-negative number, for example 5, 3,545 or 1.76.' };
                }
                var normalized = raw.replace(',', '.');
                var number = Number(normalized);
                if (!Number.isFinite(number)) {
                    return { valid: false, message: 'Use a valid number.' };
                }
                return { valid: true, normalized: normalized, number: number };
            }

            function maxScore(input) {
                var parsed = parseScore(input.dataset.adScoreMax || '');
                return parsed.valid ? parsed.number : Number.POSITIVE_INFINITY;
            }

            function setScoreError(input, message) {
                var error = document.querySelector('[data-ad-response-score-error="' + input.dataset.adResponseScoreInput + '"]');
                var showMessage = input.dataset.adTouched === 'true';
                if (error) {
                    error.textContent = showMessage ? (message || '') : '';
                }
                input.setCustomValidity(message || '');
                input.classList.toggle('is-invalid', Boolean(message) && showMessage);
            }

            function validateInput(input) {
                var parsed = parseScore(input.value);
                var message = parsed.valid ? '' : parsed.message;
                if (!message && parsed.number > maxScore(input)) {
                    message = 'Manual score cannot exceed ' + (input.dataset.adScoreMax || 'the question score') + '.';
                }
                setScoreError(input, message);
                return !message;
            }

            function formInputs(formId) {
                return inputs.filter(function (input) {
                    return input.getAttribute('form') === formId;
                });
            }

            function markPendingScore(input) {
                input.dataset.adTouched = 'true';
                validateInput(input);
            }

            function focusPendingScore(input) {
                var card = input.closest('.ad-correction-question-card') || input;
                var modalBody = input.closest('.modal-content');
                if (card && typeof card.scrollIntoView === 'function') {
                    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
                if (modalBody && typeof modalBody.scrollIntoView === 'function') {
                    window.setTimeout(function () {
                        card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }, 60);
                }
                window.setTimeout(function () {
                    input.focus({ preventScroll: true });
                    if (typeof input.select === 'function') {
                        input.select();
                    }
                }, 220);
            }

            function firstPendingScore(form) {
                var firstInvalid = null;
                formInputs(form.id).forEach(function (input) {
                    markPendingScore(input);
                    if (!input.checkValidity() && !firstInvalid) {
                        firstInvalid = input;
                    }
                });
                updateSubmitButtons();
                return firstInvalid;
            }

            function updateSubmitButtons() {
                submitButtons.forEach(function (button) {
                    if (button.dataset.adInitialDisabled === 'true') {
                        return;
                    }
                    button.disabled = false;
                });
            }

            inputs.forEach(function (input) {
                input.addEventListener('input', function () {
                    input.dataset.adTouched = 'true';
                    validateInput(input);
                    updateSubmitButtons();
                });
                input.addEventListener('blur', function () {
                    input.dataset.adTouched = 'true';
                    validateInput(input);
                    updateSubmitButtons();
                });
                validateInput(input);
            });

            submitButtons.forEach(function (button) {
                var form = document.getElementById(button.getAttribute('form') || '');
                if (!form) {
                    return;
                }
                button.addEventListener('click', function (event) {
                    var firstInvalid = firstPendingScore(form);
                    if (firstInvalid) {
                        event.preventDefault();
                        focusPendingScore(firstInvalid);
                    }
                });
                form.addEventListener('submit', function (event) {
                    var firstInvalid = firstPendingScore(form);
                    if (firstInvalid) {
                        event.preventDefault();
                        focusPendingScore(firstInvalid);
                    }
                });
            });

            updateSubmitButtons();
        });

        ready(function () {
            var forms = Array.prototype.slice.call(document.querySelectorAll('[data-ad-auto-correct-form]'));
            if (!forms.length || !window.fetch) {
                return;
            }

            function setFeedback(attemptId, message, state) {
                var feedback = document.querySelector('[data-ad-correction-feedback="' + attemptId + '"]');
                if (!feedback) {
                    return;
                }
                feedback.textContent = message || '';
                feedback.classList.toggle('is-success', state === 'success');
                feedback.classList.toggle('is-error', state === 'error');
            }

            function setBusy(form, busy) {
                var button = form.querySelector('[data-ad-auto-correct-button]');
                if (!button) {
                    return;
                }
                button.disabled = busy;
                if (busy) {
                    button.dataset.originalText = button.innerHTML;
                    button.innerHTML = '<i class="ph ph-circle-notch me-8"></i>Working';
                } else if (button.dataset.originalText) {
                    button.innerHTML = button.dataset.originalText;
                    delete button.dataset.originalText;
                }
            }

            function updateAttempt(payload) {
                Array.prototype.slice.call(document.querySelectorAll('[data-ad-attempt-score="' + payload.attemptId + '"]')).forEach(function (target) {
                    target.textContent = payload.scoreOverMax || 'Not assigned yet';
                });
                Array.prototype.slice.call(document.querySelectorAll('[data-ad-attempt-state="' + payload.attemptId + '"]')).forEach(function (target) {
                    target.textContent = payload.state || target.textContent;
                });
            }

            function updateResponses(payload) {
                (payload.responses || []).forEach(function (item) {
                    var input = document.querySelector('[data-ad-response-score-input="' + item.id + '"]');
                    if (input) {
                        input.value = item.score || '';
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                    var label = document.querySelector('[data-ad-response-score-label="' + item.id + '"]');
                    if (label) {
                        label.textContent = item.scoreLabel || 'Not assigned yet';
                        label.classList.toggle('text-success-600', Boolean(item.score));
                        label.classList.toggle('text-warning-600', !item.score);
                    }
                });
            }

            function actionUrl(form) {
                var sessionMatch = window.location.pathname.match(/;jsessionid=([^/]+)/i);
                if (!sessionMatch) {
                    return form.action;
                }
                var url = new URL(form.action, window.location.href);
                if (url.pathname.indexOf(';jsessionid=') !== -1) {
                    return url.toString();
                }
                var contextPath = '${pageContext.request.contextPath}';
                if (contextPath && url.pathname.indexOf(contextPath + '/') === 0) {
                    url.pathname = contextPath + ';jsessionid=' + sessionMatch[1] + url.pathname.substring(contextPath.length);
                } else {
                    url.pathname = url.pathname.replace(/^\/([^\/]+)/, '/$1;jsessionid=' + sessionMatch[1]);
                }
                return url.toString();
            }

            forms.forEach(function (form) {
                form.addEventListener('submit', function (event) {
                    event.preventDefault();
                    var attemptId = form.dataset.adAttemptId || '';
                    var formData = new URLSearchParams(new FormData(form));
                    formData.set('autosave', 'true');
                    setFeedback(attemptId, 'Applying automatic scores...', '');
                    setBusy(form, true);
                    fetch(actionUrl(form), {
                        method: 'POST',
                        body: formData,
                        credentials: 'same-origin',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                            'X-Requested-With': 'XMLHttpRequest'
                        }
                    }).then(function (response) {
                        if (!response.ok) {
                            return response.text().then(function (message) {
                                throw new Error(message || 'Automatic correction failed.');
                            });
                        }
                        return response.json();
                    }).then(function (payload) {
                        updateAttempt(payload);
                        updateResponses(payload);
                        var corrected = payload.automaticallyCorrectedResponses || 0;
                        setFeedback(attemptId, corrected + ' eligible response' + (corrected === 1 ? '' : 's') + ' corrected.', 'success');
                    }).catch(function (error) {
                        setFeedback(attemptId, error.message || 'Automatic correction failed.', 'error');
                    }).finally(function () {
                        setBusy(form, false);
                    });
                });
            });
        });

        ready(function () {
            var typeSearch = document.querySelector('[data-question-type-search]');
            var typeCards = Array.prototype.slice.call(document.querySelectorAll('[data-question-type-card]'));
            var categoryButtons = Array.prototype.slice.call(document.querySelectorAll('[data-question-category-list] [data-question-category]'));
            var activeCategory = 'all';

            function filterTypes() {
                var query = normalize(typeSearch ? typeSearch.value : '');
                typeCards.forEach(function (card) {
                    var matchesCategory = activeCategory === 'all' || card.dataset.questionCategory === activeCategory;
                    var matchesText = normalize(card.dataset.questionTypeText).indexOf(query) !== -1;
                    card.classList.toggle('d-none', !(matchesCategory && matchesText));
                });
            }

            categoryButtons.forEach(function (button) {
                button.addEventListener('click', function () {
                    activeCategory = button.dataset.questionCategory || 'all';
                    categoryButtons.forEach(function (current) {
                        var active = current === button;
                        current.classList.toggle('is-active', active);
                    });
                    filterTypes();
                });
            });
            if (typeSearch) {
                typeSearch.addEventListener('input', filterTypes);
            }

            var builderRoot = document.querySelector('[data-assessment-builder-root]');
            var canEditBuilder = builderRoot && builderRoot.dataset.canEdit === 'true';
            var autosaveTimers = new Map();
            var minimumQuestionScore = 0.10;

            function parseDecimal(value) {
                var parsed = parseFloat(String(value || '').replace(',', '.'));
                return Number.isFinite(parsed) ? parsed : 0;
            }

            function decimalCents(value) {
                return Math.round(parseDecimal(value) * 100);
            }

            function formatDecimal(value) {
                return (Math.round(value * 100) / 100).toFixed(2);
            }

            function clampQuestionScore(input) {
                if (!input || !input.matches || !input.matches('[data-question-score-input]')) {
                    return;
                }
                if (parseDecimal(input.value) < minimumQuestionScore) {
                    input.value = formatDecimal(minimumQuestionScore);
                }
            }

            function autosaveError(message) {
                var error = document.querySelector('[data-autosave-error]');
                if (!error) {
                    return;
                }
                if (!message) {
                    error.classList.add('d-none');
                    error.textContent = '';
                    return;
                }
                error.textContent = message;
                error.classList.remove('d-none');
            }

            function updateScoreBalanceWarning() {
                if (!builderRoot) {
                    return false;
                }
                var warning = document.querySelector('[data-score-balance-warning]');
                if (!warning) {
                    return false;
                }
                var inputs = Array.prototype.slice.call(document.querySelectorAll('[data-question-score-input]'));
                inputs.forEach(function (input) {
                    clampQuestionScore(input);
                });
                var total = inputs.reduce(function (sum, input) {
                    return sum + parseDecimal(input.value);
                }, 0);
                var maxGrade = parseDecimal(builderRoot.dataset.maxGrade);
                var mismatch = inputs.length > 0 && decimalCents(total) !== decimalCents(maxGrade);
                var totalTarget = warning.querySelector('[data-score-total]');
                var maxTarget = warning.querySelector('[data-score-maximum]');
                if (totalTarget) {
                    totalTarget.textContent = formatDecimal(total);
                }
                if (maxTarget) {
                    maxTarget.textContent = formatDecimal(maxGrade);
                }
                warning.classList.toggle('d-none', !mismatch);
                return mismatch;
            }

            function questionForms() {
                return Array.prototype.slice.call(document.querySelectorAll('[data-question-form]'));
            }

            function formPayload(form) {
                var payload = new URLSearchParams(new FormData(form));
                payload.set('autosave', 'true');
                return payload;
            }

            function isFormValidForAutosave(form) {
                Array.prototype.slice.call(form.elements).forEach(function (input) {
                    if (!input.matches || !input.matches('[data-question-score-input]')) {
                        return;
                    }
                    clampQuestionScore(input);
                });
                return form.checkValidity();
            }

            function saveQuestionForm(form) {
                if (!canEditBuilder || !form || !form.matches('[data-question-form]')) {
                    return Promise.resolve(false);
                }
                if (!isFormValidForAutosave(form)) {
                    form.dataset.autosaveDirty = 'true';
                    return Promise.resolve(false);
                }
                var payload = formPayload(form);
                return fetch(form.action, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: payload.toString()
                }).then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            throw new Error(text || 'Question could not be saved.');
                        });
                    }
                    form.dataset.autosaveDirty = 'false';
                    autosaveError('');
                    return true;
                }).catch(function (error) {
                    autosaveError(error.message || 'Question could not be saved.');
                    throw error;
                });
            }

            function scheduleAutosave(form, delay) {
                if (!canEditBuilder || !form || !form.matches('[data-question-form]')) {
                    return;
                }
                form.dataset.autosaveDirty = 'true';
                if (autosaveTimers.has(form)) {
                    window.clearTimeout(autosaveTimers.get(form));
                }
                autosaveTimers.set(form, window.setTimeout(function () {
                    autosaveTimers.delete(form);
                    saveQuestionForm(form).catch(function () {
                        // The visible autosave error is set by saveQuestionForm.
                    });
                }, delay == null ? 700 : delay));
            }

            function flushAutosaves() {
                questionForms().forEach(function (form) {
                    if (autosaveTimers.has(form)) {
                        window.clearTimeout(autosaveTimers.get(form));
                        autosaveTimers.delete(form);
                    }
                });
                return Promise.allSettled(questionForms()
                        .filter(function (form) { return form.dataset.autosaveDirty === 'true'; })
                        .map(saveQuestionForm));
            }

            function sendQuestionFormBeacon(form) {
                if (!canEditBuilder || !form || form.dataset.autosaveDirty !== 'true' || !isFormValidForAutosave(form)) {
                    return;
                }
                if (navigator.sendBeacon) {
                    navigator.sendBeacon(form.action, formPayload(form));
                    return;
                }
                fetch(form.action, {
                    method: 'POST',
                    credentials: 'same-origin',
                    keepalive: true,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body: formPayload(form).toString()
                }).catch(function () {});
            }

            function rebalanceScores() {
                if (!canEditBuilder || !builderRoot || !updateScoreBalanceWarning()) {
                    return Promise.resolve(false);
                }
                var payload = new URLSearchParams();
                payload.set('csrfToken', builderRoot.dataset.csrfToken || '');
                return fetch(builderRoot.dataset.rebalanceUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: payload.toString()
                }).then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            throw new Error(text || 'Question scores could not be adjusted.');
                        });
                    }
                    return true;
                });
            }

            function sendRebalanceBeacon() {
                if (!canEditBuilder || !builderRoot || !updateScoreBalanceWarning()) {
                    return;
                }
                var payload = new URLSearchParams();
                payload.set('csrfToken', builderRoot.dataset.csrfToken || '');
                if (navigator.sendBeacon) {
                    navigator.sendBeacon(builderRoot.dataset.rebalanceUrl, payload);
                    return;
                }
                fetch(builderRoot.dataset.rebalanceUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    keepalive: true,
                    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                    body: payload.toString()
                }).catch(function () {});
            }

            function prepareNavigationFlush() {
                if (!canEditBuilder) {
                    return;
                }
                document.addEventListener('click', function (event) {
                    var link = event.target.closest('a[href]');
                    if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                        return;
                    }
                    if (link.target && link.target !== '_self') {
                        return;
                    }
                    var target = new URL(link.href, window.location.href);
                    if (target.origin === window.location.origin
                            && target.pathname === window.location.pathname
                            && target.search === window.location.search
                            && target.hash) {
                        return;
                    }
                    var hasDirtyForms = questionForms().some(function (form) {
                        return form.dataset.autosaveDirty === 'true';
                    });
                    if (!hasDirtyForms && !updateScoreBalanceWarning()) {
                        return;
                    }
                    event.preventDefault();
                    flushAutosaves()
                            .then(rebalanceScores)
                            .catch(function (error) {
                                autosaveError(error.message || 'Changes could not be saved.');
                            })
                            .finally(function () {
                                window.location.href = link.href;
                            });
                });
                window.addEventListener('pagehide', function () {
                    questionForms().forEach(sendQuestionFormBeacon);
                    sendRebalanceBeacon();
                });
            }

            function refreshOptionOrder(list) {
                Array.prototype.slice.call(list.querySelectorAll('[data-option-row]')).forEach(function (row, index) {
                    var orderInput = row.querySelector('[data-option-order-input]');
                    if (orderInput) {
                        orderInput.value = index + 1;
                    }
                });
            }

            function updateCorrectAnswerLabels(scope) {
                var forms = scope && scope.matches && scope.matches('[data-question-form]')
                        ? [scope]
                        : Array.prototype.slice.call((scope || document).querySelectorAll('[data-question-form]'));
                forms.forEach(function (form) {
                    var stacks = Array.prototype.slice.call(form.querySelectorAll('[data-correct-answer-stack]'));
                    stacks.forEach(function (stack) {
                        var input = stack.querySelector('input[type="radio"], input[type="checkbox"]');
                        var checked = input && input.checked;
                        var row = stack.closest('[data-option-row], [data-add-option-row]');
                        var marker = row ? row.querySelector('.gape-option-choice-marker') : null;
                        stack.classList.toggle('is-correct-primary', Boolean(checked));
                        if (marker) {
                            marker.classList.toggle('is-correct', checked);
                        }
                    });
                });
            }

            function updateOptionCount(form) {
                if (!form) {
                    return;
                }
                var questionId = form.dataset.questionId;
                var count = form.querySelectorAll('[data-option-list] [data-option-row]').length;
                var summary = questionId ? document.querySelector('[data-option-count-summary][data-question-id="' + questionId + '"]') : null;
                if (summary) {
                    summary.textContent = count + ' ' + (count === 1 ? 'option' : 'options');
                }
                var warning = form.querySelector('[data-empty-options-warning]');
                if (warning && count > 0) {
                    warning.remove();
                }
            }

            function addOptionShellForButton(button) {
                return button ? button.closest('[data-add-option-row]') : null;
            }

            function setAddOptionFeedback(button, message) {
                var shell = addOptionShellForButton(button);
                if (!shell) {
                    return;
                }
                var feedback = shell.querySelector('[data-add-option-feedback]');
                var input = shell.querySelector('[data-new-option-input]');
                var visible = Boolean(message);
                shell.classList.toggle('is-invalid', visible);
                button.classList.toggle('is-blocked', visible || !(input && input.value.trim()));
                if (input) {
                    input.classList.toggle('is-invalid', visible);
                    input.setAttribute('aria-invalid', visible ? 'true' : 'false');
                    if (visible) {
                        input.focus({preventScroll: true});
                        input.setSelectionRange(input.value.length, input.value.length);
                    }
                }
                if (feedback) {
                    feedback.textContent = message || 'Question option text is required';
                }
            }

            function refreshAddOptionButtons(scope) {
                Array.prototype.slice.call((scope || document).querySelectorAll('[data-add-option-button]')).forEach(function (button) {
                    var shell = addOptionShellForButton(button);
                    var input = shell ? shell.querySelector('[data-new-option-input]') : null;
                    button.classList.toggle('is-blocked', !(input && input.value.trim()));
                });
            }

            function refreshDeleteOptionButtons(scope) {
                var forms = scope && scope.matches && scope.matches('[data-question-form]')
                        ? [scope]
                        : Array.prototype.slice.call((scope || document).querySelectorAll('[data-question-form]'));
                forms.forEach(function (form) {
                    var rows = Array.prototype.slice.call(form.querySelectorAll('[data-option-list] [data-option-row]'));
                    var canDelete = rows.length > 1;
                    rows.forEach(function (row) {
                        var button = row.querySelector('[data-delete-option-button]');
                        if (!button) {
                            return;
                        }
                        button.disabled = !canDelete;
                        button.setAttribute('aria-disabled', canDelete ? 'false' : 'true');
                        button.title = canDelete ? 'Delete option' : 'At least one option is required';
                    });
                });
            }

            function optionRowFromResponse(form, option) {
                var questionId = form.dataset.questionId;
                var questionType = form.dataset.questionType;
                var row = document.createElement('div');
                row.className = 'gape-option-row';
                row.setAttribute('draggable', 'true');
                row.dataset.optionRow = '';
                row.dataset.optionId = String(option.id);

                var orderInput = document.createElement('input');
                orderInput.type = 'hidden';
                orderInput.name = 'optionOrder_' + option.id;
                orderInput.value = String(option.orderNo || 1);
                orderInput.dataset.optionOrderInput = '';
                row.appendChild(orderInput);

                var main = document.createElement('div');
                main.className = 'gape-option-main';
                var handle = document.createElement('span');
                handle.className = 'gape-option-drag-handle';
                handle.title = 'Drag to reorder';
                handle.setAttribute('aria-label', 'Drag to reorder');
                var handleIcon = document.createElement('i');
                handleIcon.className = 'ph ph-dots-six-vertical';
                handle.appendChild(handleIcon);
                main.appendChild(handle);

                var marker = document.createElement('span');
                marker.className = 'gape-option-choice-marker ' + (questionType === 'multiple_choice' ? 'is-checkbox' : 'is-radio');
                marker.setAttribute('aria-hidden', 'true');
                main.appendChild(marker);

                var textInput = document.createElement('input');
                textInput.id = 'optionText' + option.id;
                textInput.name = 'optionText_' + option.id;
                textInput.maxLength = 300;
                textInput.value = option.text || '';
                textInput.className = 'form-control px-12 py-9 text-13 bg-white border-neutral-30 border rounded-8';
                main.appendChild(textInput);
                row.appendChild(main);

                var actions = document.createElement('div');
                actions.className = 'gape-option-actions';
                var stack = document.createElement('div');
                stack.className = 'gape-option-correct-stack';
                stack.dataset.correctAnswerStack = '';
                var label = document.createElement('span');
                label.className = 'gape-correct-answer-label';
                label.dataset.correctAnswerLabel = '';
                label.textContent = 'Correct Answer';
                stack.appendChild(label);
                var correctControl = document.createElement('label');
                correctControl.className = 'gape-option-correct-control mb-0';
                correctControl.title = 'Correct answer';
                var correctInput = document.createElement('input');
                correctInput.type = questionType === 'multiple_choice' ? 'checkbox' : 'radio';
                correctInput.name = questionType === 'multiple_choice' ? 'optionCorrect_' + option.id : 'singleCorrect_' + questionId;
                correctInput.value = questionType === 'multiple_choice' ? 'true' : String(option.id);
                correctInput.setAttribute('aria-label', 'Correct answer');
                correctInput.checked = Boolean(option.correct);
                correctControl.appendChild(correctInput);
                stack.appendChild(correctControl);
                actions.appendChild(stack);

                var deleteButton = document.createElement('button');
                deleteButton.type = 'submit';
                deleteButton.setAttribute('formaction', option.archiveUrl || '');
                deleteButton.setAttribute('formnovalidate', '');
                deleteButton.className = 'border border-danger-200 bg-danger-50 text-danger-600 w-36 h-36 rounded-8 hover-bg-danger-100 transition-03 text-16 d-inline-flex align-items-center justify-content-center';
                deleteButton.title = 'Delete option';
                deleteButton.setAttribute('aria-label', 'Delete option');
                deleteButton.dataset.deleteOptionButton = '';
                var deleteIcon = document.createElement('i');
                deleteIcon.className = 'ph ph-trash';
                deleteButton.appendChild(deleteIcon);
                actions.appendChild(deleteButton);
                row.appendChild(actions);
                return row;
            }

            function syncOptionRow(row, option) {
                var orderInput = row.querySelector('[data-option-order-input]');
                if (orderInput) {
                    orderInput.value = String(option.orderNo || 1);
                }
                var correctInput = row.querySelector('[data-correct-answer-stack] input[type="radio"], [data-correct-answer-stack] input[type="checkbox"]');
                if (correctInput) {
                    correctInput.checked = Boolean(option.correct);
                }
                var deleteButton = row.querySelector('[data-delete-option-button]');
                if (deleteButton && option.archiveUrl) {
                    deleteButton.setAttribute('formaction', option.archiveUrl);
                }
            }

            function applyQuestionOptionsState(form, options) {
                var list = form.querySelector('[data-option-list]');
                if (!list) {
                    window.location.reload();
                    return;
                }
                var rowsById = new Map();
                Array.prototype.slice.call(list.querySelectorAll('[data-option-row]')).forEach(function (row) {
                    rowsById.set(String(row.dataset.optionId), row);
                });
                (options || []).forEach(function (option) {
                    var optionId = String(option.id);
                    var row = rowsById.get(optionId);
                    if (row) {
                        syncOptionRow(row, option);
                        rowsById.delete(optionId);
                    } else {
                        row = optionRowFromResponse(form, option);
                    }
                    list.appendChild(row);
                });
                rowsById.forEach(function (row) {
                    row.remove();
                });
                refreshOptionOrder(list);
                updateOptionCount(form);
                updateCorrectAnswerLabels(form);
                refreshDeleteOptionButtons(form);
            }

            function createOptionFromButton(button) {
                var form = button.form;
                var shell = addOptionShellForButton(button);
                var input = shell ? shell.querySelector('[data-new-option-input]') : null;
                if (!form || !shell || !input) {
                    return Promise.resolve(false);
                }
                if (!input.value.trim()) {
                    setAddOptionFeedback(button, 'Question option text is required');
                    return Promise.resolve(false);
                }
                setAddOptionFeedback(button, '');
                button.disabled = true;
                var payload = new URLSearchParams(new FormData(form));
                return fetch(button.formAction || button.getAttribute('formaction'), {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json'
                    },
                    body: payload.toString()
                }).then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            throw new Error(text || 'Question option could not be created.');
                        });
                    }
                    return response.json();
                }).then(function (option) {
                    var list = form.querySelector('[data-option-list]');
                    if (!list) {
                        window.location.reload();
                        return false;
                    }
                    list.appendChild(optionRowFromResponse(form, option));
                    input.value = '';
                    var addCorrectInput = shell.querySelector('input[type="radio"], input[type="checkbox"]');
                    if (addCorrectInput) {
                        addCorrectInput.checked = false;
                    }
                    refreshOptionOrder(list);
                    updateOptionCount(form);
                    updateCorrectAnswerLabels(form);
                    refreshAddOptionButtons(shell);
                    refreshDeleteOptionButtons(form);
                    form.dataset.autosaveDirty = 'false';
                    autosaveError('');
                    return true;
                }).catch(function (error) {
                    autosaveError(error.message || 'Question option could not be created.');
                    throw error;
                }).finally(function () {
                    button.disabled = false;
                    refreshAddOptionButtons(shell);
                });
            }

            function archiveOptionFromButton(button) {
                var form = button.form;
                if (!form || button.disabled) {
                    return Promise.resolve(false);
                }
                var action = button.formAction || button.getAttribute('formaction');
                if (!action) {
                    return Promise.resolve(false);
                }
                button.disabled = true;
                var payload = new URLSearchParams(new FormData(form));
                return fetch(action, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json'
                    },
                    body: payload.toString()
                }).then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            throw new Error(text || 'Question option could not be deleted.');
                        });
                    }
                    return response.json();
                }).then(function (state) {
                    applyQuestionOptionsState(form, state.options || []);
                    autosaveError('');
                    return true;
                }).catch(function (error) {
                    autosaveError(error.message || 'Question option could not be deleted.');
                    throw error;
                }).finally(function () {
                    refreshDeleteOptionButtons(form);
                });
            }

            function dragAfterElement(list, y, rowSelector) {
                return Array.prototype.slice.call(list.querySelectorAll(rowSelector + ':not(.is-dragging)')).reduce(function (closest, child) {
                    var box = child.getBoundingClientRect();
                    var offset = y - box.top - box.height / 2;
                    if (offset < 0 && offset > closest.offset) {
                        return {offset: offset, element: child};
                    }
                    return closest;
                }, {offset: Number.NEGATIVE_INFINITY, element: null}).element;
            }

            function refreshQuestionNumbers(list) {
                Array.prototype.slice.call(list.querySelectorAll('[data-question-card]')).forEach(function (card, index) {
                    var number = card.querySelector('[data-question-number]');
                    if (number) {
                        number.textContent = 'Question ' + (index + 1);
                    }
                });
            }

            function applyQuestionOrderState(list, questions) {
                var cardsById = new Map();
                Array.prototype.slice.call(list.querySelectorAll('[data-question-card]')).forEach(function (card) {
                    cardsById.set(String(card.dataset.questionId), card);
                });
                (questions || []).forEach(function (question) {
                    var card = cardsById.get(String(question.id));
                    if (card) {
                        list.appendChild(card);
                    }
                });
                refreshQuestionNumbers(list);
            }

            function saveQuestionOrder(list) {
                if (!canEditBuilder || !builderRoot || !builderRoot.dataset.questionReorderUrl) {
                    return Promise.resolve(false);
                }
                var ids = Array.prototype.slice.call(list.querySelectorAll('[data-question-card]'))
                        .map(function (card) { return card.dataset.questionId; })
                        .filter(Boolean);
                if (ids.length === 0) {
                    return Promise.resolve(false);
                }
                var payload = new URLSearchParams();
                payload.set('csrfToken', builderRoot.dataset.csrfToken || '');
                ids.forEach(function (id) {
                    payload.append('questionId', id);
                });
                payload.set('autosave', 'true');
                return fetch(builderRoot.dataset.questionReorderUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json'
                    },
                    body: payload.toString()
                }).then(function (response) {
                    if (!response.ok) {
                        return response.text().then(function (text) {
                            throw new Error(text || 'Question order could not be saved.');
                        });
                    }
                    return response.json();
                }).then(function (state) {
                    applyQuestionOrderState(list, state.questions || []);
                    autosaveError('');
                    return true;
                }).catch(function (error) {
                    autosaveError(error.message || 'Question order could not be saved.');
                    throw error;
                });
            }

            Array.prototype.slice.call(document.querySelectorAll('[data-question-list]')).forEach(function (list) {
                var pointerState = null;
                refreshQuestionNumbers(list);

                function moveDraggedCard(clientY) {
                    if (!pointerState) {
                        return;
                    }
                    var afterElement = dragAfterElement(list, clientY, '[data-question-card]');
                    if (afterElement == null) {
                        list.appendChild(pointerState.card);
                    } else {
                        list.insertBefore(pointerState.card, afterElement);
                    }
                    refreshQuestionNumbers(list);
                }

                function endPointerDrag(event) {
                    if (!pointerState || pointerState.pointerId !== event.pointerId) {
                        return;
                    }
                    pointerState.card.classList.remove('is-dragging');
                    pointerState = null;
                    refreshQuestionNumbers(list);
                    saveQuestionOrder(list).catch(function () {
                        // The visible autosave error is set by saveQuestionOrder.
                    });
                }

                list.addEventListener('pointerdown', function (event) {
                    if (!canEditBuilder) {
                        return;
                    }
                    var handle = event.target.closest('.gape-question-drag-handle');
                    var card = event.target.closest('[data-question-card]');
                    if (!handle || !card || handle.getAttribute('draggable') !== 'true') {
                        return;
                    }
                    event.preventDefault();
                    pointerState = {
                        pointerId: event.pointerId,
                        card: card
                    };
                    card.classList.add('is-dragging');
                    if (handle.setPointerCapture) {
                        handle.setPointerCapture(event.pointerId);
                    }
                });
                list.addEventListener('pointermove', function (event) {
                    if (!pointerState || pointerState.pointerId !== event.pointerId) {
                        return;
                    }
                    event.preventDefault();
                    moveDraggedCard(event.clientY);
                });
                list.addEventListener('pointerup', endPointerDrag);
                list.addEventListener('pointercancel', endPointerDrag);
                list.addEventListener('dragstart', function (event) {
                    if (!canEditBuilder) {
                        return;
                    }
                    var handle = event.target.closest('.gape-question-drag-handle');
                    var card = event.target.closest('[data-question-card]');
                    if (!handle || !card || handle.getAttribute('draggable') !== 'true') {
                        event.preventDefault();
                        return;
                    }
                    card.classList.add('is-dragging');
                    event.dataTransfer.effectAllowed = 'move';
                    event.dataTransfer.setData('text/plain', card.dataset.questionId || '');
                });
                list.addEventListener('dragover', function (event) {
                    var card = list.querySelector('[data-question-card].is-dragging');
                    if (!card) {
                        return;
                    }
                    event.preventDefault();
                    var afterElement = dragAfterElement(list, event.clientY, '[data-question-card]');
                    if (afterElement == null) {
                        list.appendChild(card);
                    } else {
                        list.insertBefore(card, afterElement);
                    }
                    refreshQuestionNumbers(list);
                });
                list.addEventListener('dragend', function (event) {
                    var card = event.target.closest('[data-question-card]');
                    if (card) {
                        card.classList.remove('is-dragging');
                        saveQuestionOrder(list).catch(function () {
                            // The visible autosave error is set by saveQuestionOrder.
                        });
                    }
                    refreshQuestionNumbers(list);
                });
            });

            Array.prototype.slice.call(document.querySelectorAll('[data-option-list]')).forEach(function (list) {
                var pointerState = null;
                refreshOptionOrder(list);

                function moveDraggedRow(clientY) {
                    if (!pointerState) {
                        return;
                    }
                    var afterElement = dragAfterElement(list, clientY, '[data-option-row]');
                    if (afterElement == null) {
                        list.appendChild(pointerState.row);
                    } else {
                        list.insertBefore(pointerState.row, afterElement);
                    }
                    refreshOptionOrder(list);
                }

                function endPointerDrag(event) {
                    if (!pointerState || pointerState.pointerId !== event.pointerId) {
                        return;
                    }
                    var form = pointerState.row.closest('form');
                    pointerState.row.classList.remove('is-dragging');
                    pointerState = null;
                    refreshOptionOrder(list);
                    scheduleAutosave(form);
                }

                list.addEventListener('pointerdown', function (event) {
                    var handle = event.target.closest('.gape-option-drag-handle');
                    var row = event.target.closest('[data-option-row]');
                    if (!handle || !row || row.getAttribute('draggable') !== 'true') {
                        return;
                    }
                    event.preventDefault();
                    pointerState = {
                        pointerId: event.pointerId,
                        row: row
                    };
                    row.classList.add('is-dragging');
                    if (handle.setPointerCapture) {
                        handle.setPointerCapture(event.pointerId);
                    }
                });
                list.addEventListener('pointermove', function (event) {
                    if (!pointerState || pointerState.pointerId !== event.pointerId) {
                        return;
                    }
                    event.preventDefault();
                    moveDraggedRow(event.clientY);
                });
                list.addEventListener('pointerup', endPointerDrag);
                list.addEventListener('pointercancel', endPointerDrag);
                list.addEventListener('dragstart', function (event) {
                    var row = event.target.closest('[data-option-row]');
                    if (!row || row.getAttribute('draggable') !== 'true') {
                        return;
                    }
                    row.classList.add('is-dragging');
                    event.dataTransfer.effectAllowed = 'move';
                });
                list.addEventListener('dragover', function (event) {
                    var row = list.querySelector('.is-dragging');
                    if (!row) {
                        return;
                    }
                    event.preventDefault();
                    var afterElement = dragAfterElement(list, event.clientY, '[data-option-row]');
                    if (afterElement == null) {
                        list.appendChild(row);
                    } else {
                        list.insertBefore(row, afterElement);
                    }
                    refreshOptionOrder(list);
                });
                list.addEventListener('dragend', function (event) {
                    var row = event.target.closest('[data-option-row]');
                    if (row) {
                        row.classList.remove('is-dragging');
                        scheduleAutosave(row.closest('form'));
                    }
                    refreshOptionOrder(list);
                });
            });

            function ratingDesignParts(value) {
                var parts = String(value || 'stars_integer').toLowerCase().split('_');
                var style = ['stars', 'circles', 'hearts'].indexOf(parts[0]) === -1 ? 'stars' : parts[0];
                var step = parts[1] === 'half' ? 'half' : 'integer';
                return {style: style, step: step};
            }

            function clampRatingMax(value) {
                var parsed = parseInt(value, 10);
                if (Number.isNaN(parsed) || parsed < 1) {
                    return 5;
                }
                return Math.min(parsed, 100);
            }

            function normalizeRatingValue(value, step, max) {
                var parsed = parseFloat(value);
                if (Number.isNaN(parsed) || parsed < 0) {
                    parsed = 0;
                }
                parsed = Math.min(parsed, max);
                if (step === 'half') {
                    return Math.round(parsed * 2) / 2;
                }
                return Math.round(parsed);
            }

            function formatRatingValue(value) {
                return Number.isInteger(value) ? String(value) : value.toFixed(1);
            }

            function ratingIconClass(style, filled) {
                if (style === 'circles') {
                    return filled ? 'ph-fill ph-circle' : 'ph ph-circle';
                }
                if (style === 'hearts') {
                    return filled ? 'ph-fill ph-heart' : 'ph ph-heart';
                }
                return filled ? 'ph-fill ph-star' : 'ph ph-star';
            }

            function nextRatingValue(current, unit, step) {
                if (step !== 'half') {
                    return current >= unit ? unit - 1 : unit;
                }
                var half = unit - 0.5;
                if (current < half || current > unit) {
                    return half;
                }
                if (current < unit) {
                    return unit;
                }
                return unit - 1;
            }

            function renderRatingPicker(picker) {
                var style = picker.dataset.ratingStyle || 'stars';
                var step = picker.dataset.ratingStep || 'integer';
                var max = clampRatingMax(picker.dataset.ratingMax);
                var value = normalizeRatingValue(picker.dataset.ratingValue, step, max);
                var readonly = picker.dataset.ratingReadonly === 'true';
                picker.dataset.ratingMax = String(max);
                picker.dataset.ratingValue = formatRatingValue(value);
                picker.innerHTML = '';
                for (var unit = 1; unit <= max; unit += 1) {
                    var button = document.createElement('button');
                    button.type = 'button';
                    button.className = 'gape-rating-unit';
                    button.dataset.ratingUnit = String(unit);
                    button.disabled = readonly;
                    if (value >= unit) {
                        button.classList.add('is-full');
                    } else if (step === 'half' && value >= unit - 0.5) {
                        button.classList.add('is-half');
                    }

                    var base = document.createElement('span');
                    base.className = 'gape-rating-unit__base';
                    base.setAttribute('aria-hidden', 'true');
                    var baseIcon = document.createElement('i');
                    baseIcon.className = ratingIconClass(style, false);
                    base.appendChild(baseIcon);

                    var fill = document.createElement('span');
                    fill.className = 'gape-rating-unit__fill';
                    fill.setAttribute('aria-hidden', 'true');
                    var fillIcon = document.createElement('i');
                    fillIcon.className = ratingIconClass(style, true);
                    fill.appendChild(fillIcon);

                    button.appendChild(base);
                    button.appendChild(fill);
                    picker.appendChild(button);
                }
                updateRatingReadout(picker);
            }

            function updateRatingReadout(picker) {
                var wrapper = picker.closest('[data-rating-builder]');
                var readout = wrapper ? wrapper.querySelector('[data-rating-readout]') : null;
                if (!readout) {
                    readout = picker.parentElement ? picker.parentElement.querySelector('[data-rating-readout]') : null;
                }
                if (readout) {
                    readout.textContent = formatRatingValue(parseFloat(picker.dataset.ratingValue || '0')) + ' / ' + picker.dataset.ratingMax;
                }
            }

            function syncRatingBuilder(questionId) {
                var select = document.querySelector('[data-rating-design-select][data-question-id="' + questionId + '"]');
                var maxInput = document.querySelector('[data-rating-max-input][data-question-id="' + questionId + '"]');
                var expectedInput = document.querySelector('[data-rating-expected-input][data-question-id="' + questionId + '"]');
                var picker = document.querySelector('[data-rating-picker][data-question-id="' + questionId + '"]');
                if (!picker) {
                    return;
                }
                var design = ratingDesignParts(select ? select.value : picker.dataset.ratingStyle + '_' + picker.dataset.ratingStep);
                var max = clampRatingMax(maxInput ? maxInput.value : picker.dataset.ratingMax);
                var value = normalizeRatingValue(expectedInput ? expectedInput.value : picker.dataset.ratingValue, design.step, max);
                if (maxInput) {
                    maxInput.value = String(max);
                }
                if (expectedInput) {
                    expectedInput.max = String(max);
                    expectedInput.step = design.step === 'half' ? '0.5' : '1';
                    expectedInput.value = formatRatingValue(value);
                }
                picker.dataset.ratingStyle = design.style;
                picker.dataset.ratingStep = design.step;
                picker.dataset.ratingMax = String(max);
                picker.dataset.ratingValue = formatRatingValue(value);
                var builder = picker.closest('[data-rating-builder]');
                if (builder) {
                    builder.dataset.ratingStyle = design.style;
                    builder.dataset.ratingStep = design.step;
                    builder.dataset.ratingMax = String(max);
                }
                renderRatingPicker(picker);
            }

            Array.prototype.slice.call(document.querySelectorAll('[data-rating-picker]')).forEach(function (picker) {
                renderRatingPicker(picker);
                picker.addEventListener('click', function (event) {
                    var button = event.target.closest('[data-rating-unit]');
                    if (!button || button.disabled) {
                        return;
                    }
                    var unit = parseInt(button.dataset.ratingUnit, 10);
                    var max = clampRatingMax(picker.dataset.ratingMax);
                    var step = picker.dataset.ratingStep || 'integer';
                    var current = normalizeRatingValue(picker.dataset.ratingValue, step, max);
                    var next = normalizeRatingValue(nextRatingValue(current, unit, step), step, max);
                    picker.dataset.ratingValue = formatRatingValue(next);
                    var questionId = picker.dataset.questionId;
                    var expectedInput = questionId ? document.querySelector('[data-rating-expected-input][data-question-id="' + questionId + '"]') : null;
                    if (expectedInput) {
                        expectedInput.value = formatRatingValue(next);
                        scheduleAutosave(expectedInput.form);
                    }
                    renderRatingPicker(picker);
                });
            });

            Array.prototype.slice.call(document.querySelectorAll('[data-rating-design-select], [data-rating-max-input], [data-rating-expected-input]')).forEach(function (control) {
                control.addEventListener('change', function () {
                    syncRatingBuilder(control.dataset.questionId);
                    scheduleAutosave(control.form);
                });
                control.addEventListener('input', function () {
                    syncRatingBuilder(control.dataset.questionId);
                    scheduleAutosave(control.form);
                });
            });
            if (builderRoot) {
                updateCorrectAnswerLabels(builderRoot);
            }
            if (builderRoot && canEditBuilder) {
                builderRoot.addEventListener('click', function (event) {
                    var addOptionButton = event.target.closest('[data-add-option-button]');
                    if (!addOptionButton || !addOptionButton.form) {
                        return;
                    }
                    event.preventDefault();
                    createOptionFromButton(addOptionButton).catch(function () {
                        // The visible autosave error is set by createOptionFromButton.
                    });
                });
                builderRoot.addEventListener('input', function (event) {
                    var target = event.target;
                    if (!target || !target.form || !target.form.matches('[data-question-form]')) {
                        return;
                    }
                    if (target.closest('[data-add-option-row]')) {
                        var button = target.closest('[data-add-option-row]').querySelector('[data-add-option-button]');
                        setAddOptionFeedback(button, '');
                        refreshAddOptionButtons(target.closest('[data-add-option-row]'));
                        return;
                    }
                    if (target.matches('[data-question-score-input]')) {
                        clampQuestionScore(target);
                        updateScoreBalanceWarning();
                    }
                    scheduleAutosave(target.form);
                });
                builderRoot.addEventListener('change', function (event) {
                    var target = event.target;
                    if (!target || !target.form || !target.form.matches('[data-question-form]')) {
                        return;
                    }
                    if (target.closest('[data-add-option-row]')) {
                        updateCorrectAnswerLabels(target.form);
                        refreshAddOptionButtons(target.closest('[data-add-option-row]'));
                        return;
                    }
                    if (target.matches('[data-question-score-input]')) {
                        clampQuestionScore(target);
                        updateScoreBalanceWarning();
                    }
                    if (target.closest('[data-correct-answer-stack]')) {
                        updateCorrectAnswerLabels(target.form);
                    }
                    scheduleAutosave(target.form);
                });
                questionForms().forEach(function (form) {
                    form.addEventListener('submit', function (event) {
                        var submitter = event.submitter;
                        if (submitter && submitter.matches('[data-add-option-button]')) {
                            event.preventDefault();
                            createOptionFromButton(submitter).catch(function () {
                                // The visible autosave error is set by createOptionFromButton.
                            });
                            return;
                        }
                        if (submitter && submitter.matches('[data-delete-option-button]')) {
                            event.preventDefault();
                            archiveOptionFromButton(submitter).catch(function () {
                                // The visible autosave error is set by archiveOptionFromButton.
                            });
                            return;
                        }
                        if (submitter && submitter.getAttribute('formaction')) {
                            return;
                        }
                        event.preventDefault();
                        saveQuestionForm(form).catch(function () {
                            // The visible autosave error is set by saveQuestionForm.
                        });
                    });
                });
                builderRoot.addEventListener('pointerenter', function (event) {
                    var button = event.target.closest('[data-add-option-button]');
                    if (button) {
                        refreshAddOptionButtons(addOptionShellForButton(button));
                    }
                }, true);
                builderRoot.addEventListener('touchstart', function (event) {
                    var button = event.target.closest('[data-add-option-button]');
                    if (button) {
                        refreshAddOptionButtons(addOptionShellForButton(button));
                    }
                }, {passive: true});
                refreshAddOptionButtons(builderRoot);
                refreshDeleteOptionButtons(builderRoot);
                updateScoreBalanceWarning();
                prepareNavigationFlush();
            }
        });
    })();
</script>
</body>
</html>
