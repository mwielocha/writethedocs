package io.mwielocha.writethedocs

/**
  * Created by mwielocha on 22/10/2016.
  */
case class ResponseDetails(
  headers: List[ValueDetails],
  contentType: String,
  body: Content,
  statusCode: Int
)
