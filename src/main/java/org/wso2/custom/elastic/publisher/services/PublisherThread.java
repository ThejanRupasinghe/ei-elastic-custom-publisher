package org.wso2.custom.elastic.publisher.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.custom.elastic.publisher.observer.ElasticMediationFlowObserver;
import org.wso2.custom.elastic.publisher.publish.ElasticStatisticsPublisher;

import java.util.ArrayList;
import java.util.Map;

public class PublisherThread extends Thread {

    private static final Log log = LogFactory.getLog(PublisherThread.class);

    // To stop running
    private volatile boolean shutdownRequested = false;


    public void run (){

        // While not shutdown
        while (!(shutdownRequested)) {

            if (ElasticStatisticsPublisher.allMappingsQueue.isEmpty()) {

                try {
                    // Sleep for 1 second if the queue is empty
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }

            } else {

                ObjectMapper objectMapper = new ObjectMapper();

                ArrayList<String> jsonStringList = new ArrayList<String>();


                if (ElasticMediationFlowObserver.getClient().connectedNodes().isEmpty()) {

                    log.info("No available Elasticsearch nodes to connect. Waiting for nodes... ");

                    try {
                        // Sleep for 5 seconds if no nodes are available
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }

                }else{

                    // While the map queue is not empty
                    while (!(ElasticStatisticsPublisher.allMappingsQueue.isEmpty())) {

                        // Dequeue Map from the queue
                        Map<String, Object> map = ElasticStatisticsPublisher.allMappingsQueue.poll();

                        try {
                            jsonStringList.add(objectMapper.writeValueAsString(map));
                        } catch (JsonProcessingException e) {
                            log.error("Cannot convert to json");
                        }

                    }

                    // Publish the json string list
                    ElasticStatisticsPublisher.publish(jsonStringList, ElasticMediationFlowObserver.getClient());
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

}
