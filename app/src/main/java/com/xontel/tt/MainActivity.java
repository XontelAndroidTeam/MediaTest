package com.xontel.tt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    ImageView ivBackGround;
    Button btnSave;
    Button btnView;
    Uri collection;
    String[] projection = new String[] {
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DURATION,
            MediaStore.Images.Media.SIZE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivBackGround = findViewById(R.id.imageView);
        btnSave = findViewById(R.id.btnSaveImage);
        btnView = findViewById(R.id.btnvIEWImage);

       // Log.i("TAG", "d: "+this.getFilesDir()); // /data/user/0/com.xontel.tt/files
       // Log.i("TAG", "d: "+Environment.getExternalStorageDirectory()); // /storage/emulated/0


        // Log.i("TAG", "d: "+Environment.getDownloadCacheDirectory()); // /data/cache
        // Log.i("TAG", "d: "+this.getCacheDir()); // /data/user/0/com.xontel.tt/cache
        // Log.i("TAG", "d: "+this.getExternalCacheDir()); // /storage/emulated/0/Android/data/com.xontel.tt/cache

       // Log.i("TAG", "d: "+this.getExternalMediaDirs()); // [Ljava.io.File;@437b317


        btnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAllPics();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BitmapDrawable bitmapDrawable = (BitmapDrawable) ivBackGround.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();
                saveImageWithoutResolver(bitmap);
               // saveBitmap(MainActivity.this,bitmap,Bitmap.CompressFormat.PNG,"hey.png");
               // saveImageToGalleryUsingMedia(bitmap);
                //saveImageToGalleryUsingApp(bitmap);
            }
        });
    }

    private void saveImageToGalleryUsingMedia(Bitmap bitmap) {
        OutputStream fos;

        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues =  new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_" + ".png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + "Testko");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues); // Without path -> default set in Pic / Vide /  Audio

                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Objects.requireNonNull(fos);

               Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            }

        }catch(Exception e){
            Log.e("TAG", "saveImageToGallery: "+e.getMessage());
            Toast.makeText(this, "Image not saved \n" + e.toString(), Toast.LENGTH_SHORT).show();
        }


    }

    private List<Uri> getContentUris(@NonNull final Context context) {

        final List<String> allVolumes = new ArrayList<>();

        // Add the internal storage volumes as last resort.
        // These will be kept at the bottom of the list if
        // any SD-card volumes are found
        allVolumes.add(MediaStore.VOLUME_EXTERNAL_PRIMARY);
//        allVolumes.add(MediaStore.VOLUME_EXTERNAL);

        // Obtain the list of volume name candidates

        final Set<String> externalVolumeNames = MediaStore.getExternalVolumeNames(context);

        for ( String entry : externalVolumeNames) {
            Log.i("TAG", "getContentUris: "+entry);
            // If the volume is "not" already cached in the list,
            // then is an SD-card, so prioritize it by adding it
            // at the top of the list
            if (!allVolumes.contains(entry))
                allVolumes.add(0, entry);
        }

        // Finally resolve the target Image content Uris

        final List<Uri> output = new ArrayList<>();

        for (final String entry : allVolumes) {
            Log.i("TAG", "allVolumes: "+entry);
            output.add(MediaStore.Images.Media.getContentUri(entry));
        }

        return output;
    }



    @Nullable
    public Uri saveBitmap(@NonNull final Context context, @NonNull final Bitmap bitmap,
                          @NonNull final Bitmap.CompressFormat format,
                          @NonNull final String displayName) {

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

        final ContentResolver resolver = context.getContentResolver();
        final List<Uri> contentUriList = getContentUris(context);

        for ( Uri contentUri : contentUriList) {
            Log.i("TAG", "saveBitmap: "+contentUri);
            Uri uri = null;

            try {
                uri = resolver.insert(contentUri, values);

                if (uri == null)
                    throw new IOException("Failed to create new MediaStore record.");

                try (final OutputStream stream = resolver.openOutputStream(uri)) {
                    if (stream == null)
                        throw new IOException("Failed to open output stream.");

                    if (!bitmap.compress(format, 95, stream))
                        throw new IOException("Failed to save bitmap.");
                }
                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                Log.w("TAG", "Failed to save in volume: " + contentUri);

                if (uri != null) {
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(uri, null, null);
                }

                // Do not throw, and try the next volume
            }
        }

        return null;
    }



    private void getAllPics(){
       // String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
     //   String[] selectionArgs = new String[] {Environment.DIRECTORY_DCIM};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        try(Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                null,
                null,
                null
        ))  {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                Log.i("TAG", "name ->: "+name);
                Log.i("TAG", "contentUri ->: "+contentUri);
            }
            Log.e("TAG", "----------------END------------------ ");
        }catch(Exception e){
            Log.e("TAG", "error: " + e.getMessage());
        }
    }





    private void saveImageWithoutResolver(Bitmap bitmap){
        String filename = "dog_"+System.currentTimeMillis() + ".png";
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
        Log.i("TAG", "path: "+ apkFile.getPath());
        OutputStream fos ;
        try {
            fos = new FileOutputStream(apkFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            MediaScannerConnection.scanFile(this, new String[]{apkFile.getAbsolutePath()}, new String[]{"image/*"}, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String s, Uri uri) {
                    Log.i("TAG", "onScanCompleted: "+uri);
                }
            });
            Log.e("TAG", "successSaveImageWithoutResolver: ");
        } catch (FileNotFoundException e) {
            Log.e("TAG", "saveImageWithoutResolver: "+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("TAG", "saveImageWithoutResolver: "+e.getMessage());
            e.printStackTrace();
        }

    }






}