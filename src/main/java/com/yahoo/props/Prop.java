package com.yahoo.props;

import java.util.Optional;
import java.util.function.Function;

import static com.yahoo.props.PropImpl.RESET_DEPENDENCY_KEY_PREFIX;

public interface Prop<CONTEXT, TYPE> {
    static boolean isResetDependencyKey(String key) {
        return key != null && key.startsWith(RESET_DEPENDENCY_KEY_PREFIX);
    }

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

    Prop<CONTEXT, TYPE> addResetDependency(Function<CONTEXT, Object> dependencyAccess);

    default Prop<CONTEXT, TYPE> addResetDependency(Prop<CONTEXT, ?> propDependency) {
        return addResetDependency(context -> propDependency.getFrom(context));
    }
}
