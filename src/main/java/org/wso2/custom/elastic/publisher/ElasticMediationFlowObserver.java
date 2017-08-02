package org.wso2.custom.elastic.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.wso2.carbon.das.messageflow.data.publisher.observer.MessageFlowObserver;
import org.wso2.carbon.das.messageflow.data.publisher.observer.TenantInformation;

public class ElasticMediationFlowObserver implements MessageFlowObserver, TenantInformation{

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    public void destroy() {

    }

    public void updateStatistics(PublishingFlow publishingFlow) {
        log.info("update here");


    }

    public int getTenantId() {
        return 0;
    }

    public void setTenantId(int i) {

    }
}