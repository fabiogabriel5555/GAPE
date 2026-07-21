<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    request.setAttribute("activeMenu", "dashboard");
    request.setAttribute("pageTitle", "Dashboard");
    request.setAttribute("studentPageTitle", "Dashboard");
    request.setAttribute("studentPageDescription", "A student-first view for courses, class groups, messages and upcoming lessons.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="row gy-4" aria-label="My academic statistics">
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/courses" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Current courses</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.currentCourses}"/></strong></div><span class="w-44 h-44 bg-main-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item1.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">View courses</span></a></div>
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/class-groups" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Current class groups</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.currentClassGroups}"/></strong></div><span class="w-44 h-44 bg-success-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item3.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">View class groups</span></a></div>
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/lessons" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Upcoming lessons</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.upcomingLessons}"/></strong></div><span class="w-44 h-44 bg-warning-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item4.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">Open lessons</span></a></div>
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/lessons#assessments" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Available assessments</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.availableAssessments}"/></strong></div><span class="w-44 h-44 bg-neutral-900 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item2.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">Open assessments</span></a></div>
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/attendance" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Attendance rate</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.attendanceRate}"/>%</strong></div><span class="w-44 h-44 bg-main-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item5.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">View attendance</span></a></div>
    <div class="col-xl-4 col-sm-6"><a href="${pageContext.request.contextPath}/student/attendance" class="gape-student-dashboard-link-card px-20 py-20 bg-white rounded-10 d-block h-100"><div class="d-flex gap-16 justify-content-between mb-12"><div><span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Published grades</span><strong class="text-24 fw-semibold text-neutral-500"><c:out value="${studentDashboard.averageGrade}"/></strong></div><span class="w-44 h-44 bg-success-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item6.png" alt=""></span></div><span class="text-12 fw-medium text-main-600 text-decoration-underline transition-03">View academic record</span></a></div>

    <div class="col-xl-8">
        <section class="px-20 py-20 bg-white rounded-10 h-100" aria-labelledby="student-activity-heading">
            <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap"><h2 id="student-activity-heading" class="text-16 fw-medium text-neutral-500 mb-0">Academic activity</h2><span class="text-12 text-neutral-400">Last six months</span></div>
            <span class="mt-20 mb-12 border-bottom-solid d-inline-block w-100"></span>
            <div id="student-activity-chart" class="gape-student-dashboard-chart" role="img" aria-label="Monthly lessons and assessment attempts" data-labels="<c:out value='${studentDashboard.activityLabels}'/>" data-lessons="<c:out value='${studentDashboard.lessonActivity}'/>" data-assessments="<c:out value='${studentDashboard.assessmentActivity}'/>"></div>
        </section>
    </div>
    <div class="col-xl-4">
        <section class="px-20 py-20 bg-white rounded-10 h-100" aria-labelledby="student-attendance-heading">
            <div class="d-flex align-items-center justify-content-between gap-16"><h2 id="student-attendance-heading" class="text-16 fw-medium text-neutral-500 mb-0">Attendance overview</h2><span class="text-12 text-main-600 fw-semibold"><c:out value="${studentDashboard.attendanceRate}"/>%</span></div>
            <span class="mt-20 mb-12 border-bottom-solid d-inline-block w-100"></span>
            <div id="student-attendance-chart" class="gape-student-dashboard-chart" role="img" aria-label="Attendance distribution" data-attended="<c:out value='${studentDashboard.attended}'/>" data-not-attended="<c:out value='${studentDashboard.notAttended}'/>"></div>
        </section>
    </div>
    <div class="col-xl-6"><section class="px-20 py-20 bg-white rounded-10 h-100"><div class="d-flex align-items-center justify-content-between mb-20"><h2 class="text-16 fw-medium text-neutral-500 mb-0">Academic record</h2><a href="${pageContext.request.contextPath}/student/attendance" class="text-12 fw-medium text-main-600 text-decoration-underline">View all</a></div><div class="row gy-3"><div class="col-sm-6"><div class="gape-student-line-card px-16 py-16"><span class="text-13 text-neutral-400 d-block mb-6">Issued certificates</span><strong class="text-20 text-neutral-500"><c:out value="${studentDashboard.issuedCertificates}"/></strong></div></div><div class="col-sm-6"><div class="gape-student-line-card px-16 py-16"><span class="text-13 text-neutral-400 d-block mb-6">Upcoming events</span><strong class="text-20 text-neutral-500"><c:out value="${studentDashboard.upcomingEvents}"/></strong></div></div></div></section></div>
    <div class="col-xl-6"><section class="px-20 py-20 bg-white rounded-10 h-100"><div class="d-flex align-items-center justify-content-between mb-20"><h2 class="text-16 fw-medium text-neutral-500 mb-0">Quick access</h2><i class="ph ph-lightning text-main-600 text-20" aria-hidden="true"></i></div><div class="d-flex flex-column gap-12"><a href="${pageContext.request.contextPath}/messages" class="text-14 text-neutral-500 hover-text-main-600">Messages</a><a href="${pageContext.request.contextPath}/student/events" class="text-14 text-neutral-500 hover-text-main-600">My events</a><a href="${pageContext.request.contextPath}/student/attendance" class="text-14 text-neutral-500 hover-text-main-600">Enrollments &amp; Certificates</a></div></section></div>
</section>

<script>
    window.addEventListener('load', function () {
        if (!window.ApexCharts) return;
        var activity = document.getElementById('student-activity-chart');
        var attendance = document.getElementById('student-attendance-chart');
        var csv = function (value) { return value ? value.split(',').map(Number) : []; };
        if (activity) {
            new ApexCharts(activity, { chart: { type: 'bar', height: 280, toolbar: { show: false } }, series: [{ name: 'Lessons', data: csv(activity.dataset.lessons) }, { name: 'Assessment attempts', data: csv(activity.dataset.assessments) }], colors: ['#1677d2', '#f59e0b'], plotOptions: { bar: { borderRadius: 4, columnWidth: '46%' } }, dataLabels: { enabled: false }, xaxis: { categories: activity.dataset.labels.split(','), labels: { style: { colors: '#667085' } } }, yaxis: { min: 0, forceNiceScale: true, labels: { style: { colors: '#667085' } } }, grid: { borderColor: '#e9eef3' }, legend: { position: 'top', horizontalAlign: 'right' } }).render();
        }
        if (attendance) {
            new ApexCharts(attendance, { chart: { type: 'donut', height: 280 }, series: [Number(attendance.dataset.attended), Number(attendance.dataset.notAttended)], labels: ['Attended', 'Not attended'], colors: ['#16a34a', '#ef4444'], legend: { position: 'bottom' }, dataLabels: { enabled: false }, plotOptions: { pie: { donut: { labels: { show: true, total: { show: true, label: 'Records' } } } } } }).render();
        }
    });
</script>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
