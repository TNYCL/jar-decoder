package com.decompiler.bytecode.analysis.opgraph.op2rewriters;

import java.util.List;
import java.util.Map;

import com.decompiler.bytecode.BytecodeMeta;
import com.decompiler.bytecode.analysis.types.*;
import com.decompiler.bytecode.analysis.types.discovery.InferredJavaType;

public class TypeHintRecoveryImpl implements TypeHintRecovery {
    private final Map<Integer, JavaTypeInstance> iteratedTypeHints;

    public TypeHintRecoveryImpl(BytecodeMeta bytecodeMeta) {
        this.iteratedTypeHints = bytecodeMeta.getIteratedTypeHints();
    }

    @Override
    public void improve(InferredJavaType type) {
        int index = type.getTaggedBytecodeLocation();
        if (index < 0) return;
        JavaTypeInstance original = type.getJavaTypeInstance();
        JavaTypeInstance improved = iteratedTypeHints.get(index);
        if (improved == null || !(original instanceof JavaGenericRefTypeInstance)) return;
        BindingSuperContainer bindingSuperContainer = original.getBindingSupers();
        JavaGenericRefTypeInstance unboundIterable = bindingSuperContainer.getBoundSuperForBase(TypeConstants.ITERABLE);
        JavaGenericRefTypeInstance unboundThis = bindingSuperContainer.getBoundSuperForBase(original.getDeGenerifiedType());
        List<JavaTypeInstance> iterTypes = unboundIterable.getGenericTypes();
        if (iterTypes.size() != 1) return;
        String unbound = iterTypes.get(0).getRawName();
        GenericTypeBinder genericTypeBinder = GenericTypeBinder.createEmpty();
        genericTypeBinder.suggestBindingFor(unbound, improved);
        JavaTypeInstance rebound = genericTypeBinder.getBindingFor(unboundThis);
        type.forceDelegate(new InferredJavaType(rebound, InferredJavaType.Source.IMPROVED_ITERATION));
    }
}
