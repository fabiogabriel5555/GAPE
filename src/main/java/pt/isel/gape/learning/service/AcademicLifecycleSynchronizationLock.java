package pt.isel.gape.learning.service;

/**
 * Coordinates the application's two ways of refreshing temporal academic
 * state: the background scheduler and an explicit, on-demand refresh.
 *
 * <p>The refresh touches the same lifecycle tables in a fixed order.  Keeping
 * those two entry points in one JVM-level critical section prevents an
 * otherwise harmless page read from racing the scheduler and producing a
 * database deadlock.</p>
 */
final class AcademicLifecycleSynchronizationLock {

    private static final Object MONITOR = new Object();

    private AcademicLifecycleSynchronizationLock() {
    }

    static Object monitor() {
        return MONITOR;
    }
}
