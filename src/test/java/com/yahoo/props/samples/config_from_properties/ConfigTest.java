package com.yahoo.props.samples.config_from_properties;

import static com.yahoo.props.samples.config_from_properties.Config.AVAILABILITY;
import static com.yahoo.props.samples.config_from_properties.Config.CNAME;
import static com.yahoo.props.samples.config_from_properties.Config.MAIN;
import static com.yahoo.props.samples.config_from_properties.Config.PORT;
import static com.yahoo.props.samples.config_from_properties.Config.REPLICAS;
import static com.yahoo.props.samples.config_from_properties.Config.ROLES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.Sets;

public class ConfigTest {
    @Test
    public void test() throws IOException {
        Properties configs = new Properties();
        configs.load(new StringReader("cname=v1.yahooapis.com\n"//
                + "main=GQ1\n"//
                + "replicas=BF1,NE1"//
        ));
        
        assertEquals(CNAME.getFrom(configs), "v1.yahooapis.com");
        assertEquals(MAIN.getFrom(configs), Colo.GQ1);
        assertEquals(REPLICAS.getFrom(configs).size(), 2);
        assertTrue(REPLICAS.getFrom(configs).contains(Colo.BF1));
        assertTrue(REPLICAS.getFrom(configs).contains(Colo.NE1));
        assertEquals(AVAILABILITY.getFrom(configs), 0.0d);// default
        assertNull(PORT.getFrom(configs));
        assertNull(ROLES.getFrom(configs));
        
        CNAME.setTo(configs, "v2.yahooapis.com");
        assertEquals(configs.getProperty("cname"), "v2.yahooapis.com");
        assertEquals(CNAME.getFrom(configs), "v2.yahooapis.com");
        
        MAIN.setTo(configs, Colo.BF1);
        assertEquals(configs.getProperty("main"), "BF1");
        assertEquals(MAIN.getFrom(configs), Colo.BF1);
        
        REPLICAS.setTo(configs, Sets.newHashSet(Colo.GQ1, Colo.NE1, Colo.CH1, Colo.IR2));
        assertEquals(configs.getProperty("replicas"), "CH1,GQ1,IR2,NE1");
        Set<Colo> replicas = REPLICAS.getFrom(configs);
        assertEquals(replicas.size(), 4);
        
        PORT.setTo(configs, 4080);
        assertEquals(configs.getProperty("port"), "4080");
        assertTrue(PORT.getFrom(configs) == 4080);
        
        AVAILABILITY.setTo(configs, 0.12345678d);
        assertEquals(configs.getProperty("availability"), "0.12345678");
        
        AVAILABILITY.setTo(configs, 123e-2);
        assertEquals(configs.getProperty("availability"), "1.23");
    }
}
