package org.zalando.zhewbacca

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.http.Status.FORBIDDEN
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{RequestHeader, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class SecurityRuleSpec(implicit ec: ExecutionContext) extends Specification with Mockito {
  sequential

  "ValidateTokenRule" should {
    "be applicable to specific route" in {
      val rule = new ValidateTokenRule("GET", "/api/.*", Scope.Default) {
        override val authProvider: AuthProvider = mock[AuthProvider]
      }

      rule.isApplicableTo(FakeRequest("GET", "/api/foo")) must beTrue
      rule.isApplicableTo(FakeRequest("GET", "/api/foo?a=b")) must beTrue
      rule.isApplicableTo(FakeRequest("GET", "/api/")) must beTrue

      rule.isApplicableTo(FakeRequest("GET", "/api")) must beFalse
      rule.isApplicableTo(FakeRequest("PUT", "/api/foo")) must beFalse
      rule.isApplicableTo(FakeRequest("GET", "/api2")) must beFalse
    }

    "inject TokenInfo into authenticated request" in {
      import TokenInfoConverter._

      val request = FakeRequest("GET", "/")
      val rule = new ValidateTokenRule("GET", "/", Scope.Default) {
        override val authProvider: AuthProvider = {
          val tokenInfo = TokenInfo("", Scope.Default, "token-type", "test-user-id")
          val provider = mock[AuthProvider]
          provider.valid(any[Option[OAuth2Token]], any[Scope]) returns Future.successful(AuthTokenValid(tokenInfo))
        }
      }
      val nextFilter = { request: RequestHeader => Future.successful(Results.Ok(request.tokenInfo.userUid)) }

      contentAsString(rule.execute(nextFilter, request)) must beEqualTo("test-user-id")
    }

    "return error for non-authenticated request" in {
      val request = FakeRequest("GET", "/")
      val rule = new ValidateTokenRule("GET", "/", Scope.Default) {
        override val authProvider: AuthProvider = {
          val provider = mock[AuthProvider]
          provider.valid(any[Option[OAuth2Token]], any[Scope]) returns Future.successful(AuthTokenInvalid)
        }
      }
      val nextFilter = { request: RequestHeader => Future.successful(Results.Ok) }

      status(rule.execute(nextFilter, request)) must beEqualTo(FORBIDDEN)
    }
  }

  "ExplicitlyAllowedRule" should {
    "pass unmodified request to next filter" in {
      val testAttribute: TypedKey[String] = TypedKey("testAttribute")
      val originalRequest = FakeRequest("GET", "/foo").addAttr(testAttribute, "testValue")
      val rule = new ExplicitlyAllowedRule("GET", "/foo")
      val nextFilter = { request: RequestHeader => Future.successful(Results.Ok(request.attrs(testAttribute))) }

      contentAsString(rule.execute(nextFilter, originalRequest)) must beEqualTo("testValue")
    }

    "be applicable to specific request" in {
      val rule = new ExplicitlyAllowedRule("GET", "/foo.*")

      rule.isApplicableTo(FakeRequest("GET", "/foo/bar")) must beTrue
      rule.isApplicableTo(FakeRequest("GET", "/bar/foo")) must beFalse
    }
  }

  "ExplicitlyDeniedRule" should {

    "be applicable to specific request" in {
      val rule = new ExplicitlyDeniedRule("GET", "/foo.*")

      rule.isApplicableTo(FakeRequest("GET", "/foo/bar")) must beTrue
      rule.isApplicableTo(FakeRequest("GET", "/bar/foo")) must beFalse
    }

    "reject a request and respond with 403 HTTP status" in {
      val rule = new ExplicitlyDeniedRule("GET", "/foo")
      val nextFilter = { request: RequestHeader => Future.successful(Results.Ok) }

      status(rule.execute(nextFilter, FakeRequest())) must beEqualTo(FORBIDDEN)
    }

  }

  "DenyAllRule" should {

    "be applicable to all requests" in {
      val rule = new DenyAllRule

      rule.isApplicableTo(FakeRequest()) must beTrue
    }

    "reject a request and respond with 403 HTTP status" in {
      val rule = new DenyAllRule
      val nextFilter = { request: RequestHeader => Future.successful(Results.Ok) }

      status(rule.execute(nextFilter, FakeRequest())) must beEqualTo(FORBIDDEN)
    }

  }

}
