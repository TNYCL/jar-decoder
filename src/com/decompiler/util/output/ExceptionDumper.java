package com.decompiler.util.output;

public interface ExceptionDumper {
    void noteException(String path, String comment, Exception e);
}
