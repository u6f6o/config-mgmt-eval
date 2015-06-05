package com.u6f6o.apps.cfgw.fetcher;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConsulConfigFetcher implements ConfigFetcher {
    private static final Logger LOGGER = Logger.getLogger(ConsulConfigFetcher.class);

    private final String consulClientIP;

    public ConsulConfigFetcher(String consulClientIP) {
        this.consulClientIP = consulClientIP;
    }

    @Override
    public Map<String, String> fetchLatestConfig() {
        try {
            GetRequest getRequest = Unirest.get("http://" + consulClientIP + ":8500/v1/kv/app/ps?recurse");
            JSONArray array = getRequest.asJson().getBody().getArray();
            Map<String, String> result = new HashMap<String, String>((int) Math.ceil(array.length() / 0.75));

            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                String key = jsonObject.getString("Key");
                String val = jsonObject.get("Value") + "";

                result.put(key, val);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Exception while fetching configuration.", e);
        }
        return null;
    }

}
