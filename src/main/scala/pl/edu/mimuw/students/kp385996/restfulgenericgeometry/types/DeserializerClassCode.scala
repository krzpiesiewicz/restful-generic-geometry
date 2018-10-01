package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.types

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import LangEnum.LangEnum

class DeserializerClassCode(val nameOfType: String, className: String, code: String, lang: LangEnum)
extends ClassCode(className, code, lang)

object DeserializerClassCode {
  
  def apply(nameOfType: String) = {
    val className = s"DeserializerOf$nameOfType"
    val code = s"""
|import com.fasterxml.jackson.core.JsonParser
|import com.fasterxml.jackson.databind.JsonDeserializer
|import com.fasterxml.jackson.databind.ObjectMapper
|import com.fasterxml.jackson.databind.DeserializationContext
|import com.fasterxml.jackson.databind.JsonNode
|import com.fasterxml.jackson.databind.module.SimpleModule
|import com.fasterxml.jackson.module.scala._
|import com.fasterxml.jackson.annotation.PropertyAccessor
|import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
|
|class $className extends JsonDeserializer[$nameOfType] {
|
|  val objectMapper = new ObjectMapper() {
|    setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
|    registerModule(DefaultScalaModule)
|  }
|  val reader = objectMapper.readerFor(classOf[$nameOfType])
|  
|  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): $nameOfType = {
|    val node: JsonNode = jp.getCodec().readTree(jp)
|    reader.readValue(node).asInstanceOf[$nameOfType]
|  }
|}""".stripMargin

    new DeserializerClassCode(nameOfType, className, code, LangEnum.SCALA)
  }
}