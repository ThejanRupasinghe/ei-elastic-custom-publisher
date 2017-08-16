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
import org.wso2.custom.elastic.publisher.services.PublisherThread;
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ElasticMediationFlowObserver implements MessageFlowObserver {

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    // Defines elasticsearch Transport Client as client
    private static TransportClient client = null;

    PublisherThread publisherThread;

    /**
     * Instantiates the TransportClient as this class is instantiates
     */
    public ElasticMediationFlowObserver() {

        ServerConfiguration serverConf = ServerConfiguration.getInstance();

        String clusterName = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_CLUSTER_NAME);
        String host = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_HOST);
        String portString = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_PORT);

        // Elasticsearch settings object
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .build();

        client = new PreBuiltTransportClient(settings);

        try {

            int port = Integer.parseInt(portString);

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

        } catch (UnknownHostException e) {

            log.error("Unknown Elasticsearch Host");

        } catch (NumberFormatException e) {

            log.error("Invalid port number");
        }

        publisherThread = new PublisherThread();
        publisherThread.start();

    }

    /**
     * TransportClient gets closed
     */
    @Override
    public void destroy() {

        if (client != null) {
            client.close();
        }

        publisherThread.shutdown();

        if (log.isDebugEnabled()) {
            log.debug("Shutting down the mediation statistics observer of Elasticsearch");
        }

    }

    /**
     * Method is called when this observer is notified.
     * Calls to process the the publishingFlow and pass the processed json to publish.
     *
     * @param publishingFlow PublishingFlow object is passed when notified.
     */
    @Override
    public void updateStatistics(PublishingFlow publishingFlow) {

        try {

            if (client.connectedNodes().isEmpty()) {
                if(ElasticStatisticsPublisher.all.size() < 10){
                    ArrayList<String> jsonsToPublish = ElasticStatisticsPublisher.process(publishingFlow);
                }
            }else {
                ArrayList<String> jsonsToPublish = ElasticStatisticsPublisher.process(publishingFlow);
            }

        } catch (Exception e) {

            log.error("Failed to update statics from Elasticsearch publisher", e);

        }

    }

    public static TransportClient getClient(){
        return client;
    }

}