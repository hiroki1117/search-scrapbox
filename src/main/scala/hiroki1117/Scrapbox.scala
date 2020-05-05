package hiroki1117

import java.net.URLEncoder

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.typesafe.config.Config
import hiroki1117.Scrapbox.ScrapboxContent
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._

trait Scrapbox[F[_]] {
  def getProgectPages(projectName: String): F[Scrapbox.ScrapboxPages]

  def getPageContent(projectName: String ,title: String): F[Scrapbox.ScrapboxContent]
}

object Scrapbox {
  def apply[F[_]](implicit ev: Scrapbox[F]): Scrapbox[F] = ev

  final case class ScrapboxPages(pages: Seq[JsonObject])

  object ScrapboxPages {
    implicit val scrapboxPagesDecoder: Decoder[ScrapboxPages] = deriveDecoder[ScrapboxPages]
    implicit def scrapboxPagesEntityDecoder[F[_]: Sync]: EntityDecoder[F, ScrapboxPages] = jsonOf[F, ScrapboxPages]

    implicit val scrapboxPagesEncoder: Encoder[ScrapboxPages] = deriveEncoder[ScrapboxPages]
    implicit def scrapboxPagesEntityEncoder[F[_], Applicative]: EntityEncoder[F, ScrapboxPages] = jsonEncoderOf
  }

  final case class ScrapboxContent(lines: Seq[JsonObject])

  object ScrapboxContent {
    implicit val ScrapboxContentDecoder: Decoder[ScrapboxContent] = deriveDecoder[ScrapboxContent]
    implicit def ScrapboxContentEntityDecoder[F[_]: Sync]: EntityDecoder[F, ScrapboxContent] = jsonOf[F, ScrapboxContent]

    implicit val ScrapboxContentEncoder: Encoder[ScrapboxContent] = deriveEncoder[ScrapboxContent]
    implicit def ScrapboxContentEntityEncoder[F[_], Applicative]: EntityEncoder[F, ScrapboxContent] = jsonEncoderOf
  }

  def impl[F[_]: Sync](C: Client[F], config: Config): Scrapbox[F] = new Scrapbox[F] {
    val dsl = new Http4sClientDsl[F]{}
    import dsl._
    override def getProgectPages(projectName: String): F[ScrapboxPages] = {
      val uri = "https://scrapbox.io/api/" + projectName
      val request = GET(
        Uri.unsafeFromString(uri).withQueryParam("limit", "1000")
      ).map(_.addCookie(RequestCookie(
        name = "connect.sid",
        content = config.getString("scrapbox_secreat")
      ))
      )
      C.expect[Scrapbox.ScrapboxPages](request)
    }

    override def getPageContent(projectName: String, title: String): F[ScrapboxContent] = {
      val uri = "https://scrapbox.io/api/pages/" + projectName + "/" + URLEncoder.encode(title, "UTF-8")

      val request = GET(
        Uri.unsafeFromString(uri)
      ).map(_.addCookie(RequestCookie(
        name = "connect.sid",
        content = config.getString("scrapboxsecreat")
      )))
      C.expect[Scrapbox.ScrapboxContent](request)
    }
  }
}
