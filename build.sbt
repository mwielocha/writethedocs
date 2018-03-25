
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
    version := "0.6.1",
    scalaVersion := "2.12.5",
    organization := "io.mwielocha"
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

TwirlKeys.templateImports += "io.mwielocha.writethedocs._"

// POM settings for Sonatype
homepage := Some(url("https://github.com/mwielocha/writethedocs"))
scmInfo := Some(
  ScmInfo(url("https://github.com/mwielocha/writethedocs"),
  "git@github.com:mwielocha/writethedocs.git"))

developers := List(Developer("mwielocha",
  "Mikolaj Wielocha",
  "mwielocha@icloud.com",
  url("https://github.com/mwielocha")))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle := true

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)


