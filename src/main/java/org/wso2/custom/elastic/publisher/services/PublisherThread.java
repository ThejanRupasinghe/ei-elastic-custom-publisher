/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.custom.elastic.publisher.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.transport.TransportClient;
import org.wso2.custom.elastic.publisher.publish.ElasticStatisticsPublisher;

import java.util.ArrayList;
import java.util.Map;

public class PublisherThread extends Thread {

    private static final Log log = LogFactory.getLog(PublisherThread.class);

    // To stop running
    private volatile boolean shutdownRequested = false;

    private TransportClient client;

    @Override
    public void run (){

        // While not shutdown
        while (!(shutdownRequested)) {

            if (ElasticStatisticsPublisher.getAllMappingsQueue().isEmpty()) {

                try {
                    // Sleep for 1 second if the queue is empty
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Publisher Thread interrupted");
                }

            } else {

                ObjectMapper objectMapper = new ObjectMapper();

                ArrayList<String> jsonStringList = new ArrayList<String>();


                if (client.connectedNodes().isEmpty()) {

                    log.info("No available Elasticsearch nodes to connect. Waiting for nodes... ");

                    try {
                        // Sleep for 5 seconds if no nodes are available
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.warn("Publisher Thread interrupted");
                    }

                }else{

                    // While the map queue is not empty
                    while (!(ElasticStatisticsPublisher.getAllMappingsQueue().isEmpty())) {

                        // Dequeue Map from the queue
                        Map<String, Object> map = ElasticStatisticsPublisher.getAllMappingsQueue().poll();

                        try {
                            jsonStringList.add(objectMapper.writeValueAsString(map));
                        } catch (JsonProcessingException e) {
                            log.error("Cannot convert to json");
                        }

                    }

                    // Publish the json string list
                    ElasticStatisticsPublisher.publish(jsonStringList, client);
                }
            }
        }
    }

    /**
     * Shutdown thread, stop running
     */
    public void shutdown() {
        if (log.isDebugEnabled()) {
            log.debug("Statistics reporter thread is being stopped");
        }
        shutdownRequested = true;
    }

    /**
     *
     * @return boolean shutdownRequested
     */
    public boolean getShutdown() {
        return shutdownRequested;
    }

    public void setClient(TransportClient transportClient) {
        client = transportClient;
    }

}
