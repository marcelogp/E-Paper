/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epaper.command;

import android.graphics.Bitmap;
import com.epaper.DrawingPath;
import java.util.List;

public class EraseCommand extends Command
{
    public EraseCommand(DrawingPath path) {
        super(path);
    }
    
    public void apply(List<DrawingPath> stack) {
        if (stack != null)
            stack.remove(super.getPath());
    }

    public boolean applyToBitmap(Bitmap bitmapCache) {
        return false;
    }
    
    public Command invert() {
        return new DrawCommand(super.getPath());
    }
}
