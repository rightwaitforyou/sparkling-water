/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.spark.h2o.backends.external

import org.apache.spark.Logging
import org.apache.spark.h2o.backends.SparklingBackend
import org.apache.spark.h2o.utils.NodeDesc
import org.apache.spark.h2o.{H2OConf, H2OContext}
import water.api.RestAPIManager
import water.{H2O, H2OStarter}


class ExternalH2OBackend(val hc: H2OContext) extends SparklingBackend with ExternalBackendUtils with Logging{

  override def init(): Array[NodeDesc] = {
    // Start H2O in client mode and connect to existing H2O Cluster
    logTrace("Starting H2O on client mode and connecting it to existing h2o cluster")
    val h2oClientArgs = getH2OClientArgs(hc.getConf)
    logDebug(s"Arguments used for launching h2o client node: ${h2oClientArgs.mkString(" ")}")
    H2OStarter.start(h2oClientArgs, false)

    // Register web API for client
    RestAPIManager.registerClientWebAPI(hc)
    H2O.finalizeRegistration()

    if(hc.getConf.numOfExternalH2ONodes.isDefined){
      H2O.waitForCloudSize(-1, hc.getConf.numOfExternalH2ONodes.get.toInt)
    }

    cloudMembers
  }

  override def stop(stopSparkContext: Boolean): Unit = {
    if (stopSparkContext) hc.sparkContext.stop()
    H2O.orderlyShutdown(1000)
    H2O.exit(0)
  }

  override def checkAndUpdateConf(conf: H2OConf): H2OConf = {
    super.checkAndUpdateConf(conf)

    if(conf.cloudName.isEmpty){
      throw new IllegalArgumentException(
        """Cloud name has to be specified when using external backend cluster mode. It can be set either using H2OConf
          |instance or via 'spark.ext.h2o.cloud.name' spark configuration property""".stripMargin)
    }
    conf
  }
}