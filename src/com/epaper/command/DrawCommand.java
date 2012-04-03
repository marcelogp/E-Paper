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
