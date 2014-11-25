package org.hfgiii.hyrax

import akka.pattern.CircuitBreaker
/*
 * File: HyraxDependent.scala 
 * Date: 7/8/13
 * Time: 7:30 AM	
 *
trait HyraxDependent {
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

}
