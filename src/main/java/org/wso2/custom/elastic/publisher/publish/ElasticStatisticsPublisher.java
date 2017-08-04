package org.wso2.custom.elastic.publisher.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import java.util.ArrayList;
import java.util.Map;

public class ElasticStatisticsPublisher {

    private static final Log log = LogFactory.getLog(ElasticStatisticsPublisher.class);


    public static void process( PublishingFlow publishingFlow ) {

        Map<String, Object> mapping = publishingFlow.getObjectAsMap();

        mapping.put("flowid", publishingFlow.getMessageFlowId());
        mapping.put("host", PublisherUtil.getHostAddress());

        mapping.put("type",publishingFlow.getEvent(0).getComponentType());
        mapping.put("name",publishingFlow.getEvent(0).getComponentName());

        boolean success = true;

        ArrayList<PublishingEvent> events = publishingFlow.getEvents();

        for ( PublishingEvent event:events ) {
//            log.info(event.getComponentType());
//            log.info(event.getComponentName());
//            log.info(event.getFaultCount());
//            log.info(event.getEntryPoint());

            if( event.getFaultCount()>0 ){
                success = false;
                break;
            }
        }

        mapping.put("success",success);

        log.info("FlowID : " + mapping.get("flowid"));
        log.info("Host : " + mapping.get("host"));
        log.info("Type : " + mapping.get("type"));
        log.info("Name : " + mapping.get("name"));
        log.info("Success : " + mapping.get("success"));


        ObjectMapper objectMapper = new ObjectMapper();

    }
}
