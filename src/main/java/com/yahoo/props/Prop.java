package com.yahoo.props;

public interface Prop<CONTEXT, TYPE> {
    String getName();
    
    TYPE getFrom(CONTEXT context);
    
    TYPE getFrom(CONTEXT context, TYPE substIfNull);
    
    void setTo(CONTEXT context, TYPE value);
    
    void setToIfAbsent(CONTEXT context, TYPE value);
    
    void setToIfPresent(CONTEXT context, TYPE value);
    
    boolean isAbsent(CONTEXT context);
    
    boolean isPresent(CONTEXT context);
}
