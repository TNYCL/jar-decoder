package com.decompiler.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;

import com.decompiler.bytecode.analysis.loc.BytecodeLoc;
import com.decompiler.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.decompiler.bytecode.analysis.parse.Statement;
import com.decompiler.bytecode.analysis.parse.statement.GotoStatement;
import com.decompiler.bytecode.analysis.parse.statement.IfStatement;
import com.decompiler.bytecode.analysis.parse.statement.ThrowStatement;
import com.decompiler.bytecode.analysis.parse.utils.JumpType;
import com.decompiler.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.decompiler.bytecode.analysis.types.TypeConstants;

class AssertionJumps {
    static void extractAssertionJumps(List<Op03SimpleStatement> in) {
        /*
         * If we have
         *
         * if () [non-goto-jump XX]
         * throw new AssertionError
         *
         * transform BACK to
         *
         * if () goto YYY
         * throw new AssertionError
         * YYY:
         * non-goto-jump XX
         */
        WildcardMatch wcm = new WildcardMatch();
        Statement assertionError = new ThrowStatement(BytecodeLoc.TODO, wcm.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR));

        for (int x=0,len=in.size();x<len;++x) {
            Op03SimpleStatement ostm = in.get(x);
            Statement stm = ostm.getStatement();
            if (stm.getClass() != IfStatement.class) continue;
            IfStatement ifStatement = (IfStatement)stm;
            if (ifStatement.getJumpType() == JumpType.GOTO) continue;
            Op03SimpleStatement next = in.get(x+1);
            if (next.getSources().size() != 1) continue;
            wcm.reset();
            if (!assertionError.equals(next.getStatement())) continue;
            if (!ostm.getBlockIdentifiers().equals(next.getBlockIdentifiers())) continue;
            GotoStatement reJumpStm = new GotoStatement(BytecodeLoc.TODO);
            reJumpStm.setJumpType(ifStatement.getJumpType());
            Op03SimpleStatement reJump = new Op03SimpleStatement(ostm.getBlockIdentifiers(), reJumpStm, next.getIndex().justAfter());
            in.add(x+2, reJump);
            Op03SimpleStatement origTarget = ostm.getTargets().get(1);
            ostm.replaceTarget(origTarget, reJump);
            reJump.addSource(ostm);
            origTarget.replaceSource(ostm, reJump);
            reJump.addTarget(origTarget);
            ifStatement.setJumpType(JumpType.GOTO);
            len++;
        }
    }


}
