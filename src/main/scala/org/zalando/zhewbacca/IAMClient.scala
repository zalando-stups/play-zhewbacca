package org.zalando.zhewbacca

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import org.zalando.zhewbacca.metrics.PluggableMetrics
import play.api.http.Status._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import atmos.dsl._
import atmos.dsl.Slf4jSupport._

/**
  * Retrieves TokenInfo for given OAuth2 token using IAM API.
  *
  * Class applies a Circuit Breaker pattern, so it must be a singleton in the client's code. Implementation
  * depends on Play infrastructure so it will work only in a context of running application.
  *
  * @param config Play config to get configuration parameters for WS client and circuit breaker
  */
@Singleton
class IAMClient @Inject() (
    config: Configuration,
    pluggableMetrics: PluggableMetrics,
    ws: WSClient,
    actorSystem: ActorSystem,
    implicit val ec: ExecutionContext) extends (OAuth2Token => Future[Option[TokenInfo]]) {

  val logger: Logger = Logger("security.IAMClient")

  val METRICS_BREAKER_CLOSED = 0
  val METRICS_BREAKER_OPEN = 1
  val circuitStatus = new AtomicInteger()

  pluggableMetrics.gauge {
    circuitStatus.get
  }

  val authEndpoint: String = config.getOptional[String]("authorisation.iam.endpoint").getOrElse(
    throw new IllegalArgumentException("Authorisation: IAM endpoint is not configured"))

  val breakerMaxFailures: Int = config.getOptional[Int]("authorisation.iam.cb.maxFailures").getOrElse(
    throw new IllegalArgumentException("Authorisation: Circuit Breaker max failures is not configured"))

  val breakerCallTimeout: FiniteDuration = config.getOptional[FiniteDuration]("authorisation.iam.cb.callTimeout").getOrElse(
    throw new IllegalArgumentException("Authorisation: Circuit Breaker call timeout is not configured"))

  val breakerResetTimeout: FiniteDuration = config.getOptional[FiniteDuration]("authorisation.iam.cb.resetTimeout").getOrElse(
    throw new IllegalArgumentException("Authorisation: Circuit Breaker reset timeout is not configured"))

  val breakerMaxRetries: TerminationPolicy = config.getOptional[Int]("authorisation.iam.maxRetries").getOrElse(
    throw new IllegalArgumentException("Authorisation: Circuit Breaker max retries is not configured")).attempts

  val breakerRetryBackoff: FiniteDuration = config.getOptional[FiniteDuration]("authorisation.iam.retry.backoff.duration").getOrElse(
    throw new IllegalArgumentException("Authorisation: Circuit Breaker the duration of exponential backoff is not configured"))

  lazy val breaker: CircuitBreaker = new CircuitBreaker(
    actorSystem.scheduler,
    breakerMaxFailures,
    breakerCallTimeout,
    breakerResetTimeout).onHalfOpen {
    circuitStatus.set(METRICS_BREAKER_OPEN)
  }.onOpen {
    circuitStatus.set(METRICS_BREAKER_OPEN)
  }.onClose {
    circuitStatus.set(METRICS_BREAKER_CLOSED)
  }

  implicit val retryRecover = retryFor { breakerMaxRetries } using {
    exponentialBackoff { breakerRetryBackoff }
  } monitorWith {
    logger.logger onRetrying logNothing onInterrupted logWarning onAborted logError
  }

  override def apply(token: OAuth2Token): Future[Option[TokenInfo]] = {
    breaker.withCircuitBreaker(
      pluggableMetrics.timing(
        retryAsync(s"Calling $authEndpoint") {
          ws.url(authEndpoint).withQueryStringParameters(("access_token", token.value)).get()
        })).map { response =>
        response.status match {
          case OK => Some(response.json.as[TokenInfo])
          case _ => None
        }
      } recover {
        case NonFatal(e) =>
          logger.error(s"Exception occurred during validation of token '${token.toSafeString}': $e")
          None // consider any exception as invalid token
      }
  }

}
