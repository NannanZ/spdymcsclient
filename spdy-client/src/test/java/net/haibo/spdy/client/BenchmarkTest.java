/**
 * 
 */
package net.haibo.spdy.client;

import static org.junit.Assert.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.haibo.spdy.client.benchmark.Benchmark1;
import net.haibo.spdy.client.benchmark.Benchmark2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author HAIBO
 *
 */
public final class BenchmarkTest implements ICallback {
    Logger logger = null;

    LoginInfo login = null;
    @Before public void setUp() throws Exception {
        System.setProperty("http.maxConnections", "100000");
        System.setProperty("http.keepAliveDuration", String.valueOf(30 * 60 * 60 * 1000)); // 30 hours
        logger = Logger.getLogger("net.haibo.spdy.client.BenchmarkTest");

        login = UTILS.getLogin();
    }
    
    @After public void tearDown() { }
    
    @Test public void normalPerSecond() throws Exception {
        requestPerSecond("http://118.186.217.31/api/", TheRequest.Spdy, 8);
        assertEquals("", "");
    }

    // Spdy: 5, "Spdy_Http_Feed_Get_mcs.log", 5, "http://118.186.217.31/api/", TheRequest.Spdy
    // OkHttp: 5, "OkHttp_Https_Feed_Get_mcs.log", 5, "https://118.186.217.31/api/", TheRequest.OkHttp
//    @Test public void OkHttp_Spdy_request_mcs_compress() throws Exception {
//        compressRequestTest(10, "OkHttp_Spdy_request_mcs_compress.log", 10, "http://118.186.217.31/api/", TheRequest.Spdy, false);
//        assertEquals("", "");
//    }
//    
//    @Test public void OkHttp_Http_request_mcs_compress() throws Exception {
//        compressRequestTest(10, "OkHttp_Http_request_mcs_compress.log", 10, "http://123.125.36.108/api/", TheRequest.OkHttp, false);
//        assertEquals("", "");
//    }

    @Test public void OKHttp_Spdy_request_mcs_mulit_times() throws Exception {
        sequenceRequestTest(10, "OKHttp_Spdy_request_mcs_mulit_times.log", "http://118.186.217.31/api/", TheRequest.Spdy);
        assertEquals("", "");
    }
    
    @Test public void OkHttp_Http_request_mcs_mulit_times() throws Exception {
        sequenceRequestTest(10, "OkHttp_Http_request_mcs_mulit_times.log", "http://123.125.36.108/api/", TheRequest.OkHttp);
        assertEquals("", "");
    }

    @Test public void OkHttp_Spdy_request_mcs_compress_muti_channels_compress() throws Exception {
        compressRequestTest(10000, "OkHttp_Spdy_request_mcs_compress_muti_channels_compress.log", 5000, "http://118.186.217.31/api/", TheRequest.Spdy, true);
        assertEquals("", "");
    }

    @Test public void OkHttp_Http_request_mcs_compress_muti_channels_compress() throws Exception {
        compressRequestTest(10, "OkHttp_Http_request_mcs_compress_muti_channels_compress.log", 15, "http://123.125.36.108/api/", TheRequest.OkHttp, true);
        assertEquals("", "");
    }

    private void requestPerSecond(String endpoint, 
            TheRequest request, int count) throws Exception {
        MCSClient cl = UTILS.createDefaultFeedGet(request, login, this, endpoint);

//        MCSClient cl = UTILS.createDefaultMockedClient(login, this, request, endpoint);

        int step = 0;
        while (step < count) {
            ++step;
            logger.log(Level.INFO, "###REQUEST(count): " + step);
            try {
                cl.execute();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Thread.sleep(1000);
        }
    }

    private void compressRequestTest(int reqRounds, String log, 
            int concurrencyLevel, String endpoint,
            TheRequest request, 
            boolean enableMutichannels) throws Exception {
        
//        MCSClient cl = createDefaultFeedGet(request, endpoint);
        MCSClient cl = UTILS.createDefaultMockedClient(login, this, request, endpoint);
        new Benchmark2(cl, request, reqRounds, log, concurrencyLevel, enableMutichannels).run();
    }
    
    private void sequenceRequestTest(int reqCount, String log, String endpoint,
            TheRequest request) throws Exception {
        
//        MCSClient cl = createDefaultFeedGet(request, host);
        MCSClient cl = UTILS.createDefaultMockedClient(login, this, request, endpoint);
        new Benchmark1(cl, reqCount, log).run();
    }

    @Override
    public void arrives(MCSClient sender, String response) {
        System.out.println("##=>Mcs reponse arrives: " +  response);
    }
}
