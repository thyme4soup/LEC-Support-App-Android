package com.earthlite.lecsupportapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyProducts extends AppCompatActivity implements ProductLookup.OnPLCompleted {

    final static int NAME_MAX_LENGTH = 25;
    static ArrayList<Product> products = new ArrayList<>();
    final private String PROD_SERIAL_KEY = "MY_PRODUCTS";
    customAdapter adapter;
    public ListView listv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        // Define and show back button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        // Define products list
        adapter = new customAdapter(this, R.layout.product_list, products);
        adapter.setNotifyOnChange(true);
        listv = (ListView) findViewById(R.id.product_list);
        listv.setMinimumHeight(30);
        listv.setAdapter(adapter);
        listv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
                builder.setTitle(products.get(position).getTitle());
                builder.setItems(new CharSequence[]
                                {"Rename", "Request Support", "Delete", "Cancel"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int button) {
                                switch (button) {
                                    case 0:
                                        rename(products.get(position));
                                        break;
                                    case 1:
                                        if(Service.user.isValid() || Service.loadUser(MyProducts.this)) {
                                            Intent support_request = new Intent(getBaseContext(), ServiceRequest.class);
                                            Service.product = products.get(position);
                                            startActivity(support_request);
                                        } else {
                                            Intent support = new Intent(getBaseContext(), Service.class);
                                            Service.product = products.get(position);
                                            startActivity(support);
                                        }
                                        break;
                                    case 2:
                                        removeProduct(position, parent);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                if(products.get(position).loaded)
                    builder.create().show();
            }
        });

        // Hide filler message (loadProducts will display it again if there aren't any to load)
        if(products.size() == 0) {
            loadProducts();
        }
        else {
            //(This isn't the first time MyProducts has been opened, products are already loaded)
            findViewById(R.id.filler_message).setVisibility(View.GONE);
        }

        // fab acts as our product adder
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Handle product addition
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Scan, add item
                                new IntentIntegrator(MyProducts.this).initiateScan();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                createProductWithSerialInput();
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Do you have the product's barcode available?").setPositiveButton("Scan Item", dialogClickListener)
                        .setNegativeButton("Manual Input", dialogClickListener)
                        .setCancelable(true).show();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                addProduct(result.getContents());
            }

        } else {
            //Do nothing
        }
    }

    public static boolean exists(Product p) {
        for(int i = 0; i < products.size(); i++) {
            if(products.get(i).get(Product.SERIAL).equals(p.get(Product.SERIAL))) return true;
        }
        return false;
    }
    public static boolean exists(String serial) {
        for(int i = 0; i < products.size(); i++) {
            if(products.get(i).get(Product.SERIAL).equals(serial)) return true;
        }
        return false;
    }
    private void removeProduct(int position, final View parent) {
        final Product removal = products.get(position);

        // Make sure user wants to remove product
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        adapter.remove(removal);
                        if(products.size() == 0) findViewById(R.id.filler_message).setVisibility(View.VISIBLE);
                        Toast.makeText(parent.getContext(),
                                "Removed "+removal.get(Product.SERIAL),
                                Toast.LENGTH_SHORT)
                                .show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //Do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
        builder.setMessage("Remove \"" + removal.getTitle() + "\" from My Products?").setPositiveButton("Remove", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener)
                .show();
    }

    private void addProduct(final Product p) {
        if(!exists(p)) {
            // This had to be run on the UI thread but I forget why. It's probably the filler message
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.add(p);
                    //remove filler if still visible
                    if (MyProducts.this.findViewById(R.id.filler_message).getVisibility() == View.VISIBLE) {
                        MyProducts.this.findViewById(R.id.filler_message).setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    //Uses ProductLookup to find product info and add to list
    private void addProduct(String serial, String name) {
        if(!exists(serial)) {

            Product p = new Product(serial, name);
            addProduct(p);
            ProductLookup pL = new ProductLookup(this, p);

            //execute ProductLookup, which will callback onPLCompleted
            pL.execute();
        }
        else {
            Toast.makeText(getBaseContext(), "Already in My Products: "+serial, Toast.LENGTH_SHORT).show();
        }
    }
    private void rename(final Product p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyProducts.this);
        builder.setTitle("Add a personal identifier");
        // Set up the input
        final EditText input = new EditText(MyProducts.this);
        input.setText(p.name);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(NAME_MAX_LENGTH)});
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString();
                //Sanitize name
                name.replace(MainScreen.LIST_REGEX, "").replace(MainScreen.INFO_REGEX, "");
                p.name = name;
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void addProduct(final String serial) {
        if(!exists(serial)) {
            final Product p = new Product(serial);
            addProduct(p);
            ProductLookup pL = new ProductLookup(this, p);
            pL.execute();
        } else {
            MainScreen.snack("Product already exists!", findViewById(android.R.id.content), Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    //ProductLookup callback, add the product using result
    public void onPLCompleted(boolean result, Product p) {
        Log.println(Log.DEBUG, "MyProducts", "product lookup returned: " + result);
        if(result) {
            adapter.notifyDataSetChanged();
            if(p.name == null)
                rename(p);
        } else {
            adapter.remove(p);
        }
    }

    //Convert to string for easy storage and retrieval
    private String getProductsAsString() {
        String result = "";
        for(int i = 0; i < products.size(); i++) {
            if(i > 0) result += MainScreen.LIST_REGEX;
            result += products.get(i).toSerial();
        }
        return result;
    }

    private void createProductWithSerialInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MyProducts.this);
        builder.setTitle("Please enter the serial number");
        // Set up the input
        final EditText input = new EditText(MyProducts.this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String serial = input.getText().toString();
                //Sanitize serial
                serial.replace(MainScreen.LIST_REGEX,"").replace(MainScreen.INFO_REGEX,"");
                addProduct(serial);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    //Retrieve products from SharedPreferences, decode using regexes
    private void loadProducts() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String products_string = sharedPref.getString(PROD_SERIAL_KEY, "");
        if(products_string != "") {
            String[] products_array = products_string.split("\\" + MainScreen.LIST_REGEX);
            for (int i = 0; i < products_array.length; i++) {
                if(products_array[i] != "") {
                    String[] info = products_array[i].split("\\" + MainScreen.INFO_REGEX);
                    addProduct(info[1], info[0]);
                }
            }
            if (products.size() > 0) {
                findViewById(R.id.filler_message).setVisibility(View.GONE);
            }
        }
    }

    @Override
    //Save products to SharedPreferences by converting to String
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PROD_SERIAL_KEY, getProductsAsString());
        editor.commit();
    }

    public static Product getProduct(String serial) {
        for(int i = 0; i < products.size(); i++) {
            if(products.get(i).get(Product.SERIAL).equals(serial)) return products.get(i);
        }
        return null;
    }

    private class customAdapter extends ArrayAdapter<Product> {


        public customAdapter(Context context, int resource, List<Product> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int pos, View view, ViewGroup parent){
            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.product_list, parent, false);
            }
            TextView title = (TextView) view.findViewById(R.id.internal_title);
            TextView info = (TextView) view.findViewById(R.id.internal_info);
            title.setText(products.get(pos).getTitle());
            info.setText(products.get(pos).getAbbrInfo());
            return view;
        }
        @Override
        public void notifyDataSetChanged() {
            Collections.sort(products, new Product.ProductComparator());
            super.notifyDataSetChanged();
        }
    }
}
