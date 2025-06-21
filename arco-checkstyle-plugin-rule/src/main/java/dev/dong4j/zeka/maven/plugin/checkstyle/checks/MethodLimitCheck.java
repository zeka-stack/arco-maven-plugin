package dev.dong4j.zeka.maven.plugin.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.3
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
