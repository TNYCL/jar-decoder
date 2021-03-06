package com.decompiler.bytecode.analysis.parse.expression;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.expression.misc.Precedence;
import com.decompiler.bytecode.analysis.parse.rewriters.CloneHelper;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.MethodPrototype;
import com.decompiler.entities.classfilehelpers.OverloadMethodSet;
import com.decompiler.entities.constantpool.ConstantPool;
import com.decompiler.entities.constantpool.ConstantPoolEntryMethodRef;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.MiscConstants;
import com.decompiler.util.output.Dumper;

public class SuperFunctionInvokation extends AbstractMemberFunctionInvokation {
    private final boolean isOnInterface;
    private final JavaTypeInstance typeName;

    public SuperFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, List<Expression> args, List<Boolean> nulls, boolean isOnInterface) {
        super(loc, cp, function, object, args, nulls);
        this.isOnInterface = isOnInterface;
        this.typeName = null;
    }

    private SuperFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, List<Expression> args, List<Boolean> nulls, boolean isOnInterface, JavaTypeInstance name) {
        super(loc, cp, function, object, args, nulls);
        this.isOnInterface = isOnInterface;
        this.typeName = name;
    }

    public SuperFunctionInvokation withCustomName(JavaTypeInstance name) {
        return new SuperFunctionInvokation(getLoc(), getCp(), getFunction(), getObject(), getArgs(), getNulls(), isOnInterface, name);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new SuperFunctionInvokation(getLoc(), getCp(), getFunction(), cloneHelper.replaceOrClone(getObject()), cloneHelper.replaceOrClone(getArgs()), getNulls(), isOnInterface, typeName);
    }

    public boolean isEmptyIgnoringSynthetics() {
        MethodPrototype prototype = getMethodPrototype();
        for (int i=0, len=prototype.getArgs().size();i<len;++i) {
            if (!prototype.isHiddenArg(i)) return false;
        }
        return true;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (isOnInterface) {
            collector.collect(getFunction().getClassEntry().getTypeInstance());
        }
        collector.collect(typeName);
        super.collectTypeUsages(collector);
    }

    public boolean isInit() {
        return getMethodPrototype().getName().equals(MiscConstants.INIT_METHOD);
    }

    @Override
    protected OverloadMethodSet getOverloadMethodSetInner(JavaTypeInstance objectType) {
        return null;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        MethodPrototype methodPrototype = getMethodPrototype();
        List<Expression> args = getArgs();
        if (methodPrototype.getName().equals(MiscConstants.INIT_METHOD)) {
            d.print("super(");
        } else {
            // Let there now be a rant about how default methods on super classes allowed
            // multiple inheritance to sneak in by the back door.  Seriously, what?!
            if (isOnInterface) {
                d.dump(getFunction().getClassEntry().getTypeInstance()).separator(".");
            }
            if (this.typeName != null) {
                d.dump(this.typeName).separator(".");
            }
            d.print("super").separator(".").methodName(methodPrototype.getFixedName(), methodPrototype, false, false).separator("(");
        }
        boolean first = true;

        for (int x = 0; x < args.size(); ++x) {
            if (methodPrototype.isHiddenArg(x)) continue;
            Expression arg = args.get(x);
            if (!first) d.print(", ");
            first = false;
            methodPrototype.dumpAppropriatelyCastedArgumentString(arg, d);
        }
        d.separator(")");
        return d;
    }

    public String getName() {
        return "super";
    }
}
