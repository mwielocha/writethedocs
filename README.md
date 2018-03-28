# writethedocs

A tiny utility library to add self documenting capabilities to akka http routes testing.
It can basically guess how the api works absed on calls from within tests, so the accuracy of the doc depends on test coverage.
Currently only supported output format is github's `md`.

Example doc can be found here: https://github.com/mwielocha/writethedocs/blob/master/docs/ExampleApi.md
Example spec is also included: https://github.com/mwielocha/writethedocs/blob/master/src/test/scala/io/mwielocha/writethedocs/ExampleApiSpec.scala

## Installation

Artifacts are published to maven central, so just do

```scala
libraryDependencies ++= Seq(
  "io.mwielocha" %% "writethedocs" % "0.6.1"
)
```

## Usage:

Library includes a utility trait to mixing to scalatest's suites, like so:

```scala
import io.mwielocha.writethedocs.scalatest.WriteTheDocs

class ExampleApiSpec extends WordSpec with MustMatchers
  with JsonSupport with WriteTheDocs with Directives
  with ScalatestRouteTest with BeforeAndAfterAll {
  
  // specs
  
}
```

One must also wrap tested routes in `writeTheDocs` method:

```scala
request ~> writeTheDocs(route) ~> check {
  status.intValue() mustBe 200
}
```

This will create a documention in the output dir, only if all tests succeed.

One can overrite document settings to specify custom output directory:

```scala 
override protected def docSettings: DocSettings = {
  super.docSettings.withOutputDirectory("./docs/api/")
}
  ```
  
There is also an option to add hints for selected routes/headers/params to make the doc more detailed:

```scala
override def routeHints: RouteHints = {
    case d@RouteDetails(r, _, _) if r.uri == "/api/user" =>
      d.withDescription("Fetch user")
  }

  override def paramHints: ValueHints = {
    case v@ValueDetails("name", _, _, _, _, _) =>
      v.withRequired(false)
        .withDescription("Just some user name")
  }
  
  override def headerHints: ValueHints = {
    case header @ ValueDetails("Authorization", _, _, _, _, _) =>
      header.withDescription("Basic authorization")
        .withRequired(true)
  }
```
