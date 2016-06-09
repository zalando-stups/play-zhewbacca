package org.zalando.zhewbacca

import org.specs2.mutable.Specification

class ScopeTestSpec extends Specification {

  "in method" should {

    "return 'true' if this scope is completely in given scope" in {
      Scope(Set("uid")).in(Scope(Set("uid"))) must beTrue
      Scope(Set("uid", "cn")).in(Scope(Set("uid", "cn"))) must beTrue
      Scope(Set("uid")).in(Scope(Set("uid", "cn"))) must beTrue
      Scope(Set("uid", "cn")).in(Scope(Set("cn", "uid"))) must beTrue
    }

    "return 'false' if this scope is partially in given scope" in {
      Scope(Set("uid", "cn")).in(Scope(Set("uid"))) must beFalse
      Scope(Set("uid", "cn")).in(Scope(Set("cn", "foo"))) must beFalse
    }

    "return 'false' if this scope is not in given scope" in {
      Scope(Set("uid")).in(Scope(Set("cn"))) must beFalse
    }
  }

  "empty scope" should {

    "be in any scope" in {
      Scope.Empty.in(Scope.Empty) must beTrue
      Scope.Empty.in(Scope(Set("uid"))) must beTrue
      Scope.Empty.in(Scope(Set("uid", "another_scope"))) must beTrue
      Scope(Set("uid", "")).in(Scope(Set("uid"))) must beTrue
      Scope(Set("uid", "", "")).in(Scope(Set("uid", ""))) must beTrue
    }

  }

}
