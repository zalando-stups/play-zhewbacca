package org.zalando.zhewbacca

import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class OAuth2AuthProviderSpec extends Specification {

  "IAM Authorization Provider" should {

    "accept valid token with necessary scope" in {
      val tokenInfo = TokenInfo("311f3ab2-4116-45a0-8bb0-50c3bca0441d", Scope(Set("uid")), "Bearer", userUid = "1234", realm = "/employees")
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(Some(tokenInfo))).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenValid(tokenInfo))
    }

    "accept token which has many scopes" in {
      val tokenInfo = TokenInfo("311f3ab2-4116-45a0-8bb0-50c3bca0441d", Scope(Set("uid", "cn")), "Bearer", userUid = "1234", realm = "/employees")
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(Some(tokenInfo))).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenValid(tokenInfo))
    }

    "reject token when IAM reports it is not valid one" in {
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(None)).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenInvalid)
    }

    "reject token if IAM responses with Token Info for different token" in {
      val tokenInfo = TokenInfo("986c2946-c754-4e58-a0cb-7e86e3e9901b", Scope(Set("uid")), "Bearer", userUid = "1234", realm = "/employees")
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(Some(tokenInfo))).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenInsufficient)
    }

    "reject token with insufficient scopes" in {
      val tokenInfo = TokenInfo("311f3ab2-4116-45a0-8bb0-50c3bca0441d", Scope(Set("uid")), "Bearer", userUid = "1234", realm = "/employees")
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(Some(tokenInfo))).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid", "seo_description.write")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenInsufficient)
    }

    "reject non 'Bearer' token" in {
      val tokenInfo = TokenInfo("311f3ab2-4116-45a0-8bb0-50c3bca0441d", Scope(Set("uid")), "Token", userUid = "1234", realm = "/employees")
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(Some(tokenInfo))).valid(
        Some(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d")),
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenInsufficient)
    }

    "reject empty token" in {
      val request = new OAuth2AuthProvider((token: OAuth2Token) => Future.successful(None)).valid(
        None,
        Scope(Set("uid")))

      Await.result(request, 1.second) must beEqualTo(AuthTokenEmpty)
    }
  }
}
