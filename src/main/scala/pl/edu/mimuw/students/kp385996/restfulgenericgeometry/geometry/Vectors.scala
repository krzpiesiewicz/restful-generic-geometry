package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.geometry

import Algebra.AlgebraicField
import Algebra.AlgebraicFieldWithSqrt
import Algebra.AlgebraicGroup
import Algebra.AlgebraicRing

object Vectors {

  trait InnerProduct {
    def prod[T]: AlgebraicRing[T] => ((Vec[T], Vec[T]) => T)
  }

  object InnerProduct {
    implicit object defaultInnerProduct extends InnerProduct {

      def prod[T] = (ring: AlgebraicRing[T]) => {
        (v1: Vec[T], v2: Vec[T]) =>
          (v1.coords, v2.coords).zipped.map({ case (x, y) => ring.times(x, y) })
            .foldRight(ring.zero)((acc, x) => ring.plus(acc, x))
      }
    }
  }

  trait Norm {
    def norm[T]: AlgebraicFieldWithSqrt[T] => (InnerProduct => (Vec[T] => T))
  }

  object Norm {
    
    implicit object defaultNorm extends Norm {
      import AlgebraicFieldWithSqrt.implicits._
      
      def norm[T] = (field: AlgebraicFieldWithSqrt[T]) => {
        (innerProd: InnerProduct) => (v: Vec[T]) => {
          implicit val _: AlgebraicFieldWithSqrt[T] = field
          
          (v dot v).sqrt
        }
      }
    }
    
    object manhatanNorm extends Norm {
      import AlgebraicFieldWithSqrt.implicits._
      
      def norm[T] = (field: AlgebraicFieldWithSqrt[T]) => {
        (innerProd: InnerProduct) => (v: Vec[T]) => {
          implicit val _: AlgebraicFieldWithSqrt[T] = field
          
          v.coords.foldLeft(field.zero)((sum: T, c: T) => sum + (c * c).sqrt)
        }
      }
    }
  }

  class Vec[@specialized(Float, Double, Long, Int, Short, Byte) T](val coords: Vector[T]) {

    override def toString: String = coords.mkString("(", ", ", ")")

    def +(other: Vec[T])(implicit group: AlgebraicGroup[T]): Vec[T] = {
      import AlgebraicGroup.implicits._
      Vec((coords, other.coords).zipped.map({ case (x, y) => x + y }))
    }

    def -(other: Vec[T])(implicit group: AlgebraicGroup[T]): Vec[T] = {
      import AlgebraicGroup.implicits._
      Vec((coords, other.coords).zipped.map({ case (x, y) => x - y }))
    }

    def unary_-()(implicit group: AlgebraicGroup[T]): Vec[T] = {
      import AlgebraicGroup.implicits._
      Vec(coords.map(-_))
    }

    def *(t: T)(implicit ring: AlgebraicRing[T]): Vec[T] = {
      import AlgebraicRing.implicits._
      Vec(coords.map(_ * t))
    }

    def dot(other: Vec[T])(implicit ring: AlgebraicRing[T], innerProd: InnerProduct): T = {
      import AlgebraicRing.implicits._
      innerProd.prod.apply(ring).apply(this, other)
    }

    def /(t: T)(implicit field: AlgebraicField[T]): Vec[T] = {
      import AlgebraicField.implicits._
      this * t.inversed()
    }

    def norm()(implicit field: AlgebraicFieldWithSqrt[T], innerProd: InnerProduct, norm_ob: Norm): T = {
      import AlgebraicFieldWithSqrt.implicits._
      norm_ob.norm.apply(field).apply(innerProd).apply(this)
    }

    def normalized()(implicit field: AlgebraicFieldWithSqrt[T], innerProd: InnerProduct, norm_ob: Norm): Vec[T] = {
      import AlgebraicFieldWithSqrt.implicits._
      this / norm()(field, innerProd, norm_ob)
    }
  }

  object Vec {

    def apply[T](first: T, others: T*)(implicit dummy: DummyImplicit) = new Vec[T]((first :: others.toList).toVector)

    def apply[T](coords: Seq[T]) = new Vec[T](coords.toVector)
  }

  implicit class Scalar[T](t: T) {
    def *(v: Vec[T])(implicit ring: AlgebraicRing[T]): Vec[T] = v * t
    def /(v: Vec[T])(implicit field: AlgebraicField[T]): Vec[T] = v / t

    def *(coords: Seq[T])(implicit ring: AlgebraicRing[T]): Vec[T] = Vec(coords) * t
    def /(coords: Seq[T])(implicit field: AlgebraicField[T]): Vec[T] = Vec(coords) / t
  }

  implicit def toVec[T](coords: Product) = Vec(coords)

  implicit def toVec[T](coords: Seq[T]) = Vec(coords)

  implicit def toVec[T](first: T, others: T*) = Vec(first, others)

  implicit def toVector[T](v: Vec[T]): Vector[T] = v.coords
}
