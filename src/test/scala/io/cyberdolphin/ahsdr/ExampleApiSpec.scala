package io.cyberdolphin.ahsdr

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers, WordSpec}
import spray.json._

/**
  * Created by mwielocha on 22/10/2016.
  */

case class User(id: Long, name: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val userFormat = jsonFormat2(User)
}

class ExampleApiSpec extends WordSpec with MustMatchers
  with JsonSupport with Documenting
  with ScalatestRouteTest with BeforeAndAfterAll {

  private val users = List(
    User(1L, "John"),
    User(2L, "Jim"),
    User(3L, "Jake"),
    User(4L, "Jacob"),
    User(5L, "Jane")
  )

  val route = {
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
            (o, l) => complete {
              users
            }
          } ~ path(IntNumber) { number =>
            complete {
              users(number)
            }
          }
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

      req1 ~> document(route) ~> check {
        entityAs[User] mustBe User(1L, "Jim")
      }

      val req2 = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("/api/user"),
        entity = marshal(User(2L, "Hello"))
      )

      req2 ~> document(route) ~> check {
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

      req3 ~> document(route) ~> check {
        entityAs[List[User]].size mustBe 5
      }

      val req4 = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri("/api/user"),
        entity = marshal(User(500L, "Jimbo"))
      )

      req4 ~> document(route) ~> check {
        entityAs[User] mustBe User(500L, "Jimbo")
      }

      val req5 = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri("/api/users/2")
      )

      req5 ~> document(route) ~> check {
        entityAs[User] mustBe users(2)
      }

      val req6 = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri("/api/users/3")
      )

      req6 ~> document(route) ~> check {
        entityAs[User] mustBe users(3)
      }
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    writeDocumentation()
  }
}
