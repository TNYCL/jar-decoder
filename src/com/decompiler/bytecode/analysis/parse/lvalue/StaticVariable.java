package com.decompiler.bytecode.analysis.parse.lvalue;

import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.misc.Precedence;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;
import com.decompiler.entities.ClassFile;
import com.decompiler.entities.ClassFileField;
import com.decompiler.entities.constantpool.*;
import com.decompiler.entities.exceptions.ExceptionCheck;
import com.decompiler.util.output.Dumper;
import com.decompiler.util.output.TypeContext;

public class StaticVariable extends AbstractFieldVariable {

    private final boolean knownSimple;

    public StaticVariable(ConstantPoolEntry field) {
        super(field);
        this.knownSimple = false;
    }

    /*
     * Used only for matching
     */
    public StaticVariable(InferredJavaType type, JavaTypeInstance clazz, String varName) {
        super(type, clazz, varName);
        this.knownSimple = false;
    }

    public StaticVariable(ClassFile classFile, ClassFileField classFileField, boolean local) {
        super(new InferredJavaType(classFileField.getField().getJavaTypeInstance(), InferredJavaType.Source.FIELD, true), classFile.getClassType(), classFileField);
        this.knownSimple = local;
    }

    private StaticVariable(StaticVariable other, boolean knownSimple) {
        super(other);
        this.knownSimple = knownSimple;
    }

    /*
     * There are some circumstances (final assignment) where it's illegal to use the FQN of a static.
     */
    public StaticVariable getSimpleCopy() {
        if (knownSimple) return this;
        return new StaticVariable(this, true);
    }

    public StaticVariable getNonSimpleCopy() {
        if (!knownSimple) return this;
        return new StaticVariable(this, false);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (knownSimple) {
            return d.fieldName(getFieldName(), getOwningClassType(), false, true, false);
        } else {
            return d.dump(getOwningClassType(), TypeContext.Static).separator(".").fieldName(getFieldName(), getOwningClassType(), false, true, false);
        }
    }

    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof StaticVariable)) return false;
        if (!super.equals(o)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
