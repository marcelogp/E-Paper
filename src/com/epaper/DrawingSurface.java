package com.epaper;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.epaper.command.Command;
import com.epaper.command.CommandManager;
import com.epaper.command.DrawCommand;
import java.util.ArrayList;

public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback
{
    private Boolean _run;
    protected DrawThread thread;
    private Bitmap mBitmap;
    private boolean isDrawing = true;
    private DrawingPath currentDrawingPath;
    private ArrayList<CommandManager> commandManagerL;
    private Bitmap bitmapCache;
    private Boolean cacheIsDirty;
    private int curCM;

    public DrawingSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setBackgroundColor(Color.WHITE);

        commandManagerL = new ArrayList<CommandManager>();
        commandManagerL.add(new CommandManager());
        curCM = 0;

        cacheIsDirty = true;
        isDrawing = true;

        thread = new DrawThread(getHolder());
        bitmapCache = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    private void resetBitmapCache() {
        bitmapCache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    }

    void attemptErase(float x, float y) {
        if (cacheIsDirty) {
            System.err.println("ERR: Should not be dirty");
            return;
        }

        if (commandManagerL.get(curCM).performErase(bitmapCache.getHeight(),
                                                    bitmapCache.getWidth(),
                                                    Math.round(x), Math.round(y))) {
            resetBitmapCache();
            commandManagerL.get(curCM).drawAll(bitmapCache);
            isDrawing = true;
        }
    }

    // Called after any full page update is performed
    private void afterPageUpdate() {
        N2EpdController.setNormalMode();
        cacheIsDirty = true;
        isDrawing = true;
    }

    public int getCurPage() {
        return curCM + 1;
    }

    public int getLastPage() {
        return commandManagerL.size();
    }

    public final void removePage() {
        commandManagerL.remove(curCM);

        if (commandManagerL.isEmpty())
            commandManagerL.add(new CommandManager());
        else if (curCM == commandManagerL.size())
            curCM--;

        afterPageUpdate();
    }

    public final void insertPage() {
        commandManagerL.add(curCM, new CommandManager());
        afterPageUpdate();
    }

    public final void switchNextPage() {
        if (curCM == commandManagerL.size() - 1) {
            // Last page is already blank? Do nothing.
            if (commandManagerL.get(curCM).isEmpty())
                return;

            commandManagerL.add(new CommandManager());
        }
        curCM++;
        afterPageUpdate();
    }

    public final void switchPrevPage() {
        if (curCM <= 0)
            return;

        if (curCM == commandManagerL.size() - 1 && commandManagerL.get(curCM).isEmpty()) {
            removePage();
            return;
        }

        curCM--;
        afterPageUpdate();
    }

    void removeAllPages() {
        commandManagerL = new ArrayList<CommandManager>();
        commandManagerL.add(new CommandManager());
        curCM = 0;

        afterPageUpdate();
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
                            commandManagerL.get(curCM).drawAll(bitmapCache);
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

    public ArrayList<Bitmap> exportPages() {
        ArrayList<Bitmap> ans = new ArrayList<Bitmap>();

        for (CommandManager cm : commandManagerL) {
            if (cm.isEmpty())
                continue;

            Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            cm.drawAll(bmp);
            ans.add(bmp);
        }
        return ans;
    }

    public void importPages(ArrayList<Bitmap> data) {
        commandManagerL = new ArrayList<CommandManager>();
        curCM = 0;

        for (Bitmap bmp : data) {
            commandManagerL.add(new CommandManager(bmp));
        }
        afterPageUpdate();
    }

    public boolean hasMoreRedo() {
        return commandManagerL.get(curCM).hasMoreRedo();
    }

    public boolean hasMoreUndo() {
        return commandManagerL.get(curCM).hasMoreUndo();
    }

    private void addToCache(Command cmd) {
        if (cmd == null)
            return;
        if (!cmd.applyToBitmap(bitmapCache))
            cacheIsDirty = true;
        isDrawing = true;
    }

    public void redo() {
        if (commandManagerL.get(curCM).hasMoreRedo())
            addToCache(commandManagerL.get(curCM).redo());
    }

    public void undo() {
        if (commandManagerL.get(curCM).hasMoreUndo())
            addToCache(commandManagerL.get(curCM).undo());
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void start() {
        isDrawing = true;
        N2EpdController.setA2Mode();
    }

    public void end() {
        Command cmd = new DrawCommand(currentDrawingPath);
        commandManagerL.get(curCM).addCommand(cmd);
        addToCache(cmd);
        currentDrawingPath = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        isDrawing = true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        thread = new DrawThread(holder);
        thread.setRunning(true);
        thread.start();
        isDrawing = true;
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
