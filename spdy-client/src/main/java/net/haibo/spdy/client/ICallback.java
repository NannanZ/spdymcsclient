package net.haibo.spdy.client;

/**
 * @author HAIBO
 * Define the call back action when mcs client receives a http repsonse.
 */
public interface ICallback {
    void arrives(MCSClient sender, String response);
}
