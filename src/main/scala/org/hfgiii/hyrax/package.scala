package org.hfgiii

import akka.actor.{ActorSystem, ExtensionId, Extension}
import scala.concurrent.duration._
import spray.can.Http
import spray.can.HttpExt
import akka.io.IO
import com.typesafe.config.{ConfigFactory, Config}
import spray.client.pipelining._
import akka.util.Timeout
import akka.pattern._
import scala.concurrent.{Future, ExecutionContext}

/**
 * User: hfgiii
 * Date: 7/7/13
 * Time: 4:28 PM
 */

/*  Http insure (10) of  {
       myCall   {
         withFallBack  =  fallback
         retries       =  4
         timeout       =  20 ms
       }
    }

    OR
     insure 10 of  myCall
            retrying 4 times afterEach  20 ms  timeout fallingBackTo fallback
*/
package object hyrax  {

    val goodbyeCruelWordFallback =
      new FallbackConfig0[Unit] {
         val fallback =  () => println("Good Bye Cruel World")
      }

    val helloWorldDependent =
      new DependentFunctionConfig0[Unit] {
        val runfunc   =  () => println("Hello World")
      }

    sealed trait DependencyTrait

    case object NoDependency extends DependencyTrait

    case class DependencyConfiguration(dependent:DependentFunctionTrait = helloWorldDependent,
                                       fallback:FallbackTrait           = goodbyeCruelWordFallback,
                                       retries:Int                      = 1 ,
                                       timeout:Duration                 = FiniteDuration(100,MICROSECONDS),
                                       dependents:Int                   = 10) extends DependencyTrait

    case class HttpDependencyConfiguration(sr:Future[SendReceive],dependent:DependentFunctionTrait = helloWorldDependent,
                                           fallback:FallbackTrait       = goodbyeCruelWordFallback,
                                           retries:Int                  = 1 ,
                                           timeout:Duration             = FiniteDuration(100,MICROSECONDS),
                                           dependents:Int               = 10) extends DependencyTrait

    sealed trait DependentConfig

    sealed trait FallbackTrait

    trait FallbackConfig0[FO] extends FallbackTrait {

       val fallback   : () => FO

    }

    trait FallbackConfig1[FI,FO] extends FallbackTrait {

       val fallback   : (FI) => FO

    }

    sealed trait DependentFunctionTrait

    trait DependentFunctionConfig0[OO] extends DependentFunctionTrait {

      val runfunc     : () => OO

    }

    trait DependentFunctionConfig1[II,OO] extends DependentFunctionTrait {

      val runfunc    : (II) => OO

    }

    trait InsureDependent[T <: Extension, IDIO <: ExtensionId[T]] {
       val dependencyTrait:DependencyTrait

       def insure  (numberOfDependents:Int) : InsureDependent[T,IDIO]
       def of [I,O](service:(I) => O)       : InsureDependent[T,IDIO]

    }

    def fallback_=[T <: Extension, IDIO <: ExtensionId[T],FI,FO](fb:(FI) => FO)(implicit dependent:InsureDependent[T,IDIO])  {

    }

    implicit object InsureHttpDependent extends InsureDependent[HttpExt,Http.type] {
      val dependencyTrait:DependencyTrait = NoDependency
      def insure(numberOfDependents:Int):InsureDependent[HttpExt,Http.type] = {
        implicit val timeout: Timeout = 10.seconds
        implicit val system           = ActorSystem()
        implicit val ec               = ExecutionContext.Implicits.global

         val config:Config = ConfigFactory.load

         val host = config.getString("host")
         val port = config.getString("port")
         val sr =
         for { Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(host,port.toInt) } yield sendReceive(connector)

         new InsureDependent[HttpExt,Http.type] {

           val dependencyTrait:DependencyTrait = HttpDependencyConfiguration(sr,dependents = numberOfDependents)

           def insure(numberOfDependents:Int):InsureDependent[HttpExt,Http.type]     = this

           def of [I,O](service:(I) => O)       : InsureDependent[HttpExt,Http.type] =  InsureHttpDependent.of(service)
         }
      }


      def of [I,O](service:(I) => O)       : InsureDependent[HttpExt,Http.type] = this
    }
}
