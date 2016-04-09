package com.yahoo.props;

import static com.yahoo.props.Utils.nonNullMessage;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;

public class PropDefinerBuilder<CONTEXT> {
    private static final String OBJECT_CLASS_TYPE_NAME = Object.class.getTypeName();
    
    public static <CONTEXT> PropDefinerBuilder<CONTEXT> newBuilder(Class<CONTEXT> contextClass) {
        return new PropDefinerBuilder<CONTEXT>();
    }
    
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
    
    private Map<String, TypeGetter<CONTEXT, ?>> typeGetters = new HashMap<>();
    private Map<String, TypeSetter<CONTEXT, ?>> typeSetters = new HashMap<>();
    private EventHandler<CONTEXT>               afterInitEventHandler;
    private EventHandler<CONTEXT>               afterGetEventHandler;
    private EventHandler<CONTEXT>               afterSetEventHandler;
                                                
    private PropDefinerBuilder() {
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectGetter(TypeGetter<CONTEXT, Object> objectGetter) {
        
        requireNonNull(objectGetter, nonNullMessage("objectGetter"));
        
        typeGetters.put(OBJECT_CLASS_TYPE_NAME, objectGetter);
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectSetter(TypeSetter<CONTEXT, Object> objectSetter) {
        
        requireNonNull(objectSetter, nonNullMessage("objectSetter"));
        
        typeSetters.put(OBJECT_CLASS_TYPE_NAME, objectSetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeGetter(Class<TYPE> typeClass,
            TypeGetter<CONTEXT, TYPE> typeGetter) {
            
        requireNonNull(typeClass, nonNullMessage("typeClass"));
        requireNonNull(typeGetter, nonNullMessage("typeGetter"));
        
        typeGetters.put(typeClass.getTypeName(), typeGetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeSetter(Class<TYPE> typeClass,
            TypeSetter<CONTEXT, TYPE> typeSetter) {
            
        requireNonNull(typeClass, nonNullMessage("typeClass"));
        requireNonNull(typeSetter, nonNullMessage("typeSetter"));
        
        typeSetters.put(typeClass.getTypeName(), typeSetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeGetter(TypeToken<TYPE> typeToken,
            TypeGetter<CONTEXT, TYPE> typeGetter) {
            
        requireNonNull(typeToken, nonNullMessage("typeToken"));
        requireNonNull(typeGetter, nonNullMessage("typeGetter"));
        
        typeGetters.put(typeToken.getType().getTypeName(), typeGetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeSetter(TypeToken<TYPE> typeToken,
            TypeSetter<CONTEXT, TYPE> typeSetter) {
            
        requireNonNull(typeToken, nonNullMessage("typeToken"));
        requireNonNull(typeSetter, nonNullMessage("typeSetter"));
        
        typeSetters.put(typeToken.getType().getTypeName(), typeSetter);
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterInitEventHandler(EventHandler<CONTEXT> afterInitEventHandler) {
        this.afterInitEventHandler = requireNonNull(afterInitEventHandler, nonNullMessage("afterInitEventHandler"));
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterGetEventHandler(EventHandler<CONTEXT> afterGetEventHandler) {
        this.afterGetEventHandler = requireNonNull(afterGetEventHandler, nonNullMessage("afterGetEventHandler"));
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setAfterSetEventHandler(EventHandler<CONTEXT> afterSetEventHandler) {
        this.afterSetEventHandler = requireNonNull(afterSetEventHandler, nonNullMessage("afterSetEventHandler"));
        return this;
    }
    
    private TypeGetter<CONTEXT, Object> getObjectGetter() {
        return getTypeGetter(OBJECT_CLASS_TYPE_NAME);
    }
    
    private TypeSetter<CONTEXT, Object> getObjectSetter() {
        return getTypeSetter(OBJECT_CLASS_TYPE_NAME);
    }
    
    @SuppressWarnings("unchecked")
    private <TYPE> TypeGetter<CONTEXT, TYPE> getTypeGetter(String name) {
        return (TypeGetter<CONTEXT, TYPE>) typeGetters.get(name);
    }
    
    @SuppressWarnings("unchecked")
    private <TYPE> TypeSetter<CONTEXT, TYPE> getTypeSetter(String name) {
        return (TypeSetter<CONTEXT, TYPE>) typeSetters.get(name);
    }
    
    public PropDefiner<CONTEXT> build() {
        return new PropDefiner<CONTEXT>() {
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass) {
                return define(name, typeClass, null);
            }
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass,
                    Function<CONTEXT, TYPE> defaultInitializer) {
                    
                requireNonNull(name, nonNullMessage("name"));
                requireNonNull(typeClass, nonNullMessage("typeClass"));
                
                return new PropImpl<CONTEXT, TYPE>(name, resolveTypeGetter(typeClass), resolveTypeSetter(typeClass),
                        afterInitEventHandler, afterGetEventHandler, afterSetEventHandler, defaultInitializer);
            }
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken) {
                return define(name, typeToken, null);
            }
            
            @Override
            public <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken,
                    Function<CONTEXT, TYPE> defaultInitializer) {
                    
                requireNonNull(name, nonNullMessage("name"));
                requireNonNull(typeToken, nonNullMessage("typeToken"));
                
                return new PropImpl<CONTEXT, TYPE>(name, resolveTypeGetter(typeToken), resolveTypeSetter(typeToken),
                        afterInitEventHandler, afterGetEventHandler, afterSetEventHandler, defaultInitializer);
            }
            
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(Class<TYPE> typeClass) {
                return resolveTypeGetter(typeClass.getTypeName());
            }
            
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(Class<TYPE> typeClass) {
                return resolveTypeSetter(typeClass.getTypeName());
            }
            
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(TypeToken<TYPE> typeToken) {
                return resolveTypeGetter(typeToken.getType().getTypeName());
            }
            
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(TypeToken<TYPE> typeToken) {
                return resolveTypeSetter(typeToken.getType().getTypeName());
            }
            
            @SuppressWarnings("unchecked")
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(String typeClassTypeName) {
                TypeGetter<CONTEXT, TYPE> typeGetter = getTypeGetter(typeClassTypeName);
                if (typeGetter == null) {
                    TypeGetter<CONTEXT, Object> objectGetter = getObjectGetter();
                    
                    requireNonNull(objectGetter,
                            "Either objectGetter or typeGetter is required for type: " + typeClassTypeName);
                            
                    typeGetter = (context, name) -> (TYPE) objectGetter.getFrom(context, name);
                }
                return typeGetter;
            }
            
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(String typeClassTypeName) {
                TypeSetter<CONTEXT, TYPE> typeSetter = getTypeSetter(typeClassTypeName);
                if (typeSetter == null) {
                    TypeSetter<CONTEXT, Object> objectSetter = getObjectSetter();
                    
                    requireNonNull(objectSetter,
                            "Either objectSetter or typeSetter is required for type: " + typeClassTypeName);
                            
                    typeSetter = (context, name, value) -> objectSetter.setTo(context, name, value);
                }
                return typeSetter;
            }
        };
    }
}
