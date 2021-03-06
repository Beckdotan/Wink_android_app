package com.example.wink;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.net.URI;
import java.util.Calendar;

public class PhotoActivity extends AppCompatActivity {


    //uploading to firebase using using this videos:
    //https://www.youtube.com/watch?v=MfCiiTEwt3g&list=PLrnPJCHvNZuB_7nB5QD-4bNg6tpdEUImQ
    //uploading to local DB using https://www.youtube.com/watch?v=RGzblJuat1M&list=PLSrm9z4zp4mGK0g_0_jxYGgg3os9tqRUQ&index=2


    private static final int PICK_IMAGE_REQUEST =1;
    private Button mButtonChooseImage;
    private Button mSend;
    private EditText mImageTitel;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Uri mImageUri;

    private FirebaseStorage storage;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private StorageTask mUploadTask;
    public String ImgURL;


    private MaterialTimePicker picker;
    private Calendar calendar;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i( "PhotoActivity","created activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        //cosmetics -- heading the header
        getSupportActionBar().hide();


        mButtonChooseImage = findViewById(R.id.choose_image);

        mSend = findViewById(R.id.send_button);
        mImageTitel = findViewById(R.id.image_titel_text);
        mImageView = findViewById(R.id.image_view);
        mProgressBar = findViewById(R.id.progress_bar);

        storage = FirebaseStorage.getInstance();
        mStorageRef = storage.getReference("uploads");
        mDatabase = FirebaseDatabase.getInstance().getReference("Images");
     //   mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads");


        //creating bottom sheet:
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Bottom sheet dialog was created using:  https://www.youtube.com/watch?v=hfoXhiMTc0c
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(
                        PhotoActivity.this, R.style.BottomSheetDialogTheme
                );
                View bottomSheetView = LayoutInflater.from(getApplicationContext())
                        .inflate(
                                R.layout.upload_image_bottom_sheet_layout,
                                (LinearLayout) findViewById(R.id.uploadImageBottomSheetContainer)
                        );
                bottomSheetView.findViewById(R.id.schedule_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Schedule the message and sending it.
                        ShowTimePicker();
                        bottomSheetDialog.dismiss();
                    }
                });
                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
            }
        });




        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i( "PhotoActivity","clicked on chooseImageButton");
                //create new intent and preper it to send for opening new activity for choosing the window.
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                Log.i( "PhotoActivity","created intent");
                someActivityResultLauncher.launch(intent);

            }
        });


    }



    // -----------------------------------------------

    //                FUNCTIONS

    // -----------------------------------------------

    private void ShowTimePicker() {

       picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(10)
                .setTitleText("Select Notification Time")
                .build();
       picker.show(getSupportFragmentManager(), "timePicker");

       picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               calendar = Calendar.getInstance();
               calendar.set(Calendar.HOUR_OF_DAY, picker.getHour());
               calendar.set(Calendar.MINUTE, picker.getMinute());
               calendar.set(Calendar.SECOND, 0);
               calendar.set(Calendar.MILLISECOND, 0);

               if (mUploadTask != null && mUploadTask.isInProgress()){
                   Toast.makeText(PhotoActivity.this, "Upload In Progress", Toast.LENGTH_SHORT).show();
               }else {
                   uploadFile();
               }

           }

       });

    }

    // -----------------------------------------------

    //  creating new activity for the image choose window.
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.i( "PhotoActivity","in Result Launcher");
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        mImageUri  = data.getData();
                        //Load Image had to be in a function outside of the ActivityResult becouse of the "this" statment"
                        LoadImage();


                    }
                }
            });

    // -----------------------------------------------

    private void LoadImage(){
        Picasso.with(this).load(mImageUri).into(mImageView);
    }

    // -----------------------------------------------


    //getting the file extension (.jpg / .png) and such.
    private String getFileExtension(Uri uri){
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    // -----------------------------------------------

    //uplading the images to storage and creating new not in realtime db.
    private void uploadFile(){
        if (mImageUri != null){
            //uploading the image to storage with the name in milliseconds (thats how we use uniq name).
            String imgName = System.currentTimeMillis()+ "." + getFileExtension(mImageUri);
            StorageReference fileReference = mStorageRef.child(imgName);
            //fileReference.putFile(mImageUri);
            Log.i("fileRef path:", fileReference.getPath());

            mUploadTask = fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            //Cosmetics - after 5 seconds get the progress bur to 0 again.
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(0);
                                }
                            }, 5000);

                            //for debugging no need for it because after we saving in realtime db we are sending the message.
                            //Toast.makeText(PhotoActivity.this, "Upload was successful", Toast.LENGTH_LONG).show();

                            // --------------    FIRST WORKING WAY    -------------
                            UploadImage upload = new UploadImage(mImageTitel.getText().toString().trim(),  fileReference.getPath());
                            Log.i("Upload Image", "onSuccess: " + ImgURL);

                            //updating the showTime in upload.
                            upload.setShowTimeInMillis(calendar.getTimeInMillis() + "");

                            //saving the Image and matadata to realtime DB
                            String key = saveImageInDB(upload);
                            Log.i("Upload Image", "onSuccess: key " +key);

                            //if in realtime DB:
                            if (key == "0"){ // if failed in DB
                                Toast.makeText(PhotoActivity.this, "Upload was NOT successful", Toast.LENGTH_LONG).show();
                            } else { //if success in realtime DB
                                Intent intent = new Intent(PhotoActivity.this, AfterSendActivity.class);
                                startActivity(intent);
                                //Toast.makeText(PhotoActivity.this, "Great job! the note was sent!", Toast.LENGTH_LONG).show();

                                /*
                                //adding it to local DB:
                                SQLiteDBHelper myDB = new SQLiteDBHelper(PhotoActivity.this);
                                myDB.addImg(key, upload);

                                 */
                            }


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(PhotoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            mProgressBar.setProgress((int) progress);
                        }
                    });

        }else{
            Toast.makeText(this, "Please Select Image", Toast.LENGTH_LONG).show();

        }
    }

    // -----------------------------------------------

    //sending the img to DB.
    //if works - return the automatic key was given and send toast to user
    //else return "0" as didnt happen.  and send toast to user.
    public String saveImageInDB(UploadImage img) {

        // Write a message to the database
        Log.i("TAG", "saveImageInDB: ");
        try {
            //getting refrense to firebase realtime db.
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://wink-e1b43-default-rtdb.europe-west1.firebasedatabase.app");
            DatabaseReference myRef = database.getReference("Images/").push();
            //getting the new key from DB
            String key = myRef.getKey();

            //setting the key as param
            img.setId(key);
            //sending img to DB
            myRef.setValue(img);
            Log.i("saveNoteInDB", "saved in DB! :)");

            /// !!!!!!!!!!!! ----- Can add the note id to the user notes list here ----- !!!!!!!!!

            return key;
        } catch (Error e) {
            Log.e("saveNoteInDB", "didn't work");
            return "0";
        }

    }





}