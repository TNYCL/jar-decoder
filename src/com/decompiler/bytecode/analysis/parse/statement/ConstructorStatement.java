package com.decompiler.bytecode.analysis.parse.statement;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.decompiler.util.MiscConstants;
import com.decompiler.util.output.Dumper;

/**
 * This is a temporary statement - it should be replaced with an Assignment of a ConstructorInvokation
 * However, it can force the type of the constructed object, which NEW is not capable of doing....
 */
public class ConstructorStatement extends AbstractStatement {
    private MemberFunctionInvokation invokation;

    public ConstructorStatement(BytecodeLoc loc, MemberFunctionInvokation construction) {
        super(loc);
        this.invokation = construction;
        Expression object = invokation.getObject();
        object.getInferredJavaType().chain(invokation.getInferredJavaType());
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print(MiscConstants.INIT_METHOD).dump(invokation).endCodeln();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new ConstructorStatement(getLoc(), (MemberFunctionInvokation)cloneHelper.replaceOrClone(invokation));
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, invokation);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        // can't ever change, but its arguments can.
        invokation.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        invokation.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        invokation.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public void collectObjectCreation(CreationCollector creationCollector) {
        Expression object = invokation.getObject();
        creationCollector.collectConstruction(object, invokation, this.getContainer());
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredExpressionStatement(getLoc(), invokation, false);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        ConstructorStatement other = (ConstructorStatement) o;
        if (!constraint.equivalent(invokation, other.invokation)) return false;
        return true;
    }

}
