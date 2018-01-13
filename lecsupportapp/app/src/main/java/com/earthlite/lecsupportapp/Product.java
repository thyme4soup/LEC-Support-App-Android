package com.earthlite.lecsupportapp;

import android.text.TextUtils;
import android.util.Log;

import java.util.Comparator;

/**
 * Created by Earthlite27 on 5/26/17.
 */

public class Product {
    /* Index:
    [0] -> Serial Number
    [1] -> Make
    [2] -> Model
    [3] -> Size
    [4] -> Install date
     */
    public final static String[] info_titles = new String[] {
            "Serial",
            "Description",
            "Ship Date",
            "Customer Number"
    };
    public final static String[] info_ids = new String[] {
            "Table Number",
            "Description",
            "Ship Date",
            "Customer_Number"
    };
    private String[] info = new String[Math.min(info_titles.length, info_ids.length)];
    public final static int SERIAL = 0;
    public final static int DESCRIPTION = 1;
    public final static int SHIP_DATE = 2;
    public final static int CUSTOMER_NUMBER = 3;
    public boolean loaded = false;
    public String name = "";

    public Product(String[] init) {
        switch(init.length) {
            case 2:
                name = init[0];
                info[SERIAL] = init[1];
                break;
            case 1:
                name = init[0];
                break;
            default:
                break;
        }
    }

    public Product(String serial, String name) {
        set(SERIAL, serial);
        this.name = name;
    }
    public Product(String serial) {
        set(SERIAL, serial);
        this.name = null;
    }

    public String toString() {
        String res = "{";
        for(int i = 0; i < Math.min(info.length, info_ids.length); i++) {
            if(i > 0) res += ",";
            res += "\"" + info_ids[i] + "\":\"" + info[i] + "\"";
        }
        return res + "}";
    }
    public String toSerial() {
        String result = name + MainScreen.INFO_REGEX + get(SERIAL);
        return result;
    }
    public String getTitle() {
        if(name != null && name.length() > 0) {
            return name + " - " + get(SERIAL);
        }
        else return get(SERIAL);
    }
    public String getInfo() {
        String res = "";
        for(int i = 0; i < Math.min(info.length, info_titles.length); i++, res += "\n") {
            res += info_titles[i] + ": " + info[i];
        }
        return res;
    }
    public String getAbbrInfo() {
        if(loaded) {
            return niceify(get(DESCRIPTION));
        } else return "Loading...";
    }
    public String[] getInternal() {
        return info;
    }
    public String get(int i) {
        String res = "";
        try {
            res = info[i];
            if(res == null) res = "";
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
        return res;
    }
    public String getLabeled(int i) {
        String res = "";
        try {
            res = info_titles[i] + ": " + info[i];
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }
    public void set(int i, String s) {
        try {
            info[i] = s;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static String niceify(String s) {
        String[] words = s.split(" ");
        for(int i = 0; i < words.length; i++) {
            words[i] = capWord(words[i]);
        }
        return TextUtils.join(" ", words);
    }
    public static String capWord(String w) {
        if(!isAbbr(w)) {
            w = w.toLowerCase();
            w = w.replaceFirst(("" + w.charAt(0)), ("" + w.charAt(0)).toUpperCase());
        } else {
        }
        return w;
    }
    public static boolean isAbbr(String w) {
        //Assuming abbreviations are defined by a lack of vowels
        char[] vowels = "AEIOUY".toCharArray();
        for(int i = 0; i < vowels.length; i++) {
            if(w.contains("" + vowels[i])) {
                return false;
            }
        }
        return true;
    }
    public static class ProductComparator implements Comparator<Product> {

        @Override
        public int compare(Product o1, Product o2) {
            return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
    public Product dupe() {
        Product p = new Product(this.get(Product.SERIAL), this.name);
        for(int i = 0; i < info.length; i++) {
            p.set(i, info[i]);
        }
        return p;
    }
}