package br.com.frintter.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

import br.com.frintter.R;
import br.com.frintter.utils.Coordenadas;

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
    private static final Integer PONTO_INICIAL = 0;
    private static final Integer PONTO_FINAL = 1;

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
    private static Coordenadas coordenadas;
    private static int pontoInicialFinal = 0;
    public Mat imagem;
    public Mat[] imagemClone = new Mat[MAX_LINHAS];
    private Uri mUri;
    private String mDataPath;
    private Integer numLinhas = 0;
    private Integer linhaAnterior = -1;
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

    public void pintarTela() {
        imageView = new ImageView(this);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (numLinhas < MAX_LINHAS) {
                            coordenadas.setCoordenada(new Point(event.getX(), event.getY()), numLinhas);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (numLinhas < MAX_LINHAS) {
                            int tamanhoCirculo = 8;
                            int espessuraCirculo = 3;
                            int espessuraLinha = 4;

                            if (pontoInicialFinal == 0) {
                                // Desenha circulo no ponto inicial
                                Imgproc.circle(imagemClone[numLinhas], coordenadas.getCoordenada(numLinhas), tamanhoCirculo,
                                        corLinha[numLinhas], espessuraCirculo);
                                pontoInicialFinal = 1;
                            } else {
                                // Calcula distncia
                                double d = distance(coordenadas.getCoordenada(numLinhas - pontoInicialFinal),
                                        coordenadas.getCoordenada(numLinhas)) / EXTRA_PHOTO_MAT;
                                // Desenha linha entre ponto inicial e final
                                Imgproc.line(imagemClone[numLinhas], coordenadas.getCoordenada(numLinhas - pontoInicialFinal),
                                        coordenadas.getCoordenada(numLinhas), corLinha[numLinhas - pontoInicialFinal], espessuraLinha);
                                // Escreve a distancia calculada
                                Imgproc.putText(imagemClone[numLinhas], " " + (converterDoubleDoisDecimais(d)),
                                        coordenadas.getCoordenada(numLinhas), 1, 3, corLinha[numLinhas - pontoInicialFinal], espessuraLinha);
                                // Desenha circulo no ponto final
                                Imgproc.circle(imagemClone[numLinhas], coordenadas.getCoordenada(numLinhas), tamanhoCirculo,
                                        corLinha[numLinhas - pontoInicialFinal], espessuraCirculo);
                                // Quando clicado e a flag for final(1) seta para inicial(0)
                                pontoInicialFinal = 0;
                            }
//                            Imgcodecs.imwrite(mDataPath, imagemClone[numLinhas]);
                            pintarTela();
                            IncrementarNumerador();
                        }
                        break;
                }
                return true;
            }
        });

//        imageView.setImageURI(mUri);

        for (int i = numLinhas + 1; i < MAX_LINHAS; i++) {
            imagemClone[i] = imagemClone[numLinhas].clone();
        }

        final Bitmap bitmap = Bitmap.createBitmap(imagem.cols(), imagem.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imagemClone[numLinhas], bitmap);
        imageView.setImageBitmap(bitmap);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(imagem.width(), imagem.height());
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        imageView.setLayoutParams(lp);
        setContentView(imageView);
    }

    private void IncrementarNumerador() {
        // Incrementa numero de linhas
        numLinhas++;
        linhaAnterior++;
        // Incluido para realizar o metodo desfazer
        if (numLinhas >= MAX_LINHAS) {
            numLinhas = MAX_LINHAS;
            linhaAnterior = MAX_LINHAS - 1;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        mDataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);
        coordenadas = new Coordenadas();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,
                this, mLoaderCallback);

        imagem = imread(mDataPath, CV_LOAD_IMAGE_COLOR);

        for (int i = 0; i < MAX_LINHAS; i++) {
            imagemClone[i] = imagem.clone();
            Imgproc.cvtColor(imagemClone[i], imagemClone[i], Imgproc.COLOR_RGBA2BGR, 4);
        }

        if (imagem.empty()) {
            Log.d(TAG, "Image cannot found.");
        } else {
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
            case R.id.menu_desfazer:
                desfazerPhoto();
                return true;
            case R.id.menu_salvar:
                salvarPhoto();
                return true;
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

    private void desfazerPhoto() {
        int auxNumLinhas = numLinhas;

        if (numLinhas > 0) {
            if (pontoInicialFinal == 1) {
                pontoInicialFinal = 0;
            } else {
                pontoInicialFinal = 1;
            }
        }
        if (linhaAnterior > 0) {
            linhaAnterior--;
            numLinhas = linhaAnterior;
        } else {
            linhaAnterior = -1;
            numLinhas = 0;
            for (int i = 0; i < MAX_LINHAS; i++) {
                imagemClone[i] = imagem.clone();
                Imgproc.cvtColor(imagemClone[i], imagemClone[i], Imgproc.COLOR_RGBA2BGR, 4);
            }
        }
        pintarTela();

        numLinhas = --auxNumLinhas;
        if (numLinhas < 0) {
            numLinhas = 0;
        }
    }

    private void salvarPhoto() {
        Imgcodecs.imwrite(mDataPath, imagemClone[numLinhas]);
    }

    public double distance(Point pontoInicial, Point pontoFinal) {
        double deltaX = pontoInicial.y - pontoFinal.y;
        double deltaY = pontoInicial.x - pontoFinal.x;
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
                                new String[]{mDataPath});
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
