package com.decompiler.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Set;

import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.ConditionalExpression;
import com.decompiler.bytecode.analysis.parse.lvalue.StackSSALabel;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.BlockIdentifier;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.structured.StructuredScope;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.Block;
import com.decompiler.bytecode.analysis.structured.statement.StructuredBreak;
import com.decompiler.bytecode.analysis.structured.statement.StructuredComment;
import com.decompiler.bytecode.analysis.structured.statement.StructuredContinue;

public class ControlFlowCleaningTransformer implements StructuredStatementTransformer, ExpressionRewriter {

    public ControlFlowCleaningTransformer() {
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {

        if (scope.get(0) instanceof Block) {
            if (in instanceof StructuredBreak) {
                StructuredBreak inb = (StructuredBreak) in;
                Set<Op04StructuredStatement> falls = scope.getNextFallThrough(in);
                /*
                 * If one of these is also a break, AND all of these have the same target as this, this is redundant.
                 * (we check that all of them are in the scope).
                 */
                for (Op04StructuredStatement fall : falls) {
                    StructuredStatement stm = fall.getStatement();
                    if (stm instanceof StructuredBreak) {
                        StructuredBreak stmb = (StructuredBreak) stm;
                        if (stmb.getBreakBlock() == inb.getBreakBlock()) {
                            return StructuredComment.EMPTY_COMMENT;
                        }
                    }
                }
                return in;
            }
            if (in instanceof StructuredContinue) {
                StructuredContinue cont = (StructuredContinue) in;

                Set<Op04StructuredStatement> falls = scope.getNextFallThrough(in);
                for (Op04StructuredStatement fall : falls) {
                    StructuredStatement stm = fall.getStatement();
                    if (stm instanceof StructuredContinue) {
                        StructuredContinue stmb = (StructuredContinue) stm;
                        if (stmb.getContinueTgt() == cont.getContinueTgt()) {
                            return StructuredComment.EMPTY_COMMENT;
                        }
                    }
                    if (stm instanceof StructuredComment) {
                        continue;
                    }
                    return in;
                }

                BlockIdentifier block = scope.getContinueBlock();
                if (block == cont.getContinueTgt()) {
                    return StructuredComment.EMPTY_COMMENT;
                }

                return in;
            }
        }

        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

}
