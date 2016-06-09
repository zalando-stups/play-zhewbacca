package org.zalando.zhewbacca

import scala.concurrent.Future

class AlwaysPassAuthProvider(tokenInfo: TokenInfo) extends AuthProvider {
  override def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult] =
    Future.successful(AuthTokenValid(tokenInfo))
}
