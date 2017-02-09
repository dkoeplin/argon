package argon.analysis

import argon.core._

trait NameApi extends NameExp
trait NameExp extends Staging {
  object nameOf {
    def apply(x: Exp[_]): Option[String] = ctxsOf(x).headOption.flatMap(_.assignedVariable)
  }

  override def userReadable(x: Any): String = x match {
    case e: Exp[_] => nameOf(e) match {
      case Some(name) => name + " (" + super.userReadable(e) + ")"
      case None => super.userReadable(e)
    }
    case _ => super.userReadable(x)
  }
}
