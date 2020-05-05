package hiroki1117

import java.net.URLEncoder

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.typesafe.config.Config
import hiroki1117.Scrapbox.Page
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._

abstract class Scrapbox[F[_]: Applicative] {
  def getProgectPages(projectName: String): F[Scrapbox.ProjectPages]

  def getAllProjectPagesContent(scrapboxPages: Scrapbox.ProjectPages): F[Seq[Scrapbox.Page]]
}

object Scrapbox {
  def apply[F[_]](implicit ev: Scrapbox[F]): Scrapbox[F] = ev

  final case class ProjectPages(projectName: String, pages: Seq[PageInfo])
  final case class PageInfo(id: String, title: String)
  object PageInfo {
    implicit val PagesDecoder: Decoder[PageInfo] = deriveDecoder[PageInfo]
    implicit def PagesEntityDecoder[F[_]: Sync]: EntityDecoder[F, PageInfo] = jsonOf[F, PageInfo]

    implicit val PagesEncoder: Encoder[PageInfo] = deriveEncoder[PageInfo]
    implicit def PagesEntityEncoder[F[_], Applicative]: EntityEncoder[F, PageInfo] = jsonEncoderOf
  }

  object ProjectPages {
    implicit val scrapboxPagesDecoder: Decoder[ProjectPages] = deriveDecoder[ProjectPages]
    implicit def scrapboxPagesEntityDecoder[F[_]: Sync]: EntityDecoder[F, ProjectPages] = jsonOf[F, ProjectPages]

    implicit val scrapboxPagesEncoder: Encoder[ProjectPages] = deriveEncoder[ProjectPages]
    implicit def scrapboxPagesEntityEncoder[F[_], Applicative]: EntityEncoder[F, ProjectPages] = jsonEncoderOf
  }

  final case class Page(lines: Seq[Text])

  object Page {
    implicit val ScrapboxContentDecoder: Decoder[Page] = deriveDecoder[Page]
    implicit def ScrapboxContentEntityDecoder[F[_]: Sync]: EntityDecoder[F, Page] = jsonOf[F, Page]

    implicit val ScrapboxContentEncoder: Encoder[Page] = deriveEncoder[Page]
    implicit def ScrapboxContentEntityEncoder[F[_], Applicative]: EntityEncoder[F, Page] = jsonEncoderOf
  }

  final case class Text(text: String)
  object Text {
    implicit val ScrapboxContentDecoder: Decoder[Text] = deriveDecoder[Text]
    implicit def ScrapboxContentEntityDecoder[F[_]: Sync]: EntityDecoder[F, Text] = jsonOf[F, Text]

    implicit val ScrapboxContentEncoder: Encoder[Text] = deriveEncoder[Text]
    implicit def ScrapboxContentEntityEncoder[F[_], Applicative]: EntityEncoder[F, Text] = jsonEncoderOf
  }


  def impl[F[_]: Sync : Applicative](C: Client[F], config: Config): Scrapbox[F] = new Scrapbox[F] {
    val dsl = new Http4sClientDsl[F]{}
    import dsl._
    override def getProgectPages(projectName: String): F[ProjectPages] = {
      val uri = "https://scrapbox.io/api/pages/" + projectName
      val request = GET(
        Uri.unsafeFromString(uri).withQueryParam("limit", "3")
      ).map(_.addCookie(RequestCookie(
        name = "connect.sid",
        content = config.getString("scrapboxsecreat")
      ))
      )
      C.expect[Scrapbox.ProjectPages](request)
    }

    override def getAllProjectPagesContent(scrapboxPages: ProjectPages): F[Seq[Page]] = {
      val pagesTitles = scrapboxPages.pages.map(_.title)
      pagesTitles.toList.traverse(t => getPageContent(scrapboxPages.projectName, t)).map(_.toSeq)
    }


    private def getPageContent(projectName: String, title: String): F[Page] = {
      val uri = "https://scrapbox.io/api/pages/" + projectName + "/" + URLEncoder.encode(title, "UTF-8")

      val request = GET(
        Uri.unsafeFromString(uri)
      ).map(_.addCookie(RequestCookie(
        name = "connect.sid",
        content = config.getString("scrapboxsecreat")
      )))
      C.expect[Scrapbox.Page](request)
    }
  }
}
