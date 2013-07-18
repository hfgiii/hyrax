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
import scala.collection.immutable.HashMap

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

    case class HttpDependencyConfiguration(sr:Future[SendReceive],
                                           dependent:DependentFunctionTrait = helloWorldDependent,
                                           fallback:FallbackTrait           = goodbyeCruelWordFallback,
                                           retries:Int                      = 1 ,
                                           timeout:Duration                 = FiniteDuration(100,MICROSECONDS),
                                           dependents:Int                   = 10) extends DependencyTrait

    case class AccumulatedDependency(fallback:FallbackTrait    = goodbyeCruelWordFallback,
                                     retries:Int               = 1 ,
                                     timeout:Duration          = FiniteDuration(100,MICROSECONDS))

    sealed trait DependentConfig

    sealed trait FallbackTrait

    trait FallbackConfig0[FO] extends FallbackTrait {

       val fallback   : () => FO

    }

    trait FallbackConfig1[FI,FO] extends FallbackTrait {

       val fallback   : FI => FO

    }

    sealed trait DependentFunctionTrait

    trait DependentFunctionConfig0[OO] extends DependentFunctionTrait {
      val runfunc     : () => OO
    }

    trait DependentFunctionConfig1[II,OO] extends DependentFunctionTrait {
      val runfunc    :  II => OO
    }

    trait InsureDependent[C <: DependencyTrait] {
      def insure    (numberOfDependents:Int)              : ServiceDependent[C]
    }

    trait ServiceDependent[C <: DependencyTrait] {
      def of [I,O](service: I => O)  : RequirementDependent[C]
    }

    trait RequirementDependent[C <: DependencyTrait] {
      def requiring (config: => ConfigAccumulator) : C
    }

     def fallback[FI,FO](fb:(FI) => FO):ConfigAccumulator = {
        val caccum   = new ConfigAccumulator()
        val fbConfig = new FallbackConfig1[FI,FO] {
                          val fallback = fb
                       }

        caccum + ("fallback" -> AccumulatedDependency(fallback = fbConfig))
     }


    def become (ca:HttpDependencyConfiguration):HttpDependencyConfiguration = ca

    class ConfigAccumulator(dmap:HashMap[String,AccumulatedDependency] = HashMap.empty[String,AccumulatedDependency]) {
        val dependencyMap = dmap

      def + (v:(String, AccumulatedDependency)):ConfigAccumulator = {
           val dmap = dependencyMap + v

           new ConfigAccumulator(dmap)
      }

      def get(key:String):Option[AccumulatedDependency] =
           dependencyMap.get(key)

      def timeout(to:Duration):ConfigAccumulator = {
            this  + ("timeout" -> AccumulatedDependency(timeout = to))
      }

      def retries(r:Int):ConfigAccumulator = {
            this  + ("retries" -> AccumulatedDependency(retries = r))
      }

    }

  implicit def HttpInsured(http:Http.type): InsureDependent[HttpDependencyConfiguration]  = {
    implicit val timeout: Timeout = 10 seconds
    implicit val system           = ActorSystem()
    implicit val ec               = ExecutionContext.Implicits.global

    //val config:Config = ConfigFactory.load

    val host = "localhost"  //config.getString("hyrax.host")
    val port = "8080"       //config.getString("hyrax.port")
    val sr   =
        for { Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(host,port.toInt) }
        yield sendReceive(connector)

    new InsureDependent[HttpDependencyConfiguration] {

      def insure (numberOfDependents:Int)  : ServiceDependent[HttpDependencyConfiguration]  =
         new ServiceDependent[HttpDependencyConfiguration] {

           def of [I,O](service:I => O)    : RequirementDependent[HttpDependencyConfiguration]  =
             new RequirementDependent[HttpDependencyConfiguration] {

                  val dependencyConfig =
                  new DependentFunctionConfig1[I,O] {
                       val runfunc = service
                  }

                def requiring(config: => ConfigAccumulator):HttpDependencyConfiguration = {
                    val cfg = config
                    val accumDependency =
                    for {
                      fb <- cfg.get("fallback")
                      to <- cfg.get("timeout")
                      rt <- cfg.get("retries")
                    } yield AccumulatedDependency(fallback = fb.fallback,timeout = to.timeout,retries = rt.retries)

                    accumDependency match {
                      case Some(AccumulatedDependency(fb,rt,to))  =>
                             HttpDependencyConfiguration(sr,
                                                         dependent  = dependencyConfig ,
                                                         fallback   = fb               ,
                                                         retries    = rt               ,
                                                         timeout    = to               ,
                                                         dependents = numberOfDependents)
                      case None =>
                             HttpDependencyConfiguration(sr,
                                                         dependent  = dependencyConfig ,
                                                         dependents = numberOfDependents)
                    }
                }
             }
         }
     }
  }

}
