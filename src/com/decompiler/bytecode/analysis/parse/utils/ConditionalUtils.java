package com.decompiler.bytecode.analysis.parse.utils;

import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.expression.ConditionalExpression;
import com.decompiler.bytecode.analysis.parse.expression.Literal;
import com.decompiler.bytecode.analysis.parse.expression.TernaryExpression;
import com.decompiler.bytecode.analysis.parse.expression.misc.Precedence;
import com.decompiler.bytecode.analysis.types.RawJavaType;
import com.decompiler.util.MiscUtils;

public class ConditionalUtils {
    public static ConditionalExpression simplify(ConditionalExpression condition) {
        ConditionalExpression applyDemorgan = condition.getDemorganApplied(false);
        int demorganSize = applyDemorgan.getSize(Precedence.WEAKEST);
        int conditionSize = condition.getSize(Precedence.WEAKEST);
        if (demorganSize < conditionSize) {
            condition = applyDemorgan;
        }
        // Prefer right-deep trees to left-deep (other transforms (eg assert) may hardcode expected expressions, so
        // if we have to cope with all balancings of the same tree, it becomes painful!)
        // Note that this MAY NOT change ordering, just layout.
        condition = condition.getRightDeep();
        return condition;
    }

    public static Expression simplify(TernaryExpression condition) {
        if (condition.getInferredJavaType().getRawType() != RawJavaType.BOOLEAN) return condition;
        Expression e1 = condition.getLhs();
        Expression e2 = condition.getRhs();
        ConditionalExpression pred = condition.getCondition();
        if (e1.equals(Literal.TRUE) && e2.equals(Literal.FALSE)) {
            // pred is real!
            MiscUtils.handyBreakPoint();
        } else if (e1.equals(Literal.FALSE) && e2.equals(Literal.TRUE)) {
            pred = pred.getNegated();
        } else {
            return condition;
        }
        return simplify(pred);
    }

}
