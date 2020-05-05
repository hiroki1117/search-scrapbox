package hiroki1117

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe.{Encoder, Decoder, Json, HCursor}
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, Method, Uri, Request}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._

trait Scrapbox[F[_]] {
  def get: F[Scrapbox.ScrapboxPages]
}

object Scrapbox {
  def apply[F[_]](implicit ev: Scrapbox[F]): Scrapbox[F] = ev

  final case class ScrapboxPages(pages: String)
}
