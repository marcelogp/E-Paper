package com.epaper.command;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.epaper.DrawingPath;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class CommandManager
{
    private static int PROX_SIDE = 4;
    private List<DrawingPath> currentStack;
    private List<Command> redoStack;
    private List<Command> undoStack;

    public CommandManager() {
        currentStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
        redoStack = Collections.synchronizedList(new ArrayList<Command>());
        undoStack = Collections.synchronizedList(new ArrayList<Command>());
    }

    public void addCommand(Command command) {
        redoStack.clear();
        undoStack.add(command);
        command.apply(currentStack);
    }

    public Command undo() {
        final int undoLength = undoStack.size();
        
        if (undoLength <= 0) return null;
        
        Command undoCommand = undoStack.get(undoLength - 1);
        undoStack.remove(undoLength - 1);
        redoStack.add(undoCommand);
        
        undoCommand = undoCommand.invert();
        undoCommand.apply(currentStack);
        return undoCommand;
    }

    public Command redo() {
        final int redoLength = redoStack.toArray().length;

        if (redoLength <= 0) return null;
        
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

    public void performErase(Bitmap drawTo, float centerX, float centerY) {
        Bitmap bitmapTmp = drawTo.copy(Bitmap.Config.ARGB_8888, true);
        Canvas c = new Canvas(drawTo);
        Canvas ctmp = new Canvas(bitmapTmp);

        if (currentStack != null) {
            synchronized (currentStack) {
                for (int i=0; i<currentStackLength(); i++) {
                    final DrawingPath drawingPath = currentStack.get(i);
                    drawingPath.draw(ctmp);
                    
                    if (haveAnyBlack(centerX, centerY, bitmapTmp)) {
                        /* Rollback last draw and erase path from stack */
                        bitmapTmp = drawTo.copy(Bitmap.Config.ARGB_8888, true);
                        addCommand(new EraseCommand(drawingPath));
                        i--;
                    }
                    else drawingPath.draw(c);
                }
            }
        }
    }
    
    public boolean haveAnyBlack(float centerX, float centerY, Bitmap bitmap) {
        int x0 = (int)(centerX-PROX_SIDE/2.0f);
        int y0 = (int)(centerY-PROX_SIDE/2.0f);
        
        if (x0 < 0 || y0 < 0) return false;
        int prPixels[] = new int[PROX_SIDE*PROX_SIDE];
        bitmap.getPixels(prPixels, 0, PROX_SIDE, x0, y0, PROX_SIDE, PROX_SIDE);
        
        for (int i=0; i<PROX_SIDE*PROX_SIDE; i++)
            if (prPixels[i] != 0) return true;
        return false;
    }
}
