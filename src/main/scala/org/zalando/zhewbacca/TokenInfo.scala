package org.zalando.zhewbacca

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class TokenInfo(accessToken: String, scope: Scope, tokenType: String, userUid: String, realm: String, clientId: Option[String] = None)

object TokenInfo {
  implicit val tokenInfoReads: Reads[TokenInfo] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "scope").read[Seq[String]].map(names => Scope(Set(names: _*))) and
    (JsPath \ "token_type").read[String] and
    (JsPath \ "uid").read[String] and
    (JsPath \ "realm").read[String] and
    (JsPath \ "client_id").readNullable[String])(TokenInfo.apply _)
}
