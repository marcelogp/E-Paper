package com.epaper;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback
{
    private Boolean _run;
    protected DrawThread thread;
    private Bitmap mBitmap;
    private boolean isDrawing = true;
    private DrawingPath currentDrawingPath;
    private CommandManager commandManager;
    private Bitmap bitmapCache;
    private Boolean cacheIsDirty;

    public DrawingSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setBackgroundColor(Color.WHITE);

        commandManager = new CommandManager();
        thread = new DrawThread(getHolder());
        cacheIsDirty = true;
        resetBitmapCache(1, 1);
    }

    private void resetBitmapCache(int w, int h) {
        bitmapCache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    public void resetHistory() {
        commandManager = new CommandManager();
        cacheIsDirty = true;
        isDrawing = true;
    }

    private void addPath(DrawingPath path) {
        Canvas c = new Canvas(bitmapCache);
        path.draw(c);
    }

    class DrawThread extends Thread
    {
        private SurfaceHolder mSurfaceHolder;

        public DrawThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        public void setRunning(boolean run) {
            _run = run;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            while (_run) {
                if (isDrawing) {
                    try {
                        canvas = mSurfaceHolder.lockCanvas(null);
                        if (mBitmap == null) {
                            mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                        }

                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        
                        if (currentDrawingPath != null)
                            currentDrawingPath.draw(canvas);
                        else
                            isDrawing = false;

                        if (cacheIsDirty) {
                            resetBitmapCache(getWidth(), getHeight());
                            commandManager.drawAll(bitmapCache);
                            cacheIsDirty = false;
                        }
                        canvas.drawBitmap(bitmapCache, 0, 0, null);

                        canvas.drawBitmap(mBitmap, 0, 0, null);
                    } finally {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    public boolean hasMoreRedo() {
        return commandManager.hasMoreRedo();
    }

    public void redo() {
        if (commandManager.hasMoreRedo()) {
            addPath(commandManager.redo());
        }
        isDrawing = true;
    }

    public void undo() {
        if (commandManager.hasMoreUndo()) {
            cacheIsDirty = true;
            commandManager.undo();
        }
        isDrawing = true;
    }

    public boolean hasMoreUndo() {
        return commandManager.hasMoreUndo();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void start() {
        isDrawing = true;
        N2EpdController.setMode(N2EpdController.REGION_APP_3,
                                N2EpdController.WAVE_A2,
                                N2EpdController.MODE_ACTIVE_ALL);
    }

    public void end() {
        commandManager.addCommand(currentDrawingPath);
        addPath(currentDrawingPath);
        currentDrawingPath = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    public DrawingPath getDrawingPath() {
        return currentDrawingPath;
    }

    public void setDrawingPath(DrawingPath currentDrawingPath) {
        this.currentDrawingPath = currentDrawingPath;
    }
}
