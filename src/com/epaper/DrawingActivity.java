package com.epaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.epaper.brush.Brush;
import com.epaper.brush.PenBrush;

import java.io.File;
import java.io.FileOutputStream;

public class DrawingActivity extends Activity implements View.OnTouchListener, View.OnKeyListener
{
    private DrawingSurface drawingSurface;
    private Paint currentPaint;
    private Brush currentBrush;
    private Boolean toolEraser;
    private File APP_FILE_PATH = new File("/sdcard/AndroidDrawings");

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);

        setCurrentPaint(3);
        setSelectedTool(R.id.smallBtn);
        currentBrush = new PenBrush();

        drawingSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
        drawingSurface.setOnTouchListener(this);
        drawingSurface.setOnKeyListener(this);

        toolEraser = false;

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
//            case R.id.undoBtn:
//                drawingSurface.undo();
//                break;
//            case R.id.redoBtn:
//                drawingSurface.redo();
//                break;
            case R.id.saveBtn:
                final Activity currentActivity = this;
                Handler saveHandler = new Handler()
                {
                    @Override
                    public void handleMessage(Message msg) {
                        final AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).create();
                        alertDialog.setTitle("Saved");
                        alertDialog.setMessage("Your drawing had been saved");
                        alertDialog.setButton("OK", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                        alertDialog.show();
                    }
                };
                new ExportBitmapToFile(this, saveHandler, drawingSurface.getBitmap()).execute();
                break;
            case R.id.smallBtn:
                setSelectedTool(R.id.smallBtn);
                setCurrentPaint(3);
                toolEraser = false;
                break;
            case R.id.largeBtn:
                setSelectedTool(R.id.largeBtn);
                setCurrentPaint(8);
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

    private class ExportBitmapToFile extends AsyncTask<Intent, Void, Boolean>
    {
        private Context mContext;
        private Handler mHandler;
        private Bitmap nBitmap;

        public ExportBitmapToFile(Context context, Handler handler, Bitmap bitmap) {
            mContext = context;
            nBitmap = bitmap;
            mHandler = handler;
        }

        @Override
        protected Boolean doInBackground(Intent... arg0) {
            try {
                if (!APP_FILE_PATH.exists()) {
                    APP_FILE_PATH.mkdirs();
                }

                final FileOutputStream out = new FileOutputStream(new File(APP_FILE_PATH + "/myAwesomeDrawing.png"));
                nBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            if (bool) {
                mHandler.sendEmptyMessage(1);
            }
        }
    }
}
