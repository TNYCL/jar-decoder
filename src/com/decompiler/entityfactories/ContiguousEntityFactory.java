package com.decompiler.entityfactories;

import java.util.List;

import com.decompiler.util.KnowsRawSize;
import com.decompiler.util.bytestream.ByteData;
import com.decompiler.util.bytestream.OffsettingByteData;
import com.decompiler.util.functors.UnaryFunction;

public class ContiguousEntityFactory {

    public static<X extends KnowsRawSize> long build(final ByteData raw, int count, List<X> tgt, UnaryFunction<ByteData, X> func)
    {
        OffsettingByteData data = raw.getOffsettingOffsetData(0);
        for (int x=0;x<count;++x)
        {
            X tmp = func.invoke(data);
            tgt.add(tmp);
            data.advance(tmp.getRawByteLength());
        }
        return data.getOffset();
    }

    public static<X> long buildSized(final ByteData raw, int count, int itemLength, List<X> tgt, UnaryFunction<ByteData, X> func)
    {
        OffsettingByteData data = raw.getOffsettingOffsetData(0);
        for (short x=0;x<count;++x)
        {
            X tmp = func.invoke(data);
            tgt.add(tmp);
            data.advance(itemLength);
        }
        return data.getOffset(); // will be count * itemLength, of course....
    }
}
