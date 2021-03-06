package com.decompiler.entities;

import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.expression.Literal;
import com.decompiler.bytecode.analysis.parse.literal.TypedLiteral;
import com.decompiler.bytecode.analysis.parse.rewriters.LiteralRewriter;
import com.decompiler.util.output.Dumper;

public class ClassFileField {
    private final Field field;
    /*
     * Because we might lift this, we split it out from the field.
     */
    private Expression initialValue;
    private boolean isHidden;
    private boolean isSyntheticOuterRef;
    // Should use NamedVariable?
    private String overriddenName;

    public ClassFileField(Field field, LiteralRewriter literalRewriter) {
        this.field = field;
        TypedLiteral constantValue = field.getConstantValue();
        // TODO : Rewrite literals selectively based on flags.
        initialValue = constantValue == null ?
                null :
                literalRewriter.rewriteExpression(new Literal(constantValue), null, null, null);
        isHidden = false;
        isSyntheticOuterRef = false;
    }

    public Field getField() {
        return field;
    }

    public Expression getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Expression rValue) {
        this.initialValue = rValue;
    }

    public boolean shouldNotDisplay() {
        return isHidden || isSyntheticOuterRef;
    }

    public boolean isSyntheticOuterRef() {
        return isSyntheticOuterRef;
    }

    public void markHidden() {
        isHidden = true;
    }

    public void markSyntheticOuterRef() {
        isSyntheticOuterRef = true;
    }

    // This should be used only for local tidying - it will not rename referents.
    public void overrideName(String override) {
        overriddenName = override;
    }

    public String getRawFieldName() {
        return field.getFieldName();
    }

    public String getFieldName() {
        if (overriddenName != null) return overriddenName;
        return getRawFieldName();
    }

    public void dump(Dumper d, ClassFile owner) {
        field.dump(d, getFieldName(), owner,false);
        if (initialValue != null) {
            d.operator(" = ").dump(initialValue);
        }
        d.endCodeln();
    }

    public void dumpAsRecord(Dumper d, ClassFile owner) {
        field.dump(d, getFieldName(), owner, true);
        // No, we can't have initial values on records.
        // I'm leaving this in just in case one exists!
        if (initialValue != null) {
            d.operator(" = ").dump(initialValue);
        }
    }

}
