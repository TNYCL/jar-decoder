package com.decompiler.bytecode.analysis.parse.expression;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.RawJavaType;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.StringUtils;
import com.decompiler.util.collections.ListFactory;
import com.decompiler.util.output.Dumper;

/**
 * 1d array only.
 */
public class NewAnonymousArray extends AbstractNewArray implements BoxingProcessor {
    private JavaTypeInstance allocatedType;
    private int numDims;
    private List<Expression> values;
    private boolean isCompletelyAnonymous;

    public NewAnonymousArray(BytecodeLoc loc, InferredJavaType type, int numDims, List<Expression> values, boolean isCompletelyAnonymous) {
        super(loc, type);
        this.values = ListFactory.newList();
        this.numDims = numDims;
        this.allocatedType = type.getJavaTypeInstance().getArrayStrippedType();
        if (allocatedType instanceof RawJavaType) {
            for (Expression value : values) {
                value.getInferredJavaType().useAsWithoutCasting(allocatedType);
            }
        }
        // This is only true if the target array has the correct arity.
        // See ArrayTest18.
        for (Expression value : values) {
            if (numDims > 1) {
                if (value instanceof NewAnonymousArray) {
                    NewAnonymousArray newAnonymousArrayInner = (NewAnonymousArray) value;
                    newAnonymousArrayInner.isCompletelyAnonymous = true;
                }
            }
            this.values.add(value);
        }
        this.isCompletelyAnonymous = isCompletelyAnonymous;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, values);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(allocatedType);
        collector.collectFrom(values);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        for (int i = 0; i < values.size(); ++i) {
            values.set(i, boxingRewriter.sugarNonParameterBoxing(values.get(i), allocatedType));
        }
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new NewAnonymousArray(getLoc(), getInferredJavaType(), numDims, cloneHelper.replaceOrClone(values), isCompletelyAnonymous);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (!isCompletelyAnonymous) {
            d.keyword("new ").dump(allocatedType);
            for (int x = 0; x < numDims; ++x) d.print("[]");
        }
        d.separator("{");
        boolean first = true;
        for (Expression value : values) {
            first = StringUtils.comma(first, d);
            d.dump(value);
        }
        d.separator("}");
        return d;
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, values);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(values, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(values, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression value : values) {
            value.collectUsedLValues(lValueUsageCollector);
        }
    }


    @Override
    public int getNumDims() {
        return numDims;
    }

    @Override
    public int getNumSizedDims() {
        return 0;
    }

    @Override
    public Expression getDimSize(int dim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewAnonymousArray that = (NewAnonymousArray) o;

        if (isCompletelyAnonymous != that.isCompletelyAnonymous) return false;
        if (numDims != that.numDims) return false;
        if (allocatedType != null ? !allocatedType.equals(that.allocatedType) : that.allocatedType != null)
            return false;
        if (values != null ? !values.equals(that.values) : that.values != null) return false;

        return true;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        NewAnonymousArray other = (NewAnonymousArray) o;

        if (isCompletelyAnonymous != other.isCompletelyAnonymous) return false;
        if (numDims != other.numDims) return false;
        if (!constraint.equivalent(allocatedType, other.allocatedType)) return false;
        if (!constraint.equivalent(values, other.values)) return false;
        return true;
    }
}
