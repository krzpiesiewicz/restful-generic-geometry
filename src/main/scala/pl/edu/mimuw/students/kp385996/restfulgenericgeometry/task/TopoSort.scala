package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.task

import scala.collection.mutable

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import task.GraphAlgorithms.CycleFound

class Ve[T](val t: T) {
  val edges: mutable.Set[Ve[T]] = mutable.Set()

  def addEdge(target: Ve[T]) {
    edges += target
  }

  var pushed, vis, left = false
}

class Graph[T] {
  val vertices: mutable.Map[T, Ve[T]] = mutable.ListMap()

  def ve(t: T): Ve[T] = {
    vertices.getOrElseUpdate(t, new Ve(t))
  }

  def addEd(t1: T, t2: T) {
    val v1 = ve(t1)
    val v2 = ve(t2)
    v1.addEdge(v2)
  }

  def getVertices = vertices.valuesIterator

  @throws(classOf[CycleFound])
  def topoSort = GraphAlgorithms.topoSort(this)

  override def toString =
    vertices.valuesIterator.foldRight("")(
      (v, str) => { str + s"${v.t} pushed=${v.pushed} vis=${v.vis} left=${v.left} -> ${v.edges.iterator.map((u) => u.t).mkString("", ", ", "")}\n" })
}

object GraphAlgorithms {
  class CycleFound extends Throwable {}

  @throws(classOf[CycleFound])
  def topoSort[T](g: Graph[T]): List[T] = {

    case class Info(var pushed: Boolean, var vis: Boolean, var left: Boolean)

    val stack = mutable.Stack[Ve[T]]()
    var list = mutable.ListBuffer[T]()

    def pushIfAbsent(v: Ve[T]) {
      if (!v.pushed) {
        v.pushed = true
        stack.push(v)
      }
    }

    var cnt = 0
    g.getVertices.foreach { (v) =>
      {
        pushIfAbsent(v)
        cnt += 1
        while (!stack.isEmpty) {
          val currVe: Ve[T] = stack.pop
          if (!currVe.vis) {
            currVe.vis = true
            stack.push(currVe)
            currVe.edges.foreach {
              (v) =>
                {
                  if (v.vis && !v.left)
                    throw new CycleFound
                  pushIfAbsent(v)
                }
            }
          }
          else {
            currVe.left = true
            list += currVe.t
          }
        }
      }
    }
    list.toList
  }
}
