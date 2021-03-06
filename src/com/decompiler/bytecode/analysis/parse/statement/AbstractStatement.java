package com.decompiler.bytecode.analysis.parse.statement;

import java.util.List;
import java.util.Set;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import com.decompiler.bytecode.analysis.loc.HasByteCodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.parse.utils.*;
import com.decompiler.entities.exceptions.ExceptionCheck;
import com.decompiler.util.ConfusedDecompilerException;
import com.decompiler.util.output.Dumper;
import com.decompiler.util.output.ToStringDumper;

public abstract class AbstractStatement implements Statement {
    private BytecodeLoc loc;
    private StatementContainer<Statement> container;

    public AbstractStatement(BytecodeLoc loc) {
        this.loc = loc;
    }

    @Override
    public BytecodeLoc getLoc() {
        return loc;
    }

    @Override
    public void addLoc(HasByteCodeLoc loc) {
        if (loc.getLoc().isEmpty()) return;
        this.loc = BytecodeLocFactoryImpl.INSTANCE.combine(this, loc);
    }

    @Override
    public void setContainer(StatementContainer<Statement> container) {
        if (container == null) throw new ConfusedDecompilerException("Trying to setContainer null!");
        this.container = container;
    }

    @Override
    public Statement outerDeepClone(CloneHelper cloneHelper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LValue getCreatedLValue() {
        return null;
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
    }

    @Override
    public boolean doesBlackListLValueReplacement(LValue lValue, Expression expression) {
        return false;
    }

    @Override
    public void collectObjectCreation(CreationCollector creationCollector) {
    }

    @Override
    public SSAIdentifiers<LValue> collectLocallyMutatedVariables(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return new SSAIdentifiers<LValue>();
    }

    @Override
    public StatementContainer<Statement> getContainer() {
//        if (container == null) {
//            throw new ConfusedCFRException("Null container!");
//        }
        return container;
    }

    @Override
    public Expression getRValue() {
        return null;
    }

    protected Statement getTargetStatement(int idx) {
        return container.getTargetStatement(idx);
    }

    @Override
    public boolean isCompound() {
        return false;
    }

    @Override
    public List<Statement> getCompoundParts() {
        throw new ConfusedDecompilerException("Should not be calling getCompoundParts on this statement");
    }

    @Override
    public final String toString() {
        Dumper d = new ToStringDumper();
        d.print(getClass().getSimpleName()).print(": ").dump(this);
        return d.toString();
    }

    @Override
    public boolean fallsToNext() {
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return true;
    }

    @Override
    public Set<LValue> wantsLifetimeHint() {
        return null;
    }

    @Override
    public void setLifetimeHint(LValue lv, boolean usedInChildren) {
    }
}
