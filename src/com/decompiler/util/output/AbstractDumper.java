package com.decompiler.util.output;

import com.decompiler.bytecode.analysis.loc.HasByteCodeLoc;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;

abstract class AbstractDumper implements Dumper {
    protected static final String STANDARD_INDENT = "    ";
    final MovableDumperContext context;

    AbstractDumper(MovableDumperContext context) {
        this.context = context;
    }

    @Override
    public Dumper beginBlockComment(boolean inline) {
        if (context.inBlockComment != BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to nest block comments.");
        }
        if (inline) {
            print("/* ");
        } else {
            print("/*").newln();
        }
        context.inBlockComment = inline ? BlockCommentState.InLine : BlockCommentState.In;
        return this;
    }

    @Override
    public Dumper endBlockComment() {
        if (context.inBlockComment == BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to end block comment when not in one.");
        }
        BlockCommentState old = context.inBlockComment;
        context.inBlockComment = BlockCommentState.Not;
        if (old == BlockCommentState.In) {
            if (!context.atStart) {
                newln();
            }
            print(" */").newln();
        } else {
            print(" */ ");
        }
        return this;
    }

    @Override
    public Dumper comment(String s) {
        if (context.inBlockComment == BlockCommentState.Not) {
            print("// " + s);
        } else {
            print(s);
        }
        return newln();
    }

    @Override
    public void enqueuePendingCarriageReturn() {
        context.pendingCR = true;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance) {
        return dump(javaTypeInstance, TypeContext.None);
    }

    @Override
    public Dumper removePendingCarriageReturn() {
        context.pendingCR = false;
        context.atStart = false;
        return this;
    }

    @Override
    public int getCurrentLine() {
        return context.currentLine;
    }

    @Override
    public int getIndentLevel() {
        return context.indent;
    }

    @Override
    public void informBytecodeLoc(HasByteCodeLoc loc) {
    }
}
