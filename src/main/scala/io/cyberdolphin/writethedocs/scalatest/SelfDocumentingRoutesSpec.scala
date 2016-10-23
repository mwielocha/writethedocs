package io.cyberdolphin.writethedocs.scalatest

import io.cyberdolphin.writethedocs.SelfDocumentingRoutes
import org.scalatest.{Args, Status, Suite}

/**
  * Created by mwielocha on 23/10/2016.
  */
trait SelfDocumentingRoutesSpec extends SelfDocumentingRoutes with Suite {

  override def run(testName: Option[String], args: Args): Status = {
    val status = super.run(testName, args)
    if(status.succeeds()) {
      selfDocument()
    }

    status
  }
}
