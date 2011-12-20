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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.epaper.brush.Brush;
import com.epaper.brush.PenBrush;

import java.io.File;
import java.io.FileOutputStream;

public class DrawingActivity extends Activity implements View.OnTouchListener
{
    private DrawingSurface drawingSurface;
    private Paint currentPaint;
    private Brush currentBrush;
    private Button redoBtn;
    private Button undoBtn;
    private File APP_FILE_PATH = new File("/sdcard/AndroidDrawings");

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);

        setCurrentPaint();
        currentBrush = new PenBrush();

        drawingSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
        drawingSurface.setOnTouchListener(this);

        redoBtn = (Button) findViewById(R.id.redoBtn);
        undoBtn = (Button) findViewById(R.id.undoBtn);

        redoBtn.setEnabled(false);
        undoBtn.setEnabled(false);
    }

    private void setCurrentPaint() {
        currentPaint = new Paint();
        currentPaint.setDither(true);
        currentPaint.setColor(0xFF000000);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(3);
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        DrawingPath drawingPath = drawingSurface.getDrawingPath();

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

            undoBtn.setEnabled(true);
            redoBtn.setEnabled(false);
        }
        return true;
    }

    public void onClick(View view) {
        N2EpdController.setMode(N2EpdController.REGION_APP_3,
                                N2EpdController.WAVE_GU,
                                N2EpdController.MODE_ONESHOT_ALL);

        switch (view.getId()) {
            case R.id.undoBtn:
                drawingSurface.undo();
                if (drawingSurface.hasMoreUndo() == false) {
                    undoBtn.setEnabled(false);
                }
                redoBtn.setEnabled(true);
                break;

            case R.id.redoBtn:
                drawingSurface.redo();
                if (drawingSurface.hasMoreRedo() == false) {
                    redoBtn.setEnabled(false);
                }

                undoBtn.setEnabled(true);
                break;
            case R.id.saveBtn:
                final Activity currentActivity = this;
                Handler saveHandler = new Handler()
                {
                    @Override
                    public void handleMessage(Message msg) {
                        final AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).create();
                        alertDialog.setTitle("Saved");
                        alertDialog.setMessage("Your drawing had been saved :)");
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
                currentPaint.setStrokeWidth(3);
                break;
            case R.id.medBtn:
                currentPaint.setStrokeWidth(6);
                break;
            case R.id.largeBtn:
                currentPaint.setStrokeWidth(9);
                break;
            case R.id.clearBtn:
                drawingSurface.resetHistory();
                break;
        }
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
            //mHandler.post(completeRunnable);
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
