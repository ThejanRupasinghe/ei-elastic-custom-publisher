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

    private volatile boolean shutdownRequested = false;

    public void run (){

        while (!(shutdownRequested)) {
            if (ElasticStatisticsPublisher.all.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {

                ObjectMapper objectMapper = new ObjectMapper();
                ArrayList<String> jsonStringList = new ArrayList<String>();


                if (ElasticMediationFlowObserver.getClient().connectedNodes().isEmpty()) {
                    log.info("NO NODES");
                }else{
                    while (!(ElasticStatisticsPublisher.all.isEmpty())) {

                        Map<String, Object> map = ElasticStatisticsPublisher.all.poll();

                        try {
                            jsonStringList.add(objectMapper.writeValueAsString(map));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    }
                    ElasticStatisticsPublisher.publish(jsonStringList, ElasticMediationFlowObserver.getClient());
                }
            }
        }
    }

    public void shutdown() {
        if (log.isDebugEnabled()) {
            log.debug("Statistics reporter thread is being stopped");
        }
        shutdownRequested = true;
    }


}
