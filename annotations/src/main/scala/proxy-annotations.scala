package eu.swdev.scala.ts.annotation

import scala.annotation.Annotation

/**
 * Indicates that proxy code should be generated and allows to override the default interop type.
 *
 *  - When applied to a def, val, var then proxy code for accessing the definition is created; the interop
 *    type can be used to override the default interop type for the returned value
 *  - When applied to a parameter of a proxied def or proxied constructor then the default interop type
 *    of the parameter can be overridden
 *
 * This annotation has no effect when applied on classes, objects, or traits.
 */
class Proxy extends Annotation {
  def this(interopType: String) = this()
}

/**
 * Indicates that proxy code for all members should be generated.
 *
 * Can be applied to classes, objects, and traits.
 */
class ProxyMember extends Annotation

/**
 * Indicates that proxy code for a constructor should be generated.
 *
 * Can be applied to classes.
 */
class ProxyConstructor extends Annotation

/**
 * Implies [[ProxyMember]] and [[ProxyConstructor]].
 *
 * Can be applied to classes, objects, and traits.
 */
class ProxyAll extends Annotation

