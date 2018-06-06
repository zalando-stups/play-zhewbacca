package org.zalando.zhewbacca

import org.zalando.zhewbacca.TokenInfoConverter._
import play.api.Logger
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

trait SecurityRule {
  def isApplicableTo(requestHeader: RequestHeader): Boolean
  def execute(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader)(implicit ec: ExecutionContext): Future[Result]
}

abstract class StrictRule(method: String, pathRegex: String) extends SecurityRule {

  private val RequestMatcherRegex = s"^$method $pathRegex$$".r

  def isApplicableTo(requestHeader: RequestHeader): Boolean =
    RequestMatcherRegex.pattern.matcher(s"${requestHeader.method} ${requestHeader.uri}").matches

}

case class ValidateTokenRule(
    authProvider: AuthProvider,
    method: String,
    pathRegex: String,
    scope: Scope) extends StrictRule(method, pathRegex) {

  private[this] val log = Logger(this.getClass)

  override def execute(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader)(implicit ec: ExecutionContext): Future[Result] =
    RequestValidator.validate(scope, requestHeader, authProvider).flatMap[Result] {
      case Right(tokenInfo) =>
        log.info(s"Request #${requestHeader.id} authenticated as: ${tokenInfo.userUid}")
        nextFilter(requestHeader.withTokenInfo(tokenInfo))

      case Left(result) =>
        log.info(s"Request #${requestHeader.id} failed auth")
        Future.successful(result)
    }
}

/**
  * Allowed to 'pass-through' of any request. It means that no security checks will be applied.
  * It is often useful in combination with 'catch all' rule which forces to verify tokens for all endpoints.
  */
case class ExplicitlyAllowedRule(method: String, pathRegex: String) extends StrictRule(method, pathRegex) {

  override def execute(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader)(implicit ec: ExecutionContext): Future[Result] =
    nextFilter(requestHeader)

}

/**
  * Useful for explicitly denied HTTP methods or URIs.
  */
case class ExplicitlyDeniedRule(method: String, pathRegex: String) extends StrictRule(method, pathRegex) with DenySecurityRule

/**
  * Default rule for `SecurityFilter`.
  */
case object DenyAllRule extends DenySecurityRule {
  override def isApplicableTo(requestHeader: RequestHeader): Boolean = true
}

trait DenySecurityRule extends SecurityRule {
  override def execute(nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader)(implicit ec: ExecutionContext): Future[Result] =
    Future.successful(Results.Forbidden)
}
