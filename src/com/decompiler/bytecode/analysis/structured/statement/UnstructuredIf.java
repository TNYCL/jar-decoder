package com.decompiler.bytecode.analysis.structured.statement;

import java.util.LinkedList;
import java.util.Vector;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.expression.ConditionalExpression;
import com.decompiler.bytecode.analysis.parse.utils.BlockIdentifier;
import com.decompiler.bytecode.analysis.parse.utils.BlockType;
import com.decompiler.bytecode.analysis.parse.utils.ConditionalUtils;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.ConfusedDecompilerException;
import com.decompiler.util.Optional;
import com.decompiler.util.collections.ListFactory;
import com.decompiler.util.output.Dumper;

public class UnstructuredIf extends AbstractUnStructuredStatement {
    private ConditionalExpression conditionalExpression;
    private Op04StructuredStatement setIfBlock;
    private BlockIdentifier knownIfBlock;
    private BlockIdentifier knownElseBlock;

    public UnstructuredIf(BytecodeLoc loc, ConditionalExpression conditionalExpression, BlockIdentifier knownIfBlock, BlockIdentifier knownElseBlock) {
        super(loc);
        this.conditionalExpression = conditionalExpression;
        this.knownIfBlock = knownIfBlock;
        this.knownElseBlock = knownElseBlock;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, conditionalExpression);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(conditionalExpression);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("** if (").dump(conditionalExpression).print(") goto " + getContainer().getTargetLabel(1)).newln();
        if (setIfBlock != null) {
            dumper.dump(setIfBlock);
        }
        return dumper;
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier == knownIfBlock) {
            if (knownElseBlock == null) {
                Op04StructuredStatement fakeElse = new Op04StructuredStatement(new UnstructuredGoto(BytecodeLoc.TODO));
                Op04StructuredStatement fakeElseTarget = getContainer().getTargets().get(1);
                fakeElse.addTarget(fakeElseTarget);
                fakeElseTarget.addSource(fakeElse);
                LinkedList<Op04StructuredStatement> fakeBlockContent = ListFactory.newLinkedList();
                fakeBlockContent.add(fakeElse);
                Op04StructuredStatement fakeElseBlock = new Op04StructuredStatement(new Block(fakeBlockContent, true));
                return new StructuredIf(getLoc(), ConditionalUtils.simplify(conditionalExpression.getNegated()), innerBlock, fakeElseBlock);
            } else {
                setIfBlock = innerBlock;
                return this;
            }
        } else if (blockIdentifier == knownElseBlock) {
            if (setIfBlock == null) {
                throw new ConfusedDecompilerException("Set else block before setting IF block");
            }
            /* If this was a SIMPLE if, it ends in a jump to just after the ELSE block.
             * We need to snip that out.
             */
            if (knownIfBlock.getBlockType() == BlockType.SIMPLE_IF_TAKEN) {
                setIfBlock.removeLastGoto();
            }
            innerBlock = unpackElseIfBlock(innerBlock);
            return new StructuredIf(getLoc(), ConditionalUtils.simplify(conditionalExpression.getNegated()), setIfBlock, innerBlock);
        } else {
            return null;
//            throw new ConfusedCFRException("IF statement given blocks it doesn't recognise");
        }
    }

    /*
     * Maybe this could be put somewhere more general....
     */
    private static Op04StructuredStatement unpackElseIfBlock(Op04StructuredStatement elseBlock) {
        StructuredStatement elseStmt = elseBlock.getStatement();
        if (!(elseStmt instanceof Block)) return elseBlock;
        Block block = (Block) elseStmt;
        Optional<Op04StructuredStatement> maybeStatement = block.getMaybeJustOneStatement();
        if (!maybeStatement.isSet()) {
//            Dumper d = new Dumper();
//            block.dump(d);
            return elseBlock;
        }
        Op04StructuredStatement inner = maybeStatement.getValue();
        if (inner.getStatement() instanceof StructuredIf) {
            return inner;
        }
        return elseBlock;
    }

    /*
     * An unstructured if which is effectively just a goto can be converted explicitly to a structured if
     * containing an unstructured goto.
     */
    public StructuredStatement convertEmptyToGoto() {
        if (!(knownIfBlock == null && knownElseBlock == null && setIfBlock == null)) return this;
        Op04StructuredStatement gotoStm = new Op04StructuredStatement(
                new UnstructuredGoto(BytecodeLoc.TODO)
        );
        Op04StructuredStatement target = getContainer().getTargets().get(1);
        gotoStm.addTarget(target);
        target.getSources().remove(this.getContainer());
        target.addSource(gotoStm);
        return new StructuredIf(getLoc(), conditionalExpression, gotoStm);
    }
}
