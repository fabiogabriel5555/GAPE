<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "courses");
    }
%>
<c:set var="courseBackHref" value="${pageContext.request.contextPath}${courseBasePath}"/>
<c:if test="${not creating}">
    <c:set var="courseBackHref" value="${pageContext.request.contextPath}${courseBasePath}/${form.id}"/>
</c:if>
<c:set var="courseHasPhoto" value="${not empty form.photo}"/>
<c:set var="coursePhotoUrl" value=""/>
<c:if test="${courseHasPhoto}">
    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${form.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Course</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-course-photo-preview {
            background-position: center center;
            background-repeat: no-repeat;
            background-size: cover;
            border-radius: 16px;
            height: 132px;
            width: 176px;
        }

        .gape-period-builder {
            background: #fff;
            border: 1px solid #e6edf0;
            border-radius: 8px;
            padding: 18px;
        }

        .gape-period-scroll {
            overflow-x: auto;
            overflow-y: visible;
            padding: 10px 2px 16px;
            scrollbar-color: #94a3b8 #eef2f5;
            scrollbar-width: thin;
        }

        .gape-period-list {
            display: grid;
            gap: 22px 10px;
            grid-template-columns: repeat(60, minmax(0, 1fr));
            min-width: var(--gape-period-list-min-width, 0px);
            padding: 0;
            width: max(100%, var(--gape-period-list-min-width, 0px));
        }

        .gape-period-row {
            align-items: end;
            background: linear-gradient(145deg, #ffffff, #f8fbfb);
            border: 1px solid #e6edf0;
            border-radius: 12px;
            box-shadow: 0 8px 20px rgba(15, 23, 42, .045);
            display: grid;
            gap: 12px;
            grid-template-columns: minmax(180px, 1fr) repeat(2, minmax(132px, 180px));
            grid-column: 1 / -1;
            min-width: 0;
            padding: 12px;
            position: relative;
            transform: translateY(var(--gape-period-card-offset, 0px));
            transition: box-shadow .2s ease, transform .2s ease;
        }

        .gape-period-row:hover,
        .gape-period-row:focus-within {
            box-shadow: 0 12px 28px rgba(37, 99, 235, .12);
            z-index: 1;
        }

        .gape-period-row[data-period-card-density="compact"] {
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .gape-period-row[data-period-card-density="compact"] .gape-period-token {
            grid-column: 1 / -1;
        }

        .gape-period-row[data-period-card-density="narrow"] {
            grid-template-columns: minmax(0, 1fr);
        }

        .gape-period-row[data-period-card-density="narrow"] .gape-period-token {
            grid-column: auto;
        }

        .gape-period-token {
            align-items: center;
            display: flex;
            gap: 10px;
            min-width: 0;
        }

        .gape-period-token__icon {
            align-items: center;
            background: #eff6ff;
            border-radius: 8px;
            color: #2563eb;
            display: inline-flex;
            flex: 0 0 38px;
            height: 38px;
            justify-content: center;
            width: 38px;
        }

        .gape-period-token__text {
            min-width: 0;
        }

        .gape-period-token__text strong,
        .gape-period-token__text span {
            display: block;
            overflow-wrap: anywhere;
            white-space: normal;
        }

        .gape-period-date label {
            color: #64748b;
            display: block;
            font-size: 12px;
            font-weight: 600;
            margin-bottom: 6px;
        }

        .gape-period-row.is-invalid {
            background: #fffafa;
            border-color: #fecaca;
        }

        .gape-period-date input.is-invalid {
            background: #fff7f7 !important;
            border-color: #dc2626 !important;
            box-shadow: 0 0 0 3px rgba(220, 38, 38, .08);
            color: #991b1b !important;
        }

        .gape-course-submit:disabled {
            cursor: not-allowed;
            opacity: .58;
        }

        .gape-course-duration-field .select2-container {
            width: 100% !important;
        }

        .gape-course-frequency-field.is-disabled .select2-container--default .select2-selection--single,
        .gape-course-frequency-field.is-disabled .form-select {
            background: #f3f6f8;
            border-color: #e5edf0;
            cursor: not-allowed;
            opacity: .72;
        }

        .gape-course-frequency-field.is-disabled .select2-selection__rendered {
            color: #94a3b8 !important;
        }

        .gape-course-frequency-field.is-disabled .select2-selection__arrow {
            opacity: .35;
        }

        @media (max-width: 767px) {
            .gape-period-scroll {
                overflow-x: visible;
                padding: 4px 0;
            }

            .gape-period-list {
                display: grid;
                gap: 12px;
                grid-template-columns: minmax(0, 1fr);
                min-width: 0;
                padding: 4px 0;
                width: 100%;
            }

            .gape-period-row {
                grid-column: auto !important;
                grid-row: auto !important;
                grid-template-columns: 1fr;
                transform: none !important;
            }

            .gape-period-row[data-period-card-density="compact"] {
                grid-template-columns: 1fr;
            }

            .gape-period-row[data-period-card-density="compact"] .gape-period-token {
                grid-column: auto;
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
                <form action="${formAction}" method="post" enctype="multipart/form-data" class="bg-white rounded-10 px-40 py-40" data-course-context-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" id="photo" name="photo" value="<c:out value='${form.photo}'/>">
                    <input type="hidden" name="periodTemplates" value="" data-course-period-payload>
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Course' : 'Edit Course'}</h2>
                            <span class="text-14 text-neutral-500">Course and organic unit must belong to the same organization.</span>
                        </div>
                        <a href="${courseBackHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="border-bottom-dashed pb-24 mb-24">
                        <h4 class="text-18 fw-normal text-neutral-700 mb-16">Course Photo</h4>
                        <div class="avatar-upload">
                            <div class="d-flex align-items-center gap-32 flex-wrap">
                                <div class="avatar-preview flex-shrink-0">
                                    <c:choose>
                                        <c:when test="${courseHasPhoto}">
                                            <div id="courseImagePreview"
                                                 class="gape-course-photo-preview gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--image-form is-image"
                                                 data-current-image="<c:out value='${coursePhotoUrl}'/>"
                                                 data-has-current-image="true"
                                                 style="background-image: url('<c:out value='${coursePhotoUrl}'/>');">
                                                <i class="ph ph-image d-none" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div id="courseImagePreview"
                                                 class="gape-course-photo-preview gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--image-form"
                                                 data-current-image=""
                                                 data-has-current-image="false">
                                                <i class="ph ph-image" data-photo-placeholder-icon></i>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="avatar-edit">
                                    <input type="file" id="courseImageUpload" name="courseImage" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp">
                                </div>
                                <div class="d-flex align-items-center gap-16 flex-wrap">
                                    <label for="courseImageUpload" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-16 text-white transition-04 hover-bg-main-700">Upload Image</label>
                                    <button type="button" id="cancelCourseImage" class="border-main-600 border px-24 py-12 rounded-12 fw-semibold text-16 text-main-600 hover-bg-main-50 transition-04">Cancel</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <c:if test="${empty organizationOptions}">
                        <div class="alert alert-warning rounded-12 border-0" role="alert">No managed organizations are available.</div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-6 gape-select-field gape-course-context-field" data-course-organization-context>
                            <label for="organizationId" class="fw-medium text-base text-neutral-800 mb-12">Organization</label>
                            <select id="organizationId" name="organizationId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-course-organization>
                                <option value="">Select organization</option>
                                <c:forEach var="organization" items="${organizationOptions}">
                                    <option value="${organization.id}" ${form.organizationId == organization.id or selectedOrganizationId == organization.id ? 'selected' : ''}>
                                        <c:out value="${organization.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-6 gape-select-field gape-course-context-field${selectedOrganizationId == 0 ? ' opacity-75 is-disabled' : ''}" data-course-organic-unit-context>
                            <label for="organicUnitId" class="fw-medium text-base text-neutral-800 mb-12">Organic Unit</label>
                            <select id="organicUnitId" name="organicUnitId" ${selectedOrganizationId == 0 ? 'disabled' : ''} class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-course-organic-unit>
                                <option value="">No organic unit</option>
                                <c:forEach var="unit" items="${organicUnitOptions}">
                                    <option value="${unit.id}" data-organization-id="${unit.organizationId}" ${selectedOrganizationId != unit.organizationId ? 'hidden disabled' : ''} ${form.organicUnitId == unit.id ? 'selected' : ''}>
                                        <c:out value="${unit.code}"/> - <c:out value="${unit.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-7">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required pattern="[^\|]*" title="Names cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-2">
                            <label for="acronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="acronym" name="acronym" type="text" value="<c:out value='${form.acronym}'/>" required pattern="[^\|]*" title="Acronyms cannot contain |" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="ects" class="fw-medium text-base text-neutral-800 mb-12">ECTS</label>
                            <input id="ects" name="ects" type="number" min="0.01" step="0.01" required value="<c:out value='${form.ects}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3">
                            <label for="certificateMaxGrade" class="fw-medium text-base text-neutral-800 mb-12">Certificate max grade</label>
                            <input id="certificateMaxGrade" name="certificateMaxGrade" type="number" min="0.01" step="0.01" required value="<c:out value='${form.certificateMaxGrade}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-3 gape-select-field gape-course-duration-field">
                            <label for="duration" class="fw-medium text-base text-neutral-800 mb-12">Duration (in years)</label>
                            <select id="duration" name="duration" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="" ${empty form.duration ? 'selected' : ''}>Select duration</option>
                                <c:forEach var="year" begin="1" end="100">
                                    <option value="${year}" ${form.duration == year ? 'selected' : ''}>${year} ${year == 1 ? 'year' : 'years'}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-3 gape-select-field gape-course-frequency-field${empty form.duration ? ' is-disabled' : ''}" data-course-frequency-field>
                            <label for="frequency" class="fw-medium text-base text-neutral-800 mb-12">Frequency</label>
                            <select id="frequency" name="frequency" required ${empty form.duration ? 'disabled' : ''} class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select" data-course-frequency>
                                <option value="" ${empty form.frequency ? 'selected' : ''}>Select frequency</option>
                                <c:forEach var="frequency" items="${courseFrequencyOptions}">
                                    <option value="${frequency.value}" title="<c:out value='${frequency.title}'/>" ${frequency.selected ? 'selected' : ''}>
                                        <c:out value="${frequency.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <div id="course-periods" class="gape-period-builder" data-course-period-builder>
                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-14">
                                    <div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-2">Course Periods</h4>
                                        <span class="text-13 text-neutral-500" data-course-period-summary></span>
                                        <p class="text-12 text-neutral-500 mt-4 mb-0">Dates recur every occurrence. Periods may overlap and may continue into the following calendar year.</p>
                                    </div>
                                </div>
                                <div class="gape-period-scroll" data-course-period-scroll>
                                    <div class="gape-period-list" data-course-period-list>
                                        <c:forEach var="period" items="${coursePeriodTemplatesForForm}">
                                            <div class="gape-period-row" data-course-period-row>
                                                <input type="hidden" value="${period.curricularYear}" data-period-year>
                                                <input type="hidden" value="${period.term}" data-period-term>
                                                <div class="gape-period-token">
                                                    <span class="gape-period-token__icon"><i class="ph ph-calendar-dots"></i></span>
                                                    <span class="gape-period-token__text">
                                                        <strong class="text-14 text-neutral-800"><c:out value="${period.periodLabel}"/></strong>
                                                    </span>
                                                </div>
                                                <div class="gape-period-date">
                                                    <label>Start date (DD-MM)</label>
                                                    <input type="text" required inputmode="numeric" pattern="[0-9]{2}-[0-9]{2}" placeholder="DD-MM" value="<c:out value='${period.startsAt}'/>" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-white border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600" data-period-start>
                                                </div>
                                                <div class="gape-period-date">
                                                    <label>End date (DD-MM)</label>
                                                    <input type="text" required inputmode="numeric" pattern="[0-9]{2}-[0-9]{2}" placeholder="DD-MM" value="<c:out value='${period.endsAt}'/>" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-white border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600" data-period-end>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-6 gape-select-field">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="DEGREE" ${form.type == 'DEGREE' ? 'selected' : ''}>Degree</option>
                                <option value="MASTER" ${form.type == 'MASTER' ? 'selected' : ''}>Master</option>
                                <option value="SHORT_COURSE" ${form.type == 'SHORT_COURSE' ? 'selected' : ''}>Short course</option>
                                <option value="PROFESSIONAL_TRAINING" ${form.type == 'PROFESSIONAL_TRAINING' ? 'selected' : ''}>Professional training</option>
                                <option value="OTHER" ${form.type == 'OTHER' ? 'selected' : ''}>Other</option>
                            </select>
                        </div>
                        <div class="col-lg-6 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" rows="4" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600"><c:out value="${form.description}"/></textarea>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="gape-course-submit bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Save Change</button>
                        <a href="${courseBackHref}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-course-context.js?v=20260706-course-context"></script>
<script>
    (function () {
        const builder = document.querySelector('[data-course-period-builder]');
        if (!builder) {
            return;
        }
        const form = builder.closest('form');
        const durationInput = document.getElementById('duration');
        const frequencySelect = document.getElementById('frequency');
        const frequencyField = document.querySelector('[data-course-frequency-field]');
        const payloadInput = document.querySelector('[data-course-period-payload]');
        const submitButton = form ? form.querySelector('.gape-course-submit') : null;
        const list = builder.querySelector('[data-course-period-list]');
        const summary = builder.querySelector('[data-course-period-summary]');
        const frequencies = {
            MONTHLY: {
                months: 1,
                terms: [
                    ['MONTH_1', '1st Month'], ['MONTH_2', '2nd Month'], ['MONTH_3', '3rd Month'], ['MONTH_4', '4th Month'],
                    ['MONTH_5', '5th Month'], ['MONTH_6', '6th Month'], ['MONTH_7', '7th Month'], ['MONTH_8', '8th Month'],
                    ['MONTH_9', '9th Month'], ['MONTH_10', '10th Month'], ['MONTH_11', '11th Month'], ['MONTH_12', '12th Month']
                ]
            },
            BIMONTHLY: {
                months: 2,
                terms: [
                    ['BIMESTER_1', '1st Bimester'], ['BIMESTER_2', '2nd Bimester'], ['BIMESTER_3', '3rd Bimester'],
                    ['BIMESTER_4', '4th Bimester'], ['BIMESTER_5', '5th Bimester'], ['BIMESTER_6', '6th Bimester']
                ]
            },
            TRIMESTER: {
                months: 3,
                terms: [
                    ['TRIMESTER_1', '1st Trimester'], ['TRIMESTER_2', '2nd Trimester'],
                    ['TRIMESTER_3', '3rd Trimester'], ['TRIMESTER_4', '4th Trimester']
                ]
            },
            QUADRIMESTER: {
                months: 4,
                terms: [
                    ['QUADRIMESTER_1', '1st Quadrimester'], ['QUADRIMESTER_2', '2nd Quadrimester'],
                    ['QUADRIMESTER_3', '3rd Quadrimester']
                ]
            },
            SEMESTER: {
                months: 6,
                terms: [['SEMESTER_1', '1st Semester'], ['SEMESTER_2', '2nd Semester']]
            },
            ANNUAL: {
                months: 12,
                terms: [['ANNUAL', 'Annual']]
            }
        };

        function pad(value) {
            return String(value).padStart(2, '0');
        }

        function daysInMonth(month) {
            return new Date(2001, month, 0).getDate();
        }

        function ordinalLabel(value) {
            const rem100 = value % 100;
            if (rem100 >= 11 && rem100 <= 13) {
                return value + 'th';
            }
            switch (value % 10) {
                case 1:
                    return value + 'st';
                case 2:
                    return value + 'nd';
                case 3:
                    return value + 'rd';
                default:
                    return value + 'th';
            }
        }

        function coursePeriodLabel(year, termValue, termLabel) {
            const frequency = frequencies[frequencySelect.value];
            const position = frequency
                    ? frequency.terms.findIndex(function (term) { return term[0] === termValue; }) + 1
                    : 1;
            const periodPosition = ((year - 1) * (frequency ? frequency.terms.length : 1)) + position;
            if (termValue === 'ANNUAL') {
                return ordinalLabel(periodPosition) + ' Course Year';
            }
            return ordinalLabel(periodPosition) + ' ' + termLabel.replace(/^\d+(?:st|nd|rd|th)\s+/, '');
        }

        function defaultRange(frequency, index) {
            const startMonth = (index * frequency.months) + 1;
            const endMonth = (index + 1) * frequency.months;
            return {
                start: '01-' + pad(startMonth),
                end: pad(daysInMonth(endMonth)) + '-' + pad(endMonth)
            };
        }

        function rowHtml(year, termValue, termLabel, start, end) {
            return '<div class="gape-period-row" data-course-period-row>'
                + '<input type="hidden" value="' + year + '" data-period-year>'
                + '<input type="hidden" value="' + termValue + '" data-period-term>'
                + '<div class="gape-period-token">'
                + '<span class="gape-period-token__icon"><i class="ph ph-calendar-dots"></i></span>'
                + '<span class="gape-period-token__text">'
                + '<strong class="text-14 text-neutral-800">' + coursePeriodLabel(year, termValue, termLabel) + '</strong>'
                + '</span></div>'
                + '<div class="gape-period-date"><label>Start date (DD-MM)</label>'
                + '<input type="text" required inputmode="numeric" pattern="[0-9]{2}-[0-9]{2}" placeholder="DD-MM" value="' + start + '" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-white border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600" data-period-start>'
                + '</div>'
                + '<div class="gape-period-date"><label>End date (DD-MM)</label>'
                + '<input type="text" required inputmode="numeric" pattern="[0-9]{2}-[0-9]{2}" placeholder="DD-MM" value="' + end + '" class="form-control px-16 py-10 fw-normal text-14 text-neutral-700 bg-white border-neutral-30 border rounded-8 focus-visible-outline focus-border-main-600" data-period-end>'
                + '</div></div>';
        }

        function coursePeriodRows() {
            return Array.from(list.querySelectorAll('[data-course-period-row]'));
        }

        function updateCoursePeriodPayload() {
            if (!payloadInput) {
                return;
            }
            payloadInput.value = coursePeriodRows().map(function (row) {
                const year = row.querySelector('[data-period-year]').value;
                const term = row.querySelector('[data-period-term]').value;
                const start = row.querySelector('[data-period-start]').value.trim();
                const end = row.querySelector('[data-period-end]').value.trim();
                return [year, term, start, end].join('|');
            }).join('\n');
        }

        function setPeriodSubmitBlocked(blocked) {
            if (!submitButton) {
                return;
            }
            submitButton.disabled = blocked;
            if (blocked) {
                submitButton.setAttribute('aria-disabled', 'true');
                submitButton.setAttribute('title', 'Resolve the highlighted course period dates before saving.');
            } else {
                submitButton.removeAttribute('aria-disabled');
                submitButton.removeAttribute('title');
            }
        }

        function clearPeriodValidity(input) {
            input.setCustomValidity('');
            input.classList.remove('is-invalid');
            input.removeAttribute('aria-invalid');
            input.removeAttribute('title');
        }

        function markPeriodInvalid(input, message, invalidInputs) {
            if (!input || invalidInputs.has(input)) {
                return;
            }
            input.setCustomValidity(message);
            input.classList.add('is-invalid');
            input.setAttribute('aria-invalid', 'true');
            input.setAttribute('title', message);
            invalidInputs.add(input);
        }

        function refreshSelect2DisabledState(select) {
            const disabled = select.disabled;
            if (window.jQuery && window.jQuery.fn && window.jQuery(select).data('select2')) {
                window.jQuery(select).prop('disabled', disabled).trigger('change.select2');
                if (disabled) {
                    window.jQuery(select).select2('close');
                }
            }
            const container = select.nextElementSibling;
            if (container && container.classList.contains('select2-container')) {
                container.classList.toggle('select2-container--disabled', disabled);
                const selection = container.querySelector('.select2-selection');
                if (selection) {
                    selection.setAttribute('aria-disabled', disabled ? 'true' : 'false');
                    selection.setAttribute('tabindex', disabled ? '-1' : '0');
                }
            }
        }

        function setFrequencyEnabled(enabled) {
            frequencySelect.disabled = !enabled;
            if (frequencyField) {
                frequencyField.classList.toggle('is-disabled', !enabled);
            }
            refreshSelect2DisabledState(frequencySelect);
        }

        function hasDuration() {
            const years = parseInt(durationInput.value || '0', 10);
            return years >= 1 && years <= 100;
        }

        function syncFrequencyAvailability() {
            if (!hasDuration()) {
                frequencySelect.value = '';
                setFrequencyEnabled(false);
                list.innerHTML = '';
                summary.textContent = 'Select a duration to configure the course periods.';
                summary.classList.remove('text-danger-600');
                updateCoursePeriodPayload();
                setPeriodSubmitBlocked(false);
                return false;
            }
            setFrequencyEnabled(true);
            if (!frequencySelect.value) {
                list.innerHTML = '';
                summary.textContent = 'Select a frequency to configure the course periods.';
                summary.classList.remove('text-danger-600');
                updateCoursePeriodPayload();
                setPeriodSubmitBlocked(false);
                return false;
            }
            return true;
        }

        function regeneratePeriods() {
            if (!syncFrequencyAvailability()) {
                return;
            }
            const years = parseInt(durationInput.value || '0', 10);
            const frequency = frequencies[frequencySelect.value];
            if (!years || years < 1) {
                list.innerHTML = '';
                summary.textContent = 'Set a duration to configure the course periods.';
                return;
            }
            if (!frequency) {
                list.innerHTML = '';
                summary.textContent = 'Select a frequency to configure the course periods.';
                return;
            }
            const rows = [];
            for (let year = 1; year <= years; year++) {
                frequency.terms.forEach(function (term, index) {
                    const range = defaultRange(frequency, index);
                    rows.push(rowHtml(year, term[0], term[1], range.start, range.end));
                });
            }
            list.innerHTML = rows.join('');
            bindValidation();
            validatePeriodRows();
        }

        function parseDayMonth(value) {
            if (!/^\d{2}-\d{2}$/.test(value || '')) {
                return null;
            }
            const parts = value.split('-').map(Number);
            const date = new Date(2001, parts[1] - 1, parts[0]);
            if (date.getFullYear() !== 2001 || date.getMonth() !== parts[1] - 1 || date.getDate() !== parts[0]) {
                return null;
            }
            return date;
        }

        function calendarDay(date) {
            return Math.floor((date.getTime() - new Date(2001, 0, 1).getTime()) / 86400000);
        }

        function resetPeriodCardLayout(row) {
            row.style.removeProperty('grid-column');
            row.style.removeProperty('grid-row');
            row.style.removeProperty('--gape-period-card-offset');
            row.removeAttribute('data-period-card-density');
            row.removeAttribute('data-period-card-cluster');
        }

        function layoutPeriodCards() {
            const entries = [];
            coursePeriodRows().forEach(function (row) {
                resetPeriodCardLayout(row);
                const start = parseDayMonth(row.querySelector('[data-period-start]').value.trim());
                const end = parseDayMonth(row.querySelector('[data-period-end]').value.trim());
                if (!start || !end) {
                    return;
                }
                const startDay = calendarDay(start);
                let endDay = calendarDay(end);
                if (endDay < startDay) {
                    endDay += 365;
                }
                entries.push({row: row, startDay: startDay, endDay: endDay});
            });

            entries.sort(function (left, right) {
                return left.startDay - right.startDay || left.endDay - right.endDay;
            });

            const clusters = [];
            entries.forEach(function (entry) {
                const current = clusters[clusters.length - 1];
                if (!current || entry.startDay > current.endDay) {
                    clusters.push({startDay: entry.startDay, endDay: entry.endDay, entries: [entry]});
                    return;
                }
                current.entries.push(entry);
                current.endDay = Math.max(current.endDay, entry.endDay);
            });

            const maxParallelCards = clusters.reduce(function (maximum, cluster) {
                return Math.max(maximum, cluster.entries.length);
            }, 0);
            const minCardWidth = 250;
            const listMinWidth = maxParallelCards >= 6
                    ? (maxParallelCards * minCardWidth) + ((maxParallelCards - 1) * 10)
                    : 0;
            list.style.setProperty('--gape-period-list-min-width', listMinWidth + 'px');

            const columns = 60;
            clusters.forEach(function (cluster, clusterIndex) {
                const cards = cluster.entries;
                const cardCount = cards.length;
                const span = Math.max(1, Math.floor(columns / cardCount));
                const sameRange = cards.every(function (card) {
                    return card.startDay === cards[0].startDay && card.endDay === cards[0].endDay;
                });
                const clusterDuration = Math.max(1, cluster.endDay - cluster.startDay);
                cards.forEach(function (card, cardIndex) {
                    const columnStart = (cardIndex * span) + 1;
                    const columnEnd = cardIndex === cardCount - 1
                            ? columns + 1
                            : Math.min(columns + 1, columnStart + span);
                    const relativeStart = (card.startDay - cluster.startDay) / clusterDuration;
                    const offset = sameRange ? 0 : Math.round((relativeStart * 18) - 9);
                    card.row.style.gridColumn = columnStart + ' / ' + columnEnd;
                    card.row.style.gridRow = String(clusterIndex + 1);
                    card.row.style.setProperty('--gape-period-card-offset', offset + 'px');
                    card.row.dataset.periodCardCluster = String(clusterIndex + 1);
                    card.row.dataset.periodCardDensity = cardCount >= 4 ? 'narrow' : (cardCount >= 3 ? 'compact' : 'wide');
                });
            });
        }

        function validatePeriodRows() {
            const rows = coursePeriodRows();
            const invalidInputs = new Set();
            rows.forEach(function (row) {
                const startInput = row.querySelector('[data-period-start]');
                const endInput = row.querySelector('[data-period-end]');
                row.classList.remove('is-invalid');
                clearPeriodValidity(startInput);
                clearPeriodValidity(endInput);
            });

            rows.forEach(function (row) {
                const startInput = row.querySelector('[data-period-start]');
                const endInput = row.querySelector('[data-period-end]');
                const startValue = startInput.value.trim();
                const endValue = endInput.value.trim();
                const start = parseDayMonth(startValue);
                const end = parseDayMonth(endValue);
                if (!startValue) {
                    markPeriodInvalid(startInput, 'Start date is required.', invalidInputs);
                } else if (!start) {
                    markPeriodInvalid(startInput, 'Use a valid DD-MM start date.', invalidInputs);
                }
                if (!endValue) {
                    markPeriodInvalid(endInput, 'End date is required.', invalidInputs);
                } else if (!end) {
                    markPeriodInvalid(endInput, 'Use a valid DD-MM end date.', invalidInputs);
                }
            });

            rows.forEach(function (row) {
                row.classList.toggle('is-invalid', !!row.querySelector('input.is-invalid'));
            });
            layoutPeriodCards();
            updateCoursePeriodPayload();
            setPeriodSubmitBlocked(invalidInputs.size > 0);
            const count = rows.length;
            if (invalidInputs.size > 0) {
                summary.textContent = invalidInputs.size === 1
                        ? 'Resolve 1 course period date issue.'
                        : 'Resolve ' + invalidInputs.size + ' course period date issues.';
                summary.classList.add('text-danger-600');
                return false;
            }
            summary.classList.remove('text-danger-600');
            summary.textContent = count === 1 ? '1 configured period.' : count + ' configured periods.';
            return true;
        }

        function bindValidation() {
            list.querySelectorAll('[data-period-start], [data-period-end]').forEach(function (input) {
                input.addEventListener('input', validatePeriodRows);
                input.addEventListener('change', validatePeriodRows);
            });
        }

        durationInput.addEventListener('input', regeneratePeriods);
        durationInput.addEventListener('change', regeneratePeriods);
        frequencySelect.addEventListener('change', regeneratePeriods);
        if (window.jQuery) {
            window.jQuery(durationInput)
                    .off('select2:select.gapeCoursePeriods select2:clear.gapeCoursePeriods change.gapeCoursePeriods')
                    .on('select2:select.gapeCoursePeriods select2:clear.gapeCoursePeriods change.gapeCoursePeriods', regeneratePeriods);
            window.jQuery(frequencySelect)
                    .off('select2:select.gapeCoursePeriods select2:clear.gapeCoursePeriods change.gapeCoursePeriods')
                    .on('select2:select.gapeCoursePeriods select2:clear.gapeCoursePeriods change.gapeCoursePeriods', regeneratePeriods);
        }
        if (form) {
            form.addEventListener('submit', function (event) {
                if (!validatePeriodRows()) {
                    event.preventDefault();
                    event.stopPropagation();
                    const firstInvalid = list.querySelector('input.is-invalid');
                    if (firstInvalid) {
                        firstInvalid.scrollIntoView({block: 'center', behavior: 'smooth'});
                        firstInvalid.focus({preventScroll: true});
                        if (typeof firstInvalid.reportValidity === 'function') {
                            firstInvalid.reportValidity();
                        }
                    }
                }
            });
        }
        bindValidation();
        if (list.querySelector('[data-course-period-row]')) {
            syncFrequencyAvailability();
            validatePeriodRows();
        } else if (durationInput.value && frequencySelect.value) {
            regeneratePeriods();
        } else {
            syncFrequencyAvailability();
        }
    })();
</script>
<script>
    (function () {
        const imageInput = document.getElementById('courseImageUpload');
        const imagePreview = document.getElementById('courseImagePreview');
        const cancelButton = document.getElementById('cancelCourseImage');
        if (!imageInput || !imagePreview || !cancelButton) {
            return;
        }

        function placeholderIcon() {
            return imagePreview.querySelector('[data-photo-placeholder-icon]');
        }

        function showPreviewImage(url) {
            imagePreview.style.backgroundImage = "url('" + url + "')";
            imagePreview.classList.add('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.add('d-none');
            }
        }

        function showPreviewPlaceholder() {
            imagePreview.style.backgroundImage = '';
            imagePreview.classList.remove('is-image');
            const icon = placeholderIcon();
            if (icon) {
                icon.classList.remove('d-none');
            }
        }

        function restoreCurrentImage() {
            const currentImage = imagePreview.dataset.currentImage;
            if (imagePreview.dataset.hasCurrentImage === 'true' && currentImage) {
                showPreviewImage(currentImage);
                return;
            }
            showPreviewPlaceholder();
        }

        imageInput.addEventListener('change', function () {
            const file = imageInput.files && imageInput.files[0];
            if (!file) {
                return;
            }
            const reader = new FileReader();
            reader.onload = function (event) {
                showPreviewImage(event.target.result);
            };
            reader.readAsDataURL(file);
        });

        cancelButton.addEventListener('click', function () {
            imageInput.value = '';
            restoreCurrentImage();
        });
    })();
</script>
</body>
</html>
