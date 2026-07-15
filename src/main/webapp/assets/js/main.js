(function ($) {
  "use strict";

  // ==========================================
  //      Start Document Ready function
  // ==========================================
  $(document).ready(function () {



    // ============= Student dashbord sidebar js start=================
    // ========================== Course List filter bar btn start ================================
  $('.toggle-student-dashbord-button').on('click', function () {
    $('.student-dashboard-sidebar').addClass('active');
    $('.student-overlay-sidebar').addClass('show');
  });

  $('.sidebar-close, .student-overlay-sidebar').on('click', function () {
    $('.student-dashboard-sidebar').removeClass('active');
    $('.student-overlay-sidebar').removeClass('show');
  });
  // ========================== Course List filter bar btn End ================================
    // ============= Student dashbord sidebar js end=================


    // ========================= Our Popular Tutors Slider Js Start ==============
 $('.tastimonial-six-slider').slick({
  slidesToShow: 3,
  slidesToScroll: 1,
  autoplay: false,
  autoplaySpeed: 2000,
  speed: 1500,
  dots: false,
  pauseOnHover: true,
  arrows: true,
  draggable: true,
  rtl: $('html').attr('dir') === 'rtl' ? true : false,
  speed: 900,
  infinite: true,
  nextArrow: '#tastimonial-six-next',
  prevArrow: '#tastimonial-six-prev',
  responsive: [
    {
      breakpoint: 1299,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 767,
      settings: {
        slidesToShow: 1,
        arrows: false,
      }
    },
    {
      breakpoint: 575,
      settings: {
        slidesToShow: 1,
        arrows: false,
      }
    },
  ]
});
// ========================= Our Popular Tutors Slider Js end ===================




 // ========================= Our Popular Tutors Slider Js Start ==============
 $('.our-popular-tutors-six-slider').slick({
  slidesToShow: 4,
  slidesToScroll: 1,
  autoplay: false,
  autoplaySpeed: 2000,
  speed: 1500,
  dots: false,
  pauseOnHover: true,
  arrows: true,
  draggable: true,
  rtl: $('html').attr('dir') === 'rtl' ? true : false,
  speed: 900,
  infinite: true,
  nextArrow: '#our-popular-tutors-six-next',
  prevArrow: '#our-popular-tutors-six-prev',
  responsive: [
    {
      breakpoint: 1299,
      settings: {
        slidesToShow: 3,
        arrows: false,
      }
    },
    {
      breakpoint: 767,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 575,
      settings: {
        slidesToShow: 1,
        arrows: false,
      }
    },
  ]
});
// ========================= Our Popular Tutors Slider Js end ===================




// ================admin dashbord start========================


  // ========================== Course List filter bar btn start ================================
  $('.toggle-dashbord-button').on('click', function () {
    $('.dashboard-sidebar').addClass('active');
    $('.side-overlay').addClass('show');
  });

  $('.sidebar-close, .side-overlay').on('click', function () {
    $('.dashboard-sidebar').removeClass('active');
    $('.side-overlay').removeClass('show');
  });
  // ========================== Course List filter bar btn End ================================

  function normalizeSidebarFileName(value) {
    if (!value) {
      return "";
    }

    return value
      .split("?")[0]
      .split("#")[0]
      .replace(/\\/g, "/")
      .split("/")
      .filter(Boolean)
      .map(function (part) {
        return part.split(";")[0];
      })
      .filter(Boolean)
      .pop()
      .toLowerCase();
  }

  function normalizeSidebarPath(value) {
    if (!value) {
      return "";
    }

    var normalized = value
      .split("?")[0]
      .split("#")[0]
      .replace(/\\/g, "/");
    var origin = window.location.origin.toLowerCase();
    if (normalized.toLowerCase().indexOf(origin) === 0) {
      normalized = normalized.slice(origin.length);
    }

    normalized = normalized
      .split("/")
      .filter(Boolean)
      .map(function (part) {
        return part.split(";")[0];
      })
      .filter(Boolean)
      .join("/")
      .toLowerCase();

    return normalized ? "/" + normalized.replace(/\/+$/, "") : "";
  }

  function isActiveSidebarFile(currentFileName, hrefFileName) {
    if (!currentFileName || !hrefFileName) {
      return false;
    }

    if (currentFileName === hrefFileName) {
      return true;
    }

    var aliasFiles = {};

    return aliasFiles[currentFileName] === hrefFileName || aliasFiles[hrefFileName] === currentFileName;
  }

  function isActiveSidebarPath(currentPath, hrefPath) {
    if (!currentPath || !hrefPath) {
      return false;
    }

    if (currentPath === hrefPath) {
      return true;
    }

    return hrefPath.indexOf(".jsp") === -1 && currentPath.indexOf(hrefPath + "/") === 0;
  }

  function normalizeStudentSidebarMenuKey(value) {
    if (!value) {
      return "";
    }

    var normalized = String(value).trim().toLowerCase();
    var aliases = {
      "my-profile": "profile",
      "classgroups": "class-groups",
      "class_group": "class-groups",
      "class-groups-detail": "class-groups",
      "course-detail": "courses",
      "subject-detail": "subjects",
      "lesson-detail": "lessons"
    };

    return aliases[normalized] || normalized;
  }

  function isCurrentSidebarSectionPath(currentPath, sectionPath) {
    if (!currentPath || !sectionPath) {
      return false;
    }

    var index = currentPath.indexOf(sectionPath);
    if (index === -1) {
      return false;
    }

    var nextCharacter = currentPath.charAt(index + sectionPath.length);
    return !nextCharacter || nextCharacter === "/";
  }

  function studentSidebarMenuKeyForPath(currentPath, currentFileName) {
    var sectionPaths = [
      { key: "courses", paths: ["/student/courses"] },
      { key: "subjects", paths: ["/student/subjects"] },
      { key: "class-groups", paths: ["/student/class-groups"] },
      { key: "lessons", paths: ["/student/lessons"] },
      { key: "calendar", paths: ["/student/calendar", "/student/events"] },
      { key: "assessments", paths: ["/student/assessments", "/student/student/review"] },
      { key: "attendance", paths: ["/student/attendance", "/student/grades"] },
      { key: "profile", paths: ["/student/student/profile"] },
      { key: "message", paths: ["/student/student/message"] },
      { key: "dashboard", paths: ["/student/student/dashboard"] }
    ];
    var fileAliases = {
      "student-enrolled-courses.jsp": "courses",
      "student-my-profile.jsp": "profile",
      "student-message.jsp": "message",
      "student-reviews.jsp": "assessments",
      "student-home.jsp": "dashboard",
      "student-assignment.jsp": "dashboard",
      "student-my-quiz-attempts.jsp": "assessments",
      "student-settings.jsp": "profile"
    };

    for (var index = 0; index < sectionPaths.length; index += 1) {
      var section = sectionPaths[index];
      for (var pathIndex = 0; pathIndex < section.paths.length; pathIndex += 1) {
        if (isCurrentSidebarSectionPath(currentPath, section.paths[pathIndex])) {
          return section.key;
        }
      }
    }

    return fileAliases[currentFileName] || "";
  }

  function revealActiveSidebarItem($list) {
    var activeItem = $list.children("li.activePage").first().get(0);
    var scrollContainer = $list.closest(".student-dashbord-scrollbar").get(0);

    if (!activeItem || !scrollContainer) {
      return;
    }

    var viewportHeight = window.innerHeight || document.documentElement.clientHeight;
    var initialContainerRect = scrollContainer.getBoundingClientRect();
    var availableHeight = viewportHeight - initialContainerRect.top - 16;
    if (availableHeight > 140) {
      scrollContainer.style.height = availableHeight + "px";
      scrollContainer.style.maxHeight = availableHeight + "px";
    }

    var itemRect = activeItem.getBoundingClientRect();
    var containerRect = scrollContainer.getBoundingClientRect();
    var visibleTop = Math.max(containerRect.top, 0);
    var visibleBottom = Math.min(containerRect.bottom, viewportHeight);
    var topOverflow = itemRect.top - visibleTop;
    var bottomOverflow = itemRect.bottom - visibleBottom;
    var scrollPadding = 12;
    var visibleHeight = visibleBottom - visibleTop;
    var itemCenter = itemRect.top + (itemRect.height / 2);
    var visibleCenter = visibleTop + (visibleHeight / 2);

    if (topOverflow < scrollPadding || bottomOverflow > -scrollPadding) {
      scrollContainer.scrollTop += itemCenter - visibleCenter;
    }
  }

  function dynamicActiveSidebarClass(selector) {
    var currentFileName = normalizeSidebarFileName(window.location.pathname);
    var currentPath = normalizeSidebarPath(window.location.pathname);
    var routeMenuKey = studentSidebarMenuKeyForPath(currentPath, currentFileName);

    selector.each(function () {
      var $list = $(this);
      var explicitMenuKey = normalizeStudentSidebarMenuKey(
        $list.closest(".student-dashboard-sidebar").attr("data-active-menu")
      );
      var targetMenuKey = explicitMenuKey || routeMenuKey;

      $list.find("li").removeClass("activePage");
      $list.find("li > a[aria-current]").attr("aria-current", "false");

      $list.find("li").each(function () {
        var $item = $(this);
        var $anchor = $item.children("a.item-hover[href]").first();
        var itemMenuKey = normalizeStudentSidebarMenuKey($item.attr("data-menu-key"));
        var hrefFileName = normalizeSidebarFileName($anchor.attr("href"));
        var hrefPath = normalizeSidebarPath($anchor.attr("href"));

        if (
          (itemMenuKey && itemMenuKey === targetMenuKey) ||
          isActiveSidebarPath(currentPath, hrefPath) ||
          isActiveSidebarFile(currentFileName, hrefFileName)
        ) {
          $item.addClass("activePage");
          $anchor.attr("aria-current", "page");
        }
      });

      $list.children("li").each(function () {
        var $item = $(this);
        if ($item.find("li.activePage").length) {
          $item.addClass("activePage");
        }
      });

      revealActiveSidebarItem($list);
    });
  }

  if ($('.student-dashboard-sidebar').length) {
    var $studentSidebarLists = $('.student-dashboard-sidebar ul');
    dynamicActiveSidebarClass($studentSidebarLists);
    window.setTimeout(function () {
      dynamicActiveSidebarClass($studentSidebarLists);
    }, 160);
    $(window).on("load.studentSidebarActive", function () {
      dynamicActiveSidebarClass($studentSidebarLists);
      window.setTimeout(function () {
        dynamicActiveSidebarClass($studentSidebarLists);
      }, 160);
    });
  }



  // ================== Password Show Hide Js Start ==========
  // $(".toggle-password").on('click', function() {
  //   $(this).toggleClass("active");
  //   var input = $($(this).attr("id"));
  //   if (input.attr("type") == "password") {
  //     input.attr("type", "text");
  //     $(this).removeClass('ph ph-eye-closed');
  //     $(this).addClass('ph ph-eye');
  //   } else {
  //     input.attr("type", "password");
  //       $(this).addClass('ph ph-eye-closed');
  //   }
  // });

  function getPasswordTarget($toggle) {
    var targetId = $toggle.attr("id") || "";
    var dataTarget = $toggle.attr("data-toggle-target") || "";

    if (targetId.startsWith("#")) {
      return $(targetId);
    }

    if (targetId.startsWith("toggle-")) {
      return $("#" + targetId.replace("toggle-", ""));
    }

    if (dataTarget) {
      return $(dataTarget);
    }

    return $();
  }

  $(".toggle-password").off("click.eduall").on("click.eduall", function() {
    var $toggle = $(this);
    var input = getPasswordTarget($toggle);

    if (!input.length) {
      return;
    }

    $toggle.toggleClass("active");

    if (input.attr("type") === "password") {
      input.attr("type", "text");
      $toggle.removeClass('ph-bold ph-eye-closed').addClass('ph-bold ph-eye');
    } else {
      input.attr("type", "password");
      $toggle.removeClass('ph-bold ph-eye').addClass('ph-bold ph-eye-closed');
    }
  });

  // ========================= Password Show Hide Js End ===========================

// ==============react charts end===================

function initDataTable(selector, options) {
  if (typeof DataTable === "undefined" || !document.querySelector(selector)) {
    return;
  }

  new DataTable(selector, options);
}

initDataTable('#example', {
  scrollX: true,
  autoWidth: false,
  info: false,
  paging: false,
  searching: false,
});

initDataTable('#example-two', {
  scrollX: true,
  autoWidth: false,
  info: false,
  paging: false,
  searching: false,
});

initDataTable('#example-three', {
  scrollX: true,
  autoWidth: false,
  info: false,
  paging: false,
  searching: false,
});

initDataTable('#example-four', {
  scrollX: true,
  autoWidth: false,
  info: false,
  paging: false,
  searching: false,
});

initDataTable('#example-five', {
  info: false,
  paging: false,
  searching: false,
  scrollX: true,
});




// ================admin dashbord end========================



 // ========================= Text Rotation Js Start ==========================
    const text = document.querySelector(".circle__text");

    if(text) {
      text.innerHTML = text.innerText
      .split("")
      .map(
        (char, i) => `<span style="transform:rotate(${i * 11.5}deg)">${char}</span>`
        )
      .join("");
    }

    // Text Two
    const textTwo = document.querySelector(".circle__desc");

    if(textTwo) {
      textTwo.innerHTML = textTwo.innerText
      .split("")
      .map(
        (char, i) => `<span style="transform:rotate(${i * 11.5}deg)">${char}</span>`
        )
      .join("");
    }
  // ========================= Text Rotation Js End ==========================


// ============about us five js start==============
 // Floating progress bar
 $(".progress-wrapper").each(function(){
  var percentage = $(this).attr("data-perc");
  var floatingLabel = $(this).find(".floating-label");

  // Set CSS variable to be used in keyframes
  floatingLabel.css("--left-percentage", percentage);

  // Trigger reflow to restart animation
  floatingLabel[0].offsetWidth; // Force reflow
  floatingLabel.css("animation-name", "none");
  floatingLabel.css("inset-inline-start", percentage); // Ensure final position is correct
  floatingLabel.css("left", ""); // If 'left' was explicitly used
  floatingLabel.css("animation-name", "animateFloatingLabel");
});



// Semi Circle progress bar
$(".progressBar").each(function(){
  var $bar = $(this).find(".circleBar");
  var $val = $(this).find(".barNumber");
  var perc = parseInt( $val.text(), 10);

  $({p:0}).animate({p:perc}, {
      duration: 3000,
      easing: "swing",
      step: function(p) {
      $bar.css({
          transform: "rotate("+ (45+(p*1.8)) +"deg)", // 100%=180Â° so: Â° = % * 1.8
          // 45 is to add the needed rotation to have the green borders at the bottom
      });
      $val.text(p|0);
      }
  });
});
// ===========about us five js end===============




    // ========================= Brand Slider Js Start ==============
    $('.faq-brand-slider').slick({
      slidesToShow: 4,
      slidesToScroll: 1,
      autoplay: true,
      centerPadding: '100px',
      autoplaySpeed: 2000,
      speed: 1500,
      dots: false,
      pauseOnHover: true,
      arrows: false,
      draggable: true,
      rtl: $('html').attr('dir') === 'rtl' ? true : false,
      speed: 900,
      infinite: true,
      nextArrow: '#brand-next',
      prevArrow: '#brand-prev',
      responsive: [
        {
          breakpoint: 1399,
          settings: {
            slidesToShow: 3,
            arrows: false,
          }
        },
        {
          breakpoint: 992,
          settings: {
            slidesToShow: 2,
            arrows: false,
          }
        },
        {
          breakpoint: 767,
          settings: {
            slidesToShow: 2,
            arrows: false,
          }
        },
        {
          breakpoint: 424,
          settings: {
            slidesToShow: 1,
            arrows: false,
          }
        },
        {
          breakpoint: 359,
          settings: {
            slidesToShow: 1,
            arrows: false,
          }
        },
      ]
    });
    // ========================= Brand Slider Js End ===================



 // ========================= testimonial-five Slider Js Start ==============
 $('.testimonial-five-slider').slick({
  slidesToShow: 3,
  slidesToScroll: 1,
  autoplay: false,
  autoplaySpeed: 2000,
  speed: 1500,
  dots: false,
  pauseOnHover: true,
  arrows: true,
  draggable: true,
  rtl: $('html').attr('dir') === 'rtl' ? true : false,
  speed: 900,
  infinite: true,
  nextArrow: '#testimonial-five-next',
  prevArrow: '#testimonial-five-prev',
  responsive: [
    {
      breakpoint: 1299,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 767,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 575,
      settings: {
        slidesToShow: 1,
        arrows: false,
      }
    },
  ]
});
// ========================= testimonial-five-slider Js End ===================


 // ========================= our-popular-five Slider Js Start ==============
 $('.our-popular-slider').slick({
  slidesToShow: 4,
  slidesToScroll: 1,
  autoplay: false,
  autoplaySpeed: 2000,
  speed: 1500,
  dots: false,
  pauseOnHover: true,
  arrows: true,
  draggable: true,
  rtl: $('html').attr('dir') === 'rtl' ? true : false,
  speed: 900,
  infinite: true,
  nextArrow: '#our-popular-next',
  prevArrow: '#our-popular-prev',
  responsive: [
    {
      breakpoint: 1299,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 767,
      settings: {
        slidesToShow: 2,
        arrows: false,
      }
    },
    {
      breakpoint: 575,
      settings: {
        slidesToShow: 1,
        arrows: false,
      }
    },
  ]
});
// ========================= our-popular-five-slider Js End ===================


      /*===========================================
	=         Marquee Active         =
    =============================================*/
    if ($(".marquee_mode").length) {
      $('.marquee_mode').marquee({
          speed: 100,
          gap: 0,
          delayBeforeStart: 0,
          direction: $('html').attr('dir') === 'rtl' ? 'right' : 'left',
          duplicated: true,
          pauseOnHover: true,
          startVisible:true,
      });
  }


  // ============== Mobile Menu Sidebar & Offcanvas Js Start ========
  $('.toggle-mobileMenu').on('click', function () {
    $('.mobile-menu').addClass('active');
    $('.side-overlay').addClass('show');
    $('body').addClass('scroll-hide-sm');
  });

  $('.close-button, .side-overlay').on('click', function () {
    $('.mobile-menu').removeClass('active');
    $('.side-overlay').removeClass('show');
    $('body').removeClass('scroll-hide-sm');
  });
  // ============== Mobile Menu Sidebar & Offcanvas Js End ========

  // ============== Mobile Nav Menu Dropdown Js Start =======================
  var windowWidth = $(window).width();

  $('.has-submenu').on('click', function () {
    var thisItem = $(this);

    if(windowWidth < 992) {
      if(thisItem.hasClass('active')) {
        thisItem.removeClass('active')
      } else {
        $('.has-submenu').removeClass('active')
        $(thisItem).addClass('active')
      }

      var submenu = thisItem.find('.nav-submenu');

      $('.nav-submenu').not(submenu).slideUp(300);
      submenu.slideToggle(300);
    }

  });
  // ============== Mobile Nav Menu Dropdown Js End =======================

  // ===================== Scroll Back to Top Js Start ======================
  var progressPath = document.querySelector('.progress-wrap path');
  if (progressPath) {
    var pathLength = progressPath.getTotalLength();
    progressPath.style.transition = progressPath.style.WebkitTransition = 'none';
    progressPath.style.strokeDasharray = pathLength + ' ' + pathLength;
    progressPath.style.strokeDashoffset = pathLength;
    progressPath.getBoundingClientRect();
    progressPath.style.transition = progressPath.style.WebkitTransition = 'stroke-dashoffset 10ms linear';
    var updateProgress = function () {
      var scroll = $(window).scrollTop();
      var height = $(document).height() - $(window).height();
      var progress = pathLength - (scroll * pathLength / height);
      progressPath.style.strokeDashoffset = progress;
    }
    updateProgress();
    $(window).scroll(updateProgress);
    var offset = 50;
    var duration = 550;
    jQuery(window).on('scroll', function() {
      if (jQuery(this).scrollTop() > offset) {
        jQuery('.progress-wrap').addClass('active-progress');
      } else {
        jQuery('.progress-wrap').removeClass('active-progress');
      }
    });
    jQuery('.progress-wrap').on('click', function(event) {
      event.preventDefault();
      jQuery('html, body').animate({scrollTop: 0}, duration);
      return false;
    })
  }
  // ===================== Scroll Back to Top Js End ======================

  // ========================== add active class to ul>li top Active current page Js Start =====================
function dynamicActiveMenuClass(selector) {
  let FileName = normalizeSidebarFileName(window.location.pathname);

  // If we are at the root path ("/" or no file name), keep the activePage class on the Home item
  if (FileName === "" || FileName === "index.html" || FileName === "index.jsp") {
    selector.each(function () {
      $(this).find("li.nav-menu__item.has-submenu").eq(0).addClass("activePage");
    });
    return;
  }

  selector.each(function () {
    var $menu = $(this);

    // Remove activePage class from navigation menu items only.
    $menu.find("li.nav-menu__item, li.nav-submenu__item").removeClass("activePage");

    // Add activePage class to the correct li based on the current URL
    $menu.find("li.nav-menu__item, li.nav-submenu__item").each(function () {
      let anchor = $(this).children("a[href]").first();
      if (normalizeSidebarFileName($(anchor).attr("href")) == FileName) {
        $(this).addClass("activePage");
      }
    });

    // If any li has activePage element, add class to its parent li
    $menu.children("li.nav-menu__item").each(function () {
      if ($(this).find(".activePage").length) {
        $(this).addClass("activePage");
      }
    });
  });
}

if ($('.nav-menu').length) {
  dynamicActiveMenuClass($('.nav-menu'));
}
  // ========================== add active class to ul>li top Active current page Js End =====================


  // ========================== Select2 Js Start =================================
  $(document).ready(function() {
    $('.js-example-basic-single').each(function() {
      var $select = $(this);
      if ($select.hasClass('gape-eduall-select')) {
        $select.select2({
          width: '100%',
          selectionCssClass: 'gape-eduall-selection',
          dropdownCssClass: 'gape-eduall-select-dropdown'
        });
        return;
      }
      $select.select2();
    });
  });
  // ========================== Select2 Js End =================================

  // ========================= Brand Slider Js Start ==============
  $('.brand-slider').slick({
    slidesToShow: 7,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: false,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#brand-next',
    prevArrow: '#brand-prev',
    responsive: [
      {
        breakpoint: 1399,
        settings: {
          slidesToShow: 6,
          arrows: false,
        }
      },
      {
        breakpoint: 992,
        settings: {
          slidesToShow: 5,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 4,
          arrows: false,
        }
      },
      {
        breakpoint: 424,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 359,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

  // ========================= Brand Slider Js Start ==============
  $('.features-slider').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#features-next',
    prevArrow: '#features-prev',
    responsive: [
      {
        breakpoint: 991,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

  // ========================= Wishlist Button Js Start ===================
  $('.wishlist-btn').on('click', function () {
    $(this).removeClass('text-main-two-600');
    $(this).toggleClass('text-white bg-main-two-600');
  })
  // ========================= Wishlist Button Js End ===================

  // ========================= Instructor Button Js Start ===================
  $('.social-infos .social-infos__button').on('click', function () {
    $('.social-list').not($(this).siblings('.social-list')).removeClass('d-flex');
    $('.social-infos .social-infos__button').not($(this)).removeClass('active');
    $(this).siblings('.social-list').toggleClass('d-flex');
    $(this).toggleClass('active');
  });
  // ========================= Instructor Button Js End ===================


  // ========================= Instructor Button Js Start ===================
  $('.our-popular-five .our-popular-five__button').on('click', function () {
    $('.social-list').not($(this).siblings('.social-list')).removeClass('d-flex');
    $('.our-popular-five .our-popular-five__button').not($(this)).removeClass('active');
    $(this).siblings('.social-list').toggleClass('d-flex');
    $(this).toggleClass('active');
  });
  // ========================= Instructor Button Js End ===================

  // ========================= Brand Slider Js Start ==============
  $('.instructor-slider').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#instructor-next',
    prevArrow: '#instructor-prev',
    responsive: [
      {
        breakpoint: 1299,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

   // =========================Testimonials Slider Js Start ===================
   $('.testimonials__thumbs-slider').slick({
    slidesToShow: 1,
    slidesToScroll: 1,
    arrows: false,
    fade: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    asNavFor: '.testimonials__slider'
  });

  $('.testimonials__slider').slick({
    slidesToShow: 1,
    slidesToScroll: 1,
    asNavFor: '.testimonials__thumbs-slider',
    dots: false,
    arrows: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    focusOnSelect: true,
    nextArrow: '#testimonials-next',
    prevArrow: '#testimonials-prev',
  });
  // =========================Testimonials Slider Js End ===================


  // ========================= magnific Popup Js Start =====================
  $('.play-button').magnificPopup({
    type:'iframe',
    removalDelay: 300,
    mainClass: 'mfp-fade',
  });
  // ========================= magnific Popup Js End =====================


   // ========================= Counter Up Js End ===================
   const counterUp = window.counterUp.default;

   const callback = (entries) => {
     entries.forEach((entry) => {
       const el = entry.target;
       if (entry.isIntersecting && !el.classList.contains('is-visible')) {
         counterUp(el, {
           duration: 2000,
           delay: 16,
         });
         el.classList.add('is-visible');
       }
     });
   };

   const IO = new IntersectionObserver(callback, { threshold: 1 });

   // Counter Two for each
   const counterNumbers = document.querySelectorAll('.counter');
   if (counterNumbers.length > 0) {
     counterNumbers.forEach((counterNumber) => {
       IO.observe(counterNumber);
     });
   }

  // ========================= Brand Slider Js Start ==============
  $('.category-item-slider').slick({
    slidesToShow: 4,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#category-next',
    prevArrow: '#category-prev',
    responsive: [
      {
        breakpoint: 1199,
        settings: {
          slidesToShow: 3,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

  // ========================= Testimonials Slider Two Js Start ==============
  $('.testimonials-two-slider').slick({
    slidesToShow: 2,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#testimonials-two-next',
    prevArrow: '#testimonials-two-prev',
    responsive: [
      {
        breakpoint: 768,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Testimonials Slider Two Js End ===================

  // ========================= Background Image Js Start ===================
    $(".background-img").css('background-image', function () {
      var bg = 'url(' + $(this).data("background-image") + ')';
      return bg;
    });
  // ========================= Background Image Js End ===================

  // ========================= Testimonials Slider Two Js Start ==============
  $('.banner-three__slider').slick({
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    fade: true,
    nextArrow: '#banner-three-next',
    prevArrow: '#banner-three-prev',
  });

  $('.banner-three__slider').on('beforeChange', function(event, slick, currentSlide, nextSlide) {
    $('.wow').css('visibility', 'hidden').removeClass('animated');
  });

  $('.banner-three__slider').on('afterChange', function(event, slick, currentSlide) {
    new WOW().init();
    $('.wow').css('visibility', 'visible');
  });
// ========================= Testimonials Slider Two Js End ===================

  // ========================= Testimonials Slider Two Js End ===================

  // ========================= Testimonials Slider Two Js Start ==============
  $('.testimonials-three-slider').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    centerMode: true,
    centerPadding: '0px',
    nextArrow: '#testimonials-three-next',
    prevArrow: '#testimonials-three-prev',
    responsive: [
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Testimonials Slider Two Js End ===================

  // ========================= Brand Slider Js Start ==============
  $('.blog-two-slider').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#blog-two-next',
    prevArrow: '#blog-two-prev',
    responsive: [
      {
        breakpoint: 1299,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

  // ========================== Range Slider Js Start =====================
   $(function() {
    $( "#slider-range" ).slider({
        range: true,
        min: 0,
        max: 1000,
        values: [ 100, 1000 ],
        slide: function( event, ui ) {
            $( "#amount" ).val( "$" + ui.values[ 0 ] + " - $" + ui.values[ 1 ] );
        }
    });
    $( "#amount" ).val( "$" + $( "#slider-range" ).slider( "values", 0 ) +
    " - $" + $( "#slider-range" ).slider( "values", 1 ) );
  });

  // ========================== Course List filter bar btn start ================================
  $('.list-bar-btn').on('click', function () {
    $('.sidebar').addClass('active');
    $('.side-overlay').addClass('show');
  });

  $('.sidebar-close, .side-overlay').on('click', function () {
    $('.sidebar').removeClass('active');
    $('.side-overlay').removeClass('show');
  });
  // ========================== Course List filter bar btn End ================================

  // ========================== Tooltip Start ================================
  const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
  const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))
  // ========================== Tooltip Start End ================================

  // ========================= Player Js Start ===========================
    if (typeof Plyr !== "undefined") {
      if (document.querySelector('#player')) {
        new Plyr('#player');
      }
      if (document.querySelector('#featuredPlayer')) {
        new Plyr('#featuredPlayer');
      }
    }
  // ========================= Player Js End ===========================

  // ========================= Brand Slider Js Start ==============
  $('.tutor-slider').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: false,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#tutor-next',
    prevArrow: '#tutor-prev',
    responsive: [
      {
        breakpoint: 1299,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 2,
          arrows: false,
        }
      },
      {
        breakpoint: 575,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Brand Slider Js End ===================

  // ========================= Increment & Decrement Js Start ===================
  var minus = $('.quantity__minus');
  var plus = $('.quantity__plus');

  $(plus).on('click', function () {
    var input = $(this).siblings('.quantity__input');
    var value = input.val();
    value++;
    input.val(value);
  });

  $(minus).on('click', function () {
    var input = $(this).siblings('.quantity__input');
    var value = input.val();
    if(value > 1) {
      value--;
    }
    input.val(value);
  });
  // ========================= Increment & Decrement Js End ===================

  // ========================= Review Js Start ==============
  $('.review-slider, .review-slider-two').slick({
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: false,
    autoplaySpeed: 2000,
    speed: 1500,
    dots: true,
    pauseOnHover: true,
    arrows: true,
    draggable: true,
    rtl: $('html').attr('dir') === 'rtl' ? true : false,
    speed: 900,
    infinite: true,
    nextArrow: '#review-slider-next',
    prevArrow: '#review-slider-prev',
    responsive: [
      {
        breakpoint: 768,
        settings: {
          slidesToShow: 1,
          arrows: false,
        }
      },
    ]
  });
  // ========================= Review Js End ===================

  // ========================= Wow Js Start ===================
  new WOW().init();
  // ========================= Wow Js End ===================

  // ========================= AOS Animation Js Start ===================
  AOS.init({
    offset: 40,
    duration: 1000,
    // once: true,
    easing: 'ease',
  });
  // ========================= AOS Animation Js End ===================

  $('.masonry__image').magnificPopup({
    type: 'image',
    removalDelay: 300,
    mainClass: 'mfp-fade',
    gallery:{
      enabled:true
    }
  });

    // ========================= Color List Js Start ===================
    $('.color-list__button').on('click', function () {
      $('.color-list__button').removeClass('active');

      if(!$(this).hasClass('active')) {
        $(this).addClass('active');
        $(this).removeClass('border-neutral-50');
      } else {
        $(this).removeClass('active');
        $(this).addClass('border-neutral-50');
      };
    });
    // ========================= Color List Js End ===================

    // ========================= Product Details Slider Js Start ===================

    $('.product-big-thumbs').slick({
      slidesToShow: 1,
      slidesToScroll: 1,
      arrows: false,
      dots: false,
      rtl: $('html').attr('dir') === 'rtl' ? true : false,
      fade: true,
      asNavFor: '.product-small-thumbs'
    });
    $('.product-small-thumbs').slick({
      slidesToShow: 4,
      slidesToScroll: 1,
      asNavFor: '.product-big-thumbs',
      arrows: false,
      dots: false,
      rtl: $('html').attr('dir') === 'rtl' ? true : false,
      autoplay: false,
      centerMode: true,
      responsive: [
        {
          breakpoint: 575,
          settings: {
            slidesToShow: 3,
          }
        },
        {
          breakpoint: 424,
          settings: {
            slidesToShow: 2,
          }
        },
      ]
    });
    // ========================= Product Details Slider Js End ===================

    // ========================= Add To Cart Js Start ===================
    $('.add-to-cart').on('click', function () {
      $(this).toggleClass('active')
    });
    // ========================= Add To Cart Js End ===================

    function formatAssessmentWeightTotal(total) {
      if (!Number.isFinite(total)) {
        return '0%';
      }
      var fixed = total.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1');
      return fixed + '%';
    }

    function validateAssessmentWeightForm($form) {
      var $inputs = $form.find('[data-aac-weight-input]');
      var $submit = $form.find('[data-aac-weight-submit]');
      var $total = $form.find('[data-aac-weight-total]');
      var $error = $form.find('[data-aac-weight-error]');
      var total = 0;
      var valid = $inputs.length > 0;

      $inputs.each(function () {
        var $input = $(this);
        var raw = String($input.val() || '').trim().replace(',', '.');
        var inputValid = /^\d+(\.\d{1,2})?$/.test(raw);
        var value = inputValid ? Number(raw) : NaN;

        if (!Number.isFinite(value) || value < 0 || value > 100) {
          inputValid = false;
        }

        if (inputValid) {
          total += value;
        } else {
          valid = false;
        }

        $input.attr('aria-invalid', inputValid ? 'false' : 'true');
      });

      var totalValid = valid && Math.abs(total - 100) < 0.005;
      var blocked = !valid || !totalValid;
      var message = valid
        ? 'The total must be exactly 100%. Current total: ' + formatAssessmentWeightTotal(total) + '.'
        : 'Enter weights between 0 and 100 using up to two decimal places.';

      $total.text(formatAssessmentWeightTotal(total));
      $total.toggleClass('text-success-600', !blocked);
      $total.toggleClass('text-warning-600', valid && blocked);
      $total.toggleClass('text-danger-600', !valid);
      $error.text(blocked ? message : '');
      $error.toggleClass('d-none', !blocked);
      $submit
        .prop('disabled', blocked)
        .attr('aria-disabled', blocked ? 'true' : 'false')
        .attr('title', blocked ? message : 'Save weights')
        .toggleClass('opacity-50', blocked)
        .css('cursor', blocked ? 'not-allowed' : '');

      return !blocked;
    }

    $('[data-aac-weight-form]').each(function () {
      validateAssessmentWeightForm($(this));
    });

    $(document).on('keydown', '[data-aac-weight-input]', function (event) {
      if (['e', 'E', '+', '-'].indexOf(event.key) !== -1) {
        event.preventDefault();
      }
    });

    $(document).on('input', '[data-aac-weight-input]', function () {
      if (this.value.indexOf(',') !== -1) {
        this.value = this.value.replace(/,/g, '.');
      }
      validateAssessmentWeightForm($(this).closest('[data-aac-weight-form]'));
    });

    $(document).on('submit', '[data-aac-weight-form]', function (event) {
      if (!validateAssessmentWeightForm($(this))) {
        event.preventDefault();
      }
    });


  });
  // ==========================================
  //      End Document Ready function
  // ==========================================

  // ========================= Preloader Js Start =====================
    function hidePreloader() {
      $('.preloader').fadeOut(200);
    }

    $(window).on("load", hidePreloader);
    setTimeout(hidePreloader, 2500);
    // ========================= Preloader Js End=====================

    // ========================= Header Sticky Js Start ==============
    $(window).on('scroll', function() {
      if ($(window).scrollTop() >= 260) {
        $('.header').addClass('fixed-header');
      }
      else {
          $('.header').removeClass('fixed-header');
      }
    });
    // ========================= Header Sticky Js End===================

})(jQuery);

(function () {
  'use strict';

  function ready(callback) {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', callback);
    } else {
      callback();
    }
  }

  function toArray(value) {
    return Array.prototype.slice.call(value || []);
  }

  function dataKey(prefix, field) {
    if (!field) {
      return prefix;
    }
    return prefix + field.charAt(0).toUpperCase() + field.slice(1);
  }

  function opposite(direction) {
    return direction === 'asc' ? 'desc' : 'asc';
  }

  function directionForState(normalDirection, state) {
    if (state === 'normal') {
      return normalDirection || 'asc';
    }
    if (state === 'reverse') {
      return opposite(normalDirection || 'asc');
    }
    return null;
  }

  function stateForDirection(normalDirection, direction) {
    if (!direction) {
      return 'none';
    }
    return direction === (normalDirection || 'asc') ? 'normal' : 'reverse';
  }

  function parseComparable(value, type) {
    var text = String(value || '').trim();
    if (type === 'number') {
      var number = Number(text.replace(',', '.'));
      return Number.isFinite(number) ? number : 0;
    }
    if (type === 'date') {
      if (!text) {
        return 0;
      }
      if (/^-?\d+(\.\d+)?$/.test(text)) {
        return Number(text);
      }
      var parsed = Date.parse(text);
      return Number.isNaN(parsed) ? 0 : parsed;
    }
    return text;
  }

  function countLabel(count, singular) {
    var base = singular || 'item';
    return count === 1 ? '1 ' + base : count + ' ' + base + 's';
  }

  function fieldLabel(field) {
    var labels = {
      classGroup: 'Class Groups',
      course: 'Courses',
      organicUnit: 'Organic Unit',
      organization: 'Organization',
      subject: 'Subjects'
    };
    if (labels[field]) {
      return labels[field];
    }
    return String(field || 'group')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/^./, function (value) {
        return value.toUpperCase();
      });
  }

  function groupIcon(field) {
    var icons = {
      category: 'ph ph-tag',
      classGroup: 'ph ph-users-three',
      context: 'ph ph-path',
      course: 'ph ph-graduation-cap',
      organicUnit: 'ph ph-tree-structure',
      organization: 'ph ph-buildings',
      status: 'ph ph-circle-half',
      state: 'ph ph-circle-half',
      subject: 'ph ph-book-open',
      type: 'ph ph-tag'
    };
    return icons[field] || 'ph ph-stack';
  }

  function initSortGroup(root) {
    if (root.dataset.gapeSortGroupReady === 'true') {
      return;
    }

    var list = root.querySelector('[data-gape-sort-list]');
    if (!list) {
      return;
    }

    var sortOptions = toArray(root.querySelectorAll('[data-gape-sort-option]'));
    var groupOptions = toArray(root.querySelectorAll('[data-gape-group-option]'));
    if (!sortOptions.length && !groupOptions.length) {
      return;
    }
    root.dataset.gapeSortGroupReady = 'true';

    var isTable = list.tagName.toLowerCase() === 'tbody';
    var groupHiddenElements = toArray(root.querySelectorAll('[data-gape-group-hide-when-grouped]'));
    var rows = toArray(list.children).filter(function (child) {
      return child.hasAttribute('data-gape-sort-row');
    });
    var rowGroups = rows.map(function (row, index) {
      if (!row.dataset.sortIndex) {
        row.dataset.sortIndex = String(index);
      }
      return {
        row: row,
        detail: row.dataset.gapeDetailId ? document.getElementById(row.dataset.gapeDetailId) : null,
        index: index
      };
    });
    var hasGroupedOnlyRows = rowGroups.some(function (group) {
      return group.row.hasAttribute('data-gape-grouped-only');
    });
    var mobileList = root.querySelector('[data-gape-mobile-list]');
    var mobileGroupsById = new Map();
    if (mobileList) {
      toArray(mobileList.children)
        .filter(function (child) {
          return child.hasAttribute('data-gape-mobile-row');
        })
        .forEach(function (row) {
          var id = row.dataset.gapeMobileRowId;
          if (!id) {
            return;
          }
          mobileGroupsById.set(id, {
            row: row,
            detail: row.dataset.gapeMobileDetailId ? document.getElementById(row.dataset.gapeMobileDetailId) : null
          });
        });
    }

    var collator = new Intl.Collator(document.documentElement.lang || undefined, {
      numeric: true,
      sensitivity: 'base'
    });
    var activeSortField = null;
    var activeSortDirection = null;
    var activeSortType = null;
    var activeGroupField = null;
    var activeGroupDirection = null;
    var collapsedGroups = new Set();

    function originalIndex(group) {
      return Number(group.row.dataset.sortIndex || group.index || '0');
    }

    function sortValue(group, field) {
      return group.row.dataset[dataKey('sort', field)] || '';
    }

    function compareGroups(first, second) {
      if (!activeSortField || !activeSortDirection) {
        return originalIndex(first) - originalIndex(second);
      }

      var firstValue = sortValue(first, activeSortField);
      var secondValue = sortValue(second, activeSortField);
      var result;
      if (activeSortType === 'number' || activeSortType === 'date') {
        result = parseComparable(firstValue, activeSortType) - parseComparable(secondValue, activeSortType);
      } else {
        result = collator.compare(firstValue, secondValue);
      }
      if (result === 0) {
        result = originalIndex(first) - originalIndex(second);
      }
      return activeSortDirection === 'desc' ? -result : result;
    }

    function sortedGroups() {
      return currentRowGroups().slice().sort(compareGroups);
    }

    function currentRowGroups() {
      if (activeGroupField && activeGroupDirection && hasGroupedOnlyRows) {
        return rowGroups.filter(function (group) {
          return group.row.hasAttribute('data-gape-grouped-only');
        });
      }
      return rowGroups.filter(function (group) {
        return !group.row.hasAttribute('data-gape-grouped-only');
      });
    }

    function groupValue(group, field) {
      return group.row.dataset[dataKey('group', field)] || sortValue(group, field) || 'No ' + fieldLabel(field);
    }

    function groupKey(group, field) {
      return group.row.dataset[dataKey('group', field + 'Id')]
        || group.row.dataset[dataKey('group', field)]
        || groupValue(group, field);
    }

    function isContextGroupField(field) {
      return field === 'organization'
        || field === 'organicUnit'
        || field === 'course'
        || field === 'subject'
        || field === 'classGroup';
    }

    function contextFallback(field) {
      var fallbacks = {
        classGroup: 'No class group',
        course: 'No course',
        organicUnit: 'No organic unit',
        organization: 'Unknown organization',
        subject: 'No subject'
      };
      return fallbacks[field] || ('No ' + fieldLabel(field));
    }

    function contextValue(group, field) {
      return group.row.dataset[dataKey('group', field)] || sortValue(group, field) || contextFallback(field);
    }

    function contextKey(group, field) {
      return group.row.dataset[dataKey('group', field + 'Id')]
        || group.row.dataset[dataKey('group', field)]
        || contextValue(group, field);
    }

    function hasContextData(sorted, field) {
      return sorted.some(function (group) {
        return Boolean(
          group.row.dataset[dataKey('group', field + 'Id')]
          || group.row.dataset[dataKey('group', field)]
        );
      });
    }

    function hasContextValue(group, field) {
      return Boolean(
        group.row.dataset[dataKey('group', field + 'Id')]
        || group.row.dataset[dataKey('group', field)]
      );
    }

    function contextHierarchy(sorted) {
      var fieldsByGroup = {
        classGroup: ['classGroup'],
        course: ['course', 'subject', 'classGroup'],
        organicUnit: ['organicUnit', 'course', 'subject', 'classGroup'],
        organization: ['organization', 'organicUnit', 'course', 'subject', 'classGroup'],
        subject: ['subject', 'classGroup']
      };
      return (fieldsByGroup[activeGroupField] || [activeGroupField]).filter(function (field, index) {
        return index === 0 || hasContextData(sorted, field);
      });
    }

    function cssKind(field) {
      if (field === 'organicUnit') {
        return 'unit';
      }
      return String(field || 'group').replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
    }

    function compareGroupLabels(first, second) {
      var result = collator.compare(first.label, second.label);
      if (result === 0) {
        result = collator.compare(first.key, second.key);
      }
      return activeGroupDirection === 'desc' ? -result : result;
    }

    function appendBucketChild(parent, bucket) {
      parent.children.push(bucket);
      parent.childByKey.set(bucket.key, bucket);
      return bucket;
    }

    function ensureContextBucket(parent, field, group) {
      var key = field + '::' + contextKey(group, field);
      if (parent.childByKey.has(key)) {
        return parent.childByKey.get(key);
      }
      return appendBucketChild(parent, {
        childByKey: new Map(),
        children: [],
        field: field,
        itemCount: 0,
        items: [],
        key: key,
        label: contextValue(group, field)
      });
    }

    function addContextGroup(parent, fields, level, group) {
      var bucket = ensureContextBucket(parent, fields[level], group);
      bucket.itemCount += 1;
      if (level >= fields.length - 1 || !hasContextValue(group, fields[level + 1])) {
        bucket.items.push(group);
        return;
      }
      addContextGroup(bucket, fields, level + 1, group);
    }

    function contextBuckets(sorted) {
      var rootBucket = {
        childByKey: new Map(),
        children: []
      };
      var fields = contextHierarchy(sorted);
      sorted.forEach(function (group) {
        addContextGroup(rootBucket, fields, 0, group);
      });
      return {
        buckets: rootBucket.children,
        fields: fields
      };
    }

    function visibleColumnCount() {
      if (!isTable) {
        return 1;
      }
      var table = list.closest('table');
      if (!table) {
        return 1;
      }
      return toArray(table.querySelectorAll('thead th')).filter(function (column) {
        return !column.hidden;
      }).length || 1;
    }

    function removeHeaders() {
      toArray(list.querySelectorAll('[data-gape-dynamic-group-row]')).forEach(function (header) {
        header.remove();
      });
    }

    function detachGroups() {
      rowGroups.forEach(function (group) {
        group.row.remove();
        if (group.detail) {
          group.detail.remove();
        }
      });
    }

    function appendGroup(group, contextLevel) {
      if (contextLevel) {
        group.row.setAttribute('data-gape-dynamic-row-level', cssKind(contextLevel));
      } else {
        group.row.removeAttribute('data-gape-dynamic-row-level');
      }
      list.appendChild(group.row);
      if (group.detail) {
        var cell = group.detail.querySelector('td[colspan]');
        if (cell && isTable) {
          cell.colSpan = visibleColumnCount();
        }
        list.appendChild(group.detail);
      }
    }

    function appendMobile(sorted) {
      if (!mobileList || !mobileGroupsById.size) {
        return;
      }
      mobileGroupsById.forEach(function (mobileGroup) {
        mobileGroup.row.remove();
        if (mobileGroup.detail) {
          mobileGroup.detail.remove();
        }
      });
      sorted.forEach(function (group) {
        var mobileId = group.row.dataset.gapeMobileId;
        var mobileGroup = mobileId ? mobileGroupsById.get(mobileId) : null;
        if (!mobileGroup) {
          return;
        }
        mobileList.appendChild(mobileGroup.row);
        if (mobileGroup.detail) {
          mobileList.appendChild(mobileGroup.detail);
        }
      });
    }

    function createHeader(bucket) {
      var header = document.createElement(isTable ? 'tr' : 'div');
      header.className = 'gape-dynamic-group-row';
      header.setAttribute('data-gape-dynamic-group-row', '');

      var container = isTable ? document.createElement('td') : header;
      if (isTable) {
        container.colSpan = visibleColumnCount();
        container.className = 'px-20 py-12';
      }

      var heading = document.createElement('div');
      heading.className = 'gape-dynamic-group-heading';

      var title = document.createElement('div');
      title.className = 'gape-dynamic-group-title';

      var icon = document.createElement('i');
      icon.className = groupIcon(activeGroupField) + ' text-main-600 text-18';
      icon.setAttribute('aria-hidden', 'true');
      title.appendChild(icon);

      var text = document.createElement('span');
      text.className = 'gape-dynamic-group-title__text';
      text.textContent = bucket.label;
      title.appendChild(text);

      var chip = document.createElement('span');
      chip.className = 'gape-dynamic-group-chip';
      chip.textContent = fieldLabel(activeGroupField);
      title.appendChild(chip);

      var actions = document.createElement('div');
      actions.className = 'd-flex align-items-center gap-10 flex-shrink-0';

      var count = document.createElement('span');
      count.className = 'text-12 text-neutral-500';
      count.textContent = countLabel(bucket.items.length, root.dataset.gapeGroupItemLabel);
      actions.appendChild(count);

      var collapsedKey = activeGroupField + '::' + bucket.key;
      var expanded = !collapsedGroups.has(collapsedKey);
      var toggle = document.createElement('button');
      toggle.type = 'button';
      toggle.className = 'gape-dynamic-group-toggle';
      toggle.setAttribute('aria-expanded', String(expanded));
      toggle.setAttribute('aria-label', expanded ? 'Hide group items' : 'Show group items');
      toggle.setAttribute('title', expanded ? 'Hide group items' : 'Show group items');
      var toggleIcon = document.createElement('i');
      toggleIcon.className = expanded ? 'ph ph-caret-up' : 'ph ph-caret-down';
      toggleIcon.setAttribute('aria-hidden', 'true');
      toggle.appendChild(toggleIcon);
      toggle.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();
        if (collapsedGroups.has(collapsedKey)) {
          collapsedGroups.delete(collapsedKey);
        } else {
          collapsedGroups.add(collapsedKey);
        }
        render();
      });
      actions.appendChild(toggle);

      heading.appendChild(title);
      heading.appendChild(actions);
      container.appendChild(heading);
      if (isTable) {
        header.appendChild(container);
      }
      return header;
    }

    function createContextHeader(bucket, nestedMode) {
      var kind = cssKind(bucket.field);
      var header = document.createElement(isTable ? 'tr' : 'div');
      header.className = 'gape-dynamic-group-row gape-dynamic-group-row--' + kind;
      header.setAttribute('data-gape-dynamic-group-row', '');

      var container = isTable ? document.createElement('td') : header;
      if (isTable) {
        container.colSpan = visibleColumnCount();
        container.className = 'px-20 py-12';
      }

      var heading = document.createElement('div');
      heading.className = 'gape-dynamic-group-heading gape-dynamic-group-heading--' + kind;
      if (nestedMode && nestedMode !== bucket.field) {
        heading.classList.add('is-nested-under-' + cssKind(nestedMode));
      }

      var title = document.createElement('div');
      title.className = 'gape-dynamic-group-title gape-dynamic-group-title--' + kind;

      if (bucket.field === 'organization') {
        var icon = document.createElement('i');
        icon.className = groupIcon(bucket.field) + ' text-main-600 text-18';
        icon.setAttribute('aria-hidden', 'true');
        title.appendChild(icon);
      } else {
        var marker = document.createElement('span');
        marker.className = 'gape-dynamic-group-marker gape-dynamic-group-marker--' + kind;
        marker.setAttribute('aria-hidden', 'true');
        title.appendChild(marker);
      }

      var text = document.createElement('span');
      text.className = 'gape-dynamic-group-title__text';
      text.textContent = bucket.label;
      title.appendChild(text);

      if (bucket.field === 'organization' && bucket.children.length) {
        var childField = bucket.children[0].field;
        if (childField === 'organicUnit') {
          var unitCount = document.createElement('span');
          unitCount.className = 'gape-dynamic-group-chip';
          unitCount.textContent = countLabel(bucket.children.length, 'department');
          title.appendChild(unitCount);
        }
      }

      var actions = document.createElement('div');
      actions.className = 'd-flex align-items-center gap-10 flex-shrink-0';

      var count = document.createElement('span');
      count.className = 'text-12 text-neutral-500';
      count.textContent = countLabel(bucket.itemCount, root.dataset.gapeGroupItemLabel);
      actions.appendChild(count);

      var collapsedKey = bucket.key;
      var expanded = !collapsedGroups.has(collapsedKey);
      var toggle = document.createElement('button');
      toggle.type = 'button';
      toggle.className = 'gape-dynamic-group-toggle';
      toggle.setAttribute('aria-expanded', String(expanded));
      toggle.setAttribute('aria-label', expanded ? 'Hide group items' : 'Show group items');
      toggle.setAttribute('title', expanded ? 'Hide group items' : 'Show group items');
      var toggleIcon = document.createElement('i');
      toggleIcon.className = expanded ? 'ph ph-caret-up' : 'ph ph-caret-down';
      toggleIcon.setAttribute('aria-hidden', 'true');
      toggle.appendChild(toggleIcon);
      toggle.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();
        if (collapsedGroups.has(collapsedKey)) {
          collapsedGroups.delete(collapsedKey);
        } else {
          collapsedGroups.add(collapsedKey);
        }
        render();
      });
      actions.appendChild(toggle);

      heading.appendChild(title);
      heading.appendChild(actions);
      container.appendChild(heading);
      if (isTable) {
        header.appendChild(container);
      }
      return header;
    }

    function groupedBuckets(sorted) {
      var buckets = [];
      var byKey = new Map();
      sorted.forEach(function (group) {
        var key = groupKey(group, activeGroupField);
        if (!byKey.has(key)) {
          byKey.set(key, {
            key: key,
            label: groupValue(group, activeGroupField),
            items: []
          });
          buckets.push(byKey.get(key));
        }
        byKey.get(key).items.push(group);
      });
      buckets.sort(function (first, second) {
        var result = collator.compare(first.label, second.label);
        if (result === 0) {
          result = collator.compare(first.key, second.key);
        }
        return activeGroupDirection === 'desc' ? -result : result;
      });
      return buckets;
    }

    function renderContextBucket(bucket, nestedMode) {
      bucket.children.sort(compareGroupLabels);
      list.appendChild(createContextHeader(bucket, nestedMode));
      if (collapsedGroups.has(bucket.key)) {
        return;
      }
      bucket.items.forEach(function (group) {
        appendGroup(group, bucket.field);
      });
      if (bucket.children.length) {
        bucket.children.forEach(function (child) {
          renderContextBucket(child, activeGroupField);
        });
      }
    }

    function renderContextGroups(sorted) {
      var grouped = contextBuckets(sorted);
      grouped.buckets.sort(compareGroupLabels);
      grouped.buckets.forEach(function (bucket) {
        renderContextBucket(bucket, null);
      });
    }

    function updateSortOptions() {
      sortOptions.forEach(function (option) {
        var field = option.dataset.sortField;
        var normal = option.dataset.sortNormal || 'asc';
        var state = field === activeSortField
          ? stateForDirection(normal, activeSortDirection)
          : 'none';
        option.dataset.sortState = state;
        option.classList.toggle('is-active', state !== 'none');
        option.setAttribute('aria-pressed', state !== 'none' ? 'true' : 'false');
      });
      toArray(root.querySelectorAll('[data-gape-sort-toggle]')).forEach(function (toggle) {
        toggle.classList.toggle('is-active', Boolean(activeSortField));
      });
    }

    function updateGroupOptions() {
      groupOptions.forEach(function (option) {
        var field = option.dataset.groupField;
        var normal = option.dataset.groupNormal || 'asc';
        var state = field === activeGroupField
          ? stateForDirection(normal, activeGroupDirection)
          : 'none';
        option.dataset.groupState = state;
        option.classList.toggle('is-active', state !== 'none');
        option.setAttribute('aria-pressed', state !== 'none' ? 'true' : 'false');
      });
      toArray(root.querySelectorAll('[data-gape-group-toggle]')).forEach(function (toggle) {
        toggle.classList.toggle('is-active', Boolean(activeGroupField));
      });
    }

    function updateGroupedClasses() {
      var fields = ['organization', 'organicUnit', 'course', 'subject', 'classGroup'];
      var grouped = Boolean(activeGroupField);
      root.classList.toggle('gape-dynamic-grouped', grouped);
      fields.forEach(function (field) {
        root.classList.toggle('gape-dynamic-grouped-by-' + cssKind(field), activeGroupField === field);
      });
      groupHiddenElements.forEach(function (element) {
        element.hidden = grouped;
      });
    }

    function closeDropdown(toggle) {
      if (!toggle) {
        return;
      }
      if (window.bootstrap && window.bootstrap.Dropdown) {
        window.bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
      }
    }

    function render() {
      var sorted = sortedGroups();
      removeHeaders();
      detachGroups();
      updateGroupedClasses();
      if (activeGroupField && activeGroupDirection) {
        if (isContextGroupField(activeGroupField)) {
          renderContextGroups(sorted);
        } else {
          groupedBuckets(sorted).forEach(function (bucket) {
            list.appendChild(createHeader(bucket));
            if (!collapsedGroups.has(activeGroupField + '::' + bucket.key)) {
              bucket.items.forEach(appendGroup);
            }
          });
        }
      } else {
        sorted.forEach(appendGroup);
      }
      appendMobile(sorted);
      updateSortOptions();
      updateGroupOptions();
    }

    sortOptions.forEach(function (option) {
      option.addEventListener('click', function (event) {
        event.preventDefault();
        var field = option.dataset.sortField;
        var normal = option.dataset.sortNormal || 'asc';
        var currentState = field === activeSortField
          ? stateForDirection(normal, activeSortDirection)
          : 'none';
        var nextState = currentState === 'none' ? 'normal' : currentState === 'normal' ? 'reverse' : 'none';
        activeSortField = nextState === 'none' ? null : field;
        activeSortDirection = directionForState(normal, nextState);
        activeSortType = nextState === 'none' ? null : (option.dataset.sortType || null);
        render();
      });
    });

    toArray(root.querySelectorAll('[data-gape-sort-toggle]')).forEach(function (toggle) {
      toggle.addEventListener('click', function (event) {
        if (!activeSortField) {
          return;
        }
        event.preventDefault();
        event.stopPropagation();
        activeSortField = null;
        activeSortDirection = null;
        activeSortType = null;
        render();
        closeDropdown(toggle);
      });
    });

    toArray(root.querySelectorAll('[data-gape-group-toggle]')).forEach(function (toggle) {
      toggle.addEventListener('click', function (event) {
        if (!activeGroupField) {
          return;
        }
        event.preventDefault();
        event.stopPropagation();
        activeGroupField = null;
        activeGroupDirection = null;
        collapsedGroups.clear();
        render();
        closeDropdown(toggle);
      });
    });

    groupOptions.forEach(function (option) {
      option.addEventListener('click', function (event) {
        event.preventDefault();
        var field = option.dataset.groupField;
        var normal = option.dataset.groupNormal || 'asc';
        var currentState = field === activeGroupField
          ? stateForDirection(normal, activeGroupDirection)
          : 'none';
        var nextState = currentState === 'none' ? 'normal' : currentState === 'normal' ? 'reverse' : 'none';
        if (activeGroupField !== field || nextState === 'none') {
          collapsedGroups.clear();
        }
        activeGroupField = nextState === 'none' ? null : field;
        activeGroupDirection = directionForState(normal, nextState);
        render();
      });
    });

    render();
  }

  function initAllSortGroups() {
    toArray(document.querySelectorAll('[data-gape-sort-root]')).forEach(initSortGroup);
  }

  window.GapeSortGroupControls = {
    initAll: initAllSortGroups
  };

  ready(function () {
    initAllSortGroups();
  });

  window.addEventListener('pageshow', function () {
    initAllSortGroups();
  });
}());

(function () {
  'use strict';

  var DISPLAY_SELECTOR = '[data-gape-datetime-display]';
  var LISBON_TIME_ZONE = 'Europe/Lisbon';
  var dateTimeFormatter = typeof Intl !== 'undefined' && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat('pt-PT', {
      timeZone: LISBON_TIME_ZONE,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23'
    })
    : null;

  function pad(value) {
    return String(value).padStart(2, '0');
  }

  function isValidDate(year, month, day) {
    var date = new Date(year, month - 1, day);
    return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
  }

  function isValidTime(hour, minute, second) {
    return hour >= 0 && hour <= 23
      && minute >= 0 && minute <= 59
      && second >= 0 && second <= 59;
  }

  function displayDateTime(year, month, day, hour, minute, second) {
    if (!isValidDate(year, month, day) || !isValidTime(hour, minute, second)) {
      return '';
    }
    return pad(day) + '-' + pad(month) + '-' + year + ' ' + pad(hour) + '-' + pad(minute) + '-' + pad(second);
  }

  function lisbonParts(value) {
    if (!dateTimeFormatter || typeof dateTimeFormatter.formatToParts !== 'function') {
      return null;
    }
    var parts = {};
    dateTimeFormatter.formatToParts(value).forEach(function (part) {
      if (part.type !== 'literal') {
        parts[part.type] = part.value;
      }
    });
    return parts.year && parts.month && parts.day && parts.hour && parts.minute && parts.second ? parts : null;
  }

  function formatZonedIso(value) {
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    var parts = lisbonParts(date);
    if (!parts) {
      return value;
    }
    return displayDateTime(
      Number(parts.year),
      Number(parts.month),
      Number(parts.day),
      Number(parts.hour),
      Number(parts.minute),
      Number(parts.second)
    ) || value;
  }

  function formatDateTime(year, month, day, hour, minute, second, value) {
    return displayDateTime(
      Number(year),
      Number(month),
      Number(day),
      Number(hour),
      Number(minute),
      Number(second || 0)
    ) || value;
  }

  function formatDate(year, month, day, value) {
    if (!isValidDate(Number(year), Number(month), Number(day))) {
      return value;
    }
    return pad(day) + '-' + pad(month) + '-' + year;
  }

  function formatTime(hour, minute, second, value) {
    if (!isValidTime(Number(hour), Number(minute), Number(second || 0))) {
      return value;
    }
    return pad(hour) + '-' + pad(minute) + '-' + pad(second || 0);
  }

  function formatDisplayText(value) {
    if (typeof value !== 'string' || !value) {
      return value;
    }
    return value
      .replace(/\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(?:\.\d{1,3})?(?:Z|[+-]\d{2}:\d{2})\b/g, formatZonedIso)
      .replace(/\b(\d{4})-(\d{2})-(\d{2})(?:T|\s)(\d{2}):(\d{2})(?::(\d{2}))?\b/g, function (match, year, month, day, hour, minute, second) {
        return formatDateTime(year, month, day, hour, minute, second, match);
      })
      .replace(/\b(\d{1,2})[/.](\d{1,2})[/.](\d{4})(?:,?\s+)(\d{1,2}):(\d{2})(?::(\d{2}))?\b/g, function (match, day, month, year, hour, minute, second) {
        return formatDateTime(year, month, day, hour, minute, second, match);
      })
      .replace(/\b(\d{1,2})[/.](\d{1,2})[/.](\d{4})\b/g, function (match, day, month, year) {
        return formatDate(year, month, day, match);
      })
      .replace(/\b(\d{4})-(\d{2})-(\d{2})\b/g, function (match, year, month, day) {
        return formatDate(year, month, day, match);
      })
      .replace(/\b([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?\b/g, function (match, hour, minute, second) {
        return formatTime(hour, minute, second, match);
      });
  }

  function currentLisbonMinuteValue() {
    var now = new Date();
    var parts = lisbonParts(now);
    if (parts) {
      return parts.year + '-' + parts.month + '-' + parts.day + 'T' + parts.hour + ':' + parts.minute;
    }
    return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
      + 'T' + pad(now.getHours()) + ':' + pad(now.getMinutes());
  }

  function formatElement(element) {
    var walker = document.createTreeWalker(element, 4, null);
    var textNodes = [];
    var node;
    while ((node = walker.nextNode())) {
      textNodes.push(node);
    }
    textNodes.forEach(function (textNode) {
      var formatted = formatDisplayText(textNode.nodeValue);
      if (formatted !== textNode.nodeValue) {
        textNode.nodeValue = formatted;
      }
    });
  }

  function formatWithin(root) {
    if (!root) {
      return;
    }
    if (root.nodeType === 1 && root.matches && root.matches(DISPLAY_SELECTOR)) {
      formatElement(root);
    }
    if (!root.querySelectorAll) {
      return;
    }
    Array.prototype.forEach.call(root.querySelectorAll(DISPLAY_SELECTOR), formatElement);
  }

  function observeDisplayValues() {
    if (!window.MutationObserver || !document.body) {
      return;
    }
    new window.MutationObserver(function (records) {
      records.forEach(function (record) {
        Array.prototype.forEach.call(record.addedNodes, function (node) {
          if (node.nodeType === 1 || node.nodeType === 9 || node.nodeType === 11) {
            formatWithin(node);
          }
        });
      });
    }).observe(document.body, {childList: true, subtree: true});
  }

  function initialize() {
    formatWithin(document);
    observeDisplayValues();
  }

  window.GapeDateTimeFormat = {
    timeZone: LISBON_TIME_ZONE,
    currentLisbonMinuteValue: currentLisbonMinuteValue,
    formatDisplayText: formatDisplayText,
    formatTechnicalDateTime: formatDisplayText,
    formatWithin: formatWithin
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
  } else {
    initialize();
  }

  window.addEventListener('pageshow', function () {
    formatWithin(document);
  });
}());
