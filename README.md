# Props - Property Access Utility for Java

"Props" is a small but useful Java utility which makes property access from/to type-less context easy and safe.  Code gets shorter, simpler, maintainable and flawless.

For example, the following code is a common pattern of accessing data from/to Java properties (java.util.Properties).
```java
Properties p = new Properties();
p.load(/* from a .properties file */);

// read with default from the context(java.util.Properties), and cast down to a double type.
double myRatio = Double.parseDouble(p.getProperty("my-ratio", "0.1d"));
Integer myFlag = p.getProperty("my-flag") != null ? Integer.parseInt(p.getProperty("my-flag")) : null;

// do some work
myRatio *= 0.2d;
if (myFlag != null) {
  System.out.println("flag is set");
}

// cast to string, then write back to the context(java.util.Properties).
properties.setProperty("my-ratio", String.valueOf(myRatio));
```

Developers tend to start introducing constants as the number of properties to manage grows.
```java
interface Key {
  String MY_RATIO = "my-ratio";
  String MY_FLAG = "my-flag";
}

interface DefaultValue {
  String MY_RATIO = "0.1d";
}

...

double myRatio = Double.parseDouble(p.getProperty(Key.MY_RATIO, DefaultValue.MY_RATIO));
Integer myFlag = p.getProperty(Key.MY_FLAG) != null ? Integer.parseInt(p.getProperty(Key.MY_FLAG)) : null;

myRatio *= 0.2d;
if (myFlag != null) {
  System.out.println("flag is set");
}

properties.setProperty(Key.MY_RATIO, String.valueOf(myRatio));
```

It looks better.  However, there are still many potential issues with this approach left over as follows.

1. Type safety
   * Runtime error can happen if other codes access 
2. 


