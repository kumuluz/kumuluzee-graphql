# KumuluzEE GraphQL

[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-graphql/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-graphql)

> Kick-start your GraphQL server development with this powerful extension.

KumuluzEE GraphQL extension enables you to easily create your own GraphQL server with a few simple annotations. Using 
this extension requires understanding of the basic GraphQL concepts.

Read about GraphQL: [GraphQL](http://graphql.org/learn/).

This extension is built upon the [GraphQL Java implementation](https://github.com/graphql-java/graphql-java) and uses [GraphQL SPQR](https://github.com/leangen/graphql-spqr) extension for building schema from annotations. 

## Usage

You can enable KumuluzEE GraphQL by adding the following dependency to the project:
```xml
<dependency>
    <groupId>com.kumuluz.ee.graphql</groupId>
    <artifactId>kumuluzee-graphql</artifactId>
    <version>${kumuluzee-graphql.version}</version>
</dependency>
```

When KumuluzEE GraphQL is included in the project, you can start developing your GraphQL services.

### GraphQL server configuration
GraphQL server will be served on /graphql by default. You can change this with the KumuluzEE configuration framework by setting the following key:
```yaml
kumuluzee:
  server:
    graphql:
      mapping: /myCompanyGraphQL
```

### Registering GraphQL classes 

The `@GraphQLClass` annotation must be used on the classes that define GraphQL related functions. All GraphQL 
annotated functions in annotated classes will be added to your GraphQL schema.

```java
@GraphQLClass
public class HelloWorld {...}
```
### Defining GraphQL queries 
The `@GraphQLQuery` annotation will register your Java function as a Query function in GraphQL. All types and 
parameters will be automatically converted to GraphQL types and added to the schema. You can override the query name (which defaults to the function name) or add a description to the query.

```java
@GraphQLClass
public class HelloWorld {
    @GraphQLQuery
    public String hello() {
        return "world";
    }
    
    @GraphQLQuery(name="somethingElse", desciption="Hello world function")
    public String world(@GraphQLArgument(name="parameter") String parameter) {
        return "hello";
    }
}
```

### Defining GraphQL mutations
The `@GraphQLMutation` annotation is used for defining mutations. It is used the same way as `@GraphQLQuery` annotation. 
The only difference is, that mutations are used for changing persistent state, while queries only retrieve data. 

More information on this can be found in GraphQL documentation: [Queries and mutations](http://graphql.org/learn/queries/).
```java
@GraphQLClass
public class HelloWorld {
    @GraphQLMutation
    public String hello(@GraphQLArgument(name="world") String world) {
        // save to database, perform mutation
        return world;
    }
    
    @GraphQLMutation(name="somethingElse", desciption="Hello world function")
    public String world(@GraphQLArgument(name="hello") String hello) {
        // save to database, perform mutation
        return hello;
    }
}
```

### Annotating GraphQL arguments
The `@GraphQLArgument` annotation must be used for defining the arguments. It allows you to override argument's name, 
add a description, a default value, or even a custom DefaultValueProvider.

```java
@GraphQLQuery
public Integer number(@GraphQLArgument(name="number", defaultValue="0") Integer number) {
    return number;
}
  
@GraphQLQuery
public String text(@GraphQLArgument(name="text", defaultValueProvider = SomeProvider.class) String text) {
    return text;
}
```

> Avoid using primitive types as parameters (int, double...), because they cannot be `null`. If you use them, please provide their default values with this annotation or your application will crash if selected parameter will be missing from your query! It is recommended to use their wrapper classes instead (such as Integer, Double...).

Annotation can be omitted if you add -parameters to your javac compiler. You can use maven compiler plugin for that:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>${version}</version>
    <configuration>
        <compilerArgs>
            <arg>-parameters</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Managing GraphQL input fields
With the `@GraphQLInputField` annotation you can rename input fields and provide their descriptions. Annotation can 
be used on a setter, a getter or on a field, but it is recommended to be used on setters only.
```java
@GraphQLInputField(name="someField", description="Some description")
public void randomFunctionName(String someField) {
    this.someField = someField;
}
```

### Annotation `@GraphQLIgnore`
This annotation can be used to ignore a certain input/output field. If used on a getter, the field will not be 
present in the output, if used on a setter, the field will not be present in the input.
```java
@GraphQLIgnore
public String getField() {
    return field;
}
```

### Annotation `@GraphQLNonNull`
If you want to mark a parameter as required, you can annotate the type with `@GraphQLNonNull` annotation. This works 
both for input/output types (on getters or setters). It can be also used on lists: 
```java
// non null list of non null students
@GraphQLNonNull List<@GraphQLNonNull Student>

public @GraphQLNonNull String getField() {
    return field;
}
  
public void setField(@GraphQLNonNull String field) {
    this.field = field;
}

@GraphQLMutation
public String someMutation(@GraphQLNonNull @GraphQLArgument(name="field") String field) {
  return field;
} 
```

### Annotation `@GraphQLContext`
The `@GraphQLContext` annotation allows you to add a field to some object in the schema (for example if you get a field 
from another source). The following example will add a field "newField" to TestType schema. You can access all the properties from the parent object and use them for fetching your data.
```java
@GraphQLQuery(name="newField")
public String getNewField(@GraphQLContext TestType object) {
    return someBean.getSomething(object.getField1());
}
```
> It is recommended to use this when making nested queries. It is best to avoid writing logic in your JPA entities 
and keep your classes clean. 


### Annotation `@GraphQLEnvironment`
This annotation is used to inject GraphQL-java related fields, which are contained inside ResolutionEnvironment. 
It contains multiple objects such as DataFetchingEnvironment, schema, context etc. 

```java
@GraphQLQuery
public String test(@GraphQLEnvironment ResolutionEnvironment resolutionEnvironment) {
    return resolutionEnvironment.toString();
}
```
 

### Annotating custom types
If you use a custom type as a type in a query, you do not need to annotate it, if you provide default getters and 
setters. If you change getter in any way (add parameter...), you need to annotate it with a `@GraphQLQuery` or your 
field will not be registered to the schema. This happens because every field in GraphQL needs a resolver function. 
Setters cannot really take any additional parameters, so the only option is to rename them. You can do that with 
`@GraphQLInputField` annotation.

```java
@GraphQLClass
public class HelloWorld {
    @GraphQLQuery
    public TestType test() {
        TestType type = {...};
        return type;
    }
}
  
  
public class TestType {
    private String field1;
    private Integer field2;
    
    // annotation is not needed here
    public String getField1() {
        return field1;
    }
    
    // annotation is needed here, name must be specified
    @GraphQLQuery(name="field2")
    public Integer getField2(@GraphQLArgument(name="parameter") Integer parameter) {
        return field2 + 123;
    }
    
    // annotation is not needed here
    public void setField1(String field1) {
        this.field1 = field1;
    }
    
    // annotation is needed here, name must be specified
    @GraphQLInputField(name="field2")
    public void setField2withChangedName(String field2) {
        this.field2 = field2 + 123;
    }
}
```

You also need to add empty constructor, if you are using that type as input in a Mutation.
```java
@GraphQLClass
public class SomeClass {
    @GraphQLMutation
    public TestType test(@GraphQLArgument(name="input") TestType t) {
        // save to database
        return t;
    }
}
  
  
public class TestType {
    {...}
    private List<String> someList;
    
    public TestType() {
        // can also perform actions such as creating empty lists, 
        // to prevent them from being null (if not passed to Mutation)
        someList = new ArrayList<>(); 
    }
}
```
## Using GraphQLUtils for pagination, sorting and filtering
The GraphQLUtils class has a number of functions for advanced GraphQL operations, such as agination, sorting and filtering. 
The main function is GraphQLUtils.process(). It takes your ```List<YourType>``` as a parameter with optional 
parameters of classes Sort, Pagination and Filter which represent user input. Changing your output type from 
```List<YourType>``` to ```PaginationWrapper<YourType>``` is also required in order to request pagination details when querying.

Example usage (taken from the sample project):
```java
// without pagination
@GraphQLQuery
public List<Student> allStudents() {
    return facultyBean.getStudentList();
}
  
// with pagination --> parameters are optional, but recommended to use
@GraphQLQuery
public PaginationWrapper<Student> allStudents(@GraphQLArgument(name="pagination") Pagination pagination, @GraphQLArgument(name="sort") Sort sort, @GraphQLArgument(name="filter") Filter filter) {
    return GraphQLUtils.process(facultyBean.getStudentList(), pagination, sort, filter);
}
```
Example GraphQL query:
```
# without pagination
{ 
  allStudents {
    studentNumber
    name
    suername
  }
}
  
# with pagination
{
  allStudents(pagination: {offset: 0, limit: 10}, sort: {fields: [{field: "studentNumber", order: ASC}]}) {
    result {
      studentNumber
      name
      surname
    }
    pagination {
      offset
      limit
      total
    }
  }
}
```
Arguments must match in name (if you add Pagination pagination as user input, you need to use name pagination when querying).

### Pagination structure
The `PaginationWrapper` wraps your list with the pagination object (of type Pagination) and result list (your original 
list). PaginationInput type has two fields: offset and limit, while the PaginationOutput adds field total (available 
when querying).

Example GraphQL query:
```
someFunctionName(pagination: 
    {
        offset: 0, 
        limit: 10
    }
)
```


### Sorting structure
Sort object contains a list of `SortField`. Each `SortField` contains two fields: field and order (ASC or DESC). 
When querying, fields are processed in the same order as they are passed. Sorting works natively on types: Integer, Double, Float, String and Date. If your field is of another type, the comparison will default to String comparison.

Example GraphQL query:
```
someFunctionName(sort: { 
    fields: [
        {
            field: "assistant.popularity", 
            order: DESC
        },
        {
            field: "surname", 
            order: ASC
        }
    ]
})
```

### Filtering structure
Filter object contains a list of `FilterField`. Each `FilterField` contains four fields: op (operator), field, value and type.
Following operators are supported:
* EQ | Equals
* EQIC | Equals ignore case
* NEQ | Not equal
* NEQIC | Not equal ignoring case
* LIKE | Pattern matching (regex search)
* LIKEIC | Pattern matching ignore case (regex search)
* GT | Greater than
* GTE | Greater than or equal
* LT | Lower than
* LTE | Lower than or equal
* IN | In set
* INIC | In set ignore case
* NIN | Not in set
* NINIC | Not in set ignore case
* ISNULL | Null
* ISNOTNULL | Not null

Most operations work on types Integer, Double, Float, String and Date (must be passed in ISO-8601 format), while ignoring case operations (IC) only work on String.
Type needs to be passed when filtering (if not, defaults to STRING), because the value field is string and needs to be casted to the right type in order to perform comparisons.

Example GraphQL query:
```
someFunctionName(filter: {
    fields: [
        {
            op: LIKE, 
            field: "classroom", 
            value: "P.*"
        }, 
        {
            op: INIC, 
            field: "lecturer.assistant.name", 
            value: "[Bradley,scott]"
        }, 
        {
            op: NEQ,
            field: "lecturer.assistant.popularity", 
            type: DOUBLE, 
            value: "10"
        }
    ]
})
```

### GraphQLUtils JPA integration
`GraphQLUtils` fully supports `JPAUtils` from te [kumuluzee-rest](https://github.com/kumuluz/kumuluzee-rest) 
extension. `JPAUtils` optimize database requests, querying only the objects that are specified with the 
paginarion, sort and filter input fields. You can use the `GraphQLUtils` to automatically wrap the calls for you, or 
call `JPAUtils` manually.

#### GraphQLUtils calls JPAUtils
This is the easier option. Just call function GraphQLUtils.process(), pass an `EntityManager` instance and your 
Class + pagination/sort/filter structures. The function returns PaginationWrapper<Type>. If you do not want any metadata, you
 can call getResult() on wrapper object to get List<Type>.
```java
public PaginationWrapper<Student> getStudents(Pagination pagination, Sort sort, Filter filter) {
    return GraphQLUtils.process(em, Student.class, pagination, sort, filter);
}
```

#### Manual JPAUtils call
If you do not want `GraphQLUtils` to do the processing, you can call `JPAUtils` manually (`GraphQLUtils` calls function 
JPAUtils.queryEntities). Before doing that, please read [kumuluzee-rest](https://github.com/kumuluz/kumuluzee-rest) documentation.
`GraphQLUtils` has a few helper functions which you can use:
- queryParameters (converts Pagination, Sort and Filter classes to QueryParameters required for JPAUtils calls),
- wrapList (wraps List to PaginationWrapper; you need to pass number of items manually in order to get total field when querying or not pass them at all, which results in `null` field)

```java
public PaginationWrapper<Subject> getSubjectList(Pagination pagination, Sort sort, Filter filter) {
    QueryParameters queryParameters = GraphQLUtils.queryParameters(pagination, sort, filter);
    List<Subject> subjectList = JPAUtils.queryEntities(em, Subject.class, queryParameters);
    Long count = JPAUtils.queryEntitiesCount(em, Subject.class, queryParameters);
    return GraphQLUtils.wrapList(subjectList, pagination, count.intValue());
    //return GraphQLUtils.wrapList(subjectList, pagination);
}
``` 

#### Optimize JPA queries 
You can further optimize queries by providing `ResolutionEnvironment` to process or processWithoutPagination function.
`GraphQLUtils` will then extract the fields you queried and made sure, that only these fields will be present in the JPA query.

```java
@GraphQLQuery
public List<Assistant> allAssistants(@GraphQLArgument(name="sort") Sort sort, @GraphQLEnvironment ResolutionEnvironment resolutionEnvironment) {
    return GraphQLUtils.processWithoutPagination(em, Assistant.class, resolutionEnvironment, sort);
}
```
ResolutionEnvironment is injected with the `@GraphQLEnvironment` annotation and is always the third parameter of the 
function behind the `EntityManager` and class.

> Optimization is not enabled by default, because it is an experimental feature. 
If you have any problems, please submit a bug report.

### Settings defaults
Defaults can be set in config file:

```yaml
kumuluzee:
  server:
    graphql:
      defaults:
        offset: 0
        limit: 20
```
Settings default to offset 0 and limit 20.

### Using sorting/filtering without pagination
If your entity does not need pagination, you can call `GraphQLUtils` function processWithoutPagination().
Function accepts your list as parameter and Sort or Filter object, while returning the same type of list.
JPA version is also supported (you need to pass EntityManager and Class + Sort and/or Filter object).

```java
@GraphQLQuery 
public List<Student> allStudents(@GraphQLArgument(name="sort") Sort sort) {
    return GraphQLUtils.processWithoutPagination(facultyBean.getStudentList(), sort);
}
```

## Querying GraphQL endpoint
This part will explain how to query your graphql endpoint.
In most cases, you need to pass three things:
- query
- OperationName
- variables
 
There are a number of supported ways:

### Using HTTP GET
You can pass the following parameters as get parameters. 
If you will only pass one query, you can omit naming and operationName.
Variables are also optional if you do not use them in your query.
```
HTTP GET localhost:8080/graphql?operationName=query1&variables={}&query=query query1 { allStudents { result { name surname } } }
HTTP GET localhost:8080/graphql?&query= { allStudents { result { name surname } } }
```

### Using HTTP POST
This is almost the same as the above method with get request. The only difference is passing parameters in body as JSON.
```json
HTTP POST localhost:8080/graphql
Header: Content-Type: application/json
Post data: 
{
	"query": "query query1 { allStudents { result { name surname } } }",
	"operationName": "query1",
	"variables": {}
}
```

Optional parameters can also be omitted here.

### Using HTTP POST with different Content-Type
You can also use a application/graphql as content type. 
If that header is present, the post body will be treated as graphql query string.

```
HTTP POST localhost:8080/graphql
Header: Content-Type: application/graphql
Post data: 
query query1 { 
  allStudents { 
    result { 
      name 
      surname 
    } 
  } 
} 
```

### Priority
If you send a post request and add get parameters, get parameters will be prioritized.

## Adding GraphiQL (a GraphQL UI)

GraphiQL is a tool, which helps you to test your graphql endpoint. 
It is like Postman for graphql.
You write your query, parameters and graphiql will send the request. 
It also checks your query syntax and allows you to explore your schema graphically.
More information can be found [here](https://github.com/graphql/graphiql).

If you want to include GraphiQL to your project, include the following dependency:
  
```xml
<dependency>
    <groupId>com.kumuluz.ee.graphql</groupId>
    <artifactId>kumuluzee-graphql-ui</artifactId>
    <version>${kumuluzee-graphql.version}</version>
</dependency>
```

Dependency will include GraphiQL UI artifacts. If dependency is included to your project, GraphiQL will be disabled in production environment and enabled in all others.
If you want to explicitly enable or disable it, you can do so in the configuration file:
 
```yaml
kumuluzee:
  server:
    graphql:
      ui:
        enabled: true/false
```

After startup GraphQL UI is available at: http://localhost:8080/graphiql.

## Changelog

Recent changes can bwebappewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-graphql/releases).


## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-graphql/blob/master/CONTRIBUTING.md).

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-graphql/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
