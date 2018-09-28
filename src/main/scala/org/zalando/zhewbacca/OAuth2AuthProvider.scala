package org.zalando.zhewbacca

import javax.inject.{Inject, Singleton}

import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * Authorization provider which uses Zalando's IAM API to verify given OAuth2 token.
  */
@Singleton
class OAuth2AuthProvider @Inject() (getTokenInfo: (OAuth2Token) => Future[Option[TokenInfo]])(implicit ec: ExecutionContext)
  extends AuthProvider {

  val logger: Logger = Logger("security.OAuth2AuthProvider")

  private val bearerTokenType = "Bearer"

  override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
    token.map(validateToken(_, scope)).getOrElse(Future.successful(AuthTokenEmpty))

  private def validateToken(token: OAuth2Token, scope: Scope): Future[AuthResult] =
    getTokenInfo(token).map(validateTokenInfo(_, token, scope))

  private def validateTokenInfo(tokenInfo: Option[TokenInfo], token: OAuth2Token, scope: Scope): AuthResult = {
    tokenInfo match {
      case Some(tokenInfo @ TokenInfo(`token`.value, thatScope, `bearerTokenType`, _, _, _)) if scope.in(thatScope) =>
        AuthTokenValid(tokenInfo)
      case Some(tokenInfo @ TokenInfo(_, thatScope, tokenType, _, _, _)) =>
        logger.info(s"Token '${token.toSafeString} has insufficient scope or wrong type, token scopes are ${thatScope.names.mkString(", ")}," +
          s"token type is $tokenType")
        AuthTokenInsufficient
      case None =>
        logger.debug(s"Token '${token.toSafeString} is not valid'")
        AuthTokenInvalid
    }
  }
}
