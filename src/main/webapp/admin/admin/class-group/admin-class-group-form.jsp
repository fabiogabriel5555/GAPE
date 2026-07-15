<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "class-groups");
    }
%>
<c:set var="classGroupBackHref" value="${pageContext.request.contextPath}/learning/class-groups"/>
<c:if test="${not creating}">
    <c:set var="classGroupBackHref" value="${pageContext.request.contextPath}/learning/class-groups/${form.id}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - <c:out value="${classGroupPageTitle}"/></title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-class-group-wizard {
            --gape-wizard-accent: var(--main-600, #00a991);
            --gape-wizard-accent-dark: var(--main-700, #008f7c);
            --gape-wizard-border: #e6eaef;
            --gape-wizard-muted: #6b7280;
            max-width: 1280px;
            margin-inline: auto;
        }

        .gape-wizard-shell {
            display: grid;
            grid-template-columns: minmax(280px, 390px) minmax(0, 1fr);
            gap: 40px;
            align-items: start;
        }

        .gape-class-preview {
            position: sticky;
            top: 104px;
            overflow: hidden;
            border: 1px solid var(--gape-wizard-border);
            box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
        }

        .gape-preview-cover {
            position: relative;
            min-height: 205px;
            background:
                    linear-gradient(135deg, rgba(0, 169, 145, 0.14), rgba(86, 109, 240, 0.12)),
                    #eef2f5;
            display: grid;
            place-items: center;
        }

        .gape-preview-cover i {
            width: 66px;
            height: 66px;
            border-radius: 16px;
            display: grid;
            place-items: center;
            color: var(--gape-wizard-accent);
            background: rgba(255, 255, 255, 0.74);
            border: 1px solid rgba(255, 255, 255, 0.88);
            font-size: 34px;
        }

        .gape-preview-badge {
            position: absolute;
            inset-block-start: 16px;
            inset-inline-start: 16px;
            padding: 5px 12px;
            border-radius: 8px;
            background: #2f343b;
            color: #fff;
            font-size: 13px;
            font-weight: 700;
        }

        .gape-preview-body {
            min-height: 230px;
        }

        .gape-preview-title {
            min-height: 34px;
            word-break: break-word;
        }

        .gape-preview-subtitle {
            min-height: 52px;
            line-height: 1.55;
        }

        .gape-preview-chip-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .gape-preview-chip {
            border: 1px solid var(--gape-wizard-border);
            background: #f8fafc;
            border-radius: 8px;
            padding: 10px 12px;
            min-width: 0;
        }

        .gape-preview-chip span,
        .gape-review-item span {
            display: block;
            color: var(--gape-wizard-muted);
            font-size: 11px;
            line-height: 1.2;
            margin-bottom: 5px;
            text-transform: uppercase;
        }

        .gape-preview-chip strong {
            display: block;
            min-width: 0;
            color: #263238;
            font-size: 13px;
            font-weight: 700;
            overflow-wrap: anywhere;
        }

        .gape-preview-period-context {
            display: block;
            color: var(--gape-wizard-muted);
            font-size: 11px;
            line-height: 1.35;
            margin-top: 4px;
            overflow-wrap: anywhere;
        }

        .gape-wizard-progress {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 10px;
            margin-bottom: 34px;
        }

        .gape-wizard-step {
            display: flex;
            flex-direction: column;
            gap: 8px;
            border: 0;
            background: transparent;
            padding: 0;
            min-width: 0;
            text-align: start;
            cursor: pointer;
        }

        .gape-wizard-step::before {
            content: "";
            display: block;
            height: 7px;
            border-radius: 99px;
            background: #dbe9e8;
        }

        .gape-wizard-step.is-active::before,
        .gape-wizard-step.is-complete::before {
            background: var(--gape-wizard-accent);
        }

        .gape-wizard-step span {
            color: var(--gape-wizard-muted);
            font-size: 12px;
            font-weight: 700;
        }

        .gape-wizard-step strong {
            color: #334155;
            font-size: 13px;
            font-weight: 700;
            overflow-wrap: anywhere;
        }

        .gape-step-panel {
            display: none;
        }

        .gape-step-panel.is-active {
            display: block;
        }

        .gape-step-eyebrow {
            color: var(--gape-wizard-accent);
            font-size: 12px;
            font-weight: 800;
            text-transform: uppercase;
        }

        .gape-step-title {
            color: #263238;
            font-size: 32px;
            line-height: 1.2;
            font-weight: 600;
            margin-bottom: 10px;
        }

        .gape-step-copy {
            color: var(--gape-wizard-muted);
            line-height: 1.65;
            margin-bottom: 28px;
        }

        .gape-choice-grid {
            display: grid;
            gap: 14px;
        }

        .gape-choice-grid.gape-choice-grid--compact {
            grid-template-columns: repeat(3, minmax(0, 1fr));
        }

        .gape-choice-card {
            display: flex;
            align-items: flex-start;
            gap: 14px;
            border: 1px solid var(--gape-wizard-border);
            background: #fff;
            border-radius: 8px;
            padding: 16px 18px;
            min-height: 78px;
            cursor: pointer;
            transition: border-color .2s ease, box-shadow .2s ease, background .2s ease;
        }

        .gape-choice-card:hover {
            border-color: rgba(0, 169, 145, 0.42);
            box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
        }

        .gape-choice-card input {
            flex: 0 0 auto;
            width: 18px;
            height: 18px;
            margin-top: 4px;
            accent-color: var(--gape-wizard-accent);
        }

        .gape-choice-card:has(input:checked) {
            border-color: var(--gape-wizard-accent);
            background: rgba(0, 169, 145, 0.055);
        }

        .gape-choice-title {
            display: block;
            color: #263238;
            font-size: 16px;
            font-weight: 700;
            margin-bottom: 4px;
        }

        .gape-choice-card small {
            display: block;
            color: var(--gape-wizard-muted);
            line-height: 1.45;
        }

        .gape-context-card {
            border: 1px solid var(--gape-wizard-border);
            background: #f8fafc;
            border-radius: 8px;
            padding: 18px;
            height: 100%;
        }

        .gape-context-card i,
        .gape-outline-icon {
            width: 40px;
            height: 40px;
            border-radius: 8px;
            display: grid;
            place-items: center;
            background: rgba(0, 169, 145, 0.1);
            color: var(--gape-wizard-accent);
            font-size: 22px;
        }

        .gape-review-outline {
            display: grid;
            gap: 18px;
        }

        .gape-review-block {
            display: grid;
            grid-template-columns: 78px minmax(0, 1fr);
            gap: 22px;
            padding-bottom: 22px;
            border-bottom: 1px solid var(--gape-wizard-border);
        }

        .gape-review-block:last-child {
            border-bottom: 0;
            padding-bottom: 0;
        }

        .gape-review-number {
            color: #263238;
            font-size: 42px;
            line-height: 1;
            font-weight: 800;
        }

        .gape-review-heading {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 14px;
        }

        .gape-review-heading h4 {
            margin: 0;
            color: #263238;
            font-size: 18px;
            font-weight: 700;
        }

        .gape-review-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 12px;
        }

        .gape-review-item {
            min-width: 0;
            border: 1px solid var(--gape-wizard-border);
            border-radius: 8px;
            padding: 12px 14px;
            background: #fff;
        }

        .gape-review-item strong {
            display: block;
            color: #263238;
            font-size: 14px;
            font-weight: 700;
            overflow-wrap: anywhere;
        }

        .gape-wizard-actions {
            border-top: 1px dashed var(--gape-wizard-border);
            padding-top: 24px;
            margin-top: 34px;
        }

        .gape-wizard-panel {
            min-height: 560px;
        }

        @media (max-width: 1199px) {
            .gape-wizard-shell {
                grid-template-columns: 1fr;
            }

            .gape-class-preview {
                position: static;
            }

            .gape-wizard-panel {
                min-height: auto;
            }
        }

        @media (max-width: 767px) {
            .gape-class-group-wizard {
                padding: 24px 20px !important;
            }

            .gape-wizard-shell {
                grid-template-columns: minmax(0, 1fr);
                gap: 24px;
                min-width: 0;
                overflow: hidden;
            }

            .gape-class-preview,
            .gape-wizard-panel {
                max-width: 100%;
                min-width: 0;
                width: 100%;
            }

            .gape-step-panel .row {
                --bs-gutter-x: 0;
                margin-left: 0;
                margin-right: 0;
            }

            .gape-step-panel .row > [class*="col"] {
                padding-left: 0;
                padding-right: 0;
            }

            .gape-wizard-progress,
            .gape-choice-grid.gape-choice-grid--compact,
            .gape-preview-chip-grid,
            .gape-review-grid {
                grid-template-columns: 1fr;
            }

            .gape-step-title {
                font-size: 26px;
            }

            .gape-review-block {
                grid-template-columns: 1fr;
                gap: 12px;
            }

            .gape-review-number {
                font-size: 34px;
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
                <form action="${formAction}" method="post" class="gape-class-group-wizard bg-white rounded-10 px-40 py-40" data-class-group-wizard data-class-group-context-form data-class-group-context-managed data-creating="${creating}" data-class-group-selected-year="<c:out value='${classGroupSelectedYear}'/>" data-class-group-selected-term="<c:out value='${classGroupSelectedTerm}'/>" data-class-group-subject-label="<c:out value='${subjectCreationContext.name}'/>">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-32">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4"><c:out value="${classGroupPageTitle}"/></h2>
                            <span class="text-14 text-neutral-500">Set the class group context, operating model and availability before saving.</span>
                        </div>
                        <a href="${classGroupBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="gape-wizard-shell">
                        <aside class="gape-class-preview bg-white rounded-10" aria-label="Class group preview">
                            <div class="gape-preview-cover">
                                <span class="gape-preview-badge" data-preview-state>Draft</span>
                                <i class="ph ph-users-three"></i>
                            </div>
                            <div class="gape-preview-body px-24 py-24">
                                <h3 class="gape-preview-title text-24 fw-semibold text-neutral-700 mb-10" data-preview-code>Class group code</h3>
                                <p class="gape-preview-subtitle text-15 text-neutral-600 mb-24" data-preview-context>Choose a course and subject to define the academic context.</p>
                                <div class="gape-preview-chip-grid">
                                    <div class="gape-preview-chip">
                                        <span>Modality</span>
                                        <strong data-preview-modality>On-site</strong>
                                    </div>
                                    <div class="gape-preview-chip">
                                        <span>Shift</span>
                                        <strong data-preview-shift>Not set</strong>
                                    </div>
                                    <div class="gape-preview-chip">
                                        <span>Capacity</span>
                                        <strong data-preview-capacity>Not set</strong>
                                    </div>
                                    <div class="gape-preview-chip">
                                        <span>Period</span>
                                        <strong data-preview-period>Not set</strong>
                                        <small class="gape-preview-period-context" data-preview-period-context></small>
                                    </div>
                                </div>
                            </div>
                        </aside>

                        <section class="gape-wizard-panel">
                            <div class="gape-wizard-progress" aria-label="Class group setup progress">
                                <button type="button" class="gape-wizard-step is-active" data-step-indicator="0">
                                    <span>01</span>
                                    <strong>Context</strong>
                                </button>
                                <button type="button" class="gape-wizard-step" data-step-indicator="1">
                                    <span>02</span>
                                    <strong>Setup</strong>
                                </button>
                                <button type="button" class="gape-wizard-step" data-step-indicator="2">
                                    <span>03</span>
                                    <strong>Access</strong>
                                </button>
                                <button type="button" class="gape-wizard-step" data-step-indicator="3">
                                    <span>04</span>
                                    <strong>Review</strong>
                                </button>
                            </div>

                            <div class="gape-step-panel is-active" data-step-panel="0">
                                <span class="gape-step-eyebrow">Class group context</span>
                                <h3 class="gape-step-title">Let's set up your class group</h3>
                                <p class="gape-step-copy">Choose the course and subject association that this class group belongs to. This keeps the group aligned with the academic structure.</p>

                                <div class="row gy-4">
                                    <c:choose>
                                        <c:when test="${creating}">
                                            <c:choose>
                                                <c:when test="${not empty subjectCreationContext}">
                                                    <div class="col-lg-6 gape-select-field gape-class-group-context-field" data-class-group-course-context>
                                                        <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                                        <select id="courseId" name="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-class-group-course>
                                                            <option value="">Select course</option>
                                                            <c:forEach var="course" items="${courseOptions}">
                                                                <c:set var="subjectCourseAssociation" value="${subjectCreationAssociationByCourseId[course.id]}"/>
                                                                <c:set var="subjectCourseSelectable" value="${course.active and subjectCreationCourseSelectableByCourseId[course.id]}"/>
                                                                <c:set var="subjectCourseUnavailableReason" value="This subject is not associated with this course."/>
                                                                <c:if test="${not course.active}"><c:set var="subjectCourseUnavailableReason" value="This course is inactive."/></c:if>
                                                                <c:if test="${not empty subjectCourseAssociation and course.active and not subjectCourseSelectable}"><c:set var="subjectCourseUnavailableReason" value="You do not have permission to create a class group in this course."/></c:if>
                                                                <option value="${course.id}"
                                                                        title="<c:out value='${course.name}'/> | <c:out value='${course.courseManagementContextTitle}'/>"
                                                                        data-organization-id="${course.organizationId}"
                                                                        data-organic-unit-id="${course.organicUnitId}"
                                                                        data-course-acronym="<c:out value='${course.acronym}'/>"
                                                                        data-course-label="<c:out value='${course.name}'/>"
                                                                        data-organization-acronym="<c:out value='${course.organizationAcronym}'/>"
                                                                        data-organic-unit-acronym="<c:out value='${course.organicUnitAcronym}'/>"
                                                                        data-subject-association-available="${not empty subjectCourseAssociation}"
                                                                        data-subject-context-selectable="${subjectCourseSelectable}"
                                                                        data-subject-curricular-year="<c:out value='${subjectCourseAssociation.curricularYear}'/>"
                                                                        data-subject-term="<c:out value='${subjectCourseAssociation.term}'/>"
                                                                        data-context-unavailable-reason="<c:out value='${subjectCourseUnavailableReason}'/>"
                                                                        ${form.courseId == course.id and subjectCourseSelectable ? 'selected' : ''}>
                                                                    <c:out value="${course.acronym}"/> - <c:out value="${course.name}"/> | <c:out value="${course.courseManagementContextLabel}"/>
                                                                </option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                    <div class="col-lg-6 gape-class-group-context-field" data-class-group-subject-context>
                                                        <label for="subjectContext" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                        <div id="subjectContext" class="gape-class-group-readonly-value gape-class-group-readonly-value--locked-subject form-control fw-normal text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14" aria-readonly="true">
                                                            <span class="gape-class-group-readonly-value-main"><c:out value="${subjectCreationContext.acronym}"/> - <c:out value="${subjectCreationContext.name}"/></span>
                                                        </div>
                                                        <input type="hidden" id="subjectId" name="subjectId" value="${subjectCreationContext.id}" data-class-group-subject data-class-group-locked-subject>
                                                        <input type="hidden" name="subjectContextId" value="${subjectCreationContext.id}">
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="col-lg-6 gape-select-field gape-class-group-context-field" data-class-group-course-context>
                                                        <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                                        <select id="courseId" name="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-class-group-course>
                                                            <option value="">Select course</option>
                                                            <c:forEach var="course" items="${courseOptions}">
                                                                <option value="${course.id}"
                                                                        title="<c:out value='${course.name}'/> | <c:out value='${course.courseManagementContextTitle}'/>"
                                                                        data-organization-id="${course.organizationId}"
                                                                        data-organic-unit-id="${course.organicUnitId}"
                                                                        data-course-acronym="<c:out value='${course.acronym}'/>"
                                                                        data-course-label="<c:out value='${course.name}'/>"
                                                                        data-organization-acronym="<c:out value='${course.organizationAcronym}'/>"
                                                                        data-organic-unit-acronym="<c:out value='${course.organicUnitAcronym}'/>"
                                                                        ${form.courseId == course.id ? 'selected' : ''}>
                                                                    <c:out value="${course.acronym}"/> - <c:out value="${course.name}"/> | <c:out value="${course.courseManagementContextLabel}"/>
                                                                </option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                    <div class="col-lg-6 gape-select-field gape-class-group-context-field opacity-75 is-disabled" data-class-group-subject-context>
                                                        <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                        <select id="subjectId" name="subjectId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-class-group-subject disabled>
                                                            <option value="">Select subject</option>
                                                            <c:forEach var="association" items="${courseSubjectOptions}">
                                                                <option value="${association.subjectId}"
                                                                        data-parent-value="${association.courseId}"
                                                                        data-course-id="${association.courseId}"
                                                                        data-subject-acronym="<c:out value='${association.subjectAcronym}'/>"
                                                                        data-subject-label="<c:out value='${association.subjectName}'/>"
                                                                        data-curricular-year="${association.curricularYear}"
                                                                        data-term="${association.term}"
                                                                        title="<c:out value='${association.curricularPositionLabel}'/>"
                                                                        ${form.subjectId == association.subjectId and form.courseId == association.courseId ? 'selected' : ''}>
                                                                    <c:out value="${association.subjectAcronym}"/> - <c:out value="${association.subjectName}"/>
                                                                </option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="col-lg-6 gape-class-group-context-field" data-class-group-course-context>
                                                <label for="courseContext" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                                <div id="courseContext" class="gape-class-group-readonly-value form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                                    <span class="gape-class-group-readonly-value-main"><c:out value="${classGroup.course.acronym}"/> - <c:out value="${classGroup.course.name}"/></span>
                                                    <span class="gape-class-group-readonly-value-context"><c:out value="${classGroup.course.courseManagementContextLabel}"/></span>
                                                </div>
                                                <input type="hidden" name="courseId" value="${form.courseId}">
                                            </div>
                                            <div class="col-lg-6 gape-class-group-context-field" data-class-group-subject-context>
                                                <label for="subjectContext" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                <div id="subjectContext" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                                    <c:out value="${classGroup.subject.acronym}"/> - <c:out value="${classGroup.subject.name}"/>
                                                </div>
                                                <input type="hidden" name="subjectId" value="${form.subjectId}">
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                    <div class="col-12 gape-select-field gape-class-group-context-field opacity-75 is-disabled" data-class-group-period-context>
                                        <input type="hidden" id="courseOccurrenceId" name="courseOccurrenceId" value="<c:out value='${form.courseOccurrenceId}'/>" data-class-group-occurrence>
                                        <label for="courseOccurrencePeriodId" class="fw-medium text-base text-neutral-800 mb-12">Course Occurrence Period</label>
                                        <select id="courseOccurrencePeriodId" name="courseOccurrencePeriodId" required disabled class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-class-group-period>
                                            <option value="">Select occurrence period</option>
                                            <c:forEach var="occurrence" items="${courseOccurrenceOptions}">
                                                <c:forEach var="period" items="${occurrence.periods}">
                                                    <optgroup label="<c:out value='${occurrence.label}'/> - <c:out value='${period.label}'/>"
                                                              data-class-group-period-group
                                                              data-course-id="${occurrence.courseId}"
                                                              data-occurrence-id="${occurrence.id}">
                                                        <option value="${period.id}"
                                                                data-course-id="${occurrence.courseId}"
                                                                data-occurrence-id="${occurrence.id}"
                                                                data-occurrence-label="<c:out value='${occurrence.label}'/>"
                                                                data-period-label="<c:out value='${period.label}'/>"
                                                                data-occurrence-period-label="<c:out value='${occurrence.label}'/> - <c:out value='${period.label}'/>"
                                                                data-period-range="<c:out value='${period.dateRangeLabel}'/>"
                                                                data-period-state="<c:out value='${period.stateLabel}'/>"
                                                                data-curricular-year="${period.curricularYear}"
                                                                data-term="${period.termValue}"
                                                                data-start="${period.startsAtValue}"
                                                                data-end="${period.endsAtValue}"
                                                                ${form.courseOccurrencePeriodId == period.id ? 'selected' : ''}>
                                                            <c:out value="${period.dateRangeLabel}"/>
                                                        </option>
                                                    </optgroup>
                                                </c:forEach>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>
                            </div>

                            <div class="gape-step-panel" data-step-panel="1">
                                <span class="gape-step-eyebrow">Class group setup</span>
                                <h3 class="gape-step-title">Define how this class group runs</h3>
                                <p class="gape-step-copy">Add the operational identity that administrators, teachers and students will use to recognise this class group.</p>

                                <div class="row gy-4">
                                    <div class="col-lg-6">
                                        <label for="code" class="fw-medium text-base text-neutral-800 mb-12">Class Group Code</label>
                                        <input id="code" name="code" type="text" value="<c:out value='${form.code}'/>" required maxlength="30" pattern="[^|]*" title="Codes cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-lg-6 gape-select-field">
                                        <label for="shift" class="fw-medium text-base text-neutral-800 mb-12">Shift</label>
                                        <select id="shift" name="shift" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                            <c:forEach var="shiftOption" items="${shiftOptions}">
                                                <option value="${shiftOption.code}" ${form.shift == shiftOption.code ? 'selected' : ''}>
                                                    <c:out value="${shiftOption.label}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>

                                <div class="mt-28">
                                    <span class="d-block fw-medium text-base text-neutral-800 mb-12">Modality</span>
                                    <div class="gape-choice-grid gape-choice-grid--compact">
                                        <label class="gape-choice-card">
                                            <input type="radio" name="modality" value="ONSITE" ${form.modality == 'ONSITE' ? 'checked' : ''}>
                                            <span>
                                                <strong class="gape-choice-title">On-site</strong>
                                                <small>Physical classes in the assigned academic space.</small>
                                            </span>
                                        </label>
                                        <label class="gape-choice-card">
                                            <input type="radio" name="modality" value="ONLINE" ${form.modality == 'ONLINE' ? 'checked' : ''}>
                                            <span>
                                                <strong class="gape-choice-title">Online</strong>
                                                <small>Remote sessions and digital teaching activities.</small>
                                            </span>
                                        </label>
                                        <label class="gape-choice-card">
                                            <input type="radio" name="modality" value="HYBRID" ${form.modality == 'HYBRID' ? 'checked' : ''}>
                                            <span>
                                                <strong class="gape-choice-title">Hybrid</strong>
                                                <small>Combines classroom sessions with online work.</small>
                                            </span>
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <div class="gape-step-panel" data-step-panel="2">
                                <span class="gape-step-eyebrow">Class group access</span>
                                <h3 class="gape-step-title">Select class group access</h3>
                                <p class="gape-step-copy">The class group state is calculated from the selected occurrence period.</p>

                                <div class="mb-28">
                                    <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                                    <span class="d-inline-flex align-items-center gap-8 px-18 py-14 bg-neutral-20 border border-neutral-30 rounded-14 text-14 fw-semibold text-neutral-700" data-class-group-state-display>Draft</span>
                                </div>

                                <div class="row gy-4">
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="minStudents" class="fw-medium text-base text-neutral-800 mb-12">Min Students</label>
                                        <input id="minStudents" name="minStudents" type="number" min="1" step="1" required value="<c:out value='${form.minStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="maxStudents" class="fw-medium text-base text-neutral-800 mb-12">Max Students</label>
                                        <input id="maxStudents" name="maxStudents" type="number" min="2" step="1" required value="<c:out value='${form.maxStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-12">
                                        <div class="gape-date-context" data-date-context>
                                            <div class="gape-date-context__main">
                                                <span class="gape-date-context__icon"><i class="ph ph-calendar-dots text-20"></i></span>
                                                <span class="gape-date-context__text">
                                                    <strong class="gape-date-context__title" data-date-context-title>Occurrence period</strong>
                                                    <span class="gape-date-context__copy" data-date-context-copy>Select an occurrence period for this class group.</span>
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="mt-24">
                                    <label class="gape-choice-card">
                                        <input type="checkbox" name="showContentThumbnails" value="true" ${form.showContentThumbnails == 'true' ? 'checked' : ''}>
                                        <span>
                                            <strong class="gape-choice-title">Show file thumbnails</strong>
                                            <small>Display generated media previews inside pedagogical blocks when available.</small>
                                        </span>
                                    </label>
                                </div>
                            </div>

                            <div class="gape-step-panel" data-step-panel="3">
                                <span class="gape-step-eyebrow">Class group outline</span>
                                <h3 class="gape-step-title">Review the class group setup</h3>
                                <p class="gape-step-copy">Confirm the context, setup and occurrence period before the class group is saved.</p>

                                <div class="gape-review-outline">
                                    <div class="gape-review-block">
                                        <div class="gape-review-number">01</div>
                                        <div>
                                            <div class="gape-review-heading">
                                                <span class="gape-outline-icon"><i class="ph ph-graduation-cap"></i></span>
                                                <h4>Academic context</h4>
                                            </div>
                                            <div class="gape-review-grid">
                                                <div class="gape-review-item">
                                                    <span>Course</span>
                                                    <strong data-review-course>Not selected</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Subject</span>
                                                    <strong data-review-subject>Not selected</strong>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="gape-review-block">
                                        <div class="gape-review-number">02</div>
                                        <div>
                                            <div class="gape-review-heading">
                                                <span class="gape-outline-icon"><i class="ph ph-users-three"></i></span>
                                                <h4>Class group setup</h4>
                                            </div>
                                            <div class="gape-review-grid">
                                                <div class="gape-review-item">
                                                    <span>Code</span>
                                                    <strong data-review-code>Not set</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Modality</span>
                                                    <strong data-review-modality>On-site</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Shift</span>
                                                    <strong data-review-shift>Not set</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>State</span>
                                                    <strong data-review-state>Active</strong>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="gape-review-block">
                                        <div class="gape-review-number">03</div>
                                        <div>
                                            <div class="gape-review-heading">
                                                <span class="gape-outline-icon"><i class="ph ph-calendar-dots"></i></span>
                                                <h4>Capacity and period</h4>
                                            </div>
                                            <div class="gape-review-grid">
                                                <div class="gape-review-item">
                                                    <span>Capacity</span>
                                                    <strong data-review-capacity>Not set</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Period</span>
                                                    <strong data-review-period>Not set</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Content Preview</span>
                                                    <strong data-review-thumbnails>Icons</strong>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="gape-wizard-actions d-flex align-items-center gap-16 flex-wrap">
                                <button type="button" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-wizard-back disabled>Back</button>
                                <button type="button" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03" data-wizard-next>Continue</button>
                                <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 d-none" data-wizard-submit>${creating ? 'Create Class Group' : 'Save Changes'}</button>
                                <a href="${classGroupBackHref}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03 ms-sm-auto">Cancel</a>
                            </div>
                        </section>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-class-group-context.js?v=20260713-select-guidance-2"></script>
<script>
    (function () {
        function ready(callback) {
            if (window.jQuery) {
                window.jQuery(callback);
                return;
            }
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', callback);
                return;
            }
            callback();
        }

        ready(function () {
        var wizard = document.querySelector('[data-class-group-wizard]');
        if (!wizard) {
            return;
        }

        var panels = Array.prototype.slice.call(wizard.querySelectorAll('[data-step-panel]'));
        var indicators = Array.prototype.slice.call(wizard.querySelectorAll('[data-step-indicator]'));
        var previousButton = wizard.querySelector('[data-wizard-back]');
        var nextButton = wizard.querySelector('[data-wizard-next]');
        var submitButton = wizard.querySelector('[data-wizard-submit]');
        var courseSelect = wizard.querySelector('#courseId');
        var subjectSelect = wizard.querySelector('#subjectId');
        var creating = wizard.getAttribute('data-creating') === 'true';
        var contextManaged = wizard.hasAttribute('data-class-group-context-managed');
        var originalSubjectOptions = !contextManaged && subjectSelect
                ? Array.prototype.slice.call(subjectSelect.options).map(function (option) {
                    return {
                        value: option.value,
                        text: option.textContent,
                        parentValue: option.dataset.parentValue || '',
                        selected: option.selected
                    };
                })
                : [];
        var activeStep = 0;

        function textOrFallback(value, fallback) {
            var normalized = value ? String(value).trim() : '';
            return normalized.length > 0 ? normalized : fallback;
        }

        function selectedText(selector, fallback) {
            var control = wizard.querySelector(selector);
            if (!control) {
                return fallback;
            }
            if (control.tagName === 'INPUT') {
                return textOrFallback(control.value, fallback);
            }
            if (!control.options) {
                return textOrFallback(control.textContent, fallback);
            }
            var option = control.options[control.selectedIndex];
            if (!option || !option.value) {
                return fallback;
            }
            return textOrFallback(option.textContent.replace(/\s+/g, ' '), fallback);
        }

        function selectedSubjectText(fallback) {
            if (subjectSelect && subjectSelect.matches('[data-class-group-locked-subject]')) {
                return textOrFallback(wizard.getAttribute('data-class-group-subject-label'), fallback);
            }
            return selectedText('#subjectId', fallback);
        }

        function checkedChoiceLabel(name, fallback) {
            var choice = wizard.querySelector('input[name="' + name + '"]:checked');
            if (!choice) {
                return fallback;
            }
            var card = choice.closest('.gape-choice-card');
            var label = card ? card.querySelector('.gape-choice-title') : null;
            return textOrFallback(label ? label.textContent : choice.value, fallback);
        }

        function fieldValue(selector, fallback) {
            var control = wizard.querySelector(selector);
            return textOrFallback(control ? control.value : '', fallback);
        }

        function setText(selector, value) {
            var elements = wizard.querySelectorAll(selector);
            Array.prototype.forEach.call(elements, function (element) {
                element.textContent = value;
            });
        }

        function rebuildSubjectOptions(preserveCurrentValue) {
            if (contextManaged) {
                return;
            }
            if (!courseSelect || !subjectSelect) {
                return;
            }
            var selectedCourseId = courseSelect.value;
            var previousValue = preserveCurrentValue ? subjectSelect.value : '';
            var matchingOptions = originalSubjectOptions.filter(function (option) {
                return !option.parentValue || option.parentValue === selectedCourseId;
            });
            var canKeepPreviousValue = selectedCourseId && matchingOptions.some(function (option) {
                return option.value === previousValue;
            });

            subjectSelect.innerHTML = '';
            matchingOptions.forEach(function (optionData) {
                var option = document.createElement('option');
                option.value = optionData.value;
                option.textContent = optionData.text;
                if (optionData.parentValue) {
                    option.dataset.parentValue = optionData.parentValue;
                }
                subjectSelect.appendChild(option);
            });

            subjectSelect.value = canKeepPreviousValue ? previousValue : '';
            subjectSelect.disabled = !selectedCourseId;
        }

        function capacityLabel() {
            var minStudents = fieldValue('#minStudents', '');
            var maxStudents = fieldValue('#maxStudents', '');
            if (minStudents && maxStudents) {
                return minStudents + ' - ' + maxStudents + ' students';
            }
            if (minStudents) {
                return 'Minimum ' + minStudents;
            }
            if (maxStudents) {
                return 'Maximum ' + maxStudents;
            }
            return 'Not set';
        }

        function selectedPeriodOption() {
            var period = wizard.querySelector('#courseOccurrencePeriodId');
            return period && period.selectedIndex >= 0 ? period.options[period.selectedIndex] : null;
        }

        function selectedPeriodInfo() {
            var option = selectedPeriodOption();
            if (!option || !option.value) {
                return {
                    occurrence: '',
                    label: 'Not set',
                    range: '',
                    review: 'Not set'
                };
            }
            var occurrence = option.dataset.occurrenceLabel || '';
            var label = option.dataset.periodLabel || option.textContent.replace(/\s+/g, ' ').trim();
            var range = (option.dataset.start || '-') + ' - ' + (option.dataset.end || '-');
            return {
                occurrence: occurrence,
                label: label,
                range: range,
                review: [occurrence, label, range].filter(Boolean).join(' · ')
            };
        }

        function todayValue() {
            var now = new Date();
            var month = String(now.getMonth() + 1).padStart(2, '0');
            var day = String(now.getDate()).padStart(2, '0');
            return now.getFullYear() + '-' + month + '-' + day;
        }

        function computedStateLabel() {
            var option = selectedPeriodOption();
            var startsAt = option && option.value ? option.dataset.start || '' : '';
            var endsAt = option && option.value ? option.dataset.end || '' : '';
            var today = todayValue();
            if (!startsAt) {
                return 'Draft';
            }
            if (endsAt && endsAt <= today) {
                return 'Completed';
            }
            return startsAt <= today ? 'Active' : 'Scheduled';
        }

        function updatePreview() {
            updateCrossFieldValidity();
            var code = fieldValue('#code', 'Class group code');
            var course = selectedText('#courseId', selectedText('#courseContext', 'Course not selected'));
            var subject = selectedSubjectText(selectedText('#subjectContext', 'Subject not selected'));
            var context = course + ' - ' + subject;
            var modality = checkedChoiceLabel('modality', 'On-site');
            var state = computedStateLabel();
            var shift = selectedText('#shift', 'Not set');
            var capacity = capacityLabel();
            var period = selectedPeriodInfo();
            var thumbnailControl = wizard.querySelector('input[name="showContentThumbnails"]');
            var thumbnails = thumbnailControl && thumbnailControl.checked ? 'Thumbnails' : 'Icons';

            setText('[data-preview-code], [data-review-code]', code);
            setText('[data-preview-context]', context);
            setText('[data-preview-modality], [data-review-modality]', modality);
            setText('[data-preview-shift], [data-review-shift]', shift);
            setText('[data-preview-state], [data-review-state], [data-class-group-state-display]', state);
            setText('[data-preview-capacity], [data-review-capacity]', capacity);
            setText('[data-preview-period]', period.label);
            setText('[data-preview-period-context]', period.occurrence);
            setText('[data-review-period]', period.review);
            setText('[data-review-thumbnails]', thumbnails);
            setText('[data-review-course]', course);
            setText('[data-review-subject]', subject);
        }

        function updateCrossFieldValidity() {
            var minStudents = wizard.querySelector('#minStudents');
            var maxStudents = wizard.querySelector('#maxStudents');

            if (minStudents && maxStudents) {
                minStudents.setCustomValidity('');
                maxStudents.setCustomValidity('');
                if (minStudents.value && Number(minStudents.value) <= 0) {
                    minStudents.setCustomValidity('Minimum students must be greater than zero.');
                }
                if (minStudents.value) {
                    maxStudents.min = String(Number(minStudents.value) + 1);
                } else {
                    maxStudents.min = '2';
                }
                if (minStudents.value && maxStudents.value && Number(maxStudents.value) <= Number(minStudents.value)) {
                    maxStudents.setCustomValidity('Maximum students must be greater than minimum students.');
                }
            }
        }

        function visibleControls(panel) {
            return Array.prototype.slice.call(panel.querySelectorAll('input, select, textarea'))
                    .filter(function (control) {
                        return !control.disabled && control.type !== 'hidden';
                    });
        }

        function validateStep(index) {
            var controls = visibleControls(panels[index]);
            for (var i = 0; i < controls.length; i++) {
                if (!controls[i].checkValidity()) {
                    controls[i].reportValidity();
                    return false;
                }
            }
            return true;
        }

        function goToStep(index) {
            activeStep = Math.max(0, Math.min(index, panels.length - 1));
            panels.forEach(function (panel, panelIndex) {
                panel.classList.toggle('is-active', panelIndex === activeStep);
            });
            indicators.forEach(function (indicator, indicatorIndex) {
                indicator.classList.toggle('is-active', indicatorIndex === activeStep);
                indicator.classList.toggle('is-complete', indicatorIndex < activeStep);
            });
            previousButton.disabled = activeStep === 0;
            nextButton.classList.toggle('d-none', activeStep === panels.length - 1);
            submitButton.classList.toggle('d-none', activeStep !== panels.length - 1);
            updatePreview();
        }

        previousButton.addEventListener('click', function () {
            goToStep(activeStep - 1);
        });

        nextButton.addEventListener('click', function () {
            if (validateStep(activeStep)) {
                goToStep(activeStep + 1);
            }
        });

        indicators.forEach(function (indicator) {
            indicator.addEventListener('click', function () {
                var target = Number(indicator.getAttribute('data-step-indicator'));
                if (target > activeStep && !validateStep(activeStep)) {
                    return;
                }
                goToStep(target);
            });
        });

        Array.prototype.forEach.call(wizard.querySelectorAll('input, select, textarea'), function (control) {
            control.addEventListener('input', updatePreview);
            control.addEventListener('change', updatePreview);
        });

        if (courseSelect && subjectSelect && !contextManaged) {
            rebuildSubjectOptions(true);
            courseSelect.addEventListener('change', function () {
                rebuildSubjectOptions(true);
                updatePreview();
            });
        }
        wizard.addEventListener('gape:class-group-context-sync', updatePreview);
        updatePreview();
        window.setInterval(updatePreview, 30000);
        });
    })();
</script>
<script>
    (function () {
        function ready(callback) {
            if (window.jQuery) {
                window.jQuery(callback);
                return;
            }
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', callback);
                return;
            }
            callback();
        }

        ready(function () {
            var form = document.querySelector('[data-class-group-context-form]');
            if (!form) {
                return;
            }
            var course = form.querySelector('[data-class-group-course], input[name="courseId"]');
            var subject = form.querySelector('[data-class-group-subject], input[name="subjectId"]');
            var period = form.querySelector('[data-class-group-period]');
            var periodWrapper = form.querySelector('[data-class-group-period-context]');
            var occurrence = form.querySelector('[data-class-group-occurrence]');
            var title = form.querySelector('[data-date-context-title]');
            var copy = form.querySelector('[data-date-context-copy]');
            var reviewPeriod = form.querySelector('[data-review-period]');
            if (!period || !occurrence) {
                return;
            }

            function selectedSubjectOption() {
                if (!subject || !subject.options) {
                    return null;
                }
                return subject.options[subject.selectedIndex] || null;
            }

            function selectedCourseId() {
                return course ? course.value : '';
            }

            function selectedCourseOption() {
                if (!course || !course.options) {
                    return null;
                }
                return course.options[course.selectedIndex] || null;
            }

            function normalizedCode(value) {
                return (value || '').toUpperCase();
            }

            function selectedSubjectYear() {
                var option = selectedSubjectOption();
                if (option && option.dataset && option.dataset.curricularYear) {
                    return option.dataset.curricularYear;
                }
                var courseOption = selectedCourseOption();
                if (courseOption && courseOption.dataset && courseOption.dataset.subjectCurricularYear) {
                    return courseOption.dataset.subjectCurricularYear;
                }
                return form ? form.getAttribute('data-class-group-selected-year') || '' : '';
            }

            function selectedSubjectTerm() {
                var option = selectedSubjectOption();
                if (option && option.dataset && option.dataset.term) {
                    return option.dataset.term;
                }
                var courseOption = selectedCourseOption();
                if (courseOption && courseOption.dataset && courseOption.dataset.subjectTerm) {
                    return courseOption.dataset.subjectTerm;
                }
                return form ? form.getAttribute('data-class-group-selected-term') || '' : '';
            }

            function refreshSelect(select) {
                if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
                    window.jQuery(select).prop('disabled', select.disabled).trigger('change.select2');
                }
            }

            function setPeriodFieldState(enabled) {
                period.disabled = !enabled;
                if (periodWrapper) {
                    periodWrapper.classList.toggle('opacity-75', !enabled);
                    periodWrapper.classList.toggle('is-disabled', !enabled);
                }
            }

            function setPeriodUnavailableGuidance(message) {
                if (!periodWrapper) {
                    return;
                }
                var Tooltip = window.bootstrap && window.bootstrap.Tooltip;
                var existingTooltip = Tooltip && Tooltip.getInstance
                        ? Tooltip.getInstance(periodWrapper)
                        : null;
                if (existingTooltip) {
                    existingTooltip.dispose();
                }
                if (!message) {
                    delete periodWrapper.dataset.gapePeriodUnavailableReason;
                    periodWrapper.removeAttribute('data-gape-period-unavailable');
                    periodWrapper.removeAttribute('data-bs-toggle');
                    periodWrapper.removeAttribute('data-bs-placement');
                    periodWrapper.removeAttribute('data-bs-original-title');
                    periodWrapper.removeAttribute('title');
                    periodWrapper.removeAttribute('tabindex');
                    return;
                }
                periodWrapper.dataset.gapePeriodUnavailableReason = message;
                periodWrapper.setAttribute('data-gape-period-unavailable', 'true');
                periodWrapper.setAttribute('title', message);
                periodWrapper.setAttribute('tabindex', '0');
                if (!Tooltip) {
                    return;
                }
                periodWrapper.setAttribute('data-bs-toggle', 'tooltip');
                periodWrapper.setAttribute('data-bs-placement', 'top');
                new Tooltip(periodWrapper, {
                    boundary: 'viewport',
                    placement: 'top',
                    trigger: 'hover focus'
                });
            }

            function todayValue() {
                var now = new Date();
                return now.getFullYear() + '-'
                    + String(now.getMonth() + 1).padStart(2, '0') + '-'
                    + String(now.getDate()).padStart(2, '0');
            }

            function isPastPeriod(option) {
                return !!(option && option.value && option.dataset.end && option.dataset.end < todayValue());
            }

            function syncPeriodGroups() {
                Array.prototype.forEach.call(
                        period.querySelectorAll('optgroup[data-class-group-period-group]'),
                        function (group) {
                            var hasVisibleOption = Array.prototype.some.call(group.querySelectorAll('option'), function (option) {
                                return !!option.value && !option.hidden;
                            });
                            group.hidden = !hasVisibleOption;
                        }
                );
            }

            function periodSearchText(data) {
                var option = data && data.element;
                if (!option || !option.dataset) {
                    return data && data.text ? data.text : '';
                }
                return [
                    data.text,
                    option.dataset.occurrencePeriodLabel,
                    option.dataset.periodLabel,
                    option.dataset.periodRange,
                    option.dataset.periodState
                ].filter(Boolean).join(' ');
            }

            function periodMatcher(params, data) {
                if (!data) {
                    return null;
                }
                if (data.children && data.children.length) {
                    if (data.element && data.element.hidden) {
                        return null;
                    }
                    var matchingGroup = window.jQuery.extend(true, {}, data);
                    matchingGroup.children = data.children.map(function (child) {
                        return periodMatcher(params, child);
                    }).filter(Boolean);
                    return matchingGroup.children.length ? matchingGroup : null;
                }
                if (data.element && data.element.hidden) {
                    return null;
                }
                var term = params && params.term ? params.term.trim().toLowerCase() : '';
                return !term || periodSearchText(data).toLowerCase().indexOf(term) >= 0 ? data : null;
            }

            function periodTemplate(data) {
                if (data && data.children && data.children.length) {
                    var group = document.createElement('span');
                    group.className = 'gape-class-group-period-group-label';
                    group.textContent = data.text || '';
                    return group;
                }
                return data ? data.text : '';
            }

            function periodSelectionTemplate(data) {
                var option = data && data.element;
                if (!option || !option.value) {
                    return data ? data.text : '';
                }
                var selection = document.createElement('span');
                selection.className = 'gape-class-group-period-selection';
                selection.textContent = option.dataset.occurrencePeriodLabel || data.text || '';
                return selection;
            }

            function initializePeriodSelect() {
                if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
                    return;
                }
                var selectUi = window.jQuery(period);
                if (selectUi.data('select2')) {
                    selectUi.select2('destroy');
                }
                selectUi.select2({
                    width: '100%',
                    selectionCssClass: 'gape-eduall-selection',
                    dropdownCssClass: 'gape-eduall-select-dropdown gape-class-group-period-dropdown',
                    matcher: periodMatcher,
                    templateResult: periodTemplate,
                    templateSelection: periodSelectionTemplate
                });
            }

            function syncPeriodOptions() {
                var courseId = selectedCourseId();
                var year = selectedSubjectYear();
                var term = normalizedCode(selectedSubjectTerm());
                var previous = period.value;
                var hasContext = !!(courseId && year && term);
                var hasMatchingPeriod = false;
                var hasAvailable = false;
                Array.prototype.forEach.call(period.options, function (option) {
                    if (!option.value) {
                        option.hidden = false;
                        option.disabled = false;
                        option.dataset.unavailableReason = '';
                        option.title = '';
                        return;
                    }
                    var matches = hasContext
                            && option.dataset.courseId === courseId
                            && option.dataset.curricularYear === year
                            && normalizedCode(option.dataset.term) === term;
                    var unavailableReason = !matches
                            ? 'This occurrence period does not match the selected course and subject.'
                            : isPastPeriod(option)
                                    ? 'This occurrence period ended on ' + option.dataset.end + ' and cannot receive a new class group.'
                                    : '';
                    option.hidden = !matches;
                    option.disabled = !!unavailableReason;
                    option.dataset.unavailableReason = unavailableReason;
                    option.title = unavailableReason;
                    hasMatchingPeriod = hasMatchingPeriod || matches;
                    hasAvailable = hasAvailable || (matches && !option.disabled);
                });
                syncPeriodGroups();
                if (previous && period.querySelector('option[value="' + CSS.escape(previous) + '"]:not([disabled])')) {
                    period.value = previous;
                } else {
                    var first = Array.prototype.find.call(period.options, function (option) {
                        return option.value && !option.disabled && !option.hidden;
                    });
                    period.value = first ? first.value : '';
                }
                // Once course and subject identify a real context, keep the
                // selector usable even if every matching period is historical.
                // Those rows stay visible as disabled options, instead of
                // hiding the chronology behind a disabled field.
                setPeriodFieldState(hasMatchingPeriod);
                setPeriodUnavailableGuidance(
                        hasContext && !hasAvailable
                                ? 'The selected course and subject have no current or future occurrence period.'
                                : ''
                );
                syncPeriodContext();
                refreshSelect(period);
            }

            function syncPeriodContext() {
                var option = period.options[period.selectedIndex];
                if (!option || !option.value) {
                    occurrence.value = '';
                    if (title) {
                        title.textContent = 'Occurrence period';
                    }
                    if (copy) {
                        copy.textContent = 'Select an occurrence period for this class group.';
                    }
                    if (reviewPeriod) {
                        reviewPeriod.textContent = 'Not set';
                    }
                    var hasAvailablePeriod = !!period.querySelector(
                            'option[value]:not([hidden]):not([disabled])'
                    );
                    period.setCustomValidity(
                            period.disabled || !hasAvailablePeriod
                                    ? 'The selected course/subject has no current or future occurrence period.'
                                    : ''
                    );
                    return;
                }
                occurrence.value = option.dataset.occurrenceId || '';
                var range = (option.dataset.start || '-') + ' - ' + (option.dataset.end || '-');
                if (title) {
                    title.textContent = 'Occurrence period';
                }
                if (copy) {
                    copy.textContent = option.textContent.replace(/\s+/g, ' ').trim();
                }
                if (reviewPeriod) {
                    reviewPeriod.textContent = range;
                }
                period.setCustomValidity('');
            }

            ['change', 'input'].forEach(function (eventName) {
                if (course) {
                    course.addEventListener(eventName, syncPeriodOptions);
                }
                if (subject) {
                    subject.addEventListener(eventName, syncPeriodOptions);
                }
                period.addEventListener(eventName, syncPeriodContext);
            });
            if (window.jQuery) {
                window.jQuery(course).on('select2:select select2:clear change', syncPeriodOptions);
                window.jQuery(subject).on('select2:select select2:clear change', syncPeriodOptions);
                window.jQuery(period).on('select2:select select2:clear change', syncPeriodContext);
            }
            initializePeriodSelect();
            syncPeriodOptions();
        });
    })();
</script>
</body>
</html>
