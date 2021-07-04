# Props

<!-- TOC depthFrom:2 depthTo:4 withLinks:1 updateOnSave:1 orderedList:0 -->
- [Build](#build)
- [Overview](#overview)
   - [Property, Context, Component and Container](#property-context-component-and-container)
   - [Property Access - Inevitably Inconvenient and Error-prone](#property-access---inevitably-inconvenient-and-error-prone)
   - [Prop Solution - Define Property Accessor with Name and Type](#prop-solution---define-property-accessor-with-name-and-type)
- [Recommended Practices](#recommended-practices)
- [Usage References](#usage-references)
<!-- /TOC -->

## Overview
[Props](https://git.ouroath.com/localsearch/props) is a Java library that makes _Property_ access to any _Context_ easy, safe and powerful.  This library is proven to be remarkably useful in LSBE projects that achieved both development productive and code quality boosts.

Props is originated from the word Properties.  Props aims to be props for developers.

### Property, Context, Component and Container
First, please **DO NOT** confuse _Property_ here with `java.util.Properties`!  Property in Props is a generic term following the definition below.
 
A _Property_ is a named Java object attached to a _Context_ object managed by some Java _Container_ service.  A _Context_ usually holds and carries multiple properties over its lifespan and allows access to a chain of user _Components_.

Here are a few examples (out of tons).

| Container | Context | Property |
|---|---|---|
| [J2EE Web Application Server](https://www.oracle.com/java/technologies/java-ee-glance.html) (e.g. Jetty) | ServletContext | InitParameter (`java.lang.String`), Attribute (`java.lang.Object`) |
| [Spark](http://spark.apache.org/) | SparkContext.SparkConf | Configuration |
| [Vespa](https://vespa.ai/) | Execution, Request, Query | Property (`java.lang.Object`) |
| Java SDK | `java.util.Properties` (valid as just an example) | Property (`java.lang.String` or `java.lang.Object`) |

A common pattern of a container service is:
1. A _Container_ creates a _Context_ object for a given service event (e.g. request).
1. The _Container_ initializes some _Properties_ to the _Context_ from multiple sources (e.g. configurations, service events) then pass it down to the chain/set of user _Components_.
1. Each user _Component_ performs its logic with accessing _Properties_ in the given _Context_.  It reads _Properties_ to perform, and often updates back to impact others or the _Container_ services.

### Property Access - Inevitably Inconvenient and Error-prone 
Properties are defined and used by user components.  Container has no idea what properties will be used in terms of their names and types.  Hence the best containers do for property access is having a generic keyed object placeholder (e.g. `java.util.Map`) in its Context with a minimum pair of accessors (getter & setter).

Here's an example of a Java HttpServlet.
```java
public class MyServlet extends HttpServlet {

    public enum ProcessingResult {
        APPROVED, REJECTED, PENDING
    }
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // get HTTP parameters of different types
        String customerName = req.getParameter("customerName");
        if (customerName == null)
            customerName = "noname";

        String accountNoStr = req.getParameter("accountNo");
        int accountNo = accountNoStr != null ? Integer.parseInt(accountNoStr) : -1;
        
        String paidStr = req.getParameter("paid");
        boolean paid = paidStr != null ? Boolean.parseBoolean(paidStr) : false;

        // get creditScore(int) attribute which other upstream component (e.g. filter) set
        int creditScore = (Integer) req.getAttribute("creditScore");

        ProcessingResult result;
        if (creditScore > 700)
            result = ProcessingResult.APPROVED;
        else if (paid)
            result = ProcessingResult.PENDING;
        else
            result = ProcessingResult.REJECTED;

        // set processingResult(enum) attribute for other downstream components
        req.setAttribute("processingResult", result);

        // what if "creditScore" changes in other component after "processingResult" set above??
    }
}
```
There are 2 issues: Inconvenience and error-proneness
* Inconvenience
   1. _Tedious type juggling_
      * One should always cast or convert into ideal data type to use. 
      * Data types are even often unknown: no idea to which type to convert until find how it started
   2. _Absence checking absence_
      * Extra `null` checking (or `if` condition with `contains(key)` like call) is necessary whenever to confirm _absence_ state of the property.
      * To achieve some advanced conditional like `setIfAbsent` semantic, the code becomes complicated with another extra `if` clauses.

* Error-proneness
   1. Name unbound
      * Property access is done mostly by `java.lang.String` name.  Components often miscommunicate by using different names for the same (e.g. `years` vs. `yrs`, `number` vs. `no`).
   2. Type unbound
      * Property is casted or type converted for use.  Components often expect different types for the same property.  Errors are not detected until runtime.

These issues and the cost of maintenance magnify as the number of properties to manage grow to several tens or hundreds.

### Prop Solution - Define Property Accessor with Name and Type
With Props, the code example above turns simple and powerful as follows.
```java
public class MyServlet extends HttpServlet {

    public enum ProcessingResult {
        APPROVED, REJECTED, PENDING
    }
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // read HTTP parameters of different types
        String customerName = CUSTOMER_NAME.getFrom(req);
        int accountNo = ACCOUNT_NO.getFrom(req);
        boolean paid = PAID.getFrom(req);

        int creditScore = CREDIT_SCORE.getFrom(req);

        // processing result from the first call to the initializer func
        System.out.println(PROCESSING_RESULT.getFrom(req)); // REJECTED

        // reset dependency test

        PAID.setTo(req, true);
        // processing result re-evaluation by PAID reset-dependency change
        System.out.println(PROCESSING_RESULT.getFrom(req)); // PENDING

        CREDIT_SCORE.setTo(req, 700);
        // processing result re-evaluation by CREDIT_SCORE reset-dependency change
        System.out.println(PROCESSING_RESULT.getFrom(req)); // APPROVED
    }
}
```

This change is possible with one time configurator setup as follows.  This may look big, but this one time investment at the beginning of the project pays back x1000 as the project grows and matures.
```java
public class MyServlet extends HttpServlet {

    PropDefiner<HttpServletRequest> propDefiner = buildPropDefiner();

    PropDefiner<HttpServletRequest> buildPropDefiner() {
        PropDefinerBuilder<HttpServletRequest> builder = PropDefinerBuilder.newBuilder(HttpServletRequest.class);
        // make attribute overrides parameter of the same name
        builder.setTypeGetter(String.class, (req, name) -> (req.getAttribute(name) != null) ? (String) req.getAttribute(name) : req.getParameter(name));
        builder.setTypeGetter(Integer.class, (req, name) -> (req.getAttribute(name) != null) ? (Integer) req.getAttribute(name) : Integer.parseInt(req.getParameter(name)));
        builder.setTypeGetter(Boolean.class, (req, name) -> (req.getAttribute(name) != null) ? (Boolean) req.getAttribute(name) : Boolean.parseBoolean(req.getParameter(name)));
        // objects are attributes
        builder.setObjectGetter((req, name) -> req.getAttribute(name));
        builder.setObjectSetter((req, name, value) -> req.setAttribute(name, value));
        return builder.build();
    }

    Prop<HttpServletRequest, String> CUSTOMER_NAME = propDefiner.define("customer_name", String.class, req -> "noname");

    Prop<HttpServletRequest, Integer> ACCOUNT_NO = propDefiner.define("account_no", Integer.class, req -> -1);

    Prop<HttpServletRequest, Boolean> PAID = propDefiner.define("paid", Boolean.class, req -> false);

    Prop<HttpServletRequest, Integer> CREDIT_SCORE = propDefiner.define("credit_score", Integer.class, req -> 600);

    Prop<HttpServletRequest, ProcessingResult> PROCESSING_RESULT = propDefiner.define("processing_results", ProcessingResult.class,
            req -> {
                int score = CREDIT_SCORE.getFrom(req);
                if (score >= 700)
                    return ProcessingResult.APPROVED;
                else if (PAID.getFrom(req))
                    return ProcessingResult.PENDING;
                else
                    return ProcessingResult.REJECTED;
            })
            // default initializer above is re-evaluated if any of reset-dependency changes below
            .addResetDependency(CREDIT_SCORE)
            .addResetDependency(PAID);
    ...
}
```

Here is the overall process review.
1. Determine the type (Java class) of the _CONTEXT_ object of access.
2. Build `PropDefiner<CONTEXT>` instance with `PropDefinerBuilder` builder class.
3. Define `Prop<CONTEXT, TYPE>` variable by using definer instance built at \#2 above, as the property accessor. _TYPE_ is the class type of this prop.
4. Make all defined Props accessible to all components (e.g. place in a public static interface collected).
5. Use Prop variables wherever target _CONTEXT_ object is available to read/write properties in/out.

## Recommended Practices
1. **_Use Java interfaces as placeholders for global Props._**

   To serve considerable amount of Props in global scope, it's not a bad idea to place cohort/cohesion of Props in a Java interface.  This is a pattern used across all Local Search applications (see [references](#references)).

   An example below shows how multiple components share the global Props through a public Java interface.  Here, components are `Processors` in the context of [Vespa Processing framework](https://docs.vespa.ai/documentation/jdisc/processing.html).

   ```java
   // An interface - for global scope properties
   public interface Property {
       Prop<Request, String>  CUSTOMER_NAME = propDefiner.define("customer_name", String.class);
       Prop<Request, Integer> ACCOUNT_NO    = propDefiner.define("account_no", Integer.class);
       Prop<Request, Boolean> PAID          = propDefiner.define("paid", Boolean.class);
       Prop<Request, Integer> CREDIT_SCORE  = propDefiner.define("credit_score", Integer.class);
       ...
   }

   // Processor A - property setter
   import Property.*;
   public class ProcessorA extends com.yahoo.processing.Processor {
       @Override
       public Response process(Request request, Execution execution) {
           CREDIT_SCORE.setTo(request, 700);
       }
   }

   // Processor A - property getter
   import Property.*;
   public class ProcessorB extends com.yahoo.processing.Processor {
       @Override
       public Response process(Request request, Execution execution) {
           int creditScore = CREDIT_SCORE.getFrom(request);
       }
   }
   ```
2. **_Use Prop's name attribute (`java.lang.String` literal) as its instance name in order to avoid name conflicts._**
   Unexpectedly but unfortunately, it's possible to introduce a bug of defining more than one Props for the same name that result in diverse errors visible (better) or invisible/silent (worst). e.g.,
   ```java
   // at the beginning of the project
   Prop<CONTEXT, String> USER = propDefiner.define("USER", String.class);
   ...
   // years after, by a new hired developer..
   Prop<CONTEXT, Integer> USER_ID = propDefiner.define("USER", Integer.class, ctx -> -1 /* unknown user ID */ );
   ```

   There can be multiple ways outside the Props framework to prevent this situation, but one simplest and doable practice is having a policy of naming Prop instances following its name attribute (or vice versa), like
   ```java
   // at the beginning of the project
   Prop<CONTEXT, String> USER = propDefiner.define("USER", String.class);
   ...
   // years after, a new hired developer who learned Prop naming policy should notice the pre-existence of USER Prop definition above..
   // when he wanted a prop to access "USER" keyed property, then will reuse (or refactor) the existing one.
   ```

   Note that this strategy/practice may not be applicable if Props is introduced to migrate any existing property access commons where property names/keys pre-exist that may not be OK to be Java variable names.

## Usage References
| Type | Name | Container | Context | Property | Link | Comment |
|---|---|---|---|---|---|---|
|Production|Local Search|Vespa|Query|Query properties|[com.yahoo.ls.searcher.query.QueryProperty.java](https://git.vzbuilders.com/localsearch/lsbe-hv/blob/master/src/main/java/com/yahoo/ls/searcher/query/QueryProperty.java)|The serious most use case. Over 220 properties for ~50 Searcher components in Vespa|
|Production(EOL'd)|Concept Search|Vespa|HttpRequestContext|HTTP parameters w/ overrides|[com.yahoo.concept.api.HttpParam.java](https://git.vzbuilders.com/localsearch/concept-search/blob/master/src/main/java/com/yahoo/concept/api/HttpParam.java)||
|Test code as tutorial|-|Java SDK|`java.util.Properties`|keyed `java.util.String` property|[com/yahoo/props/samples/config_from_properties](src/test/java/com/yahoo/props/samples/config_from_properties)||