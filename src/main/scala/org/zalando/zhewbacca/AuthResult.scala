package org.zalando.zhewbacca

sealed abstract class AuthResult

case object AuthTokenInvalid extends AuthResult
case object AuthTokenEmpty extends AuthResult
case class AuthTokenValid(tokenInfo: TokenInfo) extends AuthResult
