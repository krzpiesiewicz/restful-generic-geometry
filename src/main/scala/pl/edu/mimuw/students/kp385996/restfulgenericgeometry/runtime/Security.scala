package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.runtime

import java.security.Policy
import java.security.ProtectionDomain
import java.security.PermissionCollection
import java.security.Permissions
import java.security.AllPermission

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.net.URL
import java.net.URLClassLoader
import java.security.Permission
import java.lang.reflect.ReflectPermission

object Security {
    
  @throws(classOf[java.util.concurrent.TimeoutException])
  def timedRun[F](timeout: Long)(f: => F): F = {
  
    import java.util.concurrent.{Callable, FutureTask, TimeUnit}
  
    val task = new FutureTask(new Callable[F]() {
      def call() = f
    })
  
    new Thread(task).start() 
  
    task.get(timeout, TimeUnit.MILLISECONDS)
  }
  
  lazy val sandboxSecurityPolicy: SandboxSecurityPolicy = new SandboxSecurityPolicy
  
  def initSandboxSecurityPolicy {
    Policy.setPolicy(sandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }
  
  def createReflectToolBox(loader: PluginClassLoader) = {
    universe.runtimeMirror(loader).mkToolBox()
  }
}

class SandboxSecurityPolicy extends Policy {
  
  override def getPermissions(domain: ProtectionDomain): PermissionCollection = {
    if (isPlugin(domain.getClassLoader)) {
      pluginPermissions
    } else {
      applicationPermissions
    }
  }

  private def isPlugin(classLoader: ClassLoader): Boolean =
    classLoader.isInstanceOf[PluginClassLoaderTrait]

  private def pluginPermissions: PermissionCollection = {
    val permissions = new Permissions
    permissions.add(new RuntimePermission("accessDeclaredMembers"))
    permissions.add(new ReflectPermission("suppressAccessChecks"))
    permissions
  }

  private def applicationPermissions: PermissionCollection = {
    val permissions = new Permissions();
    permissions.add(new AllPermission());
    permissions
  }
}

trait PluginClassLoaderTrait

class PluginClassLoader extends ClassLoader with PluginClassLoaderTrait

class URLPluginClassLoader(urls: Array[URL]) extends URLClassLoader(urls) with PluginClassLoaderTrait
