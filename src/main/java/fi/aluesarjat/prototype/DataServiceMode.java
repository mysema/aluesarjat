/**
 * 
 */
package fi.aluesarjat.prototype;

/**
 * @author tiwe
 *
 */
public enum DataServiceMode { 
  
    /**
     * Run dataset loads in parallel
     */
    PARALLEL, 
  
    /**
     * Run dataset loads in a separate thread
     */
    THREADED, 

    /**
     * Run dataset loads in current thread
     */
    NONTHREADED 

}