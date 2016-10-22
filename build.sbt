
// Generated with scalagen

val akkaVersion = "2.4.11"

lazy val root = (project in file(".")).
  settings(
    name := "akka-http-self-documenting-routes",
    version := "1.0",
    scalaVersion := "2.11.8",
    organization := "io.cyberdolphin"
  ).enablePlugins(SbtTwirl)

//mainClass in (Compile, run) := Some("...")

libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files" % "2.16.0",
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
    "org.scalatest" %% "scalatest" % "3.0.0",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion
  )

TwirlKeys.templateImports += "io.cyberdolphin.ahsdr._"

