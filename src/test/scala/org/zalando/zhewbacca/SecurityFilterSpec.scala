package org.zalando.zhewbacca

import javax.inject.{Inject, Provider}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.test.{FakeRequest, PlaySpecification}
import play.api.{Application, Mode}

class SecurityFilterSpec extends PlaySpecification with BodyParsers {

  val testTokenInfo = TokenInfo("", Scope.Empty, "token type", "user uid", realm = "/employees")

  def appWithRoutes: Application = new GuiceApplicationBuilder()
    .in(Mode.Test)
    .bindings(bind[AuthProvider] to new AlwaysPassAuthProvider(testTokenInfo))
    .overrides(
      bind[Router].toProvider[TestRouterProvider])
    .configure(
      "play.http.filters" -> "org.zalando.zhewbacca.TestingFilters",
      "authorisation.rules.file" -> "security_filter.conf")
    .build

  "SecurityFilter" should {

    "allow protected inner action to access token info" in {
      val response = route(appWithRoutes, FakeRequest(GET, "/")).get
      status(response) must beEqualTo(OK)
      contentAsString(response) must beEqualTo(testTokenInfo.tokenType)
    }

    "deny an access when there is no security rule for the reguest is given" in {
      val response = route(appWithRoutes, FakeRequest(GET, "/unprotected-by-mistake")).get
      status(response) must beEqualTo(FORBIDDEN)
    }

  }

}

class TestRouterProvider @Inject() (components: ControllerComponents) extends Provider[Router] {

  import components.{actionBuilder => Action}
  import play.api.routing.sird._

  override def get(): Router = Router.from {
    // test action returning action type. Shows the usage and makes it possible to test basic behaviour
    // security rules described in 'security_filter.conf' file
    case GET(p"/") => Action { request =>
      import TokenInfoConverter._
      Ok(request.tokenInfo.tokenType)
    }

    case GET(p"/unprotected") => Action {
      Ok
    }
  }
}