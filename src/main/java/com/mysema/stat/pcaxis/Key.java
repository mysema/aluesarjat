package com.mysema.stat.pcaxis;

import java.util.List;


public class Key {

    private final String name;
    
    private final List<String> specifiers;
    
    public Key(String name) {
        this(name, null);
    }
    public Key(String name, List<String> specifier) {
        this.name = name;
        this.specifiers = specifier;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Key) {
            Key other = (Key) o;
            if (this.name.equals(other.name)) {
                if (this.specifiers == null) {
                    return other.specifiers == null;
                } else {
                    return this.specifiers.equals(other.specifiers);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public String getName() {
        return name;
    }

    public List<String> getSpecifiers() {
        return specifiers;
    }

    public int hashCode() {
        return 31*name.hashCode() + (specifiers==null ? 0 : specifiers.hashCode());
    }
    
    public String toString() {
        if (specifiers == null) {
            return name;
        } else {
            return name + "(\"" + specifiers + "\")";
        }
    }
}
