package pt.isel.gape.web.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

class LessonStateSynchronizationListenerTest {

    @Test
    void catchesOneSynchronizationFailureWithoutStoppingTheCaller() {
        List<String> logMessages = new ArrayList<>();
        ServletContext context = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("log".equals(method.getName()) && args != null && args.length > 0) {
                        logMessages.add(String.valueOf(args[0]));
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        ServletContextEvent event = new ServletContextEvent(context);
        int[] completed = {0};

        LessonStateSynchronizationListener.runSafely(
                "class groups",
                () -> { throw new IllegalStateException("database unavailable"); },
                event
        );
        LessonStateSynchronizationListener.runSafely("lessons", () -> completed[0]++, event);

        assertEquals(1, completed[0]);
        assertTrue(logMessages.get(0).contains("class groups"));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
