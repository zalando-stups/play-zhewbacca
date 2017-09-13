package org.zalando.zhewbacca

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Authorization provider which uses Zalando's IAM API to verify given OAuth2 token.
  */
@Singleton
class OAuth2AuthProvider @Inject() (getTokenInfo: (OAuth2Token) => Future[Option[TokenInfo]])
  extends AuthProvider {

  val logger: Logger = Logger("security.OAuth2AuthProvider")

  private val bearerTokenType = "Bearer"

  override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
    token.map(validateToken(_, scope)).getOrElse(Future.successful(AuthTokenEmpty))

  private def validateToken(token: OAuth2Token, scope: Scope): Future[AuthResult] =
    getTokenInfo(token).map(tokenInfoOpt =>
      tokenInfoOpt.map(validateTokenInfo(_, token, scope)).getOrElse(invalid(token)))

  private def validateTokenInfo(tokenInfo: TokenInfo, token: OAuth2Token, scope: Scope): AuthResult = {
    tokenInfo match {
      case TokenInfo(`token`.value, thatScope, `bearerTokenType`, _) if scope.in(thatScope) => AuthTokenValid(tokenInfo)
      case TokenInfo(_, thatScope, tokenType, _) =>
        logger.info(s"Token '${token.toSafeString} has insufficient scope or wrong type, token scopes are ${thatScope.names.mkString(", ")}," +
          s"token type is $tokenType")
        invalid(token)
    }
  }

  private def invalid(token: OAuth2Token): AuthResult = {
    logger.debug(s"Token '${token.toSafeString} is not valid'")
    AuthTokenInvalid
  }
}
