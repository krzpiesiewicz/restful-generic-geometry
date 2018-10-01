package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.rest

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.http.MediaType

import org.springframework.scheduling.annotation.Async

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer

import javax.servlet.http.HttpServletRequest
import java.io.StringWriter
import org.apache.commons.io.IOUtils

import java.security.Policy

import types.AlgebraicCompilerAndLoader
import runtime.PluginLoader
import types.AlgebraicTypes
import types.LangEnum.LangEnum
import types.LangEnum.nameToEnum
import geometry.Vectors.Norm
import geometry.Vectors.InnerProduct
import types.ClassCode
import types.ClassesAndCodes
import types.DeserializerClassCode
import types.SerializerClassCode
import types.TypeClass
import types.TypeClass._
import types.ClassToLoad._

@RestController
class TypesAndObjectsController {

  private def getPlainTextFromRequest(request: HttpServletRequest): String = {
    val writer = new StringWriter
    IOUtils.copy(request.getInputStream, writer, request.getCharacterEncoding)
    writer.toString
  }

  @RequestMapping(value = Array("/type/set"), method = Array(POST), consumes = Array(MediaType.TEXT_PLAIN_VALUE))
  def setType(@RequestParam id: String, @RequestParam className: String, @RequestParam lang: String, request: HttpServletRequest) {
    try {
      val code = getPlainTextFromRequest(request)
      val classCode = new ClassCode(className, code, lang)
      val deserializerClassCode = DeserializerClassCode(className)
      val serializerClassCode = SerializerClassCode(className)

      val classMap = AlgebraicCompilerAndLoader.compileAndLoadClasses(
        List(classCode, deserializerClassCode, serializerClassCode),
        Set(className,
            classToLoadWithPermission(deserializerClassCode.className),
            classToLoadWithPermission(serializerClassCode.className)))
            
      val clazz = PluginLoader.castClassFromMap[Any](classMap, className)
      val deserializerClazz = PluginLoader.castClassFromMap[JsonDeserializer[Any]](classMap, deserializerClassCode.className)
      val serializerClazz = PluginLoader.castClassFromMap[JsonSerializer[Any]](classMap, serializerClassCode.className)

      ClassesAndCodes.setClassCode(id, classCode)
      AlgebraicTypes.setType(id, TypeClass(clazz, deserializerClazz, serializerClazz))
    } catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }

  @RequestMapping(value = Array("/context/set"), method = Array(POST), consumes = Array(MediaType.TEXT_PLAIN_VALUE))
  def setContext(@RequestParam typeID: String, @RequestParam contextID: String, @RequestParam className: String, @RequestParam lang: String, request: HttpServletRequest) {
    try {
      val code = getPlainTextFromRequest(request)
      val contextTypeCode = new ClassCode(className, code, lang)
      val typeClassCode = ClassesAndCodes.getClassCode(typeID)
      val deserializerClassCode = DeserializerClassCode(typeClassCode.className)
      val serializerClassCode = SerializerClassCode(typeClassCode.className)
      
      val classes = AlgebraicCompilerAndLoader.compileAndLoadClasses(
          List(deserializerClassCode, serializerClassCode, contextTypeCode, typeClassCode),
          Set(contextTypeCode.className, typeClassCode.className, deserializerClassCode.className, serializerClassCode.className))
          
      val obj = PluginLoader.newInstanceCastFromMap[Any](classes, contextTypeCode.className)
      val typeClass = PluginLoader.castClassFromMap[Any](classes, typeClassCode.className)
      val deserializerClass = PluginLoader.castClassFromMap[Class[JsonDeserializer[_]]](classes, deserializerClassCode.className)
      val serializerClass = PluginLoader.castClassFromMap[Class[JsonSerializer[_]]](classes, serializerClassCode.className)
//      val obj = AlgebraicCompilerAndLoader.compileClassAndCreateInstance[Any](
//        List(typeClassCode, contextTypeCode, deserializerClassCode),
//        className)
      AlgebraicTypes.setAlgebraicContextClass(typeID, contextID, TypeClass(typeClass, deserializerClass, serializerClass), obj)
    } catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }

  @RequestMapping(value = Array("/innerproduct/set"), method = Array(POST), consumes = Array(MediaType.TEXT_PLAIN_VALUE))
  def setInnerProduct(@RequestParam id: String, @RequestParam className: String, @RequestParam lang: String, request: HttpServletRequest) {
    try {
      val code = getPlainTextFromRequest(request)
      val classCode = new ClassCode(className, code, lang)
      val obj = AlgebraicCompilerAndLoader.compileClassAndCreateInstance[InnerProduct](List(classCode), className)
      AlgebraicTypes.setInnerProduct(id, obj)
    } catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }

  @RequestMapping(value = Array("/norm/set"), method = Array(POST), consumes = Array(MediaType.TEXT_PLAIN_VALUE))
  def setNorm(@RequestParam id: String, @RequestParam className: String, @RequestParam lang: String, request: HttpServletRequest) {
    try {
      val code = getPlainTextFromRequest(request)
      val classCode = new ClassCode(className, code, lang)
      val obj = AlgebraicCompilerAndLoader.compileClassAndCreateInstance[Norm](List(classCode), className)
      AlgebraicTypes.setNorm(id, obj)
    } catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }
}