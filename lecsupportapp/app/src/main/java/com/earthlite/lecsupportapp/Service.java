package com.earthlite.lecsupportapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class Service extends AppCompatActivity implements ProductLookup.OnPLCompleted {

    public static Product product;
    private final static String[] user_fields = new String[] {"Name", "Email", "Phone", "Organization"};
    static User user = new User();
    private final static boolean[] requirements = new boolean[] {true, true, true, false};

    fieldAdapter fields_adapter;
    ArrayList<String> fields_arraylist = new ArrayList<String>();
    ListView fields_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        Button get_item = (Button) findViewById(R.id.request_support);
        get_item.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Product Select");
                builder.setItems(new CharSequence[]
                                {"Scan Item", "Select from Products", "Cancel"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int button) {
                                switch (button) {
                                    case 0:
                                        new IntentIntegrator(Service.this).initiateScan();
                                        break;
                                    case 1:
                                        Intent intent = new Intent(Service.this, MyProducts.class);
                                        startActivity(intent);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                if(user.isValid()) {
                    builder.create().show();
                } else prompt();
            }
        });

        Button call = (Button) findViewById(R.id.call);
        call.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                startActivity(MainScreen.getPhoneIntent());
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //Do nothing
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Call LEC Support line?").setPositiveButton("Continue", dialogClickListener)
                        .setNegativeButton("Cancel", dialogClickListener)
                        .show();
            }
        });

        loadUser(getApplicationContext());

        fields_adapter = new fieldAdapter(this, R.layout.user_field_list, fields_arraylist);
        fields_adapter.setNotifyOnChange(true);
        fields_list = (ListView) findViewById(R.id.user_field_list);
        fields_list.setAdapter(fields_adapter);

        populateListView();
        if(product != null) {
            openRequestCard();
        }
    }

    @Override
    public void onPLCompleted(boolean result, Product p) {
        if(result) {
            openRequestCard();
        }
        else product = null;
    }

    private void openRequestCard() {
        if(user != null && user.isValid()) {
            Intent intent = new Intent(Service.this, ServiceRequest.class);
            intent.putExtra(MainScreen.USER_SERIAL_KEY, user.getSerial());
            startActivity(intent);
        } else {
            //Toast.makeText(getBaseContext(), "User Information card incomplete", Toast.LENGTH_LONG).show();
            prompt();
            product = null;
        }
    }
    private void prompt() {
        MainScreen.snack("User Information card incomplete", findViewById(android.R.id.content), Snackbar.LENGTH_LONG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_SHORT).show();
                //Remove button, add info
                String serial = result.getContents();
                if(MyProducts.exists(serial)) {
                    product = MyProducts.getProduct(serial);
                    openRequestCard();
                } else {
                    product = new Product(serial, "");
                    ProductLookup pL = new ProductLookup(this, product);
                    pL.execute();
                }
            }
        } else {
            //Do nothing
        }
    }

    public static class User {

        private String[] info;

        public User() {
            info = new String[user_fields.length];
            for(int i = 0; i < info.length; i++) {
                info[i] = "";
            }
        }
        public User(String serial) {
            this();
            String[] input = serial.split("\\" + MainScreen.INFO_REGEX);
            for(int i = 0; i < Math.min(input.length, info.length); i++) {
                update(i, input[i]);
            }
        }
        public boolean isValid() {
            for(int i = 0; i < Math.min(info.length, requirements.length); i++) {
                if(requirements[i] && info[i].equals("")) return false;
            } return true;
        }
        public String getSerial() {
            String res = "";
            for(int i = 0; i < info.length; i++) {
                if(i > 0) res += MainScreen.INFO_REGEX;
                res += info[i];
            }
            return res;
        }
        public String getEmail() {
            return get(1);
        }
        public String get(int id) {
            String s = "";
            try {
                s = info[id];
            } catch(Exception e) {}
            return s;
        }
        public void update(int id, String input) {
            if(input != null) info[id] = input;
            else info[id] = null;
        }
        public String getInfo() {
            String res = "";
            for(int i = 0; i < Math.min(info.length, user_fields.length); i++, res += "\n") {
                res += user_fields[i] + ": " + info[i];
            }
            return res;
        }
    }

    public static boolean loadUser(Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        String user_string = sharedPref.getString(MainScreen.USER_SERIAL_KEY, "");
        if(!user_string.equals("")) {
            user = new User(user_string);
        }
        return user.isValid();
    }

    @Override
    //Save products to SharedPreferences by converting to String
    protected void onStop() {
        super.onStop();
        if(user != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MainScreen.USER_SERIAL_KEY, user.getSerial());
            editor.commit();
        }
    }

    public void populateListView() {
        for(int i = 0; i < user_fields.length; i++) {
            fields_adapter.add(user_fields[i]);
        }
        fields_adapter.notifyDataSetChanged();
    }

    private class fieldAdapter extends ArrayAdapter<String> {


        public fieldAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int pos, View view, ViewGroup parent){
            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.user_field_list, parent, false);
                TextView title = (TextView) view.findViewById(R.id.field_header);
                EditText info = (EditText) view.findViewById(R.id.field_edit);

                String title_string = user_fields[pos] + ":";
                String field_hint = user_fields[pos];
                if(requirements[pos]) field_hint += "*";

                title.setText(title_string);
                info.setHint(field_hint);

                if(!user.get(pos).equals(""))
                    info.setText(user.get(pos));

                info.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                        user.update(pos, MainScreen.removeSpecialChars(s.toString()));
                    }
                });
            }
            return view;
        }
    }
}
