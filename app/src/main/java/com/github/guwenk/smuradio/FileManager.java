package com.github.guwenk.smuradio;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class FileManager {
    private Context context;
    FileManager(Context context){
        this.context = context;
    }

    Bitmap loadBitmap(String path, String filename) {
        try {
            File f=new File(path, filename+".png");
            return BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    String saveBitmap(Bitmap bitmapImage, String filename, String path) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir(path, Context.MODE_PRIVATE);
        File mypath=new File(directory,filename+".png");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}
