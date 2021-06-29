package com.decompiler.util.output;

import java.util.Set;

import com.decompiler.bytecode.analysis.loc.HasByteCodeLoc;
import com.decompiler.bytecode.analysis.parse.utils.QuotingUtils;
import com.decompiler.bytecode.analysis.types.JavaRefTypeInstance;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.types.MethodPrototype;
import com.decompiler.mapping.NullMapping;
import com.decompiler.mapping.ObfuscationMapping;
import com.decompiler.state.TypeUsageInformation;
import com.decompiler.util.collections.SetFactory;
import com.decompiler.util.getopt.Options;
import com.decompiler.util.getopt.OptionsImpl;

public abstract class StreamDumper extends AbstractDumper {
    private final TypeUsageInformation typeUsageInformation;
    protected final Options options;
    protected final IllegalIdentifierDump illegalIdentifierDump;
    private final boolean convertUTF;
    protected final Set<JavaTypeInstance> emitted;

    StreamDumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(context);
        this.typeUsageInformation = typeUsageInformation;
        this.options = options;
        this.illegalIdentifierDump = illegalIdentifierDump;
        this.convertUTF = options.getOption(OptionsImpl.HIDE_UTF8);
        this.emitted = SetFactory.newSet();
    }

    StreamDumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context, Set<JavaTypeInstance> emitted) {
        super(context);
        this.typeUsageInformation = typeUsageInformation;
        this.options = options;
        this.illegalIdentifierDump = illegalIdentifierDump;
        this.convertUTF = options.getOption(OptionsImpl.HIDE_UTF8);
        this.emitted = emitted;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return typeUsageInformation;
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return NullMapping.INSTANCE;
    }

    protected abstract void write(String s);

    @Override
    public Dumper label(String s, boolean inline) {
        processPendingCR();
        if (inline) {
            doIndent();
            write(s + ": ");
        } else {
            write(s + ":");
            newln();
        }
        return this;
    }

    @Override
    public Dumper identifier(String s, Object ref, boolean defines) {
        return print(illegalIdentifierDump.getLegalIdentifierFor(s));
    }

    @Override
    public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
        return identifier(s, null, defines);
    }

    @Override
    public Dumper packageName(JavaRefTypeInstance t) {
        String s = t.getPackageName();
        if (!s.isEmpty()) {
            keyword("package ").print(s).endCodeln().newln();
        }
        return this;
    }

    @Override
    public Dumper print(String s) {
        processPendingCR();
        doIndent();
        boolean doNewLn = false;
        if (s.endsWith("\n")) { // this should never happen.
            s = s.substring(0, s.length() - 1);
            doNewLn = true;
        }
        if (convertUTF) s = QuotingUtils.enquoteUTF(s);
        write(s);
        context.atStart = false;
        if (doNewLn) {
            newln();
        }
        context.outputCount++;
        return this;
    }

    @Override
    public Dumper print(char c) {
        return print("" + c);
    }

    @Override
    public Dumper keyword(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper operator(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper separator(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper literal(String s, Object o) {
        print(s);
        return this;
    }

    @Override
    public Dumper newln() {
        if (context.pendingCR) {
            write("\n");
            context.currentLine++;
            if (context.atStart && context.inBlockComment != BlockCommentState.Not) {
                doIndent();
            }
        }
        context.pendingCR = true;
        context.atStart = true;
        context.outputCount++;
        return this;
    }

    @Override
    public Dumper endCodeln() {
        write(";");
        context.pendingCR = true;
        context.atStart = true;
        context.outputCount++;
        return this;
    }

    private void doIndent() {
        if (!context.atStart) return;
        for (int x = 0; x < context.indent; ++x) write(STANDARD_INDENT);
        context.atStart = false;
        if (context.inBlockComment != BlockCommentState.Not) {
            write (" * ");
        }
    }

    private void processPendingCR() {
        if (context.pendingCR) {
            write("\n");
            context.atStart = true;
            context.pendingCR = false;
            context.currentLine++;
        }
    }

    @Override
    public Dumper explicitIndent() {
        print(STANDARD_INDENT);
        return this;
    }

    @Override
    public void indent(int diff) {
        context.indent += diff;
    }

    @Override
    public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        identifier(name, null, defines);
        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
        javaTypeInstance.dumpInto(this, typeUsageInformation, typeContext);
        return this;
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            return keyword("null");
        }
        return d.dump(this);
    }

    @Override
    public boolean canEmitClass(JavaTypeInstance type) {
        return emitted.add(type);
    }

    @Override
    public int getOutputCount() {
        return context.outputCount;
    }

    @Override
    public int getCurrentLine() {
        int res = context.currentLine;
        if (context.pendingCR) res++;
        return res;
    }
}
