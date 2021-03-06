package com.decompiler.bytecode.analysis.parse.expression;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.RawJavaType;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.ConfusedDecompilerException;
import com.decompiler.util.output.Dumper;

public class NewObjectArray extends AbstractNewArray {
    private List<Expression> dimSizes;
    private final JavaTypeInstance allocatedType;
    private final JavaTypeInstance resultType;
    private final int numDims;

    public NewObjectArray(BytecodeLoc loc, List<Expression> dimSizes, JavaTypeInstance resultInstance) {
        super(loc, new InferredJavaType(resultInstance, InferredJavaType.Source.EXPRESSION, true));
        this.dimSizes = dimSizes;
        this.allocatedType = resultInstance.getArrayStrippedType();
        this.resultType = resultInstance;
        this.numDims = resultInstance.getNumArrayDimensions();
        for (Expression size : dimSizes) {
            size.getInferredJavaType().useAsWithoutCasting(RawJavaType.INT);
        }
    }

    private NewObjectArray(BytecodeLoc loc, InferredJavaType inferredJavaType, JavaTypeInstance resultType, int numDims, JavaTypeInstance allocatedType, List<Expression> dimSizes) {
        super(loc, inferredJavaType);
        this.resultType = resultType;
        this.numDims = numDims;
        this.allocatedType = allocatedType;
        this.dimSizes = dimSizes;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, dimSizes);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new NewObjectArray(getLoc(), getInferredJavaType(), resultType, numDims, allocatedType, cloneHelper.replaceOrClone(dimSizes));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(allocatedType);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("new ").dump(allocatedType);
        for (Expression dimSize : dimSizes) {
            d.separator("[").dump(dimSize).separator("]");
        }
        for (int x = dimSizes.size(); x < numDims; ++x) {
            d.separator("[]");
        }
        return d;
    }

    @Override
    public int getNumDims() {
        return numDims;
    }

    @Override
    public int getNumSizedDims() {
        return dimSizes.size();
    }

    @Override
    public Expression getDimSize(int dim) {
        if (dim >= dimSizes.size()) throw new ConfusedDecompilerException("Out of bounds");
        return dimSizes.get(dim);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, dimSizes);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression dimSize : dimSizes) {
            dimSize.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewObjectArray that = (NewObjectArray) o;

        if (numDims != that.numDims) return false;
        if (allocatedType != null ? !allocatedType.equals(that.allocatedType) : that.allocatedType != null)
            return false;
        if (dimSizes != null ? !dimSizes.equals(that.dimSizes) : that.dimSizes != null) return false;
        if (resultType != null ? !resultType.equals(that.resultType) : that.resultType != null) return false;

        return true;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        NewObjectArray other = (NewObjectArray) o;
        if (numDims != other.numDims) return false;
        if (!constraint.equivalent(dimSizes, other.dimSizes)) return false;
        if (!constraint.equivalent(allocatedType, other.allocatedType)) return false;
        if (!constraint.equivalent(resultType, other.resultType)) return false;
        return true;
    }


}
