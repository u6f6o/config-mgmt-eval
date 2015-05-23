package com.u6f6o.apps.cfgw.provider.consul;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.u6f6o.apps.cfgw.provider.ConfigFetcher;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConsulConfigFetcher implements ConfigFetcher {
    private static final String KEY_VALUE_STORE_URL = "http://192.168.59.103:8501/v1/kv/?recurse";

    @Override
    public Map<String, String> fetchLatestConfig() {
        try {
            GetRequest getRequest = Unirest.get(KEY_VALUE_STORE_URL);
            JSONArray array = getRequest.asJson().getBody().getArray();
            Map<String, String> result = new HashMap<String, String>((int) Math.ceil(array.length() / 0.75));

            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                String key = jsonObject.getString("Key");
                String val = jsonObject.getString("Value");

                result.put(key, val);
            }
            return result;
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

}
