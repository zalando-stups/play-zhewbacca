package org.zalando.zhewbacca

import play.api.mvc.RequestHeader

case class OAuth2Token private[zhewbacca] (private[zhewbacca] val value: String) {

  private val nonMaskedCharacters = 8

  /**
    * Token values are equivalent to passwords, so it's not allowed to show actual token values even in logs.
    * This methods keeps only first and last 4 characters of string representation of token value.
    *
    * @return token value which is safe to use in logs or to show to anyone
    */
  def toSafeString: String = value.patch(nonMaskedCharacters / 2, "...", value.length - nonMaskedCharacters)
}

object OAuth2Token {

  private val TokenPattern = "Bearer ([a-zA-Z0-9-._~+/]+?)".r

  def from(from: RequestHeader): Option[OAuth2Token] = from.headers.get("Authorization").getOrElse("") match {
    case TokenPattern(accessToken) => Some(new OAuth2Token(accessToken))
    case _ => None
  }

}
