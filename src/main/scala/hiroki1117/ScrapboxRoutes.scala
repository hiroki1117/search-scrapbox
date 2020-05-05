package hiroki1117

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ScrapboxRoutes {
  def scrapboxRoutes[F[_]: Sync](S: Scrapbox[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "scrapbox" =>
        for {
          pages <- S.getProgectPages("sample")
          content <- S.getPageContent("sample", "hoge")
          resp <- Ok(content)
        } yield resp
    }
  }
}
