package dev.dong4j.zeka.maven.plugin.common.exception;

/**
 * Exception thrown when an additional property with a null value is encountered.
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 18:35
 * @since 1.0.0
 */
public class NullAdditionalPropertyValueException extends IllegalArgumentException {

    /** serialVersionUID */
    private static final long serialVersionUID = -7157595142587204486L;

    /**
     * Instantiates a new Null additional property value exception.
     *
     * @param name the name
     * @since 1.0.0
     */
    public NullAdditionalPropertyValueException(String name) {
        super("Additional property '" + name + "' is illegal as its value is null");
    }
}
