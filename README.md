# Play-security library

![Build Status - Master](https://travis-ci.org/zalando-incubator/play-zhewbacca.svg?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.zalando/play-zhewbacca_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/play-zhewbacca_2.11)
[![codecov.io](https://codecov.io/github/zalando-incubator/play-zhewbacca/coverage.svg?branch=master)](https://codecov.io/github/zalando-incubator/play-zhewbacca?branch=master)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/zalando-incubator/play-zhewbacca/master/LICENSE)

Play! (v2.5) library to protect REST endpoint using OAuth2 token verification. In order to access a protected endpoint clients should pass an `Authorization` header with the `Bearer` token in every request.
This library is implemented in a non-blocking way and provides two implementations of token verification.

- `AlwaysPassAuthProvider` implementation is useful for Development environment to bypass a security mechanism. This implementation assumes that every token is valid.
- `OAuth2AuthProvider` implementation acquires token information from a 3rd-party endpoint and then verifies this info. Also it applies a [Circuit Breaker](http://doc.akka.io/docs/akka/snapshot/common/circuitbreaker.html) pattern.

Clients of this library don't need to change their code in order to protect endpoints. All necessary security configurations happen in a separate configuration file.

## Users guide

Configure libraries dependencies in your `build.sbt`:

```scala
libraryDependencies += "org.zalando" %% "play-zhewbacca" % "0.2.1"
```

To configure Development environment:

```scala
package modules

import com.google.inject.AbstractModule
import org.zalando.zhewbacca._
import org.zalando.zhewbacca.metrics.{NoOpPlugableMetrics, PlugableMetrics}

class DevModule extends AbstractModule {
  val TestTokenInfo = TokenInfo("", Scope.Default, "token type", "user uid")
  
  override def configure(): Unit = {
    bind(classOf[PlugableMetrics]).to(classOf[NoOpPlugableMetrics])
    bind(classOf[AuthProvider]).toInstance(new AlwaysPassAuthProvider(TestTokenInfo))
  }
  
}
```

For Production environment use:

```scala
package modules

import com.google.inject.{ TypeLiteral, AbstractModule }
import org.zalando.zhewbacca._
import org.zalando.zhewbacca.metrics.{NoOpPlugableMetrics, PlugableMetrics}

import scala.concurrent.Future

class ProdModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AuthProvider]).to(classOf[OAuth2AuthProvider])
    bind(classOf[PlugableMetrics]).to(classOf[NoOpPlugableMetrics])
    bind(new TypeLiteral[(OAuth2Token) => Future[Option[TokenInfo]]]() {}).to(classOf[IAMClient])
  }

}
```

By default no metrics mechanism is used. User can implement ```PlugableMetrics``` to gather some simple metrics.
See ```org.zalando.zhewbacca.IAMClient``` to learn what can be measured.

You need to include `de.zalando.seo.play.security.SecurityFilter` into your applications' filters:

```scala
package filters

import javax.inject.Inject
import org.zalando.zhewbacca.SecurityFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

class MyFilters @Inject() (securityFilter: SecurityFilter) extends HttpFilters {
  val filters: Seq[EssentialFilter] = Seq(securityFilter)
}
```

and then add `play.http.filters = filters.MyFilters` line to your `application.conf`. `SecurityFilter` rejects any requests to any endpoint which does not have a corresponding rule in the `security_rules.conf` file.

Example of configuration in `application.conf` file:

```
# Full URL for authorization endpoint
authorisation.iam.endpoint = "https://info.services.auth.zalando.com/oauth2/tokeninfo"

# Maximum number of failures before opening the circuit
authorisation.iam.cb.maxFailures = 4

# Duration of time in milliseconds after which to consider a call a failure
authorisation.iam.cb.callTimeout = 2000

# Duration of time in milliseconds after which to attempt to close the circuit
authorisation.iam.cb.resetTimeout = 60000

# IAMClient depends on Play internal WS client so it also has to be configured.
# The maximum time to wait when connecting to the remote host.
# Play's default is 120 seconds
play.ws.timeout.connection = 2000

# The maximum time the request can stay idle when connetion is established but waiting for more data
# Play's default is 120 seconds
play.ws.timeout.idle = 2000

# The total time you accept a request to take. It will be interrupted, whatever if the remote host is still sending data.
# Play's default is none, to allow stream consuming.
play.ws.timeout.request = 2000

play.http.filters = filters.MyFilters

play.modules.enabled += "modules.ProdModule"
```

By default this library reads the security configuration from the `conf/security_rules.conf` file. You can change the file name by specifying a value for the key `authorisation.rules.file` in your `application.conf` file.

```
# This is an example of production-ready configuration security configuration.
# You can copy it from here and paste right into your `conf/security_rules.conf` file.
rules = [
    # All GET requests to /api/my-resource has to have a valid OAuth2 token for scopes: uid, scop1, scope2, scope3
    {
        method: GET
        pathRegex: "/api/my-resource/.*"
        scopes: ["uid", "scope1", "scope2", "scope3"]
    }

    # POST requests to /api/my-resource require only scope2.write scope
    {
        method: POST
        pathRegex: "/api/my-resource/.*"
        scopes: ["scope2.write"]
    }

    # GET requests to /bar resources allowed to be without OAuth2 token
    {
        method: GET
        pathRegex: /bar
        allowed: true
    }

    # 'Catch All' rule will immidiately reject all requests for all other endpoints
    {
        method: GET
        pathRegex: "/.*"
        allowed: false      // this is an example of inline comment
    }
]
```

The following example demonstrates how you can get access to the Token Info object inside your controller:

```scala
package controllers

import javax.inject.Inject

import org.zalando.zhewbacca.TokenInfoConverter._
import org.zalando.zhewbacca.TokenInfo

class SeoDescriptionController @Inject() extends Controller {

  def create(uid: String): Action[AnyContent] = Action { request =>
    val tokenInfo: TokenInfo = request.tokenInfo
    // do something with token info. For example, read user's UID: tokenInfo.userUid
  }
}

```
