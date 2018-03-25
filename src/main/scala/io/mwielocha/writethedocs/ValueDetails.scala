package io.mwielocha.writethedocs

/**
  * Created by mwielocha on 23/10/2016.
  */
case class ValueDetails(
  name: String,
  value: String,
  required: Option[Boolean] = None,
  `type`: Option[String] = None,
  defaultValue: Option[String] = None,
  description: Option[String] = None
) {

  def withRequired(required: Boolean) = {
    copy(required = Some(required))
  }

  def withType(`type`: String) = {
    copy(`type` = Some(`type`))
  }

  def withDefaultValue(defaultValue: String) = {
    copy(defaultValue = Some(defaultValue))
  }

  def withDescription(description: String) = {
    copy(description = Some(description))
  }
}
