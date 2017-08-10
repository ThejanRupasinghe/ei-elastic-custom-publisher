package org.wso2.custom.elastic.publisher.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;

public class ElasticStatisticsPublisherTest extends TestCase {

    public void testProcess() throws Exception {

        Settings settings = Settings.builder().put("cluster.name", "localhost").build();

        TransportClient client = new PreBuiltTransportClient(settings);

        PublishingFlow flow = new PublishingFlow();

        flow.setMessageFlowId("abcd1234");

        PublishingEvent event1 = new PublishingEvent();
        PublishingEvent event2 = new PublishingEvent();

        event1.setComponentName("TestProxy");
        event1.setComponentType("Proxy");
        event1.setFaultCount(0);
        event1.setStartTime(System.currentTimeMillis());

        event2.setComponentName("EventName");
        event2.setComponentType("EventType");
        event2.setFaultCount(0);

        flow.addEvent(event1);
        flow.addEvent(event2);

        Map<String, Object> mapping = new HashMap<String, Object>();

        mapping.put("flowid", "abcd1234");
        mapping.put("host", PublisherUtil.getHostAddress());
        mapping.put("type", "Proxy");
        mapping.put("name", "TestProxy");

        long time = event1.getStartTime();
        Date date = new Date(time);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        timeFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        String formattedTime = timeFormat.format(date);

        String timestampElastic = formattedDate + "T" + formattedTime + "Z";
        mapping.put("@timestamp", timestampElastic);

        mapping.put("success", true);

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString = objectMapper.writeValueAsString(mapping);

        Assert.assertEquals(jsonString, ElasticStatisticsPublisher.process(flow));

    }

    public void testPublish() throws Exception {

        Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();

        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));


        String jsonToPublish = "{\"@timestamp\":\"2017-08-10T08:26:28.075Z\",\"success\":true,\"host\":\"172.17.0.1\",\"name\":\"TestProxy\",\"type\":\"Proxy\",\"flowid\":\"abcd1234\"}";

        String indexId = ElasticStatisticsPublisher.publish(jsonToPublish, client);

        Assert.assertNotNull(indexId);

    }

}