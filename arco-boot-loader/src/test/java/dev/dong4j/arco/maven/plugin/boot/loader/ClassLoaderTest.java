package dev.dong4j.arco.maven.plugin.boot.loader;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import sun.net.spi.nameservice.dns.DNSNameService;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.10 17:56
 * @since 1.5.0
 */
@Slf4j
@SuppressWarnings("all")
class ClassLoaderTest {

    /**
     * Test classloader
     *
     * @since 1.5.0
     */
    @Test
    void test_classloader() {
        System.out.println("ClassLoaderTest's ClassLoader is " + ClassLoaderTest.class.getClassLoader());
        System.out.println("DNSNameService's ClassLoader is " + DNSNameService.class.getClassLoader());
        System.out.println("String's ClassLoader is " + String.class.getClassLoader());
    }

    /**
     * Test parent classloader
     *
     * @since 1.5.0
     */
    @Test
    void test_parent_classloader() {
        System.out.println("ClassLoaderTest's ClassLoader is " + ClassLoaderTest.class.getClassLoader());
        System.out.println("The Parent of ClassLoaderTest's ClassLoader is " + ClassLoaderTest.class.getClassLoader().getParent());
        System.out.println("The GrandParent of ClassLoaderTest's ClassLoader is " + ClassLoaderTest.class.getClassLoader().getParent().getParent());

        System.out.println("The ContextClassLoader is " + Thread.currentThread().getContextClassLoader());
    }

}
