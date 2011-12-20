package com.epaper.brush;

import android.graphics.Path;

public interface IBrush
{
    public void mouseDown(Path path, float x, float y);

    public void mouseMove(Path path, float x, float y);

    public void mouseUp(Path path, float x, float y);
}
