package org.wso2.custom.elastic.publisher.publish;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticStatisticPublisher {

    public void process( PublishingFlow publishingFlow ) {

        ObjectMapper objectMapper = new ObjectMapper();

    }
}
