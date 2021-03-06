package com.decompiler.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Map;

import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import com.decompiler.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import com.decompiler.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.types.FormalTypeParameter;
import com.decompiler.bytecode.analysis.types.JavaGenericBaseInstance;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;
import com.decompiler.entities.constantpool.ConstantPool;

public class IllegalGenericRewriter extends AbstractExpressionRewriter {
    private final ConstantPool cp;
    private final Map<String, FormalTypeParameter> formalParams;

    public IllegalGenericRewriter(ConstantPool cp, Map<String, FormalTypeParameter> formalParams) {
        this.cp = cp;
        this.formalParams = formalParams;
    }

    private boolean hasIllegalGenerics(JavaTypeInstance javaTypeInstance, boolean constructor) {
        if (!(javaTypeInstance instanceof JavaGenericBaseInstance)) return false;
        JavaGenericBaseInstance genericBaseInstance = (JavaGenericBaseInstance) javaTypeInstance;
        return genericBaseInstance.hasForeignUnbound(cp, 0, constructor, formalParams);
    }

    private void maybeRewriteExpressionType(InferredJavaType inferredJavaType, boolean constructor) {
        JavaTypeInstance javaTypeInstance = inferredJavaType.getJavaTypeInstance();
        if (hasIllegalGenerics(javaTypeInstance, constructor)) {
            JavaTypeInstance deGenerified = javaTypeInstance.getDeGenerifiedType();
            inferredJavaType.deGenerify(deGenerified);
        }
    }

    private void maybeRewriteExplicitCallTyping(AbstractFunctionInvokation abstractFunctionInvokation) {
        List<JavaTypeInstance> list = abstractFunctionInvokation.getExplicitGenerics();
        if (list == null) return;
        for (JavaTypeInstance type : list) {
            if (hasIllegalGenerics(type, false)) {
                abstractFunctionInvokation.setExplicitGenerics(null);
                return;
            }
        }
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof AbstractFunctionInvokation) {
            maybeRewriteExplicitCallTyping((AbstractFunctionInvokation)expression);
        }
        maybeRewriteExpressionType(expression.getInferredJavaType(), expression instanceof AbstractConstructorInvokation);
        return expression;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        maybeRewriteExpressionType(lValue.getInferredJavaType(), false);
        return lValue;
    }
}
