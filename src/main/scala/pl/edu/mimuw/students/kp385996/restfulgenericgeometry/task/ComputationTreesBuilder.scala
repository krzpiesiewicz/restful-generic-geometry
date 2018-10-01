package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import scala.collection.mutable

import scala.collection._
import scala.collection.convert.decorateAsScala._

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import computation.Identifier
import computation.Node
import computation.ComputationNode
import types.VectorSpace
import computation.ComputationEvaluator
import computation.ScalarNode
import computation.VecNode
import computation.IdentifierOfScalar
import computation.IdentifierOfVec
import computation.IdentifierNode
import computation.ExplicitValueNode
import computation.ExplicitValue

import task.GraphAlgorithms.CycleFound

import IdentifierInTask.createIdentifier

class ComputationTreesBuilder[T](implicit space: VectorSpace[T], evaluator: ComputationEvaluator[T],
    valueMapper: ExplicitValueMapper[T]) {

  private val idBuildMap = mutable.Map[Identifier, NodeBuilder]()

  @throws(classOf[ParsingException])
  def buildTrees(map: Map[Identifier, JsonNode]): Map[Identifier, Node[T]] = {

    val sortedIds = try {
      topoSortedIds(map)
    } catch {
      case _: CycleFound => throw new ParsingException("There is a cycle in identifiers dependency graph.")
    }

    sortedIds.foreach { (id) =>
      {
        val jnode = map.get(id) match {
          case Some(jn) => jn
          case None => throw new RuntimeException(s"TopoSort result list contains an unknown id = $id")
        }
        idBuildMap += (id -> NodeBuilder(jnode))
      }
    }

    sortedIds.map { (id) =>
      {
        val nodeBuilder = idBuildMap.get(id) match {
          case Some(bn) => bn
          case None => throw new RuntimeException(s"TopoSort result list contains an unknown id = $id")
        }
        evaluator.setNodeForId(id, nodeBuilder.evaluatedNode)
        (id -> nodeBuilder.evaluatedNode)
      }
    }.toMap
  }

  private trait NodeBuilder {
    val evaluatedNode: Node[T]
  }

  private case class OperationNodeBuilder(val operName: String, children: List[JsonNode]) extends NodeBuilder {
    lazy val args: List[NodeBuilder] = children.map { (jn) => NodeBuilder(jn) }
    lazy val evaluatedNode: ComputationNode[T] = ComputationNode(operName)(args.map { (bn) => bn.evaluatedNode })
  }

  private case class IdentifierNodeBuilder(val identifier: Identifier) extends NodeBuilder {
    lazy val evaluatedNode: IdentifierNode[T] = {
      val bn = idBuildMap.get(identifier) match {
        case Some(bn) => bn
        case None => throw new ParsingException("Reference to an unknown identifier.")
      }
      IdentifierNode(identifier, bn.evaluatedNode)
    }
  }

  private case class ExplicitValueNodeBuilder(val wrapper: ExplicitValueWrapper) extends NodeBuilder {
    lazy val evaluatedNode: ExplicitValueNode[T] = ExplicitValueNode(wrapper.explicitValue.asInstanceOf[ExplicitValue[T]])
  }

  private object NodeBuilder {

    private class NodeBuilderCreated(val nodeBuilder: NodeBuilder) extends Throwable

    def apply(jnode: JsonNode): NodeBuilder = {

      @throws(classOf[NodeBuilderCreated])
      def tryReadExplicitValue(jnode: JsonNode) {
        val wrapper = valueMapper.readExplicitValue(jnode)
        if (wrapper != null)
          throw new NodeBuilderCreated(new ExplicitValueNodeBuilder(wrapper))
      }

      try {
        if (jnode.isTextual) {
          val text = jnode.asText
          try {
            val id = createIdentifier(text)
            throw new NodeBuilderCreated(new IdentifierNodeBuilder(id))
          } catch {
            case e: ParsingException => {
              tryReadExplicitValue(jnode)
              throw new ParsingException(s"The text '$text' cannot be parsed as an explicit value or an identifier. ${e.getMessage}")
            }
          }
        }

        tryReadExplicitValue(jnode)

        val text = "Node cannot be parsed as explicit value"

        if (jnode.isObject) {
          val (operName, node) = {
            val opText = s"$text or operation node. Operation node should contain only one fieldname (name of an operation)."
            
            val iter = jnode.fields
            if (!iter.hasNext)
              throw new ParsingException(opText)
            assert(iter.hasNext)
            val entry = iter.next
            
            if (iter.hasNext)
              throw new ParsingException(opText)
            
            (entry.getKey, entry.getValue)
          }

          if (!node.isArray())
            throw new ParsingException(s"$text. A value in operation node should be a list of arguments for the operation.")

          throw new NodeBuilderCreated(new OperationNodeBuilder(operName, node.elements.asScala.toList))
        }

        throw new ParsingException(s"$text or operation node. It seems to be a missing node.")
      } catch {
        case res: NodeBuilderCreated => res.nodeBuilder
        case e: ParsingException => throw e
      }
    }
  }

  @throws(classOf[CycleFound])
  private def topoSortedIds(map: Map[Identifier, JsonNode]): List[Identifier] = {
    val g = new Graph[Identifier]
    map.keys.foreach { (id) => g.ve(id) }

    def addDependencyEdgesForId(myId: Identifier, jnode: JsonNode) {

      val stack = mutable.Stack[JsonNode]()
      stack.push(jnode)
      while (!stack.isEmpty) {
        val node = stack.pop()

        if (node.isTextual) {
          try {
            val id = new Identifier(node.asText)
            g.addEd(myId, id)
          } catch {
            case _: ParsingException => ()
          }
        }

        if (node.isArray) {
          node.elements.asScala.foreach { (n) =>
            {
              stack.push(n)
            }
          }
        }

        if (node.isObject) {
          node.fields.asScala.foreach { entry =>
            {
              val (name: String, childNode: JsonNode) = (entry.getKey, entry.getValue)
              stack.push(childNode)
            }
          }
        }
      }
    }

    map foreach { case (id, jnode) => addDependencyEdgesForId(id, jnode) }

    g.topoSort
  }
}
