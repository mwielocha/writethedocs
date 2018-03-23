package io.cyberdolphin.writethedocs

import io.circe.Printer
import io.circe.parser._

import scala.util.{Failure, Success, Try}

/**
  * Created by mwielocha on 22/10/2016.
  */
object TemplateHelpers {

  private val printer = Printer.indented("   ")

  def contentTypeBlock(contentType: String): String = {
    contentType match {
      case "application/json" => "json"
      case _ => ""
    }
  }

  def prettify(content: String): String = {
    parse(content)
      .map(_.pretty(printer))
      .getOrElse(content)
  }

  def paramType(param: Any): String = {
    Try(param.toString.toDouble) match {
      case Success(_) => "number"
      case Failure(_) => "string"
    }
  }
}
