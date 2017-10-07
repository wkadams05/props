package com.yahoo.props;

import com.yahoo.props.PropDefinerBuilder.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.yahoo.props.Utils.nonNullMessage;
import static java.util.Objects.requireNonNull;

class PropImpl<CONTEXT, TYPE> implements Prop<CONTEXT, TYPE> {
    private String                          name;
    private Type                            type;
    private Function<CONTEXT, TYPE>         defaultInitializer;
    private TypeGetter<CONTEXT, TYPE>       typeGetter;
    private TypeSetter<CONTEXT, TYPE>       typeSetter;
    private TypeGetter<CONTEXT, String>     internalGetter;
    private TypeSetter<CONTEXT, String>     internalSetter;
    private EventHandler<CONTEXT>           afterInitEventHandler;
    private EventHandler<CONTEXT>           afterGetEventHandler;
    private EventHandler<CONTEXT>           afterSetEventHandler;
    private List<Function<CONTEXT, Object>> targetMonitors;

    PropImpl(String name,
             Type type,
             TypeGetter<CONTEXT, TYPE> typeGetter,
             TypeSetter<CONTEXT, TYPE> typeSetter,
             TypeGetter<CONTEXT, String> internalGetter,
             TypeSetter<CONTEXT, String> internalSetter,
             EventHandler<CONTEXT> afterInitEventHandler,
             EventHandler<CONTEXT> afterGetEventHandler,
             EventHandler<CONTEXT> afterSetEventHandler,
             Function<CONTEXT, TYPE> defaultInitializer) {
        this.name = name;
        this.type = type;
        this.typeGetter = typeGetter;
        this.typeSetter = typeSetter;
        this.internalGetter = internalGetter;
        this.internalSetter = internalSetter;
        this.afterInitEventHandler = afterInitEventHandler;
        this.afterGetEventHandler = afterGetEventHandler;
        this.afterSetEventHandler = afterSetEventHandler;
        this.defaultInitializer = defaultInitializer;
    }

    @Override
    public String getName() {
        return name;
    }

    private TYPE callTypeGetter(CONTEXT context) {
        if (typeGetter instanceof ComprehensiveTypeGetter) {
            return ((ComprehensiveTypeGetter<CONTEXT, TYPE>) typeGetter).getFrom(context, type, name);
        } else {
            return typeGetter.getFrom(context, name);
        }
    }

    private void callTypeSetter(CONTEXT context, TYPE value) {
        if (typeSetter instanceof ComprehensiveTypeSetter) {
            ((ComprehensiveTypeSetter<CONTEXT, TYPE>) typeSetter).setTo(context, type, name, value);
        } else {
            typeSetter.setTo(context, name, value);
        }
        updateTargetHashes(context, value);
    }

    private void updateTargetHashes(CONTEXT context, TYPE value) {
        if (hasTargetMonitors()) {
            for (int i = 0; i < targetMonitors.size(); i++) {
                Object target = targetMonitors.get(i).apply(context);
                if (value == null) {
                    clearTargetHash(context, i);
                } else {
                    storeTargetHash(context, i, target);
                }
            }
        }
    }

    private void storeTargetHash(CONTEXT context, int no, Object nullableTarget) {
        internalSetter.setTo(context, targetKey(no), targetHash(nullableTarget));
    }

    private void clearTargetHash(CONTEXT context, int no) {
        internalSetter.setTo(context, targetKey(no), null);
    }

    private Optional<String> readTargetHash(CONTEXT context, int no) {
        return Optional.of(internalGetter.getFrom(context, targetKey(no)));
    }

    private String targetKey(int no) {
        return String.format("%s-TARGET_MONITOR#%d", name, no);
    }

    private String targetHash(Object nullableTarget) {
        return (nullableTarget == null) ? "NULL" : String.valueOf(nullableTarget.hashCode());
    }

    private boolean isNotNull(TYPE valueNullable) {
        return valueNullable != null;
    }

    private boolean hasTargetMonitors() {
        return targetMonitors != null && targetMonitors.size() > 0;
    }

    private boolean hasDefaultInitializer() {
        return defaultInitializer != null;
    }


    private boolean isAnyMonitoringTargetChanged(CONTEXT context) {
        for (int no = 0; no < targetMonitors.size(); no++) {
            Object target = targetMonitors.get(no).apply(context);
            String newHash = targetHash(target);
            Optional<String> oldHash = readTargetHash(context, no);
            if (oldHash.isPresent() && !oldHash.get().equals(newHash)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TYPE getFrom(CONTEXT context, TYPE substIfNull) {

        requireNonNull(context, nonNullMessage("context"));

        TYPE value = callTypeGetter(context);

        if (isNotNull(value)
                && hasDefaultInitializer()
                && hasTargetMonitors()
                && isAnyMonitoringTargetChanged(context)) {
            value = null;
            setTo(context, null);
        }

        if (value == null) {
            if (defaultInitializer != null) {
                value = defaultInitializer.apply(context);
                // initialize
                callTypeSetter(context, value);
                if (afterInitEventHandler != null) {
                    afterInitEventHandler.onEvent(context, name, value);
                }
            } else {
                value = substIfNull;
            }
        }
        if (afterGetEventHandler != null) {
            afterGetEventHandler.onEvent(context, name, value);
        }
        return value;
    }

    @Override
    public void setTo(CONTEXT context, TYPE value) {
        callTypeSetter(context, value);
        if (afterSetEventHandler != null) {
            afterSetEventHandler.onEvent(context, name, value);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Prop) {
            @SuppressWarnings("unchecked")
            Prop<CONTEXT, TYPE> other = (Prop<CONTEXT, TYPE>) o;
            return Objects.equals(this.getName(), other.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public Optional<Function<CONTEXT, TYPE>> getDefaultInitializer() {
        return Optional.ofNullable(defaultInitializer);
    }

    @Override
    public void overrideDefaultInitializer(Function<CONTEXT, TYPE> defaultInitializer) {
        this.defaultInitializer = defaultInitializer;
    }

    @Override
    public Prop<CONTEXT, TYPE> addTargetMonitorForReset(Function<CONTEXT, Object> targetMonitor) {
        if (targetMonitors == null) {
            targetMonitors = new ArrayList<>();
        }
        targetMonitors.add(targetMonitor);
        return this;
    }
}
