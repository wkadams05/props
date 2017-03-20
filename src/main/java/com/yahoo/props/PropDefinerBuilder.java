package com.yahoo.props;

import static com.yahoo.props.Utils.nonNullMessage;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.reflect.TypeToken;

public class PropDefinerBuilder<CONTEXT> {
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
    public interface ComprehensiveTypeGetter<CONTEXT, TYPE> extends TypeGetter<CONTEXT, TYPE> {
        default TYPE getFrom(CONTEXT context, String name) {
            throw new IllegalAccessError();
        }
        
        TYPE getFrom(CONTEXT context, Type type, String name);
    }
    
    @FunctionalInterface
    public interface ComprehensiveTypeSetter<CONTEXT, TYPE> extends TypeSetter<CONTEXT, TYPE> {
        default void setTo(CONTEXT context, String name, TYPE value) {
            throw new IllegalAccessError();
        }
        
        void setTo(CONTEXT context, Type type, String name, TYPE value);
    }
    
    @FunctionalInterface
    public interface EventHandler<CONTEXT> {
        void onEvent(CONTEXT context, String name, Object value);
    }
    
    private Map<Type, TypeGetter<CONTEXT, ?>> typeGetters = new HashMap<>();
    private Map<Type, TypeSetter<CONTEXT, ?>> typeSetters = new HashMap<>();
    
    private Map<Predicate<Type>, ComprehensiveTypeGetter<CONTEXT, ?>> comprehensiveTypeGetters = new HashMap<>();
    private Map<Predicate<Type>, ComprehensiveTypeSetter<CONTEXT, ?>> comprehensiveTypeSetters = new HashMap<>();
    
    private EventHandler<CONTEXT> afterInitEventHandler;
    private EventHandler<CONTEXT> afterGetEventHandler;
    private EventHandler<CONTEXT> afterSetEventHandler;
    
    private PropDefinerBuilder() {
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectGetter(TypeGetter<CONTEXT, Object> objectGetter) {
        
        requireNonNull(objectGetter, nonNullMessage("objectGetter"));
        
        typeGetters.put(Object.class, objectGetter);
        return this;
    }
    
    public PropDefinerBuilder<CONTEXT> setObjectSetter(TypeSetter<CONTEXT, Object> objectSetter) {
        
        requireNonNull(objectSetter, nonNullMessage("objectSetter"));
        
        typeSetters.put(Object.class, objectSetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeGetter(Class<TYPE> typeClass,
            TypeGetter<CONTEXT, TYPE> typeGetter) {
        
        requireNonNull(typeClass, nonNullMessage("typeClass"));
        requireNonNull(typeGetter, nonNullMessage("typeGetter"));
        
        typeGetters.put(typeClass, typeGetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeSetter(Class<TYPE> typeClass,
            TypeSetter<CONTEXT, TYPE> typeSetter) {
        
        requireNonNull(typeClass, nonNullMessage("typeClass"));
        requireNonNull(typeSetter, nonNullMessage("typeSetter"));
        
        typeSetters.put(typeClass, typeSetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeGetter(TypeToken<TYPE> typeToken,
            TypeGetter<CONTEXT, TYPE> typeGetter) {
        
        requireNonNull(typeToken, nonNullMessage("typeToken"));
        requireNonNull(typeGetter, nonNullMessage("typeGetter"));
        
        typeGetters.put(typeToken.getType(), typeGetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeSetter(TypeToken<TYPE> typeToken,
            TypeSetter<CONTEXT, TYPE> typeSetter) {
        
        requireNonNull(typeToken, nonNullMessage("typeToken"));
        requireNonNull(typeSetter, nonNullMessage("typeSetter"));
        
        typeSetters.put(typeToken.getType(), typeSetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeGetter(Predicate<Type> typeFilter,
            ComprehensiveTypeGetter<CONTEXT, TYPE> typeGetter) {
        
        requireNonNull(typeFilter, nonNullMessage("typeFilter"));
        requireNonNull(typeGetter, nonNullMessage("typeGetter"));
        
        comprehensiveTypeGetters.put(typeFilter, typeGetter);
        return this;
    }
    
    public <TYPE> PropDefinerBuilder<CONTEXT> setTypeSetter(Predicate<Type> typeFilter,
            ComprehensiveTypeSetter<CONTEXT, TYPE> typeSetter) {
        
        requireNonNull(typeFilter, nonNullMessage("typeFilter"));
        requireNonNull(typeSetter, nonNullMessage("typeSetter"));
        
        comprehensiveTypeSetters.put(typeFilter, typeSetter);
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
        return getTypeGetter(Object.class);
    }
    
    private TypeSetter<CONTEXT, Object> getObjectSetter() {
        return getTypeSetter(Object.class);
    }
    
    @SuppressWarnings("unchecked")
    private <TYPE> TypeGetter<CONTEXT, TYPE> getTypeGetter(Type type) {
        TypeGetter<CONTEXT, TYPE> getter = (TypeGetter<CONTEXT, TYPE>) typeGetters.get(type);
        if (getter == null) {
            Optional<Predicate<Type>> typeFilter = comprehensiveTypeGetters.keySet().stream()
                    .filter(filter -> filter.test(type)).findFirst();
            if (typeFilter.isPresent()) {
                getter = (TypeGetter<CONTEXT, TYPE>) comprehensiveTypeGetters.get(typeFilter.get());
            }
        }
        return getter;
    }
    
    @SuppressWarnings("unchecked")
    private <TYPE> TypeSetter<CONTEXT, TYPE> getTypeSetter(Type type) {
        TypeSetter<CONTEXT, TYPE> setter = (TypeSetter<CONTEXT, TYPE>) typeSetters.get(type);
        if (setter == null) {
            Optional<Predicate<Type>> typeFilter = comprehensiveTypeSetters.keySet().stream()
                    .filter(filter -> filter.test(type)).findFirst();
            if (typeFilter.isPresent()) {
                setter = (TypeSetter<CONTEXT, TYPE>) comprehensiveTypeSetters.get(typeFilter.get());
            }
        }
        return setter;
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
                
                return new PropImpl<CONTEXT, TYPE>(name, typeClass, resolveTypeGetter(typeClass),
                        resolveTypeSetter(typeClass), afterInitEventHandler, afterGetEventHandler, afterSetEventHandler,
                        defaultInitializer);
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
                
                return new PropImpl<CONTEXT, TYPE>(name, typeToken.getType(), resolveTypeGetter(typeToken),
                        resolveTypeSetter(typeToken), afterInitEventHandler, afterGetEventHandler, afterSetEventHandler,
                        defaultInitializer);
            }
            
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(TypeToken<TYPE> typeToken) {
                return resolveTypeGetter(typeToken.getType());
            }
            
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(TypeToken<TYPE> typeToken) {
                return resolveTypeSetter(typeToken.getType());
            }
            
            @SuppressWarnings("unchecked")
            private <TYPE> TypeGetter<CONTEXT, TYPE> resolveTypeGetter(Type type) {
                TypeGetter<CONTEXT, TYPE> typeGetter = getTypeGetter(type);
                if (typeGetter == null) {
                    TypeGetter<CONTEXT, Object> objectGetter = getObjectGetter();
                    
                    requireNonNull(objectGetter,
                            "Either objectGetter or typeGetter is required for type: " + type.getTypeName());
                    
                    typeGetter = (context, name) -> (TYPE) objectGetter.getFrom(context, name);
                }
                return typeGetter;
            }
            
            private <TYPE> TypeSetter<CONTEXT, TYPE> resolveTypeSetter(Type type) {
                TypeSetter<CONTEXT, TYPE> typeSetter = getTypeSetter(type);
                if (typeSetter == null) {
                    TypeSetter<CONTEXT, Object> objectSetter = getObjectSetter();
                    
                    requireNonNull(objectSetter,
                            "Either objectSetter or typeSetter is required for type: " + type.getTypeName());
                    
                    typeSetter = (context, name, value) -> objectSetter.setTo(context, name, value);
                }
                return typeSetter;
            }
        };
    }
}
