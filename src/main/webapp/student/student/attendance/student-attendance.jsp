<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    request.setAttribute("activeMenu", "attendance");
    request.setAttribute("pageTitle", "Enrollments & Certificates");
    request.setAttribute("studentPageTitle", "Enrollments & Certificates");
    request.setAttribute("studentPageDescription", "Attendance records, absence notifications, grades and certificates.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Attendance</h6>
            <span class="text-14 text-neutral-500">Presence, absence and permanence records.</span>
        </div>
        <span class="gape-student-icon ${absenceNotificationCount > 0 ? 'gape-student-soft-amber' : 'gape-student-soft-main'} text-24">
            <i class="ph ph-user-check"></i>
        </span>
    </div>

    <c:if test="${absenceNotificationCount > 0}">
        <div class="bg-warning-50 text-warning-600 rounded-10 px-18 py-14 mb-18 d-flex align-items-center gap-10">
            <i class="ph ph-bell-ringing text-20"></i>
            <span class="text-14 fw-medium"><c:out value="${absenceNotificationCount}"/> record(s) require justification.</span>
        </div>
    </c:if>

    <div class="d-flex flex-column gap-14">
        <c:forEach var="record" items="${attendanceRecords}">
            <article class="gape-student-line-card px-18 py-18">
                <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                    <div class="min-w-0">
                        <h4 class="text-17 fw-semibold text-neutral-800 mb-0"><c:out value="${record.lessonTitle}"/></h4>
                        <div class="d-flex align-items-center gap-10 flex-wrap mt-8">
                            <span class="${record.statusBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                <c:out value="${record.statusLabel}"/>
                            </span>
                            <span class="text-13 text-neutral-500"><i class="ph ph-clock me-6"></i>In: <c:out value="${record.checkIn}"/></span>
                            <span class="text-13 text-neutral-500"><i class="ph ph-clock-counter-clockwise me-6"></i>Out: <c:out value="${record.checkOut}"/></span>
                            <span class="text-13 text-neutral-500"><i class="ph ph-timer me-6"></i><c:out value="${record.permanenceLabel}"/></span>
                            <span class="text-13 text-neutral-500"><i class="ph ph-note me-6"></i><c:out value="${record.notes}"/></span>
                        </div>
                    </div>
                    <span class="${record.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                        <c:out value="${record.stateLabel}"/>
                    </span>
                </div>
                <c:if test="${record.canSubmitJustification}">
                    <form action="${pageContext.request.contextPath}/student/attendance/justifications" method="post" enctype="multipart/form-data" class="row gy-3 mt-16">
                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                        <input type="hidden" name="attendanceRecordId" value="${record.id}">
                        <div class="col-lg-7">
                            <label class="text-13 text-neutral-600 mb-6 d-block" for="reason-${record.id}">Reason</label>
                            <input type="text" id="reason-${record.id}" name="reason" maxlength="300" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                        </div>
                        <div class="col-lg-3">
                            <label class="text-13 text-neutral-600 mb-6 d-block" for="attachment-${record.id}">Attachment</label>
                            <input type="file" id="attachment-${record.id}" name="attachmentFile" accept=".pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.txt" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-2 d-flex align-items-end">
                            <button type="submit" class="bg-main-600 px-18 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center w-100 justify-content-center" style="min-height: 44px;">
                                <i class="ph ph-paper-plane-tilt me-8"></i>Submit
                            </button>
                        </div>
                    </form>
                </c:if>
            </article>
        </c:forEach>
        <c:if test="${empty attendanceRecords}">
            <div class="gape-student-empty text-center px-24 py-40">
                <span class="gape-student-icon gape-student-soft-main text-28 mb-16"><i class="ph ph-user-check"></i></span>
                <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No attendance records</h4>
                <p class="text-14 text-neutral-500 mb-0">Attendance records will appear after lessons are processed.</p>
            </div>
        </c:if>
    </div>
</section>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Justifications</h6>
            <span class="text-14 text-neutral-500">Submitted decisions and processing state.</span>
        </div>
        <span class="gape-student-icon gape-student-soft-main text-24">
            <i class="ph ph-file-text"></i>
        </span>
    </div>
    <div class="overflow-x-auto">
        <table class="table mb-0">
            <thead>
            <tr>
                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Lesson</th>
                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Reason</th>
                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Submitted</th>
                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Processed</th>
                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="justification" items="${justifications}">
                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                    <td class="py-20 px-20 text-14 text-neutral-700">
                        <c:out value="${justification.lessonTitle}"/>
                        <span class="d-block text-12 text-neutral-500">Record #<c:out value="${justification.attendanceRecordId}"/></span>
                    </td>
                    <td class="py-20 px-20 text-14 text-neutral-500">
                        <c:out value="${justification.reason}"/>
                        <span class="d-block text-12">Attachment: <c:out value="${justification.attachmentLabel}"/></span>
                    </td>
                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${justification.submittedAt}"/></td>
                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${justification.processedAt}"/></td>
                    <td class="py-20 px-20">
                        <span class="${justification.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                            <c:out value="${justification.stateLabel}"/>
                        </span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty justifications}">
                <tr>
                    <td colspan="5" class="py-32 px-20 text-center text-14 text-neutral-500">No justifications submitted.</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-grades-certificates-content.jspf" %>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
