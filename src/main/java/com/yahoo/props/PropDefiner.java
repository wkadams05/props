package com.yahoo.props;


import java.util.function.Function;

import com.google.common.reflect.TypeToken;

public interface PropDefiner<CONTEXT> {
    
    <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass);
    
    <TYPE> Prop<CONTEXT, TYPE> define(String name, Class<TYPE> typeClass,
            Function<CONTEXT, TYPE> defaultValueInitializer);
            
    <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken);
    
    <TYPE> Prop<CONTEXT, TYPE> define(String name, TypeToken<TYPE> typeToken,
            Function<CONTEXT, TYPE> defaultValueInitializer);
}
