/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.custom.elastic.publisher.observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.das.messageflow.data.publisher.observer.MessageFlowObserver;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import org.wso2.custom.elastic.publisher.publish.ElasticStatisticsPublisher;
import org.wso2.custom.elastic.publisher.services.PublisherThread;
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import org.w3c.dom.Element;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticMediationFlowObserver implements MessageFlowObserver {

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    // Defines elasticsearch Transport Client as client
    private TransportClient client = null;

    // Thread to publish json strings to Elasticsearch
    private PublisherThread publisherThread = null;

    int queueSize = ElasticObserverConstants.DEFAULT_QUEUE_SIZE;
    boolean queueExceeded = false;

    /**
     * Instantiates the TransportClient as this class is instantiated
     */
    public ElasticMediationFlowObserver() {
        ServerConfiguration serverConf = ServerConfiguration.getInstance();

        // Takes configuration details form carbon.xml
        String clusterName = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_CLUSTER_NAME);
        String host = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_HOST);
        String portString = serverConf.getFirstProperty(ElasticObserverConstants.OBSERVER_PORT);
        String queueSizeString = serverConf.getFirstProperty(ElasticObserverConstants.QUEUE_SIZE);
        String username = serverConf.getFirstProperty(ElasticObserverConstants.USERNAME);
        String passwordInConfig = serverConf.getFirstProperty(ElasticObserverConstants.PASSWORD);
        String sslKey = serverConf.getFirstProperty(ElasticObserverConstants.SSL_KEY);
        String sslCert = serverConf.getFirstProperty(ElasticObserverConstants.SSL_CERT);
        String sslCa = serverConf.getFirstProperty(ElasticObserverConstants.SSL_CA);

        // Elasticsearch settings object
        Settings.Builder settingsBuilder = Settings.builder()
                .put("cluster.name", clusterName)
                .put("transport.tcp.compress", true);


        // If username is not null, Secure Vault password should be configured
        if (username != null && passwordInConfig.equals("password")) {
            // carbon.xml document element
            Element element = serverConf.getDocumentElement();

            // Creates Secret Resolver from carbon.xml document element
            SecretResolver secretResolver = SecretResolverFactory.create(element, true);

            // Resolves password using the defined alias
            String password = secretResolver.resolve(ElasticObserverConstants.PASSWORD_ALIAS);

            // If the alias is wrong or there is no password resolver returns the alias string again
            if (password.equals(ElasticObserverConstants.PASSWORD_ALIAS)) {
                log.error("No password in Secure Vault for the alias " + ElasticObserverConstants.PASSWORD_ALIAS);
                password = null;
            }

            // Can use password without ssl
            if (password != null) {
                settingsBuilder.put("xpack.security.user", username + ":" + password)
                        .put("request.headers.X-Found-Cluster", clusterName);

                if (sslKey != null && sslCert != null && sslCa != null) {
                    settingsBuilder.put("xpack.ssl.key", sslKey)
                            .put("xpack.ssl.certificate", sslCert)
                            .put("xpack.ssl.certificate_authorities", sslCa)
                            .put("xpack.security.transport.ssl.enabled", "true");
                }
            }
        }

        client = new PreBuiltXPackTransportClient(settingsBuilder.build());

        try {

            int port = Integer.parseInt(portString);
            queueSize = Integer.parseInt(queueSizeString);

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

            // wrong cluster name provided, given cluster is down or wrong access credentials
            if (client.connectedNodes().isEmpty()) {
                log.error("Can not connect to any Elasticsearch nodes. Please give correct configurations, " +
                        "run Elasticsearch and restart WSO2-EI.");
                client.close();
            } else {
                // checking the access privileges
                IndexResponse responseIndex = client.prepareIndex("eidata", "data", "1")
                        .setSource("{" +
                                "\"test_att\":\"test\"" +
                                "}", XContentType.JSON)
                        .get();

                DeleteResponse responseDel = client.prepareDelete("eidata", "data", "1").get();

                startPublishing();
                log.info("Elasticsearch mediation statistic publishing enabled");
            }
        } catch (UnknownHostException e) {
            log.error("Unknown Elasticsearch Host",e);
            client.close();
        } catch (NumberFormatException e) {
            log.error("Invalid port number or queue size value",e);
            client.close();
        } catch (ElasticsearchSecurityException e) { // lacks access privileges
            log.error("Elasticsearch access credentials are wrong or lacks user privilages.",e);
            client.close();
        } catch (Exception e) {
            log.error("Elasticsearch connection error",e);
            client.close();
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
     * Invokes the process method considering about the queue size.
     *
     * @param publishingFlow PublishingFlow object is passed when notified.
     */
    @Override
    public void updateStatistics(PublishingFlow publishingFlow) {
        if (publisherThread != null) {
            if(queueExceeded){
                if(ElasticStatisticsPublisher.getAllMappingsQueue().size() < queueSize){
                    log.info("Event queueing started.");
                    queueExceeded = false;
                }
            }else{
                if(ElasticStatisticsPublisher.getAllMappingsQueue().size() > queueSize){
                    log.warn("Event queue size exceeded. Dropping incoming events.");
                    queueExceeded = true;
                }
            }

            if(!queueExceeded){
                try {
                    if (!(publisherThread.getShutdown())) {
                        ElasticStatisticsPublisher.process(publishingFlow);
                    }
                } catch (Exception e) {
                    log.error("Failed to update statistics from Elasticsearch publisher", e);
                }
            }
        }
    }


    /**
     * Instantiates the publisher thread, passes the transport client and starts.
     */
    private void startPublishing() {
        publisherThread = new PublisherThread();
        publisherThread.setName("ElasticsearchPublisherThread");
        publisherThread.setClient(client);
        publisherThread.start();
    }
}