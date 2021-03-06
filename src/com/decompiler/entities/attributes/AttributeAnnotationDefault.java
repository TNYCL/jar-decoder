package com.decompiler.entities.attributes;

import com.decompiler.bytecode.analysis.parse.utils.Pair;
import com.decompiler.entities.annotations.ElementValue;
import com.decompiler.entities.constantpool.ConstantPool;
import com.decompiler.state.TypeUsageCollector;
import com.decompiler.util.bytestream.ByteData;
import com.decompiler.util.output.Dumper;

public class AttributeAnnotationDefault extends Attribute {
    public static final String ATTRIBUTE_NAME = "AnnotationDefault";

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_REMAINDER = 6;

    private final int length;
    private final ElementValue elementValue;

    public AttributeAnnotationDefault(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        Pair<Long, ElementValue> tmp = AnnotationHelpers.getElementValue(raw, OFFSET_OF_REMAINDER, cp);
        this.elementValue = tmp.getSecond();
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return elementValue.dump(d);
    }

    @Override
    public long getRawByteLength() {
        return OFFSET_OF_REMAINDER + length;
    }

    @Override
    public String toString() {
        return "Annotationdefault : " + elementValue;
    }

    public ElementValue getElementValue() {
        return elementValue;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        elementValue.collectTypeUsages(collector);
    }
}
