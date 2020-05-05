package hiroki1117

import cats.{Applicative, Functor}
import cats.effect.{IO, Sync}
import hiroki1117.Repository.HitPage
import hiroki1117.Scrapbox.Page

abstract class Repository[F[_]: Applicative] {
  def saveProject(projectName: String ,pages: Seq[Page]): F[IO[Unit]]

  def searchKeyword(projectName: String, keyword: String): F[Seq[HitPage]]

  def available(projectName: String): F[IO[Boolean]]
}

object Repository {
  def apply[F[_]](implicit ev: Repository[F]): Repository[F] = ev

  final case class HitPage(projectName: String, pageTitle: String, line: String)

  def inMemoryImpl[F[_]: Sync : Applicative]: Repository[F] = new Repository[F] {
    var map: Map[String, Seq[Page]] = Map.empty

    override def saveProject(projectName: String, pages: Seq[Page]): F[IO[Unit]] = {
      map = Map(projectName -> pages)
      Sync[F].delay(IO.unit)
    }

    override def searchKeyword(projectName: String, keyword: String): F[Seq[HitPage]] = {
      map.get(projectName) match {
        case Some(pages) => {
          val r = pages.foldLeft[Seq[HitPage]](Seq.empty){(acc, page) => pageFinder(projectName, keyword, page) match {
            case Some(v) => v +: acc
            case None => acc
          }}
          Sync[F].delay(r)
        }
        case None => Sync[F].delay(Seq())
      }
    }

    override def available(projectName: String): F[IO[Boolean]] = {
      Sync[F].delay(IO(map.contains(projectName)))
    }

    private def pageFinder(projectName: String, keyword: String, page: Page): Option[HitPage] = {
      val title = page.title
      val result = page.lines.view.map(_.text).find(_.contains(keyword))
      result.map(HitPage(projectName, keyword, _))
    }
  }

}