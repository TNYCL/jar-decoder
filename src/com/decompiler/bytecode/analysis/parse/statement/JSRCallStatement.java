package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.StructuredComment;
import com.decompiler.util.output.Dumper;

public class JSRCallStatement extends AbstractStatement {

    public JSRCallStatement(BytecodeLoc loc) {
        super(loc);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new JSRCallStatement(getLoc());
    }

    /*
     * Obviously, we don't want to be emitting this in any language - these should have been transformed away before
     * the final output.
     */
    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("CALL " + getTargetStatement(0).getContainer().getLabel() + ";").newln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredComment("JSR Call");
    }


    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }
}
