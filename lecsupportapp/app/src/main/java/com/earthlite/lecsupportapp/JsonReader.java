package com.earthlite.lecsupportapp;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonReader {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url_s) throws JSONException {
        try {
            URL url = new URL(url_s);
            InputStream is = url.openStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                return json;
            } catch (Exception e) {
                return null;
            } finally {
                is.close();
            }
        } catch (MalformedURLException mUE) {
            mUE.printStackTrace();
            return null;
        } catch (IOException iOE) {
            iOE.printStackTrace();
            return null;
        }
    }

    public static JSONObject getJsonFeed(String url) throws IOException, JSONException {
        return readJsonFromUrl(url);
    }
    private static String stringGet(JSONObject jsonFeed, String id) throws JSONException, IOException {
        String get = (String) jsonFeed.get(id);
        return get;
    }
    /**
     * Takes product input and modifies it, returns boolean indicating success or failure
     */
    public static boolean populateProduct(Product p) {
        try {
            String url = MainScreen.BASE_WS_URL + "?serial=" + p.get(Product.SERIAL);
            JSONObject j = getJsonFeed(url);

            // Redefine p using info_ids. If more columns are needed they should be changed in info_ids
            for(int i = 0; i < Product.info_ids.length; i++) {
                p.set(i, stringGet(j, Product.info_ids[i]));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
