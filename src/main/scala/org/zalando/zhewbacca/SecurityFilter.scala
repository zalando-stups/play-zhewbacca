package org.zalando.zhewbacca

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * `SecurityFilter` intercepts every request and validates it against security rules.
  *
  * It forwards an original request to the next filter in the chain if this request doesn't have corresponding
  * security rule. Authenticated requests will be modified to include `TokenInfo` information into request's metadata.
  *
  * @param rulesRepository security rules repository
  * @param mat materializer (required by Play framework)
  * @param ec an ExecutionContext for rules
  */
class SecurityFilter @Inject() (
    rulesRepository: SecurityRulesRepository,
    implicit val mat: Materializer,
    implicit val ec: ExecutionContext) extends Filter {

  private val logger = Logger(getClass)

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    rulesRepository.get(requestHeader).getOrElse {
      logger.debug(s"No security rules found for ${requestHeader.method} ${requestHeader.uri}. Access denied.")
      DenyAllRule
    }.execute(nextFilter, requestHeader)
  }

}

