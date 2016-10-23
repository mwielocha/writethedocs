package io.cyberdolphin.writethedocs

/**
  * Created by mwielocha on 22/10/2016.
  */
case class RequestDetails(
  method: String,
  uri: String,
  contentType: String,
  headers: List[ValueDetails],
  body: Option[String],
  params: List[ValueDetails]
) {

  val id = {
    s"$method-$uri"
      .replaceAll("/|\\{|\\}", "")
      .toLowerCase
  }
}
