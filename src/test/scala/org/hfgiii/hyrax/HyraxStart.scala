package org.hfgiii.hyrax

import spray.can.Http
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: hfgiii
 * Date: 7/7/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */

object HyraxStart  {

      val          runfunc = (str:String) => str
      val          fbfunc  = (str:String) => str
      implicit val httpc   = HttpDependencyConfiguration (null)


  def main(args: Array[String]) {
    val httpCfg =
    Http insure 10 of runfunc requiring {
      //new ConfigAccumulator().retries(4).timeout(FiniteDuration(100,MICROSECONDS))
      fallback(fbfunc) retries(4) timeout(FiniteDuration(100,MICROSECONDS))
    }

    println(httpCfg)
  }

}
