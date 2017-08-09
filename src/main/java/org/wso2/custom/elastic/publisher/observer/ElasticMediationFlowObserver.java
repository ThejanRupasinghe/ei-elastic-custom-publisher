package org.wso2.custom.elastic.publisher.observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.das.messageflow.data.publisher.observer.MessageFlowObserver;

import org.wso2.custom.elastic.publisher.publish.ElasticStatisticsPublisher;
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticMediationFlowObserver implements MessageFlowObserver {

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    // Elasticsearch settings object
    private Settings settings;

    // Defines elasticsearch Transport Client as client
    private TransportClient client = null;

    ServerConfiguration serverConf = ServerConfiguration.getInstance();


    public ElasticMediationFlowObserver() {

        String clusterName = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_CLUSTER_NAME);
        String host = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_HOST);
        String portString = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_PORT);

        settings = Settings.builder().put("cluster.name", clusterName).build();

        client = new PreBuiltTransportClient(settings);

        try {

            int port = Integer.parseInt(portString);

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

        } catch (UnknownHostException e) {

            log.error("Unknown Elasticsearch Host");

        } catch (NumberFormatException e) {

            log.error("Invalid port number");
        }

    }

    public void destroy() {

        if (client != null) {
            client.close();
        }

        if (log.isDebugEnabled()) {
            log.debug("Shutting down the mediation statistics observer of Elasticsearch");
        }

    }

    public void updateStatistics(PublishingFlow publishingFlow) {
//        log.info("update starts");


        try {

            ElasticStatisticsPublisher.process(publishingFlow, client);

        } catch (Exception e) {

            log.error("Failed to update statics from Elasticsearch publisher", e);

        }

//        log.info("update finishes");

    }

}