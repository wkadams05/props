package com.yahoo.props;

public class Utils {
    public static String nonNullMessage(String objName) {
        return String.format("'%s' shouldn't be null", objName);
    }
}
