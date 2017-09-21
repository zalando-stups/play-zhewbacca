package org.zalando.zhewbacca

import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.{FakeRequest, PlaySpecification}
import play.api.{Application, Mode}

class SecurityFilterSpec extends PlaySpecification with BodyParsers {

  val TestTokenInfo = TokenInfo("", Scope.Empty, "token type", "user uid")

  val routes: PartialFunction[(String, String), Handler] = {
    // test action returning action type. Shows the usage and makes it possible to test basic behaviour
    // security rules described in 'security_filter.conf' file
    case ("GET", "/") => Action { request =>
      import TokenInfoConverter._
      Ok(request.tokenInfo.tokenType)
    }

    case ("GET", "/unprotected") => Action {
      Ok
    }
  }

  def appWithRoutes: Application = new GuiceApplicationBuilder()
    .in(Mode.Test)
    .bindings(bind[AuthProvider] to new AlwaysPassAuthProvider(TestTokenInfo))
    .routes(routes)
    .configure(
      "play.http.filters" -> "org.zalando.zhewbacca.TestingFilters",
      "authorisation.rules.file" -> "security_filter.conf")
    .build

  "SecurityFilter" should {

    "allow protected inner action to access token info" in {
      val response = route(appWithRoutes, FakeRequest(GET, "/")).get
      status(response) must beEqualTo(OK)
      contentAsString(response) must beEqualTo(TestTokenInfo.tokenType)
    }

    "deny an access when there is no security rule for the reguest is given" in {
      val response = route(appWithRoutes, FakeRequest(GET, "/unprotected-by-mistake")).get
      status(response) must beEqualTo(FORBIDDEN)
    }

  }

}