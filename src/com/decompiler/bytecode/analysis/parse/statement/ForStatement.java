package com.decompiler.bytecode.analysis.parse.statement;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import com.decompiler.bytecode.analysis.parse.expression.ConditionalExpression;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.UnstructuredFor;
import com.decompiler.util.StringUtils;
import com.decompiler.util.collections.ListFactory;
import com.decompiler.util.output.Dumper;

public class ForStatement extends AbstractStatement {
    private ConditionalExpression condition;
    private BlockIdentifier blockIdentifier;
    private AssignmentSimple initial;
    private List<AbstractAssignmentExpression> assignments;

    ForStatement(BytecodeLoc loc, ConditionalExpression conditionalExpression, BlockIdentifier blockIdentifier, AssignmentSimple initial, List<AbstractAssignmentExpression> assignments) {
        super(loc);
        this.condition = conditionalExpression;
        this.blockIdentifier = blockIdentifier;
        this.initial = initial;
        this.assignments = assignments;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        List<AbstractAssignmentExpression> assigns = ListFactory.newList();
        for (AbstractAssignmentExpression ae : assignments) {
            assigns.add((AbstractAssignmentExpression)cloneHelper.replaceOrClone(ae));
        }
        return new ForStatement(getLoc(), (ConditionalExpression)cloneHelper.replaceOrClone(condition), blockIdentifier, (AssignmentSimple)initial.deepClone(cloneHelper), assigns);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, assignments, condition, initial);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("for ").separator("(");
        if (initial != null) dumper.dump(initial);
        dumper.print("; ").dump(condition).print("; ");
        boolean first = true;
        for (AbstractAssignmentExpression assignment : assignments) {
            first = StringUtils.comma(first, dumper);
            dumper.dump(assignment);
        }
        dumper.separator(") ");
        dumper.comment(" // ends " + getTargetStatement(1).getContainer().getLabel() + ";").newln();
        return dumper;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        for (AbstractAssignmentExpression assignment : assignments) {
            assignment.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
        }
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        condition = expressionRewriter.rewriteExpression(condition, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
        for (int i = 0, len = assignments.size(); i < len; ++i) {
            assignments.set(i, (AbstractAssignmentExpression) expressionRewriter.rewriteExpression(assignments.get(i), ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE));
        }
    }


    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        condition.collectUsedLValues(lValueUsageCollector);
        for (AbstractAssignmentExpression assignment : assignments) {
            assignment.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredFor(getLoc(), condition, blockIdentifier, initial, assignments);
    }

    public BlockIdentifier getBlockIdentifier() {
        return blockIdentifier;
    }

    public ConditionalExpression getCondition() {
        return condition;
    }

    public AssignmentSimple getInitial() {
        return initial;
    }

    public List<AbstractAssignmentExpression> getAssignments() {
        return assignments;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        ForStatement other = (ForStatement) o;
        if (!constraint.equivalent(condition, other.condition)) return false;
        if (!constraint.equivalent(initial, other.initial)) return false;
        if (!constraint.equivalent(assignments, other.assignments)) return false;
        return true;
    }

}
