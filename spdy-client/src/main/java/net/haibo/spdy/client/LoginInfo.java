/**
 * 
 */
package net.haibo.spdy.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * @author HAIBO 
 * 
 * Definition of login certification info entity.
 * It will be deserialized as an object by passing the 
 * json string to the constructor.
 * 
 * And it will never throw any exception during initialization, 
 * but you can check the validation through the 
 * <code>isValid</code> method after construction.
 * 
 */
public class LoginInfo {
    private static final Gson GSON = new Gson();
    private static final TypeToken<Info> INFO = new TypeToken<Info>() { };
    
    public static class Info {
        String session_key; String ticket; String vip_icon_url; String web_ticket;
        int uid; String secret_key; String user_name; String head_url;
        long now; int login_count; int int32; int is_guide; String vip_url;
    }
    private Info info = null;
    
    public LoginInfo(String json) {
        try {
            this.setInfo((Info)GSON.fromJson(json, INFO.getType()));
        } catch(JsonSyntaxException ex) {
            System.err.println("Login info parse ERROR! [raw data] : \n" + json);
            info = null;
        } catch (Throwable ex) {
            System.err.println("###!Should never come here ERROR! [raw data] : \n" + json);
            info = null;
        }
    }
    
    public boolean isValid() {
        return (info != null);
    }

    public Info getInfo() {
        return info;
    }

    private void setInfo(Info info) {
        this.info = info;
    }
}
