/**
 * 
 */
package net.haibo.spdy.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Definition of mcs batch run response info entity.
 * It will be deserialized as an object by passing the 
 * json string to the constructor.
 * <p>
 * And it will never throw any exception during initialization, 
 * but you can check the validation through the 
 * {@linkplain BatchrunInfo#isValid()} method after construction.
 * 
 */
public class BatchrunInfo {
    private final static Gson GSON = new Gson();
    private Map<String, String> info = null;
    
    public BatchrunInfo(String json) {
        try {
            Map<String, String> target = new HashMap<String, String>();
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (JsonElement it : arr) {
                JsonObject item = it.getAsJsonObject();
                for (Entry<String, JsonElement> itt : item.entrySet()) {
                    target.put(itt.getKey(), itt.getValue().toString());
                }
            }
            setInfo(target);
        } catch(JsonSyntaxException ex) {
            System.err.println("BatchrunResp info parse ERROR! [raw data] : \n" + json);
            info = null;
        } catch (Throwable ex) {
            System.err.println("###!Should never come here ERROR! [raw data] : \n" + json);
            info = null;
        }
    }
    
    public Map<String, String> getInfo() {
        return info;
    }

    private void setInfo(Map<String, String> info) {
        this.info = info;
    }

    public boolean isValid() {
        return (info != null);
    }
}
