package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.StructuredComment;
import com.decompiler.util.output.Dumper;

public class JSRRetStatement extends AbstractStatement {
    private Expression ret;

    public JSRRetStatement(BytecodeLoc loc, Expression ret) {
        super(loc);
        this.ret = ret;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, ret);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("Ret");
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new JSRRetStatement(getLoc(), cloneHelper.replaceOrClone(ret));
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        ret = ret.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        ret = expressionRewriter.rewriteExpression(ret, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        ret.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredComment("JSR Ret");
    }


    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        JSRRetStatement other = (JSRRetStatement) o;
        if (!constraint.equivalent(ret, other.ret)) return false;
        return true;
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }
}
