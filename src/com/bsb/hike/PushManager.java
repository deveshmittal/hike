package com.bsb.hike;

import java.net.URISyntaxException;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.bsb.hike.utils.AccountUtils;

public class PushManager extends Thread {

    private MQTT mqtt;
    private volatile boolean finished;

    public PushManager() throws URISyntaxException {
        mqtt = new MQTT();
        mqtt.setHost(AccountUtils.HOST, 1883);
        mqtt.setKeepAlive((short) (60*5));
        mqtt.setWillTopic("connection_terminated");
    }

    private BlockingConnection connect() {
        BlockingConnection connection;
        while (true) {
            try {
                Log.d("PushManager","creating blockingConnection");
                connection = mqtt.blockingConnection();
                Log.d("PushManager","connecting");
                connection.connect();
                Log.d("PushManager","subscribing to topics");
                Topic[] topics = {new Topic("foo", QoS.AT_LEAST_ONCE)};
                byte[] qos = connection.subscribe(topics);
                Log.d("PushManager","subscribed");
                return connection;
            } catch (Exception e) {
                Log.e("PushManager", "trying to connect", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        Log.d("PushManager", "Making connection");
        BlockingConnection connection = connect();
        Log.d("PushManager", "Connection made");
        while (!finished) {
            Message message;
            try {
                Log.d("PushManager", "receiving message");
                message = connection.receive();
                Log.d("PushManager", "message received");
            } catch (Exception e) {
                connection = connect();
                continue;
            }

            byte[] payload = message.getPayload();
            String str = new String(payload);
            try {
                JSONObject obj = new JSONObject(str);
                System.out.println("PushManager.run() -- json received:" + obj.toString());
            } catch (JSONException e) {
                Log.e("PushManager", "invalid JSON Message", e);
            }
            message.ack();
        }
    }

}
