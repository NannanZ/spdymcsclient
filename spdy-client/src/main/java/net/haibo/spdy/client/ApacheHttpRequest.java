/**
 * 
 */
package net.haibo.spdy.client;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


/**
 * @author HAIBO
 * It's an implementation of abstract http request using Apache http client.
 * 
 * Reference you might need:
 * http://hc.apache.org/httpcomponents-client-4.3.x/examples.html
 * https://httpbin.org/
 */
public class ApacheHttpRequest implements IHttpRequest {
    
    public static boolean allowInsecure = true;
    
    @Override
    public String post(String url, String dataWrapper) throws Exception {
        CloseableHttpClient httpclient = null;
        if (allowInsecure) {
            httpclient = HttpClients.custom().setSslcontext(UTILS.createInsecureSslContext())
                .setHostnameVerifier(UTILS.createInsecureX509HostnameVerifier()).build();
        } else {
            httpclient = HttpClients.createDefault();
        }
        
        try {
            HttpPost httppost = new HttpPost(url);
            StringEntity body = new StringEntity(dataWrapper);
            body.setContentType("application/x-www-form-urlencoded");
            httppost.setEntity(body);

            System.out.println("@$<< Executing request: " + httppost.getRequestLine());
            
            CloseableHttpResponse response = httpclient.execute(httppost);
            
            try {
                System.out.println("----------------------------------------");
                System.out.println("@$>> " + response.getStatusLine());
                HttpEntity entity = response.getEntity();
                String result = entity != null ? EntityUtils.toString(entity) : null;
                System.out.println(result);
                System.out.println("----------------------------------------");
                return result;
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }

    @Override
    public String get(String url) throws Exception {
        CloseableHttpClient httpclient = null;
        if (allowInsecure) {
            httpclient = HttpClients.custom().setSslcontext(UTILS.createInsecureSslContext())
                .setHostnameVerifier(UTILS.createInsecureX509HostnameVerifier()).build();
        } else {
            httpclient = HttpClients.createDefault();
        }
        try {
            HttpGet httpget = new HttpGet(url);
            System.out.println("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) throws ClientProtocolException,
                        IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        String result = entity != null ? EntityUtils.toString(entity) : null;
                        System.out.println(result);
                        return result;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
            };
            String response = httpclient.execute(httpget, responseHandler);
            System.out.println(response);
            return response;
        } catch (Exception e) {
            throw e;
        } finally {
            httpclient.close();
        }
    }

    @Override public IHttpRequest accept(IRequestVisitor v) {
        v.visit(this);
        return this;
    }

    @Override
    public String mutliPart(String url, Object body) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public String post(String url, byte[] body, String mediaType) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public Object buildMutliparts(Map<String, String> query, List<File> files) {
        throw new NotImplementedException();
    }

}
