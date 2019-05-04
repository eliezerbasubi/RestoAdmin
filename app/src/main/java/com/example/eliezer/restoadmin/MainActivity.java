package com.example.eliezer.restoadmin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    Uri imageUri;
    Uri resultUri;
    private byte[] thumb_byte = new byte[0];
    private File thumb_filePath;
    private static final int PICK_IMAGE_REQUEST =1;
    private ProgressDialog progressDialog;

    private ImageButton btnImage;
    private EditText edtName,edtPrice;
    private Button addFood,seeFood;
    private ImageView display;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Food");
        mStorage = FirebaseStorage.getInstance().getReference();

        btnImage = findViewById(R.id.food_image);
        edtName = findViewById(R.id.food_name);
        edtPrice = findViewById(R.id.food_price);
        display = findViewById(R.id.display_img);
        addFood = findViewById(R.id.add_food);
        seeFood = findViewById(R.id.see_orders);

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), PICK_IMAGE_REQUEST);
            }
        });

        addFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stName = edtName.getText().toString();
                String stPrice = edtPrice.getText().toString();

                UploadImages(stPrice,stName);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageUri = data.getData();

            CropImage.activity(imageUri)
                    //.setAspectRatio(8, 8)
                    //.setMinCropWindowSize(800, 800)
                    .start(this);

            thumb_filePath = new File(imageUri.getPath());
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();

                thumb_filePath = new File(resultUri.getPath());

                //IF CROPPING IT'S OKAY, IT SHOULD DISPLAY THE IMAGE


                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                    display.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                thumb_byte = baos.toByteArray();

            }
        }
    }

    public void UploadImages(final String food_name,
                             final String food_price){
        final String push_id = mDatabase.push().getKey().toString();
        final StorageReference riversRef = mStorage.child("food_images").child(push_id+".jpg");

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Upload Articles");
        progressDialog.setMessage("Wait until the uploading process is done");
        progressDialog.show();

        if (resultUri != null){

            UploadTask thumbTask = riversRef.putBytes(thumb_byte);
            thumbTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {
                    if (thumb_task.isSuccessful()){
                        final String thumb_url = thumb_task.getResult().getDownloadUrl().toString();

                        Map<String ,String> map = new HashMap<>();
                        map.put("price",food_price);
                        map.put("image",thumb_url);
                        map.put("name",food_name.toLowerCase());

                        mDatabase.push().setValue(map).addOnCompleteListener(new OnCompleteListener <Void>() {
                            @Override
                            public void onComplete(@NonNull Task <Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Food successfully added", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();

                                   // clearFields();
                                }else{
                                    Toast.makeText(MainActivity.this, "Failed to add", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    Log.i("WORK", "insertion success");
                                }
                            }
                        });
                    }else{
                        Toast.makeText(MainActivity.this, "Thumbnail image not uploaded", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            Toast.makeText(this, "Image uri is null", Toast.LENGTH_SHORT).show();
        }
    }

}
