/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epaper;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

class FileManager
{
    public static void savePages(String path, ArrayList<Bitmap> exportBitmaps) throws IOException {
        File dest = new File(path);
        System.err.println("DEST IS "+path);

        if (!dest.exists())
            dest.mkdirs();

        for (int i = 0; i < exportBitmaps.size(); i++) {
            String fileName = String.format("%03d.png", i);

            final FileOutputStream out = new FileOutputStream(path + "/" + fileName);
            System.err.println("Writing "+fileName+": Bitmap "+ exportBitmaps.get(i).getWidth()+"x"+exportBitmaps.get(i).getHeight());
            exportBitmaps.get(i).compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        }
    }
}
