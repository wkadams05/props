package com.yahoo.props.samples.config_from_properties;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.reflect.TypeToken;
import com.yahoo.props.Prop;
import com.yahoo.props.PropDefiner;
import com.yahoo.props.PropDefinerBuilder;

@SuppressWarnings("serial")
public interface Config {
    class Definer {
        private static final PropDefiner<Properties> INSTANCE = buildDefiner();
        
        private static PropDefiner<Properties> buildDefiner() {
            PropDefinerBuilder<Properties> builder = PropDefinerBuilder.newBuilder(Properties.class);
            
            // Object
            builder.setObjectSetter((props, key, value) -> props.setProperty(key, String.valueOf(value)));
            
            // String
            builder.setTypeGetter(String.class, (props, key) -> props.getProperty(key));
            
            // String[]
            builder.setTypeGetter(String[].class,
                    (props, key) -> props.getProperty(key) != null ? props.getProperty(key).split("\\s*,\\s*") : null);
            builder.setTypeSetter(String[].class,
                    (props, key, value) -> props.setProperty(key, Joiner.on(',').join(value)));
                    
            // Integer
            builder.setTypeGetter(Integer.class,
                    (props, key) -> props.getProperty(key) != null ? Integer.parseInt(props.getProperty(key)) : null);
                    
            // Double
            builder.setTypeGetter(Double.class,
                    (props, key) -> props.getProperty(key) != null ? Double.parseDouble(props.getProperty(key)) : null);
                    
            // Colo (Enum)
            builder.setTypeGetter(Colo.class, (props, key) -> props.getProperty(key) != null
                    ? Colo.valueOf(props.getProperty(key).toUpperCase()) : null);
            builder.setTypeSetter(Colo.class, (props, key, value) -> props.setProperty(key, value.toString()));
            
            // Set<Colo>
            builder.setTypeGetter(new TypeToken<Set<Colo>>() {},
                    (props, key) -> Splitter.onPattern("\\s*,\\s*").omitEmptyStrings()
                            .splitToList(props.getProperty(key, "")).stream()
                            .map(colo -> Colo.valueOf(colo.trim().toUpperCase())).collect(Collectors.toSet()));
            builder.setTypeSetter(new TypeToken<Set<Colo>>() {}, (props, key, value) -> props.setProperty(key,
                    value.stream().map(colo -> colo.toString()).sorted().reduce((a, b) -> a + "," + b).get()));
                    
            return builder.build();
        }
        
        private static PropDefiner<Properties> get() {
            return INSTANCE;
        }
    }
    
    Prop<Properties, String>    CNAME        = Definer.get().define("cname", String.class);
    Prop<Properties, String[]>  ROLES        = Definer.get().define("roles", String[].class);
    Prop<Properties, Colo>      MAIN         = Definer.get().define("main", Colo.class);
    Prop<Properties, Set<Colo>> REPLICAS     = Definer.get().define("replicas", new TypeToken<Set<Colo>>() {},
            props -> new HashSet<Colo>(0));
    Prop<Properties, Integer>   PORT         = Definer.get().define("port", Integer.class);
    Prop<Properties, Double>    AVAILABILITY = Definer.get().define("availability", Double.class, props -> 0.0d);
}
