package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.computation

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import runtime.Security.timedRun

import scala.collection.mutable

class ComputationEvaluator[T] {
  
  import ComputationEvaluator._
  
  val explicitValuesForIds: mutable.Map[Identifier, ExplicitValue[T]] = mutable.HashMap()

  val nodesForIds: mutable.Map[Identifier, Node[T]] = mutable.HashMap()

  def setNodeForId(id: Identifier, node: Node[T]) {
    nodesForIds += (id -> node)
  }

  def compute(node: ComputationNode[T]): ExplicitValue[T] = {
    node.args.foreach((v) => v.value)
    try {
      timedRun(OPERATION_TIMEOUT)(node.eval.apply)
    } catch {
      case e: java.util.concurrent.TimeoutException =>
        throw new ComputationException(s"Single operation lasts too long (more than $OPERATION_TIMEOUT ms).")
    }
  }

  def getValueForId(id: Identifier): ExplicitValue[T] = explicitValuesForIds.get(id) match {
    case Some(value) => value
    case None => {
      val explicitRes = evalNode(id)
      explicitValuesForIds += (id -> explicitRes)
      explicitRes
    }
  }

  def evalNode(id: Identifier): ExplicitValue[T] =
    nodesForIds.get(id) match {
      case Some(en: ExplicitValueNode[T]) => en.value
      case Some(cn: ComputationNode[T]) => compute(cn)
      case Some(in: IdentifierNode[T]) => getValueForId(in.id)
      case Some(n) => throw new ComputationException(s"Not known type of ComputationNode for $id ($n).")
      case None => throw new ComputationException(s"No ComputationNode for $id.")
    }
}

object ComputationEvaluator {
  val OPERATION_TIMEOUT = 100
}
