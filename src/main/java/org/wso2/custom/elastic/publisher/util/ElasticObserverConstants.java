package org.wso2.custom.elastic.publisher.util;

import org.wso2.carbon.das.data.publisher.util.DASDataPublisherConstants;

public class ElasticObserverConstants {

    // Constant to get data from carbon.xml
    private static final String OBSERVER_ELEMENT = DASDataPublisherConstants.STAT_CONFIG_ELEMENT + ".ElasticObserver";
    public static final String OBSERVER_HOST = OBSERVER_ELEMENT + ".Host";
    public static final String OBSERVER_CLUSTER_NAME = OBSERVER_ELEMENT + ".ClusterName";
    public static final String OBSERVER_PORT = OBSERVER_ELEMENT + ".Port";
    public static final String QUEUE_SIZE = OBSERVER_ELEMENT + ".QueueSize";

}
