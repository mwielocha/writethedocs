package io.mwielocha.writethedocs

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.mwielocha.writethedocs.scalatest.WriteTheDocs
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}
import io.circe._
import io.circe.generic.semiauto._

/**
  * Created by mwielocha on 22/10/2016.
  */

case class User(id: Long, name: String)

trait JsonSupport extends ErrorAccumulatingCirceSupport {
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
}

class ExampleApiSpec extends WordSpec with MustMatchers
  with JsonSupport with WriteTheDocs with Directives
  with ScalatestRouteTest with BeforeAndAfterAll {


  override def routeHints: RouteHints = {
    case d@RouteDetails(r, _, _) if r.uri == "/api/user" =>
      d.withDescription("Fetch user")
  }

  override def paramHints: ValueHints = {
    case v@ValueDetails("name", _, _, _, _, _) =>
      v.withRequired(false)
        .withDescription("Just some user name")
  }

  override protected def docSettings: DocSettings = {
    super.docSettings
      .withIncludeBadRequests(true)
  }

  private val users = List(
    User(1L, "John"),
    User(2L, "Jim"),
    User(3L, "Jake"),
    User(4L, "Jacob"),
    User(5L, "Jane")
  )

  val route: Route = Route.seal {
    pathPrefix("api") {
      path("user") {
        get {
          complete {
            User(1L, "Jim")
          }
        } ~ (post & entity(as[User])) { user =>
          complete {
            user
          }
        }
      } ~ pathPrefix("users") {
        get {
          (pathEnd & parameter("offset") & parameter("limit")) {
            (_, _) => complete {
              users
            }
          } ~ path(IntNumber) { number =>
            complete {
              users(number)
            }
          }
        }
      } ~ path("image") {
        (post & pathEnd & entity(as[Multipart.FormData])) {
          data =>
            complete(data.mediaType.value)
        }
      }
    }
  }

  "Akka http route" should {

    "document itself upon calling in test" in {

      val queryBuilder1 = Query.newBuilder
      queryBuilder1 += "name" -> "John"

      val req1 = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri("/api/user").withQuery(
          queryBuilder1.result()
        )
      )

      req1 ~> writeTheDocs(route) ~> check {
        entityAs[User] mustBe User(1L, "Jim")
      }

      val req2 = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("/api/user"),
        entity = marshal(User(2L, "Hello"))
      )

      req2 ~> writeTheDocs(route) ~> check {
        entityAs[User] mustBe User(2L, "Hello")
      }

      val queryBuilder3 = Query.newBuilder
      queryBuilder3 += "offset" -> "0"
      queryBuilder3 += "limit" -> "10"

      val req3 = HttpRequest(
        method = HttpMethods.GET,
        headers = List[HttpHeader](
          Authorization(
            BasicHttpCredentials("admin", "admin")
          )
        ),
        uri = Uri("/api/users").withQuery(
          queryBuilder3.result()
        )
      )

      req3 ~> writeTheDocs(route) ~> check {
        entityAs[List[User]].size mustBe 5
      }

      val req4 = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("/api/user"),
        entity = marshal(User(500L, "Jimbo"))
      )

      req4 ~> writeTheDocs(route) ~> check {
        entityAs[User] mustBe User(500L, "Jimbo")
      }

      val req5 = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri("/api/users/2")
      )

      req5 ~> writeTheDocs(route) ~> check {
        entityAs[User] mustBe users(2)
      }

      val req6 = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri("/api/users/3")
      )

      req6 ~> writeTheDocs(route) ~> check {
        entityAs[User] mustBe users(3)
      }

      val req7 = HttpRequest(
        method = HttpMethods.PUT,
        uri = Uri("/api/users")
      )

      req7 ~> writeTheDocs(route) ~> check {
        status.intValue() mustBe 405
      }

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "data",
          HttpEntity.Strict(
            MediaTypes.`application/json`,
            ByteString("""{"image": 100}""")
          )
        ),
        Multipart.FormData.BodyPart.fromPath(
          "attachment",
          ContentTypes.`application/octet-stream`,
          Paths.get(getClass.getResource("/test.png").toURI),
          100000
        )
      )

      val req8 = Post("/api/image", formData)

      req8 ~> writeTheDocs(route) ~> check {
        status.intValue() mustBe 200
      }
    }
  }
}
