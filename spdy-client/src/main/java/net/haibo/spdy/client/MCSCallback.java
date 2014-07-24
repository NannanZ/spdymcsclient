package net.haibo.spdy.client;

/**
 * Define the call back action when mcs client receives a http repsonse.
 */
public interface MCSCallback {
    void arrives(MCSClient sender, String response);
}
