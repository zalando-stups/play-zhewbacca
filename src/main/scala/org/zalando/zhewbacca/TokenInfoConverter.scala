package org.zalando.zhewbacca

import play.api.mvc.RequestHeader

object TokenInfoConverter {

  implicit class AuthenticatedRequestHeader(underlying: RequestHeader) {

    private val AccessTokenKey = "tokenInfo.access_token"
    private val ScopeKey = "tokenInfo.scope"
    private val ScopeSeparator = '|'
    private val TokenTypeKey = "tokenInfo.token_type"
    private val UidKey = "tokenInfo.uid"

    def tokenInfo: TokenInfo = {
      val accessToken = underlying.tags.getOrElse(AccessTokenKey, sys.error("access token not provided"))
      val scopeNames = underlying.tags.getOrElse(ScopeKey, sys.error("scope not provided"))
        .split(ScopeSeparator)
        .toSet
      val tokenType = underlying.tags.getOrElse(TokenTypeKey, sys.error("token type is not provided"))
      val uid = underlying.tags.getOrElse(UidKey, sys.error("user id is not provided"))

      TokenInfo(accessToken, Scope(scopeNames), tokenType, uid)
    }

    private[zhewbacca] def withTokenInfo(tokenInfo: TokenInfo): RequestHeader = {
      underlying
        .withTag(AccessTokenKey, tokenInfo.accessToken)
        .withTag(ScopeKey, tokenInfo.scope.names.mkString(ScopeSeparator.toString))
        .withTag(TokenTypeKey, tokenInfo.tokenType)
        .withTag(UidKey, tokenInfo.userUid)
    }
  }

}
