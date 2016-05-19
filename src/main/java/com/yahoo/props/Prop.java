package com.yahoo.props;

public interface Prop<CONTEXT, TYPE> {
    String getName();
    
    TYPE getFrom(CONTEXT context);
    
    TYPE getFrom(CONTEXT context, TYPE substIfNull);
    
    void setTo(CONTEXT context, TYPE value);
    
    void setToIfNull(CONTEXT context, TYPE value);
}
