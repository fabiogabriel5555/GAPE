<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - My Profile</title>
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
                <a href="${pageContext.request.contextPath}/instructor/instructor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor Details</a>
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
            <li class="nav-submenu__item">
                <a href="privacy-policy.jsp" class="nav-submenu__link hover-bg-neutral-30">Privacy Policy</a>
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
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-dashboard-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Account Settings</a>
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


<!-- ==========my dashbord start=========== -->
<div class="dashbord bg-main-25">
    <div class="d-flex">

        
<!-- ========Dashdord Sidebar start======== -->
<div class="dashboard-sidebar px-20 py-24 max-w-288-px bg-white w-100 border-end border-neutral-40 position-relative">
    <a href="#">
      <img src="assets/images/logo/logo.svg" alt="" class="">
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
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-my-profile.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-user-circle"></i></span>
                       My Profile</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-message.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-chat-dots"></i></span>
                       Message</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-courses.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-watch"></i></span>
                       Courses</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-wishlist.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-bookmark-simple"></i></span>
                       Wishlist</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-reviews.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-sparkle"></i></span>
                       Reviews</a>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-quiz-attempts.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
                       <span class="text-16 text-main-600 item-hover__text transition-03"><i class="ph ph-seal-question"></i></span>
                       Quiz Attempts</a>
                   </li>
                   <li class="mb-8">
                       <span class="fw-normal text-14 text-neutral-500">Admin</span>
                   </li>
                   <li class="mb-8">
                       <a href="${pageContext.request.contextPath}/admin/admin-dashbord-settings.jsp" class="fw-medium d-flex align-items-center text-14 gap-8 text-neutral-500 hover-bg-main-600 px-24 py-10 hover-text-white rounded-12 item-hover flex-wrap">
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
            <!-- =====messsage dashbord nab start======== -->
<div class="px-24 py-16 bg-neutral-10 border-bottom border-neutral-40 w-100">
    <div class="d-flex align-items-center justify-content-between gap-24 w-100">
        <div class="d-flex align-items-center gap-24">
            <button type="button" class="toggle-dashbord-button text-neutral-500 text-28 line-height-1 d-lg-none d-block">
                <i class="ph-bold ph-list"></i>
            </button>
            <div class="max-w-357-px position-relative d-sm-block d-none">
                <form action="#">
                    <input type="text" placeholder="Search" class="ps-16 pe-36 py-9 border border-neutral-40 rounded-pill focus-visible-outline focus-border-main-600 text-14 line-height-1">
                    <button type="button" class="w-28 h-28 bg-main-600 text-white text-16 rounded-circle justify-content-center align-items-center d-flex position-absolute top-50-percent translate-middle-y inset-inline-end-0-px me-4">
                        <i class="ph-bold ph-magnifying-glass"></i>
                    </button>
                </form>
            </div>
        </div>

        <div class="d-flex align-items-center gap-16">
            <a href="#" class="px-20 py-10 border-main-600 border rounded-pill text-14 text-main-600 hover-bg-main-600 hover-text-white hover-border-600 d-lg-block d-none line-height-1">Create a New Course</a>
            <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 hover-bg-main-600  transition-03">
                <button type="button" class="hover-text-white transition-03">
                    <i class="ph ph-translate"></i>
                </button>
            </div>
            <div class="position-relative">
                <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 position-relative">
                    <div>
                        <button class="dropdown-toggle w-36 h-36 border-neutral-50 border rounded-pill hover-bg-main-600 hover-text-white transition-03" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="ph ph-bell-simple"></i>
                        </button>
                        <ul class="dropdown-menu rounded-12">

                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                                <div class="d-flex w-100 justify-content-between gap-12">
                                    <div class="d-flex align-items-center gap-12 ">
                                        <span class="w-36 h-36">
                                            <img src="assets/images/thumbs/reviewer-img1.png" alt="">
                                        </span>
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
                    <div class="position-absolute inset-inline-start-22px top--20-percent">
                        <span class="bg-main-600 text-white w-16 h-16 rounded-circle text-9 d-flex justify-content-center align-items-center">2</span>
                    </div>
                </div>
            </div>

            <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 hover-bg-main-600 transition-03">
                <button type="button" class=" hover-text-white transition-03">
                    <i class="ph ph-chat-dots"></i>
                </button>
            </div>

            <div class="dropdown d-flex align-items-center gap-12">
                <button class="dropdown-toggle gape-user-avatar-trigger w-36 h-36 border-neutral-50 border rounded-pill" type="button" data-bs-toggle="dropdown" aria-expanded="false" title="${sessionScope['gape.auth.userEmail']}">
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
                        <a class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03" href="${pageContext.request.contextPath}/admin/admin-dashbord-settings.jsp">
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

        </div>
    </div>
</div>
<!-- =====message nab end======== -->
        <!-- =========my profile start=========== -->
            <div class="px-24 py-24">
                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center gap-16 justify-content-between mb-20">
                        <span class="fw-medium text-16 text-neutral-500">My Profile</span>
                        <div class="d-flex align-items-center gap-8">
                            <span class="text-20">
                                <i class="ph-bold ph-download-simple"></i>
                            </span>
                            <span class="text-20">
                                <i class="ph ph-printer"></i>
                            </span>
                        </div>
                    </div>
                    <div class="d-flex flex-wrap">

                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-top border-bottom px-20 py-26 line-height-105 border-end flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">First Name</span>
                                <span class="text-14 fw-normal text-neutral-600">Cameron</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 border-end flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Last Name</span>
                                <span class="text-14 fw-normal text-neutral-600">Williamson</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 border-end flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Username</span>
                                <span class="text-14 fw-normal text-neutral-600">@Williamson458</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 border-end flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Email</span>
                                <span class="text-14 fw-normal text-neutral-600">demo@gmail.com</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 border-end flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Phone Number</span>
                                <span class="text-14 fw-normal text-neutral-600">(808) 555-0111</span>
                            </div>
                        </div>

                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-top border-bottom px-20 py-26 line-height-105 flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Registration Date</span>
                                <span class="text-14 fw-normal text-neutral-600">June 05, 2025 9:25 PM</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Skill/Occupation</span>
                                <span class="text-14 fw-normal text-neutral-600">UI/UX Design</span>
                            </div>
                            <div class="d-flex align-items-center gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-26 line-height-105 flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Address</span>
                                <span class="text-14 fw-normal text-neutral-600">2464 Royal Ln. Mesa, New Jersey 45463</span>
                            </div>
                            <div class="d-flex gap-16 justify-content-between border-neutral-30 border-bottom px-20 py-24 line-height-105 flex-wrap text-end">
                                <span class="text-14 fw-normal text-neutral-600">Biography</span>
                                <span class="text-14 fw-normal text-neutral-600 max-w-344-px pb-40">It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout.</span>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        <!-- ============my profile end========== -->
          <!-- =========message profile footer start============== -->
<div class="bg-neutral-20 border-neutral-40 border-top px-24 py-16 mt-auto">
    <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
        <p class="fw-medium text-14 text-neutral-500">
            Copyright &copy; 2025 
            <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="text-main-600 fw-medium">GAPE</a>.
            All Rights Reserved
        </p>
        <div class="d-flex align-items-center gap-24">
            <a href="privacy-policy.jsp" class="fw-medium text-14 text-neutral-500 hover-text-main-600">Privacy Policy</a>
            <a href="#" class="fw-medium text-14 text-neutral-500 hover-text-main-600">Terms & Conditions</a>
        </div>
    </div>
</div>
<!-- =========message profile footer end============== -->
        </div>
        <!-- ==========my dashbord end============ -->
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








