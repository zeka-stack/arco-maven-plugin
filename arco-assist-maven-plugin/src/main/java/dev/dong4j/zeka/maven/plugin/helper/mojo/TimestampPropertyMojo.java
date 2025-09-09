package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 时间戳属性生成Mojo，用于在Maven构建过程中生成和注入时间戳属性
 * <p>
 * 该Mojo在Maven验证阶段执行，提供灵活的时间戳生成功能
 * 支持多种时间格式、时区配置、时间偏移和国际化支持
 * 生成的时间戳属性可被其他Maven插件或应用程序使用
 * <p>
 * 主要特性：
 * - 灵活格式：支持自定义时间格式模式（SimpleDateFormat）
 * - 时区支持：支持任意时区配置，默认为GMT
 * - 时间偏移：支持对当前时间进行各种单位的偏移计算
 * - 国际化：支持不同语言和地区的本地化配置
 * - 属性注入：自动将生成的时间戳注入到Maven属性中
 * <p>
 * 支持的时间偏移单位：
 * - <b>millisecond</b>：毫秒级偏移
 * - <b>second</b>：秒级偏移（默认）
 * - <b>minute</b>：分钟级偏移
 * - <b>hour</b>：小时级偏移
 * - <b>day</b>：天级偏移
 * - <b>week</b>：周级偏移
 * - <b>month</b>：月级偏移
 * - <b>year</b>：年级偏移
 * <p>
 * 配置参数：
 * - <b>name</b>：要设置的属性名称（必填）
 * - <b>pattern</b>：时间格式模式，遵循Java SimpleDateFormat规则
 * - <b>timeZone</b>：时区配置，默认为GMT
 * - <b>offset</b>：时间偏移量，默认为0
 * - <b>unit</b>：偏移单位，默认为second
 * - <b>locale</b>：本地化配置，格式为"zh,CN"或"en,US"
 * <p>
 * 使用场景：
 * - 版本号中包含时间戳信息
 * - 构建产物的时间标识
 * - 日志文件命名中的时间戳
 * - API版本信息中的构建时间
 * - CI/CD流水线中的时间标记
 * - 数据库迁移脚本的版本控制
 * <p>
 * 使用示例：
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;dev.dong4j&lt;/groupId&gt;
 *     &lt;artifactId&gt;arco-assist-maven-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *         &lt;execution&gt;
 *             &lt;goals&gt;
 *                 &lt;goal&gt;timestamp-property&lt;/goal&gt;
 *             &lt;/goals&gt;
 *             &lt;configuration&gt;
 *                 &lt;name&gt;build.timestamp&lt;/name&gt;
 *                 &lt;pattern&gt;yyyy-MM-dd HH:mm:ss&lt;/pattern&gt;
 *                 &lt;timeZone&gt;Asia/Shanghai&lt;/timeZone&gt;
 *                 &lt;locale&gt;zh,CN&lt;/locale&gt;
 *             &lt;/configuration&gt;
 *         &lt;/execution&gt;
 *     &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 * <p>
 * 执行配置：
 * - 默认阶段：validate（验证阶段）
 * - 目标名称：timestamp-property
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "timestamp-property", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class TimestampPropertyMojo extends ZekaMavenPluginAbstractMojo {

    /** The property to set. */
    @Parameter(required = true)
    private String name;
    /** The date/time pattern to be used. The values are as defined by the Java SimpleDateFormat class. */
    @Parameter
    private String pattern;
    /** The timezone to use for displaying time. The values are as defined by the Java {$link TimeZone} class. */
    @Parameter(defaultValue = "GMT")
    private String timeZone;
    /** An offset to apply to the current time. */
    @Parameter(defaultValue = "0")
    private int offset;

    /**
     * The unit of the offset to be applied to the current time. Valid Values are
     * <ul>
     * <li>millisecond</li>
     * <li>second</li>
     * <li>minute</li>
     * <li>hour</li>
     * <li>day</li>
     * <li>week</li>
     * <li>month</li>
     * <li>year</li>
     * </ul>
     */
    @Parameter(defaultValue = "second")
    private String unit;

    /**
     * The locale to use, for example <code>en,US</code>.
     */
    @Parameter
    private String locale;

    /**
     * Execute
     *
     * @throws MojoExecutionException mojo execution exception
     * @throws MojoFailureException   mojo failure exception
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        Locale locale;
        if (this.locale != null) {
            String[] bits = this.locale.split("[,_]");
            if (bits.length == 1) {
                locale = new Locale(bits[0].trim());
            } else if (bits.length == 2) {
                locale = new Locale(bits[0].trim(), bits[1].trim());
            } else if (bits.length == 3) {
                locale = new Locale(bits[0].trim(), bits[1].trim(), bits[2].trim());
            } else {
                throw new MojoExecutionException("expecting language,country,variant but got more than three parts");
            }
        } else {
            locale = Locale.getDefault();
            getLog().warn("Using platform locale (" + locale.toString()
                + " actually) to format date/time, i.e. build is platform dependent!");
        }

        DateFormat format;
        if (pattern == null) {
            format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        } else {
            try {
                format = new SimpleDateFormat(pattern, locale);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        TimeZone timeZone;
        if (this.timeZone != null) {
            timeZone = TimeZone.getTimeZone(this.timeZone);
        } else {
            timeZone = TimeZone.getTimeZone("GMT");
        }

        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.setTimeZone(timeZone);
        if (offset != 0 && unit != null) {
            unit = unit.toLowerCase();
            if (unit.indexOf("millisecond") == 0) {
                calendar.add(Calendar.MILLISECOND, offset);
            } else if (unit.indexOf("second") == 0) {
                calendar.add(Calendar.SECOND, offset);
            } else if (unit.indexOf("minute") == 0) {
                calendar.add(Calendar.MINUTE, offset);
            } else if (unit.indexOf("hour") == 0) {
                calendar.add(Calendar.HOUR, offset);
            } else if (unit.indexOf("day") == 0) {
                calendar.add(Calendar.DAY_OF_MONTH, offset);
            } else if (unit.indexOf("week") == 0) {
                calendar.add(Calendar.WEEK_OF_YEAR, offset);
            } else if (unit.indexOf("month") == 0) {
                calendar.add(Calendar.MONTH, offset);
            } else if (unit.indexOf("year") == 0) {
                calendar.add(Calendar.YEAR, offset);
            }
        }

        format.setTimeZone(timeZone);
        defineProperty(name, format.format(calendar.getTime()));

        getLog().info("注入: " + name + ", 格式: " + pattern + ", 区域:" + timeZone);
    }
}
