package org.zalando.zhewbacca.metrics

import scala.concurrent.Future

class NoOpPluggableMetrics extends PluggableMetrics {
  override def timing[A](a: Future[A]): Future[A] = a

  override def gauge[A](f: => A): Unit = ()
}
