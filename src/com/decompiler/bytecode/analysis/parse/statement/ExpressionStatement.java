package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.decompiler.entities.exceptions.ExceptionCheck;
import com.decompiler.util.output.Dumper;

public class ExpressionStatement extends AbstractStatement {
    private Expression expression;

    public ExpressionStatement(Expression expression) {
        super(expression.getLoc());
        this.expression = expression;
    }

    @Override
    public Dumper dump(Dumper d) {
        return expression.dump(d).endCodeln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, expression);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        expression = expression.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        expression = expressionRewriter.rewriteExpression(expression, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new ExpressionStatement(cloneHelper.replaceOrClone(expression));
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        expression.collectUsedLValues(lValueUsageCollector);
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredExpressionStatement(getLoc(), expression, false);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ExpressionStatement)) return false;
        ExpressionStatement other = (ExpressionStatement) o;
        return expression.equals(other.expression);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return expression.canThrow(caught);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ExpressionStatement)) return false;
        ExpressionStatement other = (ExpressionStatement) o;
        return constraint.equivalent(expression, other.expression);
    }
}
