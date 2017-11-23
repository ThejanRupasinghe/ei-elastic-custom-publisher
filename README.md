# WSO2 EI Custom Mediation Flow Observer for Elasticsearch
This is an implementation of a custom mediation flow observer for [WSO2 Enterprise Integrator](https://wso2.com/integration#download) which publishes flow statistics to [Elasticsearch](https://www.elastic.co/products/elasticsearch).
This uses the [interface](https://docs.wso2.com/display/EI611/Customizing+Statistics+Publishing) exposed by the existing EI Analytics profile.

<b>This applies to EI 6.1.1 and above. For EI 6.1.1 please take the latest WUM update before adding this.</b> 

## Building and Adding to EI
* Get a clone or download source from [github](https://github.com/ThejanRupasinghe/ei-elastic-custom-publisher).
* Run the Maven command ```mvn clean install``` in the cloned directory.
* Copy the “org.wso2.custom.elastic.publisher_1.0.jar” in the “target” directory to the “dropins” directory of EI installation.

## Configuring EI to run the custom observer
* Add following lines to “carbon.xml” in the conf folder of EI, changed with your Elasticsearch cluster details.
```
<MediationFlowStatisticConfig>
    <AnalyticPublishingDisable>true</AnalyticPublishingDisable>
    <Observers>
        org.wso2.custom.elastic.publisher.observer.ElasticMediationFlowObserver
    </Observers>
    <ElasticObserver>
        <Host>localhost</Host>
        <Port>9300</Port>
        <ClusterName>elasticsearch</ClusterName>
        <QueueSize>5000</QueueSize>
        <Username>transport_client_user</Username>
        <Password>changeme</Password>
        <SslKey>/path/to/client.key</SslKey>
        <SslCertificate>/path/to/client.crt</SslCertificate>
        <SslCa>/path/to/ca.crt</SslCa>
    </ElasticObserver>
</MediationFlowStatisticConfig>
```
```<AnalyticPublishingDisable>true</AnalyticPublishingDisable>``` : This disables the default analytic publisher in EI. If you don't want to disable it delete this line.  
```<Host>localhost</Host>```    : IP/Host which your Elasticsearch binds to  
```<Port>9300</Port>```         : Port Number which your Elasticsearch listens to  
```<ClusterName>elasticsearch</ClusterName>``` : Elasticsearch cluster name. This can be configured from elasticsearch.yml - default name is “elasticsearch”  
```<BufferSize>5000</BufferSize>``` : Size of the buffering queue which keeps the statistic events in the custom observer.  Program will drop the incoming events after the maximum buffer size is reached.  
```<BulkSize>500</BulkSize>``` : Size of the events bulk, that the client will publish at a time.  
```<BulkCollectingTimeOut>5000</BulkCollectingTimeOut>``` : This is the timeout value for collecting the events for the bulk from the buffer (in milliseconds).
```<BufferEmptySleepTime>1000</BufferEmptySleepTime>``` : This is the sleep time for the publisher thread when the buffer is empty (in milliseconds).  
```<NoNodesSleepTime>5000</NoNodesSleepTime>``` : This is the sleep time for the publisher thread when there is no Elasticsearch nodes connected to the client.
#### Provide following configurations additionally for X-Pack secured Elasticsearch cluster
```<Username>```  :  Username of the created user with access privileges   
```<Password>```  :  Password given to the user  
```<SslKey>```   :  Absolute path to the SSL key file of the client  
```<SslCertificate>``` :  Absolute path to the SSL certificate file of the client  
```<SslCa>```  :  Configures Transport Client in observer to trust any certificates signed by the specified CA, which is used to sign Elasticsearch node certificates.  

* Enable statistics for services as in the [documentation](https://docs.wso2.com/display/EI611/Prerequisites+to+Publish+Statistics).
* Restart WSO2 EI and watch for the log line “Elasticsearch mediation statistic publishing enabled” and verify that the mediation flow observer is running.