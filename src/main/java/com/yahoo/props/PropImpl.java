package com.yahoo.props;

import java.util.Objects;
import java.util.function.Function;

import com.yahoo.props.PropDefinerBuilder.EventHandler;
import com.yahoo.props.PropDefinerBuilder.TypeGetter;
import com.yahoo.props.PropDefinerBuilder.TypeSetter;

class PropImpl<CONTEXT, TYPE> implements Prop<CONTEXT, TYPE> {
    private String                    name;
    private Function<CONTEXT, TYPE>   defaultValueInitializer;
    private TypeGetter<CONTEXT, TYPE> typeGetter;
    private TypeSetter<CONTEXT, TYPE> typeSetter;
    private EventHandler<CONTEXT>     afterInitEventHandler;
    private EventHandler<CONTEXT>     afterGetEventHandler;
    private EventHandler<CONTEXT>     afterSetEventHandler;
                                      
    PropImpl(String name, TypeGetter<CONTEXT, TYPE> typeGetter, TypeSetter<CONTEXT, TYPE> typeSetter,
            EventHandler<CONTEXT> afterInitEventHandler, EventHandler<CONTEXT> afterGetEventHandler,
            EventHandler<CONTEXT> afterSetEventHandler, Function<CONTEXT, TYPE> defaultValueInitializer) {
        this.name = name;
        this.typeGetter = typeGetter;
        this.typeSetter = typeSetter;
        this.afterInitEventHandler = afterInitEventHandler;
        this.afterGetEventHandler = afterGetEventHandler;
        this.afterSetEventHandler = afterSetEventHandler;
        this.defaultValueInitializer = defaultValueInitializer;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public TYPE getFrom(CONTEXT context) {
        return getFrom(context, null);
    }
    
    @Override
    public TYPE getFrom(CONTEXT context, TYPE substIfNull) {
        TYPE value = typeGetter.getFrom(context, name);
        if (value == null) {
            if (defaultValueInitializer != null) {
                value = defaultValueInitializer.apply(context);
                // initialize
                typeSetter.setTo(context, name, value);
                afterInitEventHandler.onEvent(context, name, value);
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
        typeSetter.setTo(context, name, value);
        if (afterSetEventHandler != null) {
            afterSetEventHandler.onEvent(context, name, value);
        }
    }
    
    @Override
    public void setToIfNull(CONTEXT context, TYPE value) {
        if (getFrom(context) == null) {
            setTo(context, value);
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
}
