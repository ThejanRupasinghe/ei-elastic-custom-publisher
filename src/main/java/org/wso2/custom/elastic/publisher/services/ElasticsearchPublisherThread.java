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
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import java.util.ArrayList;
import java.util.Map;

/**
 * This thread dequeues data maps from the allMappingsQueue, converts them to json strings and
 * invokes publishing to Elasticsearch
 */
public class ElasticsearchPublisherThread extends Thread {

    private static final Log log = LogFactory.getLog(ElasticsearchPublisherThread.class);

    // To stop running
    private volatile boolean shutdownRequested = false;

    private TransportClient client;

    // Whether nodes connected and can publish
    boolean isPublishing = true;

    @Override
    public void run() {
        if(log.isDebugEnabled()){
            log.debug("Elasticsearch publisher thread started.");
        }

        // While not shutdown
        while (!shutdownRequested) {
            // First check whether the event queue is empty
            if (ElasticStatisticsPublisher.getAllMappingsQueue().isEmpty()) {
                try {
                    // Sleep for 1 second if the queue is empty
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Publisher Thread interrupted", e);
                    Thread.currentThread().interrupt();
                    throw new RuntimeException();
                }
            } else {
                if (isPublishing) {
                    // If nodes has connected before, check whether nodes are not connected now
                    if (client.connectedNodes().isEmpty()) {
                        // Log only once
                        log.info("No available Elasticsearch nodes to connect. Waiting for nodes... ");
                        isPublishing = false;
                    }
                } else {
                    // If nodes has not connected before, check whether node are connected now
                    if (!(client.connectedNodes().isEmpty())) {
                        // Log only once
                        log.info("Elasticsearch node connected");
                        isPublishing = true;
                    }
                }

                if (!isPublishing) {
                    try {
                        // Sleep for 5 seconds if no nodes are available
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.warn("Publisher Thread interrupted", e);
                        Thread.currentThread().interrupt();
                        throw new RuntimeException();
                    }
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();

                    ArrayList<String> jsonStringList = new ArrayList<>();

//                    // While the map queue is not empty
//                    while (!ElasticStatisticsPublisher.getAllMappingsQueue().isEmpty()) {
//
//                        // Dequeue Map from the queue
//                        Map<String, Object> map = ElasticStatisticsPublisher.getAllMappingsQueue().poll();
//
//                        try {
//                            jsonStringList.add(objectMapper.writeValueAsString(map));
//                        } catch (JsonProcessingException e) {
//                            log.error("Cannot convert to json", e);
//                        }
//                    }

                    long startTime = System.currentTimeMillis();

                    // Publish a fixed size bulk
                    while (ElasticObserverConstants.PUBLISHING_BULK_SIZE > jsonStringList.size()) {

                        // Dequeue Map from the queue
                        Map<String, Object> map = ElasticStatisticsPublisher.getAllMappingsQueue().poll();

                        if (map != null) {
                            try {
                                String jsonString = objectMapper.writeValueAsString(map);
                                jsonStringList.add(jsonString);

                                if(log.isDebugEnabled()){
                                    log.debug("Added JSON String: " + jsonString);
                                }
                            } catch (JsonProcessingException e) {
                                log.error("Cannot convert to json", e);
                            }
                        }


                        if((System.currentTimeMillis()-startTime)>5000){
                            if(log.isDebugEnabled()){
                                log.debug("Polling time-out exceeded. Publishing collected events.(<500)");
                            }

                            break;
                        }
                    }

                    // Publish the json string list
                    ElasticStatisticsPublisher.publish(jsonStringList, client);

                    if(log.isDebugEnabled()){
                        log.info("Published :" + jsonStringList.size() + " events");
                    }
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
     * @return boolean shutdownRequested
     */
    public boolean getShutdown() {
        return shutdownRequested;
    }

    /**
     * @param transportClient configured TransportClient
     */
    public void setClient(TransportClient transportClient) {
        client = transportClient;
    }
}
