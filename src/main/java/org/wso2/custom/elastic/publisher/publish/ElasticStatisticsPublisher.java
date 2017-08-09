package org.wso2.custom.elastic.publisher.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
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
    public static String process(PublishingFlow publishingFlow) {

        Map<String, Object> mapping = new HashMap<String, Object>();

        // Adds message flow id and host
        mapping.put("flowid", publishingFlow.getMessageFlowId());
        mapping.put("host", PublisherUtil.getHostAddress());

        // First event contains the type and the name of the service
        mapping.put("type", publishingFlow.getEvent(0).getComponentType());
        mapping.put("name", publishingFlow.getEvent(0).getComponentName());

        // Takes start time of the first event as the timestamp
        long time = publishingFlow.getEvent(0).getStartTime();
        Date date = new Date(time);

        // Formatting timestamp according to Elasticsearch
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        timeFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        String formattedTime = timeFormat.format(date);

        String timestampElastic = formattedDate + "T" + formattedTime + "Z";
        mapping.put("@timestamp", timestampElastic);

        // Whether the message flow is success or not
        boolean success = true;

        ArrayList<PublishingEvent> events = publishingFlow.getEvents();

        // If any event contains a fault success=false
        for (PublishingEvent event : events) {

            if (event.getFaultCount() > 0) {
                success = false;
                break;
            }
        }

        mapping.put("success", success);

        if (log.isDebugEnabled()) {
            log.debug("FlowID : " + mapping.get("flowid"));
            log.debug("Host : " + mapping.get("host"));
            log.debug("Type : " + mapping.get("type"));
            log.debug("Name : " + mapping.get("name"));
            log.debug("Success : " + mapping.get("success"));
            log.debug("Timestamp : " + mapping.get("@timestamp"));
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {

            // Converts Map to json string
            String jsonString = objectMapper.writeValueAsString(mapping);

            return jsonString;

        } catch (JsonProcessingException e) {

            log.error("Error in converting to json string " + e);

            return null;

        }

    }

    /**
     * Publishes the simplified json to Elasticsearch using the Transport client
     *
     * @param jsonToSend json string to be published to Elasticsearch
     * @param client     elasticsearch Transport client
     * @return ID of the indexed document if success, null if failed
     */
    public static String publish(String jsonToSend, TransportClient client) {

        if (log.isDebugEnabled()) {
            log.info("Sending json to Elasticsearch : " + jsonToSend);
        }

        log.info(jsonToSend);

        try {

            // publishing to Elasticsearch
            IndexResponse response = client.prepareIndex("test_eidata", "data")
                    .setSource(jsonToSend)
                    .get();

            log.info(response.getId());

            // id of the indexed document
            return response.getId();

        } catch (NoNodeAvailableException e) {

            log.error("No available Elasticsearch Nodes to connect. Please give correct configurations and run Elasticsearch.");

            return null;

        }

    }
}
