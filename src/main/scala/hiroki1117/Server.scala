package hiroki1117
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import com.typesafe.config.ConfigFactory
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.global

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).withIdleTimeout(3.minutes).stream
      scrapboxImpl = Scrapbox.impl[F](client, ConfigFactory.load())
      repositoryImpl = Repository.inMemoryImpl[F]

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
          Ping.ping[F] <+>
          ScrapboxRoutes.scrapboxRoutes(scrapboxImpl, repositoryImpl)
        ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .withIdleTimeout(3.minutes)
        .serve
    } yield exitCode
    }.drain
}
