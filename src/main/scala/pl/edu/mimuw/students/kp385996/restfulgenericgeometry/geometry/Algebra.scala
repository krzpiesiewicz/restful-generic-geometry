package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.geometry

object Algebra {

  trait AlgebraicGroup[T] {
    type Type = T
    val zero: T

    def plus(x: T, y: T): T
    def minus(x: T, y: T): T
    def negate(x: T): T = minus(zero, x)
  }

  object AlgebraicGroup {
    object implicits {
      implicit def element[T](t: T)(implicit group: AlgebraicGroup[T]): AlgebraicGroupElement[T] = new AlgebraicGroupElement[T](t)(group)
    }
  }

  class AlgebraicGroupElement[T](t: T)(implicit group: AlgebraicGroup[T]) {
    import group._

    def +(other: T) = plus(t, other)
    def -(other: T) = minus(t, other)
    def unary_-(): T = negate(t)
  }

  trait AlgebraicRing[T] extends AlgebraicGroup[T] {
    val one: T

    def times(x: T, y: T): T
  }

  object AlgebraicRing {
    object implicits {
      implicit def element[T](t: T)(implicit ring: AlgebraicRing[T]): AlgebraicRingElement[T] = new AlgebraicRingElement[T](t)(ring)
    }
  }

  class AlgebraicRingElement[T](t: T)(implicit ring: AlgebraicRing[T]) extends AlgebraicGroupElement[T](t)(ring) {
    import ring._

    def *(other: T): T = times(t, other)
  }

  trait AlgebraicField[T] extends AlgebraicRing[T] {
    def divide(x: T, y: T): T

    def inversed(x: T): T = divide(one, x)
  }

  object AlgebraicField {
    object implicits {
      implicit def element[T](t: T)(implicit field: AlgebraicField[T]): AlgebraicFieldElement[T] = new AlgebraicFieldElement[T](t)(field)
    }
  }

  class AlgebraicFieldElement[T](t: T)(implicit field: AlgebraicField[T]) extends AlgebraicRingElement[T](t)(field) {
    import field._

    def /(other: T): T = divide(t, other)
    def inversed(): T = field.inversed(t)
  }

  trait AlgebraicFieldWithSqrt[T] extends AlgebraicField[T] {
    def sqrt(x: T): T
  }

  object AlgebraicFieldWithSqrt {
    object implicits {
      implicit def element[T](t: T)(implicit field: AlgebraicFieldWithSqrt[T]): AlgebraicFieldWithSqrtElement[T] =
        new AlgebraicFieldWithSqrtElement[T](t)(field)
    }
  }

  class AlgebraicFieldWithSqrtElement[T](t: T)(implicit field: AlgebraicFieldWithSqrt[T]) extends AlgebraicFieldElement[T](t)(field) {
    def sqrt: T = field.sqrt(t)
  }

  class NumericRing[T](implicit num: Numeric[T]) extends AlgebraicRing[T] {
    val zero: T = num.zero
    val one: T = num.one

    import num._

    def plus(x: T, y: T): T = x + y
    def minus(x: T, y: T): T = x - y
    def times(x: T, y: T): T = x * y
  }

  implicit def numericRing[T](implicit num: Numeric[T]): AlgebraicRing[T] = new NumericRing[T]()(num)

  class IntegralField[T](implicit int: Integral[T]) extends NumericRing[T]()(int) with AlgebraicField[T] {
    import int._

    def divide(x: T, y: T): T = x / y
  }

  implicit def integralField[T](implicit int: Integral[T]): AlgebraicField[T] = new IntegralField[T]()(int)

  class FractionalField[T](implicit frac: Fractional[T]) extends NumericRing[T]()(frac) with AlgebraicField[T] {
    import frac._

    def divide(x: T, y: T): T = x / y
  }

  implicit def fractionalField[T](implicit frac: Fractional[T]): AlgebraicField[T] = new FractionalField[T]()(frac)

  abstract class FractionalFieldWithSqrt[T](implicit frac: Fractional[T])
    extends FractionalField[T]()(frac) with AlgebraicFieldWithSqrt[T] {
  }

  implicit object doubleField extends FractionalFieldWithSqrt[Double] {
    def sqrt(x: Double): Double = math.sqrt(x)
  }

  implicit object floatField extends FractionalFieldWithSqrt[Float] {
    def sqrt(x: Float): Float = math.sqrt(x.asInstanceOf[Double]).asInstanceOf[Float]
  }
}
