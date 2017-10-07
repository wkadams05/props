package com.yahoo.props;

import java.util.Optional;
import java.util.function.Function;

public interface Prop<CONTEXT, TYPE> {
    String getName();

    default TYPE getFrom(CONTEXT context) {
        return getFrom(context, null);
    }

    TYPE getFrom(CONTEXT context, TYPE substIfNull);

    void setTo(CONTEXT context, TYPE value);

    default void setToIfAbsent(CONTEXT context, TYPE value) {
        if (isAbsent(context))
            setTo(context, value);
    }

    default void setToIfPresent(CONTEXT context, TYPE value) {
        if (isPresent(context))
            setTo(context, value);
    }

    default boolean isAbsent(CONTEXT context) {
        return getFrom(context) == null;
    }

    default boolean isPresent(CONTEXT context) {
        return !isAbsent(context);
    }

    Optional<Function<CONTEXT, TYPE>> getDefaultInitializer();

    void overrideDefaultInitializer(Function<CONTEXT, TYPE> defaultInitializer);

    /**
     * <p>
     * Adds target monitor (access to monitoring target object). If any of monitoring targets change and defaultInitializer exists,
     * the value gets reset to be re-initialized.
     * </p>
     * <p>
     * This takes no effect unless defaultInitializer is set.
     * </p>
     *
     * @param targetMonitor Access function to monitoring target object
     * @return Target object to monitor
     */
    Prop<CONTEXT, TYPE> addTargetMonitorForReset(Function<CONTEXT, Object> targetMonitor);
}
