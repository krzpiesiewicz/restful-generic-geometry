package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.computation

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import geometry.Vectors.Vec
import types.VectorSpace

class ComputationException(msg: String) extends RuntimeException(msg)

//trait Value[T] {
//  val value: Any
//}

trait ExplicitValue[T] {
  val value: Any
}

case class ExplicitScalarValue[T](val value: T) extends ExplicitValue[T]

case class ExplicitVecValue[T](val value: Vec[T]) extends ExplicitValue[T]

trait Node[T] {
  val value: ExplicitValue[T]
}

trait ScalarNode[T] extends Node[T] {
  val value: ExplicitScalarValue[T]
}

trait VecNode[T] extends Node[T] {
  val value: ExplicitVecValue[T]
}

trait ExplicitValueNode[T] extends Node[T]

case class ExplicitScalarNode[T](val value: ExplicitScalarValue[T]) extends ExplicitValueNode[T] with ScalarNode[T]

case class ExplicitVecNode[T](val value: ExplicitVecValue[T]) extends ExplicitValueNode[T] with VecNode[T]

object ExplicitValueNode {
  def apply[T](value: ExplicitValue[T]) = value match {
    case esv: ExplicitScalarValue[T] => new ExplicitScalarNode(esv)
    case evv: ExplicitVecValue[T] => new ExplicitVecNode(evv)
    case _ => throw new ComputationException("An attempt to create an ExplicitValueNode of unknown type (only possible: Scalar and Vec).")
  }
}

case class Identifier(val name: String) {
  override def toString = name
}

trait IdentifierNode[T] extends Node[T] {
  val id: Identifier
}

case class IdentifierOfScalar[T](id: Identifier)(implicit evaluator: ComputationEvaluator[T])
  extends IdentifierNode[T] with ScalarNode[T] {
  lazy val value = evaluator.getValueForId(id).asInstanceOf[ExplicitScalarValue[T]]
}

case class IdentifierOfVec[T](id: Identifier)(implicit evaluator: ComputationEvaluator[T])
  extends IdentifierNode[T] with VecNode[T] {
  lazy val value = evaluator.getValueForId(id).asInstanceOf[ExplicitVecValue[T]]
}

object IdentifierNode {
  def apply[T](id: Identifier, node: Node[T])(implicit evaluator: ComputationEvaluator[T]) =
    node match {
      case sn: ScalarNode[T] => new IdentifierOfScalar(id)(evaluator)
      case vn: VecNode[T] => new IdentifierOfVec(id)(evaluator)
      case _ => throw new ComputationException("An attempt to create an IdentifierNode of unknown type (only possible: Scalar and Vec).")
    }
}

trait ComputationNode[T] extends Node[T] {
  val value: ExplicitValue[T]
  val args: Seq[Node[T]]
  val eval: () => ExplicitValue[T]
}

case class ScalarComputationNode[T](val args: Seq[Node[T]])(val eval: () => ExplicitScalarValue[T])(implicit evaluator: ComputationEvaluator[T])
  extends ComputationNode[T] with ScalarNode[T] {
  lazy val value: ExplicitScalarValue[T] = evaluator.compute(this).asInstanceOf[ExplicitScalarValue[T]]
}

case class VecComputationNode[T](val args: Seq[Node[T]])(val eval: () => ExplicitVecValue[T])(implicit evaluator: ComputationEvaluator[T])
  extends ComputationNode[T] with VecNode[T] {
  lazy val value: ExplicitVecValue[T] = evaluator.compute(this).asInstanceOf[ExplicitVecValue[T]]
}

object ComputationNode {

  def apply[T](operType: String)(args: List[Node[T]])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) = {
    args.length match {
      case 0 => throw new ComputationException("Arguments list cannot be empty.")
      case 1 => applyUnary[T](operType)(args(0))
      case 2 => applyBinary[T](operType)(args(0), args(1))
      case _ => throw new ComputationException("Arguments list is too long. We support only unary and binary operations.")
    }
  }

  def applyBinary[T](operType: String)(arg1: Node[T], arg2: Node[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) = {
    (arg1, arg2) match {
      case (v1: VecNode[T], v2: VecNode[T]) =>
        getBinaryVecOper(operType, v1, v2)
      case (t1: ScalarNode[T], t2: ScalarNode[T]) =>
        getBinaryScalarOper(operType, t1, t2)
      case (v: VecNode[T], t: ScalarNode[T]) =>
        getBinaryVecAndScalarOper(operType, v, t)
      case (t: ScalarNode[T], v: VecNode[T]) =>
        getBinaryVecAndScalarOper(operType, v, t)
      case _ => throw new ComputationException("Unknown Node[T] types.")
    }
  }

  def applyUnary[T](operType: String)(arg: Node[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) = {
    arg match {
      case v: VecNode[T] => getUnaryVecOper(operType, v)
      case t: ScalarNode[T] => getUnaryScalarOper(operType, t)
    }
  }

  private def getUnaryVecOper[T](operType: String, v: VecNode[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) = {
    operType match {
      case "neg" =>
        new VecComputationNode(List(v))(
          () => ExplicitVecValue(v.value.value.unary_-()(space.algebraicContext.getGroup)))
      case "normalized" =>
        new VecComputationNode(List(v))(
          () => ExplicitVecValue(v.value.value.normalized()(space.algebraicContext.getFieldWithSqrt, space.innerProd, space.norm)))
      case "norm" =>
        new ScalarComputationNode(List(v))(
          () => ExplicitScalarValue(v.value.value.norm()(space.algebraicContext.getFieldWithSqrt, space.innerProd, space.norm)))
      case _ =>
        throw new ComputationException(s"Not known operation '$operType' : Vec[T]")
    }
  }

  private def getBinaryVecOper[T](operType: String, v1: VecNode[T], v2: VecNode[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) = {
    operType match {
      case "+" =>
        new VecComputationNode(List(v1, v2))(
          () => ExplicitVecValue(v1.value.value.+(v2.value.value)(space.algebraicContext.getGroup)))
      case "-" =>
        new VecComputationNode(List(v1, v2))(
          () => ExplicitVecValue(v1.value.value.-(v2.value.value)(space.algebraicContext.getGroup)))
      case "dot" =>
        new ScalarComputationNode(List(v1, v2))(
          () => ExplicitScalarValue((v1.value.value).dot(v2.value.value)(space.algebraicContext.getRing, space.innerProd)))
      case _ =>
        throw new ComputationException(s"Not known operation '$operType' : Vec[T] x Vec[T] --> ?")
    }
  }

  private def getUnaryScalarOper[T](operType: String, t: ScalarNode[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) =
    operType match {
      case "neg" =>
        new ScalarComputationNode(List(t))(
          () => ExplicitScalarValue(space.algebraicContext.getGroup.negate(t.value.value)))
      case "sqrt" =>
        new ScalarComputationNode(List(t))(
          () => ExplicitScalarValue(space.algebraicContext.getFieldWithSqrt.sqrt(t.value.value)))
      case _ =>
        throw new ComputationException(s"Not known operation '$operType' : T --> ?")
    }

  private def getBinaryScalarOper[T](operType: String, t1: ScalarNode[T], t2: ScalarNode[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) =
    operType match {
      case "+" =>
        new ScalarComputationNode(List(t1, t2))(
          () => ExplicitScalarValue(space.algebraicContext.getGroup.plus(t1.value.value, t2.value.value)))
      case "-" =>
        new ScalarComputationNode(List(t1, t2))(
          () => ExplicitScalarValue(space.algebraicContext.getGroup.minus(t1.value.value, t2.value.value)))
      case "*" =>
        new ScalarComputationNode(List(t1, t2))(
          () => ExplicitScalarValue(space.algebraicContext.getRing.times(t1.value.value, t2.value.value)))
      case "/" =>
        new ScalarComputationNode(List(t1, t2))(
          () => ExplicitScalarValue(space.algebraicContext.getField.divide(t1.value.value, t2.value.value)))
      case _ =>
        throw new ComputationException(s"Not known operation '$operType' : T x T --> ?")
    }

  private def getBinaryVecAndScalarOper[T](operType: String, v: VecNode[T], t: ScalarNode[T])(implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T]) =
    operType match {
      case "*" =>
        new VecComputationNode(List(v, t))(
          () => ExplicitVecValue(v.value.value.*(t.value.value)(space.algebraicContext.getRing)))
      case "/" =>
        new VecComputationNode(List(v, t))(
          () => ExplicitVecValue(v.value.value./(t.value.value)(space.algebraicContext.getField)))
      case _ =>
        throw new ComputationException(s"Not known operation '$operType' : Vec[T] x T --> ? or T x Vec[T] --> ?")
    }
}
