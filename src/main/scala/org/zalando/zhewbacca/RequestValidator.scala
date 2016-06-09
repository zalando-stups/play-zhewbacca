package org.zalando.zhewbacca

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.control.NonFatal

private[zhewbacca] object RequestValidator {

  val logger: Logger = Logger(this.getClass)

  def validate[A](scope: Scope, requestHeader: RequestHeader, authProvider: AuthProvider): Future[Either[Result, TokenInfo]] = {
    authProvider.valid(OAuth2Token.from(requestHeader), scope).map {
      case AuthTokenValid(tokenInfo) => Right(tokenInfo)
      case AuthTokenInvalid => Left(Results.Forbidden)
      case AuthTokenEmpty => Left(Results.Unauthorized)
    } recover {
      case NonFatal(e) =>
        logger.error(e.getMessage, e)
        logger.debug("Request forbidden because of failure in Authentication Provider")
        Left(Results.Forbidden)
    }
  }
}
