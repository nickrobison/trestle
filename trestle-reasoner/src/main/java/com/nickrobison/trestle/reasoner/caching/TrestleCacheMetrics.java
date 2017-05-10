package com.nickrobison.trestle.reasoner.caching;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
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

    @Override
    public Map<String, Metric> getMetrics() {
        final Set<ObjectInstance> cacheBeans = getCacheBeans();
        final List<String> statsNames = getStatsNames();

        final Map<String, Metric> gauges = new HashMap<>(cacheBeans.size() * statsNames.size());

        cacheBeans.forEach(bean -> {
            final ObjectName objectName = bean.getObjectName();
            final String cacheName = objectName.getKeyProperty("Cache");

            statsNames.forEach(statsName -> {
                final JmxAttributeGauge jmxAttributeGauge = new JmxAttributeGauge(objectName, statsName);
                gauges.put(String.format("%s.%s", cacheName, statsName), jmxAttributeGauge);
            });
        });

        return Collections.unmodifiableMap(gauges);
    }

    private Set<ObjectInstance> getCacheBeans() {
        try {
            return ManagementFactory.getPlatformMBeanServer().queryMBeans(ObjectName.getInstance(M_BEAN_COORDINATES), null);

        } catch (MalformedObjectNameException e) {
            logger.error("Unable to get Cache statistics MBean, are stats enabled?", e);
            throw new RuntimeException(e);
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
