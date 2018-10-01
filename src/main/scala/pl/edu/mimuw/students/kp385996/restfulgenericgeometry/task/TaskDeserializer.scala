package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import scala.collection._
import scala.collection.convert.decorateAsScala._

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ObjectMapper

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import types.AlgebraicTypes
import computation.Identifier
import computation.ComputationEvaluator
import types.VectorSpace

import IdentifierInTask.createIdentifier
import types.VectorSpace

import rest.ForbiddenException

class ParsingException(msg: String) extends RuntimeException(msg)

object IdentifierInTask {

  def createIdentifier(name: String): Identifier = {
    if (!name.substring(1).matches("[a-zA-Z][a-zA-Z0-9_]*") || name.charAt(0) != '$')
      throw new ParsingException(s"'$name' cannot be an identifier; it should start with '$$'.")
    new Identifier(name)
  }
}

class IdentifierDeserializer extends JsonDeserializer[Identifier] {

  @throws(classOf[ParsingException])
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Identifier = {
    val node: JsonNode = jp.getCodec().readTree(jp);

    if (!node.isTextual)
      throw new ParsingException("Identifier must be textual.")
    createIdentifier(node.asText)
  }
}

class TaskDeserializer extends JsonDeserializer[ArithmeticTask] {

  @throws(classOf[ParsingException])
  def onlyOneTextField(container: JsonNode, name: String) = {
    val node = container.findValue(name)
    if (node != null) {
      if (node.isTextual)
        node.asText
      else
        throw new ParsingException(s"Property '$name' is not textual")
    } else ""
  }

  @throws(classOf[ForbiddenException])
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): ArithmeticTask = {
    try {
      val mapper = new ObjectMapper()
      val container: JsonNode = jp.getCodec().readTree(jp)

      val space = {
        val algebraicTypeID = onlyOneTextField(container, "Type")
        val algebraicContextID = onlyOneTextField(container, "Context")

        if (algebraicContextID != "" && algebraicTypeID != "")
          throw new ParsingException("Properties: 'Type' and 'Context' cannot be specified together.")

        if (algebraicContextID == "" && algebraicTypeID == "")
          throw new ParsingException("One of the properties: 'Type' or 'Context' has to be specified.")

        val innerProductID = onlyOneTextField(container, "InnerProduct")
        val normID = onlyOneTextField(container, "Norm")

        val spaceTmp = if (algebraicTypeID != "")
          AlgebraicTypes.getVectorSpaceForType(algebraicTypeID, innerProductID, normID)
        else
          AlgebraicTypes.getVectorSpaceForContext(algebraicContextID, innerProductID, normID)

        spaceTmp.asInstanceOf[VectorSpace[spaceTmp.algebraicContext.Type]]
      }

      type T = space.algebraicContext.Type

      val valueMapper = new ExplicitValueMapper[T](
        space.algebraicContext.typeClass.clazz,
        space.algebraicContext.typeClass.deserializerClassOption,
        space.algebraicContext.typeClass.serializerClassOption)

      implicit val evaluator: ComputationEvaluator[T] = new ComputationEvaluator[T]

      val fields = container.fields.asScala

      val idJnodeMap = fields.foldLeft(Map[Identifier, JsonNode]())(
        (map, e) => {
          val name = e.getKey
          val node = e.getValue
          try {
            val id = createIdentifier(name)
            map + (id -> node)
          } catch {
            case _: Throwable => map
          }
        })

      val idNodeMap = new ComputationTreesBuilder[T]()(space, evaluator, valueMapper).buildTrees(idJnodeMap)

      val resultTemplate = {
        val resNode = container.findValue("Res")
        if (resNode == null)
          throw new ParsingException("Property 'Res' should be specified")
        new ResultsTemplateWithIdentifiers[T](resNode, valueMapper)
      }

      new TypedArithmeticTask[T](
        space,
        idNodeMap,
        evaluator,
        resultTemplate)
    } catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }
}
