package org.zalando.zhewbacca

import scala.concurrent.Future

trait AuthProvider {
  def valid(token: Option[OAuth2Token], scope: Scope): Future[AuthResult]
}
