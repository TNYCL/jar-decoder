package com.decompiler.bytecode.analysis.structured.statement;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.lvalue.LocalVariable;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.utils.BlockIdentifier;
import com.decompiler.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.decompiler.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.collections.ListFactory;
import com.decompiler.util.output.Dumper;

public class StructuredIter extends AbstractStructuredBlockStatement {
    private final BlockIdentifier block;
    private LValue iterator;
    private Expression list;

    StructuredIter(BytecodeLoc loc, BlockIdentifier block, LValue iterator, Expression list, Op04StructuredStatement body) {
        super(loc, body);
        this.block = block;
        this.iterator = iterator;
        this.list = list;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        iterator.collectTypeUsages(collector);
        list.collectTypeUsages(collector);
        super.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, list);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (block.hasForeignReferences()) dumper.label(block.getName(), true);
        dumper.keyword("for ").separator("(");
        if (iterator.isFinal()) dumper.keyword("final ");
        LValue.Creation.dump(dumper, iterator).separator(" : ").dump(list).separator(") ");
        getBody().dump(dumper);
        return dumper;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        getBody().linearizeStatementsInto(out);
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return block;
    }


    @Override
    public boolean supportsContinueBreak() {
        return true;
    }

    @Override
    public boolean supportsBreak() {
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        // While it's not strictly speaking 2 blocks, we can model it as the statement / definition
        // section of the for as being an enclosing block.  (otherwise we add the variable in the wrong scope).
        scopeDiscoverer.enterBlock(this);
        list.collectUsedLValues(scopeDiscoverer);
        iterator.collectLValueAssignments(null, this.getContainer(), scopeDiscoverer);
        scopeDiscoverer.processOp04Statement(getBody());
        scopeDiscoverer.leaveBlock(this);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        // we're always creator.  But we could verify additionally.
    }

    @Override
    public boolean alwaysDefines(LValue scopedEntity) {
        if (scopedEntity == null) return false;
        return scopedEntity.equals(iterator);
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        if (scopedEntity == null) return false;
        return scopedEntity.equals(iterator);
    }

    @Override
    public List<LValue> findCreatedHere() {
        if (!(iterator instanceof LocalVariable)) return null;
        return ListFactory.newImmutableList(iterator);
    }


    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        iterator = expressionRewriter.rewriteExpression(iterator, null, this.getContainer(), null);
        list = expressionRewriter.rewriteExpression(list, null, this.getContainer(), null);
    }

}
