# RESTful Generic Geometry
RESTful API for generic geometry calculations of my own design, written in Scala + Spring + Jackson. I was working on the project within Web Services course at MIMUW in 2018.

The API's calculating features are based on [geometry.Algebra.scala](https://github.com/krzpiesiewicz/restful-generic-geometry/blob/master/src/main/scala/pl/edu/mimuw/students/kp385996/restfulgenericgeometry/geometry/Algebra.scala) and [geometry.Vectors.scala](https://github.com/krzpiesiewicz/restful-generic-geometry/blob/master/src/main/scala/pl/edu/mimuw/students/kp385996/restfulgenericgeometry/geometry/Vectors.scala). There are algebraic traits describing groups, rings, fields, spaces, inner proucts, norms and default implicit classes based on scala.math.

Contents:
1. [Basics](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#basics)
   - [Control fields](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#control-fields)
   - [Identifiers' fields](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#identifiers-fields)
   - [Explicit values](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#explicit-values)
   - [Result template](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#result-template)
   - [Operations](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#operations)
   - [List of operations](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#list-of-operations)
2. [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#defining-own-types-operation-contexts-inner-products-norms)
3. [Security](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#security)

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
`"InnerProduct"`, `"Norm"` are described in [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#defining-own-types-operation-contexts-inner-products-norms).

### Identifiers' fields
An identifier is a string starting with `'$'` character (e.g. `"$v1"`). Its value could be given explicitly (e.g. `"a": 5`), a link to other identifier (e.g. `"$a": "$b"`) or an operation (e.g. `{"+": [5, 7]}`)

### Explicit values
There are two defaultly supported explicit values: scalars and vectors. Vector is a simple list of scalars. E.g. for fractional types like `"Double"` or `"Float"` values `5.0`, `1`, `3.14` are scalars and `[5.0, 1, 3.14]` is a vector.
To create own manners of defining explicit values see [Defining own types, operation contexts, inner products, norms](https://github.com/krzpiesiewicz/restful-generic-geometry/new/master?readme=1#defining-own-types-operation-contexts-inner-products-norms).

### Result template
The value of `"Res"` is a JSONlike result template, where every identifier given as a JSON value is replaced by the value of the identifier. See examples below.

### Operations
An operation is a JSON node with one field (the name/symbol of the operation) with a value being a list of arguments for this operation (e.g. `{"+": [5, 7]}` is a sum of 5 and 7, `{"-": [[1.0, 1.0], [1.5, 2.1]]}` is a difference of vectors `[1.0, 1.0]` and `[1.5, 2.1]`).

Operation arguments could be explicit values, identifiers or operation. See examples below:

Request 1:
```
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
```
{
  "v1": [2.5, 3.1],
  "v2": [5.0, 15.0],
  "dot": 59.0,
  "nv2": [0.3162277660168379, 0.9486832980505138]
}
```

2. Request 2 (nested operations):
```
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
```
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

## Security
