package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.expression.ConditionalExpression;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.*;
import com.decompiler.entities.exceptions.ExceptionCheck;
import com.decompiler.util.output.Dumper;

public class IfExitingStatement extends AbstractStatement {

    private ConditionalExpression condition;
    private Statement statement;

    public IfExitingStatement(BytecodeLoc loc, ConditionalExpression conditionalExpression, Statement statement) {
        super(loc);
        this.condition = conditionalExpression;
        this.statement = statement;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("if ").separator("(").dump(condition).separator(") ");
        statement.dump(dumper);
        return dumper;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new IfExitingStatement(getLoc(), (ConditionalExpression)cloneHelper.replaceOrClone(condition), statement.deepClone(cloneHelper));
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, condition, statement);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        Expression replacementCondition = condition.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
        if (replacementCondition != condition) {
            this.condition = (ConditionalExpression) replacementCondition;
        }
        statement.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        condition = expressionRewriter.rewriteExpression(condition, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
        statement.rewriteExpressions(expressionRewriter, ssaIdentifiers);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        condition.collectUsedLValues(lValueUsageCollector);
        statement.collectLValueUsage(lValueUsageCollector);
    }

    public ConditionalExpression getCondition() {
        return condition;
    }

    public Statement getExitStatement() {
        return statement;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredIf(getLoc(), condition, new Op04StructuredStatement(Block.getBlockFor(false, statement.getStructuredStatement())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IfExitingStatement that = (IfExitingStatement) o;

        if (condition != null ? !condition.equals(that.condition) : that.condition != null) return false;
        if (!statement.equals(that.statement)) return false;

        return true;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        IfExitingStatement other = (IfExitingStatement) o;
        if (!constraint.equivalent(condition, other.condition)) return false;
        if (!constraint.equivalent(statement, other.statement)) return false;
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return condition.canThrow(caught) || statement.canThrow(caught);
    }
}
