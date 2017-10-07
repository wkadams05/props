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
    private List<Function<CONTEXT, Object>> dependencyAccessList;

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
        updateDependencyHashes(context, value);
    }

    private void updateDependencyHashes(CONTEXT context, TYPE value) {
        if (hasDependencies()) {
            for (int i = 0; i < dependencyAccessList.size(); i++) {
                Object dependency = dependencyAccessList.get(i).apply(context);
                if (value == null) {
                    clearDependencyHash(context, i);
                } else {
                    storeDependencyHash(context, i, dependency);
                }
            }
        }
    }

    private void storeDependencyHash(CONTEXT context, int no, Object nullableTarget) {
        internalSetter.setTo(context, dependencyKey(no), dependencyHash(nullableTarget));
    }

    private void clearDependencyHash(CONTEXT context, int no) {
        internalSetter.setTo(context, dependencyKey(no), null);
    }

    private Optional<String> readDependencyHash(CONTEXT context, int no) {
        return Optional.of(internalGetter.getFrom(context, dependencyKey(no)));
    }

    private String dependencyKey(int no) {
        return String.format("%s-DEPENDENCY#%d", name, no);
    }

    private String dependencyHash(Object nullableTarget) {
        return (nullableTarget == null) ? "NULL" : String.valueOf(nullableTarget.hashCode());
    }

    private boolean isNotNull(TYPE valueNullable) {
        return valueNullable != null;
    }

    private boolean hasDependencies() {
        return dependencyAccessList != null && dependencyAccessList.size() > 0;
    }

    private boolean hasDefaultInitializer() {
        return defaultInitializer != null;
    }


    private boolean hasAnyDependencyChanged(CONTEXT context) {
        for (int no = 0; no < dependencyAccessList.size(); no++) {
            Object target = dependencyAccessList.get(no).apply(context);
            String newHash = dependencyHash(target);
            Optional<String> oldHash = readDependencyHash(context, no);
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
                && hasDependencies()
                && hasAnyDependencyChanged(context)) {
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
    public Prop<CONTEXT, TYPE> addResetDependency(Function<CONTEXT, Object> dependencyAccess) {
        if (dependencyAccessList == null) {
            dependencyAccessList = new ArrayList<>();
        }
        dependencyAccessList.add(dependencyAccess);
        return this;
    }
}
