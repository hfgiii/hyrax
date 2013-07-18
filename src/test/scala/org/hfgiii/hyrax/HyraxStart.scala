package org.hfgiii.hyrax

import spray.can.Http
import spray.httpx.RequestBuilding._
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: hfgiii
 * Date: 7/7/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */

object HyraxStart  {

  val  get     = (url:String) => Get(url)
  val  fbfunc  = (str:String) => str


  def main(args: Array[String]) {

    val httpWrapped =
    become {
       Http insure 10 of get requiring {
           fallback {
             fbfunc
           } timeout {
                FiniteDuration(100,MICROSECONDS)
           }  retries 4

        }
    }
    println(httpWrapped)
  }

}
