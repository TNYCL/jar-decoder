package com.decompiler.bytecode.analysis.parse.utils.scope;

import java.util.Map;

import com.decompiler.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.decompiler.bytecode.analysis.parse.Expression;
import com.decompiler.bytecode.analysis.parse.LValue;
import com.decompiler.bytecode.analysis.parse.StatementContainer;
import com.decompiler.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import com.decompiler.bytecode.analysis.parse.lvalue.FieldVariable;
import com.decompiler.bytecode.analysis.parse.lvalue.LocalVariable;
import com.decompiler.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import com.decompiler.bytecode.analysis.parse.utils.ReadWrite;
import com.decompiler.bytecode.analysis.structured.StructuredStatement;
import com.decompiler.bytecode.analysis.types.InnerClassInfo;
import com.decompiler.bytecode.analysis.types.JavaRefTypeInstance;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.bytecode.analysis.variables.NamedVariable;
import com.decompiler.bytecode.analysis.variables.VariableFactory;
import com.decompiler.entities.Method;
import com.decompiler.state.AbstractTypeUsageCollector;
import com.decompiler.state.TypeUsageInformation;
import com.decompiler.util.collections.MapFactory;
import com.decompiler.util.getopt.Options;
import com.decompiler.util.output.Dumper;

public class LocalClassScopeDiscoverImpl extends AbstractLValueScopeDiscoverer {
    private final Map<JavaTypeInstance, Boolean> localClassTypes = MapFactory.newIdentityMap();
    private final TypeUsageSpotter typeUsageSpotter = new TypeUsageSpotter();
    private final JavaTypeInstance scopeType;

    public LocalClassScopeDiscoverImpl(Options options, Method method, VariableFactory variableFactory) {
        super(options, method.getMethodPrototype(), variableFactory);
        scopeType = method.getMethodPrototype().getClassType();

        JavaTypeInstance thisClassType = method.getClassFile().getClassType();
        while (thisClassType != null) {
            if (null != localClassTypes.put(thisClassType, Boolean.FALSE)) break;
            InnerClassInfo innerClassInfo = thisClassType.getInnerClassHereInfo();
            if (!innerClassInfo.isInnerClass()) break;
            thisClassType = innerClassInfo.getOuterClass();
        }
    }

    private static class SentinelNV implements NamedVariable {
        private final JavaTypeInstance typeInstance;

        private SentinelNV(JavaTypeInstance typeInstance) {
            this.typeInstance = typeInstance;
        }

        @Override
        public void forceName(String name) {
        }

        @Override
        public String getStringName() {
            return typeInstance.getRawName();
        }

        @Override
        public boolean isGoodName() {
            return true;
        }

        @Override
        public Dumper dump(Dumper d) {
            return null;
        }

        @Override
        public Dumper dump(Dumper d, boolean defines) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SentinelNV that = (SentinelNV) o;

            if (typeInstance != null ? !typeInstance.equals(that.typeInstance) : that.typeInstance != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return typeInstance != null ? typeInstance.hashCode() : 0;
        }
    }

    @Override
    public void processOp04Statement(Op04StructuredStatement statement) {
        statement.getStatement().collectTypeUsages(typeUsageSpotter);
        super.processOp04Statement(statement);
    }

    @Override
    public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        collect(localVariable, ReadWrite.WRITE);
    }

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        Class<?> lValueClass = lValue.getClass();

        if (lValueClass == SentinelLocalClassLValue.class) {
            SentinelLocalClassLValue localClassLValue = (SentinelLocalClassLValue) lValue;

            JavaTypeInstance type = localClassLValue.getLocalClassType();
            if (type.getDeGenerifiedType() == scopeType) return;
            defineHere(lValue, type, true);

        }  else if (lValueClass == FieldVariable.class) {
            lValue.collectLValueUsage(this);
        }
    }

    private void defineHere(LValue lValue, JavaTypeInstance type, boolean immediate) {

        NamedVariable name = new SentinelNV(type);
        NamedVariable keyName = new SentinelNV(type.getDeGenerifiedType());

        ScopeDefinition previousDef = earliestDefinition.get(keyName);
        // If it's in scope, no problem.
        if (previousDef != null) {
            if (previousDef.isImmediate()
            || !immediate) {
                return;
            }
            if (previousDef.getDepth() < currentDepth) {
                previousDef.setImmediate();
                return;
            }
            if (!previousDef.isImmediate()) {
                earliestDefinitionsByLevel.get(currentDepth).remove(keyName);
                discoveredCreations.remove(previousDef);
            }
        }

        ScopeDefinition scopeDefinition = new ScopeDefinition(currentDepth, currentBlock, currentBlock.peek(), lValue, type, name, currentMark, immediate);
        earliestDefinition.put(keyName, scopeDefinition);
        earliestDefinitionsByLevel.get(currentDepth).put(keyName, true);
        discoveredCreations.add(scopeDefinition);
        localClassTypes.put(type, Boolean.TRUE);
    }

    @Override
    public boolean descendLambdas() {
        return true;
    }

    class TypeUsageSpotter extends AbstractTypeUsageCollector {

        @Override
        public void collectRefType(JavaRefTypeInstance type) {
            collect(type);
        }

        @Override
        public void collect(JavaTypeInstance type) {
            if (type == null) return;
            Boolean localClass = localClassTypes.get(type);
            if (localClass == null) {
                localClass = ConstructorInvokationSimple.isAnonymousMethodType(type);
                localClassTypes.put(type, localClass);
            }
            if (localClass == Boolean.FALSE) return;
            LValue sentinel = new SentinelLocalClassLValue(type);
            defineHere(sentinel, type, false);
        }

        @Override
        public TypeUsageInformation getTypeUsageInformation() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isStatementRecursive() {
            return false;
        }
    }

}
