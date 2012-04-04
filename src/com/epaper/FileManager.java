/*
 * Copyright (C) 2012 Marcelo Povoa <marcelogpovoa at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epaper;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.*;
import java.util.ArrayList;

class FileManager
{
    public static void savePages(String path, ArrayList<Bitmap> pages, ProgressDialog progress) throws IOException {
        File dest = new File(path);
        
        if (!dest.exists())
            dest.mkdirs();
        
        if (progress != null)
            progress.setMax(pages.size());
        
        for (int i = 0; i < pages.size(); i++) {
            final FileOutputStream out = new FileOutputStream(getFilePath(path, i));
            pages.get(i).compress(Bitmap.CompressFormat.PNG, 10, out);
            out.flush();
            out.close();
            
            if (progress != null)
                progress.setProgress(i + 1);
        }
    }
    
    public static ArrayList<Bitmap> loadPages(String path, int w, int h) throws IOException {
        ArrayList<Bitmap> res = new ArrayList<Bitmap>();
        
        File fd = new File(path);
        
        if (fd.isFile() && fd.getName().endsWith(".png"))
            res.add(importFile(fd, w, h));
        else {
            File files[] = fd.listFiles(new FilenameFilter()
            {
                public boolean accept(File f, String s) {
                    return s.endsWith(".png");
                }
            });
            
            for (File f : files)
                res.add(importFile(f, w, h));
        }
        return res;
    }
    

    private static String getFilePath(String path, int index) {
        return path + "/" + String.format("%03d.png", index);
    }
    
    private static Bitmap importFile(File fd, int w, int h) throws IOException {
        FileInputStream inp;
        try {
            inp = new FileInputStream(fd);
        } catch (FileNotFoundException e) {
            return null;
        }
        
        Log.i("", "Importing file: " + fd.getName());
        
        Bitmap tmpBmp = BitmapFactory.decodeStream(inp);
        inp.close();
        
        return Bitmap.createScaledBitmap(tmpBmp, w, h, true);
    }
}
