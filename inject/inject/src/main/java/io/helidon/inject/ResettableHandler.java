package io.helidon.inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Support for resetting (mostly from testing).
 */
public class ResettableHandler {
    private static final Lock RESETTABLES_LOCK = new ReentrantLock();
    private static final List<Resettable> RESETTABLES = new ArrayList<>();

    /**
     * Resets the bootstrap state.
     */
    protected static void reset() {
        try {
            RESETTABLES_LOCK.lock();
            RESETTABLES.forEach(it -> it.reset(true));
            RESETTABLES.clear();
        } finally {
            RESETTABLES_LOCK.unlock();
        }
    }

    /**
     * Register a resettable instance. When {@link #reset()} is called, this instance is removed from the list.
     *
     * @param instance resettable type that can be reset during testing
     */
    protected static void addResettable(Resettable instance) {
        try {
            RESETTABLES_LOCK.lock();
            RESETTABLES.add(instance);
        } finally {
            RESETTABLES_LOCK.unlock();
        }
    }
}
