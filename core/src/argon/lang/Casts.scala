package argon.lang

import argon._
import forge._

trait CastsExp {

}

trait CastsApi {
  implicit class CastOps[A](x: A) {
    @api def to[B:Type](implicit cast: Cast[A,B]): B = cast(x)
  }
}