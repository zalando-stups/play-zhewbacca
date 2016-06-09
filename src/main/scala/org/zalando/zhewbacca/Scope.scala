package org.zalando.zhewbacca

case class Scope private[zhewbacca] (names: Set[String]) {

  private val nonEmptyNames = names.filterNot(_.trim.isEmpty)

  def in(that: Scope): Boolean = {
    nonEmptyNames.intersect(that.names) == nonEmptyNames
  }
}

object Scope {
  val Default = Scope(Set("uid"))
  val Empty = Scope(Set(""))
}
