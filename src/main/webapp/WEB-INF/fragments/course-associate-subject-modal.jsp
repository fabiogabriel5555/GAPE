<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="modal fade" id="associateCourseSubjectModal" tabindex="-1" aria-labelledby="associateCourseSubjectModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold" id="associateCourseSubjectModalLabel">Associate Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/subjects" method="post" data-course-live-form data-course-live-panel="structure">
                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                <input type="hidden" name="returnTo" value="${currentReturnTo}#course-structure">
                <div class="modal-body">
                    <p class="text-14 text-neutral-600 mb-20">Select an existing subject and its curricular position in this course.</p>
                    <div class="mb-20 gape-select-field">
                        <label for="courseDetailSubjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                        <select id="courseDetailSubjectId" name="subjectId" required class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                            <option value="">Select subject</option>
                            <c:forEach var="subject" items="${availableSubjectOptions}">
                                <option value="${subject.id}"><c:out value="${subject.name}"/></option>
                            </c:forEach>
                        </select>
                        <c:if test="${empty availableSubjectOptions}">
                            <span class="d-block text-12 text-neutral-500 mt-8">There are no available subjects to associate.</span>
                        </c:if>
                    </div>
                    <div class="row gy-3">
                        <div class="col-sm-6 gape-select-field">
                            <label for="courseDetailSubjectYear" class="fw-medium text-base text-neutral-800 mb-12">Course year</label>
                            <select id="courseDetailSubjectYear" name="curricularYear" required data-course-year-select data-fixed-duration-years="${course.durationYears}" data-placeholder="Select course year" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                <option value="">Select course year</option>
                                <c:forEach var="yearOption" items="${course.durationYearOptions}">
                                    <option value="${yearOption.value}"><c:out value="${yearOption.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-sm-6 gape-select-field">
                            <label for="courseDetailSubjectTerm" class="fw-medium text-base text-neutral-800 mb-12">Period</label>
                            <select id="courseDetailSubjectTerm" name="term" required class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                <option value="">Select period</option>
                                <c:forEach var="period" items="${coursePeriodTemplates}">
                                    <c:if test="${period.curricularYear == 1}">
                                        <option value="${period.term}"><c:out value="${period.termLabel}"/></option>
                                    </c:if>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <div class="form-check common-check mb-0">
                                <input class="form-check-input" type="checkbox" id="courseDetailSubjectMandatory" name="mandatory" value="true">
                                <label class="form-check-label fw-medium" for="courseDetailSubjectMandatory">Mandatory subject</label>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-neutral-30">
                    <button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="cd-primary-button border-0">Associate Subject</button>
                </div>
            </form>
        </div>
    </div>
</div>
