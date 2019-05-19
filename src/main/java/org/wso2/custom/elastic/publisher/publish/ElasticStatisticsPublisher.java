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

package org.wso2.custom.elastic.publisher.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentType;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import org.elasticsearch.client.transport.TransportClient;
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Processes the PublishingFlow into json strings and publishes to Elasticsearch using the TransportClient
 */
public class ElasticStatisticsPublisher {

    private ElasticStatisticsPublisher() {
    }

    private static final Log log = LogFactory.getLog(ElasticStatisticsPublisher.class);

    /*
    Queue to store all the Maps of data to be converted to json strings
    This works as the event Buffer to store events before publishing. Size is configured through carbon.xml
     */
    private static Queue<Map<String, Object>> allMappingsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Processes the PublishingFlow into a simple json format
     *
     * @param publishingFlow PublishingFlow object which contains the publishing events
     */
    public static void process(PublishingFlow publishingFlow) {

        // Takes message flow id and host
        String flowid = publishingFlow.getMessageFlowId();
        String host = PublisherUtil.getHostAddress();

        ArrayList<PublishingEvent> events = publishingFlow.getEvents();

        for (PublishingEvent event : events) {
            String componentType = event.getComponentType();
            String componentName = event.getComponentName();
            //Integer componentIndex = event.getComponentIndex();
            String componentId = event.getComponentId();
            long startTime = event.getStartTime();
            long endTime = event.getEndTime();
            long duration = event.getDuration();
            String beforePayload = event.getBeforePayload();
            String afterPayload = event.getAfterPayload();
            Map contextPropertyMap = event.getContextPropertyMap();
            Map transportPropertyMap = event.getTransportPropertyMap();
            String entryPoint = event.getEntryPoint();
            Integer entryPointHashcode = event.getEntryPointHashcode();
            Integer[] children = event.getChildren();
            Integer hashCode = event.getHashCode();
            int faultCount = event.getFaultCount();
            
            
            // Map to store details of the event
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("flowid", flowid);
            mapping.put("host", host);
            mapping.put("type", componentType);
            mapping.put("name", componentName);
            //mapping.put("componentIndex", componentIndex);
            mapping.put("componentId", componentId);
            mapping.put("startTime", getFormattedDate(startTime));
            mapping.put("endTime", getFormattedDate(endTime));
            mapping.put("duration", duration);
            mapping.put("beforePayload", beforePayload);
            mapping.put("afterPayload", afterPayload);
            mapping.put("contextPropertyMap", contextPropertyMap);
            mapping.put("transportPropertyMap", transportPropertyMap);
            mapping.put("entryPoint", entryPoint);
            mapping.put("entryPointHashcode", entryPointHashcode);
            mapping.put("children", children);
            mapping.put("hashCode", hashCode);
            mapping.put("faultCount", faultCount);
            
	        // Enqueue the Map to the queue
	        allMappingsQueue.add(mapping);
        }
    }

    /**
     * Publishes the array list of simplified jsons to Elasticsearch using the Transport client
     *
     * @param jsonsToSend array list of json strings to be published to Elasticsearch
     * @param client      elasticsearch Transport client
     */
    public static void publish(List<String> jsonsToSend, TransportClient client) {
        try {
            // Prepares the bulk request
            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (String jsonString : jsonsToSend) {
                bulkRequest.add(client.prepareIndex("eidata", "data")
                        .setSource(jsonString, XContentType.JSON)
                );
            }

            // Send the bulk request
            BulkResponse response = bulkRequest.get();

            if (log.isDebugEnabled()) {
                //log.debug("Bulk Request took " + response.getTookInMillis() + " milliseconds");
                log.debug("Bulk Request took " + response.getTook().getMillis() + " milliseconds");
            }
        } catch (NoNodeAvailableException e) {
            log.error("No available Elasticsearch Nodes to connect. Please give correct configurations and" +
                    " run Elasticsearch.", e);
        } catch (ElasticsearchSecurityException e) {
            log.error("Elasticsearch user lacks access to write.", e);
            client.close();
        }
    }

    /**
     * Takes time in milliseconds and returns the formatted date and time according to Elasticsearch
     *
     * @param time long time in millis
     * @return timeStamp formatted according to the Elasticsearch
     */
    private static String getFormattedDate(long time) {
        Date date = new Date(time);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        timeFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        String formattedTime = timeFormat.format(date);

        return formattedDate + "T" + formattedTime + "Z";
    }

    /**
     * @return queue which includes all the Maps that are to be converted into json strings
     */
    public static Queue<Map<String, Object>> getAllMappingsQueue() {
        return allMappingsQueue;
    }
}
