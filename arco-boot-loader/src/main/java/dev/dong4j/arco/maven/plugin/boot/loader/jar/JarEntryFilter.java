package dev.dong4j.arco.maven.plugin.boot.loader.jar;

/**
 * Interface that can be used to filter and optionally rename jar entries.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
interface JarEntryFilter {

    /**
     * Apply the jar entry filter.
     *
     * @param name the current entry name. This may be different that the original entry             name if a previous filter has been
     *             applied
     * @return the new name of the entry or {@code null} if the entry should not be     included.
     * @since 1.0.0
     */
    AsciiBytes apply(AsciiBytes name);

}
