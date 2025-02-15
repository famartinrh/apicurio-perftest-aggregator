/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.perftest.agg.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;

import org.apache.commons.io.IOUtils;

import io.apicurio.perftest.agg.AggregatorConfiguration;

/**
 * @author eric.wittmann@gmail.com
 */
public class AggregatorResourceImpl implements AggregatorResource {

    @Inject
    AggregatorConfiguration config;

    /**
     * @see io.apicurio.perftest.agg.rest.AggregatorResource#uploadLog(java.lang.String, java.io.InputStream)
     */
    @Override
    public void uploadLog(String logName, InputStream data) throws IOException {
        if (!logName.endsWith(".log.zip")) {
            logName += ".log.zip";
        }
        System.out.println("Uploading log: " + logName);
        File logFile = new File(config.getLogsDirectory(), logName);
        System.out.println("Writing to: " + logFile.getAbsolutePath());
        try (InputStream from = data ; FileOutputStream to = new FileOutputStream(logFile)) {
            IOUtils.copy(from, to);
            System.out.println("Upload of '" + logName + "' completed.");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * @see io.apicurio.perftest.agg.rest.AggregatorResource#aggregate()
     */
    @Override
    public synchronized void aggregate() throws Exception {
        try {
            String shell = config.getShell();
            String processSh = config.getProcessSh();
            ProcessBuilder pb = new ProcessBuilder(shell, processSh);
            pb.environment().putIfAbsent("GATLING_HOME", System.getenv("GATLING_HOME"));
            pb.environment().putIfAbsent("LOGS_DIR", config.getLogsDirectory().getAbsolutePath());
            pb.environment().putIfAbsent("HTML_DIR", config.getHtmlDirectory().getAbsolutePath());
            pb.environment().putIfAbsent("RESULTS_DIR", config.getResultsDirectory().getAbsolutePath());

            Process p = pb.start();
            IOUtils.copy(p.getInputStream(), System.out);
            IOUtils.copy(p.getErrorStream(), System.err);
            p.waitFor();
            System.out.println("====> Aggregation complete!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerErrorException(500, e);
        }
    }

    /**
     * @see io.apicurio.perftest.agg.rest.AggregatorResource#info()
     */
    @Override
    public String info() {
        return "Hello from Apicurio Performance Test Aggregator";
    }

}
