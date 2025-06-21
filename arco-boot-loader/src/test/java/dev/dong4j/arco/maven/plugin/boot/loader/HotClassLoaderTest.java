package dev.dong4j.arco.maven.plugin.boot.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
class HotClassLoaderTest {

    /**
     * Test
     *
     * @throws Exception exception
     * @since 1.9.0
     */
    @Test
    void test() throws Exception {
        String javaCode = "import loader.dev.dong4j.arco.maven.plugin.boot.Base;\n" +
            "public class T implements Base {\n" +
            "    @Override\n" +
            "    public String say() {\n" +
            "        return \"hello\";\n" +
            "    }\n" +
            "}";

        String version = "1";

        Class<?> clazz = ClassEngine.getInstance().compileAndLoadClass("T", javaCode, version, false);
        Base base1 = (Base) clazz.newInstance();

        Assertions.assertEquals("hello", base1.say());

        javaCode = "import loader.dev.dong4j.arco.maven.plugin.boot.Base;\n" +
            "public class T implements Base {\n" +
            "    @Override\n" +
            "    public String say() {\n" +
            "        return \"hello aaa\";\n" +
            "    }\n" +
            "}";
        version = "2";

        clazz = ClassEngine.getInstance().compileAndLoadClass("T", javaCode, version, false);
        Base base2 = (Base) clazz.newInstance();
        Assertions.assertEquals("hello aaa", base2.say());
    }
}
