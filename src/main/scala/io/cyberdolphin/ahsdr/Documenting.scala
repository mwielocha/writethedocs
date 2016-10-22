package io.cyberdolphin.ahsdr

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import better.files._
import com.typesafe.config.ConfigFactory

import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
  * Created by mwielocha on 22/10/2016.
  */
trait Documenting extends Directives {

  private val UUID = "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})".r
  private val ObjectId = "([a-fA-F0-9]{24})".r

  private def isInt(s: String) = Try(s.toInt).isSuccess
  private def isDouble(s: String) = Try(s.toDouble).isSuccess

  private val details = new ArrayBuffer[RouteDetails]()

  private val config = ConfigFactory.defaultApplication()

  private val target = Try {
    config.getString("ahsdr.targer.dir")
  }.getOrElse("./docs")

  def title = {
    getClass
      .getSimpleName
      .replace("Spec", "")
  }

  def documentFileName = {
    s"$title.md"
  }

  private lazy val output = File(s"$target/$documentFileName")
    .createIfNotExists(createParents = true)

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

  private def awaitContent(dataBytes: Source[ByteString, Any], encoding: String): String = {
    Await.result(content(dataBytes, encoding), 25 millis)
  }

  private def awaitContentOrNone(dataBytes: Source[ByteString, Any], encoding: String): Option[String] = {
    Some(awaitContent(dataBytes, encoding)).filter(_.nonEmpty).map {
      TemplateHelpers.prettify
    }
  }

  def documentResponse(requestDetails: RequestDetails): Directive0 = {

    mapResponse { response =>

      val responseDetails = ResponseDetails(
        response.entity.contentType.value,
        response.headers.map(h => h.name() -> h.value()) (breakOut),
        awaitContentOrNone(
          response.entity.dataBytes,
          response.encoding.value),
        response.status.intValue()
      )

      details += RouteDetails(
        requestDetails,
        responseDetails
      )

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

  def document(route: Route, endpoint: String): Route = document(route, Some(endpoint))

  def document(route: Route, endpoint: Option[String] = None): Route = {
    // Route.seal()

    extractRequest { request =>

      val requestDetails = RequestDetails(
        request.method.value,
        endpoint.getOrElse(normalize(request.uri.path).mkString),
        request.entity.contentType.value,
        request.headers.map(h => h.name() -> h.value()) (breakOut),
        awaitContentOrNone(
          request.entity.dataBytes,
          request.encoding.value),
        request.uri.query()
      )

      documentResponse(requestDetails) {
        route
      }
    }
  }

  def writeDocumentation(): Unit = {

    Try(output < "")

    write {
      txt.Documentation.render(
        title,
        details.distinct
          .sortBy(_.request.uri)
          .toList
      ).body.trim()
    }

    details.clear()
  }
}
