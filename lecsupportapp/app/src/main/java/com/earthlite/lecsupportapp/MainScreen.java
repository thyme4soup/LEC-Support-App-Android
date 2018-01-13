package com.earthlite.lecsupportapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;


public class MainScreen extends AppCompatActivity {

    // Network and contact constants
    final static String BASE_WS_URL = "************";
    final static String SUPPORT_PHONE_NUMBER = "************";
    final static String[] SUPPORT_EMAIL_RECIPIENT = new String[] {"************"};

    // Email structure constants
    final static String SUBJECT_HEADER = "[Support Request] ";
    final static String BODY_TEMPLATE =
            "[Support Request]\n" +
            "Issue Type: %s\n" +
            "Description:\n%s\n\n" +
            "[Product Info]\n" +
            "%s\n" +
            "[Customer Info]\n" +
            "%s\n";

    // Serial constants
    final static String LIST_REGEX = "/";
    final static String INFO_REGEX = "|";
    final static String USER_SERIAL_KEY = "USER";
    final static String PRODUCT_SERIAL_KEY = "PRODUCT";

    final static boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        getSupportActionBar().hide();


        Button support = (Button) findViewById(R.id.s_button);
        support.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainScreen.this, Service.class);
                startActivity(intent);
            }
        });

        Button products = (Button) findViewById(R.id.p_button);
        products.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainScreen.this, MyProducts.class);
                startActivity(intent);
            }
        });

        Button web = (Button) findViewById(R.id.w_button);
        web.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Open website
                                String url = "https://www.livingearthcrafts.com";
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                                break;


                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Would you like to go to the LEC website?")
                        .setPositiveButton("Go", dialogClickListener)
                        .setNegativeButton("Cancel", dialogClickListener)
                        .show();
            }
        });
        support.setMinimumWidth(300);
        products.setMinimumWidth(300);
        web.setMinimumWidth(300);
        // Load user is called in anticipation of Products -> Service Request skipping Service
        Service.loadUser(getApplicationContext());
    }

    // Intent shorthands
    public static Intent getEmailIntent(Context c, Product p, String issue_type, Editable issue_description, ArrayList<Uri> images) {

        String body = String.format(BODY_TEMPLATE,
                issue_type,
                issue_description,
                p.getInfo(),
                Service.user.getInfo());
        d("Email Intent", "Body: " + body);
        ArrayList<Uri> shared = new ArrayList<>();
        for(int i = 0; i < images.size(); i++) {
            shared.add(FileProvider.getUriForFile(c, "com.earthlite.asd.fileprovider", new File(images.get(i).getPath())));
            Log.println(Log.DEBUG, "Email Intent", "image uri: " + images.get(i).toString());
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, SUPPORT_EMAIL_RECIPIENT);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, SUBJECT_HEADER + p.get(Product.DESCRIPTION));
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, shared);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        return emailIntent;
    }
    public static Intent getPhoneIntent() {
        Intent phone = new Intent(Intent.ACTION_DIAL);
        phone.setData(Uri.parse(SUPPORT_PHONE_NUMBER));
        return phone;
    }

    public static String getValueName(String s) {
        return s.toLowerCase().replace(' ', '_');
    }
    public static String removeSpecialChars(String s) {
        return s.replace(LIST_REGEX, "").replace(INFO_REGEX, "");
    }

    // Debug shorthand
    public static void d(String title, String info) {
        if(DEBUG) {
            Log.println(Log.DEBUG, title, info);
        }
    }
    // Snack shorthand
    public static void snack(String text, View v, int length) {
        Snackbar snack = Snackbar.make(v,
                Html.fromHtml("<font color=\"#ffffff\">" + text + "</font>"),
                length);
        snack.show();
    }
}
