package com.nickrobison.trestle.reasoner.caching;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.JmxAttributeGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.management.CacheStatisticsMXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by nrobison on 4/30/17.
 */
public class TrestleCacheMetrics implements MetricSet {
    private static final String M_BEAN_COORDINATES = "javax.cache:type=CacheStatistics,CacheManager=*,Cache=*";
    private static final Logger logger = LoggerFactory.getLogger(TrestleCacheMetrics.class);
    private final Set<String> cacheNames;

    TrestleCacheMetrics(Set<String> cacheNames) {
        this.cacheNames = cacheNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Set<ObjectInstance> cacheBeans = getCacheBeans();
        final List<String> statsNames = getStatsNames();

        final Map<String, Metric> gauges = new HashMap<>();

//        Filter caches to only those specified by the constructor
        cacheBeans
                .stream()
                .filter(cache -> this.cacheNames.contains(cache.getObjectName().getKeyProperty("Cache")))
                .forEach(cache -> {
                    final ObjectName objectName = cache.getObjectName();
                    final String cacheName = objectName.getKeyProperty("Cache");
                    statsNames
                            .forEach(stat -> {
                                final JmxAttributeGauge jmxAttributeGauge = new JmxAttributeGauge(objectName, stat);
                                gauges.put(String.format("%s.%s", cacheName, stat), jmxAttributeGauge);
                            });

                });

        return Collections.unmodifiableMap(gauges);
    }

    private Set<ObjectInstance> getCacheBeans() {
        try {
            return ManagementFactory.getPlatformMBeanServer().queryMBeans(ObjectName.getInstance(M_BEAN_COORDINATES), null);

        } catch (MalformedObjectNameException e) {
            logger.error("Unable to get Cache statistics MBean, are stats enabled?", e);
            throw new IllegalStateException(e);
        }
    }

    private List<String> getStatsNames() {
        final Method[] declaredMethods = CacheStatisticsMXBean.class.getDeclaredMethods();
        List<String> availableStatsNames = new ArrayList<>(declaredMethods.length);

        for (Method method : declaredMethods) {
            final String name = method.getName();
            if (name.startsWith("get")) {
                availableStatsNames.add(name.substring(3));
            }
        }
        return availableStatsNames;
    }
}
