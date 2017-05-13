package br.com.frintter.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

import br.com.frintter.R;

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public final class LabActivity extends AppCompatActivity {
    
    public static final String PHOTO_FILE_EXTENSION = ".png";
    public static final String PHOTO_MIME_TYPE = "image/png";
    
    public static final String EXTRA_PHOTO_URI =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_DATA_PATH";
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final Integer MAX_LINHAS = 10;
    private static final Integer correcaoX = 0; //150;
    private static final Integer correcaoY = 0;//50;
    public static double EXTRA_PHOTO_MAT;
    private static Scalar[] corLinha = {
            new Scalar(255, 255, 0),   // Amarelo
            new Scalar(153, 204, 50),  // Amarelo Esverdeado
            new Scalar(0, 127, 255),   // Azul Ardósia
            new Scalar(255, 127, 0),   // Coral
            new Scalar(255, 0, 255),   // Magenta
            new Scalar(50, 205, 50),   // Verde Limão
            new Scalar(255, 0, 0),     // Vermelho
            new Scalar(112, 147, 219), // Turquesa Escuro
            new Scalar(217, 217, 25),  // Bright Ouro
            new Scalar(77, 77, 255)    // Azul Neon
    };
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

    public static double converterDoubleDoisDecimais(double distaciaDouble) {
        DecimalFormat fmt = new DecimalFormat("0.00");
        String string = fmt.format(distaciaDouble);
        String[] part = string.split("[,]");
        String string2 = part[0] + "." + part[1];
        return Double.parseDouble(string2);
    }
    
    public void pintarTela(){
        imageView = new ImageView(this);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getX();
                        initialY = event.getY();

                        if (numLinhas < MAX_LINHAS) {
                            Imgproc.circle(imagem, new Point(initialX, initialY), 8, corLinha[numLinhas], 3);
                            Imgcodecs.imwrite(mDataPath, imagem);
                            pintarTela();
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        float finalX = event.getX() - correcaoX;
                        float finalY = event.getY() - correcaoY;

                        double d = distance(new Point(initialX, initialY), new Point(finalX, finalY)) / EXTRA_PHOTO_MAT;

                        if (numLinhas < MAX_LINHAS) {
                            Imgproc.line(imagem, new Point(initialX, initialY), new Point(finalX, finalY), corLinha[numLinhas], 4);
                            Imgproc.putText(imagem, " " + (converterDoubleDoisDecimais(d)) + " cm", new Point(finalX, finalY), 1, 3, corLinha[numLinhas], 4);
                            Imgproc.circle(imagem, new Point(finalX, finalY), 8, corLinha[numLinhas], 3);
                            Imgcodecs.imwrite(mDataPath, imagem);
                            pintarTela();
                        }

                        numLinhas += 1;
                        break;
                }
                return true;
            }
        });

        imageView.setImageURI(mUri);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        setContentView(imageView);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        mDataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,
                this, mLoaderCallback);

        imagem = imread(mDataPath, CV_LOAD_IMAGE_COLOR);

        if (imagem.empty()){
            Log.d(TAG, "Image cannot found.");
        }else{
            pintarTela();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public double distance(Point p, Point q) {
        double deltaX = p.y - q.y;
        double deltaY = p.x - q.x;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

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
}
