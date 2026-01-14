package com.nodecraft.hytale.performance_diagnostics.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public final class JmxUtil {
    private JmxUtil() {}

    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return ManagementFactory.getOperatingSystemMXBean();
    }

    public static boolean isSunOperatingSystemMXBean(OperatingSystemMXBean bean) {
        return bean instanceof com.sun.management.OperatingSystemMXBean;
    }

    public static double getProcessCpuLoad(OperatingSystemMXBean bean) {
        if (isSunOperatingSystemMXBean(bean)) {
            return ((com.sun.management.OperatingSystemMXBean) bean).getProcessCpuLoad();
        }
        return -1.0;
    }

    public static double getSystemCpuLoad(OperatingSystemMXBean bean) {
        if (isSunOperatingSystemMXBean(bean)) {
            return ((com.sun.management.OperatingSystemMXBean) bean).getSystemCpuLoad();
        }
        return -1.0;
    }
}
