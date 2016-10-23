package io.cyberdolphin.writethedocs

import java.util.concurrent.ConcurrentLinkedQueue

import akka.http.scaladsl.model.{HttpHeader, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import better.files._

import scala.collection.JavaConversions._
import scala.collection.breakOut
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
  * Created by mwielocha on 22/10/2016.
  */
trait SelfDocumentingRoutes {

  private val UUID = "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})".r
  private val ObjectId = "([a-fA-F0-9]{24})".r

  private def isInt(s: String) = Try(s.toInt).isSuccess
  private def isDouble(s: String) = Try(s.toDouble).isSuccess

  private val details = new ConcurrentLinkedQueue[RouteDetails]

  protected def documentSettings = DocumentationSettings("./docs")

  def documentTitle = {
    getClass
      .getSimpleName
      .replace("Spec", "")
  }

  def documentFileName = {
    s"$documentTitle.md"
  }

  private lazy val output = File(
    s"${documentSettings.outputDirectory}" +
      s"/$documentFileName"
    ).createIfNotExists(createParents = true)

  implicit def materializer: ActorMaterializer
  import scala.concurrent.ExecutionContext.Implicits.global

  private def write(doc: Any): Unit = {
    output << doc.toString.trim
  }

  private def content(dataBytes: Source[ByteString, Any], encoding: String): Future[String] = {

    dataBytes.runWith {

      val zero = new StringBuffer()

      Sink.fold[StringBuffer, ByteString](zero) {
        case (buffer, element) =>
          buffer.append {
            element.decodeString("UTF-8")
          }
      }

    }.map(_.toString)
  }

  type RouteHints = PartialFunction[RouteDetails, RouteDetails]
  type ValueHints = PartialFunction[ValueDetails, ValueDetails]

  def headerHints: ValueHints = { case x => x }
  def paramHints: ValueHints = { case x => x }
  def routeHints: RouteHints = { case x => x }

  private def awaitContent(dataBytes: Source[ByteString, Any], encoding: String): String = {
    Await.result(content(dataBytes, encoding), 25 millis)
  }

  private def awaitContentOrNone(dataBytes: Source[ByteString, Any], encoding: String): Option[String] = {
    Some(awaitContent(dataBytes, encoding)).filter(_.nonEmpty).map {
      TemplateHelpers.prettify
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
        response.entity.contentType.value,
        headers(response.headers),
        awaitContentOrNone(
          response.entity.dataBytes,
          response.encoding.value),
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

  def selfDocumentedRoute(route: Route, endpoint: String): Route = {
    selfDocumentedRoute(route, Some(endpoint))
  }

  def selfDocumentedRoute(route: Route, endpoint: Option[String] = None): Route = {
    // Route.seal()

    extractRequest { request =>

      val requestDetails = RequestDetails(
        request.method.value,
        endpoint.getOrElse(normalize(request.uri.path).mkString),
        request.entity.contentType.value,
        headers(request.headers),
        awaitContentOrNone(
          request.entity.dataBytes,
          request.encoding.value),
        params(request.uri)
      )

      documentResponse(requestDetails) {
        route
      }
    }
  }

  def selfDocument(): Unit = {

    if(documentSettings.enabled && details.nonEmpty) {

      val filtered = details.filterNot {
        d => !documentSettings.includeBadRequests &&
          (d.response.statusCode / 100) == 4
      }.filterNot {
        d => !documentSettings.includeInternalServerErrors &&
          (d.response.statusCode / 100) == 5
      }

      Try(output < "")

      write {
        txt.Documentation.render(
          documentTitle,
          filtered.toList.distinct
            .sortBy(_.request.uri)
        ).body.trim()
      }
    }

    details.clear()

  }
}
