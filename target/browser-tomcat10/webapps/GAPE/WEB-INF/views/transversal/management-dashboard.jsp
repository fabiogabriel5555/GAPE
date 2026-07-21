<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GAPE - Dashboard</title>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/assets/images/logo/favicon.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/select2.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/slick.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/magnific-popup.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/jquery-ui.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/plyr.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/editor-quill.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/animate.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dataTables.dataTables.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/aos.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css?v=20260715-management-views">
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>
<div class="progress-wrap">
    <svg class="progress-circle svg-content" width="100%" height="100%" viewBox="-1 -1 102 102" aria-hidden="true">
        <path d="M50,1 a49,49 0 0,1 0,98 a49,49 0 0,1 0,-98"></path>
    </svg>
</div>

<div class="dashbord bg-main-25 w-100 overflow-hidden">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-w-0">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>

            <main class="px-24 pb-24 flex-grow-1 gape-management-dashboard" id="management-dashboard-main" aria-busy="false">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>
                <div class="gape-management-dashboard-feedback" data-management-async-feedback aria-live="polite" hidden></div>

                <c:url var="dashboardWorkspaceUrl" value="/dashboard">
                    <c:param name="tab" value="dashboard"/>
                    <c:if test="${not empty selectedManagementView}">
                        <c:param name="viewId" value="${selectedManagementView.id}"/>
                    </c:if>
                </c:url>
                <c:url var="dashboardLogsUrl" value="/dashboard">
                    <c:param name="tab" value="logs"/>
                </c:url>
                <div class="gape-management-dashboard-mode-grid mb-24" role="tablist" aria-label="Dashboard content">
                    <a href="${dashboardWorkspaceUrl}" role="tab" aria-selected="${dashboardTab == 'dashboard'}"
                       aria-label="Dashboard"
                       class="gape-management-dashboard-mode-card ${dashboardTab == 'dashboard' ? 'is-active' : ''}"
                       data-management-tab data-management-tab-card>
                        <span class="gape-management-dashboard-mode-icon bg-main-50 text-main-600" aria-hidden="true">
                            <i class="ph ph-squares-four"></i>
                        </span>
                        <span class="min-w-0">
                            <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Dashboard</span>
                            <span class="text-13 text-main-600 fw-semibold">Authorized panels and reports</span>
                        </span>
                    </a>
                    <c:if test="${managementCanViewLogs}">
                        <a href="${dashboardLogsUrl}" role="tab" aria-selected="${dashboardTab == 'logs'}"
                           aria-label="Logs"
                           class="gape-management-dashboard-mode-card ${dashboardTab == 'logs' ? 'is-active' : ''}"
                           data-management-tab data-management-tab-card>
                            <span class="gape-management-dashboard-mode-icon bg-info-50 text-info-600" aria-hidden="true">
                                <i class="ph ph-list-checks"></i>
                            </span>
                            <span class="min-w-0">
                                <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Logs</span>
                                <span class="text-13 text-main-600 fw-semibold">Authorized activity history</span>
                            </span>
                        </a>
                    </c:if>
                </div>

                <c:choose>
                    <c:when test="${dashboardTab == 'logs'}">
                        <section class="gape-management-card bg-white rounded-10 px-20 py-20" aria-labelledby="dashboard-logs-heading">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                                <div class="min-w-0">
                                    <div class="d-flex align-items-center gap-10 flex-wrap mb-4">
                                        <h2 class="text-18 fw-semibold text-neutral-500 mb-0" id="dashboard-logs-heading">Logs</h2>
                                        <span class="px-10 py-4 rounded-pill bg-main-50 text-main-600 text-12 fw-semibold"><c:out value="${managementProfileLabel}"/></span>
                                        <span class="px-10 py-4 rounded-pill bg-neutral-20 text-neutral-600 text-12 fw-semibold"><c:out value="${auditLogCount}"/> visible</span>
                                    </div>
                                    <p class="text-14 text-neutral-400 mb-0">Activity is shown only when the record belongs to your authorized organization, subject, class group or personal context.</p>
                                </div>
                            </div>

                            <c:if test="${not empty auditFilterError}">
                                <div class="alert alert-danger text-14 mb-16" role="alert"><c:out value="${auditFilterError}"/></div>
                            </c:if>

                            <c:url var="clearAuditLogsUrl" value="/dashboard"><c:param name="tab" value="logs"/></c:url>
                            <form method="get" action="${pageContext.request.contextPath}/dashboard" class="gape-management-dashboard-audit-filters rounded-8 px-16 py-16 mb-20" data-management-audit-form>
                                <input type="hidden" name="tab" value="logs">
                                <div class="row gy-3 align-items-end">
                                    <div class="col-12 col-xl-3 col-lg-4">
                                        <label for="auditUserId" class="form-label text-13 fw-medium text-neutral-500 mb-6">User ID</label>
                                        <input id="auditUserId" name="userId" type="number" min="1" step="1" inputmode="numeric" class="form-control text-14 bg-white" placeholder="Any visible user"
                                               value="<c:out value='${auditUserId}'/>" ${auditUserLocked ? 'readonly aria-readonly="true"' : ''} data-management-audit-filter>
                                        <c:if test="${auditUserLocked}">
                                            <span class="d-block text-12 text-neutral-400 mt-6">Your history is locked to your own user.</span>
                                        </c:if>
                                    </div>
                                    <div class="col-12 col-xl-3 col-lg-4">
                                        <label for="auditOperationType" class="form-label text-13 fw-medium text-neutral-500 mb-6">Operation</label>
                                        <input id="auditOperationType" name="operationType" type="search" maxlength="120" class="form-control text-14 bg-white" placeholder="For example: USER_UPDATE"
                                               value="<c:out value='${auditOperationType}'/>" data-management-audit-filter>
                                    </div>
                                    <div class="col-12 col-xl-2 col-lg-4">
                                        <label for="auditEntityType" class="form-label text-13 fw-medium text-neutral-500 mb-6">Entity</label>
                                        <input id="auditEntityType" name="entityType" type="search" maxlength="120" class="form-control text-14 bg-white" placeholder="For example: class_group"
                                               value="<c:out value='${auditEntityType}'/>" data-management-audit-filter>
                                    </div>
                                    <div class="col-6 col-xl-2 col-lg-3">
                                        <label for="auditOccurredFrom" class="form-label text-13 fw-medium text-neutral-500 mb-6">From</label>
                                        <input id="auditOccurredFrom" name="occurredFrom" type="date" class="form-control text-14 bg-white"
                                               value="<c:out value='${auditOccurredFrom}'/>" data-management-audit-filter>
                                    </div>
                                    <div class="col-6 col-xl-2 col-lg-3">
                                        <label for="auditOccurredUntil" class="form-label text-13 fw-medium text-neutral-500 mb-6">Until</label>
                                        <input id="auditOccurredUntil" name="occurredUntil" type="date" class="form-control text-14 bg-white"
                                               value="<c:out value='${auditOccurredUntil}'/>" data-management-audit-filter>
                                    </div>
                                    <div class="col-12 col-xl-3 col-lg-4">
                                        <label for="auditOutcome" class="form-label text-13 fw-medium text-neutral-500 mb-6">Outcome</label>
                                        <select id="auditOutcome" name="outcome" class="form-select text-14 bg-white" data-management-audit-filter>
                                            <option value="">All outcomes</option>
                                            <option value="success" ${auditOutcome == 'success' ? 'selected' : ''}>Success</option>
                                            <option value="failure" ${auditOutcome == 'failure' ? 'selected' : ''}>Failure</option>
                                            <option value="denied" ${auditOutcome == 'denied' ? 'selected' : ''}>Denied</option>
                                            <option value="unauthorized" ${auditOutcome == 'unauthorized' ? 'selected' : ''}>Unauthorized</option>
                                        </select>
                                    </div>
                                    <div class="col-12 col-xl-9 col-lg-8 d-flex justify-content-xl-end align-items-end gap-10 flex-wrap">
                                        <button type="submit" class="btn btn-main rounded-8 text-14 px-16 py-10" data-management-audit-submit>Apply filters</button>
                                        <a href="${clearAuditLogsUrl}" class="btn btn-outline-main rounded-8 text-14 px-16 py-10" data-management-tab data-management-audit-clear>Clear</a>
                                    </div>
                                </div>
                            </form>

                            <div class="table-responsive gape-management-dashboard-table-wrap">
                                <table class="table mb-0 gape-management-dashboard-audit-table">
                                    <thead>
                                    <tr>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Date</th>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Operation</th>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">User</th>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Entity</th>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Outcome</th>
                                        <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">IP</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${auditLogs}" var="log">
                                        <c:set var="auditOutcomeBadgeClass" value="bg-neutral-40 text-neutral-600"/>
                                        <c:if test="${log.outcome == 'success'}"><c:set var="auditOutcomeBadgeClass" value="bg-success-50 text-success-600"/></c:if>
                                        <c:if test="${log.outcome == 'failure' or log.outcome == 'denied' or log.outcome == 'unauthorized'}"><c:set var="auditOutcomeBadgeClass" value="bg-danger-50 text-danger-600"/></c:if>
                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-16 px-16 text-14 text-neutral-500 text-nowrap"><c:out value="${log.occurredAt}"/></td>
                                            <td class="py-16 px-16">
                                                <strong class="d-block text-14 text-neutral-600"><c:out value="${log.operationType}"/></strong>
                                                <span class="d-block text-12 text-neutral-400 mt-4">Session: <c:out value="${empty log.sessionId ? '-' : log.sessionId}"/></span>
                                            </td>
                                            <td class="py-16 px-16 text-14 text-neutral-500"><c:out value="${empty log.userId ? '-' : log.userId}"/></td>
                                            <td class="py-16 px-16">
                                                <strong class="d-block text-14 text-neutral-600"><c:out value="${log.affectedEntityType}"/></strong>
                                                <span class="d-block text-12 text-neutral-400 mt-4 text-break"><c:out value="${log.affectedEntityIdentifier}"/></span>
                                            </td>
                                            <td class="py-16 px-16"><span class="px-10 py-4 rounded-pill text-12 fw-semibold ${auditOutcomeBadgeClass}"><c:out value="${log.outcome}"/></span></td>
                                            <td class="py-16 px-16 text-14 text-neutral-500 text-nowrap"><c:out value="${empty log.sourceIp ? '-' : log.sourceIp}"/></td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty auditLogs}">
                                        <tr>
                                            <td colspan="6" class="py-32 px-16 text-center text-14 text-neutral-400">No activity records match the current authorized filters.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </c:when>
                    <c:otherwise>
                <section class="gape-management-card bg-white rounded-10 px-20 py-20 mb-24" aria-labelledby="management-dashboard-heading">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap gape-management-card__header">
                        <div class="min-w-0">
                            <div class="d-flex align-items-center gap-10 flex-wrap mb-4">
                                <h2 class="text-18 fw-semibold text-neutral-500 mb-0" id="management-dashboard-heading">Management overview</h2>
                                <span class="px-10 py-4 rounded-pill bg-main-50 text-main-600 text-12 fw-semibold"><c:out value="${managementProfileLabel}"/></span>
                            </div>
                            <p class="text-14 text-neutral-400 mb-0">
                                Panels and reports are limited to the scope currently assigned to your profile.
                            </p>
                        </div>
                    </div>

                    <form method="get" action="${pageContext.request.contextPath}/dashboard" class="row gy-3 align-items-end mt-12" data-management-panel-form>
                        <div class="col-xl-5 col-lg-6">
                            <label for="managementViewId" class="form-label text-13 fw-medium text-neutral-500 mb-6">Panel or report</label>
                            <select class="form-select text-14 bg-main-25 border-neutral-40" id="managementViewId" name="viewId" data-management-view-select>
                                <c:if test="${empty managementViews}">
                                    <option value="">No panel is available</option>
                                </c:if>
                                <c:forEach items="${managementViews}" var="managementView">
                                    <option value="${managementView.id}" ${not empty selectedManagementView and selectedManagementView.id == managementView.id ? 'selected' : ''}>
                                        <c:out value="${managementView.title}"/> — <c:out value="${managementView.scopeLabel}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-3 col-lg-3">
                            <label for="managementTypeFilter" class="form-label text-13 fw-medium text-neutral-500 mb-6">Filter catalogue</label>
                            <select class="form-select text-14 bg-main-25 border-neutral-40" id="managementTypeFilter" data-management-type-filter>
                                <option value="all" selected>All panels</option>
                                <option value="report">Reports</option>
                                <option value="dashboard">Dashboards</option>
                                <option value="control_panel">Control panels</option>
                            </select>
                        </div>
                        <div class="col-xl-4 col-lg-3 d-flex gap-10 flex-wrap">
                            <button type="submit" class="btn btn-main rounded-8 text-14 px-16 py-10">Open</button>
                        </div>
                    </form>
                </section>

                <c:choose>
                    <c:when test="${not empty selectedManagementView}">
                        <section class="gape-management-dashboard__selected mb-24" aria-labelledby="selected-panel-heading">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-16">
                                <div class="min-w-0">
                                    <div class="d-flex align-items-center gap-8 flex-wrap mb-6">
                                        <h2 id="selected-panel-heading" class="text-20 fw-semibold text-neutral-500 mb-0"><c:out value="${selectedManagementView.title}"/></h2>
                                        <span class="px-10 py-4 rounded-pill text-12 fw-semibold ${selectedManagementView.typeBadgeClass}"><c:out value="${selectedManagementView.typeLabel}"/></span>
                                        <span class="px-10 py-4 rounded-pill bg-neutral-20 text-neutral-600 text-12 fw-semibold"><c:out value="${selectedManagementView.scopeLabel}"/></span>
                                    </div>
                                    <p class="text-14 text-neutral-400 mb-0"><c:out value="${selectedManagementView.description}"/></p>
                                </div>
                                <div class="d-flex align-items-center gap-8 flex-wrap">
                                    <c:if test="${selectedManagementView.configurable}">
                                        <button type="button" class="btn btn-outline-main rounded-8 text-13 px-14 py-8" data-management-edit
                                                data-id="${selectedManagementView.id}"
                                                data-title="<c:out value='${selectedManagementView.title}'/>"
                                                data-description="<c:out value='${selectedManagementView.configurationDescription}'/>"
                                                data-type="${selectedManagementView.typeValue}"
                                                data-scope="${selectedManagementView.scopeValue}"
                                                data-scope-target-id="${selectedManagementView.scopeTargetId}"
                                                data-state="${selectedManagementView.stateValue}"
                                                data-bs-toggle="modal" data-bs-target="#managementViewModal">
                                            <i class="ph ph-gear-six me-4" aria-hidden="true"></i>Configure
                                        </button>
                                        <button type="button" class="btn btn-outline-main rounded-8 text-13 px-14 py-8" data-bs-toggle="modal" data-bs-target="#managementRecipientsModal">
                                            <i class="ph ph-users-three me-4" aria-hidden="true"></i>Recipients
                                        </button>
                                    </c:if>
                                </div>
                            </div>

                            <div class="row gy-4">
                                <div class="col-xl-3 col-sm-6">
                                    <div class="px-20 py-20 bg-white rounded-10 h-100 gape-management-dashboard-kpi">
                                        <div class="d-flex gap-16 justify-content-between mb-12">
                                            <div>
                                                <span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Courses</span>
                                                <h3 class="text-22 fw-semibold text-neutral-500 mb-0"><c:out value="${selectedManagementView.courseCount}"/></h3>
                                            </div>
                                            <span class="w-44 h-44 bg-main-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item1.png" alt=""></span>
                                        </div>
                                        <span class="text-12 fw-medium text-main-600">Current scope</span>
                                    </div>
                                </div>
                                <div class="col-xl-3 col-sm-6">
                                    <div class="px-20 py-20 bg-white rounded-10 h-100 gape-management-dashboard-kpi">
                                        <div class="d-flex gap-16 justify-content-between mb-12">
                                            <div>
                                                <span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Class groups</span>
                                                <h3 class="text-22 fw-semibold text-neutral-500 mb-0"><c:out value="${selectedManagementView.classGroupCount}"/></h3>
                                            </div>
                                            <span class="w-44 h-44 bg-success-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item2.png" alt=""></span>
                                        </div>
                                        <span class="text-12 fw-medium text-success-600">Academic delivery</span>
                                    </div>
                                </div>
                                <div class="col-xl-3 col-sm-6">
                                    <div class="px-20 py-20 bg-white rounded-10 h-100 gape-management-dashboard-kpi">
                                        <div class="d-flex gap-16 justify-content-between mb-12">
                                            <div>
                                                <span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Active enrollments</span>
                                                <h3 class="text-22 fw-semibold text-neutral-500 mb-0"><c:out value="${selectedManagementView.activeEnrollmentCount}"/></h3>
                                            </div>
                                            <span class="w-44 h-44 bg-warning-600 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item3.png" alt=""></span>
                                        </div>
                                        <span class="text-12 fw-medium text-warning-700">Live population</span>
                                    </div>
                                </div>
                                <div class="col-xl-3 col-sm-6">
                                    <div class="px-20 py-20 bg-white rounded-10 h-100 gape-management-dashboard-kpi">
                                        <div class="d-flex gap-16 justify-content-between mb-12">
                                            <div>
                                                <span class="fw-normal text-14 text-neutral-400 mb-4 d-block">Lessons</span>
                                                <h3 class="text-22 fw-semibold text-neutral-500 mb-0"><c:out value="${selectedManagementView.lessonCount}"/></h3>
                                            </div>
                                            <span class="w-44 h-44 bg-neutral-900 rounded-circle justify-content-center align-items-center d-flex"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item4.png" alt=""></span>
                                        </div>
                                        <span class="text-12 fw-medium text-neutral-500">Scheduled content</span>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <section class="row gy-4 mb-24" aria-label="Indicators and charts">
                            <div class="col-xl-8">
                                <div class="gape-management-card bg-white px-20 py-20 rounded-10 h-100">
                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-18">
                                        <div>
                                            <h3 class="text-16 fw-semibold text-neutral-500 mb-4">Overview information</h3>
                                            <span class="text-13 text-neutral-400">Current indicators for the selected scope.</span>
                                        </div>
                                        <span class="text-12 fw-medium text-neutral-400">Real-time scope snapshot</span>
                                    </div>
                                    <div id="management-overview-chart" class="gape-management-dashboard-chart" aria-label="Scope indicator overview chart"></div>
                                </div>
                            </div>
                            <div class="col-xl-4">
                                <div class="gape-management-card bg-white px-20 py-20 rounded-10 h-100">
                                    <div class="mb-12">
                                        <h3 class="text-16 fw-semibold text-neutral-500 mb-4">Assessment workflow</h3>
                                        <span class="text-13 text-neutral-400">Submitted and corrected attempts.</span>
                                    </div>
                                    <div id="management-distribution-chart" class="gape-management-dashboard-donut" aria-label="Assessment workflow distribution chart"></div>
                                    <div class="gape-management-dashboard-chart-legend">
                                        <span><i class="bg-main-600"></i>Assessments</span>
                                        <span><i class="bg-warning-600"></i>Submitted</span>
                                        <span><i class="bg-success-600"></i>Corrected</span>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <section class="row gy-4 mb-24" aria-label="Scope summary tables">
                            <div class="col-xl-7">
                                <div class="gape-management-card bg-white px-20 py-20 rounded-10 h-100">
                                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-16">
                                        <div>
                                            <h3 class="text-16 fw-semibold text-neutral-500 mb-4">Indicator summary</h3>
                                            <span class="text-13 text-neutral-400">Detailed values used by this panel.</span>
                                        </div>
                                        <span class="px-10 py-4 rounded-pill bg-main-25 text-main-600 text-12 fw-semibold"><c:out value="${selectedManagementView.scopeLabel}"/> scope</span>
                                    </div>
                                    <div class="table-responsive gape-management-dashboard-table-wrap">
                                        <table class="table mb-0">
                                            <thead>
                                            <tr>
                                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Indicator</th>
                                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16 text-end">Value</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Subjects</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.subjectCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Assessments</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.assessmentCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Submitted attempts</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.submittedAttemptCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Corrected attempts</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.correctedAttemptCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Pending correction</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.pendingCorrectionCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Attendance records</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.attendanceRecordCount}"/></td></tr>
                                            <tr><td class="py-14 px-16 text-14 text-neutral-500">Issued certificates</td><td class="py-14 px-16 text-14 fw-semibold text-neutral-600 text-end"><c:out value="${selectedManagementView.issuedCertificateCount}"/></td></tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                            <div class="col-xl-5">
                                <div class="gape-management-card bg-white px-20 py-20 rounded-10 h-100">
                                    <h3 class="text-16 fw-semibold text-neutral-500 mb-8">Access and visibility</h3>
                                    <p class="text-13 text-neutral-400 mb-16">This view is available because your active profile satisfies its scope rule.</p>
                                    <div class="gape-management-summary mb-16">
                                        <span><i class="ph ph-eye me-6 text-main-600" aria-hidden="true"></i><c:out value="${selectedManagementView.scopeLabel}"/></span>
                                        <span><i class="ph ph-layout me-6 text-success-600" aria-hidden="true"></i><c:out value="${selectedManagementView.typeLabel}"/></span>
                                        <span><i class="ph ph-shield-check me-6 text-warning-700" aria-hidden="true"></i>Authorized</span>
                                    </div>
                                    <c:choose>
                                        <c:when test="${selectedManagementView.configurable}">
                                            <div class="gape-management-dashboard-note gape-management-dashboard-note--success">
                                                <i class="ph ph-gear-six" aria-hidden="true"></i>
                                                <span>You can configure this panel and its explicit recipient list. Recipients still need their own scope permission.</span>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="gape-management-dashboard-note">
                                                <i class="ph ph-lock-key" aria-hidden="true"></i>
                                                <span>This panel is read-only for your current profile.</span>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <section class="gape-management-dashboard-empty bg-white rounded-10 px-24 py-40 mb-24 text-center" aria-live="polite">
                            <span class="gape-management-dashboard-empty__icon"><i class="ph ph-chart-line-up" aria-hidden="true"></i></span>
                            <h2 class="text-20 fw-semibold text-neutral-500 mt-16 mb-8">No panel is available in this scope</h2>
                            <p class="text-14 text-neutral-400 mb-0">When a panel or report is granted to your active academic context, it will appear here.</p>
                        </section>
                    </c:otherwise>
                </c:choose>

                <section class="gape-management-card bg-white rounded-10 px-20 py-20" aria-labelledby="management-catalogue-heading">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-16">
                        <div>
                            <h2 id="management-catalogue-heading" class="text-16 fw-semibold text-neutral-500 mb-4">Available dashboards and reports</h2>
                            <span class="text-13 text-neutral-400">Search and open a dashboard or a report that is already authorized for you.</span>
                        </div>
                        <div class="gape-management-dashboard-search-wrap">
                            <label class="visually-hidden" for="managementCatalogueSearch">Search dashboards and reports</label>
                            <i class="ph ph-magnifying-glass" aria-hidden="true"></i>
                            <input id="managementCatalogueSearch" class="form-control text-14" type="search" placeholder="Search catalogue" data-management-catalogue-search>
                        </div>
                    </div>
                    <div class="table-responsive gape-management-dashboard-table-wrap">
                        <table class="table mb-0" data-management-catalogue>
                            <thead>
                            <tr>
                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Panel</th>
                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Scope</th>
                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16">Type</th>
                                <th class="text-13 fw-semibold text-neutral-500 py-12 px-16 text-end">Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${managementViews}" var="managementView">
                                <c:url var="managementDashboardUrl" value="/dashboard"><c:param name="viewId" value="${managementView.id}"/></c:url>
                                <tr class="hover-bg-neutral-20 transition-03" data-management-panel-row data-management-type="${managementView.typeValue}" data-management-search="${fn:toLowerCase(managementView.title)} ${fn:toLowerCase(managementView.scopeLabel)} ${fn:toLowerCase(managementView.typeLabel)}">
                                    <td class="py-16 px-16">
                                        <strong class="d-block text-14 text-neutral-600"><c:out value="${managementView.title}"/></strong>
                                        <span class="d-block text-12 text-neutral-400 mt-4 text-line-1"><c:out value="${managementView.description}"/></span>
                                    </td>
                                    <td class="py-16 px-16 text-14 text-neutral-500"><c:out value="${managementView.scopeLabel}"/></td>
                                    <td class="py-16 px-16"><span class="px-10 py-4 rounded-pill text-12 fw-semibold ${managementView.typeBadgeClass}"><c:out value="${managementView.typeLabel}"/></span></td>
                                    <td class="py-16 px-16 text-end">
                                        <a href="${managementDashboardUrl}" class="btn btn-outline-main rounded-8 text-13 px-12 py-7" data-management-open-view>Open</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <tr data-management-no-results hidden>
                                <td colspan="4" class="py-24 px-16 text-center text-14 text-neutral-400">No matching panel is available.</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                    </c:otherwise>
                </c:choose>
            </main>

            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${managementCanConfigure}">
    <div class="modal fade" id="managementViewModal" tabindex="-1" aria-labelledby="managementViewModalTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 rounded-12 overflow-hidden">
                <form method="post" action="${pageContext.request.contextPath}/dashboard" data-management-view-form data-management-async-form>
                    <input type="hidden" name="csrfToken" value="<c:out value='${managementDashboardCsrfToken}'/>">
                    <input type="hidden" name="operation" value="update" data-management-operation>
                    <input type="hidden" name="managementViewId" value="" data-management-view-id>
                    <div class="modal-header border-bottom border-neutral-30 px-24 py-18">
                        <div>
                            <h2 class="modal-title text-18 fw-semibold text-neutral-600 mb-4" id="managementViewModalTitle" data-management-modal-title>Configure panel</h2>
                            <p class="text-13 text-neutral-400 mb-0">Scope rules are revalidated by the server when you save.</p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body px-24 py-20">
                        <div class="row gy-16">
                            <div class="col-md-7">
                                <label for="managementViewTitle" class="form-label text-13 fw-medium text-neutral-500">Title</label>
                                <input id="managementViewTitle" name="title" type="text" maxlength="160" class="form-control" required data-management-title>
                            </div>
                            <div class="col-md-5">
                                <label for="managementViewType" class="form-label text-13 fw-medium text-neutral-500">Type</label>
                                <select id="managementViewType" name="type" class="form-select" required data-management-type>
                                    <option value="dashboard">Dashboard</option>
                                    <option value="report">Report</option>
                                    <option value="control_panel">Control panel</option>
                                    <option value="other">Other</option>
                                </select>
                            </div>
                            <div class="col-12">
                                <label for="managementViewDescription" class="form-label text-13 fw-medium text-neutral-500">Description <span class="text-neutral-400">(optional)</span></label>
                                <textarea id="managementViewDescription" name="description" class="form-control" rows="3" maxlength="500" data-management-description></textarea>
                            </div>
                            <div class="col-md-6">
                                <label for="managementViewScope" class="form-label text-13 fw-medium text-neutral-500">Visibility scope</label>
                                <select id="managementViewScope" name="visibilityScope" class="form-select" required data-management-scope>
                                    <c:forEach items="${managementScopeChoices}" var="scopeChoice">
                                        <option value="${scopeChoice.value}" data-management-target-required="${scopeChoice.targetRequired}"><c:out value="${scopeChoice.label}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-6" data-management-target-field>
                                <label for="managementViewScopeTarget" class="form-label text-13 fw-medium text-neutral-500">Scope target</label>
                                <select id="managementViewScopeTarget" name="scopeContextId" class="form-select" data-management-scope-target>
                                    <option value="">No target required</option>
                                    <c:forEach items="${managementScopeTargetOptions}" var="scopeTargetOption">
                                        <option value="${scopeTargetOption.targetId}" data-management-scope-target-option="${scopeTargetOption.scopeValue}" hidden>
                                            <c:out value="${scopeTargetOption.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <span class="d-block text-12 text-neutral-400 mt-6" data-management-target-hint></span>
                            </div>
                            <div class="col-md-6">
                                <label for="managementViewState" class="form-label text-13 fw-medium text-neutral-500">State</label>
                                <select id="managementViewState" name="state" class="form-select" required data-management-state>
                                    <option value="active">Active</option>
                                    <option value="inactive">Inactive</option>
                                    <option value="archived">Archived</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer border-top border-neutral-30 px-24 py-16">
                        <button type="button" class="btn btn-outline-main rounded-8 text-14 px-16 py-9" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-main rounded-8 text-14 px-16 py-9" data-management-save-label data-management-submit>Save changes</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<c:if test="${not empty selectedManagementView and selectedManagementView.configurable}">
    <div class="modal fade" id="managementRecipientsModal" tabindex="-1" aria-labelledby="managementRecipientsModalTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 rounded-12 overflow-hidden">
                <form method="post" action="${pageContext.request.contextPath}/dashboard" data-management-recipients-form data-management-async-form>
                    <input type="hidden" name="csrfToken" value="<c:out value='${managementDashboardCsrfToken}'/>">
                    <input type="hidden" name="operation" value="recipients">
                    <input type="hidden" name="managementViewId" value="${selectedManagementView.id}">
                    <div class="modal-header border-bottom border-neutral-30 px-24 py-18">
                        <div>
                            <h2 class="modal-title text-18 fw-semibold text-neutral-600 mb-4" id="managementRecipientsModalTitle">Explicit recipients</h2>
                            <p class="text-13 text-neutral-400 mb-0">Use user IDs separated by commas, spaces or new lines.</p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body px-24 py-20">
                        <label for="managementRecipientUserIds" class="form-label text-13 fw-medium text-neutral-500">Recipient user IDs</label>
                        <textarea id="managementRecipientUserIds" name="recipientUserIds" class="form-control" rows="4" inputmode="numeric" placeholder="For example: 12, 28, 41"><c:out value="${managementSelectedRecipientCsv}"/></textarea>
                        <div class="gape-management-dashboard-note mt-14">
                            <i class="ph ph-info" aria-hidden="true"></i>
                            <span>An explicit recipient never bypasses the panel's organization, course, subject, class group or personal scope rule.</span>
                        </div>
                    </div>
                    <div class="modal-footer border-top border-neutral-30 px-24 py-16">
                        <button type="button" class="btn btn-outline-main rounded-8 text-14 px-16 py-9" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-main rounded-8 text-14 px-16 py-9" data-management-submit>Save recipients</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<c:if test="${not empty selectedManagementView}">
    <div id="management-dashboard-data"
         data-course-count="${selectedManagementView.courseCount}"
         data-subject-count="${selectedManagementView.subjectCount}"
         data-class-group-count="${selectedManagementView.classGroupCount}"
         data-enrollment-count="${selectedManagementView.activeEnrollmentCount}"
         data-lesson-count="${selectedManagementView.lessonCount}"
         data-assessment-count="${selectedManagementView.assessmentCount}"
         data-submitted-attempt-count="${selectedManagementView.submittedAttemptCount}"
         data-corrected-attempt-count="${selectedManagementView.correctedAttemptCount}"></div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script src="${pageContext.request.contextPath}/assets/js/gape-management-views-dashboard.js?v=20260716-realtime"></script>
</body>
</html>
