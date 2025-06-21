package dev.dong4j.arco.maven.plugin.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验工程内是否有非法的new
 * <p>对于反射调用,暂时未实现</p>
 *
 * @author dong4j
 * @version 1.0.3
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.02.28 16:14
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class IllegalNewCheck extends AbstractCheck {
    /** Pkg name */
    private String pkgName;
    /** File name */
    private String fileName;
    /** Un check package */
    private final List<String> unCheckPackage = new ArrayList<>();
    /** Illegal classes */
    private final List<String> illegalClasses = new ArrayList<>();
    /** Imports */
    private final List<FullIdent> imports = new ArrayList<>();
    /** Class names */
    private final List<String> classNames = new ArrayList<>();
    /** New class list */
    private final List<DetailAST> newClassList = new ArrayList<>();
    /** Err count */
    private int errCount = 0;

    /**
     * checkstyle配置中的属性设置
     *
     * @param names names
     * @since 1.0.0
     */
    public void setClasses(@NotNull String... names) {
        for (String name : names) {
            illegalClasses.add(name);
            System.out.println("不允许实例化的类: " + name);
        }
    }

    /**
     * Sets unpackage *
     *
     * @param unpackages unpackages
     * @since 1.0.0
     */
    public void setUnpackage(@NotNull String... unpackages) {
        for (String unpackage : unpackages) {
            unCheckPackage.add(unpackage);
            System.out.println("不需要校验的包: " + unpackage);
        }
    }

    /**
     * 本校验器默认接收的ast类型
     *
     * @return the int [ ]
     * @since 1.0.0
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            TokenTypes.IMPORT,
            TokenTypes.LITERAL_NEW,
            TokenTypes.PACKAGE_DEF,
            TokenTypes.CLASS_DEF,
        };
    }

    /**
     * Get acceptable tokens int [ ]
     *
     * @return the int [ ]
     * @since 1.0.0
     */
    @Override
    public int[] getAcceptableTokens() {
        return new int[0];
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
     * 一个class开始解析
     *
     * @param rootAST root ast
     * @since 1.0.0
     */
    @Override
    public void beginTree(DetailAST rootAST) {
        super.beginTree(rootAST);
        pkgName = null;
        fileName = null;
        imports.clear();
        newClassList.clear();
        classNames.clear();

        FileContents fileContents = getFileContents();
        fileName = fileContents.getFileName();
    }

    /**
     * 一个class结束解析
     *
     * @param rootAST root ast
     * @since 1.0.0
     */
    @Override
    public void finishTree(DetailAST rootAST) {
        super.finishTree(rootAST);

        // 如果当前校验的class在放过的列表中,则不进行校验
        if (unCheckPackage.contains(pkgName)) {
            return;
        }

        for (DetailAST ast : newClassList) {
            if (!postProcessLiteralNew(ast)) {
                errCount++;
            }
        }
    }

    /**
     * Destroy
     *
     * @since 1.0.0
     */
    @Override
    public void destroy() {
        super.destroy();
        if (errCount > 0) {
            throw new RuntimeException("禁止编译,共有[" + errCount + "]个非法远程调用");
        }
    }

    /**
     * Visit token *
     *
     * @param ast ast
     * @since 1.0.0
     */
    @Override
    public void visitToken(@NotNull DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.LITERAL_NEW:
                processLiteralNew(ast);
                break;
            case TokenTypes.PACKAGE_DEF:
                processPackageDef(ast);
                break;
            case TokenTypes.IMPORT:
                processImport(ast);
                break;
            case TokenTypes.CLASS_DEF:
                processClassDef(ast);
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + ast);
        }
    }

    /**
     * 执行new关键字的鉴别
     *
     * @param ast ast
     * @since 1.0.0
     */
    private void processLiteralNew(@NotNull DetailAST ast) {
        if (ast.getParent().getType() != TokenTypes.METHOD_REF) {
            newClassList.add(ast);
        }
    }

    /**
     * 执行包名树的鉴别
     *
     * @param ast ast
     * @since 1.0.0
     */
    private void processPackageDef(@NotNull DetailAST ast) {
        DetailAST packageNameAST = ast.getLastChild()
            .getPreviousSibling();
        FullIdent packageIdent =
            FullIdent.createFullIdent(packageNameAST);
        pkgName = packageIdent.getText();
    }

    /**
     * 执行导入包树的鉴别
     *
     * @param ast ast
     * @since 1.0.0
     */
    private void processImport(DetailAST ast) {
        FullIdent name = FullIdent.createFullIdentBelow(ast);
        imports.add(name);
    }

    /**
     * 执行class的树鉴别
     *
     * @param ast ast
     * @since 1.0.0
     */
    private void processClassDef(@NotNull DetailAST ast) {
        DetailAST identToken = ast.findFirstToken(TokenTypes.IDENT);
        String className = identToken.getText();
        classNames.add(className);
    }

    /**
     * 执行非法实例化校验
     *
     * @param newTokenAst new token ast
     * @return the boolean
     * @since 1.0.0
     */
    private boolean postProcessLiteralNew(@NotNull DetailAST newTokenAst) {
        boolean flag = true;
        DetailAST typeNameAst = newTokenAst.getFirstChild();
        DetailAST nameSibling = typeNameAst.getNextSibling();
        if (nameSibling.getType() != TokenTypes.ARRAY_DECLARATOR) {
            // 不是new A[];这种类型的
            FullIdent typeIdent = FullIdent.createFullIdent(typeNameAst);
            String typeName = typeIdent.getText();
            int lineNo = newTokenAst.getLineNo();
            String fqClassName = getIllegalInstantiation(typeName);
            if (fqClassName != null) {
                System.err.println("非法远程调用,错误文件[" + fileName + "],行号[" + lineNo + "]");
                log(lineNo, "非法远程调用");
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 获取非法实例化得类名
     *
     * @param className class name
     * @return illegal instantiation
     * @since 1.0.0
     */
    private String getIllegalInstantiation(String className) {
        String fullClassName = null;

        if (illegalClasses.contains(className)) {
            // new com.xxx.xxx.xx.A() 匹配全包名实例化
            fullClassName = className;
        } else {
            int pkgNameLen;

            if (pkgName == null) {
                pkgNameLen = 0;
            } else {
                pkgNameLen = pkgName.length();
            }

            for (String illegal : illegalClasses) {
                if (isSamePackage(className, pkgNameLen, illegal)) {
                    fullClassName = illegal;
                } else {
                    fullClassName = checkImportStatements(className);
                }

                if (fullClassName != null) {
                    break;
                }
            }
        }
        return fullClassName;
    }

    /**
     * 处理同一个包下的new
     *
     * @param className  class name
     * @param pkgNameLen pkg name len
     * @param illegal    illegal
     * @return boolean boolean
     * @since 1.0.0
     */
    private boolean isSamePackage(String className, int pkgNameLen, String illegal) {
        return pkgName != null
            && className.length() == illegal.length() - pkgNameLen - 1
            && illegal.charAt(pkgNameLen) == '.'
            && illegal.endsWith(className)
            && illegal.startsWith(pkgName);
    }

    /**
     * new出来的实例与导入的包进行匹配
     *
     * @param className class name
     * @return string string
     * @since 1.0.0
     */
    private String checkImportStatements(String className) {
        String illegalType = null;
        for (FullIdent importLineText : imports) {
            String importArg = importLineText.getText();
            if (importArg.endsWith(".*")) {
                importArg = importArg.substring(0, importArg.length() - 1)
                    + className;
            }
            if (CommonUtil.baseClassName(importArg).equals(className)
                && illegalClasses.contains(importArg)) {
                illegalType = importArg;
                break;
            }
        }
        return illegalType;
    }
}
