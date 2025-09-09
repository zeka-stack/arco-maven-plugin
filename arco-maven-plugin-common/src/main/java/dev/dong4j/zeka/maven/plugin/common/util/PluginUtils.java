package dev.dong4j.zeka.maven.plugin.common.util;

import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.enums.ModuleType;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Maven插件工具类，提供了模块类型管理和系统属性操作的工具方法
 * <p>
 * 该工具类为Maven插件系统提供了模块类型的读取和设置功能
 * 通过系统属性机制实现模块类型的传递和共享，为不同的Maven插件提供一致的模块信息
 * <p>
 * 主要功能特性：
 * <ul>
 *     <li>模块类型读取 - 从系统属性中获取当前模块的类型</li>
 *     <li>模块类型设置 - 将模块类型信息存储到系统属性中</li>
 *     <li>类型安全转换 - 使用EnumUtils确保类型转换的安全性</li>
 *     <li>空值处理 - 优雅处理未设置或无效的模块类型</li>
 * </ul>
 * <p>
 * 该工具类在Maven插件的模块化构建过程中起到关键作用
 * 允许在不同的构建阶段和插件间传递模块类型信息，实现上下文的一致性
 * <p>
 * 使用场景：
 * <ul>
 *     <li>Maven插件中的模块类型识别和传递</li>
 *     <li>多模块项目中的模块信息共享</li>
 *     <li>条件化构建逻辑中的模块类型判断</li>
 *     <li>插件链中的上下文信息传递</li>
 * </ul>
 * <p>
 * 技术实现特点：
 * <ul>
 *     <li>使用Lombok @UtilityClass注解确保工具类的不可实例化</li>
 *     <li>集成Apache Commons Lang3的EnumUtils提供安全的枚举转换</li>
 *     <li>使用JetBrains @NotNull注解增强空值安全性</li>
 *     <li>通过系统属性实现跨插件的信息共享</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dongshijie@gmail.com"
 * @date 2020.05.01 20:15
 * @since 1.0.0
 */
@UtilityClass
public class PluginUtils {

    /**
     * Module type
     *
     * @return the module type
     * @since 1.0.0
     */
    public static ModuleType moduleType() {
        String moduleType = System.getProperty(Plugins.MODULE_TYPE);
        return EnumUtils.getEnum(ModuleType.class, moduleType);
    }

    /**
     * Module type
     *
     * @param type type
     * @since 1.0.0
     */
    public static void moduleType(@NotNull ModuleType type) {
        System.setProperty(Plugins.MODULE_TYPE, type.name());
    }
}
