/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epaper;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.util.ArrayList;

class FileManager
{
    public static void savePages(String path, ArrayList<Bitmap> pages, ProgressDialog progress) throws IOException {
        File dest = new File(path);

        if (!dest.exists())
            dest.mkdirs();
        
        if (progress!=null)
            progress.setMax(pages.size());

        for (int i = 0; i < pages.size(); i++) {
            final FileOutputStream out = new FileOutputStream(getFilePath(path, i));
            pages.get(i).compress(Bitmap.CompressFormat.PNG, 10, out);
            out.flush();
            out.close();
            
            if (progress != null)
                progress.setProgress(i+1);
        }
    }

    public static ArrayList<Bitmap> loadPages(String path, int w, int h) throws IOException {
        ArrayList<Bitmap> res = new ArrayList<Bitmap>();
        
        for (int i = 0;; i++) {
            FileInputStream inp;
            try {
                inp = new FileInputStream(getFilePath(path, i));
            } catch (FileNotFoundException e) {
                break;
            }
            
            Bitmap tmpBmp = BitmapFactory.decodeStream(inp);
            res.add(Bitmap.createScaledBitmap(tmpBmp, w, h, true));
            
            inp.close();
        }
        return res;
    }

    private static String getFilePath(String path, int index) {
        return path + "/" + String.format("%03d.png", index);
    }
}
