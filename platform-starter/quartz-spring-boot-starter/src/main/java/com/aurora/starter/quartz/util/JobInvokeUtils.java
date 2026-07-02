package com.aurora.starter.quartz.util;

import com.aurora.starter.common.utils.StringUtils;
import com.aurora.starter.quartz.core.job.JobContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务调用工具 —— 通过 invokeTarget 字符串反射调用目标方法.
 * <p>
 * 语法: {@code beanName.method('arg1',1L,2D,3,true)}.
 * 支持参数类型: String / Long / Double / Integer / Boolean.
 */
public final class JobInvokeUtils {

    /** 由 {@code QuartzAutoConfiguration} 在启动时注入. */
    public static org.springframework.context.ApplicationContext applicationContext;

    private JobInvokeUtils() {
    }

    /**
     * 执行方法.
     */
    public static void invokeMethod(JobContext job) throws Exception {
        String invokeTarget = job.getInvokeTarget();
        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        List<Object[]> methodParams = getMethodParams(invokeTarget);

        Object bean;
        if (isValidClassName(beanName)) {
            bean = Class.forName(beanName).getDeclaredConstructor().newInstance();
        } else {
            bean = applicationContext.getBean(beanName);
        }
        invokeMethod(bean, methodName, methodParams);
    }

    private static void invokeMethod(Object bean, String methodName, List<Object[]> methodParams)
            throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (methodParams != null && !methodParams.isEmpty()) {
            Method method = bean.getClass().getDeclaredMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getDeclaredMethod(methodName);
            method.invoke(bean);
        }
    }

    /**
     * 是否为完整类名(包含包路径).
     */
    public static boolean isValidClassName(String invokeTarget) {
        return StringUtils.countMatches(invokeTarget, ".") > 1;
    }

    /**
     * 提取 bean 名称.
     */
    public static String getBeanName(String invokeTarget) {
        String beanName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringBeforeLast(beanName, ".");
    }

    /**
     * 提取方法名.
     */
    public static String getMethodName(String invokeTarget) {
        String methodName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringAfterLast(methodName, ".");
    }

    /**
     * 解析方法参数.
     */
    public static List<Object[]> getMethodParams(String invokeTarget) {
        String methodStr = StringUtils.substringBetween(invokeTarget, "(", ")");
        if (StringUtils.isEmpty(methodStr)) {
            return new ArrayList<>();
        }
        String[] methodParams = methodStr.split(",(?=(?:[^']*'[^']*')*[^']*$)");
        List<Object[]> classes = new ArrayList<>();
        for (String raw : methodParams) {
            String str = StringUtils.trimToEmpty(raw);
            if (StringUtils.contains(str, "'")) {
                classes.add(new Object[]{StringUtils.replace(str, "'", ""), String.class});
            } else if (StringUtils.equals(str, "true") || StringUtils.equalsIgnoreCase(str, "false")) {
                classes.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            } else if (StringUtils.containsIgnoreCase(str, "L")) {
                classes.add(new Object[]{
                        Long.valueOf(StringUtils.replaceIgnoreCase(str, "L", "")),
                        Long.class
                });
            } else if (StringUtils.containsIgnoreCase(str, "D")) {
                classes.add(new Object[]{
                        Double.valueOf(StringUtils.replaceIgnoreCase(str, "D", "")),
                        Double.class
                });
            } else {
                classes.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
        }
        return classes;
    }

    private static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] classes = new Class<?>[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            classes[i] = (Class<?>) methodParams.get(i)[1];
        }
        return classes;
    }

    private static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] classes = new Object[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            classes[i] = methodParams.get(i)[0];
        }
        return classes;
    }
}
