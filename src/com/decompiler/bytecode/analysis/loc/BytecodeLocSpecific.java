package com.decompiler.bytecode.analysis.loc;

import java.util.Collection;
import java.util.Collections;

import com.decompiler.entities.Method;

class BytecodeLocSpecific extends BytecodeLoc {
    enum Specific {
        DISABLED,
        TODO,
        NONE
    }

    private final Specific type;

    BytecodeLocSpecific(Specific type) {
        this.type = type;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
    }

    @Override
    public String toString() {
        return type.name();
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return Collections.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
