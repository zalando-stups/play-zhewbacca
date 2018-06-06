package org.zalando.zhewbacca

import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

private[zhewbacca] object RequestValidator {

  val logger: Logger = Logger(this.getClass)

  def validate[A](scope: Scope, requestHeader: RequestHeader, authProvider: AuthProvider)(implicit ec: ExecutionContext): Future[Either[Result, TokenInfo]] = {
    authProvider.valid(OAuth2Token.from(requestHeader), scope).map {
      case AuthTokenValid(tokenInfo) => Right(tokenInfo)
      case AuthTokenInvalid => Left(Results.Unauthorized)
      case AuthTokenEmpty => Left(Results.Unauthorized)
      case AuthTokenInsufficient => Left(Results.Forbidden)
    } recover {
      case NonFatal(e) =>
        logger.error(e.getMessage, e)
        logger.debug("Request unauthorized because of failure in Authentication Provider")
        Left(Results.Unauthorized)
    }
  }
}
