package pt.isel.gape.web.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.SessionCookieConfig;

class SecurityConfigurationListenerTest {

    @Test
    void configuresHttpOnlyAndSameSiteSessionCookiesAtStartup() {
        CookieConfigurationState state = new CookieConfigurationState();
        SessionCookieConfig cookieConfig = cookieConfigProxy(state);
        ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> "getSessionCookieConfig".equals(method.getName())
                        ? cookieConfig
                        : defaultValue(method.getReturnType())
        );

        new SecurityConfigurationListener().contextInitialized(new ServletContextEvent(servletContext));

        assertTrue(state.httpOnly);
        assertEquals("Lax", state.attributes.get("SameSite"));
    }

    private static SessionCookieConfig cookieConfigProxy(CookieConfigurationState state) {
        return (SessionCookieConfig) Proxy.newProxyInstance(
                SessionCookieConfig.class.getClassLoader(),
                new Class<?>[]{SessionCookieConfig.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setHttpOnly" -> {
                        state.httpOnly = (Boolean) args[0];
                        yield null;
                    }
                    case "setAttribute" -> {
                        state.attributes.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "setSecure" -> {
                        state.secure = (Boolean) args[0];
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        return null;
    }

    private static final class CookieConfigurationState {
        private final Map<String, String> attributes = new HashMap<>();
        private boolean httpOnly;
        private boolean secure;
    }
}
