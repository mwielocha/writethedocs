package io.cyberdolphin.ahsdr

/**
  * Created by mwielocha on 22/10/2016.
  */
case class RequestDetails(
  method: String,
  uri: String,
  contentType: String,
  headers: Map[String, String],
  body: Option[String],
  params: Seq[(String, String)]
) {

  val id = {
    s"$method-$uri"
      .replaceAll("/|\\{|\\}", "")
      .toLowerCase
  }
}
