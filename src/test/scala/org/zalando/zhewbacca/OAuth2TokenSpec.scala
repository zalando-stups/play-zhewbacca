package org.zalando.zhewbacca

import org.specs2.mutable._
import play.api.test.FakeRequest

class OAuth2TokenSpec extends Specification {

  "OAuth2 token" should {

    "be extracted from 'Authorization' request header" in {
      val request = FakeRequest().withHeaders("Authorization" -> "Bearer 267534eb-3135-4b64-9bab-9573300a0634")
      OAuth2Token.from(request) must be equalTo Some(OAuth2Token("267534eb-3135-4b64-9bab-9573300a0634"))
    }

    "be extracted only from first 'Authorization' request header" in {
      val request = FakeRequest().withHeaders(
        "Authorization" -> "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==",
        "Authorization" -> "Bearer 78d6c4c9-b777-4524-9c59-dbf55a3f8ad1")

      OAuth2Token.from(request) must be equalTo None
    }

    "be empty for request without 'Authorization' header in it" in {
      val request = FakeRequest().withHeaders("Content-type" -> "application/json")
      OAuth2Token.from(request) must be equalTo None
    }

    "be empty for request without headers" in {
      val request = FakeRequest()
      OAuth2Token.from(request) must be equalTo None
    }

    "be empty for request with other type of authorization" in {
      val request = FakeRequest().withHeaders("Authorization" -> "Token 0675b082ebea09b4484b053285495458")
      OAuth2Token.from(request) must be equalTo None
    }

  }

  "toSafeString method" should {

    "mask token value except first and last 4 characters" in {
      val request = FakeRequest().withHeaders("Authorization" -> "Bearer dbc1ec97-d01a-4b10-b853-ec7dedeff8d9")
      OAuth2Token.from(request).get.toSafeString must be equalTo "dbc1...f8d9"
    }

  }

}
