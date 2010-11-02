package com.mysema.stat.pcaxis;


public class Key {

    private final String name;
    
    private final String specifier;
    
    public Key(String name) {
        this(name, null);
    }
    public Key(String name, String specifier) {
        this.name = name;
        this.specifier = specifier;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Key) {
            Key other = (Key) o;
            if (this.name.equals(other.name)) {
                if (this.specifier == null) {
                    return other.specifier == null;
                } else {
                    return this.specifier.equals(other.specifier);
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

    public String getSpecifier() {
        return specifier;
    }

    public int hashCode() {
        return 31*name.hashCode() + (specifier==null ? 0 : specifier.hashCode());
    }
    
    public String toString() {
        if (specifier == null) {
            return name;
        } else {
            return name + "(\"" + specifier + "\")";
        }
    }
}
