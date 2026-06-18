package com.aurora.starter.common.utils.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 异步线程数据传递处理
 */
public class RunnableDecorator {

    public static final Logger LOG = LoggerFactory.getLogger("RunnableDecorator");

    /**
     * 装饰线程任务
     *  如果执行时线程名是以 threadNamePrefix 为前缀，则任务是在线程池内线程执行
     *      此时获取当前线程的数据传递给执行的线程，并在执行完成后清理
     *  如果执行时线程名不是以 threadNamePrefix 为前缀则，则任务是根据拒绝策略在其他线程执行
     *      此时直接执行任务，不传递任何数据
     * @param runnable 原任务
     * @param threadNamePrefix 当前线程池线程名称前缀
     * @return
     */
    public static Runnable decorate(final Runnable runnable, final String threadNamePrefix) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        Map<String, Object> data = RequestThread.getData();
        return () -> {
            if (!Thread.currentThread().getName().startsWith(threadNamePrefix)) {
                //此时任务是根据拒绝策略可能在当前主线程执行。此情况直接执行任务不需要处理传递线程数据了
                runnable.run();
                return;
            }
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                RequestThread.setData(data);
                runnable.run();
            } catch (Throwable t) {
                LOG.error("线程发现异常:", t);
                throw t;
            } finally {
                MDC.clear();
                RequestThread.clear();
            }
        };
    }

    /**
     * 包装 Supplier，使其在异步线程中保留原线程 MDC 和 RequestThread 数据
     *
     * @param supplier         原始 Supplier
     * @param threadNamePrefix 线程名前缀，用于判断是否是线程池执行
     * @param <T>              返回值类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> decorate(final Supplier<T> supplier, final String threadNamePrefix) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        Map<String, Object> data = RequestThread.getData();

        return () -> {
            // 如果是调用者线程执行（如线程池满执行 CallerRunsPolicy）
            if (!Thread.currentThread().getName().startsWith(threadNamePrefix)) {
                return supplier.get();
            }

            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                RequestThread.setData(data);
                return supplier.get();
            } catch (Throwable t) {
                LOG.error("线程发现异常:", t);
                throw t;
            } finally {
                MDC.clear();
                RequestThread.clear();
            }
        };
    }
}