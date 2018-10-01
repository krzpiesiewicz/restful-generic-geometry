# RESTful Generic Geometry
RESTful API for generic geometry calculations of my own design, written in Scala + Spring + Jackson. I was working on the project within Web Services course at MIMUW in 2018.

The API's calculating features are based on [geometry.Algebra.scala](https://github.com/krzpiesiewicz/restful-generic-geometry/blob/master/src/main/scala/pl/edu/mimuw/students/kp385996/restfulgenericgeometry/geometry/Algebra.scala) and [geometry.Vectors.scala](https://github.com/krzpiesiewicz/restful-generic-geometry/blob/master/src/main/scala/pl/edu/mimuw/students/kp385996/restfulgenericgeometry/geometry/Vectors.scala). There are algebraic traits describing groups, rings, fields, spaces, inner proucts, norms and default implicit classes based on scala.math.

Contents:
1. [Basics](https://github.com/krzpiesiewicz/restful-generic-geometry#basics)
   - [Control fields](https://github.com/krzpiesiewicz/restful-generic-geometry#control-fields)
   - [Identifiers' fields](https://github.com/krzpiesiewicz/restful-generic-geometry#identifiers-fields)
   - [Explicit values](https://github.com/krzpiesiewicz/restful-generic-geometry#explicit-values)
   - [Result template](https://github.com/krzpiesiewicz/restful-generic-geometry#result-template)
   - [Operations](https://github.com/krzpiesiewicz/restful-generic-geometry#operations)
   - [List of operations](https://github.com/krzpiesiewicz/restful-generic-geometry#list-of-operations)
2. [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry#defining-own-types-operation-contexts-inner-products-norms)
   - [Creating own inner products and norms](https://github.com/krzpiesiewicz/restful-generic-geometry#creating-own-inner-products-and-norms)
   - [Creating own types](https://github.com/krzpiesiewicz/restful-generic-geometry#Creating-own-types)
   - [Creating own operation contexts](https://github.com/krzpiesiewicz/restful-generic-geometry#creating-own-operation-contexts)
   - [Using own types and context in task request](https://github.com/krzpiesiewicz/restful-generic-geometry#using-own-types-and-context-in-task-request)
   - [Creating custom serialization and deserialization](https://github.com/krzpiesiewicz/restful-generic-geometry#creating-custom-serialization-and-deserialization)
3. [Security](https://github.com/krzpiesiewicz/restful-generic-geometry#security)
   - [Denying access to network and file descriptors](https://github.com/krzpiesiewicz/restful-generic-geometry#denying-access-to-network-and-file-descriptors)
   - [Canceling task with simple operation lasting too long](https://github.com/krzpiesiewicz/restful-generic-geometry#canceling-task-with-simple-operation-lasting-too-long)
   - [Remark](https://github.com/krzpiesiewicz/restful-generic-geometry#remark)

## Basics
The basic service is calculating algebraic tasks sent to server as JSON (`http://127.0.0.1:1235/task`, `Content-Type: application/json`).

Such a request can have 3 kinds of fields:
1. Control fields (`"Type"`, `"Context"`, `"InnerProduct"`, `"Norm"`).
2. Identifiers' fields (string starting with `'$'` character; e.g. `"$v1"`).
3. Result template field `"Res"`.

Every request has to contain fields: 
- `"Type"` or `"Context"`,
- `"Res"`

### Control fields
`"Type"` forces service to use certain type like: `Byte`, `Short`, `Int`, `Long`, `Float`, `Double` and own types declared by client. The `"Context"` specifies a nondefault operation object (e.g. client's group, ring, field) that should be used during calculating.
`"InnerProduct"`, `"Norm"` are described in [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry#defining-own-types-operation-contexts-inner-products-norms).

### Identifiers' fields
An identifier is a string starting with `'$'` character (e.g. `"$v1"`). Its value could be given explicitly (e.g. `"a": 5`), a link to other identifier (e.g. `"$a": "$b"`) or an operation (e.g. `"$a": {"+": [5, 7]}`)

### Explicit values
There are two defaultly supported explicit values: scalars and vectors. Vector is a simple list of scalars. E.g. for fractional types like `"Double"` or `"Float"` values `5.0`, `1`, `3.14` are scalars and `[5.0, 1, 3.14]` is a vector.
To create own manners of defining explicit values see [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry#defining-own-types-operation-contexts-inner-products-norms).

### Result template
The value of `"Res"` is a JSONlike result template, where every identifier given as a JSON value is replaced by the value of the identifier. See examples below.

### Operations
An operation is a JSON node with one field (the name/symbol of the operation) with a value being a list of arguments for this operation (e.g. `{"+": [5, 7]}` is a sum of 5 and 7, `{"-": [[1.0, 1.0], [1.5, 2.1]]}` is a difference of vectors `[1.0, 1.0]` and `[1.5, 2.1]`).

Operation arguments could be explicit values, identifiers or operation. See examples below:

Request 1:
```javascript
{
  "Type": "Double",
  "$v1": {"+": [[1.0, 1.0], [1.5, 2.1]]},
  "$v2": {"*": [[1, 3], 5]},
  "$dot": {"dot": ["$v1", "$v2"]},
  "$nv2": {"normalized": ["$v2"]},
  "Res": {"v1": "$v1", "v2": "$v2", "dot": "$dot", "nv2": "$nv2"}
}
```
Result for request 1:
```javascript
{
  "v1": [2.5, 3.1],
  "v2": [5.0, 15.0],
  "dot": 59.0,
  "nv2": [0.3162277660168379, 0.9486832980505138]
}
```

2. Request 2 (nested operations):
```javascript
{
  "Type": "Double",
  "$res": {"dot": [
                    {"+": [
                            [1.0, 1.0],
                            [1.5, 2.1]
                          ]
                    },
                    {"*": [
                            [1, 3],
                            5
                          ]
                    }
                  ]
          },
  "Res": "$res"
}
```
Result for request 1
```javascript
59.0
```

### List of operations
Binary operations:
   1. `T x T --> T`
      - `"+"`, `"-"` (if an `AlgebraicGroup[T]` given),
      - `"*"` (if an `AlgebraicRing[T]` given),
      - `"/"`, `"inversed"` (if an `AlgebraicField[T]` given),
      - `"sqrt"` (if an `AlgebraicFieldWithSqrt[T]` given)
   2. `Vec[T] x Vec[T] --> Vec[T]`
      - `"+"`, `"-"` (if an `AlgebraicGroup[T]` given),
   3. `Vec[T] x Vec[T] --> T`
      - `"dot"` (if an `AlgebraicRing[T]` given)
   4. `Vec[T] x T --> Vec[T]` or `T x Vec[T] --> Vec[T]`
      - `"*"` (if an `AlgebraicRing[T]` given),
      - `"/"` (if an `AlgebraicField[T]` given)

Unary operations:
   1. `T --> T`
      - `"neg"` (if an `AlgebraicGroup[T]` given),
      - `"sqrt"` (if an `AlgebraicFieldWithSqrt[T]` given)
   2. `Vec[T] --> Vec[T]`
      - `"neg"` (if an `AlgebraicGroup[T]` given),
      - `"normalized"` (if an `AlgebraicFieldWithSqrt[T]` given)
   3. `Vec[T] --> T`
      - `"norm"` (if an `AlgebraicFieldWithSqrt[T]` given)
   
## Defining own types, operation contexts, inner products, norms.
You can send to server codes written in Scala or Java.
### Creating own inner products and norms
The default InnerProduct is the standard inner product and the default Norm is the Euclidean norm.
Look how to create the manhatan (taxicab) norm in Scala: `http://127.0.0.1:1235/norm/set?id=manhatanNorm&className=ManhatanNorm&lang=scala`
```scala
import pl.edu.mimuw.students.kp385996.genericgeometry.geometry.Algebra._
import pl.edu.mimuw.students.kp385996.genericgeometry.geometry.Vectors._

class ManhatanNorm extends Norm {
  import AlgebraicFieldWithSqrt.implicits._
      
  def norm[T] = (field: AlgebraicFieldWithSqrt[T]) => {
    (innerProd: InnerProduct) => (v: Vec[T]) => {
      implicit val _: AlgebraicFieldWithSqrt[T] = field
 
      v.coords.foldLeft(field.zero)((sum: T, c: T) => sum + (c * c).sqrt)
    }
  }
}
```

and how to use it:
```javascript
{
  "Type": "Double",
  "Norm": "manhatanNorm",
  "$n": {"norm": [[1.0, 1.0]]},
  "Res": "$n"
}
```

result:
```javascript
2.0
```
### Creating own types
in Scala: `http://127.0.0.1:1235/type/set?id=Complex&className=MyComplex&lang=scala`
```scala
case class MyComplex(val re: Double, val im: Double)
```

in Java: `http://127.0.0.1:1235/type/set?id=Complex&className=MyComplex&lang=java`
```java
class MyComplex {

  public final double re;
  public final double im;
  
  public MyComplex() {
    re = im = 0.0;
  }
  
  public MyComplex(double r, double i) {
    re = r; im = i;
  }
}
```

### Creating own operation contexts
in Scala: `http://127.0.0.1:1235/context/set?contextID=ComplexField&typeID=Complex&className=ComplexField&lang=scala`
```scala
import pl.edu.mimuw.students.kp385996.genericgeometry.geometry.Algebra._
import pl.edu.mimuw.students.kp385996.genericgeometry.geometry.Vectors._

class ComplexField extends AlgebraicField[MyComplex] {
  type T = MyComplex

  val zero = new MyComplex(0, 0)
  val one = new MyComplex(1, 0)

  def plus(x: T, y: T) = new MyComplex(x.re + y.re, x.im + y.im)
  def minus(x: T, y: T) = new MyComplex(x.re - y.re, x.im - y.im)

  def times(x: T, y: T) = new MyComplex(x.re * y.re - x.im * y.im, x.re * y.im + x.im * y.re)
  def divide(x: T, y: T) = new MyComplex((x.re * y.re + x.im * y.im) / (x.im * x.im + y.im * y.im), (x.im * y.re - x.re * y.im) / (x.im * x.im + y.im * y.im))
}
```

in Java it is similar :smirk:

### Using own types and context in task request
```javascript
{
  "Context": "ComplexField",
  "$c": {"*": ["$a", "$b"]},
  "$a": {"re": 3, "im": 3},
  "$b": {"re": -1, "im": 4},
  "Res": "$c"
}
```
result:
```javascript
{
  "re": -15,
  "im": 9.0
}
```

### Creating custom serialization and deserialization
in Scala: `http://127.0.0.1:1235/type/set?id=Complex&className=MyComplex&lang=scala`
```scala
import com.fasterxml.jackson.annotation._

case class MyComplex(@JsonProperty("Re") val re: Double, @JsonProperty("Im") val im: Double)
```

in Java: `http://127.0.0.1:1235/type/set?id=Complex&className=MyComplex&lang=java`
```java
import com.fasterxml.jackson.annotation.*;

class MyComplex {

  @JsonProperty("Re")
  public final Double re;
  
  @JsonProperty("Im")
  public final Double im;
  
  public MyComplex() {
    re = im = 0.0;
  }
  
  public MyComplex(double r, double i) {
    re = r; im = i;
  }
}
```

Then we use `"Re"` and `"Im"` in task requests:
```javascript
{
  "Context": "ComplexField",
  "$c": {"*": ["$a", "$b"]},
  "$a": {"Re": 3, "Im": 3},
  "$b": {"Re": -1, "Im": 4},
  "Res": "$c"
}
```

result:
```javascript
{
  "Re": -15,
  "Im": 9.0
}
```
## Security
Thanks to java security policy I have managed to implement blocking some types of attacks.

### Denying access to network and file descriptors
```scala
import pl.edu.mimuw.students.kp385996.restfulgenericgeometry.geometry.Vectors._
import java.io._

class ManhatanNorm extends Norm {
  import AlgebraicFieldWithSqrt.implicits._
      
  def norm[T] = (field: AlgebraicFieldWithSqrt[T]) => {
    (innerProd: InnerProduct) => (v: Vec[T]) => {
      implicit val _: AlgebraicFieldWithSqrt[T] = field
      
      val w = new FileWriter(new File("EvilManhatanNorm.txt"))
      w.write("HAHAHAAAAAAAaaaaa")
      w.flush
      w.close
      
      v.coords.foldLeft(field.zero)((sum: T, c: T) => sum + (c * c).sqrt)
    }
  }
}
```
When we run task:
```javascript
{
  "Type": "Double",
  "Norm": "evilManhatanNorm",
  "$n": {"norm": [[1.0, 1.0]]},
  "Res": "$n"
}
```
, we get:
```javascript
{
  "timestamp": 1538412219853,
  "status": 403,
  "error": "Forbidden",
  "message": "java.security.AccessControlException: access denied (\"java.io.FilePermission\" \"EvilManhatanNorm.txt\" \"write\")",
  "path": "/task"
}
```

### Canceling task with simple operation lasting too long
`http://127.0.0.1:1235/context/set?contextID=SlowComplexField&typeID=Complex&className=ComplexField&lang=scala`
```scala
import pl.edu.mimuw.students.kp385996.restfulgenericgeometry.geometry.Algebra._
import pl.edu.mimuw.students.kp385996.restfulgenericgeometry.geometry.Vectors._

class ComplexField extends AlgebraicField[MyComplex] {
  type T = MyComplex

  val zero = new MyComplex(0, 0)
  val one = new MyComplex(1, 0)

  def plus(x: T, y: T) = new MyComplex(x.re + y.re, x.im + y.im)
  def minus(x: T, y: T) = new MyComplex(x.re - y.re, x.im - y.im)

  def times(x: T, y: T) = {
    Thread.sleep(2000)
    new MyComplex(x.re * y.re - x.im * y.im, x.re * y.im + x.im * y.re)
  }
  def divide(x: T, y: T) = new MyComplex((x.re * y.re + x.im * y.im) / (x.im * x.im + y.im * y.im), (x.im * y.re - x.re * y.im) / (x.im * x.im + y.im * y.im))
}
```

when we run task:
```javascript
{
  "Context": "SlowComplexField",
  "$c": {"*": ["$a", "$b"]},
  "$a": {"re": 3, "im": 3},
  "$b": {"re": -1, "im": 4},
  "Res": "$c"
}
```

we get:
```javascript
{
  "timestamp": 1538412672750,
  "status": 403,
  "error": "Forbidden",
  "message": "Single operation lasts too long (more than 100 ms).",
  "path": "/task"
}
```

### Remark
Of course the above security tricks are too weak. For instance, there should be:
- denying creating threads (editing policy of ThreadGroup could help),
- preventing alocating too much memory.
