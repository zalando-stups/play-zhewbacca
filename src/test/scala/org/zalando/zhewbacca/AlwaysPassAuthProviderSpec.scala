package org.zalando.zhewbacca

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class AlwaysPassAuthProviderSpec(implicit ee: ExecutionEnv) extends Specification {
  val testTokenInfo = TokenInfo("", Scope.Empty, "token type", "user uid", realm = "/employees")

  "'Always pass' Authorization Provider" should {
    "accept any tokens and scopes and treat them as valid" in {
      val authProvider = new AlwaysPassAuthProvider(testTokenInfo)
      val scope = Scope(Set("any_scope"))
      val token = Some(OAuth2Token("6afe9886-0a0a-4ace-8bc7-fb96920fb764"))

      authProvider.valid(token, scope) must beEqualTo(AuthTokenValid(testTokenInfo)).await
    }
  }

}
