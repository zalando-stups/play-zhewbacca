package org.zalando.zhewbacca

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext

class SecurityRulesRepositorySpec extends Specification with Mockito {

  "SecurityRulesRepository" should {

    "load rules from default file" in {
      val provider = mock[AuthProvider]
      val repository = new SecurityRulesRepository(Configuration(), provider)
      val expectedRule = new ValidateTokenRule("GET", "/foo", Scope(Set("uid", "entity.read"))) {
        override val authProvider: AuthProvider = provider
      }

      repository.get(FakeRequest("GET", "/foo")) must beSome(expectedRule)
    }

    "load rules from custom file" in {
      val provider = mock[AuthProvider]
      val config = Configuration("authorisation.rules.file" -> "security_custom-security.conf")
      val repository = new SecurityRulesRepository(config, provider)
      val expectedRule = new ValidateTokenRule("POST", "/bar.*", Scope(Set("uid"))) {
        override val authProvider: AuthProvider = provider
      }

      repository.get(FakeRequest("POST", "/bar.*")) must beSome(expectedRule)
    }

    "raise an error when custom file is not available" in {
      val authProvider = mock[AuthProvider]
      val config = Configuration("authorisation.rules.file" -> "this-file-does-not-exist.conf")

      new SecurityRulesRepository(config, authProvider) must
        throwA[RuntimeException]("configuration file this-file-does-not-exist.conf for security rules not found")
    }

    "allow comments in security rules configuration file" in {
      val provider = mock[AuthProvider]
      val config = Configuration("authorisation.rules.file" -> "security_commented.conf")
      val repository = new SecurityRulesRepository(config, provider)
      val expectedRule = new ValidateTokenRule("OPTIONS", "/", Scope(Set("app.resource.read"))) {
        override val authProvider: AuthProvider = provider
      }

      repository.get(FakeRequest("OPTIONS", "/")) must beSome(expectedRule)
    }

    "raise an error when it cannot parse a configuration file" in {
      val authProvider = mock[AuthProvider]
        def config(fileName: String): Configuration = Configuration("authorisation.rules.file" -> fileName)

      new SecurityRulesRepository(config("security_unknown-http-method.conf"), authProvider) must throwA[RuntimeException]
      new SecurityRulesRepository(config("security_no-scopes.conf"), authProvider) must throwA[RuntimeException]
    }

    "return None if there is no configured rules for given request" in {
      val authProvider = mock[AuthProvider]
      val repository = new SecurityRulesRepository(Configuration(), authProvider)

      repository.get(FakeRequest("GET", "/unknown-uri")) must beNone
    }

    "allow explicitly to pass-through or deny a request for a specific URI" in {
      val authProvider = mock[AuthProvider]
      val configuration = Configuration("authorisation.rules.file" -> "security_pass-through.conf")
      val repository = new SecurityRulesRepository(configuration, authProvider)

      repository.get(FakeRequest("GET", "/foo")).get must beAnInstanceOf[ExplicitlyAllowedRule]
      repository.get(FakeRequest("GET", "/bar")).get must beAnInstanceOf[ExplicitlyDeniedRule]
    }

  }

}
