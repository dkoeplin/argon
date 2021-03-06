package argon.codegen.scalagen

import argon.core._
import argon.nodes._
import argon.util.escapeString

trait ScalaGenString extends ScalaCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case StringType => "String"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(c: String) => escapeString(c)
    case _ => super.quoteConst(c)
  }

  def emitToString(lhs: Sym[_], x: Exp[_], tp: Type[_]) = tp match {
    case _ => emit(src"val $lhs = $x.toString")
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case e@ToString(x) => emitToString(lhs, x, e.mT)
    case StringConcat(x,y) => emit(src"val $lhs = $x + $y")
    case StringMk(x,start,delim,end) => emit(src"""val $lhs = ${x}.mkString($start, $delim, $end)""")
    case StringEquals(x,y) => emit(src"val $lhs = $x == $y")
    case StringDiffer(x,y) => emit(src"val $lhs = $x != $y")
    case StringSlice(x,start,end) => emit(src"val $lhs = $x.substring($start,$end);")
    case StringLength(x) => emit(src"val $lhs = $x.length();")
    case _ => super.emitNode(lhs, rhs)
  }



}
