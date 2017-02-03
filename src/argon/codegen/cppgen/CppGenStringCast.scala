package argon.codegen.cppgen

import argon.ops.StringCastExp

trait CppGenStringCast extends CppCodegen {
  val IR: StringCastExp
  import IR._

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]) = rhs match {
    case StringToFltPt(x) => lhs.tp match {
      case DoubleType() => emit(src"val $lhs = std::stof($x);")
      case FloatType()  => emit(src"val $lhs = std::stof($x);")
    }

    case StringToFixPt(x) => lhs.tp match {
      case IntType()  => emit(src"val $lhs = std::stoi($x);")
      case LongType() => emit(src"val $lhs = std::stol($x);")
    }

    case StringToBool(x) => emit(src"val $lhs = $x.toBoolean")

    case _ => super.emitNode(lhs, rhs)
  }
}
