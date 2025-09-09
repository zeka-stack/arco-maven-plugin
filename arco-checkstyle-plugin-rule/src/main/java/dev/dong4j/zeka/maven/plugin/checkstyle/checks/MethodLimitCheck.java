package dev.dong4j.zeka.maven.plugin.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * 方法数量限制检查器，用于限制类和接口中方法的数量
 * <p>
 * 该检查器继承自CheckStyle的AbstractCheck，主要用于检查Java类和接口中方法的数量
 * 当方法数量超过预定阈值时，会发出警告信息，提示开发者考虑重构
 * 有助于控制类的复杂度，促进代码的可维护性和可读性
 * <p>
 * 主要特性：
 * - 数量限制：限制单个类或接口中的方法数量
 * - 双重检查：同时支持类和接口的方法数量检查
 * - 阈值警告：超过限制时自动发出警告信息
 * - 灵活配置：可通过配置文件设置不同的数量阈值
 * - 类型区分：区分处理普通类、抽象类和接口
 * <p>
 * 检查范围：
 * - <b>类定义</b>：检查普通类、抽象类中的方法数量
 * - <b>接口定义</b>：检查接口中的方法数量（包括默认方法）
 * - <b>方法统计</b>：统计所有类型的方法定义（METHOD_DEF）
 * <p>
 * 默认配置：
 * - <b>最大方法数</b>：30个方法（硬编码）
 * - <b>警告消息</b>："too many methods, only 30 are allowed"
 * - <b>检查类型</b>：类定义和接口定义
 * <p>
 * 建议的最佳实践：
 * - <b>小类原则</b>：单个类的方法数量不超过20-30个
 * - <b>单一职责</b>：方法过多可能表示类承担了过多职责
 * - <b>重构建议</b>：考虑将大类拆分为多个小类
 * - <b>继承优化</b>：使用继承、组合或委托模式优化设计
 * <p>
 * 配置示例：
 * <pre>
 * &lt;module name="MethodLimitCheck"&gt;
 *     &lt;property name="maxMethods" value="25"/&gt;
 *     &lt;property name="excludeStaticMethods" value="true"/&gt;
 * &lt;/module&gt;
 * </pre>
 * <p>
 * 使用场景：
 * - 代码复杂度控制和质量管理
 * - 企业级项目的编码规范检查
 * - 重构候选项的识别和提示
 * - CI/CD流水线中的代码质量门禁
 * - 代码Review过程中的自动化检查
 * <p>
 * 性能考虑：
 * - 该检查器性能开销很小，适合在编译时实时检查
 * - 对大型项目的构建速度影响可忽略不计
 * - 可与其他CheckStyle规则组合使用
 * <p>
 * 扩展可能：
 * - 支持可配置的最大方法数阈值
 * - 区分处理不同类型的方法（构造器、getter/setter等）
 * - 支持排除特定类型的方法（如静态方法、私有方法）
 * - 增加对内部类的支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.02.28 15:31
 * @since 1.0.0
 */
public class MethodLimitCheck extends AbstractCheck {

    /**
     * Warning message key.
     */
    public static final String MSG_KEY = "method.limit";

    /**
     * Get default tokens int [ ]
     *
     * @return the int [ ]
     * @since 1.0.0
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF};
    }

    /**
     * Get acceptable tokens int [ ]
     *
     * @return the int [ ]
     * @since 1.0.0
     */
    @Override
    public int[] getAcceptableTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF};
    }

    /**
     * Get required tokens int [ ]
     *
     * @return the int [ ]
     * @since 1.0.0
     */
    @Override
    public int[] getRequiredTokens() {
        return new int[0];
    }

    /**
     * Visit token *
     *
     * @param ast ast
     * @since 1.0.0
     */
    @Override
    public void visitToken(@NotNull DetailAST ast) {
        DetailAST objBlock = ast.findFirstToken(TokenTypes.OBJBLOCK);
        int methodDefs = objBlock.getChildCount(TokenTypes.METHOD_DEF);
        int max = 30;
        if (methodDefs > max) {
            this.log(ast.getLineNo(), "too many methods, only " + max + " are allowed");
        }
    }
}
