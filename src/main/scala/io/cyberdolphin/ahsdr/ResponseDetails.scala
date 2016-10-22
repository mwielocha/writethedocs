package io.cyberdolphin.ahsdr

/**
  * Created by mwielocha on 22/10/2016.
  */
case class ResponseDetails(
  contentType: String,
  headers: Map[String, String],
  body: Option[String],
  statusCode: Int
)
