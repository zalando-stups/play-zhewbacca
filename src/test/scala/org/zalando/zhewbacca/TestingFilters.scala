package org.zalando.zhewbacca

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

class TestingFilters @Inject() (securityFilter: SecurityFilter) extends HttpFilters {
  val filters: Seq[EssentialFilter] = Seq(securityFilter)
}
