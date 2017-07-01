package argon.codegen.cppgen

import argon.core._
import argon.nodes._
import argon.util.escapeString

trait CppGenString extends CppCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case StringType => "string"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(c: String) => "string(" + escapeString(c) + ")"
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ToString(x) => emit(src"${lhs.tp} $lhs = std::to_string($x);")
    case StringConcat(x,y) => emit(src"${lhs.tp} $lhs = string_plus($x, $y);")
    case StringMk(x,start,delim,end) => 
      emit(src"""${lhs.tp} $lhs = string($start);""")
      open(src"""for (int ${lhs}_i = 0; ${lhs}_i < ${x}.length(); ${lhs}_i++) {""")
        emit(src"${lhs} = ${lhs} + ${x}[${lhs}_i];")
        emit(src"if (${lhs}_i < ${x}.length() - 1) { ${lhs} = ${lhs} + $delim; }")
      close("}")
    case StringEquals(x,y) => emit(src"${lhs.tp} $lhs = $x == $y;")
    case StringDiffer(x,y) => emit(src"${lhs.tp} $lhs = $x != $y;")
    case StringSlice(x,start,end) => emit(src"${lhs.tp} $lhs = $x.substr($start,${end}-${start});")
    case StringLength(x) => emit(src"${lhs.tp} $lhs = $x.length();")
    case _ => super.emitNode(lhs, rhs)
  }

}

