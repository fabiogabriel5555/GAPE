<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - ${creating ? 'Create Assessment' : 'Edit Assessment'}</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
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

                <c:set var="assessmentFormAction" value="${pageContext.request.contextPath}/learning/assessments"/>
                <c:set var="assessmentBackUrl" value="${pageContext.request.contextPath}/learning/assessments"/>
                <c:if test="${not creating}">
                    <c:set var="assessmentFormAction" value="${pageContext.request.contextPath}/learning/assessments/${form.id}"/>
                    <c:set var="assessmentBackUrl" value="${pageContext.request.contextPath}/learning/assessments/${form.id}"/>
                </c:if>

                <form action="${assessmentFormAction}" method="post" class="bg-white rounded-10 px-32 py-32 border border-neutral-30" data-assessment-form data-creating="${creating}">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-28">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Assessment' : 'Edit Assessment'}</h2>
                            <span class="text-14 text-neutral-500">Configure type, context, grading, availability and correction mode.</span>
                        </div>
                        <a href="${assessmentBackUrl}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-8">
                            <label for="title" class="fw-medium text-base text-neutral-800 mb-12">Title</label>
                            <input id="title" name="title" type="text" maxlength="160" required value="<c:out value='${form.title}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-4">
                            <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                            <span class="d-inline-flex align-items-center gap-8 px-16 py-14 bg-neutral-20 border border-neutral-30 rounded-8 text-14 fw-semibold text-neutral-700" data-assessment-state-display>Draft</span>
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" maxlength="500" rows="3" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><c:out value="${form.description}"/></textarea>
                        </div>

                        <div class="col-lg-4">
                            <label for="mode" class="fw-medium text-base text-neutral-800 mb-12">Mode</label>
                            <select id="mode" name="mode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="online" ${form.mode == 'online' ? 'selected' : ''}>Live</option>
                                <option value="onsite" ${form.mode == 'onsite' ? 'selected' : ''}>In-Person</option>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="correctionMode" class="fw-medium text-base text-neutral-800 mb-12">Correction</label>
                            <select id="correctionMode" name="correctionMode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <option value="automatic" ${form.correctionMode == 'automatic' ? 'selected' : ''}>Automatic</option>
                                    <option value="mixed" ${form.correctionMode == 'mixed' ? 'selected' : ''}>Mixed</option>
                                    <option value="manual" ${form.correctionMode == 'manual' ? 'selected' : ''}>Manual</option>
                                </select>
                            <span class="text-12 text-neutral-500 mt-8 d-none" data-correction-mode-note>In-person assessments require manual correction.</span>
                            </div>

                            <div class="col-lg-4" data-subject-context>
                                <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                <select id="subjectId" name="subjectId" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <option value="">Select subject</option>
                                    <c:forEach var="subject" items="${subjectOptions}">
                                        <option value="${subject.value}" title="<c:out value='${subject.title}'/>" ${subject.selected ? 'selected' : ''}>
                                            <c:out value="${subject.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <span class="text-12 text-neutral-500 mt-8 d-block">Required for subject-level exams. Block assessments inherit the subject automatically.</span>
                            </div>
                            <div class="col-lg-4" data-block-context>
                                <label for="contentBlockId" class="fw-medium text-base text-neutral-800 mb-12">Pedagogical Block</label>
                                <select id="contentBlockId" name="contentBlockId" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <option value="">Select block</option>
                                    <c:forEach var="block" items="${contentBlockOptions}">
                                        <option value="${block.value}" title="<c:out value='${block.title}'/>" ${block.selected ? 'selected' : ''}>
                                            <c:out value="${block.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <span class="text-12 text-neutral-500 mt-8 d-block">Required for forms and tests. Optional for exams that should appear inside a class group block.</span>
                            </div>
                            <div class="col-lg-4" data-class-group-context>
                                <label for="classGroupIds" class="fw-medium text-base text-neutral-800 mb-12">Applicable Class Groups</label>
                                <select id="classGroupIds" name="classGroupIds" multiple size="4" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <c:forEach var="group" items="${classGroupOptions}">
                                        <option value="${group.value}" title="<c:out value='${group.title}'/>" ${group.selected ? 'selected' : ''}>
                                            <c:out value="${group.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <span class="text-12 text-neutral-500 mt-8 d-block">Required for exams without a pedagogical block.</span>
                            </div>

                        <div class="col-lg-3">
                            <label for="maxGrade" class="fw-medium text-base text-neutral-800 mb-12">Maximum Grade</label>
                            <input id="maxGrade" name="maxGrade" type="number" step="0.01" min="0.01" required value="<c:out value='${form.maxGrade}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="passingGrade" class="fw-medium text-base text-neutral-800 mb-12">Passing Grade</label>
                            <input id="passingGrade" name="passingGrade" type="number" step="0.01" min="0" required value="<c:out value='${form.passingGrade}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="attemptsLimit" class="fw-medium text-base text-neutral-800 mb-12">Attempts Limit</label>
                            <input id="attemptsLimit" name="attemptsLimit" type="number" min="1" value="<c:out value='${form.attemptsLimit}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Unlimited">
                        </div>
                        <div class="col-lg-3">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-assessment-type>
                                <option value="exam" ${form.type == 'exam' ? 'selected' : ''}>Exam</option>
                                <option value="test" ${form.type == 'test' ? 'selected' : ''}>Test</option>
                                <option value="form" ${form.type == 'form' ? 'selected' : ''}>Form</option>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="enrollmentMode" class="fw-medium text-base text-neutral-800 mb-12">Enrollment</label>
                            <select id="enrollmentMode" name="enrollmentMode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="auto_approve" ${form.enrollmentMode == 'auto_approve' ? 'selected' : ''}>Automatic</option>
                                <option value="manual" ${form.enrollmentMode == 'manual' ? 'selected' : ''}>Manual</option>
                            </select>
                        </div>
                        <c:if test="${creating}">
                            <div class="col-lg-3">
                                <label class="fw-medium text-base text-neutral-800 mb-12 d-block">Builder</label>
                                <span class="d-inline-flex align-items-center gap-8 px-16 py-13 bg-main-50 text-main-600 rounded-8 text-14 fw-semibold">
                                    <i class="ph ph-pencil-ruler"></i> Questions after save
                                </span>
                            </div>
                        </c:if>

                        <div class="col-lg-6">
                            <label for="availableFrom" class="fw-medium text-base text-neutral-800 mb-12">Available From</label>
                            <input id="availableFrom" name="availableFrom" type="datetime-local" value="<c:out value='${form.availableFrom}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-6">
                            <label for="availableUntil" class="fw-medium text-base text-neutral-800 mb-12">Available Until</label>
                            <input id="availableUntil" name="availableUntil" type="datetime-local" value="<c:out value='${form.availableUntil}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-14 flex-wrap border-top-dashed pt-24 mt-28">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">${creating ? 'Create Assessment' : 'Save Changes'}</button>
                        <a href="${assessmentBackUrl}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
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

        ready(function () {
            var form = document.querySelector('[data-assessment-form]');
            if (!form) {
                return;
            }
            var type = form.querySelector('[data-assessment-type]');
            var mode = form.querySelector('#mode');
            var correctionMode = form.querySelector('#correctionMode');
            var correctionModeNote = form.querySelector('[data-correction-mode-note]');
            var stateDisplay = form.querySelector('[data-assessment-state-display]');
            var creating = form.getAttribute('data-creating') === 'true';
            var subject = form.querySelector('#subjectId');
            var block = form.querySelector('#contentBlockId');
            var classGroups = form.querySelector('#classGroupIds');
            var availableFrom = form.querySelector('#availableFrom');
            var availableUntil = form.querySelector('#availableUntil');
            var subjectWrapper = form.querySelector('[data-subject-context]');
            var blockWrapper = form.querySelector('[data-block-context]');
            var classGroupWrapper = form.querySelector('[data-class-group-context]');

            function syncContext() {
                var isExam = type && type.value === 'exam';
                var hasSubject = subject && subject.value;
                var hasBlock = block && block.value;
                var hasClassGroups = classGroups && Array.prototype.some.call(classGroups.options, function (option) {
                    return option.selected;
                });
                if (subject) {
                    subject.required = isExam && !hasBlock;
                    subject.disabled = false;
                    subject.setCustomValidity('');
                    if (isExam && !hasSubject && !hasBlock) {
                        subject.setCustomValidity('Select a subject for exams without a pedagogical block.');
                    }
                }
                if (block) {
                    block.required = !isExam;
                    block.disabled = false;
                    block.setCustomValidity('');
                }
                if (classGroups) {
                    classGroups.required = isExam && !hasBlock;
                    classGroups.disabled = !isExam || hasBlock;
                    classGroups.setCustomValidity('');
                    if (isExam && !hasBlock && !hasClassGroups) {
                        classGroups.setCustomValidity('Select at least one applicable class group.');
                    }
                }
                if (subjectWrapper) {
                    subjectWrapper.classList.toggle('opacity-75', !isExam && hasBlock);
                }
                if (blockWrapper) {
                    blockWrapper.classList.toggle('opacity-75', isExam && hasSubject && !hasBlock);
                }
                if (classGroupWrapper) {
                    classGroupWrapper.classList.toggle('d-none', !isExam || hasBlock);
                }
            }

            function syncAvailability() {
                if (!availableFrom || !availableUntil) {
                    return;
                }
                var now = localMinuteValue();
                availableFrom.min = creating ? now : '';
                availableUntil.min = creating ? (availableFrom.value || now) : (availableFrom.value || '');
                availableFrom.setCustomValidity('');
                availableUntil.setCustomValidity('');
                if (availableUntil.value && !availableFrom.value) {
                    availableFrom.setCustomValidity('Availability start is required when an end is set.');
                }
                if (creating && availableFrom.value && availableFrom.value < now) {
                    availableFrom.setCustomValidity('Availability start cannot be in the past.');
                }
                if (creating && availableUntil.value && availableUntil.value < now) {
                    availableUntil.setCustomValidity('Availability end cannot be in the past.');
                }
                if (availableFrom.value && availableUntil.value && availableUntil.value < availableFrom.value) {
                    availableUntil.setCustomValidity('Availability end cannot be before start.');
                }
                if (stateDisplay) {
                    stateDisplay.textContent = computedStateLabel();
                }
            }

            function syncCorrectionMode() {
                if (!mode || !correctionMode) {
                    return;
                }
                var isInPerson = mode.value === 'onsite';
                if (isInPerson) {
                    correctionMode.value = 'manual';
                }
                correctionMode.disabled = isInPerson;
                if (correctionModeNote) {
                    correctionModeNote.classList.toggle('d-none', !isInPerson);
                }
            }

            function localMinuteValue() {
                var now = new Date();
                now.setSeconds(0, 0);
                var month = String(now.getMonth() + 1).padStart(2, '0');
                var day = String(now.getDate()).padStart(2, '0');
                var hours = String(now.getHours()).padStart(2, '0');
                var minutes = String(now.getMinutes()).padStart(2, '0');
                return now.getFullYear() + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
            }

            function computedStateLabel() {
                var now = localMinuteValue();
                if (!availableFrom.value) {
                    return 'Draft';
                }
                if (availableUntil.value && availableUntil.value <= now) {
                    return 'Completed';
                }
                return availableFrom.value <= now ? 'Active' : 'Scheduled';
            }

            if (type) {
                type.addEventListener('change', syncContext);
            }
            if (mode) {
                mode.addEventListener('change', syncCorrectionMode);
            }
            if (subject) {
                subject.addEventListener('change', syncContext);
            }
            if (block) {
                block.addEventListener('change', syncContext);
            }
            if (classGroups) {
                classGroups.addEventListener('change', syncContext);
            }
            if (availableFrom) {
                availableFrom.addEventListener('input', syncAvailability);
            }
            if (availableUntil) {
                availableUntil.addEventListener('input', syncAvailability);
            }
            syncContext();
            syncCorrectionMode();
            syncAvailability();
            window.setInterval(syncAvailability, 30000);
        });
    })();
</script>
</body>
</html>
