package com.decompiler.bytecode.analysis.opgraph.op3rewriters;

import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.ArithmeticOperation;
import com.decompiler.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.RawJavaType;
import com.decompiler.bytecode.analysis.types.StackType;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;

/*
 * There are some circumstances where we simply can't tell if something is a boolean at the point
 * it's created. (unless relying on type metadata, which is a no no!!)
 *
 * true ^ true === 1 ^ 1
 *
 * We don't try and turn things INTO booleans at this stage, as we are early in the pipeline, and
 * we are likely still dealing with misclassified bools.
 */
public class BadBoolAssignmentRewriter extends AbstractExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof ArithmeticOperation) {
            ArithmeticOperation op = (ArithmeticOperation)expression;
            JavaTypeInstance resType = op.getInferredJavaType().getJavaTypeInstance();
            RawJavaType rawRes = resType.getRawTypeOfSimpleType();
            if (resType.getStackType() == StackType.INT && resType != RawJavaType.BOOLEAN) {
                InferredJavaType l = op.getLhs().getInferredJavaType();
                InferredJavaType r = op.getRhs().getInferredJavaType();
                if (l.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                    l.useInArithOp(r, rawRes, true);
                }
                if (r.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                    r.useInArithOp(l, rawRes, true);
                }
            }
        }
        return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
    }
}
