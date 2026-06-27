<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Lesson Form</title>
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

                <form action="${formAction}" method="post" class="bg-white rounded-10 px-32 py-32 border border-neutral-30" data-lesson-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <c:if test="${not empty formReturnTo}">
                        <input type="hidden" name="returnTo" value="<c:out value='${formReturnTo}'/>">
                    </c:if>
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-28">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Lesson' : 'Edit Lesson'}</h2>
                            <span class="text-14 text-neutral-500">Configure schedule, delivery mode and access data.</span>
                        </div>
                        <a href="${fn:escapeXml(lessonBackHref)}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-6">
                            <label for="classGroupId" class="fw-medium text-base text-neutral-800 mb-12">Class Group</label>
                            <select id="classGroupId" name="classGroupId" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-class-group>
                                <option value="">Select class group</option>
                                <c:forEach var="classGroup" items="${classGroupOptions}">
                                    <option value="${classGroup.id}"
                                            data-organization-id="${classGroup.course.organizationId}"
                                            data-class-group-group="<c:out value='${classGroup.contextGroupLabel}'/>"
                                            data-class-group-group-title="<c:out value='${classGroup.contextGroupTitle}'/>"
                                            data-class-group-context-label="<c:out value='${classGroup.contextGroupLabel}'/>"
                                            data-class-group-context-title="<c:out value='${classGroup.contextGroupTitle}'/>"
                                            title="<c:out value='${classGroup.contextTitle}'/>"
                                            ${form.classGroupId == classGroup.id ? 'selected' : ''}>
                                        <c:out value="${classGroup.code}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="bg-neutral-20 border border-neutral-30 rounded-8 px-14 py-10 mt-10 d-none" data-lesson-class-group-context>
                                <span class="text-12 text-neutral-500 d-block mb-4">Context</span>
                                <span class="text-13 text-neutral-700" data-lesson-class-group-context-text></span>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <label for="contentBlockId" class="fw-medium text-base text-neutral-800 mb-12">Pedagogical Block</label>
                            <select id="contentBlockId" name="contentBlockId" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-block>
                                <option value="">Select block</option>
                                <c:forEach var="entry" items="${contentBlocksByClassGroup}">
                                    <c:forEach var="block" items="${entry.value}">
                                        <option value="${block.id}" data-class-group-id="${entry.key}" ${form.contentBlockId == block.id ? 'selected' : ''}>
                                            <c:out value="${block.orderNo}"/> - <c:out value="${block.name}"/>
                                        </option>
                                    </c:forEach>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <label for="title" class="fw-medium text-base text-neutral-800 mb-12">Title</label>
                            <input id="title" name="title" type="text" maxlength="160" required value="<c:out value='${form.title}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" maxlength="500" rows="3" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><c:out value="${form.description}"/></textarea>
                        </div>
                        <div class="col-lg-3">
                            <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                            <span class="d-inline-flex align-items-center gap-8 px-16 py-14 bg-neutral-20 border border-neutral-30 rounded-8 text-14 fw-semibold text-neutral-700" data-lesson-state-display>Draft</span>
                        </div>
                        <div class="col-lg-3">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-type>
                                <c:forEach var="type" items="${lessonTypes}">
                                    <option value="${type.value}" ${type.selected ? 'selected' : ''}><c:out value="${type.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="startsAt" class="fw-medium text-base text-neutral-800 mb-12">Starts At</label>
                            <input id="startsAt" name="startsAt" type="datetime-local" value="<c:out value='${form.startsAt}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="endsAt" class="fw-medium text-base text-neutral-800 mb-12">Ends At</label>
                            <input id="endsAt" name="endsAt" type="datetime-local" value="<c:out value='${form.endsAt}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-4" data-room-field>
                            <label for="physicalRoomCode" class="fw-medium text-base text-neutral-800 mb-12">Physical Room</label>
                            <select id="physicalRoomCode" name="physicalRoomCode" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-room>
                                <option value="">No room</option>
                                <c:forEach var="room" items="${physicalRoomOptions}">
                                    <option value="<c:out value='${room.code}'/>" data-organization-id="${room.organizationId}" data-room-group="<c:out value='${room.contextLabel}'/>" title="<c:out value='${room.contextTitle}'/>" ${form.physicalRoomCode == room.code ? 'selected' : ''}>
                                        <c:out value="${room.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-4" data-meeting-field>
                            <label for="provider" class="fw-medium text-base text-neutral-800 mb-12">Provider</label>
                            <select id="provider" name="provider" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-provider>
                                <option value="">Select provider</option>
                                <c:forEach var="provider" items="${providerOptions}">
                                    <option value="${provider.value}" ${provider.selected ? 'selected' : ''}><c:out value="${provider.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-4" data-meeting-field>
                            <label for="accessUrl" class="fw-medium text-base text-neutral-800 mb-12">Meeting Link</label>
                            <input id="accessUrl" name="accessUrl" type="url" maxlength="255" value="<c:out value='${form.accessUrl}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="https://...">
                        </div>
                        <div class="col-12">
                            <label class="d-inline-flex align-items-center gap-10 px-16 py-12 border border-neutral-30 rounded-8 bg-neutral-20 text-14 text-neutral-700">
                                <input type="checkbox" name="attendanceRequired" value="true" ${form.attendanceRequired ? 'checked' : ''}>
                                Attendance required
                            </label>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-14 flex-wrap border-top-dashed pt-24 mt-28">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">${creating ? 'Create Lesson' : 'Save Changes'}</button>
                        <a href="${fn:escapeXml(lessonBackHref)}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03">Cancel</a>
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
            var form = document.querySelector('[data-lesson-form]');
            if (!form) {
                return;
            }
            var classGroup = form.querySelector('[data-lesson-class-group]');
            var block = form.querySelector('[data-lesson-block]');
            var room = form.querySelector('[data-lesson-room]');
            var type = form.querySelector('[data-lesson-type]');
            var stateDisplay = form.querySelector('[data-lesson-state-display]');
            var provider = form.querySelector('[data-lesson-provider]');
            var accessUrl = form.querySelector('#accessUrl');
            var startsAt = form.querySelector('#startsAt');
            var endsAt = form.querySelector('#endsAt');
            var classGroupContext = form.querySelector('[data-lesson-class-group-context]');
            var classGroupContextText = form.querySelector('[data-lesson-class-group-context-text]');
            var originalClassGroups = classGroup ? Array.prototype.slice.call(classGroup.options).map(function (option) {
                return {
                    value: option.value,
                    text: option.textContent,
                    organizationId: option.dataset.organizationId || '',
                    groupLabel: option.dataset.classGroupGroup || '',
                    groupTitle: option.dataset.classGroupGroupTitle || '',
                    contextLabel: option.dataset.classGroupContextLabel || '',
                    contextTitle: option.dataset.classGroupContextTitle || '',
                    title: option.getAttribute('title') || '',
                    selected: option.selected
                };
            }) : [];
            var originalBlocks = block ? Array.prototype.slice.call(block.options).map(function (option) {
                return { value: option.value, text: option.textContent, classGroupId: option.dataset.classGroupId || '', selected: option.selected };
            }) : [];
            var originalRooms = room ? Array.prototype.slice.call(room.options).map(function (option) {
                return {
                    value: option.value,
                    text: option.textContent,
                    organizationId: option.dataset.organizationId || '',
                    groupLabel: option.dataset.roomGroup || '',
                    title: option.getAttribute('title') || '',
                    selected: option.selected
                };
            }) : [];

            function rebuildSelect(select, options, placeholder, predicate) {
                if (!select) {
                    return;
                }
                var previousValue = select.value;
                select.innerHTML = '';
                var first = document.createElement('option');
                first.value = '';
                first.textContent = placeholder;
                select.appendChild(first);
                var groups = {};
                options.filter(predicate).forEach(function (optionData) {
                    var parent = select;
                    if (optionData.groupLabel) {
                        parent = groups[optionData.groupLabel];
                        if (!parent) {
                            parent = document.createElement('optgroup');
                            parent.label = optionData.groupLabel;
                            if (optionData.groupTitle) {
                                parent.title = optionData.groupTitle;
                            }
                            groups[optionData.groupLabel] = parent;
                            select.appendChild(parent);
                        }
                    }
                    var option = document.createElement('option');
                    option.value = optionData.value;
                    option.textContent = optionData.text;
                    if (optionData.title) {
                        option.title = optionData.title;
                    }
                    if (optionData.classGroupId) {
                        option.dataset.classGroupId = optionData.classGroupId;
                    }
                    if (optionData.organizationId) {
                        option.dataset.organizationId = optionData.organizationId;
                    }
                    if (optionData.groupLabel) {
                        option.dataset.roomGroup = optionData.groupLabel;
                        option.dataset.classGroupGroup = optionData.groupLabel;
                    }
                    if (optionData.groupTitle) {
                        option.dataset.classGroupGroupTitle = optionData.groupTitle;
                    }
                    if (optionData.contextLabel) {
                        option.dataset.classGroupContextLabel = optionData.contextLabel;
                    }
                    if (optionData.contextTitle) {
                        option.dataset.classGroupContextTitle = optionData.contextTitle;
                    }
                    parent.appendChild(option);
                });
                select.value = previousValue;
                if (select.value !== previousValue) {
                    select.value = '';
                }
            }

            function selectedOrganizationId() {
                if (!classGroup || !classGroup.value) {
                    return '';
                }
                var option = classGroup.options[classGroup.selectedIndex];
                return option ? option.dataset.organizationId || '' : '';
            }

            function syncClassGroupContext() {
                if (!classGroupContext || !classGroupContextText || !classGroup) {
                    return;
                }
                var option = classGroup.options[classGroup.selectedIndex];
                var label = option ? option.dataset.classGroupContextLabel || '' : '';
                var title = option ? option.dataset.classGroupContextTitle || option.getAttribute('title') || '' : '';
                classGroupContext.classList.toggle('d-none', !label);
                classGroupContextText.textContent = label;
                if (title) {
                    classGroupContext.title = title;
                    classGroupContextText.title = title;
                } else {
                    classGroupContext.removeAttribute('title');
                    classGroupContextText.removeAttribute('title');
                }
            }

            function syncContextFields() {
                var selectedClassGroup = classGroup ? classGroup.value : '';
                var selectedOrganization = selectedOrganizationId();
                rebuildSelect(block, originalBlocks, 'Select block', function (optionData) {
                    return optionData.value && optionData.classGroupId === selectedClassGroup;
                });
                rebuildSelect(room, originalRooms, 'No room', function (optionData) {
                    return optionData.value && optionData.organizationId === selectedOrganization;
                });
                syncClassGroupContext();
                syncTypeFields();
            }

            function syncTypeFields() {
                var selectedType = type ? type.value : 'ONLINE';
                var onsite = selectedType === 'ONSITE';
                var online = selectedType === 'ONLINE';
                var hybrid = selectedType === 'HYBRID';
                form.querySelectorAll('[data-meeting-field]').forEach(function (wrapper) {
                    wrapper.classList.toggle('d-none', onsite);
                    Array.prototype.forEach.call(wrapper.querySelectorAll('input'), function (input) {
                        input.required = !onsite && input.name === 'accessUrl';
                        input.disabled = onsite;
                    });
                    Array.prototype.forEach.call(wrapper.querySelectorAll('select'), function (select) {
                        select.required = !onsite && select.name === 'provider';
                        select.disabled = onsite;
                    });
                });
                form.querySelectorAll('[data-room-field]').forEach(function (wrapper) {
                    wrapper.classList.toggle('d-none', online);
                    Array.prototype.forEach.call(wrapper.querySelectorAll('select'), function (select) {
                        select.required = onsite || hybrid;
                        select.disabled = online;
                    });
                });
                validateMeetingLink();
                validateSchedule();
            }

            function providerHost(providerValue) {
                switch ((providerValue || '').toLowerCase()) {
                    case 'zoom':
                        return 'zoom.us';
                    case 'teams':
                        return 'teams.microsoft.com';
                    case 'meet':
                        return 'meet.google.com';
                    default:
                        return '';
                }
            }

            function hostMatches(host, expectedHost) {
                return host === expectedHost || host.endsWith('.' + expectedHost);
            }

            function validateMeetingLink() {
                if (!provider || !accessUrl || (type && type.value === 'ONSITE')) {
                    return;
                }
                accessUrl.setCustomValidity('');
                var expectedHost = providerHost(provider.value);
                if (!accessUrl.value || !expectedHost) {
                    return;
                }
                try {
                    var parsed = new URL(accessUrl.value);
                    if (parsed.protocol !== 'https:' || !hostMatches(parsed.hostname.toLowerCase(), expectedHost)) {
                        accessUrl.setCustomValidity('The meeting link must match the selected provider.');
                    }
                } catch (ignored) {
                    accessUrl.setCustomValidity('The meeting link must be a valid HTTPS URL.');
                }
            }

            function validateSchedule() {
                if (!startsAt || !endsAt) {
                    return;
                }
                var now = localMinuteValue();
                startsAt.min = now;
                endsAt.required = false;
                endsAt.min = startsAt.value || now;
                startsAt.setCustomValidity('');
                endsAt.setCustomValidity('');
                if (endsAt.value && !startsAt.value) {
                    startsAt.setCustomValidity('Start date is required when an end date is set.');
                }
                if (startsAt.value && startsAt.value < now) {
                    startsAt.setCustomValidity('Start date cannot be in the past.');
                }
                if (endsAt.value && endsAt.value < now) {
                    endsAt.setCustomValidity('End date cannot be in the past.');
                }
                if (startsAt.value && endsAt.value && endsAt.value <= startsAt.value) {
                    endsAt.setCustomValidity('End date must be after start date.');
                }
                if (stateDisplay) {
                    stateDisplay.textContent = computedStateLabel();
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
                if (!startsAt.value) {
                    return 'Draft';
                }
                if (endsAt.value && endsAt.value <= now) {
                    return 'Completed';
                }
                return startsAt.value <= now ? 'Active' : 'Scheduled';
            }

            if (classGroup) {
                classGroup.addEventListener('change', syncContextFields);
                rebuildSelect(classGroup, originalClassGroups, 'Select class group', function (optionData) {
                    return optionData.value;
                });
            }
            if (type) {
                type.addEventListener('change', syncTypeFields);
            }
            if (provider) {
                provider.addEventListener('change', validateMeetingLink);
            }
            if (accessUrl) {
                accessUrl.addEventListener('input', validateMeetingLink);
            }
            if (startsAt) {
                startsAt.addEventListener('input', validateSchedule);
            }
            if (endsAt) {
                endsAt.addEventListener('input', validateSchedule);
            }
            syncContextFields();
            window.setInterval(validateSchedule, 30000);
        });
    })();
</script>
</body>
</html>
