package org.hfgiii

import akka.actor.{ExtensionId, Extension}

/**
 * User: hfgiii
 * Date: 7/7/13
 * Time: 4:28 PM
 */

/*
                insure (10) of  {
                     myCall   {
                       withFallBack  =  fallback
                       retries           =  4
                       timeout         =   20 ms
                   }
                }
                insure 10 of  myCall
                        retrying 4 times afterEach  20 ms  timeout   fallingBackTo   fallback
             */
package object hyrax  {
    class DependentConfig


       trait InsureDependent[T <: Extension, IDIO <: ExtensionId[T] ] {
          def insure(numberOfDependents:Int)(implicit io:IDIO):InsureDependent[T,IDIO]
          def of [I,O](service:(I) => O) :Unit
       }
}
