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

public class ElasticObserverConstants {

    private ElasticObserverConstants() {
    }

    // Constant to get data from carbon.xml
    private static final String OBSERVER_ELEMENT = DASDataPublisherConstants.STAT_CONFIG_ELEMENT + ".ElasticObserver";
    public static final String OBSERVER_HOST = OBSERVER_ELEMENT + ".Host";
    public static final String OBSERVER_CLUSTER_NAME = OBSERVER_ELEMENT + ".ClusterName";
    public static final String OBSERVER_PORT = OBSERVER_ELEMENT + ".Port";
    public static final String QUEUE_SIZE = OBSERVER_ELEMENT + ".QueueSize";

}
