package org.wso2.custom.elastic.publisher.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ElasticStatisticsPublisher {

    private static final Log log = LogFactory.getLog(ElasticStatisticsPublisher.class);

    /**
     * Processes the PublishingFlow into a simple json format
     *
     * @param publishingFlow PublishingFlow object which contains the publishing events
     * @return if success processed json, if exception null
     */
    public static ArrayList<String> process(PublishingFlow publishingFlow) {

        ArrayList<Map<String, Object>> allMappings = new ArrayList<Map<String, Object>>();

        // Takes message flow id and host
        String flowid = publishingFlow.getMessageFlowId();
        String host = PublisherUtil.getHostAddress();

        // Takes main message details
        /*
        Map<String, Object> mappingApiProxy = new HashMap<String, Object>();
        mappingApiProxy.put("flowid", flowid);
        mappingApiProxy.put("host", host);
        mappingApiProxy.put("type", publishingFlow.getEvent(0).getComponentType());
        mappingApiProxy.put("name", publishingFlow.getEvent(0).getComponentName());
        mappingApiProxy.put("@timestamp", getFormattedDate(publishingFlow.getEvent(0).getStartTime()));
        if (publishingFlow.getEvent(0).getFaultCount() > 0) {
            mappingApiProxy.put("success", false);
        } else {
            mappingApiProxy.put("success", true);
        }
        allMappings.add(mappingApiProxy);
        */

        ArrayList<PublishingEvent> events = publishingFlow.getEvents();


        for (PublishingEvent event : events) {

            String componentType = event.getComponentType();
            String componentName = event.getComponentName();
            boolean success = true;

            // TODO: 8/15/17 how to consider inbound endpoints
            if (componentType.equals("Sequence") || componentType.equals("Endpoint") || componentType.equals("API") || componentType.equals("Proxy Service")) {
                Map<String, Object> mapping = new HashMap<String, Object>();

                if (!(componentName.equals("API_INSEQ") || componentName.equals("API_OUTSEQ") ||
                        componentName.equals("PROXY_INSEQ") || componentName.equals("PROXY_OUTSEQ") ||
                        componentName.equals("AnonymousEndpoint") )) {

                    mapping.put("type", componentType);
                    mapping.put("name", componentName);
                    mapping.put("flowid", flowid);
                    mapping.put("host", host);
                    mapping.put("@timestamp", getFormattedDate(event.getStartTime()));
                    if (event.getFaultCount() > 0) {
                        success = false;
                    }
                    mapping.put("success", success);
                    allMappings.add(mapping);
                }

            }
        }


        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<String> jsonStringList = new ArrayList<String>();


        try {

            for (Map<String, Object> map : allMappings) {

                jsonStringList.add(objectMapper.writeValueAsString(map));

            }

            return jsonStringList;

        } catch (JsonProcessingException e) {

            log.error("Error in converting to json string " + e);

            return null;

        }

    }

    /**
     * Publishes the simplified json to Elasticsearch using the Transport client
     *
     * @param jsonsToSend json string to be published to Elasticsearch
     * @param client      elasticsearch Transport client
     * @return ID of the indexed document if success, null if failed
     */
    public static String publish(ArrayList<String> jsonsToSend, TransportClient client) {

        try {

            if (client.connectedNodes().isEmpty()) {
                log.info("NO NODES");
            }

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (String jsonString : jsonsToSend) {
                bulkRequest.add(client.prepareIndex("test_eidata", "data")
                        .setSource(jsonString)
                );
                log.info(jsonString);
            }

            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                log.info("No Failures");
            }
            return null;

        } catch (NoNodeAvailableException e) {

            log.error("No available Elasticsearch Nodes to connect. Please give correct configurations and run Elasticsearch.");

            return null;

        }

    }

    private static String getFormattedDate(long time) {

        Date date = new Date(time);

        // Formatting timestamp according to Elasticsearch
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
//        timeFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        String formattedTime = timeFormat.format(date);

        String timestampElastic = formattedDate + "T" + formattedTime + "Z";

        return timestampElastic;
    }
}
