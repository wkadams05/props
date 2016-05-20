# Props - Property Access Utility for Java

"Props" is a small but useful Java utility which makes property access from/to type-less context easy and safe.  Code gets shorter, simpler, maintainable and flawless.

For example, the following code is a common pattern of accessing data from/to Java properties (java.util.Properties).
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

1. Type safety
   * Types of properties are basically unknown, until one finds the code where the value is set to.
   * e.g. Some other developer may cast the value of ```Key.MY_FLAG``` to ```Boolean``` presumably.  The error won't be captured during build time but runtime.
   ```java
   // rutime error!
   boolean myFlag = Boolean.parseBoolean(p.getProperty(Key.MY_FLAG, "false"));
   ```
2. Tedious type handling
   * Every code which either read from or write to the context should repeat type casting which makes deveopment counter-productive and hurts readability of codes.
3. Bothersome ```null``` checking
   * ```null``` checking (or ```if``` condition with ```contains(key)``` like call) is necessary whenever to confirm ```absent``` state.
   * For some conditional operations like ```setIfAbsent``` semantic, the code gets complicated with ```if``` clauses.

