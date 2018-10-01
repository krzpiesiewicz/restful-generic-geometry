package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.runtime

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
import scala.reflect.ClassTag

import Security.timedRun
import java.net.URLClassLoader
import java.io.File
import java.net.URL

class LoaderException(msg: String) extends RuntimeException(msg)

class PluginLoader(val compileTimeout: Long, val loadTimeout: Long) {
  private val pluginLoader = new PluginClassLoader

  private val toolBox = Security.createReflectToolBox(pluginLoader)

  def loadClass(name: String) {
    try pluginLoader.loadClass(name)
    catch {
      case e: Throwable => throw new LoaderException(s"Cannot load class of name='$name'. ${e.getMessage}.")
    }
  }

  def compileObject(objCode: String, objName: String): Any = {
    val code: String = s"""$objCode
    |$objName
    """.stripMargin

    try {
      val f = timedRun(compileTimeout)(() => toolBox.compile(toolBox.parse(code))).apply
      f()
    } catch {
      case e: Throwable => throw new LoaderException(s"Cannot compile the object '$objName'. ${e.getMessage}.")
    }
  }

  def compileClazz(typeCode: String, typeName: String): Class[Any] = {
    val code: String = s"""$typeCode
    |scala.reflect.classTag[$typeName].runtimeClass
    """.stripMargin

    try {
      val f = timedRun(compileTimeout)(() => toolBox.compile(toolBox.parse(code))).apply
      f().asInstanceOf[Class[Any]]
    } catch {
      case e: Throwable => throw new LoaderException(s"Cannot compile the type '$typeName'. ${e.getMessage}.")
    }
  }
}

object PluginLoader {
  
  def URLsFromPaths(pathsToJars: Array[String]): Array[URL] = pathsToJars map { (p) => new File(p).toURI.toURL }
  
  def loadClassesFromJarsWithTheClassLoader(urlClassLoader: URLClassLoader, classesNames: Set[String]): Map[String, Class[_]] =
    (classesNames map { (name) =>
      try {
        (name, urlClassLoader.loadClass(name).asInstanceOf[Class[_]])
      } catch {
        case e: Exception => throw new RuntimeException(s"Cannot load class '$name' from jars: ${urlClassLoader.getURLs}")
      }
    }).toMap
  
  def loadClassesFromJars(pathsToJars: Array[String], classesNames: Set[String]): Map[String, Class[_]] = {
    val jarsURLs = URLsFromPaths(pathsToJars)
    val urlClassLoader = new URLPluginClassLoader(jarsURLs)
    
    loadClassesFromJarsWithTheClassLoader(urlClassLoader, classesNames: Set[String])
  }
  
  def loadClassFromJarsWithTheClassLoader(urlClassLoader: URLClassLoader, className: String): Class[_] =
    loadClassesFromJarsWithTheClassLoader(urlClassLoader, Set(className)).getOrElse(className, null)
  
  def loadClassFromJars(pathsToJars: Array[String], className: String): Class[_] =
    loadClassesFromJars(pathsToJars, Set(className)).getOrElse(className, null)

  def castClassTo[T](clazz: Class[_])(implicit tag: ClassTag[T]): Class[T] =
    try {
      clazz.asInstanceOf[Class[T]]
    } catch {
      case e: Exception => throw new RuntimeException(s"Cannot cast Class[$clazz] to ${tag}")
    }

  def castClassFromMap[T](map: Map[String, Class[_]], className: String)(implicit tag: ClassTag[T]): Class[T] =
    map.get(className) match {
      case Some(clazz) => castClassTo[T](clazz)
      case None => throw new RuntimeException(s"Cannot get class '$className' from map ($map).")
    }
  
  def newInstanceCastFromMap[T](map: Map[String, Class[_]], className: String)(implicit tag: ClassTag[T]): T =
    newInstanceCastTo[T](castClassFromMap[T](map, className))
  
  def newInstanceCastTo[T](clazz: Class[T]): T = {
    val ctor = clazz.getConstructors()(0)
    try {
      ctor.newInstance().asInstanceOf[T]
    } catch {
      case e: Exception => throw new RuntimeException(s"Cannot create instance of class ${clazz.getCanonicalName}. ${e.getMessage}")
    }
  }
}
