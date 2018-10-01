package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.types

import scala.reflect.ClassTag
import sys.process._
import java.io._
import java.net.URLClassLoader

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import runtime.PluginLoader

import geometry.Algebra
import geometry.Vectors


import LangEnum.LangEnum

class ClassToLoad(val className: String, val classLoaderOpt: Option[(Array[String]) => URLClassLoader])

object ClassToLoad {
  implicit def apply(className: String) = new ClassToLoad(className, None)
  
  def classToLoadWithPermission(className: String) =
    new ClassToLoad(className, Some((paths: Array[String]) => new URLClassLoader(PluginLoader.URLsFromPaths(paths))))
}

import ClassToLoad._

object AlgebraicCompilerAndLoader {

  def compileAndLoadClasses(typesCodes: List[ClassCode], classesToLoad: Set[ClassToLoad]): Map[String, Class[_]] = {
    val idName = s"${this.hashCode}_${System.nanoTime}"
    val dirName = s"compiler/$idName"
    val dir = new File(dirName)

    if (dir.mkdir) {
      val javaPath = s"$dirName/java"
      val scalaPath = s"$dirName/scala"
        
      val javaDir = if (typesCodes.exists((cc) => cc.lang == LangEnum.JAVA)) {
        val f = new File(javaPath)
        f.mkdir
        f
      }
      else
        null
        
      val scalaDir = if (typesCodes.exists((cc) => cc.lang == LangEnum.SCALA)) {
        val f = new File(scalaPath)
        f.mkdir
        f
      }
      else
        null
          
      try {
        typesCodes foreach { (cc: ClassCode) =>
          {
            val path = cc.lang match {
              case LangEnum.JAVA => s"$javaPath/${cc.className}.java"
              case LangEnum.SCALA => s"$scalaPath/${cc.className}.scala"
            }
            val w = new FileWriter(new File(path))
            w.write(cc.code)
            w.flush
            w.close
          }
        }

        val out = new StringBuilder
        val err = new StringBuilder

        val logger = ProcessLogger(
          (o: String) => out.append(o),
          (e: String) => err.append(e))

        val jarName = "classes"
        val returnedCode = (s"compiler/build.sh $idName $jarName") ! logger

        if (returnedCode == 0) {
          val jarFilePath = s"$dirName/$jarName.jar"

          PluginLoader.loadClassesFromJars(Array(jarFilePath), classesToLoad.map((c) => c.className))
//          classesToLoad.foldLeft(Map[String, Class[_]]())((map, classToLoad) =>
//            classToLoad.classLoaderOpt match {
//              case None =>
//                map + (classToLoad.className -> PluginLoader.loadClassFromJars(Array(jarFilePath), classToLoad.className))
//              case Some(urlClassLoaderCreator) => {
//                val urlClassLoader = urlClassLoaderCreator(Array(jarFilePath))
//                map + (classToLoad.className -> PluginLoader.loadClassFromJarsWithTheClassLoader(urlClassLoader, classToLoad.className))
//              }
//            })
        } else
          throw new RuntimeException(s"Compilation failed: $err")

      } finally {
//        s"rm -r $dirName" ! // to jest zło, które wszystko psuło
        if (javaDir != null)
          javaDir.delete
        if (scalaDir!= null)
          scalaDir.delete
      }
    } else
      throw new RuntimeException(s"Cannot create directory '$dirName'.")
  }

  def compileLoadAndCastClassTo[T](typeCodes: List[ClassCode], className: String)(implicit tag: ClassTag[T]): Class[T] =
    PluginLoader.castClassTo[T](compileAndLoadClasses(typeCodes, Set(className)).getOrElse(className, null))

  def compileClassAndCreateInstance[T](typeCodes: List[ClassCode], className: String)(implicit tag: ClassTag[T]): T =
    PluginLoader.newInstanceCastTo[T](compileLoadAndCastClassTo[T](typeCodes, className))
}
