package org.zalando.zhewbacca.metrics

import scala.concurrent.Future

trait PlugableMetrics {
  def timing[A](a: Future[A]): Future[A]
  def gauge[A](f: => A): Unit
}
