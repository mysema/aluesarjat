/**
 * 
 */
package fi.aluesarjat.prototype;

enum Format { 
    
    /**
     * 
     */
    JSON("application/json", "UTF-8"), 
    
    /**
     * 
     */
    JSONP("text/javascript", "UTF-8"),
    
    /**
     * 
     */
    CSV("text/csv", "ISO-8859-15");
    
    private final String contentType;
    
    private final String encoding;
    
    private Format(String contentType, String encoding) {
        this.contentType = contentType;
        this.encoding = encoding;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public String getCharacterEncoding() {
        return encoding;
    }
    
}