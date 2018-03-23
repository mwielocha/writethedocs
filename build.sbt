
// Generated with scalagen

val akkaVersion = "10.0.11"

val circeVersion = "0.9.2"

scalacOptions in ThisBuild := Seq(
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ypartial-unification",
  "-language:postfixOps"
)

lazy val root = (project in file(".")).
  settings(
    name := "writethedocs",
    version := "0.5.3",
    scalaVersion := "2.12.5",
    organization := "io.cyberdolphin"
  ).enablePlugins(SbtTwirl)

//mainClass in (Compile, run) := Some("...")

libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files" % "3.4.0",
    "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
    "org.scalatest" %% "scalatest" % "3.0.0",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "de.heikoseeberger" %% "akka-http-circe" % "1.19.0" % "test"
  )

TwirlKeys.templateImports += "io.cyberdolphin.writethedocs._"


