package com.decompiler.bytecode.analysis.structured.statement;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.decompiler.bytecode.analysis.structured.StructuredScope;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.output.Dumper;

public class StructuredReturn extends AbstractStructuredStatement implements BoxingProcessor {

    /*
     * Note that this will be null if we're returning void.
     * If we're ACTUALLY returning null, this will be a null-expr.
     */
    private Expression value;
    private final JavaTypeInstance fnReturnType;

    public StructuredReturn(BytecodeLoc loc) {
        super(loc);
        this.value = null;
        this.fnReturnType = null;
    }

    public StructuredReturn(BytecodeLoc loc, Expression value, JavaTypeInstance fnReturnType) {
        super(loc);
        this.value = value;
        this.fnReturnType = fnReturnType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, value);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(fnReturnType);
        collector.collectFrom(value);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (value == null) {
            dumper.keyword("return").print(";");
        } else {
            dumper.keyword("return ").dump(value).print(";");
        }
        dumper.newln();
        return dumper;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (value != null) value.collectUsedLValues(scopeDiscoverer);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        if (value == null) return false;
        value = boxingRewriter.sugarNonParameterBoxing(value, fnReturnType);
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(this.getContainer());
        if (value != null) {
            value = expressionRewriter.rewriteExpression(value, null, this.getContainer(), null);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != StructuredReturn.class) return false;
        StructuredReturn other = (StructuredReturn)obj;
        if (value == null) {
            if (other.value != null) return false;
        } else {
            if (!value.equals(other.value)) return false;
        }

        if (fnReturnType == null) {
            if (other.fnReturnType != null) return false;
        } else {
            if (!fnReturnType.equals(other.fnReturnType)) return false;
        }
        return true;
    }

    @Override
    public boolean canFall() {
        return false;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredReturn)) return false;
        StructuredReturn other = (StructuredReturn) o;
        if (value == null) {
            if (other.value != null) return false;
        } else {
            if (!value.equals(other.value)) return false;
        }

        matchIterator.advance();
        return true;
    }
}
