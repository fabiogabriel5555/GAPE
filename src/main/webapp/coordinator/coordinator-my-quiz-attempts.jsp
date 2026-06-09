<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - Coordinator Dashboard My Quiz Attempts</title>
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
                <a href="${pageContext.request.contextPath}/coordinator/coordinator.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator Details</a>
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
                <a href="${pageContext.request.contextPath}/admin/admin-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
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
                <a href="${pageContext.request.contextPath}/student/student-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-home.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Account Settings</a>
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



<!-- ==================== Header Start Here ==================== -->
<header class="header bg-neutral-900">
    <div class="container container--xl">
        <nav class="header-inner flex-between gap-8">

            <div class="header-content-wrapper flex-align flex-grow-1">
                <!-- Logo Start -->
                <a href="index.jsp" class="link">
                    <img src="assets/images/logo/logo-white.svg" alt="Logo">
                </a>
                <!-- Logo End  -->

                <!-- Select Start -->
                <div class="d-sm-block d-none">
                    <div class="student-dashboard-select border-neutral-600 border rounded-pill position-relative">
    <span class="select-icon position-absolute top-50 d-xxl-block d-none translate-middle-y inset-inline-start-0 z-1 ms-lg-4 ms-12 text-xl pointer-event-none d-flex">
        <i class="ph-bold ph-squares-four"></i>
    </span>
    <select class="js-example-basic-single student-dashboard-select" name="state">
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
                <!-- Select End -->

                <!-- Menu Start  -->
                <div class="header-menu d-lg-block d-none">

<ul class="nav-menu flex-align ">
    <li class="nav-menu__item"><a href="index.jsp" class="nav-menu__link text-white">Home</a></li>

    <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link text-white">Courses</a>
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
        <a href="javascript:void(0)" class="nav-menu__link text-white">Pages</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="about-four.jsp" class="nav-submenu__link hover-bg-neutral-30"> About Four</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator Details</a>
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
        <a href="javascript:void(0)" class="nav-menu__link text-white">Dashboard</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30"> Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
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
                <a href="${pageContext.request.contextPath}/student/student-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-home.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Account Settings</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item">
        <a href="contact.jsp" class="nav-menu__link text-white">Contact</a>
    </li>
</ul>
                </div>
                <!-- Menu End  -->
            </div>

            <!-- Header Right start -->
            <div class="header-right flex-align">
                <form action="#" class="search-form position-relative d-xl-block d-none">
                    <input type="text" class="common-input rounded-pill bg-neutral-700 text-neutral-30 pe-48 border-neutral-600 border" placeholder="Search...">
                    <button type="submit" class="w-36 h-36 bg-main-600 hover-bg-main-700 rounded-circle flex-center text-md text-white position-absolute top-50 translate-middle-y inset-inline-end-0 me-8">
                        <i class="ph-bold ph-magnifying-glass"></i>
                    </button>
                </form>
                <div>
                    <button class="dropdown-toggle w-48 h-48 text-24 border-neutral-600 border bg-neutral-700 rounded-pill hover-bg-main-600 hover-text-white transition-03 text-white position-relative" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="ph ph-shopping-cart"></i>
                        <span class="w-22 h-22 flex-center rounded-circle bg-main-two-600 text-white text-xs position-absolute top-n6 end-n4">1</span>
                    </button>
                    <ul class="dropdown-menu rounded-12">

                        <li>
                            <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                            <div class="d-flex w-100 justify-content-between gap-12">
                                <div class="d-flex align-items-center gap-12 ">
                                    <span class="w-36 h-36"><img src="assets/images/thumbs/reviewer-img1.png" alt=""></span>
                                    <div>
                                        <span class="text-md fw-semibold text-line-1">Ronald Richards</span>
                                        <p class="text-sm text-line-1 text-neutral-300">You can stitch between artboards</p>
                                    </div>
                                </div>
                                <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                            </div>
                            </a>
                        </li>

                        <li>
                            <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                            <div class="d-flex w-100 justify-content-between gap-12">
                                <div class="d-flex align-items-center gap-12 ">
                                    <span class="w-36 h-36"><img src="assets/images/thumbs/reviewer-img4.png" alt=""></span>
                                    <div>
                                        <span class="text-md fw-semibold text-line-1">Arlene McCoy</span>
                                        <p class="text-sm text-line-1 text-neutral-300">Invite you to prototyping</p>
                                    </div>
                                </div>
                                <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                            </div>
                            </a>
                        </li>

                        <li>
                            <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                            <div class="d-flex w-100 justify-content-between gap-12">
                                <div class="d-flex align-items-center gap-12 ">
                                    <span class="w-36 h-36"><img src="assets/images/thumbs/instructor-details-thumb.png" alt=""></span>
                                    <div>
                                        <span class="text-md fw-semibold text-line-1">Annette Black</span>
                                        <p class="text-sm text-line-1 text-neutral-300">Invite you to prototyping</p>
                                    </div>
                                </div>
                                <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                            </div>
                            </a>
                        </li>
                    </ul>
                </div>

                <div class="dropdown">
                    <button class="dropdown-toggle gape-user-avatar-trigger w-48 h-48" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <%@ include file="/WEB-INF/fragments/user-avatar-content.jspf" %>
                    </button>
                    <ul class="dropdown-menu rounded-12">
                        <li>
                            <a class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03" href="${pageContext.request.contextPath}/profile">
                            <span><i class="ph ph-user-circle"></i></span>
                            <span>My Profile</span>
                            </a>
                        </li>
                        <li>
                            <a class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03" href="${pageContext.request.contextPath}/coordinator/coordinator-account-settings.jsp">
                            <span><i class="ph ph-gear"></i></span>
                            <span>Settings</span>
                            </a>
                        </li>
                        <li>
                            <form action="${pageContext.request.contextPath}/auth/logout" method="post" class="m-0">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.logoutCsrfToken']}">
                                <button type="submit" class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03 border-0 bg-transparent w-100 text-start">
                            <span><i class="ph ph-power"></i></span>
                            <span>Log Out</span>
                                </button>
                            </form>
                        </li>
                    </ul>
                </div>
                <button type="button" class="toggle-mobileMenu d-lg-none text-white flex-center">
                    <i class="ph ph-list"></i>
                </button>
            </div>
            <!-- Header Right End  -->
        </nav>
    </div>
</header>
<!-- ==================== Header End Here ==================== -->



<!-- ============== student dashbord banner section start =============== -->
<section class="breadcrumb pt-80 pb-187 bg-neutral-900 position-relative z-1 overflow-hidden mb-0 z-n1">
    <img src="assets/images/shapes/shape1.png" alt="" class="position-absolute inset-block-start-5-persent inset-inline-start-45-persent animation-rotation d-md-block d-none">
    <img src="assets/images/shapes/shape2.png" alt="" class="position-absolute inset-inline-start-0-persent inset-block-end-0-persent animation-scalation d-md-block d-none">
    <img src="assets/images/shapes/shape3.png" alt="" class="position-absolute inset-inline-end-35-persent inset-block-end-30-persent animation-walking d-md-block d-none">
    <img src="assets/images/shapes/shape5.png" alt="" class="shape six animation-walking d-md-block d-none">
    <img src="assets/images/shapes/shape4.png" alt="" class="position-absolute inset-inline-end-40-persent inset-block-start-5-persent animation-scalation">
    <img src="assets/images/shapes/shape4.png" alt="" class="position-absolute inset-inline-end-2-persent inset-block-end-45-persent animation-scalation">
    <img src="assets/images/shapes/shape4.png" alt="" class="position-absolute inset-block-end-30-persent inset-inline-end-45-persent animation-scalation">
    <img src="assets/images/shapes/shape4.png" alt="" class="shape nine animation-scalation">
    <img src="assets/images/shapes/shape6.png" alt="" class="animation-scalation inset-inline-end-2-persent inset-block-start-2-persent position-absolute">

    <div class="container container--lg">
        <div class="d-flex align-items-center gap-16 justify-content-between flex-wrap">
            <div class="breadcrumb__wrapper">
                <h1 class="breadcrumb__title display-4 fw-semibold text-white mb-20">Coordinator Dashboard</h1>
                <ul class="breadcrumb__list d-flex align-items-center gap-4 flex-wrap">
                    <li class="breadcrumb__item">
                        <a href="index.jsp" class="breadcrumb__link text-white hover-text-warning-800 fw-medium">Home</a>
                    </li>
                    <li class="breadcrumb__item">
                        <i class="text-white d-flex ph-bold ph-caret-right"></i>
                    </li>
                    <li class="breadcrumb__item">
                        <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="breadcrumb__link text-white hover-text-warning-800 fw-medium">Dashboard</a>
                    </li>
                    <li class="breadcrumb__item @@arrowTwoShowHide">
                        <i class="text-white d-flex ph-bold ph-caret-right"></i>
                    </li>
                    <li class="breadcrumb__item">
                        <span class="text-main-two-600">Coordinator Dashboard</span>
                    </li>
                </ul>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/coordinator/coordinator.jsp" class="border-warning-800 border px-32 py-16 rounded-pill text-warning-800 hover-bg-warning-900 hover-text-white">Become an Coordinator</a>
            </div>
        </div>
    </div>
</section>
<!-- ============== student dashbord banner section end =============== -->

<!-- =========== coordinator dashbord section start ============== -->

<section class="bg-main-25 explore-course position-relative z-1 pb-80 w-100 h-100">
    <div class="container container--lg">
        <div class="d-flex gap-24 mt--120 z-2 position-relative">
            <div class="student-overlay-sidebar"></div>

<!-- ========Dashdord Sidebar start======== -->
<div class="student-dashboard-sidebar px-20 py-24 max-w-288-px bg-white rounded-10 w-100 h-100 position-relative">
    <div class="text-center">
        <img src="assets/images/thumbs/instructor-dashboard-img1.png" alt="" class="mb-20">
        <h5 class="mb-4 text-neutral-500">Cameron Williamson</h5>
        <span class="text-neutral-500 text-14 fw-normal mb-12">info@example.com</span>
        <ul class="d-flex align-items-center gap-4 justify-content-center text-center mb-4">
            <li class="text-16 text-warning-500"><i class="ph-fill ph-star"></i></li>
            <li class="text-16 text-warning-500"><i class="ph-fill ph-star"></i></li>
            <li class="text-16 text-warning-500"><i class="ph-fill ph-star"></i></li>
            <li class="text-16 text-warning-500"><i class="ph-fill ph-star"></i></li>
            <li class="text-16 text-warning-500"><i class="ph-fill ph-star-half"></i></li>
        </ul>
        <span class="text-14 fw-normal text-neutral-300">4.8/5 (1.5K Reviews)</span>
    </div>
    <span class="w-100 bg-main-100 mb-24 mt-24 h-1"></span>
     <div class="overflow-x-auto">
        <div class="student-dashbord-scrollbar min-w-max pb-80">
            <span class="text-neutral-500 fw-normal text-14 mb-8">Welcome Williamson,</span>
               <ul>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-home.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph-bold ph-house"></i></span>
                       Dashboard</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-profile.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-user-circle"></i></span>
                       My Profile</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-message.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-chat-dots"></i></span>
                        Message</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-enrolled-courses.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-watch"></i></span>
                       Enrolled Courses</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-reviews.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-sparkle"></i></span>
                       Reviews</a>
                   </li>
                   <li class="mb-8 activePage">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-quiz-attempts.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03">
                            <i class="ph ph-seal-question"></i>
                        </span>
                      My Quiz Attempts</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-order-history.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03">
                        <i class="ph ph-shopping-cart"></i>
                    </span>
                       Order History</a>
                   </li>

                   <li class="text-neutral-500 fw-normal text-14 mb-8 mt-8">Coordinator</li>

                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-my-courses.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03">
                        <i class="ph ph-graduation-cap"></i>
                        </span>
                       My Courses</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-announcements.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03 transform-scale-x--1px">
                            <i class="ph ph-megaphone"></i>
                        </span>
                        Announcements</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-assignment.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03 transform-scale-x--1px">
                            <i class="ph ph-file-text"></i>
                        </span>
                        Assignments</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-quiz-attempts.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03 transform-scale-x--1px">
                            <i class="ph ph-seal-question"></i>
                        </span>
                        Quiz Attempts</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/coordinator/coordinator-account-settings.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-gear"></i></span>
                       Account Settings</a>
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

                <!-- ============Feedbacks start============ -->
                <div class="w-100">

                    <div class="px-24 py-24 bg-white rounded-10">
                        <div class="d-flex align-items-center gap-8 justify-content-between">
                            <h6 class="mb-0 fw-medium">Quiz Attempts</h6>
                            <button type="button" class="toggle-student-dashbord-button text-neutral-900 text-24 d-xl-none d-block">
                                <i class="ph-bold ph-list"></i>
                            </button>
                        </div>

                        <div class="mb-24">

                            <div class="">
                                <table id="example-five" class="display min-w-max w-100 overflow-x-auto">
                                    <thead>
                                        <tr class="bg-main-25">
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

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Financial Planning for Millennials</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Brooklyn Simmons</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">4</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">7</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">1</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Photography for Beginners</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Ralph Edwards</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">4</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">4</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">8</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Adobe Photoshop Essentials</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Cameron Williamson</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">9</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">3</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">1</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Leadership and Management Essentials</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Kristin Watson</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">1</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">2</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Web Development Bootcamp</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Annette Black</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">4</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">1</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Digital Marketing Fundamentals</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Courtney Henry</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">4</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">3</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">2</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Digital Marketing 101</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Bessie Cooper</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">2</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">3</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">5</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Introduction to Python Programming</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Albert Flores</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">3</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">8</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Social Media Strategy</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Esther Howard</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">8</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">9</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Project Management Fundamentals</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Jane Cooper</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">2</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">5</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">7</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-success-500 border-neutral-30 border rounded-pill bg-main-05 px-16 py-8">Pass</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Social Media Strategy</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Jacob Jones</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">9</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">8</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                        <tr class="hover-bg-neutral-20 transition-03">
                                            <td class="py-28 px-20 shadow-none">
                                                <div>
                                                    <h6 class="fw-medium text-14 text-neutral-500 mb-5">Creative Writing Essentials</h6>
                                                    <span class="fw-normal text-12 text-neutral-500">Cody Fisher</span>
                                                </div>
                                            </td>
                                            <td class="text-14 fw-normal text-neutral-500 py-28 px-20 shadow-none">January 20, 2025</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">1</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">6</td>
                                            <td class="py-28 px-20 shadow-none text-14 fw-normal text-neutral-500">5</td>
                                            <td class="py-22 px-16 shadow-none">
                                                <span class="text-12 fw-normal text-warning-600 border-neutral-30 border rounded-pill bg-warning-5 px-16 py-8">Fail</span>
                                            </td>
                                            <td class="py-28 px-20 shadow-none">
                                                <div class="d-flex align-items-center gap-12">
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-pencil-simple-line"></i>
                                                    </button>
                                                    <button type="button" class="text-neutral-500 text-24 hover-text-main-600 transition-03">
                                                        <i class="ph ph-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>

                                    </tbody>
                                </table>
                            </div>

                        </div>

                        <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
                            <div class="form-check form-switch d-flex align-items-center gap-8">
                                <input class="form-check-input focus-box-shadow w-60 h-32" type="checkbox" role="switch" id="flexSwitchCheckDefault">
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
        </div>
    </div>
</section>
<!-- =========== coordinator dashbord section end ============== -->

<!-- ==================== student Footer Start Here ==================== -->
<footer class="footer bg-neutral-900 position-relative z-1">
    <div class="container">
        <!-- bottom Footer -->
        <div class="bottom-footer border-top border-dashed border-neutral-600 border-0 py-32">
            <div class="container container-two">
                <div class="bottom-footer__inner flex-between gap-16 flex-wrap">
                    <p class="text-white text-16 fw-normal">Copyright &copy; 2026
                            <span class="text-main-600">GAPE</span>
                        All Rights Reserved.
                    </p>
                    <div class="d-flex align-items-center gap-24">
                        <a href="#" class="text-white text-16 fw-normal hover-text-warning-600">Terms & Conditions</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</footer>
<!-- ==================== student Footer End Here ==================== -->

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











