package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.ObjectMapper

import scala.collection._
import scala.collection.convert.decorateAsScala._
import scala.collection.mutable.Queue

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import computation.ComputationEvaluator
import computation.Identifier
import task.IdentifierInTask.createIdentifier

trait ResultTemplate[T] {
  def getJson(evaluator: ComputationEvaluator[T]): JsonNode
}

class ResultsTemplateWithIdentifiers[T](container: JsonNode, valueMapper: ExplicitValueMapper[T])
  extends ResultTemplate[T] {

  import ResultsTemplateWithIdentifiers._

  def getJson(evaluator: ComputationEvaluator[T]) = {
    val rootNode: JsonNode = container.deepCopy()
    val queue = Queue[NodeWrapper]()

    def compNode(wrapper: NodeWrapper): JsonNode = {
      val node = wrapper.node
      if (node.isTextual) {
        val id = try {createIdentifier(node.asText)} catch {
          case e: Exception => null
        }
        if (id != null) {
          try {
            val value = evaluator.getValueForId(id)
            val newNode: JsonNode = valueMapper.writeExplicitValue(ExplicitValueWrapper(value))
            wrapper.replaceNode(newNode)
            newNode
          }
          catch {
            case e: Error => {throw e}
          }
        }
        else
          node
      }
      else {
        if (node.isArray) {
          for (i <- 0 to node.size - 1)
            queue += NodeInArray(node.asInstanceOf[ArrayNode], node.get(i), i)
        }
        if (node.isObject) {
          node.fields.asScala.foreach { entry =>
            {
              val (name: String, childNode: JsonNode) = (entry.getKey, entry.getValue)
              queue += NodeWithLabel(node.asInstanceOf[ObjectNode], childNode, name)
            }
          }
        }
        node
      }
    }

    val root = Root(rootNode)
    val resNode = compNode(root)

    while (!queue.isEmpty) {
      val node = queue.dequeue
      compNode(node)
    }
    resNode
  }
}

object ResultsTemplateWithIdentifiers {
  trait NodeWrapper {
    val node: JsonNode

    def replaceNode(newNode: JsonNode)
  }

  case class Root(val node: JsonNode) extends NodeWrapper {
    def replaceNode(newNode: JsonNode) {}
  }

  case class NodeInArray(val arrayNode: ArrayNode, val node: JsonNode, val idx: Int) extends NodeWrapper {
    def replaceNode(newNode: JsonNode) {
      arrayNode.set(idx, newNode)
    }
  }

  case class NodeWithLabel(val parentNode: ObjectNode, val node: JsonNode, val fieldName: String) extends NodeWrapper {
    {
      if (parentNode.findValue(fieldName) == null)
        throw new Exception(s"parentNode should contain child with fieldName '$fieldName'")
    }

    def replaceNode(newNode: JsonNode) {
      parentNode.set(fieldName, newNode)
    }
  }
}
