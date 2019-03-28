package org.zalando.zhewbacca.metrics

import scala.concurrent.Future

trait PluggableMetrics {
  def timing[A](a: Future[A]): Future[A]
  def gauge[A](f: => A): Unit
}
