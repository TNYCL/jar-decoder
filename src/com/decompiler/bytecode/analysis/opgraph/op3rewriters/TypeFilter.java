package com.decompiler.bytecode.analysis.opgraph.op3rewriters;

import com.decompiler.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.decompiler.util.functors.Predicate;

public class TypeFilter<T> implements Predicate<Op03SimpleStatement> {
    private final Class<T> clazz;
    private final boolean positive;

    public TypeFilter(Class<T> clazz) {
        this.clazz = clazz;
        this.positive = true;
    }

    public TypeFilter(Class<T> clazz, boolean positive) {
        this.clazz = clazz;
        this.positive = positive;
    }

    @Override
    public boolean test(Op03SimpleStatement in) {
        return (positive == clazz.isInstance(in.getStatement()));
    }
}