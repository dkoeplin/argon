package argon.traversal

import argon.core.Statements

trait ScopeTraversal {
  val IR: Statements
  import IR._

  protected var innerScope: Seq[Int] = _

  private def availableStms = if (innerScope ne null) innerScope else 0 until IR.curNodeId

  private def withInnerScope[A](scope: Seq[Int])(body: => A): A = {
    val saveInner = innerScope
    innerScope = scope
    val result = body
    innerScope = saveInner
    result
  }

  private def scopeSanityCheck[T:Staged](block: Block[T], scope: Seq[Int]): Unit = {
    val observable = block.effectful.map(defOf).map(_.id).distinct // node ids for effect producers
    val actual = observable intersect scope
    val missing = observable diff actual
    if (missing.nonEmpty) {
      val expectedStms = observable.flatMap{s => stmFromNodeId(s)}
      val actualStms = actual.flatMap{s => stmFromNodeId(s)}
      val missingStms = missing.flatMap{s => stmFromNodeId(s)}
      throw new EffectsOrderException(block.result, expectedStms, actualStms, missingStms)
    }
  }

  final protected def traverseScope(scope: Scope[_]): Unit = scope match {
    case block: Block[_] => traverseBlock(block)
    case lambda: Lambda[_] => traverseLambda(lambda)
  }

  final protected def traverseLambda(lambda: Lambda[_]): Unit = __traverseLambda(lambda)(mtyp(lambda.tp))
  private def __traverseLambda[T:Staged](lambda: Lambda[T]): Unit = {
    val inputs = lambda.inputs.map(defOf).map(_.id)
    withInnerScope(availableStms diff inputs) {
      traverseBlock(lambda.block)
    }
  }

  final protected def traverseBlock(block: Block[_]): Unit = __traverseBlock(block)(mtyp(block.tp))
  private def __traverseBlock[T:Staged](block: Block[T]): Unit = {
    val allDependencies = block.result +: block.effectful // Result and scheduling dependencies
    val schedule = IR.getLocalSchedule(availableNodes = availableStms, result = allDependencies.map(_.id))

    scopeSanityCheck(block, schedule)

    // Delay all other symbols as part of the inner scope
    withInnerScope(availableStms diff schedule) {
      schedule.foreach { nodeId =>
        stmFromNodeId(nodeId).foreach(visitStm)
      }
    }
  }

  protected def visitStm(stm: Stm): Unit = stm.rhs.scopes.foreach {blk => traverseScope(blk) }
}