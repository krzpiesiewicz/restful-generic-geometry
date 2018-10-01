package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.types

import java.util.concurrent.ConcurrentHashMap
import scala.collection._
import scala.collection.convert.decorateAsScala._

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import LangEnum.LangEnum

object LangEnum extends Enumeration {
  type LangEnum = Value
  val JAVA, SCALA = Value

  implicit def nameToEnum(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()) match {
      case Some(v) => v
      case None => throw new Exception(s"String $name cannot be cast to LangEnum")
    }
}

class ClassCode(val className: String, val code: String, val lang: LangEnum)

object ClassesAndCodes {
  val idsToTypeCodes: mutable.Map[String, ClassCode] = new ConcurrentHashMap().asScala
  
  def getClassCode(id: String) = idsToTypeCodes.getOrElse(id, null)
  
  def setClassCode(id: String, classCode: ClassCode) = idsToTypeCodes += (id -> classCode)
}