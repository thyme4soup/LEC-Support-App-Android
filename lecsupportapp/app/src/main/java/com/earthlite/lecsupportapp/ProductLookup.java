package com.earthlite.lecsupportapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by Earthlite27 on 5/26/17.
 */

public class ProductLookup extends AsyncTask<Void, Void, Void> {

    private boolean showLoad = false;
    private AppCompatActivity parent;
    ProgressDialog proDialog;
    Product product;
    boolean result = false;

    public ProductLookup(AppCompatActivity parent, Product p) {
        this.parent = parent;
        product = p;
    }
    public ProductLookup(AppCompatActivity parent, Product p, boolean showLoad) {
        this.product = p;
        this.parent = parent;
        this.showLoad = showLoad;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if(showLoad) {
            // Showing progress loading dialog
            proDialog = new ProgressDialog(parent);
            proDialog.setMessage("Finding Serial...");
            proDialog.setCancelable(false);
            proDialog.show();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        //try {
        //    Thread.sleep(2000);
        //} catch (Exception e) {}
        result = JsonReader.populateProduct(product);
        product.loaded = result;
        if(result) register();
        return null;
    }
    private void register() {
        try {
            String url_s = MainScreen.BASE_WS_URL + "register.php?serial="+product.get(Product.SERIAL)+"&email="+Service.user.getEmail();
            URL url = new URL(url_s);
            InputStream is = url.openStream();
            is.close();
        } catch (MalformedURLException mUE) {
            mUE.printStackTrace();
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
    }

    @Override
    protected void onPostExecute(Void requestresult) {
        super.onPostExecute(requestresult);
        // Dismiss the progress dialog
        if(showLoad) {
            if (proDialog.isShowing())
                proDialog.dismiss();
        }
        if(!result) {
            MainScreen.snack("Product lookup unsuccessful. Is this a valid barcode?", parent.findViewById(android.R.id.content), Snackbar.LENGTH_LONG);
        }
        ((OnPLCompleted) parent).onPLCompleted(result, product);
    }

    public interface OnPLCompleted {
        void onPLCompleted(boolean result, Product p);
    }
}
