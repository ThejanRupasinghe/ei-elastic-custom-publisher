package org.wso2.custom.elastic.publisher.publish;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.Arrays;


public class ElasticStatisticsPublisherTest extends TestCase {

    public void testProcess() throws Exception {

        PublishingFlow flow = new PublishingFlow();
        Integer[] CHILDREN = {1, 2, 3};

        flow.setMessageFlowId("abcd1234");

        PublishingEvent event1 = new PublishingEvent();
        PublishingEvent event2 = new PublishingEvent();

        event1.setComponentName("TestProxy");
        event1.setComponentType("Proxy Service");
        event1.setFaultCount(12);
        event1.setStartTime(System.currentTimeMillis());
        event1.setBeforePayload("test1");
        event1.setAfterPayload("test1");
        event1.setComponentId("componentId");
        event1.setEndTime(System.currentTimeMillis());
        event1.setDuration(5);
        event1.setContextPropertyMap(new HashMap<String, String>() {{  put("contextPropertyMap", "value1");    put("key2", "value2");}});
        event1.setTransportPropertyMap(new HashMap<String, String>() {{  put("TransportPropertyMap", "value1");    put("key2", "value2");}});
        event1.setEntryPoint("sampleEntry");
        event1.setEntryPointHashcode(new Integer(1234));
        event1.setChildren(CHILDREN);
        event1.setHashCode(new Integer(123456));

        event2.setComponentName("TestSequence");
        event2.setComponentType("Sequence");
        event2.setFaultCount(10);
        event2.setStartTime(System.currentTimeMillis());
        event2.setBeforePayload("test2");
        event2.setAfterPayload("test2");
        event2.setComponentId("componentId");
        event2.setEndTime(System.currentTimeMillis());
        event2.setDuration(5);
        event2.setContextPropertyMap(new HashMap<String, String>() {{  put("contextPropertyMap", "value1");    put("key2", "value2");}});
        event2.setTransportPropertyMap(new HashMap<String, String>() {{  put("TransportPropertyMap", "value1");    put("key2", "value2");}});
        event2.setEntryPoint("sampleEntry");
        event2.setEntryPointHashcode(1234);
        event2.setChildren(CHILDREN);
        event2.setHashCode(123456);
        
        flow.addEvent(event1);
        flow.addEvent(event2);

        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> map2 = new HashMap<String, Object>();

        // for event1
        map1.put("flowid", "abcd1234");
        map1.put("host", PublisherUtil.getHostAddress());
        map1.put("type", "Proxy Service");
        map1.put("name", "TestProxy");
        map1.put("faultCount",12);
        map1.put("beforePayload","test1");
        map1.put("afterPayload","test1");
        map1.put("componentId", "componentId");
        map1.put("duration", 5l);
        map1.put("contextPropertyMap", new HashMap<String, String>() {{  put("contextPropertyMap", "value1");    put("key2", "value2");}});
        map1.put("transportPropertyMap", new HashMap<String, String>() {{  put("TransportPropertyMap", "value1");    put("key2", "value2");}});
        map1.put("entryPoint", "sampleEntry");
        map1.put("entryPointHashcode", new Integer(1234));
        map1.put("children", CHILDREN);
        map1.put("hashCode", new Integer(123456));

        long time1 = event1.getStartTime();
        Date date1 = new Date(time1);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate1 = dateFormat.format(date1);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        timeFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        String formattedTime1 = timeFormat.format(date1);

        String timestampElastic1 = formattedDate1 + "T" + formattedTime1 + "Z";
        map1.put("startTime", timestampElastic1);
        map1.put("endTime", timestampElastic1);


        // for event2
        map2.put("flowid", "abcd1234");
        map2.put("host", PublisherUtil.getHostAddress());
        map2.put("type", "Sequence");
        map2.put("name", "TestSequence");
        map2.put("faultCount",10);
        map2.put("beforePayload","test2");
        map2.put("afterPayload","test2");
        map2.put("componentId", "componentId");
        map2.put("duration", 5l);
        map2.put("contextPropertyMap", new HashMap<String, String>() {{  put("contextPropertyMap", "value1");    put("key2", "value2");}});
        map2.put("transportPropertyMap", new HashMap<String, String>() {{  put("TransportPropertyMap", "value1");    put("key2", "value2");}});
        map2.put("entryPoint", "sampleEntry");
        map2.put("entryPointHashcode", new Integer(1234));
        map2.put("children", CHILDREN);
        map2.put("hashCode", new Integer(123456));

        long time2 = event2.getStartTime();
        Date date2 = new Date(time2);

        String formattedDate2 = dateFormat.format(date2);
        String formattedTime2 = timeFormat.format(date2);

        String timestampElastic2 = formattedDate2 + "T" + formattedTime2 + "Z";
        map2.put("startTime", timestampElastic2);
        map2.put("endTime", timestampElastic2);


        Queue<Map<String, Object>> queue = new ConcurrentLinkedQueue<Map<String, Object>>();
        queue.add(map1);
        queue.add(map2);

        ElasticStatisticsPublisher.process(flow);

        
                
        Assert.assertTrue(map1.equals(ElasticStatisticsPublisher.getAllMappingsQueue().poll()) &&
                map2.equals(ElasticStatisticsPublisher.getAllMappingsQueue().poll())
        );
    }
}