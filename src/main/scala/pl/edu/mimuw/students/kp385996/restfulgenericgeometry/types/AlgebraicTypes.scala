package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.types

import java.util.concurrent.ConcurrentHashMap
import scala.collection._
import scala.collection.convert.decorateAsScala._

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import AlgebraicContextEnum.AlgebraicContextEnum

import geometry.Algebra._
import geometry.Vectors._

class TypeException(msg: String) extends Exception(msg)

object AlgebraicContextEnum extends Enumeration {
  type AlgebraicContextEnum = Value
  val Group, Ring, Field, FieldWithSqrt = Value

  def getName(arg: AlgebraicContextEnum): String =
    arg match {
      case Group => "AlgebraicGroup"
      case Ring => "AlgebraicRing"
      case Field => "AlgebraicField"
      case FieldWithSqrt => "FieldWithSqrt"
    }

  def getTypeEnum(obj: Any): AlgebraicContextEnum = {
    if (obj.isInstanceOf[AlgebraicFieldWithSqrt[_]])
      AlgebraicContextEnum.FieldWithSqrt
    else if (obj.isInstanceOf[AlgebraicField[_]])
      AlgebraicContextEnum.Field
    else if (obj.isInstanceOf[AlgebraicRing[_]])
      AlgebraicContextEnum.Ring
    else if (obj.isInstanceOf[AlgebraicGroup[_]])
      AlgebraicContextEnum.Group
    else
      throw new TypeException("Object does not implement any of algebraic context traits.")
  }
}

class TypeClass[T](val clazz: Class[T],
    val deserializerClassOption: Option[Class[JsonDeserializer[T]]],
    val serializerClassOption: Option[Class[JsonSerializer[T]]]) {
  type Type = T
}

object TypeClass {
  def apply[T](clazz: Class[T], deserializerClass: Class[_], serializerClass: Class[_]) = try {
    new TypeClass(clazz,
        Some(deserializerClass.asInstanceOf[Class[JsonDeserializer[T]]]),
        Some(serializerClass.asInstanceOf[Class[JsonSerializer[T]]]))
  } catch {
    case e: Exception => throw new RuntimeException(s"Cannot cast deserializer class '$deserializerClass'  to JsonDeserializer[$clazz]" +
        s" or cannot cast serializer class '$serializerClass' to JsonSerializer[$clazz].")
  }
}

trait AlgebraicContextTrait {
  val typeClass: TypeClass[_]
}

class AlgebraicContext[T](
  val ID: String,
  val algebraicContextEnum: AlgebraicContextEnum,
  val typeClass: TypeClass[T],
  contextObj: Any) extends AlgebraicContextTrait {

  type Type = T

  val ctxtObj = algebraicContextEnum match {
    case AlgebraicContextEnum.Group => contextObj.asInstanceOf[AlgebraicGroup[T]]
    case AlgebraicContextEnum.Ring => contextObj.asInstanceOf[AlgebraicRing[T]]
    case AlgebraicContextEnum.Field => contextObj.asInstanceOf[AlgebraicField[T]]
    case AlgebraicContextEnum.FieldWithSqrt => contextObj.asInstanceOf[AlgebraicFieldWithSqrt[T]]
  }

  def getGroup: AlgebraicGroup[T] = {
    if (ctxtObj.isInstanceOf[AlgebraicGroup[T]])
      ctxtObj.asInstanceOf[AlgebraicGroup[T]]
    else
      throw new TypeException(getCastExceptionMsg(AlgebraicContextEnum.Group))
  }

  def getRing: AlgebraicRing[T] = {
    if (ctxtObj.isInstanceOf[AlgebraicRing[T]])
      ctxtObj.asInstanceOf[AlgebraicRing[T]]
    else
      throw new TypeException(getCastExceptionMsg(AlgebraicContextEnum.Ring))
  }

  def getField: AlgebraicField[T] = {
    if (ctxtObj.isInstanceOf[AlgebraicField[T]])
      ctxtObj.asInstanceOf[AlgebraicField[T]]
    else
      throw new TypeException(getCastExceptionMsg(AlgebraicContextEnum.Field))
  }

  def getFieldWithSqrt: AlgebraicFieldWithSqrt[T] = {
    if (ctxtObj.isInstanceOf[AlgebraicFieldWithSqrt[T]])
      ctxtObj.asInstanceOf[AlgebraicFieldWithSqrt[T]]
    else
      throw new TypeException(getCastExceptionMsg(AlgebraicContextEnum.FieldWithSqrt))
  }

  private def getCastExceptionMsg(targetType: AlgebraicContextEnum): String = {
    val typeName = AlgebraicContextEnum.getName(targetType)
    s"In AlgebraicContext of ID=$ID: cannot cast ctxtObj as instance of $typeName"
  }
}

class VectorSpace[T](val algebraicContext: AlgebraicContext[T], val innerProd: InnerProduct, val norm: Norm) {
}

object AlgebraicTypes {

  private val typesMap: concurrent.Map[String, TypeClass[_]] = new ConcurrentHashMap().asScala

  private val algebraicContextMap: concurrent.Map[String, AlgebraicContext[_]] = new ConcurrentHashMap().asScala

  private val algebraicContextMapForTypes: concurrent.Map[String, AlgebraicContext[_]] = new ConcurrentHashMap().asScala

  private val innerProductsMap: concurrent.Map[String, InnerProduct] = new ConcurrentHashMap().asScala

  private val normsMap: concurrent.Map[String, Norm] = new ConcurrentHashMap().asScala

  private val forbiddenIDs = mutable.Set[String]()

  {
    setDefaultTypeClass("String", new TypeClass(classOf[String], None, None))

    setDefaultTypeAndAlgebraicContext("Byte", "DefaultByte", new TypeClass(classOf[Byte], None, None), integralField[Byte])
    setDefaultTypeAndAlgebraicContext("Short", "DefaultShort", new TypeClass(classOf[Short], None, None), integralField[Short])
    setDefaultTypeAndAlgebraicContext("Int", "DefaultInt", new TypeClass(classOf[Int], None, None), integralField[Int])
    setDefaultTypeAndAlgebraicContext("Long", "DefaultLong", new TypeClass(classOf[Long], None, None), integralField[Long])

    setDefaultTypeAndAlgebraicContext("Float", "DefaultFloat", new TypeClass(classOf[Float], None, None), floatField)
    setDefaultTypeAndAlgebraicContext("Double", "DefaultDouble", new TypeClass(classOf[Double], None, None), doubleField)

    setDefaultInnerProduct("", InnerProduct.defaultInnerProduct)
    setDefaultNorm("", Norm.defaultNorm)
  }

  def getType[T](typeID: String): TypeClass[T] = get(typesMap, typeID).asInstanceOf[TypeClass[T]]

  def getAlgebraicContext[T](algebraicContextID: String): AlgebraicContext[T] =
    get(algebraicContextMap, algebraicContextID).asInstanceOf[AlgebraicContext[T]]

  def getAlgebraicContextForType[T](typeID: String): AlgebraicContext[T] =
    get(algebraicContextMapForTypes, typeID).asInstanceOf[AlgebraicContext[T]]

  def getInnerProduct(productID: String): InnerProduct =
    get(innerProductsMap, productID).asInstanceOf[InnerProduct]

  def getNorm(normID: String): Norm =
    get(normsMap, normID).asInstanceOf[Norm]

  @throws(classOf[TypeException])
  def getVectorSpaceForContext[T](algebraicContextID: String, productID: String, normID: String) = {
    val ctxt: AlgebraicContext[T] = getAlgebraicContext(algebraicContextID)
    if (ctxt == null)
      throw new TypeException(s"Cannot find algebraic context of id='$algebraicContextID'.")
    new VectorSpace(ctxt, getInnerProduct(productID), getNorm(normID)).asInstanceOf[VectorSpace[ctxt.Type]]
  }

  @throws(classOf[TypeException])
  def getVectorSpaceForType[T](typeID: String, productID: String, normID: String) = {
    val ctxt: AlgebraicContext[T] = getAlgebraicContextForType(typeID)
    if (ctxt == null)
      throw new TypeException(s"Cannot find algebraic context for type of id='$typeID'.")
    new VectorSpace(ctxt, getInnerProduct(productID), getNorm(normID)).asInstanceOf[VectorSpace[ctxt.Type]]
  }

  @throws(classOf[TypeException])
  def setType[T](typeID: String, typeClass: TypeClass[T]) {
    if (forbiddenIDs.contains(typeID))
      throw new TypeException(s"Cannot update predefined type or object of id='$typeID'.")
    masterSetTypeClass(typeID, typeClass)
  }

//  @throws(classOf[TypeException])
//  def setAlgebraicContextClass(typeID: String, algebraicContextID: String, contxtobj: Any) {
//    if (forbiddenIDs.contains(algebraicContextID))
//      throw new TypeException(s"Cannot update predefined type or object of id='$algebraicContextID'.")
//    masterSetAlgebraicContextClass(typeID, algebraicContextID, contxtobj)
//  }
  
  @throws(classOf[TypeException])
  def setAlgebraicContextClass[T](typeID: String, algebraicContextID: String, typeClass: TypeClass[T], contxtobj: Any) {
    if (forbiddenIDs.contains(algebraicContextID))
      throw new TypeException(s"Cannot update predefined type or object of id='$algebraicContextID'.")
    setType(typeID, typeClass)
    masterSetAlgebraicContextClass(typeID, algebraicContextID, typeClass, contxtobj)
  }

  @throws(classOf[TypeException])
  def setInnerProduct(productID: String, prodobj: Any) {
    if (forbiddenIDs.contains(productID))
      throw new TypeException(s"Cannot update predefined type or object of id='$productID'.")
    masterSetInnerProduct(productID, prodobj)
  }

  @throws(classOf[TypeException])
  def setNorm(normID: String, normobj: Any) {
    if (forbiddenIDs.contains(normID))
      throw new TypeException(s"Cannot update predefined type or object of id='$normID'.")
    masterSetNorm(normID, normobj)
  }

  private def get(map: Map[String, _], k: String) =
    if (k == null) {
      null
    } else
      map.get(k) match {
        case Some(v) => v
        case None => throw new TypeException(s"'$k' not found.")
      }

  private def masterSetTypeClass[T](typeID: String, typeClass: TypeClass[T]) {
    typesMap += (typeID -> typeClass)
  }

  @throws(classOf[TypeException])
  private def masterSetAlgebraicContextClass[T](typeID: String, algebraicContextID: String, typeClass: TypeClass[T], contxtobj: Any) {
    val enum = AlgebraicContextEnum.getTypeEnum(contxtobj)
    val ctxt = new AlgebraicContext(algebraicContextID, enum, typeClass, contxtobj)
    algebraicContextMap += (algebraicContextID -> ctxt)
    algebraicContextMapForTypes += (typeID -> ctxt)
  }

  private def masterSetInnerProduct(productID: String, prodobj: Any) {
    val obj = try { prodobj.asInstanceOf[InnerProduct] }
    catch {
      case e: Exception =>
        throw new TypeException("Object is not an instance of InnerProduct.")
    }
    innerProductsMap += (productID -> obj)
  }

  private def masterSetNorm(normID: String, normobj: Any) {
    val obj = try normobj.asInstanceOf[Norm]
    catch {
      case e: Exception =>
        throw new TypeException("Object is not an instance of Norm.")
    }
    normsMap += (normID -> obj)
  }

  private def setDefaultTypeClass[T](typeID: String, typeClass: TypeClass[T]) {
    forbiddenIDs += typeID
    masterSetTypeClass(typeID, typeClass)
  }

  @throws(classOf[TypeException])
  private def setDefaultTypeAndAlgebraicContext[T](typeID: String, algebraicContextID: String,
    typeClass: TypeClass[T],
    contxtobj: Any) {

    setDefaultTypeClass(typeID, typeClass)
    forbiddenIDs += algebraicContextID
    masterSetAlgebraicContextClass(typeID, algebraicContextID, typeClass, contxtobj)
  }

  @throws(classOf[TypeException])
  private def setDefaultInnerProduct(productID: String, prodobj: Any) {
    forbiddenIDs += productID
    masterSetInnerProduct(productID, prodobj)
  }

  @throws(classOf[TypeException])
  private def setDefaultNorm(normID: String, normobj: Any) {
    forbiddenIDs += normID
    masterSetNorm(normID, normobj)
  }
}
