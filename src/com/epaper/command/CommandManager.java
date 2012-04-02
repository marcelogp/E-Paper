package com.epaper.command;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.epaper.DrawingPath;
import java.util.*;

public class CommandManager
{
    private static float STROKE_DIST = 8.0f;
    private List<DrawingPath> currentStack;
    private List<Command> redoStack;
    private List<Command> undoStack;
    private Bitmap background;

    public CommandManager() {
        currentStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
        redoStack = Collections.synchronizedList(new ArrayList<Command>());
        undoStack = Collections.synchronizedList(new ArrayList<Command>());
    }
    
    public CommandManager(Bitmap bg) {
        this();
        background = bg;
    }

    public void addCommand(Command command) {
        redoStack.clear();
        undoStack.add(command);
        command.apply(currentStack);
    }

    public Command undo() {
        final int undoLength = undoStack.size();

        if (undoLength <= 0)
            return null;

        Command undoCommand = undoStack.get(undoLength - 1);
        undoStack.remove(undoLength - 1);
        redoStack.add(undoCommand);

        undoCommand = undoCommand.invert();
        undoCommand.apply(currentStack);
        return undoCommand;
    }

    public Command redo() {
        final int redoLength = redoStack.toArray().length;

        if (redoLength <= 0)
            return null;

        Command redoCommand = redoStack.get(redoLength - 1);
        redoStack.remove(redoLength - 1);
        redoCommand.apply(currentStack);
        undoStack.add(redoCommand);

        return redoCommand;
    }

    public int currentStackLength() {
        return currentStack.toArray().length;
    }

    public void drawAll(Bitmap drawTo) {
        Canvas c = new Canvas(drawTo);
        
        if (background != null)
            c.drawBitmap(background, 0, 0, null);
        
        if (currentStack != null) {
            synchronized (currentStack) {
                final Iterator i = currentStack.iterator();

                while (i.hasNext()) {
                    final DrawingPath drawingPath = (DrawingPath) i.next();
                    drawingPath.draw(c);
                }
            }
        }
    }

    public boolean hasMoreRedo() {
        return redoStack.toArray().length > 0;
    }

    public boolean hasMoreUndo() {
        return undoStack.toArray().length > 0;
    }

    public boolean performErase(int h, int w, int x, int y) {
        Bitmap bitmapTmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas ctmp = new Canvas(bitmapTmp);
        
        bitmapTmp.eraseColor(0xFFFFFFFF);
        
        if (currentStack != null) {
            synchronized (currentStack) {
                for (int i = 0; i < currentStackLength(); i++) {
                    final DrawingPath drawingPath = currentStack.get(i);

                    /* Draw each path thicker and with an id color to
                       later check for erase action at x,y */
                    Paint iPaint = new Paint(drawingPath.paint);
                    iPaint.setColor(0xFF000000 | i);
                    iPaint.setStrokeWidth(iPaint.getStrokeWidth() + STROKE_DIST);
                    ctmp.drawPath(drawingPath.path, iPaint);
                }

                return removePath(x, y, bitmapTmp);
            }
        }
        return false;
    }

    public boolean removePath(int x, int y, Bitmap bitmap) {
        int pxy = bitmap.getPixel(x, y);
        
        if (pxy != -1) { /* Non-white -> there is a Path here */
            int idx = pxy ^ 0xFF000000;
            final DrawingPath drawingPath = currentStack.get(idx);
            addCommand(new EraseCommand(drawingPath));
            return true;
        }
        return false;
    }
    
    public boolean isEmpty() {
        return currentStack.isEmpty() && background == null;
    }
}
