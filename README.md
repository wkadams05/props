# Props - Property Access Utility for Java
_Props_ is a small but useful Java utility which makes property access from/to type-less context easy and safe.  Code gets shorter, simpler, maintainable and flawless.

## SD Build Status
[![Build Status](https://api.screwdriver.corp.yahoo.com:4443/badge/105768/component/icon)](https://api.screwdriver.corp.yahoo.com:4443/badge/105768/component/target)

## Dependency Declaration ([Artifactory](http://artifactory.ops.yahoo.com:4080/webapp/#/artifacts/browse/tree/General/maven-local-release/com/yahoo/props))
```xml
<dependency>
    <groupId>com.yahoo</groupId>
    <artifactId>props</artifactId>
    <version>${latest-release-version}</version>
</dependency>
```

## Quick Start
The following code is a common pattern of accessing data from/to Java properties (java.util.Properties).
```java
Properties properties = new Properties();
properties.load(...);

double myDouble = Double.parseDouble(properties.getProperty("my-double", "0.1d"));
Integer myFlag = properties.getProperty("my-flag") != null ? Integer.parseInt(p.getProperty("my-flag")) : null;

myDouble *= 0.2d;
if (myFlag != null) {
  System.out.println("flag is set");
}

properties.setProperty("my-dbl" /* typo by mistake by other developer */, String.valueOf(myDouble));
```

The team introduced constants to address typo issues.

```java
interface Key {
  String MY_DOUBLE = "my-double";
  String MY_FLAG = "my-flag";
}

interface DefaultValue {
  String MY_DOUBLE = "0.1d";
}

...

double myDouble = Double.parseDouble(p.getProperty(Key.MY_DOUBLE, DefaultValue.MY_DOUBLE));
Integer myFlag = p.getProperty(Key.MY_FLAG) != null ? Integer.parseInt(p.getProperty(Key.MY_FLAG)) : null;

myDouble *= 0.2d;
if (myFlag != null) {
  System.out.println("flag is set");
}

properties.setProperty(Key.MY_DOUBLE, String.valueOf(myDouble)); // looks better
```

Still, there are many potential issues and inconveniences that bother developers.  For example,

1. _Type unknown_
   * Types of properties are basically unknown, until one finds the code where the value is set to.
   * e.g. Some other developer may cast the value of ```Key.MY_FLAG``` to ```Boolean``` presumably.  The error won't be captured during build time but runtime.
   ```java
   // rutime error!
   boolean myFlag = Boolean.parseBoolean(p.getProperty(Key.MY_FLAG, "false"));
   ```
2. _Tedious type juggling_
   * Every code which either read from or write to the context should repeat type casting which makes deveopment counter-productive and hurts readability of codes.
3. _Bothersome ```null``` checking_
   * ```null``` checking (or ```if``` condition with ```contains(key)``` like call) is necessary whenever to confirm ```absent``` state.
   * For some conditional operations like ```setIfAbsent``` semantic, the code gets complicated with ```if``` clauses.

The following is the _Props_ version for the example code above.
```java
interface My {
  Props<Properties, Double> DOUBLE = getDefiner().define("my-double", Double.class, properties -> 1.0d);
  Props<Properties, Integer> FLAG = getDefiner().define("my-flag", Integer.class);
}

My.DOUBLE.setTo(properties, My.DOUBLE.getFrom(properties) * 0.2d);
if (My.FLAG.getFrom(properties) != null) {
  System.out.println("flag is set");
}
```

More details like how ```getDefiner()``` is given, please refer to a sample case at https://git.corp.yahoo.com/jw/props/tree/master/src/test/java/com/yahoo/props/samples/config_from_properties

The power of _Props_ framework is that code can access any arbitrary context objects (not only ```java.util.Properties```) via the same _Props_ framework interfaces (e.g. HTTP parameters).
