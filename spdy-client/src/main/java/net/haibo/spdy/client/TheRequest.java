/**
 * 
 */
package net.haibo.spdy.client;

/**
 * It enums all the possible implemenation of http request
 * and you can use the <code>create()</code> factory alike method
 * to create the right kind implemenation of http request.
 */
public enum TheRequest {
    OkHttp {
        @Override public HttpRequest create() {
            return new SpdyHttpRequest();
        }
    },
    
    Spdy {
        @Override public HttpRequest create() {
            return new SpdyHttpRequest().forceSpdy();
        }
    },

    Apache {
        @Override public HttpRequest create() {
            return new ApacheHttpRequest();
        }
    };

    public abstract HttpRequest create();
}
