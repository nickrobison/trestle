package com.nickrobison.metrician;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * Created by nrobison on 3/21/17.
 */

/**
 * Default class which registers various JVM/JMX metrics with the {@link MetricRegistry}
 */
@Metriced
public class JVMMetrics {

    private static final Logger logger = LoggerFactory.getLogger(JVMMetrics.class);
    private static final String JAVA_LANG_TYPE_OPERATING_SYSTEM = "java.lang:type=OperatingSystem";
    private final MetricRegistry registry;
    private final OperatingSystemMXBean operatingSystemMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final int processors;
    private final MBeanServer platformMBeanServer;

    @Inject
    JVMMetrics(MetricRegistry registry) {
        this.registry = registry;
//        Get the Beans
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
//        Get the total number of processors
        processors = operatingSystemMXBean.getAvailableProcessors();

//        JVM metrics
        this.registerJVMMetrics();
    }

    private void registerJVMMetrics() {
        logger.debug("Registering JVM Memory Gauges");
        this.registry.registerAll(new MemoryUsageGaugeSet());
        logger.debug("Registering JVM Thread gauges");
        this.registry.registerAll(new ThreadStatesGaugeSet());
        logger.debug("Registering JVM Garbage Collector Gauges");
        this.registry.registerAll(new GarbageCollectorMetricSet());
    }

    //    Public Methods

    /**
     * Returns the current uptime of the JVM instance
     * @return - long of uptime
     */
    public long currentUptime() {
        return this.runtimeMXBean.getUptime();
    }

    /**
     * Returns the number of available processors
     * @return - int number of processors
     */
    public int numProcessors() {
        return this.processors;
    }


//    Protected metrics

    /**
     * Get current CPU usage
     * @return - double of current CPU usage
     */
    @Gauge(name = "jvm-cpu-usage", absolute = true)
    double getCPUUsage() {
        final AttributeList list;
        try {
            final ObjectName objectName = ObjectName.getInstance(JAVA_LANG_TYPE_OPERATING_SYSTEM);
            list = platformMBeanServer.getAttributes(objectName, new String[]{"ProcessCpuLoad"});
        } catch (Exception e) {
            logger.error("Unable to get ProcessCpuLoad Value", e);
            return Double.NaN;
        }
        if (list.isEmpty()) {
            return Double.NaN;
        }
        final Attribute attribute = (Attribute) list.get(0);
        final Double value = (Double) attribute.getValue();
        if (value == -1.0) {
            return Double.NaN;
        }
        return ((int) (value * 1000) / 10.0);
    }
}
