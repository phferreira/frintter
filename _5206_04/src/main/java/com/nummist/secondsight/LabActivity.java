package com.nummist.secondsight;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.nummist.secondsight.filters.Filter;
import com.nummist.secondsight.filters.NoneFilter;
import com.nummist.secondsight.filters.ar.ImageDetectionFilter;
import com.nummist.secondsight.filters.convolution.StrokeEdgesFilter;
import com.nummist.secondsight.filters.curve.CrossProcessCurveFilter;
import com.nummist.secondsight.filters.curve.PortraCurveFilter;
import com.nummist.secondsight.filters.curve.ProviaCurveFilter;
import com.nummist.secondsight.filters.curve.VelviaCurveFilter;
import com.nummist.secondsight.filters.mixer.RecolorCMVFilter;
import com.nummist.secondsight.filters.mixer.RecolorRCFilter;
import com.nummist.secondsight.filters.mixer.RecolorRGVFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.text.DecimalFormat;

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public final class LabActivity extends AppCompatActivity {
    
    public static final String PHOTO_FILE_EXTENSION = ".png";
    public static final String PHOTO_MIME_TYPE = "image/png";
    
    public static final String EXTRA_PHOTO_URI =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_DATA_PATH";
    public static double EXTRA_PHOTO_MAT ;

    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final Integer MAX_LINHAS = 2;
    private static final Integer correcaoX = 300;
    private static final Integer correcaoY = 300;

    public Mat imagem;

    float initialX, initialY;

    private Uri mUri;
    private String mDataPath;
    private Integer numLinhas = 0;
    private ImageView imageView;

    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                    }
                }
            };

    public void pintarTela(){
//        Bitmap bm = Bitmap.createBitmap(imagem.cols(), imagem.rows(),Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(imagem, bm);
//        // find the imageview and draw it!
        ImageView imageView = new ImageView(this);
//        imageView.setImageBitmap(bm);
        imageView.setImageURI(mUri);
        setContentView(imageView);
    }
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        mDataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);

//        ImageView imageView = new ImageView(this);
//        imageView.setImageURI(mUri);

//        setContentView(imageView);
//
//        paint.setAntiAlias(true);
//        paint.setStrokeWidth(6);
//        paint.setColor(Color.BLUE);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeJoin(Paint.Join.ROUND);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,
                this, mLoaderCallback);

        imagem = imread(mDataPath, CV_LOAD_IMAGE_COLOR);
//
        if (imagem.empty()){
            Log.d(TAG, "Image cannot found.");
        }else{
            pintarTela();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.removeItem(R.id.menu_share);
        getMenuInflater().inflate(R.menu.activity_lab, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_delete:
            deletePhoto();
            return true;
        case R.id.menu_edit:
            editPhoto();
            return true;
        case R.id.menu_share:
            sharePhoto();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public double distance(Point p, Point q) {
        double deltaX = p.y - q.y;
        double deltaY = p.x - q.x;
        double result = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return result;
    }

    public static double converterDoubleDoisDecimais(double precoDouble) {
        DecimalFormat fmt = new DecimalFormat("0.00");
        String string = fmt.format(precoDouble);
        String[] part = string.split("[,]");
        String string2 = part[0]+"."+part[1];
        double preco = Double.parseDouble(string2);
        return preco;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX() - correcaoX;
                initialY = event.getY() - correcaoY;

                Log.d(TAG, "Action was DOWN");
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "Action was MOVE");
                break;

            case MotionEvent.ACTION_UP:
                float finalX = event.getX() - correcaoX;
                float finalY = event.getY() - correcaoY;

                double d = distance(new Point(initialX, initialY), new Point(finalX, finalY)) / EXTRA_PHOTO_MAT;

                Log.d(TAG, "Action was UP");



                if (numLinhas < MAX_LINHAS) {
                    if (numLinhas == 0) {
                        Imgproc.line(imagem, new Point(initialX, initialY), new Point(finalX, finalY), new Scalar(200, 0, 0), 8);
                        Imgproc.putText(imagem, "    " + (converterDoubleDoisDecimais(d)) + " cm", new Point(finalX, finalY), 1, 7, new Scalar(200, 0, 0), 15);
                    }else{
                        Imgproc.line(imagem, new Point(initialX, initialY), new Point(finalX, finalY), new Scalar(150, 150, 0), 8);
                        Imgproc.putText(imagem, "    " + (converterDoubleDoisDecimais(d)) + " cm", new Point(finalX, finalY), 1, 7, new Scalar(150, 150, 0), 15);
                    }
                    Imgcodecs.imwrite(mDataPath, imagem);
                    pintarTela();
                }

                numLinhas += 1;


//                if (initialX < finalX) {
//                    Log.d(TAG, "Left to Right swipe performed");
//                }
//
//                if (initialX > finalX) {
//                    Log.d(TAG, "Right to Left swipe performed");
//                }
//
//                if (initialY < finalY) {
//                    Log.d(TAG, "Up to Down swipe performed");
//                }
//
//                if (initialY > finalY) {
//                    Log.d(TAG, "Down to Up swipe performed");
//                }

                break;

            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG,"Action was CANCEL");
                break;

            case MotionEvent.ACTION_OUTSIDE:
                Log.d(TAG, "Movement occurred outside bounds of current screen element");
                break;
        }
        return super.onTouchEvent(event);
    }

    /*
             * Show a confirmation dialog. On confirmation ("Delete"), the
             * photo is deleted and the activity finishes.
             */
    private void deletePhoto() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(
                LabActivity.this);
        alert.setTitle(R.string.photo_delete_prompt_title);
        alert.setMessage(R.string.photo_delete_prompt_message);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.delete,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int which) {
                        getContentResolver().delete(
                                Images.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.MediaColumns.DATA + "=?",
                                new String[] { mDataPath });
                        finish();
                    }
                });
        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }
    
    /*
     * Show a chooser so that the user may pick an app for editing
     * the photo.
     */
    private void editPhoto() {
        final Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(mUri, PHOTO_MIME_TYPE);
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_edit_chooser_title)));
    }
    
    /*
     * Show a chooser so that the user may pick an app for sending
     * the photo.
     */
    private void sharePhoto() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(PHOTO_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, mUri);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.photo_send_extra_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.photo_send_extra_text));
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_send_chooser_title)));
    }
}
