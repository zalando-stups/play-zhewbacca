package org.zalando.zhewbacca

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class RequestValidatorSpec extends Specification {

  val testTokenInfo = TokenInfo("", Scope.Empty, "token type", "user uid", realm = "/employees")

  "Request Validator" should {
    "provide token information when token is valid" in {
      val authProvider = new AuthProvider {
        override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
          Future.successful(AuthTokenValid(testTokenInfo))
      }

      val result = Await.result(RequestValidator.validate(Scope(Set("uid")), FakeRequest(), authProvider), 1.seconds)
      result.isRight must beTrue
      result.right.get must beEqualTo(testTokenInfo)
    }

    "return HTTP status 401 (unauthorized) when token was not provided" in {
      val authProvider = new AuthProvider {
        override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
          Future.successful(AuthTokenEmpty)
      }

      val result = Await.result(RequestValidator.validate(Scope(Set("uid")), FakeRequest(), authProvider), 1.seconds)
      result.isLeft must beTrue
      result.left.get must beEqualTo(Results.Unauthorized)
    }

    "return HTTP status 401 (unauthorized) when token is in valid" in {
      val authProvider = new AuthProvider {
        override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
          Future.successful(AuthTokenInvalid)
      }

      val result = Await.result(RequestValidator.validate(Scope(Set("uid")), FakeRequest(), authProvider), 1.seconds)
      result.isLeft must beTrue
      result.left.get must beEqualTo(Results.Unauthorized)
    }

    "return HTTP status 401 (unauthorized) when Authorization provider has failed" in {
      val authProvider = new AuthProvider {
        override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
          Future.failed(new RuntimeException)
      }

      val result = Await.result(RequestValidator.validate(Scope(Set("uid")), FakeRequest(), authProvider), 1.seconds)
      result.isLeft must beTrue
      result.left.get must beEqualTo(Results.Unauthorized)
    }

    "return HTTP status 403 (forbidden) in case insufficient scopes" in {
      val authProvider = new AuthProvider {
        override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
          Future.successful(AuthTokenInsufficient)
      }

      val result = Await.result(RequestValidator.validate(Scope(Set("uid")), FakeRequest(), authProvider), 1.seconds)
      result.isLeft must beTrue
      result.left.get must beEqualTo(Results.Forbidden)
    }
  }
}
