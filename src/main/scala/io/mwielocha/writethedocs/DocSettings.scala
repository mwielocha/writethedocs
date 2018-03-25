package io.mwielocha.writethedocs

/**
  * Created by mwielocha on 23/10/2016.
  */
case class DocSettings(
  outputDirectory: String,
  enabled: Boolean = true,
  includeBadRequests: Boolean = false,
  includeInternalServerErrors: Boolean = false
) {

  def withEnabled(e: Boolean) = {
    copy(enabled = e)
  }

  def withOutputDirectory(d: String) = {
    copy(outputDirectory = d)
  }

  def withIncludeBadRequests(b: Boolean) = {
    copy(includeBadRequests = b)
  }

  def withInternalServerErrors(b: Boolean) = {
    copy(includeInternalServerErrors = b)
  }
}

