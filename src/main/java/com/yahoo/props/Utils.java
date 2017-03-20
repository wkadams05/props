package com.yahoo.props;

import java.lang.reflect.Type;

public class Utils {
    public static String nonNullMessage(String objName) {
        return String.format("'%s' shouldn't be null", objName);
    }
    
    @SuppressWarnings("unchecked")
    public static <TYPE> TYPE resolveEnumValue(Type enumType, Object valueOrName) {
        if (valueOrName != null) {
            if (((Class<TYPE>) enumType).isInstance(valueOrName)) {
                return (TYPE) valueOrName;
            } else {
                assert (valueOrName instanceof String);
                String name = (String) valueOrName;
                for (TYPE value : ((Class<TYPE>) enumType).getEnumConstants()) {
                    if (name.equalsIgnoreCase(value.toString())) {
                        return value;
                    }
                }
            }
        }
        
        return null;
    }
}
