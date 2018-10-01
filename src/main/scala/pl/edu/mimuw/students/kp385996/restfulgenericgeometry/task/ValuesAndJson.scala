package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.TypeFactory

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.core.json.JsonGeneratorImpl
import com.fasterxml.jackson.core.JsonFactory

import java.io.IOException

import scala.collection._
import scala.collection.convert.decorateAsScala._
import scala.collection.convert.ImplicitConversionsToJava._

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import geometry.Vectors.Vec

import computation.ExplicitValue
import computation.ExplicitVecValue
import computation.ExplicitScalarValue



abstract class ExplicitValueWrapper {
  type Type
  val explicitValue: ExplicitValue[Type]

  override def toString = explicitValue match {
    case t: ExplicitScalarValue[Type] => t.value.toString
    case v: ExplicitVecValue[Type] => v.value.toString
    case _ => throw new ParsingException("Unknown ExplicitValue case class.")
  }
}

object ExplicitValueWrapper {
  def apply[T](explicitVal: ExplicitValue[T]) = new ExplicitValueWrapper {
    type Type = T
    val explicitValue: ExplicitValue[T] = explicitVal
  }
}

class ExplicitValueWrapperSerializer[T](typeClass: Class[T],
    typeSerializerClassOpt: Option[Class[JsonSerializer[T]]])
    extends JsonSerializer[ExplicitValueWrapper] {
  
  val objectMapper = new ObjectMapper() {
    setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    registerModule(DefaultScalaModule)
       
    typeSerializerClassOpt match {
      case Some(serializerClass) => {
        val module = new SimpleModule
        module.addSerializer(typeClass, serializerClass.newInstance)
        registerModule(module)
      }
      case None => {}
    }
  }
  
  override def serialize(wrapper: ExplicitValueWrapper, jgen: JsonGenerator, provider: SerializerProvider) {
    type T = wrapper.Type
    
    wrapper.explicitValue match {
      case t: ExplicitScalarValue[T] => objectMapper.writeValue(jgen, t.value)
      case v: ExplicitVecValue[T] => {
        jgen.writeStartArray()
        v.value.coords foreach { (c) => objectMapper.writeValue(jgen, c) }
        jgen.writeEndArray()
      }
      case _ => throw new ParsingException("Unknown ExplicitValue case class.")
    }
  }
}

class ExplicitValueMapper[T](typeClass: Class[T],
    typeDeserializerClassOpt: Option[Class[JsonDeserializer[T]]],
    typeSerializerClassOpt: Option[Class[JsonSerializer[T]]])
    extends ObjectMapper {
  {
    val module = new SimpleModule()
    module.addDeserializer(classOf[ExplicitValueWrapper],
        new ExplicitValueWrapperDeserializer[T](typeClass, typeDeserializerClassOpt))
    module.addSerializer(classOf[ExplicitValueWrapper],
        new ExplicitValueWrapperSerializer[T](typeClass, typeSerializerClassOpt))

    registerModule(DefaultScalaModule)
    registerModule(module)
  }

  def readExplicitValue(jnode: JsonNode) = treeToValue(jnode, classOf[ExplicitValueWrapper])
  
  def writeExplicitValue(wrapper: ExplicitValueWrapper): JsonNode = valueToTree(wrapper)
}

class ExplicitValueWrapperDeserializer[T](typeClass: Class[T],
    typeDeserializerClassOpt: Option[Class[JsonDeserializer[T]]])
extends JsonDeserializer[ExplicitValueWrapper] {

  val objectMapper = new ObjectMapper() {
    setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    registerModule(DefaultScalaModule)
    
    typeDeserializerClassOpt match {
      case Some(deserializerClass) => {
        val module = new SimpleModule
        module.addDeserializer(typeClass, deserializerClass.newInstance)
        registerModule(module)
      }
      case None => {}
    }
  }
  val scalarReader = objectMapper.readerFor(typeClass)
  val vecReader = objectMapper.readerFor(TypeFactory.defaultInstance().constructParametricType(classOf[java.util.List[_]], typeClass))

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): ExplicitValueWrapper = {
    val node: JsonNode = jp.getCodec().readTree(jp);

    val explicitVal = {
      constructVec(node) match {
        case Some(v) => new ExplicitVecValue[T](v)
        case None => {
          constructScalar(node) match {
            case Some(t) => new ExplicitScalarValue[T](t)
            case None => null
          }
        }
      }
    }

    if (explicitVal == null)
      null
    else {
      new ExplicitValueWrapper {
        type Type = T
        val explicitValue = explicitVal
      }
    }
  }

  private def constructVec(node: JsonNode): Option[Vec[T]] = {
    try {
      val list: java.util.List[T] = vecReader.readValue(node)

      if (list == null || list.isEmpty())
        None
      else
        Some(Vec(list.asScala))
    } catch {
      case error: IOException => None
    }
  }

  private def constructScalar(node: JsonNode): Option[T] = {
    try {
      val t: T = scalarReader.readValue(node).asInstanceOf[T]
      
      if (t == null)
        None
      else
        Some(t)
    } catch {
      case error: IOException => None
    }
  }
}
