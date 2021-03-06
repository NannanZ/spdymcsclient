/**
 * 
 */
package net.haibo.spdy.client;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Definition of abstract http requst for mcs call
 */
public interface HttpRequest {
    public String get(String url) throws Exception;
    public String post(String url, String dataWrapper) throws Exception;
    public String mutliPart(String url, Object body) throws Exception;
    public String post(String url, byte[] body, String mediaType) throws Exception;
    public Object buildMutliparts(Map<String, String> query, List<File> files);
    public HttpRequest accept(RequestVisitor v);
}
