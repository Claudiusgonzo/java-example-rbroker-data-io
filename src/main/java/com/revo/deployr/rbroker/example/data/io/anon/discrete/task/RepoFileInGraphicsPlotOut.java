/*
 * RepoFileInGraphicsPlotOut.java
 *
 * Copyright (c) 2010-2016, Microsoft Corporation
 *
 * This program is licensed to you under the terms of Version 2.0 of the
 * Apache License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0) for more details.
 *
 */
package com.revo.deployr.rbroker.example.data.io.anon.discrete.task;

import com.revo.deployr.client.*;
import com.revo.deployr.client.auth.*;
import com.revo.deployr.client.auth.basic.*;
import com.revo.deployr.client.data.*;
import com.revo.deployr.client.factory.*;
import com.revo.deployr.client.params.*;
import com.revo.deployr.client.broker.*;
import com.revo.deployr.client.broker.config.*;
import com.revo.deployr.client.broker.task.*;
import com.revo.deployr.client.broker.options.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.IOUtils;

import org.apache.log4j.Logger;

public class RepoFileInGraphicsPlotOut
                implements RTaskListener, RBrokerListener {

    private static Logger log =
        Logger.getLogger(RepoFileInGraphicsPlotOut.class);
    private static CountDownLatch latch = new CountDownLatch(1);

    /*
     * Hipparcos star dataset URL endpoint.
     */
    private static String HIP_DAT_URL =
        "http://astrostatistics.psu.edu/datasets/HIP_star.dat";

    public static void main(String args[]) throws Exception {

        new RepoFileInGraphicsPlotOut();
    }

    public RepoFileInGraphicsPlotOut() {

        RBroker rBroker = null;

        try {

            /*
             * Determine DeployR server endpoint.
             */
            String endpoint = System.getProperty("endpoint");
            log.info("[ CONFIGURATION  ] Using endpoint [ " +
                                        endpoint + " ]");

            /*
             * Build configuration to initialize RBroker instance.
             */
            DiscreteBrokerConfig brokerConfig =
                new DiscreteBrokerConfig(endpoint, null,
                    Integer.getInteger("example-max-concurrency"));

            log.info("[ CONFIGURATION  ] Using broker config " +
                                    "[ DiscreteBrokerConfig ]");

            /*
             * Establish RBroker connection to DeployR server.
             */
            rBroker =
                RBrokerFactory.discreteTaskBroker(brokerConfig);

            /*
             * Register asynchronous listeners for task and
             * broker runtime events.
             */
            rBroker.addTaskListener(this);
            rBroker.addBrokerListener(this);

            log.info("[   CONNECTION   ] Established anonymous " +
                    "discrete broker [ RBroker ].");

            /*
             * Create the DiscreteTaskOptions object·
             * to specify data inputs and output to the task.
             *
             * This options object can be used to pass standard
             * execution model parameters on execution calls. All
             * fields are optional.
             *
             * See the Standard Execution Model chapter in the
             * Client Library Tutorial on the DeployR website for
             * further details.
             */
            DiscreteTaskOptions options = 
                                    new DiscreteTaskOptions();

            /* 
             * Preload from the DeployR repository the following
             * data input file:
             * /testuser/example-data-io/hipStar.dat
             */
            TaskPreloadOptions preloadWorkspace =
                                new TaskPreloadOptions();
            preloadWorkspace.filename = "hipStar.rData";
            preloadWorkspace.directory = "example-data-io";
            preloadWorkspace.author = "testuser";
            options.preloadWorkspace = preloadWorkspace;

            log.info("[   DATA INPUT   ] Repository binary file input " +
                "set on task, [ DiscreteTaskOptions.preloadWorkspace ].");

            /*
             * Declare task for execution on broker based on
             * repository-managed R script:
             * /testuser/example-data-io/dataIO.R
             */
            RTask rTask = RTaskFactory.discreteTask("dataIO.R",
                                                    "example-data-io",
                                                    "testuser",
                                                    null, options);

            rBroker.submit(rTask);

            log.info("[   EXECUTION    ] Discrete task " +
                    "submitted to broker [ RTask ].");

            /*
             * Simple block on main thread of this sample application
             * until example RTask execution completes.
             */
            latch.await();

        } catch(Exception ex) {
            log.warn("Unexpected error: ex=" + ex);
        } finally {
            /*
             * Clean-up after example completes.
             */
            if(rBroker != null) {
                try{
                    rBroker.shutdown();
                } catch(Exception bex) {
                    log.warn("RBroker.shutdown ex=" + bex);
                }
            }
        }            

    }

    /*
     * RBroker asynchronous callback listener on RTask completion.
     */
    public void onTaskCompleted(RTask rTask, RTaskResult rTaskResult) {


        if(rTaskResult.isSuccess()) {

            log.info("[  TASK RESULT   ] Discrete task " +
                        "completed in " + rTaskResult.getTimeOnCall() +
                        "ms [ RTaskResult ].");

            /*
             * Retrieve R graphics device plots (results) called
             * unnamedplot*.png that was generated by the execution.
             */
            List<URL> results = rTaskResult.getGeneratedPlots();

            for(URL result : results) {
                log.info("[  DATA OUTPUT   ] Retrieved graphics device " +
                    "plot " + result.getFile()
                                       .substring(result.getFile()
                                       .lastIndexOf("/")+1) +
                                       " [ URL ]");
                InputStream fis = null;
                try {
                    fis = result.openStream();
                    IOUtils.toByteArray(fis);
                } catch(Exception ex) {
                    log.warn("Graphics device plot " + ex);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }
        } else {
            log.info("[  TASK RESULT   ] Discrete task " +
                    "failed, cause " + rTaskResult.getFailure());
        }

        /*
         * Unblock main thread so example application can release 
         * resources before exit.
         */
        latch.countDown();
    }

    /*
     * RBroker asynchronous callback listener on RTask error.
     */
    public void onTaskError(RTask rTask, Throwable throwable) {
        log.info("onTaskError: rTask=" + rTask +
                                ", error=" + throwable);
        /*
         * Unblock main thread so example application can release 
         * resources before exit.
         */
        latch.countDown();
    }

    public void onRuntimeError(Throwable throwable) {
        log.info("onRuntimeError: cause=" + throwable); 
        /*
         * Unblock main thread so example application can release 
         * resources before exit.
         */
        latch.countDown();
    }

    public void onRuntimeStats(RBrokerRuntimeStats stats,
                                        int maxConcurrency) {
        /*
         * Log/profile runtime RBroker stats as desired...
         */
    }

}
