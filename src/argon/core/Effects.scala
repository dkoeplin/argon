package argon.core

import scala.collection.mutable

trait Effects extends Symbols { this: Staging =>
  var context: List[Sym[_]] = _
  final def checkContext(): Unit = if (context == null) throw new UninitializedEffectContextException()

  case class Dependencies(deps: List[Sym[_]]) extends Metadata[Dependencies] {
    def mirror(f:Tx) = Dependencies(f.tx(deps))
  }
  object depsOf {
    def apply(x: Sym[_]) = metadata[Dependencies](x).map(_.deps).getOrElse(Nil)
    def update(x: Sym[_], deps: List[Sym[_]]) = metadata.add(x, Dependencies(deps))
  }

  case class Effects(
    cold:    Boolean = false,
    simple:  Boolean = false,
    global:  Boolean = false,
    mutable: Boolean = false,
    reads:   Set[Sym[_]] = Set.empty,
    writes:  Set[Sym[_]] = Set.empty
  ) extends Metadata[Effects] {
    def mirror(f: Tx) = this.copy(reads = f.tx(reads), writes = f.tx(writes))
    private def combine(that: Effects, m1: Boolean, m2: Boolean) = Effects(
      cold = this.cold || that.cold,
      simple = this.simple || that.simple,
      global = this.global || that.global,
      mutable = (m1 && this.mutable) || (m2 && that.mutable),
      reads = this.reads union that.reads,
      writes = this.writes union that.writes
    )
    def orElse(that: Effects) = this.combine(that, m1 = false, m2 = false)
    def andAlso(that: Effects) = this.combine(that, m1 = true, m2 = true)
    def andThen(that: Effects) = this.combine(that, m1 = false, m2 = true)
    def star = this.copy(mutable = false) // Pure orElse this

    def isPure = !simple && !global && !mutable && reads.isEmpty && writes.isEmpty
    def isMutable = mutable
    def isIdempotent = !simple && !global && !mutable && writes.isEmpty
    def mayWrite(ss: Set[Sym[_]]) = global || ss.exists { s => writes contains s }
    def mayRead(ss: Set[Sym[_]]) = global || ss.exists { s => reads contains s }
  }
  val Pure    = Effects()
  val Cold    = Effects(cold = true)
  val Simple  = Effects(simple = true)
  val Global  = Effects(global = true)
  val Mutable = Effects(mutable = true)
  def Read(s: Sym[_]) = Effects(reads = Set(s))
  def Read(ss: Set[Sym[_]]) = Effects(reads = ss)
  def Write(s: Sym[_]) = Effects(writes = Set(s))
  def Write(ss: Set[Sym[_]]) = Effects(writes = ss)

  object effectsOf {
    def apply(s: Sym[_]) = metadata[Effects](s).getOrElse(Pure)
    def update(s: Sym[_], e: Effects) = metadata.add(s, e)
  }
  object Effectful {
    def unapply(x: Sym[_]): Option[(Effects,List[Sym[_]])] = {
      val deps = depsOf(x)
      val effects = effectsOf(x)
      if (effects.isPure && deps.isEmpty) None else Some((effects,deps))
    }
  }

  final def isMutable(s: Sym[_]): Boolean = metadata[Effects](s).exists(_.mutable)

  final def effectDependencies(effects: Effects): List[Sym[_]] = if (effects.global) context else {
    val read = effects.reads
    val write = effects.writes
    val accesses = read ++ write  // Cannot read/write prior to allocation

    var unwrittenAccesses = accesses
    // Find most recent write to each accessed memory
    var hazards = mutable.ListBuffer[Sym[_]]()

    val iter = context.iterator
    while (iter.hasNext) {
      iter.next match { case e@Effectful(u,_) =>
        if (u.mayRead(write)) hazards += e    // WAR hazards
        if (unwrittenAccesses.isEmpty) {
          val (written, unwritten) = unwrittenAccesses.partition(u.writes.contains)
          unwrittenAccesses = unwritten
          hazards ++= written                 // *AW hazards
        }
        if (accesses contains e) hazards += e // "AAA" hazards (access after allocate)
      }
    }
    val simpleDep  = if (effects.simple) context.find{case Effectful(u,_) => u.simple } else None
    val globalDep  = context.find{case Effectful(u,_) => u.global }

    hazards.result ++ simpleDep ++ globalDep
  }

  /** Compiler debugging **/
  override def readable(a: Any): String = a match {
    case d: Dependencies => c"${d.deps}"
    case e: Effects =>
      if (e == Pure) "Pure"
      else if (e == Cold) "Cold"
      else if (e == Mutable) "Mutable"
      else if (e == Simple)  "Simple"
      else if (e == Global)  "Global"
      else {
        "(" +
           ((if (e.cold) List(c"cold=${e.cold}") else Nil) ++
            (if (e.simple) List(c"simple=${e.simple}") else Nil) ++
            (if (e.global) List(c"global=${e.global}") else Nil) ++
            (if (e.mutable)  List("mutable") else Nil) ++
            (if (e.reads.nonEmpty) List(c"""reads={${e.reads.map(readable).mkString(",")}}""") else Nil) ++
            (if (e.writes.nonEmpty) List(c"""writes={${e.writes.map(readable).mkString(",")}}""") else Nil)).mkString(", ") + ")"
      }
    case _ => super.readable(a)
  }

}
