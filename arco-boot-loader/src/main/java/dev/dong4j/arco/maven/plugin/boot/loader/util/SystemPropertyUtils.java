package dev.dong4j.arco.maven.plugin.boot.loader.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * 用于解析文本中占位符的帮助程序类, 通常应用于文件路径.
 * 文本可以包含 $ 占位符, 解析为系统属性: 例如 $user.dir, 可以使用键和值之间的 ":" 分隔符提供默认值
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:47
 * @see System#getProperty(String) System#getProperty(String)System#getProperty(String)System#getProperty(String)System#getProperty(String)
 * @since 1.0.0
 */
@SuppressWarnings("all")
public abstract class SystemPropertyUtils {

    /**
     * Prefix for system property placeholders: "${".
     */
    public static final String PLACEHOLDER_PREFIX = "${";

    /**
     * Suffix for system property placeholders: "}".
     */
    public static final String PLACEHOLDER_SUFFIX = "}";

    /**
     * Value separator for system property placeholders: ":".
     */
    public static final String VALUE_SEPARATOR = ":";

    /** SIMPLE_PREFIX */
    private static final String SIMPLE_PREFIX = PLACEHOLDER_PREFIX.substring(1);

    /**
     * Resolve ${...} placeholders in the given text, replacing them with corresponding
     * system property values.
     *
     * @param text the String to resolve
     * @return the resolved String
     * @throws IllegalArgumentException if there is an unresolvable placeholder
     * @see #PLACEHOLDER_PREFIX #PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX #PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX
     * @since 1.0.0
     */
    @Contract("null -> null; !null -> !null")
    public static String resolvePlaceholders(String text) {
        if (text == null) {
            return text;
        }
        return parseStringValue(null, text, text, new HashSet<>());
    }

    /**
     * Resolve ${...} placeholders in the given text, replacing them with corresponding
     * system property values.
     *
     * @param properties a properties instance to use in addition to System
     * @param text       the String to resolve
     * @return the resolved String
     * @throws IllegalArgumentException if there is an unresolvable placeholder
     * @see #PLACEHOLDER_PREFIX #PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX#PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX #PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX#PLACEHOLDER_SUFFIX
     * @since 1.0.0
     */
    @Contract("_, null -> null; _, !null -> !null")
    public static String resolvePlaceholders(Properties properties, String text) {
        if (text == null) {
            return text;
        }
        return parseStringValue(properties, text, text, new HashSet<>());
    }

    /**
     * Parse string value string
     *
     * @param properties          properties
     * @param value               value
     * @param current             current
     * @param visitedPlaceholders visited placeholders
     * @return the string
     * @since 1.0.0
     */
    private static @NotNull String parseStringValue(Properties properties, String value, String current,
                                                    Set<String> visitedPlaceholders) {

        StringBuilder buf = new StringBuilder(current);

        int startIndex = current.indexOf(PLACEHOLDER_PREFIX);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException(
                        "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the
                // placeholder
                // key.
                placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
                // Now obtain the value for the fully resolved key...
                String propVal = resolvePlaceholder(properties, value, placeholder);
                if (propVal == null) {
                    int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + VALUE_SEPARATOR.length());
                        propVal = resolvePlaceholder(properties, value, actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(properties, value, propVal, visitedPlaceholders);
                    buf.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propVal);
                    startIndex = buf.indexOf(PLACEHOLDER_PREFIX, startIndex + propVal.length());
                } else {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf(PLACEHOLDER_PREFIX, endIndex + PLACEHOLDER_SUFFIX.length());
                }
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }

    /**
     * Resolve placeholder string
     *
     * @param properties      properties
     * @param text            text
     * @param placeholderName placeholder name
     * @return the string
     * @since 1.0.0
     */
    private static @Nullable String resolvePlaceholder(Properties properties, String text, String placeholderName) {
        String propVal = getProperty(placeholderName, null, text);
        if (propVal != null) {
            return propVal;
        }
        return (properties != null) ? properties.getProperty(placeholderName) : null;
    }

    /**
     * Gets property *
     *
     * @param key key
     * @return the property
     * @since 1.0.0
     */
    public static String getProperty(String key) {
        return getProperty(key, null, "");
    }

    /**
     * Gets property *
     *
     * @param key          key
     * @param defaultValue default value
     * @return the property
     * @since 1.0.0
     */
    public static String getProperty(String key, String defaultValue) {
        return getProperty(key, defaultValue, "");
    }

    /**
     * Search the System properties and environment variables for a value with the
     * provided key. Environment variables in {@code UPPER_CASE} style are allowed where
     * System properties would normally be {@code lower.case}.
     *
     * @param key          the key to resolve
     * @param defaultValue the default value
     * @param text         optional extra context for an error message if the key resolution fails (e.g. if System
     *                     properties                     are not accessible)
     * @return a static property value or null of not found
     * @since 1.0.0
     */
    public static String getProperty(String key, String defaultValue, String text) {
        try {
            String propVal = System.getProperty(key);
            if (propVal == null) {
                // Fall back to searching the system environment.
                propVal = System.getenv(key);
            }
            if (propVal == null) {
                // Try with underscores.
                String name = key.replace('.', '_');
                propVal = System.getenv(name);
            }
            if (propVal == null) {
                // Try uppercase with underscores as well.
                String name = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
                propVal = System.getenv(name);
            }
            if (propVal != null) {
                return propVal;
            }
        } catch (Throwable ex) {
            System.err.println("Could not resolve key '" + key + "' in '" + text
                + "' as system property or in environment: " + ex);
        }
        return defaultValue;
    }

    /**
     * Find placeholder end index int
     *
     * @param buf        buf
     * @param startIndex start index
     * @return the int
     * @since 1.0.0
     */
    private static int findPlaceholderEndIndex(@NotNull CharSequence buf, int startIndex) {
        int index = startIndex + PLACEHOLDER_PREFIX.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + PLACEHOLDER_SUFFIX.length();
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, SIMPLE_PREFIX)) {
                withinNestedPlaceholder++;
                index = index + SIMPLE_PREFIX.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    /**
     * Substring match boolean
     *
     * @param str       str
     * @param index     index
     * @param substring substring
     * @return the boolean
     * @since 1.0.0
     */
    private static boolean substringMatch(CharSequence str, int index, @NotNull CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }

}
