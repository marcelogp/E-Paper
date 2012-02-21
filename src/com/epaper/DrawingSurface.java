package com.epaper;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.epaper.command.Command;
import com.epaper.command.CommandManager;
import com.epaper.command.DrawCommand;

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
        bitmapCache = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    private void resetBitmapCache() {
        bitmapCache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    }

    public void resetHistory() {
        commandManager = new CommandManager();
        cacheIsDirty = true;
        isDrawing = true;
    }
    
    void attemptErase(float x, float y) {
        if (cacheIsDirty) {
            System.err.println("ERR: Should not be dirty");
            return;
        }
        
        if (commandManager.performErase(bitmapCache.getHeight(), 
                                        bitmapCache.getWidth(),
                                        Math.round(x), Math.round(y))) {
            resetBitmapCache();
            commandManager.drawAll(bitmapCache);
            isDrawing = true;
        }
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
                            resetBitmapCache();
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
    
    public boolean hasMoreUndo() {
        return commandManager.hasMoreUndo();
    }
    
    private void addToCache(Command cmd) {
        if (cmd == null) return;
        if (!cmd.applyToBitmap(bitmapCache))
            cacheIsDirty = true;
        isDrawing = true;
    }

    public void redo() {
        if (commandManager.hasMoreRedo())
            addToCache(commandManager.redo());
    }

    public void undo() {
        if (commandManager.hasMoreUndo())
            addToCache(commandManager.undo());
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
        Command cmd = new DrawCommand(currentDrawingPath);
        commandManager.addCommand(cmd);
        addToCache(cmd);
        currentDrawingPath = null;
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        isDrawing=true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        thread = new DrawThread(holder);
        thread.setRunning(true);
        thread.start();
        isDrawing=true;
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
