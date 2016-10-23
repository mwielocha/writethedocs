package io.cyberdolphin.writethedocs

/**
  * Created by mwielocha on 22/10/2016.
  */
case class ResponseDetails(
  contentType: String,
  headers: List[ValueDetails],
  body: Option[String],
  statusCode: Int
)
