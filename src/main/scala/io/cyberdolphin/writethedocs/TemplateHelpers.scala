package io.cyberdolphin.writethedocs

import spray.json._

import scala.util.{Failure, Success, Try}

/**
  * Created by mwielocha on 22/10/2016.
  */
object TemplateHelpers {

  def contentTypeBlock(contentType: String): String = {
    contentType match {
      case "application/json" => "json"
      case _ => ""
    }
  }

  def prettify(content: String): String = {
    Try(content.parseJson)
      .map(_.prettyPrint)
      .getOrElse(content)
  }

  def paramType(param: Any): String = {
    Try(param.toString.toDouble) match {
      case Success(_) => "number"
      case Failure(_) => "string"
    }
  }
}
