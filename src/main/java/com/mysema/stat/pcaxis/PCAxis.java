package com.mysema.stat.pcaxis;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

public final class PCAxis {

    public static final Key DATA = new Key("DATA");
    
    public static final Key HEADING = new Key("HEADING");
    
    public static final Key STUB = new Key("STUB");
    
    public static final Key TITLE = new Key("TITLE");

    public static Object convert(String value) {
        if (value.startsWith("\"")) {
            return value.substring(1, value.length() - 1);
        } else {
            return new BigDecimal(value);
        }
    }
    
    public static String convertString(String value) {
        return value.substring(1, value.length() - 1);
    }
    
    @SuppressWarnings("unchecked")
    public static Map<Key, List<Object>> parse(InputStream in) {
        try {
            // create a CharStream that reads from standard input
            ANTLRInputStream input = new ANTLRInputStream(in, "Windows-1252"); // create a lexer that feeds off of input CharStream
            PCAxisANTLRLexer lexer = new PCAxisANTLRLexer(input); // create a buffer of tokens pulled from the lexer
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            // create a parser that feeds off the tokens buffer
            PCAxisANTLRParser parser = new PCAxisANTLRParser(tokens); // begin parsing at rule r parser.r();
            
            return (Map<Key, List<Object>>) parser.px().result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RecognitionException e) {
            throw new RuntimeException(e);
        }
    }
    
    private PCAxis(){}
    
}
