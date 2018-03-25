package io.mwielocha.writethedocs

/**
  * Created by mwielocha on 22/10/2016.
  */
case class RequestDetails(
  method: String,
  uri: String,
  contentType: String,
  headers: List[ValueDetails],
  body: Seq[Content],
  params: List[ValueDetails]
) {

  val id: String = {
    s"$method-$uri"
      .replaceAll("/|\\{|\\}", "")
      .toLowerCase
  }
}


