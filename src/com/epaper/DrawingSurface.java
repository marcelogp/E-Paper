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
        bitmapCache = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
    }

    private void resetBitmapCache() {
        bitmapCache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_4444);
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
                            mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
                        }

                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        DrawingPath drawPath = null;
                        
                        if (currentDrawingPath != null)
                            drawPath = currentDrawingPath;
                        else
                            isDrawing = false;
                        
                        if (cacheIsDirty) {
                            resetBitmapCache();
                            commandManagerL.get(curCM).drawAll(bitmapCache);
                            cacheIsDirty = false;
                        }
                        canvas.drawBitmap(bitmapCache, 0, 0, null);

                        if (drawPath != null)
                            drawPath.draw(canvas);
                        
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

            Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_4444);
            cm.drawAll(bmp);
            ans.add(bmp);
        }
        return ans;
    }

    public void importPages(ArrayList<Bitmap> data) {
        commandManagerL = new ArrayList<CommandManager>();
        curCM = 0;

        for (Bitmap bmp : data)
            if (bmp != null)
                commandManagerL.add(new CommandManager(bmp));

        if (commandManagerL.isEmpty())
            commandManagerL.add(new CommandManager());

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
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
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
