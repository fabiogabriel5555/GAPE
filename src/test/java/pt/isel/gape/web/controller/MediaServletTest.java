package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class MediaServletTest {

    private Path uploadRoot;
    private Path webappRoot;

    @BeforeEach
    void setUp() throws Exception {
        Path testRoot = Files.createDirectories(Path.of("target", "media-servlet-test"));
        uploadRoot = Files.createTempDirectory(testRoot, "uploads-");
        webappRoot = Files.createTempDirectory(testRoot, "webapp-");
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteDirectory(uploadRoot);
        deleteDirectory(webappRoot);
    }

    @Test
    void servesImageWithAvailableExtensionWhenStoredPathExtensionChanged() throws Exception {
        Path userDirectory = Files.createDirectories(uploadRoot.resolve("users/1"));
        byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        Files.write(userDirectory.resolve("profile.jpg"), imageBytes);

        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("/users/1/profile.webp"),
                responseProxy(responseState)
        );

        assertEquals(0, responseState.errorStatus);
        assertEquals("image/jpeg", responseState.contentType);
        assertEquals(imageBytes.length, responseState.contentLength);
        assertArrayEquals(imageBytes, responseState.body.toByteArray());
    }

    @Test
    void servesImageWhenRequestStillContainsUploadsPrefix() throws Exception {
        Path userDirectory = Files.createDirectories(uploadRoot.resolve("users/4"));
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        Files.write(userDirectory.resolve("profile.png"), imageBytes);

        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("/uploads/users/4/profile.webp"),
                responseProxy(responseState)
        );

        assertEquals(0, responseState.errorStatus);
        assertEquals("image/png", responseState.contentType);
        assertArrayEquals(imageBytes, responseState.body.toByteArray());
    }

    @Test
    void servesImageWhenRequestStillContainsMediaPrefix() throws Exception {
        Path userDirectory = Files.createDirectories(uploadRoot.resolve("users/4"));
        byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        Files.write(userDirectory.resolve("profile.jpeg"), imageBytes);

        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("//media/users/4/profile.webp"),
                responseProxy(responseState)
        );

        assertEquals(0, responseState.errorStatus);
        assertEquals("image/jpeg", responseState.contentType);
        assertArrayEquals(imageBytes, responseState.body.toByteArray());
    }

    @Test
    void servesTemplateAssetImageWhenPhotoUsesOriginalEduallPath() throws Exception {
        Path assetDirectory = Files.createDirectories(webappRoot.resolve("assets/images/thumbs"));
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        Files.write(assetDirectory.resolve("testimonials-three-img1.png"), imageBytes);

        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("/assets/images/thumbs/testimonials-three-img1.webp"),
                responseProxy(responseState)
        );

        assertEquals(0, responseState.errorStatus);
        assertEquals("image/png", responseState.contentType);
        assertArrayEquals(imageBytes, responseState.body.toByteArray());
    }

    @Test
    void servesTemplateAssetImageWhenRequestContainsAbsoluteWebappPrefix() throws Exception {
        Path assetDirectory = Files.createDirectories(webappRoot.resolve("assets/images/thumbs"));
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        Files.write(assetDirectory.resolve("testimonials-three-img1.png"), imageBytes);

        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("/C:/project/GAPE/src/main/webapp/assets/images/thumbs/testimonials-three-img1.webp"),
                responseProxy(responseState)
        );

        assertEquals(0, responseState.errorStatus);
        assertEquals("image/png", responseState.contentType);
        assertArrayEquals(imageBytes, responseState.body.toByteArray());
    }

    @Test
    void keepsReturningNotFoundWhenNoEquivalentImageExists() throws Exception {
        MediaServlet servlet = new MediaServlet(uploadRoot, webappRoot);
        servlet.init(servletConfig());

        TestHttpServletResponse responseState = new TestHttpServletResponse();
        servlet.doGet(
                requestProxy("/users/1/profile.webp"),
                responseProxy(responseState)
        );

        assertEquals(HttpServletResponse.SC_NOT_FOUND, responseState.errorStatus);
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (directory != null && Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to delete " + path, exception);
                            }
                        });
            }
        }
    }

    private ServletConfig servletConfig() {
        return (ServletConfig) Proxy.newProxyInstance(
                ServletConfig.class.getClassLoader(),
                new Class<?>[]{ServletConfig.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServletContext" -> servletContext();
                    case "getServletName" -> "mediaServlet";
                    default -> null;
                }
        );
    }

    private ServletContext servletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMimeType" -> mimeType((String) args[0]);
                    case "getRealPath" -> webappRoot.toString();
                    default -> null;
                }
        );
    }

    private HttpServletRequest requestProxy(String pathInfo) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> pathInfo;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private HttpServletResponse responseProxy(TestHttpServletResponse responseState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendError" -> {
                        responseState.errorStatus = (Integer) args[0];
                        yield null;
                    }
                    case "setContentType" -> {
                        responseState.contentType = (String) args[0];
                        yield null;
                    }
                    case "setContentLengthLong" -> {
                        responseState.contentLength = (Long) args[0];
                        yield null;
                    }
                    case "setHeader" -> null;
                    case "getOutputStream" -> responseState.outputStream();
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static String mimeType(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static final class TestHttpServletResponse {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private int errorStatus;
        private String contentType;
        private long contentLength;

        private ServletOutputStream outputStream() {
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int value) throws IOException {
                    body.write(value);
                }
            };
        }
    }
}
