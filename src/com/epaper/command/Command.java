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
