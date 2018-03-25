package io.mwielocha.writethedocs

case class Content(
  name: Option[String] = None,
  contentType: Option[String] = None,
  body: Option[String]
)
