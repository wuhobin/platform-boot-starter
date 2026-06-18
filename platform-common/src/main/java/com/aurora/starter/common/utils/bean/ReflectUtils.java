package com.aurora.starter.common.utils.bean;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aurora.starter.common.utils.JsonUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 反射工具类.
 *
 * @author Luo
 * @version 1.0
 * @date 2021-9-23 10:59:37
 */
@Slf4j
@UtilityClass
public class ReflectUtils {
    private static final Map<Class<?>, List<Field>> ALL_FIELD_CACHE = new ConcurrentReferenceHashMap<>(256);

    private static final Map<String, List<Field>> PART_FIELD_CACHE = new ConcurrentReferenceHashMap<>(256);

    /**
     * 获取 Bean 的所有 get方法.
     *
     * @param type 类
     * @return PropertyDescriptor数组
     */
    public static PropertyDescriptor[] getBeanGetters(final Class type) {
        return getPropertiesHelper(type, true, false);
    }

    /**
     * 获取 Bean 的所有 set方法.
     *
     * @param type 类
     * @return PropertyDescriptor数组
     */
    public static PropertyDescriptor[] getBeanSetters(final Class type) {
        return getPropertiesHelper(type, false, true);
    }

    /**
     * 获取 Bean 的所有 PropertyDescriptor.
     *
     * @param type  类
     * @param read  读取方法
     * @param write 写方法
     * @return PropertyDescriptor数组
     */
    public static PropertyDescriptor[] getPropertiesHelper(final Class type, final boolean read, final boolean write) {
        try {
            PropertyDescriptor[] all = BeanUtils.getPropertyDescriptors(type);
            if (read && write) {
                return all;
            } else {
                List<PropertyDescriptor> properties = new ArrayList<>(all.length);
                for (PropertyDescriptor pd : all) {
                    if (read && pd.getReadMethod() != null) {
                        properties.add(pd);
                    } else if (write && pd.getWriteMethod() != null) {
                        properties.add(pd);
                    }
                }
                return properties.toArray(new PropertyDescriptor[0]);
            }
        } catch (BeansException ex) {
            throw new CodeGenerationException(ex);
        }
    }

    /**
     * 获取 bean 的属性信息.
     *
     * @param propertyType 类型
     * @param propertyName 属性名
     * @return {Property}
     */
    @Nullable
    public static Property getProperty(final Class<?> propertyType, final String propertyName) {
        PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(propertyType, propertyName);
        if (propertyDescriptor == null) {
            return null;
        }
        return ReflectUtils.getProperty(propertyType, propertyDescriptor, propertyName);
    }

    /**
     * 获取 bean 的属性信息.
     *
     * @param propertyType       类型
     * @param propertyDescriptor PropertyDescriptor
     * @param propertyName       属性名
     * @return {Property}
     */
    public static Property getProperty(final Class<?> propertyType, final PropertyDescriptor propertyDescriptor, final String propertyName) {
        Method readMethod = propertyDescriptor.getReadMethod();
        Method writeMethod = propertyDescriptor.getWriteMethod();
        return new Property(propertyType, readMethod, writeMethod, propertyName);
    }

    /**
     * 获取 bean 的属性信息.
     *
     * @param propertyType 类型
     * @param propertyName 属性名
     * @return {Property}
     */
    @Nullable
    public static TypeDescriptor getTypeDescriptor(final Class<?> propertyType, final String propertyName) {
        Property property = ReflectUtils.getProperty(propertyType, propertyName);
        if (property == null) {
            return null;
        }
        return new TypeDescriptor(property);
    }

    /**
     * 获取 类属性信息.
     *
     * @param propertyType       类型
     * @param propertyDescriptor PropertyDescriptor
     * @param propertyName       属性名
     * @return {Property}
     */
    public static TypeDescriptor getTypeDescriptor(final Class<?> propertyType, final PropertyDescriptor propertyDescriptor, final String propertyName) {
        Method readMethod = propertyDescriptor.getReadMethod();
        Method writeMethod = propertyDescriptor.getWriteMethod();
        Property property = new Property(propertyType, readMethod, writeMethod, propertyName);
        return new TypeDescriptor(property);
    }

    /**
     * 获取 类属性.
     *
     * @param clazz     类信息
     * @param fieldName 属性名
     * @return Field
     */
    @Nullable
    public static Field getField(final Class<?> clazz, final String fieldName) {
        Class<?> cls = clazz;
        while (cls != Object.class) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                cls = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取 所有 field 属性上的注解.
     *
     * @param clazz           类
     * @param fieldName       属性名
     * @param annotationClass 注解
     * @param <T>             注解泛型
     * @return 注解
     */
    @Nullable
    public static <T extends Annotation> T getAnnotation(final Class<?> clazz, final String fieldName, final Class<T> annotationClass) {
        Field field = ReflectUtils.getField(clazz, fieldName);
        if (field == null) {
            return null;
        }
        return field.getAnnotation(annotationClass);
    }

    /**
     * 调用Getter方法.
     *
     * @param obj          对象
     * @param propertyName 字段名
     * @return Object
     */
    public static Object invokeGetterMethod(final Object obj, final String propertyName) {
        String getterMethodName = "get" + StringUtils.capitalize(propertyName);
        return invokeMethod(obj, getterMethodName, new Class[]{}, new Object[]{});
    }

    /**
     * 调用Setter方法.使用value的Class来查找Setter方法.
     *
     * @param obj          对象
     * @param propertyName 字段名
     * @param value        字段值
     */
    public static void invokeSetterMethod(final Object obj, final String propertyName, final Object value) {
        invokeSetterMethod(obj, propertyName, value, null);
    }

    /**
     * 调用Setter方法.
     *
     * @param obj          对象
     * @param propertyName 字段名
     * @param value        字段值
     * @param propertyType 用于查找Setter方法,为空时使用value的Class替代.
     */
    public static void invokeSetterMethod(final Object obj, final String propertyName, final Object value, final Class<?> propertyType) {
        Class<?> type = propertyType != null ? propertyType : value.getClass();
        String setterMethodName = "set" + StringUtils.capitalize(propertyName);
        invokeMethod(obj, setterMethodName, new Class[]{type}, new Object[]{value});
    }

    /**
     * 直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数.
     *
     * @param obj       对象
     * @param fieldName 字段名
     * @return Object
     */
    public static Object getFieldValue(final Object obj, final String fieldName) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
        }

        Object result = null;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            log.error("不可能抛出的异常{}", e.getMessage());
        }
        return result;
    }

    /**
     * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数.
     *
     * @param obj       对象
     * @param fieldName 字段名
     * @param value     字段值
     */
    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
        }

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            log.error("不可能抛出的异常:{}", e.getMessage());
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     * 如向上转型到Object仍无法找到, 返回null.
     *
     * @param obj       对象
     * @param fieldName 字段名
     * @return Field
     */
    public static Field getAccessibleField(final Object obj, final String fieldName) {
        Objects.requireNonNull(obj, "object不能为空");
        Objects.requireNonNull(fieldName, "fieldName");
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // Field不在当前类定义,继续向上转型
            }
        }
        return null;
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符. 用于一次性调用的情况.
     *
     * @param obj            对象
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @param args           参数值
     * @return Object
     */
    public static Object invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes, final Object[] args) {
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("Could not find method [" + methodName + "] on target [" + obj + "]");
        }

        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw convertReflectionExceptionToUnchecked(e);
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问. 如向上转型到Object仍无法找到, 返回null.
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object...args)
     *
     * @param obj            对象
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return Method
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName, final Class<?>... parameterTypes) {
        Objects.requireNonNull(obj, "object不能为空");

        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Method method = superClass.getDeclaredMethod(methodName, parameterTypes);

                method.setAccessible(true);

                return method;

            } catch (NoSuchMethodException e) {
                // Method不在当前类定义,继续向上转型
            }
        }
        return null;
    }

    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型. 如无法找到, 返回Object.class.
     *
     * @param clazz 类
     * @param <T>   类型
     * @return Class
     */
    public static <T> Class<T> getSuperClassGenricType(final Class clazz) {
        return getSuperClassGenricType(clazz, 0);
    }

    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型. 如无法找到, 返回Object.class.
     *
     * @param clazz 类
     * @param index 下标
     * @return Class
     */
    public static Class getSuperClassGenricType(final Class clazz, final int index) {

        Type genType = clazz.getGenericSuperclass();

        if (!(genType instanceof ParameterizedType)) {
            log.warn(clazz.getSimpleName() + "'s superclass not ParameterizedType");
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            log.warn("Index: " + index + ", Size of " + clazz.getSimpleName() + "'s Parameterized Type: " + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            log.warn(clazz.getSimpleName() + " not set the actual class on superclass generic parameter");
            return Object.class;
        }

        return (Class) params[index];
    }

    /**
     * 将反射时的checked exception转换为unchecked exception.
     *
     * @param e 异常
     * @return RuntimeException
     */
    public static RuntimeException convertReflectionExceptionToUnchecked(final Exception e) {
        if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException || e instanceof NoSuchMethodException) {
            return new IllegalArgumentException("Reflection Exception.", e);
        } else if (e instanceof InvocationTargetException) {
            return new RuntimeException("Reflection Exception.", ((InvocationTargetException) e).getTargetException());
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException("Unexpected Checked Exception.", e);
    }

    /**
     * 返回Class所有字段及父类字段.
     *
     * @param clazz 类
     * @return List
     */
    public static List<Field> getDeclaredFields(final Class<?> clazz) {
        List<Field> fieldList = ALL_FIELD_CACHE.getOrDefault(clazz, new ArrayList<>());
        if (CollectionUtil.isNotEmpty(fieldList)) {
            return fieldList;
        }
        Class<?> cls = clazz;
        for (; cls != Object.class; cls = cls.getSuperclass()) {
            Field[] fields = cls.getDeclaredFields();
            if (fields.length > 0) {
                fieldList.addAll(Stream.of(fields).filter(field -> !Modifier.isFinal(field.getModifiers())).collect(Collectors.toList()));
            }
        }
        ALL_FIELD_CACHE.put(clazz, fieldList);
        return fieldList;
    }

    /**
     * 返回Class所有字段及忽略父类字段.
     *
     * @param clazz       类
     * @param ignoreClazz 忽略类
     * @return List
     */
    public static List<Field> getDeclaredFields(final Class<?> clazz, final Class<?> ignoreClazz) {
        String key = String.format("%s-%s", clazz.getName(), ignoreClazz.getName());
        List<Field> fileds = PART_FIELD_CACHE.getOrDefault(key, new ArrayList<>());
        if (CollectionUtil.isNotEmpty(fileds)) {
            return fileds;
        }

        Class<?> cls = clazz;
        for (; cls != Object.class && cls != ignoreClazz; cls = cls.getSuperclass()) {
            Field[] fields = cls.getDeclaredFields();
            if (fields.length > 0) {
                fileds.addAll(Stream.of(fields).filter(field -> !Modifier.isFinal(field.getModifiers())).collect(Collectors.toList()));
            }
        }
        PART_FIELD_CACHE.put(key, fileds);
        return fileds;
    }


    /**
     * 处理对象数据转String
     *   默认数据类型直接转String
     *   集合、数组，Map 仅打印长度
     *   其他对象根据属性处理，最终结果转json
     *
     * @param obj 需要处理的对象
     * @return 长度超过二十则打印长度，否则打印数据内容
     */
    public static String processObject(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        if (ClassUtil.isSimpleValueType(obj.getClass())) {
            return String.valueOf(obj);
        }
        if (obj instanceof Collection || obj instanceof Map || obj.getClass().isArray()) {
            int len = ObjectUtil.length(obj);
            return String.format("%s@%s", obj.getClass(), len);
        }
        Map<String, Object> map = new HashMap<>();
        ReflectionUtils.doWithFields(obj.getClass(), f -> {
            f.setAccessible(true);
            if (obj.getClass() == f.getType()) {
                map.put(f.getName(), JsonUtil.toJson(f.get(obj)));
            } else {
                map.put(f.getName(), processObject(f.get(obj)));
            }
        }, f -> !Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers()));
        return JsonUtil.toJson(map);
    }
}
