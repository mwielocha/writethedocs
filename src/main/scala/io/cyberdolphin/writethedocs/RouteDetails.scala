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

  val score = 1000 -
    response.statusCode +
    description.map(_.length)
      .getOrElse(0)
    request.headers.size +
    request.params.size +
    request.headers.foldLeft(0) {
    case (total, d) => total +
      d.description
        .map(_.length)
        .getOrElse(0)
    } +
    request.params.foldLeft(0) {
      case (total, d) => total +
        d.description
          .map(_.length)
          .getOrElse(0)
    } + request.body
      .map(_.length)
      .getOrElse(0)
}
