package com.decompiler.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.util.BoxingHelper;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.*;
import com.decompiler.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.structured.StructuredScope;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.types.RawJavaType;
import com.decompiler.bytecode.analysis.types.StackType;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;

public class TernaryCastCleaner extends AbstractExpressionRewriter implements StructuredStatementTransformer {

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    /*
     * A couple of silly transforms we have to apply to ternaries after the fact.
     */
    private static Expression applyTransforms(TernaryExpression t) {
        InferredJavaType inferredJavaType = t.getInferredJavaType();
        ConditionalExpression condition = t.getCondition();
        Expression lhs = t.getLhs();
        Expression rhs = t.getRhs();
        if (inferredJavaType.getJavaTypeInstance().getStackType() != StackType.REF) {
            // Clean up truly disgusting ternaries which involve incorrect cast of boolean to integer.
            // (Introduced in cast)
            if (condition instanceof BooleanExpression &&
                ((BooleanExpression) condition).getInner().getInferredJavaType().getJavaTypeInstance() != RawJavaType.BOOLEAN) {
                if (lhs == Literal.INT_ONE && rhs == Literal.INT_ZERO) {
                    BooleanExpression b = (BooleanExpression) condition;
                    return b.getInner();
                }
            }

            if (lhs instanceof Literal) {
                lhs = ((Literal) lhs).appropriatelyCasted(inferredJavaType);
                return new TernaryExpression(BytecodeLoc.TODO, inferredJavaType, condition, lhs, rhs);
            } else if (rhs instanceof Literal) {
                rhs = ((Literal) rhs).appropriatelyCasted(inferredJavaType);
                return new TernaryExpression(BytecodeLoc.TODO, inferredJavaType, condition, lhs, rhs);
            }
            return t;
        }
        // Ok - what if it *is* a ref.
        // Special (NASTY) case -
        // x = a ? (Number)boxedDouble : (Number)boxedInt
        // vs 
        // x = a ? boxedDouble : boxedInt
        // (These ARE different - see ternaryTest5b/c)
        if (BoxingHelper.isBoxedTypeInclNumber(lhs.getInferredJavaType().getJavaTypeInstance()) &&
            BoxingHelper.isBoxedTypeInclNumber(rhs.getInferredJavaType().getJavaTypeInstance()) &&
            !BoxingHelper.isBoxedType(t.getInferredJavaType().getJavaTypeInstance())) {
            InferredJavaType typ = t.getInferredJavaType();
            return new TernaryExpression(BytecodeLoc.TODO, t.getInferredJavaType(),
                    condition,
                    new CastExpression(BytecodeLoc.NONE, typ, lhs),
                    new CastExpression(BytecodeLoc.NONE, typ, rhs));
        }
        return t;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof TernaryExpression) {
            expression = applyTransforms((TernaryExpression)expression);
        }
        return expression;
    }
}
