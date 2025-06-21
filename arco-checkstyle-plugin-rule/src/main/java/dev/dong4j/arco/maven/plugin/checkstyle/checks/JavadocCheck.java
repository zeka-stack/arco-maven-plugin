package dev.dong4j.arco.maven.plugin.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTag;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTypeCheck;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>Description: 检查 javadoc 是否存在未修改的 version 和 since</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2022.01.21 00:06
 * @see JavadocTypeCheck
 * @since 2022.1.1
 */
@SuppressWarnings("all")
public class JavadocCheck extends AbstractCheck {

    public static final String MSG_JAVADOC_MISSING = "xxxx";

    /** Specify the visibility scope where Javadoc comments are checked. */
    private Scope scope = Scope.PRIVATE;

    /** Specify the visibility scope where Javadoc comments are not checked. */
    private Scope excludeScope;

    /** Specify the regexp to define variable names to ignore. */
    private Pattern ignoreNamePattern;

    /**
     * Setter to specify the visibility scope where Javadoc comments are checked.
     *
     * @param scope a scope.
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Setter to specify the visibility scope where Javadoc comments are not checked.
     *
     * @param excludeScope a scope.
     */
    public void setExcludeScope(Scope excludeScope) {
        this.excludeScope = excludeScope;
    }

    /**
     * Setter to specify the regexp to define variable names to ignore.
     *
     * @param pattern a pattern.
     */
    public void setIgnoreNamePattern(Pattern pattern) {
        ignoreNamePattern = pattern;
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
            TokenTypes.VARIABLE_DEF,
            TokenTypes.ENUM_CONSTANT_DEF,
        };
    }

    /*
     * Skipping enum values is requested.
     * Checkstyle's issue #1669: https://github.com/checkstyle/checkstyle/issues/1669
     */
    @Override
    public int[] getRequiredTokens() {
        return new int[]{
            TokenTypes.VARIABLE_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (shouldCheck(ast)) {
            final FileContents contents = getFileContents();
            final TextBlock textBlock = contents.getJavadocBefore(ast.getLineNo());

            if (textBlock == null) {
                log(ast, MSG_JAVADOC_MISSING);
            } else {
                // final List<JavadocTag> tags = getJavadocTags(textBlock);
                // final int lineNo = ast.getLineNo();
                // if (ScopeUtil.isOuterMostType(ast)) {
                //     // don't check author/version for inner classes
                //     checkTag(lineNo, tags, JavadocTagInfo.AUTHOR.getName(),
                //              authorFormat);
                //     checkTag(lineNo, tags, JavadocTagInfo.VERSION.getName(),
                //              versionFormat);
                // }
                //
                // final List<String> typeParamNames =
                //     CheckUtil.getTypeParameterNames(ast);
                //
                // if (!allowMissingParamTags) {
                //     //Check type parameters that should exist, do
                //     for (final String typeParamName : typeParamNames) {
                //         checkTypeParamTag(
                //             lineNo, tags, typeParamName);
                //     }
                // }

            }
        }
    }

    // private List<JavadocTag> getJavadocTags(TextBlock textBlock) {
    //     final JavadocTags tags = JavadocUtil.getJavadocTags(textBlock,
    //                                                         JavadocUtil.JavadocTagType.BLOCK);
    //     for (final InvalidJavadocTag tag : tags.getInvalidTags()) {
    //         log(tag.getLine(), tag.getCol(), MSG_UNKNOWN_TAG,
    //             tag.getName());
    //     }
    //     return tags.getValidTags();
    // }

    /**
     * Verifies that a type definition has a required tag.
     *
     * @param lineNo        the line number for the type definition.
     * @param tags          tags from the Javadoc comment for the type definition.
     * @param tagName       the required tag name.
     * @param formatPattern regexp for the tag value.
     */
    private void checkTag(int lineNo, List<JavadocTag> tags, String tagName,
                          Pattern formatPattern) {
        // if (formatPattern != null) {
        //     boolean hasTag = false;
        //     final String tagPrefix = "@";
        //     for (int i = tags.size() - 1; i >= 0; i--) {
        //         final JavadocTag tag = tags.get(i);
        //         if (tag.getTagName().equals(tagName)) {
        //             hasTag = true;
        //             if (!formatPattern.matcher(tag.getFirstArg()).find()) {
        //                 log(lineNo, MSG_TAG_FORMAT, tagPrefix + tagName, formatPattern.pattern());
        //             }
        //         }
        //     }
        //     if (!hasTag) {
        //         log(lineNo, MSG_MISSING_TAG, tagPrefix + tagName);
        //     }
        // }
    }

    /**
     * Decides whether the variable name of an AST is in the ignore list.
     *
     * @param ast the AST to check
     * @return true if the variable name of ast is in the ignore list.
     */
    private boolean isIgnored(DetailAST ast) {
        final String name = ast.findFirstToken(TokenTypes.IDENT).getText();
        return ignoreNamePattern != null && ignoreNamePattern.matcher(name).matches()
            || "serialVersionUID".equals(name);
    }

    /**
     * Whether we should check this node.
     *
     * @param ast a given node.
     * @return whether we should check a given node.
     */
    private boolean shouldCheck(final DetailAST ast) {
        boolean result = false;
        if (!ScopeUtil.isInCodeBlock(ast) && !isIgnored(ast)) {
            Scope customScope = Scope.PUBLIC;
            if (ast.getType() != TokenTypes.ENUM_CONSTANT_DEF
                && !ScopeUtil.isInInterfaceOrAnnotationBlock(ast)) {
                final DetailAST mods = ast.findFirstToken(TokenTypes.MODIFIERS);
                customScope = ScopeUtil.getScopeFromMods(mods);
            }

            final Scope surroundingScope = ScopeUtil.getSurroundingScope(ast);
            result = customScope.isIn(scope) && surroundingScope.isIn(scope)
                && (excludeScope == null
                || !customScope.isIn(excludeScope)
                || !surroundingScope.isIn(excludeScope));
        }
        return result;
    }
}
