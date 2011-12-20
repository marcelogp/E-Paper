package com.epaper;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class CommandManager
{
    private List<DrawingPath> currentStack;
    private List<DrawingPath> redoStack;

    public CommandManager() {
        currentStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
        redoStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
    }

    public void addCommand(DrawingPath command) {
        redoStack.clear();
        currentStack.add(command);
    }

    public void undo() {
        final int length = currentStackLength();

        if (length > 0) {
            final DrawingPath undoCommand = currentStack.get(length - 1);
            currentStack.remove(length - 1);
            redoStack.add(undoCommand);
        }
    }

    public DrawingPath redo() {
        final int length = redoStack.toArray().length;

        if (length > 0) {
            final DrawingPath redoCommand = redoStack.get(length - 1);
            redoStack.remove(length - 1);
            currentStack.add(redoCommand);
            return redoCommand;
        }
        return null;
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
        return currentStack.toArray().length > 0;
    }

    void resetDraw() {
    }
}
