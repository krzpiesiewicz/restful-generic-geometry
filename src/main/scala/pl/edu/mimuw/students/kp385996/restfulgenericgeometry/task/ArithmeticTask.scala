package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scala.collection._

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import types.VectorSpace
import computation.ComputationEvaluator
import computation.Node
import computation.Identifier
import com.fasterxml.jackson.databind.JsonNode

@JsonDeserialize(using = classOf[TaskDeserializer])
trait ArithmeticTask {
  def eval: Unit
  def getRes: JsonNode
}

class TypedArithmeticTask[T](
  val space: VectorSpace[T],
  val idNodeMap: Map[Identifier, Node[T]],
  val evaluator: ComputationEvaluator[T],
  val resTemp: ResultTemplate[T])
  extends ArithmeticTask {
  
  def eval {
    idNodeMap.valuesIterator.foreach((n) => n.value)
  }
  
  def getRes: JsonNode = resTemp.getJson(evaluator)
}