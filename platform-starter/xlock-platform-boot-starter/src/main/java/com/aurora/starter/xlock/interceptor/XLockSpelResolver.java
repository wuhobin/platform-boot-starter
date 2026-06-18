package com.aurora.starter.xlock.interceptor;

import com.aurora.starter.xlock.annotation.XKey;
import com.aurora.starter.xlock.annotation.XLock;
import com.aurora.starter.xlock.exception.LockKeyBuilderException;
import com.aurora.starter.xlock.model.KeyInfo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * spring el解析key策略.
 *
 * @author breggor
 */
@Slf4j
public class XLockSpelResolver {


    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    /**
     * 获取KeyInfo.
     *
     * @param joinPoint joinPoint
     * @param xLock     xLock
     * @return KeyInfo
     */
    public KeyInfo getKeyInfo(final JoinPoint joinPoint, final XLock xLock) {
        String key = joinPoint.toString();
        KeyInfo keyInfo = genKeyInfo(joinPoint, xLock);
        if (!xLock.disableLog()) {
            log.info("[分布式锁] - 获取当前线程keyInfo:{} {}", key, keyInfo);
        }
        return keyInfo;
    }

    /**
     * key构建对象.
     *
     * @param joinPoint joinPoint
     * @param xLock     锁注解
     * @return key构建对象
     * @throws LockKeyBuilderException key构建异常
     */
    private KeyInfo genKeyInfo(final JoinPoint joinPoint, final XLock xLock) throws LockKeyBuilderException {
        final Object[] args = joinPoint.getArgs();
        Method method = getMethod(joinPoint);
        KeyInfo.KeyInfoBuilder builder = KeyInfo.builder()
            .prefix(xLock.prefix())
            .keys(getKeys(method, xLock.keys(), args))
            .leaseTime(xLock.leaseTime()).waitTime(xLock.waitTime())
            .timeUnit(xLock.timeUnit())
            .disableLog(xLock.disableLog())
            .errorMessage(xLock.errorMessage());
        return builder.build();
    }

    private String[] getKeys(Method method, String[] keys, Object[] args) {
        List<String> result = new ArrayList<>();
        //获取锁方法上注解key
        if (keys != null && keys.length > 0) {
            EvaluationContext context = new MethodBasedEvaluationContext(null, method, args, PARAMETER_NAME_DISCOVERER);
            for (String key : keys) {
                if (StringUtils.hasLength(key)) {
                    try {
                        Expression expression = EXPRESSION_PARSER.parseExpression(key);
                        Object val = expression.getValue(context);
                        if (Objects.nonNull(val)) {
                            result.add(val.toString());
                        }
                    } catch (Exception e) {
                        result.add(key);
                    }
                }
            }
        }
        //获取锁方法参数的注解key
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(XKey.class) != null) {
                XKey anno = parameters[i].getAnnotation(XKey.class);
                if (anno.value().isEmpty()) {
                    result.add(args[i].toString());
                } else {
                    Object key = EXPRESSION_PARSER.parseExpression(anno.value()).getValue(new StandardEvaluationContext(args[i]));
                    if (Objects.nonNull(key)) {
                        result.add(key.toString());
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            result.add(KeyInfo.EMPTY_KEY);
        }
        return result.toArray(String[]::new);
    }

    /**
     * 获取切面方法.
     *
     * @param joinPoint 切面方法
     * @return 方法对象
     */
    private Method getMethod(final JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(), method.getParameterTypes());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return method;
    }

}
