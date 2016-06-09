package org.zalando.zhewbacca

import org.specs2.mutable.Specification
import play.api.test.FakeRequest

class TokenInfoConverterSpec extends Specification {

  "TokenInfoConverter" should {
    "extract TokenInfo from request metadata" in {
      import org.zalando.zhewbacca.TokenInfoConverter._

      val tokenInfo = TokenInfo("12345", Scope(Set("uid", "entity.write", "entity.read")), "Bearer", "test-user-uid")
      val request = FakeRequest().withTokenInfo(tokenInfo)

      request.tokenInfo must beEqualTo(tokenInfo)
    }

    "raise exception when TokenInfo not present in request metadata" in {
      import org.zalando.zhewbacca.TokenInfoConverter._

      val request = FakeRequest()

      request.tokenInfo must throwA[RuntimeException]("access token not provided")
    }
  }

}
