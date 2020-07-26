package eu.swdev.scala.ts.annotation

import scala.annotation.Annotation

/**
 * Indicates that adapter code should be generated and allows to override the default interoperation type.
 *
 * The interop type is the type that is exposed to the JavaScript side. It is either the type of parameters
 * or of returned values. ScalaTs chooses a default interop type if no interop type is specified.
 *
 *  - When applied to a def, val, var then adapter code for accessing the definition is created; the interop
 *    specifies the type of the returned value.
 *  - When applied to a parameter of an adapted 'def' or constructor then the interop type specifies the type
 *    of the exposed parameter.
 *
 * This annotation has no effect when applied on classes, objects, or traits.
 */
class Adapt extends Annotation {
  def this(interopType: String) = this()
}

/**
 * Indicates that adapter code for all members should be generated.
 *
 * Can be applied to classes, objects, and traits.
 */
class AdaptMembers extends Annotation

/**
 * Indicates that adapter code for a constructor should be generated.
 *
 * Can be applied to classes.
 */
class AdaptConstructor extends Annotation

/**
 * Implies [[AdaptMembers]] and [[AdaptConstructor]].
 *
 * Can be applied to classes, objects, and traits.
 */
class AdaptAll extends Annotation

