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
    <title>GAPE - Class Group</title>
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
                <form action="${formAction}" method="post" class="gape-class-group-wizard bg-white rounded-10 px-40 py-40" data-class-group-wizard>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-32">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Class Group' : 'Edit Class Group'}</h2>
                            <span class="text-14 text-neutral-500">Set the class group context, operating model and availability before saving.</span>
                        </div>
                        <a href="${classGroupBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

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
                                        <span>Dates</span>
                                        <strong data-preview-dates>Not set</strong>
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
                                            <div class="col-lg-6 gape-select-field">
                                                <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                                <select id="courseId" name="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14">
                                                    <option value="">Select course</option>
                                                    <c:forEach var="course" items="${courseOptions}">
                                                        <option value="${course.id}" ${form.courseId == course.id ? 'selected' : ''}>
                                                            <c:out value="${course.name}"/>
                                                        </option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <div class="col-lg-6 gape-select-field">
                                                <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                <select id="subjectId" name="subjectId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14">
                                                    <option value="">Select subject</option>
                                                    <c:forEach var="association" items="${courseSubjectOptions}">
                                                        <option value="${association.subjectId}" data-parent-value="${association.courseId}" ${form.subjectId == association.subjectId and form.courseId == association.courseId ? 'selected' : ''}>
                                                            <c:out value="${association.subjectName}"/> - <c:out value="${association.curricularPositionLabel}"/>
                                                        </option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="col-lg-6">
                                                <label for="courseContext" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                                <input id="courseContext" type="text" value="<c:out value='${classGroup.courseName}'/>" readonly class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                                <input type="hidden" name="courseId" value="${form.courseId}">
                                            </div>
                                            <div class="col-lg-6">
                                                <label for="subjectContext" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                                <input id="subjectContext" type="text" value="<c:out value='${classGroup.subjectName}'/>" readonly class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                                <input type="hidden" name="subjectId" value="${form.subjectId}">
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
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
                                <p class="gape-step-copy">The class group state is calculated from the date window. Leave the start date empty to keep it as draft.</p>

                                <div class="mb-28">
                                    <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                                    <span class="d-inline-flex align-items-center gap-8 px-18 py-14 bg-neutral-20 border border-neutral-30 rounded-14 text-14 fw-semibold text-neutral-700" data-class-group-state-display>Draft</span>
                                </div>

                                <div class="row gy-4">
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="minStudents" class="fw-medium text-base text-neutral-800 mb-12">Min Students</label>
                                        <input id="minStudents" name="minStudents" type="number" min="0" step="1" value="<c:out value='${form.minStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="maxStudents" class="fw-medium text-base text-neutral-800 mb-12">Max Students</label>
                                        <input id="maxStudents" name="maxStudents" type="number" min="0" step="1" value="<c:out value='${form.maxStudents}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="startsAt" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                                        <input id="startsAt" name="startsAt" type="date" value="<c:out value='${form.startsAt}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-lg-3 col-sm-6">
                                        <label for="endsAt" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                                        <input id="endsAt" name="endsAt" type="date" value="<c:out value='${form.endsAt}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
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
                                <p class="gape-step-copy">Confirm the context, setup and access window before the class group is saved.</p>

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
                                                <h4>Capacity and dates</h4>
                                            </div>
                                            <div class="gape-review-grid">
                                                <div class="gape-review-item">
                                                    <span>Capacity</span>
                                                    <strong data-review-capacity>Not set</strong>
                                                </div>
                                                <div class="gape-review-item">
                                                    <span>Dates</span>
                                                    <strong data-review-dates>Not set</strong>
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
                <c:if test="${not creating}">
                    <div class="mt-24">
                        <%@ include file="/WEB-INF/fragments/class-group-enrollment-management.jspf" %>
                        <%@ include file="/WEB-INF/fragments/class-group-teacher-management.jspf" %>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
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
        var originalSubjectOptions = subjectSelect
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
            var option = control.options[control.selectedIndex];
            if (!option || !option.value) {
                return fallback;
            }
            return textOrFallback(option.textContent.replace(/\s+/g, ' '), fallback);
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

        function dateLabel() {
            var startsAt = fieldValue('#startsAt', '');
            var endsAt = fieldValue('#endsAt', '');
            if (startsAt && endsAt) {
                return startsAt + ' - ' + endsAt;
            }
            if (startsAt) {
                return 'Starts ' + startsAt;
            }
            if (endsAt) {
                return 'Ends ' + endsAt;
            }
            return 'Not set';
        }

        function todayValue() {
            var now = new Date();
            var month = String(now.getMonth() + 1).padStart(2, '0');
            var day = String(now.getDate()).padStart(2, '0');
            return now.getFullYear() + '-' + month + '-' + day;
        }

        function computedStateLabel() {
            var startsAt = fieldValue('#startsAt', '');
            var endsAt = fieldValue('#endsAt', '');
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
            var subject = selectedText('#subjectId', selectedText('#subjectContext', 'Subject not selected'));
            var context = course + ' - ' + subject;
            var modality = checkedChoiceLabel('modality', 'On-site');
            var state = computedStateLabel();
            var shift = selectedText('#shift', 'Not set');
            var capacity = capacityLabel();
            var dates = dateLabel();
            var thumbnailControl = wizard.querySelector('input[name="showContentThumbnails"]');
            var thumbnails = thumbnailControl && thumbnailControl.checked ? 'Thumbnails' : 'Icons';

            setText('[data-preview-code], [data-review-code]', code);
            setText('[data-preview-context]', context);
            setText('[data-preview-modality], [data-review-modality]', modality);
            setText('[data-preview-shift], [data-review-shift]', shift);
            setText('[data-preview-state], [data-review-state], [data-class-group-state-display]', state);
            setText('[data-preview-capacity], [data-review-capacity]', capacity);
            setText('[data-preview-dates], [data-review-dates]', dates);
            setText('[data-review-thumbnails]', thumbnails);
            setText('[data-review-course]', course);
            setText('[data-review-subject]', subject);
        }

        function updateCrossFieldValidity() {
            var minStudents = wizard.querySelector('#minStudents');
            var maxStudents = wizard.querySelector('#maxStudents');
            var startsAt = wizard.querySelector('#startsAt');
            var endsAt = wizard.querySelector('#endsAt');

            if (minStudents && maxStudents) {
                maxStudents.setCustomValidity('');
                if (minStudents.value && maxStudents.value && Number(minStudents.value) > Number(maxStudents.value)) {
                    maxStudents.setCustomValidity('Maximum students must be greater than or equal to minimum students.');
                }
            }

            if (startsAt && endsAt) {
                var today = todayValue();
                startsAt.min = today;
                endsAt.min = startsAt.value || today;
                startsAt.setCustomValidity('');
                endsAt.setCustomValidity('');
                if (endsAt.value && !startsAt.value) {
                    startsAt.setCustomValidity('Start date is required when an end date is set.');
                }
                if (startsAt.value && startsAt.value < today) {
                    startsAt.setCustomValidity('Start date cannot be in the past.');
                }
                if (endsAt.value && endsAt.value < today) {
                    endsAt.setCustomValidity('End date cannot be in the past.');
                }
                if (startsAt.value && endsAt.value && startsAt.value > endsAt.value) {
                    endsAt.setCustomValidity('End date cannot be before start date.');
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

        if (courseSelect && subjectSelect) {
            rebuildSubjectOptions(true);
            courseSelect.addEventListener('change', function () {
                rebuildSubjectOptions(true);
                updatePreview();
            });
        }

        updatePreview();
        window.setInterval(updatePreview, 30000);
        });
    })();
</script>
</body>
</html>
