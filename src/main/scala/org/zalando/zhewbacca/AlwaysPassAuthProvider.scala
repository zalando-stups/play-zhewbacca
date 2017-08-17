package org.zalando.zhewbacca

import scala.concurrent.Future

/**
  * This implementation is useful for Development environment to bypass a security mechanism.
  * It assumes that every token is valid.
  */
class AlwaysPassAuthProvider(tokenInfo: TokenInfo) extends AuthProvider {
  override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
    Future.successful(AuthTokenValid(tokenInfo))
}
