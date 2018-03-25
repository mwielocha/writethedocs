package io.mwielocha.writethedocs.scalatest

import io.mwielocha.writethedocs.DocWriter
import org.scalatest.{Args, Status, Suite}

/**
  * Created by mwielocha on 23/10/2016.
  */
trait WriteTheDocs extends DocWriter with Suite {

  abstract override def run(testName: Option[String], args: Args): Status = {
    val status = super.run(testName, args)
    if(status.succeeds()) {
      writeTheTemplate()
    }

    status
  }
}
