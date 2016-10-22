package io.cyberdolphin.ahsdr

/**
  * Created by mwielocha on 22/10/2016.
  */
case class RouteDetails(
  request: RequestDetails,
  response: ResponseDetails
) {
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case RouteDetails(otherRequest, otherResponse) =>
        request.uri == otherRequest.uri &&
          request.method == otherRequest.method
      case _ => false
    }
  }
}
