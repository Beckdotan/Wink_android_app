package com.example.wink;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;

public class SQLiteDBHelper extends SQLiteOpenHelper {


    //was created using https://www.youtube.com/watch?v=RGzblJuat1M&list=PLSrm9z4zp4mGK0g_0_jxYGgg3os9tqRUQ&index=2

    //for DB purpeses.
    private static final String TAG = "LOCAL DB: ";
    private Context context;
    public static final String DATABASE_NAME = "WinksDB.db";
    public static final int DATABASE_VERSION = 1;


    public static final String TABLE_NAME = "my_winks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "img_title";
    public static final String COLUMN_LINK = "img_link";
    public static final String COLUMN_SHOWED = "was_showed"; // will be 0 for no and 1 for yes.
    public static final String IMG  = "img";
    public static final String SHOW_TIME = "show_time"; // will be treated as String

    //other things

    public SQLiteDBHelper thisContext = this;
    public int ImgHeight = 1000;
    public int ImgWidth = 2000;


    //constructor
    public SQLiteDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {



        String query = "CREATE TABLE " + TABLE_NAME +
                        " (" + COLUMN_ID + " TEXT, " +
                        COLUMN_TITLE +  " TEXT, " +
                        COLUMN_LINK +  " TEXT, " +
                        IMG + " BLOB, " +
                        COLUMN_SHOWED + " INTEGER, " +
                        SHOW_TIME + " TEXT); ";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);

    }


    public String addImg(String key, UploadImage uploadImage){

        //using this API https://github.com/nostra13/Android-Universal-Image-Loader

        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context).build();
        ImageLoader.getInstance().init(config);

        ImageLoader imageLoader = ImageLoader.getInstance();
        ImageSize targetSize = new ImageSize(ImgWidth, ImgHeight);

        // Load image, decode it to Bitmap and return Bitmap to callback
         imageLoader.loadImage(uploadImage.getImageUrl(), targetSize, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                // Do whatever you want with Bitmap
                byte[] currentImg;
                currentImg = getBytesFromBitmap(loadedImage);
                Log.i(TAG, "onLoadingComplete: Created byte[] from img");

                //adding to local DB
                SQLiteDatabase db = thisContext.getWritableDatabase();
                ContentValues cv = new ContentValues();
                //adding vals to cv
                cv.put(COLUMN_ID, key);
                cv.put(COLUMN_TITLE, uploadImage.getImageName());
                cv.put(COLUMN_LINK, uploadImage.getImageUrl());
                cv.put(COLUMN_SHOWED, uploadImage.getWasShown());
                cv.put(IMG, currentImg);
                Log.e(TAG, "onLoadingComplete: " + uploadImage.getShowTimeInMillis());
                cv.put(SHOW_TIME, (uploadImage.getShowTimeInMillis() + "")); // getting the time in millis as String;

                //checking if succeeded.
                long result = db.insert(TABLE_NAME,null, cv);
                if (result == -1){
                    Log.e(TAG, "addImg: FAILED TO SAVE IMG");
                }else{
                    Log.i(TAG, "addImg: Img was saved to local db");
                }
            }
        });
         //TODO: to the caller for this function there is no way knowing if this didn't succeed for now. need to add that.
        Log.i(TAG, key);
        return key;
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        if (bitmap!=null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return stream.toByteArray();
        }
        return null;
    }

    Cursor readAllData(){
        String query = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        if(db != null){
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }


    public Boolean isInLocalDB(String noteId){
        String Query = "Select * from " + TABLE_NAME + " where " + SQLiteDBHelper.COLUMN_ID + " = " + "'" + noteId + "'";
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor cursor = database.rawQuery(Query, null);
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

}
