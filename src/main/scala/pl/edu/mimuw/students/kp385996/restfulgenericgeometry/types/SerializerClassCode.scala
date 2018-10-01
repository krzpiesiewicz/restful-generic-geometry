package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.types

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import LangEnum.LangEnum

class SerializerClassCode(val nameOfType: String, className: String, code: String, lang: LangEnum)
extends ClassCode(className, code, lang)

object SerializerClassCode {
  
  def apply(nameOfType: String) = {
    val className = s"SerializerOf$nameOfType"
    val code = s"""
|import com.fasterxml.jackson.databind.JsonSerializer
|import com.fasterxml.jackson.databind.SerializerProvider
|import com.fasterxml.jackson.core.JsonGenerator
|
|class $className extends JsonSerializer[$nameOfType] {
|
|  override def serialize(obj: $nameOfType, jgen: JsonGenerator, provider: SerializerProvider) {
|    jgen.writeObject(obj)
|  }
|}""".stripMargin

    new SerializerClassCode(nameOfType, className, code, LangEnum.SCALA)
  }
}