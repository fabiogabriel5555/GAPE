<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Attendance</title>
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

                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Records</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${attendanceCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Absences</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${absenceCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Late/Partial</span>
                            <h2 class="text-32 fw-semibold text-warning-600 mb-0">${lateOrPartialCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Pending</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${pendingJustificationCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Attendance</h2>
                            <span class="text-14 text-neutral-500">Presence, absence and permanence records by lesson.</span>
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

                    <div class="overflow-x-auto">
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
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="mb-20">
                        <h2 class="text-18 fw-medium text-neutral-700 mb-4">Justifications</h2>
                        <span class="text-14 text-neutral-500">Submitted absence, late and partial-presence justifications.</span>
                    </div>
                    <div class="overflow-x-auto">
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
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<script>
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
