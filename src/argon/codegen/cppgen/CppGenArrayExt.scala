package argon.codegen.cppgen

import argon.ops.ArrayExtExp

trait CppGenArrayExt extends CppGenArray {
  val IR: ArrayExtExp
  import IR._

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArrayUpdate(array, i, data) => emit(src"val $lhs = $array.update($i, $data)")
    case MapIndices(size, func, i)   =>
      open(src"val $lhs = Array.tabulate($size){$i => ")
      emitBlock(func)
      close("}")

    case ArrayForeach(array,apply,func,i) =>
      open(src"val $lhs = $array.indices.foreach{$i => ")
      visitBlock(apply)
      emitBlock(func)
      close("}")

    case ArrayMap(array,apply,func,i) =>
      open(src"val $lhs = $array.indices.map{$i => ")
      visitBlock(apply)
      emitBlock(func)
      close("}")

    case ArrayZip(a, b, applyA, applyB, func, i) =>
      open(src"val $lhs = $a.indices.map{$i => ")
      visitBlock(applyA)
      visitBlock(applyB)
      emitBlock(func)
      close("}")

    case ArrayReduce(array, apply, reduce, i, rV) =>
      open(src"val $lhs = $array.reduce{(${rV._1},${rV._2}) => ")
      emitBlock(reduce)
      close("}")

    case ArrayFilter(array, apply, cond, i) =>
      open(src"val $lhs = $array.filter{${apply.result} => ")
      emitBlock(cond)
      close("}")

    case ArrayFlatMap(array, apply, func, i) =>
      open(src"val $lhs = $array.indices.flatMap{$i => ")
      visitBlock(apply)
      emitBlock(func)
      close("}")

    case _ => super.emitNode(lhs, rhs)
  }
}
