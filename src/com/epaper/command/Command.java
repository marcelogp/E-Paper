/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epaper.command;

import android.graphics.Bitmap;
import com.epaper.DrawingPath;
import java.util.List;

public abstract class Command
{
    private DrawingPath path;

    public Command(DrawingPath path) {
        this.path = path;
    }

    public DrawingPath getPath() {
        return path;
    }
    
    public abstract void apply(List<DrawingPath> stack);
    public abstract boolean applyToBitmap(Bitmap bitmapCache);
    public abstract Command invert();
}
