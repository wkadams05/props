package com.yahoo.props;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.function.Predicate;

public interface TypeFilter extends Predicate<Type> {
    TypeFilter ENUM          = type -> (type instanceof Class) && ((Class<?>) type).isEnum();
    TypeFilter PARAMETERIZED = type -> type instanceof ParameterizedType;
    TypeFilter GENERIC_ARRAY = type -> type instanceof GenericArrayType;
    TypeFilter WILDCARD      = type -> type instanceof WildcardType;
    TypeFilter TYPE_VARIABLE = type -> type instanceof TypeVariable;
}
