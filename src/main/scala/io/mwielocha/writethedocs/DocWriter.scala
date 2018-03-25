package io.mwielocha.writethedocs

import java.util.concurrent.ConcurrentLinkedQueue

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer
import better.files._

import scala.collection.JavaConverters._
import scala.collection.breakOut
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

/**
  * Created by mwielocha on 22/10/2016.
  */
trait DocWriter {

  private val UUID = "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})".r
  private val ObjectId = "([a-fA-F0-9]{24})".r

  private def isInt(s: String) = Try(s.toInt).isSuccess
  private def isDouble(s: String) = Try(s.toDouble).isSuccess

  private val details = new ConcurrentLinkedQueue[RouteDetails]

  protected def docSettings = DocSettings("./docs")

  protected def defaultAwaitTimeout: FiniteDuration = 250 millis

  def documentTitle: String = {
    getClass
      .getSimpleName
      .replace("Spec", "")
      .replace("Tests", "")
  }

  def documentFileName: String = {
    s"$documentTitle.md"
  }

  private lazy val output = File(
    s"${docSettings.outputDirectory}" +
      s"/$documentFileName"
  ).createIfNotExists(createParents = true)

  implicit def materializer: ActorMaterializer
  import scala.concurrent.ExecutionContext.Implicits.global

  private def write(doc: Any): Unit = {
    output append doc.toString.trim
  }

  private val isPrintable: Set[MediaType] = Set(
    MediaTypes.`application/json`,
    MediaTypes.`application/javascript`,
    MediaTypes.`text/plain`,
    MediaTypes.`application/xml`
  )

  private def contentFromEntity(entity: HttpEntity): Future[Seq[Content]] = {

    entity.contentType.mediaType match {

      case t if t.isMultipart =>
        contentFromMultipartFormData(entity)

      case t if isPrintable(t) =>
        entity.toStrict(defaultAwaitTimeout).map {
          strict => Content(
            body = Some(TemplateHelpers
            .prettify(strict.data.utf8String))
          ) :: Nil
        }

      case _ =>
        Future.successful {
          Content(
            name = None,
            contentType =
              Some(entity.contentType.value),
            body = None
          ) :: Nil
        }
    }
  }

  private def contentFromMultipartFormData(entity: HttpEntity): Future[Seq[Content]] = {
    val unmarhsaller = implicitly[FromEntityUnmarshaller[Multipart.FormData]]

    for {
      multipart <- unmarhsaller(entity)
      strict <- multipart.toStrict(defaultAwaitTimeout)
      strictParts = strict.strictParts
      result <- Future.sequence {
        strictParts.map {
          part =>
            contentFromEntity(part.entity).map {
              _.map {
                _.copy(
                  name = Some(part.name),
                  contentType =
                    Some(part.entity.contentType.value)
                )
              }
            }
        }
      }
    } yield result.flatten
  }

  type RouteHints = PartialFunction[RouteDetails, RouteDetails]
  type ValueHints = PartialFunction[ValueDetails, ValueDetails]

  def headerHints: ValueHints = { case x => x }
  def paramHints: ValueHints = { case x => x }
  def routeHints: RouteHints = { case x => x }


  private def awaitContentFromEntity(entity: HttpEntity): Seq[Content] = {

    Try(Await.result(contentFromEntity(entity), defaultAwaitTimeout)) match {

      case Success(response) => response

      case Failure(_: TimeoutException) => Content(
        body = Some(s"[ERROR] Response processing took more than $defaultAwaitTimeout, " +
        s"override `defaultAwaitTimeout: FiniteDuration` for longer timeout.")
      ) :: Nil

      case Failure(e) =>
        Content(
          body = Some(s"[ERROR] On response processing: ${e.getMessage}")
        ) :: Nil
    }
  }

  private def headers(in: Seq[HttpHeader]): List[ValueDetails] = {
    in.map { header =>
      val v = ValueDetails(
        header.name(),
        header.value())
      if(headerHints.isDefinedAt(v)) headerHints(v) else v
    } (breakOut)
  }

  private def params(uri: Uri): List[ValueDetails] = {
    uri.query().map {
      case (name, value) =>
        val v = ValueDetails(name, value)
        if(paramHints.isDefinedAt(v)) paramHints(v) else v
    } (breakOut)
  }

  def documentResponse(requestDetails: RequestDetails): Directive0 = {

    mapResponse { response =>

      val responseDetails = ResponseDetails(
        headers(response.headers),
        response.entity.contentType.value,
        awaitContentFromEntity(response.entity).head,
        response.status.intValue()
      )

      val routeDetails = RouteDetails(
        requestDetails,
        responseDetails
      )

      val withHints = if(routeHints.isDefinedAt(routeDetails)) {
        routeHints(routeDetails)
      } else routeDetails

      details.add(withHints)

      response
    }
  }

  private def normalize(path: Uri.Path, result: List[String] = List.empty): List[String] = {
    if(path.isEmpty) result
    else path.head.toString match {
      case UUID(_) => normalize(path.tail, result :+ "{uuid}")
      case ObjectId(_) => normalize(path.tail, result :+ "{objectId}")
      case s if isInt(s) => normalize(path.tail, result :+ "{integer}")
      case s if isDouble(s) => normalize(path.tail, result :+ "{double}")
      case s => normalize(path.tail, result :+ s)
    }
  }

  def writeTheDocs(route: Route, endpoint: String): Route = {
    writeTheDocs(route, Some(endpoint))
  }

  def writeTheDocs(route: Route, endpoint: Option[String] = None): Route = {
    // Route.seal()

    extractRequest { request =>

      val requestDetails = RequestDetails(
        request.method.value,
        endpoint.getOrElse(normalize(request.uri.path).mkString),
        request.entity.contentType.value,
        headers(request.headers),
        awaitContentFromEntity(request.entity),
        params(request.uri)
      )

      documentResponse(requestDetails) {
        route
      }
    }
  }

  protected def writeTheTemplate(): Unit = {

    val detailsAsScala = details.asScala

    if(docSettings.enabled && detailsAsScala.nonEmpty) {

      val filtered = detailsAsScala.filterNot {
        d => !docSettings.includeBadRequests &&
          (d.response.statusCode / 100) == 4
      }.filterNot {
        d => !docSettings.includeInternalServerErrors &&
          (d.response.statusCode / 100) == 5
      }

      if (filtered.nonEmpty) {

        // try to guess the most detailed call to render


        val grouped = filtered
          .groupBy(_.request.id)

        val scored: List[RouteDetails] = {
          grouped.flatMap {
            case (_, routes) =>
              routes.toList
                .sortBy(_.score) {
                  Ordering.Int.reverse
                }.headOption
          } (breakOut)
        }

        Try(output write "")

        write {
          txt.TheDoc.render(
            documentTitle,
            scored.sortBy {
              _.request.uri
            }
          ).body.trim()
        }
      }
    }

    details.clear()

  }
}
