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

public class ElasticMediationFlowObserver implements MessageFlowObserver {

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    // Defines elasticsearch Transport Client as client
    private static TransportClient client = null;

    // Thread to publish jsons to Elasticsearch
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
        Settings settings = Settings.builder().put("cluster.name", clusterName).build();

        client = new PreBuiltTransportClient(settings);

        Exception exp = null;

        try {

            int port = Integer.parseInt(portString);

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

        } catch (UnknownHostException e) {

            exp = e;
            log.error("Unknown Elasticsearch Host");

        } catch (NumberFormatException e) {

            exp = e;
            log.error("Invalid port number");

        }finally {

            // Only if there is no exception, publisher thread is started
            if(exp==null){
                publisherThread = new PublisherThread();
                publisherThread.start();
            }

        }

    }

    /**
     * TransportClient gets closed
     */
    @Override
    public void destroy() {

        publisherThread.shutdown();

        if (client != null) {
            client.close();
        }

        if (log.isDebugEnabled()) {
            log.debug("Shutting down the mediation statistics observer of Elasticsearch");
        }

    }

    /**
     * Method is called when this observer is notified.
     * Invokes the process method for the publishing flow considering whether there are any nodes connected.
     *
     * @param publishingFlow PublishingFlow object is passed when notified.
     */
    @Override
    public void updateStatistics(PublishingFlow publishingFlow) {

        try {

            // If no connected nodes queue size will be limited
            if (client.connectedNodes().isEmpty()) {

                // TODO: 8/16/17 Take the queue size from carbon.xml 
                if(ElasticStatisticsPublisher.allMappingsQueue.size() < 10){
                    ElasticStatisticsPublisher.process(publishingFlow);
                    ElasticStatisticsPublisher.allMappingsQueue.toString();
                }
                
            }else {
                
                ElasticStatisticsPublisher.process(publishingFlow);
                
            }

        } catch (Exception e) {

            log.error("Failed to update statics from Elasticsearch publisher", e);

        }

    }

    /**
     *
     * @return Transport Client for Elasticsearch
     */
    public static TransportClient getClient(){
        return client;
    }

}