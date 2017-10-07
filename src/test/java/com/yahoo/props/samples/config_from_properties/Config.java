package com.yahoo.props.samples.config_from_properties;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.reflect.TypeToken;
import com.yahoo.props.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yahoo.props.samples.config_from_properties.Config.Helper.getDefiner;

@SuppressWarnings("serial")
public interface Config {
    Prop<Properties, String>    CNAME        = getDefiner().define("cname", String.class);
    Prop<Properties, String[]>  ROLES        = getDefiner().define("roles", String[].class);
    Prop<Properties, Colo>      MAIN         = getDefiner().define("main", Colo.class);
    Prop<Properties, Set<Colo>> REPLICAS     = getDefiner().define("replicas", new TypeToken<Set<Colo>>() {
            },
            props -> new HashSet<Colo>(0));
    Prop<Properties, Integer>   PORT         = getDefiner().define("port", Integer.class);
    Prop<Properties, Double>    AVAILABILITY = getDefiner().define("availability", Double.class,
            props -> ((double) REPLICAS.getFrom(props).size() / 10.0d))
            .addTargetMonitorForReset(props -> REPLICAS.getFrom(props));
    Prop<Properties, Env>       ENV          = getDefiner().define("env", Env.class);

    class Helper {
        private static final PropDefiner<Properties> DEFINER = buildDefiner();

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

            // Enum types
            builder.setTypeGetter(TypeFilter.ENUM,
                    (props, type, key) -> Utils.resolveEnumValue(type, props.getProperty(key)));
            builder.setTypeSetter(TypeFilter.ENUM,
                    (props, type, key, value) -> props.setProperty(key, value.toString()));

            // Set<Colo>
            builder.setTypeGetter(new TypeToken<Set<Colo>>() {
                                  },
                    (props, key) -> Splitter.onPattern("\\s*,\\s*").omitEmptyStrings()
                            .splitToList(props.getProperty(key, "")).stream()
                            .map(colo -> Colo.valueOf(colo.trim().toUpperCase())).collect(Collectors.toSet()));
            builder.setTypeSetter(new TypeToken<Set<Colo>>() {
            }, (props, key, value) -> props.setProperty(key,
                    value == null ? "" : value.stream().map(colo -> colo.toString()).sorted().reduce((a, b) -> a + "," + b).get()));

            return builder.build();
        }

        static PropDefiner<Properties> getDefiner() {
            return DEFINER;
        }
    }
}
