package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.decompiler.bytecode.analysis.parse.utils.LValueRewriter;
import com.decompiler.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.StructuredThrow;
import com.decompiler.entities.exceptions.ExceptionCheck;
import com.decompiler.util.output.Dumper;

public class ThrowStatement extends ReturnStatement {
    private Expression rvalue;

    public ThrowStatement(BytecodeLoc loc, Expression rvalue) {
        super(loc);
        this.rvalue = rvalue;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, rvalue);
    }

    @Override
    public ReturnStatement deepClone(CloneHelper cloneHelper) {
        return new ThrowStatement(getLoc(), cloneHelper.replaceOrClone(rvalue));
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("throw ").dump(rvalue).endCodeln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = rvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        rvalue = expressionRewriter.rewriteExpression(rvalue, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        rvalue.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredThrow(getLoc(), rvalue);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ThrowStatement)) return false;
        ThrowStatement other = (ThrowStatement) o;
        return rvalue.equals(other.rvalue);
    }

    // HAHAHAHAH
    @Override
    public boolean canThrow(ExceptionCheck caught) {
        // Oh good grief.  this is going to be a massive pain.  What could this type be throwing?
        // For now, let's handle the simple case where it's throwing a new exception.
        return caught.checkAgainstException(rvalue);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        ThrowStatement other = (ThrowStatement) o;
        if (!constraint.equivalent(rvalue, other.rvalue)) return false;
        return true;
    }

}
