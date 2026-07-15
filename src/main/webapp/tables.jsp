<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Assessment Attempts");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - Dashbord Assessments</title>
    <!-- Favicon -->
    <link rel="shortcut icon" href="assets/images/logo/favicon.png">
    <!-- Bootstrap -->
    <link rel="stylesheet" href="assets/css/bootstrap.min.css">
    <!-- select2 -->
    <link rel="stylesheet" href="assets/css/select2.min.css">
    <!-- Slick -->
    <link rel="stylesheet" href="assets/css/slick.css">
    <!-- Slick -->
    <link rel="stylesheet" href="assets/css/magnific-popup.css">
    <!-- jquery-ui -->
    <link rel="stylesheet" href="assets/css/jquery-ui.css">
    <!-- plyr Css -->
    <link rel="stylesheet" href="assets/css/plyr.css">
    <!-- Editor js Toolbar Start -->
    <link rel="stylesheet" href="assets/css/editor-quill.css">
    <!-- animate -->
    <link rel="stylesheet" href="assets/css/animate.css">
    <!-- dataTables.dataTables -->
    <link rel="stylesheet" href="assets/css/dataTables.dataTables.min.css">

    <link rel="stylesheet" href="assets/css/aos.css">
    <!-- Main css -->
    <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>

<!--==================== Preloader Start ====================-->
  <div class="preloader">
    <img src="assets/images/icons/preloader.gif" alt="">
  </div>
<!--==================== Preloader End ====================-->

<!--==================== Overlay Start ====================-->
<div class="overlay"></div>
<!--==================== Overlay End ====================-->

<!--==================== Sidebar Overlay End ====================-->
<div class="side-overlay"></div>
<!--==================== Sidebar Overlay End ====================-->

<!-- ==================== Scroll to Top End Here ==================== -->
<div class="progress-wrap">
  <svg class="progress-circle svg-content" width="100%" height="100%" viewBox="-1 -1 102 102">
      <path d="M50,1 a49,49 0 0,1 0,98 a49,49 0 0,1 0,-98" />
  </svg>
</div>
<!-- ==================== Scroll to Top End Here ==================== -->

<!-- ==================== Mobile Menu Start Here ==================== -->
<div class="mobile-menu scroll-sm d-lg-none d-block">
    <button type="button" class="close-button"><i class="ph ph-x"></i> </button>
    <div class="mobile-menu__inner">
        <a href="index.jsp" class="mobile-menu__logo">
            <img src="assets/images/logo/logo.svg" alt="Logo">
        </a>
        <div class="mobile-menu__menu">

<ul class="nav-menu flex-align nav-menu--mobile">
    <li class="nav-menu__item"><a href="index.jsp" class="nav-menu__link">Home</a></li>

    <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Courses</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="course.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course Grid View</a>
            </li>
            <li class="nav-submenu__item">
                <a href="course-list-view.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course List View</a>
            </li>
            <li class="nav-submenu__item">
                <a href="course-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="lesson-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Lesson Details</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Pages</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="about-four.jsp" class="nav-submenu__link hover-bg-neutral-30"> About Four</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/tutor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/tutor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="tutor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Premium Tutors</a>
            </li>
            <li class="nav-submenu__item">
                <a href="tutor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Premium Tutors Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="events.jsp" class="nav-submenu__link hover-bg-neutral-30">Events</a>
            </li>
            <li class="nav-submenu__item">
                <a href="event-details.jsp" class="nav-submenu__link hover-bg-neutral-30">Event Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="apply-admission.jsp" class="nav-submenu__link hover-bg-neutral-30">Apply Admission</a>
            </li>
        </ul>
    </li>
        <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Dashboard</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30"> Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Messages</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/assessments" class="nav-submenu__link hover-bg-neutral-30">Assessments</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/profile" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-home.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/assessments" class="nav-submenu__link hover-bg-neutral-30">Assessments</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/class-groups" class="nav-submenu__link hover-bg-neutral-30">Instructor Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/assessments" class="nav-submenu__link hover-bg-neutral-30">Assessments</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/class-groups" class="nav-submenu__link hover-bg-neutral-30">Instructor My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/class-groups" class="nav-submenu__link hover-bg-neutral-30">Instructor Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/learning/assessments" class="nav-submenu__link hover-bg-neutral-30">Assessments</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Account Settings</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item">
        <a href="contact.jsp" class="nav-menu__link">Contact</a>
    </li>
</ul>

            <div class="d-sm-none d-block mt-24">
                <div class="header-select border border-neutral-30 bg-main-25 rounded-pill position-relative">
    <span class="select-icon d-xxl-block d-none position-absolute top-50 translate-middle-y inset-inline-start-0 z-1 ms-lg-4 ms-12 text-xl pointer-event-none d-flex">
        <i class="ph-bold ph-squares-four"></i>
    </span>
    <select class="js-example-basic-single border-0" name="state">
        <option value="1" selected disabled>Categories</option>
        <option value="1">Design</option>
        <option value="1">Development</option>
        <option value="1">Architecture</option>
        <option value="1">Life Style</option>
        <option value="1">Data Science</option>
        <option value="1">Marketing</option>
        <option value="1">Music</option>
        <option value="1">Typography</option>
        <option value="1">Finance</option>
        <option value="1">Motivation</option>
    </select>
</div>
            </div>

        </div>
    </div>
</div>
<!-- ==================== Mobile Menu End Here ==================== -->


<!-- ==========message dashbord start=========== -->
<div class="dashbord bg-main-25">
    <div class="d-flex">

<!-- ========Dashdord Sidebar start======== -->
<div class="dashboard-sidebar px-20 py-24 max-w-288-px bg-white w-100 border-end border-neutral-40 position-relative">
    <a href="${pageContext.request.contextPath}/index.jsp" class="dashboard-sidebar__site-logo">
      <img src="assets/images/logo/logo.svg" alt="GAPE" class="">
    </a>
    <span class="w-100 bg-neutral-40 mb-24 mt-24 h-1"></span>
     <div class="overflow-x-auto">
        <div class="scrollbar min-w-max">
            <span class="text-neutral-500 fw-normal text-14 mb-8">Welcome Henry,</span>
               <ul>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph-bold ph-house"></i></span>
                       Dashboard</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-my-profile.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-user-circle"></i></span>
                       My Profile</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-message.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-chat-dots"></i></span>
                       Message</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-courses.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-watch"></i></span>
                       Courses</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-reviews.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-sparkle"></i></span>
                       Reviews</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/learning/assessments" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-seal-question"></i></span>
                       Assessments</a>
                   </li>
                   <li class="mb-8">
                       <span class="fw-normal text-14 text-neutral-500">Admin</span>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/profile" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-gear"></i></span>
                       Settings</a>
                   </li>
               </ul>
           </div>
     </div>

     <div class="position-absolute inset-block-end-0 inset-inline-start-0 pb-16 px-16 w-100">
         <form action="${pageContext.request.contextPath}/auth/logout" method="post" class="m-0">
             <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.logoutCsrfToken']}">
             <button type="submit" class="text-14 fw-medium text-neutral-500 d-flex align-items-center gap-8  hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap bg-white border-0 w-100 text-start">
             <span class="text-16 text-main-600 item-hover__text transition-03">
                 <i class="ph ph-sign-out"></i>
             </span>
             Logout
             </button>
         </form>
     </div>
</div>
<!-- ========Dashdord Sidebar end======== -->

         <div class="dashbord-body my-profile flex-grow-1">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>

            <!-- ============Feedbacks start============ -->
                <div class="px-24 py-24">

                <div class="px-24 py-24 bg-white rounded-10">
                    <div class="mb-24">
                        <h6 class="fw-medium text-16 text-neutral-500 mb-24">Assessment Attempts</h6>
                        <div class="row gy-4">

                            <div class="col-lg-4 col-md-2">
                                <div class="">
                                    <span class="fw-normal text-14 text-neutral-500 mb-12">Courses</span>
                                    <select class="form-select pe-24 border-neutral-40 border rounded-pill bg-main-25 px-20 py-10 w-100 text-12 fw-normal  text-neutral-700 line-height-105">
                                        <option value="1">All</option>
                                        <option value="2">Everyone</option>
                                        <option value="3">One</option>
                                        <option value="4">Two</option>
                                    </select>
                                </div>
                            </div>

                            <div class="col-lg-4 col-md-2">
                                <div class="">
                                    <span class="fw-normal text-14 text-neutral-500 mb-12">Sort By :</span>
                                    <select class="form-select pe-24 border-neutral-40 border rounded-pill bg-main-25 px-20 py-10 w-100 text-12 fw-normal  text-neutral-700 line-height-105">
                                        <option value="1">Default</option>
                                        <option value="2">date</option>
                                        <option value="3">Category</option>
                                        <option value="4">Rating</option>
                                    </select>
                                </div>
                            </div>

                            <div class="col-lg-4 col-md-2">
                                <div class="">
                                    <span class="fw-normal text-14 text-neutral-500 mb-12">Sort By Offer :</span>
                                    <select class="form-select pe-24 border-neutral-40 border rounded-pill bg-main-25 px-20 py-10 w-100 text-12 fw-normal  text-neutral-700 line-height-105">
                                        <option value="1">Free</option>
                                        <option value="2">Basic</option>
                                        <option value="3">Standard</option>
                                        <option value="4">Premium</option>
                                    </select>
                                </div>
                            </div>

                        </div>
                    </div>
                    <div class="mb-24">

                        <div class="overflow-y-auto">
                            <table id="example-five" class="display min-w-max w-100">
                                <thead>
                                    <tr class="bg-main-25 border-bottom border-neutral-30">
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Quiz</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Date</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Qus</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">TM</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">CA</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Result</th>
                                        <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Action</th>
                                    </tr>
                                </thead>
                                <tbody>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Financial Planning for Millennials</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Brooklyn Simmons</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">4</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">7</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">1</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Photography for Beginners</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Ralph Edwards</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">4</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">4</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">8</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Adobe Photoshop Essentials</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Cameron Williamson</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">9</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">3</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">1</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Leadership and Management Essentials</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Kristin Watson</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">1</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">2</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Web Development Bootcamp</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Annette Black</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">4</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">1</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Digital Marketing Fundamentals</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Courtney Henry</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">4</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">3</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">2</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Digital Marketing 101</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Bessie Cooper</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">2</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">3</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">5</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Introduction to Python Programming</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Albert Flores</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">3</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">8</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Social Media Strategy</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Esther Howard</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">8</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">9</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Project Management Fundamentals</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Jane Cooper</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">2</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">5</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">7</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-success-50 px-20 py-8 border-neutral-30 border rounded-pill text-success-600">Pass</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Social Media Strategy</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Jacob Jones</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">9</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">8</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                    <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <h4 class="fw-medium text-14 text-neutral-500 mb-1">Creative Writing Essentials</h4>
                                            <span class="fw-normal text-12 text-neutral-500">Cody Fisher</span>
                                        </td>
                                        <td class="text-14 fw-normal py-28 px-20 shadow-none line-height-105">January 20, 2025</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">1</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">6</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">5</td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <span class="bg-warning-30 px-20 py-8 border-neutral-30 border rounded-pill text-warning-600">Fail</span>
                                        </td>
                                        <td class="py-28 px-20 shadow-none line-height-105">
                                            <div class="d-flex align-items-center gap-12  justify-content-end">
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                                <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                            </div>
                                        </td>
                                    </tr>

                                </tbody>
                            </table>
                        </div>

                    </div>

                    <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
                        <div class="form-check form-switch">
                            <input class="form-check-input focus-box-shadow" type="checkbox" role="switch" id="flexSwitchCheckDefault">
                            <label class="form-check-label text-14 fw-normal text-neutral-500" for="flexSwitchCheckDefault">Dense</label>
                        </div>
                        <div class="d-flex align-items-center gap-40 flex-wrap">

                            <div class="d-flex align-items-center gap-16">
                                <span class="fw-normal text-14 text-neutral-500">Rows per page:</span>
                                <select class="form-select w-auto pe-32 text-14 fw-normal text-neutral-500 bg-transparent border-0">
                                    <option value="1">12</option>
                                    <option value="1">13</option>
                                    <option value="1">11</option>
                                    <option value="1">15</option>
                                </select>
                            </div>

                            <div class="d-flex align-items-center gap-16">
                                <span class="fw-normal text-14 text-neutral-500">1-12 of 100</span>
                                <div class="d-flex align-items-center gpa-8">
                                    <span class="text-20 text-neutral-500">
                                        <i class="ph-bold ph-caret-left"></i>
                                    </span>
                                    <span class="text-20 text-neutral-500">
                                        <i class="ph-bold ph-caret-right"></i>
                                    </span>
                                </div>
                            </div>

                        </div>

                    </div>

                </div>
                </div>
            <!-- ============Feedbacks end============ -->

          <!-- =========message profile footer start============== -->
<div class="bg-neutral-20 border-neutral-40 border-top px-24 py-16 mt-auto">
    <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
        <p class="fw-medium text-14 text-neutral-500">
            Copyright &copy; 2026
            <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="text-main-600 fw-medium">GAPE</a>.
            All Rights Reserved
        </p>
        <div class="d-flex align-items-center gap-24">
            <a href="#" class="fw-medium text-14 text-neutral-500 hover-text-main-600">Terms & Conditions</a>
        </div>
    </div>
</div>
<!-- =========message profile footer end============== -->
        </div>
        <!-- ==========message dashbord end============ -->
    </div>
</div>



    <!-- Jquery js -->
    <script src="assets/js/jquery-3.7.1.min.js"></script>
    <!-- Bootstrap Bundle Js -->
    <script src="assets/js/boostrap.bundle.min.js"></script>
    <!-- select2 Js -->
    <script src="assets/js/select2.min.js"></script>
    <!-- Phosphor Icon Js -->
    <script src="assets/js/phosphor-icon.js"></script>
    <!-- Slick js -->
    <script src="assets/js/slick.min.js"></script>
    <!-- Slick js -->
    <script src="assets/js/counter.min.js"></script>
    <!-- magnific popup -->
    <script src="assets/js/magnific-popup.min.js"></script>
    <!-- Jquery Ui js -->
    <script src="assets/js/jquery-ui.js"></script>
    <!-- marquee js -->
    <script src="assets/js/marquee.min.js"></script>
    <!-- react charts-->
     <script src="assets/js/apexcharts.js"></script>
    <!-- plyr Js -->
    <script src="assets/js/plyr.js"></script>
    <!-- vanilla Tilt -->
    <!-- Editor js Toolbar Start -->
    <script src="assets/js/editor-quill.js"></script>
    <!-- dataTables -->
    <script src="assets/js/dataTables.min.js"></script>
    <!-- Tilt -->
    <script src="assets/js/vanilla-tilt.min.js"></script>
    <!-- wow -->
    <script src="assets/js/wow.min.js"></script>

    <script src="assets/js/aos.js"></script>

    <!-- main js -->
    <script src="assets/js/main.js"></script>






</body>
</html>
