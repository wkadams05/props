package com.yahoo.props;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;

public class PropDefinerBuilder<CONTEXT> {
    @FunctionalInterface
    public interface TypeGetter<CONTEXT, TYPE> {
        TYPE getFrom(CONTEXT context, String name);
    }
    
    @FunctionalInterface
    public interface TypeSetter<CONTEXT, TYPE> {
        void setTo(CONTEXT context, String name, TYPE value);
    }
    
    @FunctionalInterface
    public interface EventHandler<CONTEXT> {
        void onEvent(CONTEXT context, String name, Object value);
    }
    
    public static <CONTEXT> PropDefinerBuilder<CONTEXT> newBuilder(Class<CONTEXT> contextClass) {
        return new PropDefinerBuilder<CONTEXT>();
    }
    
    private Map<String, TypeGetter<CONTEXT, ?>> typeGetters = new HashMap<>();
    private Map<String, TypeSetter<CONTEXT, ?>> typeSetters = new HashMap<>();
    private EventHandler<CONTEXT>               afterInitEventHandler;
    private EventHandler<CONTEXT>               afterGetEventHandler;
    private EventHandler<CONTEXT>               afterSetEventHandler;
                                                
    private PropDefinerBuilder() {
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectGetter(TypeGetter<CONTEXT, Object> objectGetter) {
        typeGetters.put(Object.class.getName(), objectGetter);
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectSetter(TypeSetter<CONTEXT, Object> objectSetter) {
        typeSetters.put(Object.class.getName(), objectSetter);
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterInitEventHandler(EventHandler<CONTEXT> eventHandler) {
        this.afterInitEventHandler = eventHandler;
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterGetEventHandler(EventHandler<CONTEXT> eventHandler) {
        this.afterGetEventHandler = eventHandler;
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterSetEventHandler(EventHandler<CONTEXT> eventHandler) {
        this.afterSetEventHandler = eventHandler;
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> addTypeGetter(Class<TYPE> typeClass, TypeGetter<CONTEXT, TYPE> getter) {
        typeGetters.put(typeClass.getName(), getter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> addTypeSetter(Class<TYPE> typeClass, TypeSetter<CONTEXT, TYPE> setter) {
        typeSetters.put(typeClass.getName(), setter);
        return this;
    }
    
    public PropDefiner<CONTEXT> build() {
        return new PropDefiner<CONTEXT>() {
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass) {
                return define(name, typeClass, null);
            }
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass,
                    Function<CONTEXT, TYPE> defaultValueInitializer) {
                return new PropImpl<CONTEXT, TYPE>(name, resolveTypeGetter(typeClass), resolveTypeSetter(typeClass),
                        afterInitEventHandler, afterGetEventHandler, afterSetEventHandler, defaultValueInitializer);
            }
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken) {
                return define(name, typeToken, null);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken,
                    Function<CONTEXT, TYPE> defaultValueInitializer) {
                if (typeToken.getType() instanceof ParameterizedType) {
                    return new PropImpl<CONTEXT, TYPE>(name, resolveTypeGetter(null), resolveTypeSetter(null),
                            afterInitEventHandler, afterGetEventHandler, afterSetEventHandler, defaultValueInitializer);
                } else {
                    return define(name, (Class<TYPE>) typeToken.getType());
                }
            }
            
            @SuppressWarnings("unchecked")
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(Class<TYPE> typeClass) {
                TypeGetter<CONTEXT, TYPE> typeGetter = null;
                if (typeClass != null) {
                    typeGetter = (TypeGetter<CONTEXT, TYPE>) typeGetters.get(typeClass.getName());
                }
                if (typeClass == null || typeGetter == null) {
                    TypeGetter<CONTEXT, Object> objectGetter = (TypeGetter<CONTEXT, Object>) typeGetters
                            .get(Object.class.getName());
                    typeGetter = (context, name) -> (TYPE) objectGetter.getFrom(context, name);
                }
                return typeGetter;
            }
            
            @SuppressWarnings("unchecked")
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(Class<TYPE> typeClass) {
                TypeSetter<CONTEXT, TYPE> typeSetter = null;
                if (typeClass != null) {
                    typeSetter = (TypeSetter<CONTEXT, TYPE>) typeSetters.get(typeClass.getName());
                }
                if (typeClass == null || typeSetter == null) {
                    TypeSetter<CONTEXT, Object> objectSetter = (TypeSetter<CONTEXT, Object>) typeSetters
                            .get(Object.class.getName());
                    typeSetter = (context, name, value) -> objectSetter.setTo(context, name, value);
                }
                return typeSetter;
            }
        };
    }
}
