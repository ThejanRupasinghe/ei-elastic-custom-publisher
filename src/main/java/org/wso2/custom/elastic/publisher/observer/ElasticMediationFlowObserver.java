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
    Exception exp = null;


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

        // carbon.xml document element
        Element element = serverConf.getDocumentElement();

        // Creates Secret Resolver from carbon.xml document element
        SecretResolver secretResolver = SecretResolverFactory.create(element, true);

        // Resolves password using the defined alias
        String password = secretResolver.resolve(ElasticObserverConstants.PASSWORD_ALIAS);

        // If the alias is wrong or there is no password resolver returns the alias string again
        if (password.equals(ElasticObserverConstants.PASSWORD_ALIAS)) {
            log.error("No password in Secure Vault for the alias Elastic.User.Password");
            password = null;
        }

        // Elasticsearch settings object
        Settings.Builder settingsBuilder = Settings.builder()
                .put("cluster.name", clusterName);

        if (username != null && password != null) {

            // TODO: 9/7/17 take certificate paths from config file
            settingsBuilder.put("xpack.security.user", username + ":" + password)
                    .put("xpack.ssl.key", "/home/thejan/WSO2/MyProject/CompleteTest/WithXPack/Elasticsearch/elasticsearch-5.4.3-node0/config/x-pack/certificates/node0/node0.key")
                    .put("xpack.ssl.certificate", "/home/thejan/WSO2/MyProject/CompleteTest/WithXPack/Elasticsearch/elasticsearch-5.4.3-node0/config/x-pack/certificates/node0/node0.crt")
                    .put("xpack.ssl.certificate_authorities", "/home/thejan/WSO2/MyProject/CompleteTest/WithXPack/Elasticsearch/elasticsearch-5.4.3-node0/config/x-pack/certificates/ca/ca.crt")
                    .put("xpack.security.transport.ssl.enabled", "true");

        }

        client = new PreBuiltXPackTransportClient(settingsBuilder.build());

        try {

            int port = Integer.parseInt(portString);
            queueSize = Integer.parseInt(queueSizeString);

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

            // wrong cluster name provided or given cluster is down
            if (client.connectedNodes().isEmpty()) {

                log.error("Can not connect to any Elasticsearch nodes. Please give correct configurations " +
                        "and run Elasticsearch.");

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

            exp = e;
            log.error("Unknown Elasticsearch Host");
            client.close();

        } catch (NumberFormatException e) {

            exp = e;
            log.error("Invalid port number or queue size value");
            client.close();

        } catch (ElasticsearchSecurityException e) { // lacks access privileges

            exp = e;
            log.error("Wrong Elasticsearch access credentials.");
            client.close();

        } catch (Exception e) {

            exp = e;
            log.error("Elasticsearch connection error");
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


        if ((exp == null)) {

            try {

                // if connectedNodes is not empty and publisher thread is not instantiated
                if (publisherThread == null && !(client.connectedNodes().isEmpty())) {

                    startPublishing();

                }

                // Statistics should only be processed if the queue size is not exceeded

                if (ElasticStatisticsPublisher.getAllMappingsQueue().size() < queueSize) {

                    if (publisherThread == null) {

                        ElasticStatisticsPublisher.process(publishingFlow);

                    } else {

                        if (!(publisherThread.getShutdown()) &&
                                ElasticStatisticsPublisher.getAllMappingsQueue().size() < queueSize) {

                            ElasticStatisticsPublisher.process(publishingFlow);

                        }

                    }

                }

            } catch (Exception e) {

                log.error("Failed to update statistics from Elasticsearch publisher", e);

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