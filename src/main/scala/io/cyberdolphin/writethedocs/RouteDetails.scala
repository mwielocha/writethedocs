package io.cyberdolphin.writethedocs

/**
  * Created by mwielocha on 22/10/2016.
  */
case class RouteDetails(
  request: RequestDetails,
  response: ResponseDetails,
  description: Option[String] = None
) {

  def withDescription(description: String) = {
    copy(description = Some(description))
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case RouteDetails(otherRequest, otherResponse, _) =>
        request.id == otherRequest.id &&
          response.statusCode == otherResponse.statusCode
      case _ => false
    }
  }

  override def hashCode(): Int = {
    request.id.hashCode +
      response.statusCode.hashCode()
  }
}
