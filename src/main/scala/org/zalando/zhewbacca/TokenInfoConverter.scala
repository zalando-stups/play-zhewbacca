package org.zalando.zhewbacca

import play.api.libs.typedmap.TypedKey
import play.api.mvc.RequestHeader

object TokenInfoConverter {

  private val AccessTokenKey: TypedKey[String] = TypedKey("tokenInfo.access_token")
  private val ScopeKey: TypedKey[String] = TypedKey("tokenInfo.scope")
  private val ScopeSeparator = '|'
  private val TokenTypeKey: TypedKey[String] = TypedKey("tokenInfo.token_type")
  private val UidKey: TypedKey[String] = TypedKey("tokenInfo.uid")
  private val ClientIdKey: TypedKey[Option[String]] = TypedKey("tokenInfo.client_id")
  private val RealmKey: TypedKey[String] = TypedKey("tokenInfo.realm")

  implicit class AuthenticatedRequestHeader(underlying: RequestHeader) {

    def tokenInfo: TokenInfo = {
      val accessToken = underlying.attrs.get(AccessTokenKey).getOrElse(sys.error("access token not provided"))
      val scopeNames = underlying.attrs.get(ScopeKey).getOrElse(sys.error("scope not provided"))
        .split(ScopeSeparator)
        .toSet
      val tokenType = underlying.attrs.get(TokenTypeKey).getOrElse(sys.error("token type is not provided"))
      val uid = underlying.attrs.get(UidKey).getOrElse(sys.error("user id is not provided"))
      val clientId = underlying.attrs.get(ClientIdKey).flatten
      val realm = underlying.attrs.get(RealmKey).getOrElse(sys.error("realm is not provided"))

      TokenInfo(accessToken, Scope(scopeNames), tokenType, uid, clientId, realm)
    }

    private[zhewbacca] def withTokenInfo(tok: TokenInfo): RequestHeader = {
      underlying
        .addAttr(AccessTokenKey, tok.accessToken)
        .addAttr(ScopeKey, tok.scope.names.mkString(ScopeSeparator.toString))
        .addAttr(TokenTypeKey, tok.tokenType)
        .addAttr(UidKey, tok.userUid)
        .addAttr(ClientIdKey, tok.clientId)
        .addAttr(RealmKey, tok.realm)
    }
  }

}
