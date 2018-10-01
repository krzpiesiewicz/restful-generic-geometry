package json

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.scalactic.source.Position.apply

import org.junit.runner.RunWith

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.JsonDeserializer

import java.net.URLClassLoader

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import runtime.URLPluginClassLoader
import runtime.PluginLoader

import task.ExplicitValueMapper
import task.TaskDeserializer
import task.ExplicitValueWrapperSerializer

import types.TypeClass
import types.ClassesAndCodes
import types.AlgebraicTypes
import types.ClassCode
import types.DeserializerClassCode
import types.AlgebraicCompilerAndLoader
import types.ClassToLoad
import types.SerializerClassCode



@RunWith(classOf[JUnitRunner])
class MappingInRuntime extends FunSuite with Matchers {

  val mapper = new ObjectMapper

  def jNode(str: String): JsonNode = mapper.readTree(str)

  def jParser(str: String): JsonParser = mapper.getFactory.createParser(str)

  //  test("Complext compile test") {
  //    val id = "Complex"
  //    val className = "MyComplex"
  //    val lang = "scala"
  //    val code = """case class MyComplex(val re: Double, val im: Double)"""
  //    val classCode = new ClassCode(className, code, lang)
  //    val deserializerClassCode = DeserializerClassCode(className)
  //    val serializerClassCode = SerializerClassCode(className)
  //
  //    val classMap = AlgebraicCompilerAndLoader.compileAndLoadClasses(
  //      List(classCode, deserializerClassCode, serializerClassCode),
  //      Set(className,
  //          ClassToLoad.classToLoadWithPermission(deserializerClassCode.className),
  //          ClassToLoad.classToLoadWithPermission(serializerClassCode.className)))
  //
  //    val clazz = PluginLoader.castClassFromMap[Any](classMap, className)
  //    val deserializerClazz = PluginLoader.castClassFromMap[JsonDeserializer[Any]](classMap, deserializerClassCode.className)
  //    val serializerClazz = PluginLoader.castClassFromMap[JsonSerializer[Any]](classMap, serializerClassCode.className)
  //
  //    ClassesAndCodes.setClassCode(id, classCode)
  //    AlgebraicTypes.setType(id, TypeClass(clazz, deserializerClazz, serializerClazz))
  //
  //    val typeClass: TypeClass[Any] = AlgebraicTypes.getType(id)
  //    val mapper = new ExplicitValueMapper(typeClass.clazz, typeClass.deserializerClassOption, typeClass.serializerClassOption)
  ////    val mapper = new ExplicitValueMapper(clazz, Some(deserializerClazz), Some(serializerClazz))
  //    val w = mapper.readExplicitValue(jNode("""{"re" : 1.0, "im" : 2}"""))
  //
  //    println(w)
  //  }

  def setTypes {
    val paths = Array("compiler/tmp2/classes.jar")
    val urls = PluginLoader.URLsFromPaths(paths)

    val urlClassLoader = new URLClassLoader(urls)

    val deserializerClazz = PluginLoader.loadClassFromJarsWithTheClassLoader(urlClassLoader, "DeserializerOfMyComplex").asInstanceOf[Class[JsonDeserializer[_]]]
    val serializerClazz = PluginLoader.loadClassFromJarsWithTheClassLoader(urlClassLoader, "SerializerOfMyComplex").asInstanceOf[Class[JsonSerializer[_]]]
    val clazz = PluginLoader.loadClassFromJars(paths, "MyComplex").asInstanceOf[Class[_]]

    AlgebraicTypes.setType("Complex", TypeClass(clazz, deserializerClazz, serializerClazz))
  }

  test("Complext simple test") {
    setTypes

    val typeClass: TypeClass[Any] = AlgebraicTypes.getType("Complex")

    val mapper = new ExplicitValueMapper(typeClass.clazz, typeClass.deserializerClassOption, typeClass.serializerClassOption)
    val w = mapper.readExplicitValue(jNode("""{"re" : 1.0, "im" : 2}"""))

    println(w)
  }

  test("Complext test") {
    val paths = Array("compiler/tmp2/classes.jar")
    val urls = PluginLoader.URLsFromPaths(paths)

    val urlClassLoader = new URLClassLoader(urls)
    val urlPluginClassLoader = new URLPluginClassLoader(urls)

    val deserializerClazz = PluginLoader.loadClassFromJarsWithTheClassLoader(urlPluginClassLoader, "DeserializerOfMyComplex").asInstanceOf[Class[JsonDeserializer[_]]]
    val serializerClazz = PluginLoader.loadClassFromJarsWithTheClassLoader(urlPluginClassLoader, "SerializerOfMyComplex").asInstanceOf[Class[JsonSerializer[_]]]

    val clazz = PluginLoader.loadClassFromJarsWithTheClassLoader(urlPluginClassLoader, "MyComplex").asInstanceOf[Class[_]]

    val typeClass = TypeClass(clazz, deserializerClazz, serializerClazz)
    AlgebraicTypes.setType("Complex", typeClass)

    //    val paths = Array("compiler/tmp2/classes.jar")
    //    val urls = PluginLoader.URLsFromPaths(paths)

    val ctxtClass = PluginLoader.loadClassFromJarsWithTheClassLoader(urlPluginClassLoader, "ComplexField").asInstanceOf[Class[Any]]
    val ctxtObj = PluginLoader.newInstanceCastTo(ctxtClass)

    AlgebraicTypes.setAlgebraicContextClass("Complex", "ComplexField", typeClass, ctxtObj)

    val jp = jParser("""
|{
|  "Context": "ComplexField",
|  "$c": {"*": ["$a", "$b"]},
|  "$a": {"re": 3, "im": 3},
|  "$b": {"re": -1, "im": 4},
|  "Res": "$c"
|}
""".stripMargin)

    val taskDes = new TaskDeserializer
    val task = taskDes.deserialize(jp, null)

    println(task.getRes)
  }
}