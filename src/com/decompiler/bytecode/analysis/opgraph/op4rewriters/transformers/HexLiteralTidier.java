package com.decompiler.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.ArithOp;
import com.decompiler.bytecode.analysis.parse.expression.ArithmeticMutationOperation;
import com.decompiler.bytecode.analysis.parse.expression.ArithmeticOperation;
import com.decompiler.bytecode.analysis.parse.expression.Literal;
import com.decompiler.bytecode.analysis.parse.expression.LiteralHex;
import com.decompiler.bytecode.analysis.parse.literal.TypedLiteral;
import com.decompiler.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.decompiler.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.decompiler.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.decompiler.bytecode.analysis.structured.StructuredScope;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;

public class HexLiteralTidier extends AbstractExpressionRewriter implements StructuredStatementTransformer {

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    private static boolean bitOp(ArithOp op) {
        switch (op) {
            case AND:
            case OR:
            case XOR:
                return true;
            default:
                return false;
        }
    }

    private static Expression applyTransforms(ArithmeticOperation t) {
        if (!bitOp(t.getOp())) return t;
        Expression l = convertLiteral(t.getLhs());
        Expression r = convertLiteral(t.getRhs());
        if (l == null && r == null) {
            return t;
        }
        return new ArithmeticOperation(BytecodeLoc.TODO,
                l == null ? t.getLhs() : l,
                r == null ? t.getRhs() : r,
                t.getOp()
                );
    }

    private static Expression applyTransforms(ArithmeticMutationOperation t) {
        if (!bitOp(t.getOp())) return t;
        Expression r = convertLiteral(t.getMutation());
        if (r == null) return t;
        return new ArithmeticMutationOperation(BytecodeLoc.TODO,
                t.getUpdatedLValue(), r, t.getOp());
    }

    private static Expression convertLiteral(Expression e) {
        if (e instanceof Literal) {
            Literal l = (Literal)e;
            TypedLiteral tl = l.getValue();
            // there's no way this should be anything other than integral, but... ;)
            // No point using hex if < 10, it just makes it messy.
            switch (tl.getType()) {
                case Long:
                    long lv = tl.getLongValue();
                    if (lv >= 0 && lv < 10) return null;
                    break;
                case Integer:
                    int iv = tl.getIntValue();
                    if (iv >= 0 && iv< 10) return null;
                    break;
                default:
                    return null;
            }
            return new LiteralHex(tl);
        }
        return null;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof ArithmeticOperation) {
            expression = applyTransforms((ArithmeticOperation)expression);
        } else if (expression instanceof ArithmeticMutationOperation) {
            expression = applyTransforms((ArithmeticMutationOperation)expression);
        }
        return expression;
    }
}
