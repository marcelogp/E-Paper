package com.epaper.command;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.epaper.DrawingPath;
import java.util.List;

public class DrawCommand extends Command
{
    public DrawCommand(DrawingPath path) {
        super(path);
    }
    
    public void apply(List<DrawingPath> stack) {
        if (stack != null)
            stack.add(super.getPath());
    }

    public boolean applyToBitmap(Bitmap bmp) {
        Canvas c = new Canvas(bmp);
        super.getPath().draw(c);
        return true;
    }

    public Command invert() {
        return new EraseCommand(super.getPath());
    }
}
