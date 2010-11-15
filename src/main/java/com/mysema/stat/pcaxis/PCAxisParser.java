package com.mysema.stat.pcaxis;

import static com.mysema.stat.pcaxis.PCAxis.DATA;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.CharSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;

public class PCAxisParser {

    private static Logger logger = LoggerFactory.getLogger(PCAxisParser.class);

    private final static CharSet KEY = CharSet.getInstance("A-Z-");

    private final static CharSet EQ = CharSet.getInstance("=");

    private final static CharSet SCOL = CharSet.getInstance(";");

    private final static CharSet WS = CharSet.getInstance(" \t\n\f\r");

    private final static CharSet COMMA = CharSet.getInstance(",");

    private final static CharSet DOT = CharSet.getInstance(".");

    private final static CharSet NUMBER = CharSet.getInstance("0-9");

    private final static CharSet QUOTE = CharSet.getInstance("\"");

    private final static CharSet LPAR = CharSet.getInstance("(");

    private final static CharSet RPAR = CharSet.getInstance(")");

    private final DatasetHandler handler;

    private PushbackReader in;

    private int ch;

    private StringBuilder sb;

    private int row;

    private char[] recentRead;

    private int recentIndex;

    private Dataset dataset;

    private List<DimensionType> dimensionTypes;

    public PCAxisParser(DatasetHandler handler) {
        this.handler = Assert.notNull(handler, "handler");
    }

    private void init() {
        ch = -2;
        sb = new StringBuilder(128);
        row = 1;
        recentRead = new char[20];
        recentIndex = -1;
    }

    public Dataset parse(String datasetName, InputStream in) throws IOException {
        try {
        this.in = new PushbackReader(new InputStreamReader(in, "Windows-1252"), 1);
        init();
        handler.begin();
            dataset = new Dataset(datasetName);
            do {
                Key key = header();
                expect(EQ);

                if (DATA.equals(key)) {
                    handler.addDataset(dataset);
                    dimensionTypes = dataset.getDimensionTypes();

                    try {
                        streamData(0, new Dimension[dataset.dimensions()]);
                    } catch (EOFException e) {
                        logger.warn("Unexpected end of DATA");
                    }
                    handler.commit();
                    return dataset;
                } else {
                    List<String> values = values();
                    dataset.set(key, values);
                }
                skipWhitespace();
            } while (ch != -1);
        } catch (Exception e) {
            handler.rollback();
            throw new IOException("Failed to parse " + datasetName + " found " + location(), e);
        } finally {
            in.close();
        }
        throw new IllegalStateException("DATA -block not found");
    }

    private void streamData(final int dimensionIndex, final Dimension[] dimensionValues) throws IOException {
        DimensionType dimension = dataset.getDimensionType(dimensionIndex);

        List<Dimension> values = dimension.getDimensions();
        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            Dimension dimensionValue = values.get(valueIndex);
            dimensionValues[dimensionIndex] = dimensionValue;

            if (dimensionIndex + 1 == dimensionTypes.size()) {
                String value = value();
                if (value == null) {
                    throw new EOFException();
                }
                handler.addItem(new Item(dataset, asList(dimensionValues), value));
            } else {
                streamData(dimensionIndex + 1, dimensionValues);
            }
        }
    }

    private List<Dimension> asList(Dimension[] dimensionValues) {
        return new ArrayList<Dimension>(Arrays.asList(dimensionValues));
    }

    private String value() throws IOException {
        skipWhitespace();
        nextChar();
        String value;
        if (in(QUOTE)) {
            // skip QUOTE
            value = "\"" + collectWhileNotIn(QUOTE) + "\"";
            nextChar(); // skip QUOTE
        } else if (in(NUMBER)) {
            pushback(); // pushback latest number
            value = collectWhileIn(NUMBER, DOT);
        } else if (in(SCOL)) {
            return null;
        } else {
            throw new IOException("Expected string or number found " + location());
        }
        return value.intern();
    }

    private List<String> values() throws IOException {
        List<String> values = new ArrayList<String>();
        String value = value();
        do {
            values.add(value);
            skipWhileIn(WS, COMMA);
            value = value();
        } while (value != null);
        return values;
    }

    private void expect(CharSet charSet) throws IOException {
        if (nextChar().notIn(charSet)) {
            throw new IOException("Expected " + charSet + " found " + location());
        }
    }

    private Key header() throws IOException {
        skipWhitespace();
        String name = collectWhileIn(KEY);
        String spec = null;

        if (skipWhileIn(LPAR, QUOTE)) {
            spec = collectWhileNotIn(QUOTE);
            skipWhileIn(QUOTE, RPAR);
        }

        return new Key(name, spec);
    }

    public String collectWhileNotIn(CharSet... charSets) throws IOException {
        sb.setLength(0);
        while (!nextChar().in(charSets)) {
            sb.append((char) ch);
        }
        pushback();
        return sb.toString();
    }

    public String collectWhileIn(CharSet... charSets) throws IOException {
        sb.setLength(0);
        while (nextChar().in(charSets)) {
            sb.append((char) ch);
        }
        pushback();
        return sb.toString();
    }

    private boolean skipWhileIn(CharSet... charSets) throws IOException {
        boolean found = false;
        while (nextChar().in(charSets)) {
            found = true;
        }
        pushback();
        return found;
    }

//    private boolean skipWhileNotIn(CharSet... charSets) throws IOException {
//        boolean found = false;
//        while (!nextChar().in(charSets)) {
//            found = true;
//        }
//        pushback();
//        return found;
//    }

    private void skipWhitespace() throws IOException {
        while (nextChar().in(WS)) ; //NOSONAR
        pushback();
    }

    private boolean in(CharSet charSet) throws IOException {
        if (ch == -2) {
            throw new IOException("Advance first!");
        } else if (ch != -1) {
            return charSet.contains((char) ch);
        } else {
            return false;
        }
    }

    private boolean notIn(CharSet charSet) throws IOException {
        if (ch == -2) {
            throw new IOException("Advance first!");
        } else if (ch != -1) {
            return !charSet.contains((char) ch);
        } else {
            return true;
        }
    }

    private boolean in(CharSet... charSets) throws IOException {
        for (CharSet cs : charSets) {
            if (in(cs)) {
                return true;
            }
        }
        return false;
    }

    private PCAxisParser nextChar() throws IOException {
        ch = in.read();
        if (ch == '\n') {
            row++;
        }
        if (ch != -1) {
            recentRead[++recentIndex % recentRead.length] = (char) ch;
        }
        return this;
    }

    private void pushback() throws IOException {
        in.unread(ch);
        if (ch == '\n') {
            row--;
        }
        recentIndex--;

        ch = -2;
    }

    private String location() {
        StringBuilder s = new StringBuilder(recentRead.length + 10);
        if (ch > 0) {
            s.append((char) ch);
        } else {
            s.append(ch);
        }
        s.append("@");
        s.append(row);
        s.append(": ");

        int startIndex = recentIndex+1 - recentRead.length;
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (; startIndex <= recentIndex; startIndex++) {
            s.append(recentRead[startIndex % recentRead.length]);
        }
        return s.toString();
    }

    @Override
    public String toString() {
        return location();
    }
}
