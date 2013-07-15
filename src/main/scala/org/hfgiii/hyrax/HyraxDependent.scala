package org.hfgiii.hyrax

import akka.pattern.CircuitBreaker
/*
 * File: HyraxDependent.scala 
 * Date: 7/8/13
 * Time: 7:30 AM	
 *
 * Copyright (c) Saks Fifth Avenue.
 * 12 East 49th Street, New York City, New York, 10017, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Saks
 * Fifth Avenue. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Saks.
 *
 */
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
