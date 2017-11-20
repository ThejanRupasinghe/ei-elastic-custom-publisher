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

package org.wso2.custom.elastic.publisher.util;

import org.wso2.carbon.das.data.publisher.util.DASDataPublisherConstants;

/**
 * Stores needed constant values
 */
public class ElasticObserverConstants {

    private ElasticObserverConstants() {
    }

    // Constant to get data from carbon.xml
    private static final String OBSERVER_ELEMENT = DASDataPublisherConstants.STAT_CONFIG_ELEMENT + ".ElasticObserver";
    public static final String OBSERVER_HOST_CONFIG = OBSERVER_ELEMENT + ".Host";
    public static final String OBSERVER_CLUSTER_NAME_CONFIG = OBSERVER_ELEMENT + ".ClusterName";
    public static final String OBSERVER_PORT_CONFIG = OBSERVER_ELEMENT + ".Port";
    public static final String QUEUE_SIZE_CONFIG = OBSERVER_ELEMENT + ".QueueSize";
    public static final String USERNAME_CONFIG = OBSERVER_ELEMENT + ".Username";
    public static final String PASSWORD_CONFIG = OBSERVER_ELEMENT + ".Password";
    public static final String PASSWORD_ALIAS = "Elastic.User.Passwor";
    public static final String SSL_KEY_CONFIG = OBSERVER_ELEMENT + ".SslKey";
    public static final String SSL_CERT_CONFIG = OBSERVER_ELEMENT + ".SslCertificate";
    public static final String SSL_CA_CONFIG = OBSERVER_ELEMENT + ".SslCa";
    public static final int DEFAULT_QUEUE_SIZE = 5000;
    
    public static final int PUBLISHING_BULK_SIZE = 500; // TODO: 11/19/17 to be decided


    /*

     <MediationFlowStatisticConfig>
        <AnalyticPublishingDisable>true</AnalyticPublishingDisable>
        <Observers>
            org.wso2.custom.elastic.publisher.observer.ElasticMediationFlowObserver
        </Observers>
        <ElasticObserver>
            <Host>localhost</Host>
            <Port>9300</Port>
            <ClusterName>elasticsearch</ClusterName>
            <BufferSize>5000</BufferSize>
            <Username>transport_client_user</Username>
            <Password>changeme</Password>
            <SslKey>/path/to/client.key</SslKey>
            <SslCertificate>/path/to/client.crt</SslCertificate>
            <SslCa>/path/to/ca.crt</SslCa>
        </ElasticObserver>
     </MediationFlowStatisticConfig>

     */
}