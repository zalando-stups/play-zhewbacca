package org.zalando.zhewbacca

import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import org.zalando.zhewbacca.metrics.NoOpPluggableMetrics
import play.api.http.{DefaultFileMimeTypes, FileMimeTypesConfiguration, Port}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.WsTestClient
import play.api.{Application, Configuration, Mode}
import play.core.server.Server

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

// @see https://www.playframework.com/documentation/2.6.x/ScalaTestingWebServiceClients
class IAMClientSpec extends Specification {
  sequential

  implicit val mimeTypes = new DefaultFileMimeTypes(new FileMimeTypesConfiguration())

  "IAM Client" should {

    "parse response into TokenInfo" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("valid-token-with-uid-scope.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(Some(TokenInfo(
            "311f3ab2-4116-45a0-8bb0-50c3bca0441d",
            Scope(Set("uid")),
            "Bearer",
            userUid = "user uid",
            realm = "/employees")))

        }
      }
    }

    "parse response into TokenInfo when client_id provided" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("valid-token-with-client-id.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(Some(TokenInfo(
            "311f3ab2-4116-45a0-8bb0-50c3bca0441d",
            Scope(Set("uid")),
            "Bearer",
            userUid = "user uid",
            realm = "/employees",
            clientId = Some("Kashyyyk"))))

        }
      }
    }

    "skip all unknown fields in response" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("valid-token-with-unknown-field.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(Some(TokenInfo(
            "311f3ab2-4116-45a0-8bb0-50c3bca0441d",
            Scope(Set("uid")),
            "Bearer",
            userUid = "user uid",
            realm = "/services")))

        }
      }
    }

    "should not return a Token Info when remote server responses that token is not valid" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("access-token-not-valid.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(None)

        }
      }
    }

    "should not return a Token Info when response cannot be parsed" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("access-token-not-valid.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(None)

        }
      }
    }

    "should not return a Token Info when necessary fields in response are missed" in {
      val app: Application = fakeApp(response = Results.Ok.sendResource("token-with-missed-token-type.json"))
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          Await.result(request, 5.second) must beEqualTo(None)

        }
      }
    }

    "should not return a Token Info when remote server operates slow" in {
      val app: Application = fakeApp(delay = 5.seconds)
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          val request = iamClient(port, client, app.actorSystem)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          // should drop a connection faster than in 5 seconds I think, so we await only 5 seconds for result
          Await.result(request, 5.seconds) must beEqualTo(None)

        }
      }
    }

    "should not return a Token Info when remote server is unavailable" in {
      val app: Application = fakeApp()
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { client =>

          // to emulate a connection timeout (not a request timeout) we need to try to establish
          // a connection to non-routable IP address.
          val additionalConfig = Map("authorisation.iam.endpoint" -> "http://10.0.0.1/oauth2/tokeninfo")
          val request = iamClient(port, client, app.actorSystem, additionalConfig)
            .apply(OAuth2Token("311f3ab2-4116-45a0-8bb0-50c3bca0441d"))

          // should drop a connection faster than in 5 seconds I think, so we await only 5 seconds for result
          Await.result(request, 5.second) must beEqualTo(None)

        }
      }
    }

    "stop calling remote server when failed requests rate exceed a threshold" in {
      val app: Application = fakeApp(delay = 5.seconds)
      Server.withApplication(application = app) { port =>
        WsTestClient.withClient { wsClient =>

          val client = iamClient(port, wsClient, app.actorSystem)

          // circuit breaker will be open after 5 attempts
          (1 to 5).foreach { attempt =>
            Await.result(client.apply(OAuth2Token(attempt.toString)), 3.seconds) must beEqualTo(None)
          }

          // all later requests should fail fast
          Await.result(client.apply(OAuth2Token("any-token")), 1.second) must beEqualTo(None)
        }
      }
    }

  }

  def fakeApp(delay: Duration = 0.second, response: Result = Results.Ok): Application = {
    val routes: PartialFunction[(String, String), Handler] = {
      case ("GET", "/tokeninfo") => Action {
        Thread.sleep(delay.toMillis)
        response
      }
    }

    new GuiceApplicationBuilder()
      .in(Mode.Test)
      .routes(routes)
      .configure("play.akka.actor-system" -> s"application_iam_client_${java.util.UUID.randomUUID}")
      .build
  }

  private def iamClient(port: Port, client: WSClient, actorSystem: ActorSystem,
    additionalConfig: Map[String, Any] = Map.empty): IAMClient = {
    val clientConfig = Configuration.from(Map(
      "authorisation.iam.endpoint" -> s"http://localhost:$port/tokeninfo",
      "authorisation.iam.cb.maxFailures" -> 4,
      "authorisation.iam.cb.callTimeout" -> 2000,
      "authorisation.iam.cb.resetTimeout" -> 60000,
      "authorisation.iam.maxRetries" -> 3,
      "authorisation.iam.retry.backoff.duration" -> 100,
      "play.ws.timeout.connection" -> 2000,
      "play.ws.timeout.idle" -> 2000,
      "play.ws.timeout.request" -> 2000) ++ additionalConfig)

    val metricsConfig = Configuration.from(Map(
      // generate new name each time so different registries are used
      "metrics.name" -> java.util.UUID.randomUUID.toString))

    new IAMClient(clientConfig, new NoOpPluggableMetrics, client, actorSystem, ExecutionContext.Implicits.global)
  }
}
