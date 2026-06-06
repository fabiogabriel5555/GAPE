package pt.isel.gape.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.isel.gape.access.model.AccessProfileType;

class SessionUserTest {

    @Test
    void normalizesUploadPrefixFromStoredPhoto() {
        SessionUser sessionUser = new SessionUser(
                4L,
                "Student User",
                "student@gape.local",
                "uploads\\users\\4\\profile.png",
                Set.of(AccessProfileType.STUDENT)
        );

        assertEquals("users/4/profile.png", sessionUser.photo());
    }

    @Test
    void normalizesMediaPrefixAndQueryStringFromStoredPhoto() {
        SessionUser sessionUser = new SessionUser(
                4L,
                "Student User",
                "student@gape.local",
                "/media/users/4/profile.webp?v=1",
                Set.of(AccessProfileType.STUDENT)
        );

        assertEquals("users/4/profile.webp", sessionUser.photo());
    }

    @Test
    void normalizesAbsoluteUploadPathFromStoredPhoto() {
        SessionUser sessionUser = new SessionUser(
                4L,
                "Student User",
                "student@gape.local",
                "C:/project/GAPE/uploads/users/4/profile.jpg",
                Set.of(AccessProfileType.STUDENT)
        );

        assertEquals("users/4/profile.jpg", sessionUser.photo());
    }

    @Test
    void keepsTemplateAssetPhotoPathRelativeToWebapp() {
        SessionUser sessionUser = new SessionUser(
                1L,
                "Admin User",
                "admin@gape.local",
                "assets/images/thumbs/testimonials-three-img1.png",
                Set.of(AccessProfileType.ADMINISTRATOR)
        );

        assertEquals("assets/images/thumbs/testimonials-three-img1.png", sessionUser.photo());
    }

    @Test
    void normalizesAbsoluteTemplateAssetPhotoPath() {
        SessionUser sessionUser = new SessionUser(
                1L,
                "Admin User",
                "admin@gape.local",
                "C:/project/GAPE/src/main/webapp/assets/images/thumbs/testimonials-three-img1.png",
                Set.of(AccessProfileType.ADMINISTRATOR)
        );

        assertEquals("assets/images/thumbs/testimonials-three-img1.png", sessionUser.photo());
    }

    @Test
    void rejectsUnsafeStoredPhotoPath() {
        SessionUser sessionUser = new SessionUser(
                4L,
                "Student User",
                "student@gape.local",
                "../private/profile.png",
                Set.of(AccessProfileType.STUDENT)
        );

        assertFalse(sessionUser.hasPhoto());
    }
}
