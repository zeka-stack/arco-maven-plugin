package dev.dong4j.arco.maven.plugin.helper.mojo;

import dev.dong4j.arco.maven.plugin.common.ZekaStackMavenPluginAbstractMojo;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.3
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "timestamp-property", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class TimestampPropertyMojo extends ZekaStackMavenPluginAbstractMojo {

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
