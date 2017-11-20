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
import org.wso2.custom.elastic.publisher.services.ElasticsearchPublisherThread;
import org.wso2.custom.elastic.publisher.util.ElasticObserverConstants;

import org.w3c.dom.Element;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class is instantiated by MediationStatisticsComponent.
 * Gets stored in MessageFlowObserverStore and updateStatistics() is notified by the MessageFlowReporterThread.
 */
public class ElasticMediationFlowObserver implements MessageFlowObserver {

    private static final Log log = LogFactory.getLog(ElasticMediationFlowObserver.class);

    // Defines elasticsearch Transport Client as client
    private TransportClient client = null;

    // Thread to publish json strings to Elasticsearch
    private ElasticsearchPublisherThread publisherThread = null;

    // Event buffering queue size = 5000
    int bufferSize = ElasticObserverConstants.DEFAULT_BUFFER_SIZE;

    // Size of the event publishing bulk = 500
    int bulkSize = ElasticObserverConstants.DEFAULT_PUBLISHING_BULK_SIZE;

    // Time out for collecting configured fixed size bulk = 5000
    long bulkTimeOut = ElasticObserverConstants.DEFAULT_BULK_COLLECTING_TIMEOUT;

    // PublisherThread sleep time when the buffer is empty = 1000 (in millis)
    long bufferEmptySleep = ElasticObserverConstants.DEFAULT_BUFFER_EMPTY_SLEEP_TIME;

    // PublisherThread sleep time when the Elasticsearch server is down = 5000 (in millis)
    long noNodesSleep = ElasticObserverConstants.DEFAULT_NO_NODES_SLEEP_TIME;

    // Whether the event queue exceeded or not, accessed by MessageFlowReporter threads
    private volatile boolean bufferExceeded = false;

    /**
     * Instantiates the TransportClient as this class is instantiated
     */
    public ElasticMediationFlowObserver() {
        ServerConfiguration serverConf = ServerConfiguration.getInstance();

        // Takes configuration details form carbon.xml
        String clusterName = serverConf.getFirstProperty(ElasticObserverConstants.CLUSTER_NAME_CONFIG);
        String host = serverConf.getFirstProperty(ElasticObserverConstants.HOST_CONFIG);
        String portString = serverConf.getFirstProperty(ElasticObserverConstants.PORT_CONFIG);
        String queueSizeString = serverConf.getFirstProperty(ElasticObserverConstants.BUFFER_SIZE_CONFIG);
        String bulkSizeString = serverConf.getFirstProperty(ElasticObserverConstants.BULK_SIZE_CONFIG);
        String bulkCollectingTimeOutString = serverConf.getFirstProperty(ElasticObserverConstants.BULK_COLLECTING_TIME_OUT_CONFIG);
        String bufferEmptySleepString = serverConf.getFirstProperty(ElasticObserverConstants.BUFFER_EMPTY_SLEEP_TIME_CONFIG);
        String noNodesSleepString = serverConf.getFirstProperty(ElasticObserverConstants.NO_NODES_SLEEP_TIME_CONFIG);
        String username = serverConf.getFirstProperty(ElasticObserverConstants.USERNAME_CONFIG);
        String passwordInConfig = serverConf.getFirstProperty(ElasticObserverConstants.PASSWORD_CONFIG);
        String sslKey = serverConf.getFirstProperty(ElasticObserverConstants.SSL_KEY_CONFIG);
        String sslCert = serverConf.getFirstProperty(ElasticObserverConstants.SSL_CERT_CONFIG);
        String sslCa = serverConf.getFirstProperty(ElasticObserverConstants.SSL_CA_CONFIG);

        if (log.isDebugEnabled()) {
            log.debug("Configurations taken from carbon.xml.");
        }

        try {
            int port = Integer.parseInt(portString);
            bufferSize = Integer.parseInt(queueSizeString);
            bulkSize = Integer.parseInt(bulkSizeString);
            bulkTimeOut = Integer.parseInt(bulkCollectingTimeOutString);
            bufferEmptySleep = Integer.parseInt(bufferEmptySleepString);
            noNodesSleep = Integer.parseInt(noNodesSleepString);

            if (log.isDebugEnabled()) {
                log.debug("Cluster Name: " + clusterName);
                log.debug("Host: " + host);
                log.debug("Port: " + port);
                log.debug("Buffer Size: " + bufferSize);
                log.debug("Username: " + username);
                log.debug("SSL Key Path: " + sslKey);
                log.debug("SSL Certificate Path: " + sslCert);
                log.debug("SSL CA Cert Path: " + sslCa);
            }

            // Elasticsearch settings builder object
            Settings.Builder settingsBuilder = Settings.builder()
                    .put("cluster.name", clusterName)
                    .put("transport.tcp.compress", true);

            // If username is not null; password can be in plain or Secure Vault
            if (username != null && passwordInConfig != null) {
                String password;

                // TODO: 11/17/17 find from element the secure vault is configured or not
                if ("password".equals(passwordInConfig)) {
                    // carbon.xml document element
                    Element element = serverConf.getDocumentElement();

                    // Creates Secret Resolver from carbon.xml document element
                    SecretResolver secretResolver = SecretResolverFactory.create(element, true);

                    // Resolves password using the defined alias
                    password = secretResolver.resolve(ElasticObserverConstants.PASSWORD_ALIAS);

                    // If the alias is wrong and there is no password, resolver returns the alias string again
                    if (ElasticObserverConstants.PASSWORD_ALIAS.equals(password)) {
                        log.error("No password in Secure Vault for the alias " + ElasticObserverConstants.PASSWORD_ALIAS);
                        password = null;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Password resolved from Secure Vault.");
                        }
                    }
                } else {
                    password = passwordInConfig;
                    if (log.isDebugEnabled()) {
                        log.debug("Password taken from carbon.xml");
                    }
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

                        if (log.isDebugEnabled()) {
                            log.debug("SSL keys and certificates added.");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("SSL is not configured.");
                        }
                    }
                }
            }

            client = new PreBuiltXPackTransportClient(settingsBuilder.build());

            if (log.isDebugEnabled()) {
                log.debug("Transport Client is built.");
            }

            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

            if (log.isDebugEnabled()) {
                log.debug("Host & Port added to the client.");
            }

            // Wrong cluster name provided or given cluster is down or wrong access credentials
            if (client.connectedNodes().isEmpty()) {
                log.error("Can not connect to any Elasticsearch nodes. Please give correct configurations, " +
                        "run Elasticsearch and restart WSO2-EI.");
                client.close();

                if (log.isDebugEnabled()) {
                    log.debug("No nodes connected. Reasons: Wrong cluster name/ Given cluster is down/ " +
                            "Wrong access credentials");
                }
            } else {
                /*
                    Client needs access rights to read and write to Elasticsearch cluster as described in the article.
                    If the given user credential has no access to write, it only can be identified when the first bulk
                    of events are published.
                    So, to check the access privileges before hand, here put a test json string and delete it.
                */
                client.prepareIndex("eidata", "data", "1")
                        .setSource("{" +
                                "\"test_att\":\"test\"" +
                                "}", XContentType.JSON)
                        .get();

                client.prepareDelete("eidata", "data", "1").get();

                if (log.isDebugEnabled()) {
                    log.debug("Access privileges for given user is sufficient.");
                }

                startPublishing();
                log.info("Elasticsearch mediation statistic publishing enabled.");
            }
        } catch (UnknownHostException e) {
            log.error("Unknown Elasticsearch Host.", e);
            client.close();
        } catch (NumberFormatException e) {
            log.error("Invalid port number, queue size or time value.", e);
            client.close();
        } catch (ElasticsearchSecurityException e) { // lacks access privileges
            log.error("Elasticsearch user credentials lacks access privileges.", e);
            client.close();
        } catch (Exception e) {
            log.error("Elasticsearch connection error.", e);
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
            if (bufferExceeded) {
                // If the queue has exceeded before, check the queue is not exceeded now
                if (ElasticStatisticsPublisher.getAllMappingsQueue().size() < bufferSize) {
                    // Log only once
                    log.info("Event buffering started.");
                    bufferExceeded = false;
                }
            } else {
                // If the queue has not exceeded before, check the queue is exceeded now
                if (ElasticStatisticsPublisher.getAllMappingsQueue().size() >= bufferSize) {
                    // Log only once
                    log.warn("Maximum buffer size reached. Dropping incoming events.");
                    bufferExceeded = true;
                }
            }

            if (!bufferExceeded) {
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
        publisherThread = new ElasticsearchPublisherThread();
        publisherThread.setName("ElasticsearchPublisherThread");
        publisherThread.init(client, bulkSize, bulkTimeOut, bufferEmptySleep, noNodesSleep);
        publisherThread.start();
    }
}
