package com.yahoo.props;

import static com.yahoo.props.Utils.nonNullMessage;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.yahoo.props.PropDefinerBuilder.EventHandler;
import com.yahoo.props.PropDefinerBuilder.TypeGetter;
import com.yahoo.props.PropDefinerBuilder.TypeSetter;
import com.yahoo.props.PropDefinerBuilder.ComprehensiveTypeGetter;
import com.yahoo.props.PropDefinerBuilder.ComprehensiveTypeSetter;

class PropImpl<CONTEXT, TYPE> implements Prop<CONTEXT, TYPE> {
    private String                    name;
    private Type                      type;
    private Function<CONTEXT, TYPE>   defaultInitializer;
    private TypeGetter<CONTEXT, TYPE> typeGetter;
    private TypeSetter<CONTEXT, TYPE> typeSetter;
    private EventHandler<CONTEXT>     afterInitEventHandler;
    private EventHandler<CONTEXT>     afterGetEventHandler;
    private EventHandler<CONTEXT>     afterSetEventHandler;
    
    PropImpl(String name, Type type, TypeGetter<CONTEXT, TYPE> typeGetter, TypeSetter<CONTEXT, TYPE> typeSetter,
            EventHandler<CONTEXT> afterInitEventHandler, EventHandler<CONTEXT> afterGetEventHandler,
            EventHandler<CONTEXT> afterSetEventHandler, Function<CONTEXT, TYPE> defaultInitializer) {
        this.name = name;
        this.type = type;
        this.typeGetter = typeGetter;
        this.typeSetter = typeSetter;
        this.afterInitEventHandler = afterInitEventHandler;
        this.afterGetEventHandler = afterGetEventHandler;
        this.afterSetEventHandler = afterSetEventHandler;
        this.defaultInitializer = defaultInitializer;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public TYPE getFrom(CONTEXT context) {
        return getFrom(context, null);
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
    }
    
    @Override
    public TYPE getFrom(CONTEXT context, TYPE substIfNull) {
        
        requireNonNull(context, nonNullMessage("context"));
        
        TYPE value = callTypeGetter(context);
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
    public void setToIfAbsent(CONTEXT context, TYPE value) {
        if (isAbsent(context)) {
            setTo(context, value);
        }
    }
    
    @Override
    public void setToIfPresent(CONTEXT context, TYPE value) {
        if (isPresent(context)) {
            setTo(context, value);
        }
    }
    
    @Override
    public boolean isAbsent(CONTEXT context) {
        return getFrom(context) == null;
    }
    
    @Override
    public boolean isPresent(CONTEXT context) {
        return !isAbsent(context);
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
}
