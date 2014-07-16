/**
 * 
 */
package net.haibo.spdy.client;

/**
 * @author HAIBO
 * It'a static context class of Mcs client.
 * You need change the values of some necessary properties
 */
public interface CONTEXT  {
    public String UDID = "dummydeviceid";
    public String API_KEY = "need API_KEY";
    public String SECRET_KEY = "need SECRET_KEY";
    public String API_VERSION = "1.0";
    public String API_ENDPOINT = "http://api.m.renren.com/api/";
    // 
    //public static string HOST = "https://mc2.test.renren.com/api/";
    // http://api.m.renren.com/api/
// host: 123.125.36.108 api.m.renren.com
    
    public String PUBLISH_DATA = "20140623";
    public String OS = "WINDOWS";
    public String APP_ID = "need APP_ID";
    public String APP_NAME = "JavaMcsClient";
    
    // Windows
    // NOTE: This config has been dropped, pls using <code>allowInsecure</code>.
//    public String CERTIFICATION_FILE_PATH = "D:/cert/jssecacerts";
    public String LOG_FILE_PATH = "d:/logs/";
    
    // Linux
    // NOTE: This config has been dropped, pls using <code>allowInsecure</code>.
//    public String CERTIFICATION_FILE_PATH = "/home/haibo-dev/cert/jssecacerts";
//    public String LOG_FILE_PATH = "/home/haibo-dev/logs/";
    
    public static class UserInfo {
        public String userName; public String password;
    }
    public static class MockSetting {
        public int length; public int waitTime;
    }

    /** Prepare for a dummied context with an account info for testing */
    public static final CONTEXT DUMMY = new CONTEXT() {
        @Override public UserInfo account() {
            UserInfo info = new UserInfo();
            info.userName = "need account"; info.password = "need password";
            return info;
        }
        @Override public MockSetting mockSetting() {
            MockSetting setting = new MockSetting();
            setting.length = 1000; setting.waitTime = 1;
            return setting;
        }
    };
    public UserInfo account();
    public MockSetting mockSetting();
} 
