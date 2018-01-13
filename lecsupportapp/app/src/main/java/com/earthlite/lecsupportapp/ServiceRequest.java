package com.earthlite.lecsupportapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServiceRequest extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    static Product product;
    ArrayList<Uri> issueImages = new ArrayList<>();
    String issue_option = "Other";
    String mCurrentPhotoPath;
    Uri sent;
    iconAdapter icons_adapter;
    ListView icons_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        if (ContextCompat.checkSelfPermission(ServiceRequest.this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ServiceRequest.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 5);
        }

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        Button send = (Button) findViewById(R.id.send_button);
        send.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(product != null) {
                    sendEmail();
                }
                else {
                    Toast.makeText(ServiceRequest.this, "No product scanned", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button add_image = (Button) findViewById(R.id.add_image);
        add_image.setOnClickListener(image_selection);

        if(Service.product != null) {
            product = Service.product;
            Service.product = null;
        }
        if (product != null) {
            addInfo();
        } else {
            MainScreen.d("Service Request", "product is null, exiting");
            finish();
            MainScreen.snack("Error loading product", findViewById(android.R.id.content), Snackbar.LENGTH_LONG);
        }

        icons_adapter = new iconAdapter(this, R.layout.image_icon, issueImages);
        icons_adapter.setNotifyOnChange(true);
        icons_list = (ListView) findViewById(R.id.image_list);
        icons_list.setAdapter(icons_adapter);
        icons_list.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                removeImage(position, view);
            }
        });

        Spinner spinner = (Spinner) findViewById(R.id.issue_spinner);
        ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.issue_options, R.layout.spinner_text);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinner_adapter);

    }

    public Button.OnClickListener image_selection = new Button.OnClickListener () {
        public void onClick(View view) {
            selectImage();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case 1:
                    addImage(Uri.parse(mCurrentPhotoPath));
                    mCurrentPhotoPath = null;
                    break;
                case 2:
                    Uri selectedImage = data.getData();
                    String[] filePath = { MediaStore.Images.Media.DATA };
                    Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                    c.moveToFirst();
                    int columnIndex = c.getColumnIndex(filePath[0]);
                    String picturePath = c.getString(columnIndex);
                    addImage(Uri.parse(picturePath));
                    break;
                case 3:
                    finish();
                    product = null;
                    break;
            }
        }
        if(resultCode == RESULT_CANCELED) {
        }
    }

    private void sendEmail() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = MainScreen.getEmailIntent(
                        getBaseContext(),
                        product,
                        ((Spinner) findViewById(R.id.issue_spinner)).getSelectedItem().toString(),
                        ((EditText) findViewById(R.id.issue)).getText(),
                        issueImages);
                startActivityForResult(Intent.createChooser(intent, "Send email..."), 3);
            }
        });
    }

    private void selectImage() {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(ServiceRequest.this);
        builder.setTitle("Add a photo of the issue");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo"))
                {
                    if (ContextCompat.checkSelfPermission(ServiceRequest.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ServiceRequest.this,
                                new String[]{Manifest.permission.CAMERA}, 6);
                    } else {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Ensure that there's a camera activity to handle the intent
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            // Create the File where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException ex) {
                                // Error occurred while creating the File
                                ex.printStackTrace();
                            }
                            // Continue only if the File was successfully created
                            if (photoFile != null) {
                                Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), "com.earthlite.asd.fileprovider", photoFile);
                                //Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                //        "com.earthlite.asd.fileprovider",
                                //        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                                sent = photoUri;
                                startActivityForResult(takePictureIntent, 1);
                            }
                        }
                    }
                }
                else if (options[item].equals("Choose from Gallery"))
                {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                }
                else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void addImage(Uri f) {
        File file = new File(f.getPath());
        if(file.exists()) {
            issueImages.add(f);
            icons_adapter.notifyDataSetChanged();
        }
        else {
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void addInfo() {
        TextView title = (TextView) this.findViewById(R.id.request_title);
        String p_title = product.getTitle();
        title.setText(p_title);
        this.findViewById(R.id.request_card).invalidate();
    }

    private void removeImage(final int index, final View parent) {

        final Uri removal = issueImages.get(index);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        icons_adapter.remove(removal);
                        Toast.makeText(parent.getContext(),
                                "Removed "+removal.getLastPathSegment(),
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
        builder.setMessage("Remove \"" + removal.getLastPathSegment() + "\" from the image list?")
                .setPositiveButton("Remove", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener)
                .show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        issue_option = (String) parent.getItemAtPosition(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class iconAdapter extends ArrayAdapter<Uri> {

        public iconAdapter(Context context, int resource, List<Uri> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int pos, View view, ViewGroup parent){
            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.image_icon, parent, false);
            }

            Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(issueImages.get(pos).getEncodedPath()), 200, 200);
            if(resized != null) {
                ImageView iv = (ImageView) view.findViewById(R.id.image_icon);
                iv.setImageBitmap(resized);
            }
            ((TextView) view.findViewById(R.id.file_name)).setText(issueImages.get(pos).getLastPathSegment());
            return view;
        }
    }
}
