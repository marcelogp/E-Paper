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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.epaper.brush.Brush;
import com.epaper.brush.PenBrush;
import com.epaper.kaloer.filepicker.FilePickerActivity;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DrawingActivity extends Activity implements View.OnTouchListener, View.OnKeyListener
{
    private static final int REQUEST_PICK_DIR = 1;
    private static final int PAINT_SIZE_SMALL = 3;
    private static final int PAINT_SIZE_LARGE = 8;
    private DrawingSurface drawingSurface;
    private Paint currentPaint;
    private Brush currentBrush;
    private Boolean toolEraser;
    private String defaultDir;
    private ProgressDialog saveProgress;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);

        setCurrentPaint(PAINT_SIZE_SMALL);
        setSelectedTool(R.id.smallBtn);
        currentBrush = new PenBrush();

        drawingSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
        drawingSurface.setOnTouchListener(this);
        drawingSurface.setOnKeyListener(this);

        toolEraser = false;

        defaultDir = Environment.getExternalStorageDirectory() + "/E-Paper";

        drawingSurface.start(); // ensures that first drawing will respond quickly
    }

    private void setCurrentPaint(int size) {
        currentPaint = new Paint();
        currentPaint.setDither(true);
        currentPaint.setColor(0xFF000000);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(size);
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        DrawingPath drawingPath = drawingSurface.getDrawingPath();

        if (toolEraser) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                drawingSurface.attemptErase(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            drawingSurface.start();

            drawingPath = new DrawingPath();
            drawingPath.paint = currentPaint;
            drawingPath.path = new Path();

            drawingSurface.setDrawingPath(drawingPath);
            currentBrush.mouseDown(drawingPath.path, motionEvent.getX(), motionEvent.getY());

        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            currentBrush.mouseMove(drawingPath.path, motionEvent.getX(), motionEvent.getY());

        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            drawingSurface.end();

            currentBrush.mouseUp(drawingPath.path, motionEvent.getX(), motionEvent.getY());
        }
        return true;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.newBtn:
                N2EpdController.setNormalMode();

                new AlertDialog.Builder(this) // 
                        .setTitle("Confirmation") //
                        .setMessage("Discard unsaved changes?") //
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {
                        drawingSurface.removeAllPages();
                        updatePageNumbers();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

                break;
            case R.id.undoBtn:
                drawingSurface.undo();
                break;
            case R.id.redoBtn:
                drawingSurface.redo();
                break;
            case R.id.saveBtn:
                N2EpdController.setNormalMode();
                final EditText input = new EditText(this);
                input.setText(getDefaultDir());

                new AlertDialog.Builder(this) // 
                        .setTitle("Destination") //
                        .setMessage("Path to save") //
                        .setView(input) //
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable path = input.getText();

                        savePages(path.toString());
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();

                break;
            case R.id.loadBtn:
                N2EpdController.setNormalMode();

                Intent intent = new Intent(this, FilePickerActivity.class);
                ArrayList<String> fileTypes = new ArrayList<String>();
                fileTypes.add(".png");
                
                intent.putExtra(FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS, fileTypes);

                intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH, defaultDir);
                startActivityForResult(intent, REQUEST_PICK_DIR);

                break;
            case R.id.smallBtn:
                setSelectedTool(R.id.smallBtn);
                setCurrentPaint(PAINT_SIZE_SMALL);
                toolEraser = false;
                break;
            case R.id.largeBtn:
                setSelectedTool(R.id.largeBtn);
                setCurrentPaint(PAINT_SIZE_LARGE);
                toolEraser = false;
                break;
            case R.id.eraserBtn:
                setSelectedTool(R.id.eraserBtn);
                toolEraser = true;
                break;
            case R.id.remPageBtn:
                drawingSurface.removePage();
                break;
            case R.id.newPageBtn:
                drawingSurface.insertPage();
                break;
            case R.id.prevBtn:
                drawingSurface.switchPrevPage();
                break;
            case R.id.nextBtn:
                drawingSurface.switchNextPage();
                break;
        }
        updatePageNumbers();
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;

        switch (keyCode) {
            case 92:
                drawingSurface.undo();
                break;
            case 93:
                drawingSurface.redo();
                break;
            case 94:
                drawingSurface.switchPrevPage();
                break;
            case 95:
                drawingSurface.switchNextPage();
                break;
            default:
                return false;
        }
        updatePageNumbers();
        return true;
    }

    private void updatePageNumbers() {
        String cp = String.valueOf(drawingSurface.getCurPage());
        String lp = String.valueOf(drawingSurface.getLastPage());

        ((TextView) findViewById(R.id.curPage)).setText(cp);
        ((TextView) findViewById(R.id.lastPage)).setText(lp);
    }

    private void setSelectedTool(int sel) {
        ((ToggleButton) findViewById(R.id.smallBtn)).setChecked(false);
        ((ToggleButton) findViewById(R.id.largeBtn)).setChecked(false);
        ((ToggleButton) findViewById(R.id.eraserBtn)).setChecked(false);
        ((ToggleButton) findViewById(sel)).setChecked(true);
    }

    private void savePages(String path) {
        saveProgress = new ProgressDialog(this);
        saveProgress.setIndeterminate(false);
        saveProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        saveProgress.setTitle("Saving...");
        saveProgress.show();

        Thread t = new SaveThread(path);
        t.start();
    }

    class SaveThread extends Thread
    {
        String path;

        public SaveThread(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                FileManager.savePages(path, drawingSurface.exportPages(), saveProgress);
            } catch (IOException e) {
                e.printStackTrace();
                saveHandler.sendEmptyMessage(1);
                return;
            }
            saveHandler.sendEmptyMessage(0);
        }
    }
    private Handler saveHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 0)
                showAlert("Save failed. Make sure directory is writable");
             
            saveProgress.dismiss();
        }
    };

    private void loadPages(String path) {
        try {
            int w = drawingSurface.getWidth();
            int h = drawingSurface.getHeight();
            
            drawingSurface.importPages(FileManager.loadPages(path,w,h));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Load failed. Corrupt file?");
            return;
        }
        updatePageNumbers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_DIR:
                    if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                        loadPages(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
                    }
            }
        }
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this) // 
                .setMessage(message) //
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }

    private String getDefaultDir() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm");
        return defaultDir + "/" + sdf.format(new Date(System.currentTimeMillis()));
    }
}
