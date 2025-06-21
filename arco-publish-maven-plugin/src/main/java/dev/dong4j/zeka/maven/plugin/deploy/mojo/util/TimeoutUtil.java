package dev.dong4j.zeka.maven.plugin.deploy.mojo.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Description: 简单超时工具类</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.07.02 23:10
 * @since 1.5.0
 */
@Slf4j
@UtilityClass
@SuppressWarnings("all")
public final class TimeoutUtil {
    /** executor */
    private static final ExecutorService TIMEOUT_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Process
     *
     * @param <T>     parameter
     * @param task    task
     * @param timeout timeout
     * @return the t
     * @throws TimeoutException timeout exception
     * @since 1.5.0
     */
    @Contract("null, _ -> null")
    public static <T> T process(Callable<T> task, long timeout) throws TimeoutException {
        if (task == null) {
            return null;
        }
        Future<T> futureRet = TIMEOUT_EXECUTOR.submit(task);
        try {
            return futureRet.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupt Exception", e);
        } catch (ExecutionException e) {
            log.error("Task execute exception", e);
        } catch (TimeoutException e) {
            log.warn("任务处理超时 [{}]s", timeout);
            if (!futureRet.isCancelled()) {
                futureRet.cancel(true);
            }
            throw e;
        }
        return null;
    }
}
