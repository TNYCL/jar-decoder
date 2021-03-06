package com.decompiler.bytecode.analysis.opgraph;

import java.util.Map;

import com.decompiler.util.ConfusedDecompilerException;
import com.decompiler.util.collections.MapFactory;

class GraphConversionHelper<X extends Graph<X>, Y extends MutableGraph<Y>> {
    private final Map<X, Y> correspondance;

    GraphConversionHelper() {
        this.correspondance = MapFactory.newMap();
    }

    private Y findEntry(X key, X orig, String dbg) {
        Y value = correspondance.get(key);
        if (value == null)
            throw new ConfusedDecompilerException("Missing key when tying up graph " + key + ", was " + dbg + " of " + orig);
        return value;
    }

    void patchUpRelations() {
        for (Map.Entry<X, Y> entry : correspondance.entrySet()) {
            X orig = entry.getKey();
            Y newnode = entry.getValue();

            for (X source : orig.getSources()) {
                newnode.addSource(findEntry(source, orig, "source"));
            }

            for (X target : orig.getTargets()) {
                newnode.addTarget(findEntry(target, orig, "target"));
            }
        }
    }

    void registerOriginalAndNew(X original, Y newnode) {
        correspondance.put(original, newnode);
    }
}
