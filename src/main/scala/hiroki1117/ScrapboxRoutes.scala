package hiroki1117

import cats.effect.{IO, Sync}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ScrapboxRoutes {
  def scrapboxRoutes[F[_]: Sync](S: Scrapbox[F], R: Repository[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "scrapbox" =>
        for {
          pagesInfo <- S.getProgectPages("diary-hiroki")
          pages <- S.getAllProjectPagesContent(pagesInfo)
          resp <- Ok(
            pages.map(p => p.lines.map(_.text).mkString("¥n")).mkString("¥n¥n")
          )
        } yield resp

      case GET -> Root / "search" / keyword => {

        for {
          shouldFetch <- R.available("diary-hiroki")
          pagesInfo <- S.getProgectPages("diary-hiroki")
          pages <- S.getAllProjectPagesContent(pagesInfo)
          _ <- R.saveProject(pagesInfo.projectName, pages)
          searchResult <- R.searchKeyword("diary-hiroki", keyword)
          resp <- Ok(searchResult.mkString("\n"))
        } yield resp
      }
    }
  }
}
