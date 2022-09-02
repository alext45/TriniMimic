package com.example.trinimimic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.HapticFeedbackConstants;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.security.acl.Permission;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //Variables
    String[] permissionsList = new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE};
    Boolean frontCamera = false;

    //Views
    private PreviewView cameraPreview;
    public ImageView captureButton;
    public ImageView uploadButton;
    public ImageView mimicImage;
    public ImageView flipButton;
    public SeekBar alphaSlider;
    Vibrator vibrator;

    //Camera things
    CameraSelector cameraSelector;
    ProcessCameraProvider cameraProvider;
    ImageAnalysis imageAnalysis;
    Preview preview;


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void init(){
        //Make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide(); //Hide action bar

        //Handle Permissions
        Context context = getApplicationContext();
        for (String s : permissionsList) {
            if (ContextCompat.checkSelfPermission(context, s) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(permissionsList,1);
            }
        }

        //Initialize Views
        cameraPreview = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        uploadButton = findViewById(R.id.uploadButton);
        mimicImage = findViewById(R.id.mimicImage);
        flipButton = findViewById(R.id.flipCamera);
        alphaSlider = findViewById(R.id.alphaSlider);

        captureButton.setHapticFeedbackEnabled(true);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);



        startPreview(CameraSelector.LENS_FACING_BACK);
    }

    public void startPreview(int lensDirection){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                bindImageAnalysis(lensDirection);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(int lensDirection) {

        imageAnalysis = new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ImageProxy::close);
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {

            }
        };
        orientationEventListener.enable();

        preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensDirection).build();

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
    }


    public void captureImage(View view) throws FileNotFoundException {


        vibrator.vibrate(100);

        ImageCapture imageCapture =
                new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();


        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, imageAnalysis, preview);


        //Make file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis());
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+File.separator+"TriniMimic");
            Uri fileUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
            OutputStream out = resolver.openOutputStream(Objects.requireNonNull(fileUri));

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(out).build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            // insert your code here.
                            cameraProvider.unbind(imageCapture);
                            Toast.makeText(getApplicationContext(),"saved", Toast.LENGTH_SHORT).show();

                        }
                        @Override
                        public void onError(ImageCaptureException error) {
                            // insert your code here.
                            Toast.makeText(getApplicationContext(), error.toString(),Toast.LENGTH_LONG).show();
                        }
                    }
            );
        } else{
            File out = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+ File.separator+"TriniMimic"+File.separator+System.currentTimeMillis()+".jpeg");

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(out).build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            // insert your code here.
                            cameraProvider.unbind(imageCapture);
                            Toast.makeText(getApplicationContext(),"saved", Toast.LENGTH_SHORT).show();

                        }
                        @Override
                        public void onError(ImageCaptureException error) {
                            // insert your code here.
                            Toast.makeText(getApplicationContext(), error.toString(),Toast.LENGTH_LONG).show();
                        }
                    }
            );
        }

    }

    private void uploadImage() {
        Intent openDoc = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openDoc.addCategory(Intent.CATEGORY_OPENABLE);
        openDoc.setType("image/*");

        startActivityForResult(openDoc, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        //The shutter button of the camera
        captureButton.setOnClickListener(v -> {
            try {
                captureImage(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //This button is used to open the document picker
        uploadButton.setOnClickListener(v -> uploadImage());

        //when image is clicked its opacity goes to 75
        mimicImage.setOnClickListener(v -> {
            mimicImage.setImageAlpha(75);
            alphaSlider.setProgress(75);
        });

        //Slider that determines the opacity of the image
        alphaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mimicImage.setImageAlpha(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Flip the camera from front to back and vise versa
        flipButton.setOnClickListener(v -> {
            if(frontCamera){
                startPreview(CameraSelector.LENS_FACING_BACK);
                frontCamera = false;

                //Mirror the mimic Image
                mimicImage.setRotationY(0);
            } else {
                startPreview(CameraSelector.LENS_FACING_FRONT);
                frontCamera = true;

                //Un mirror the mimic Image
                mimicImage.setRotationY(180);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            Uri background = null;
            if (data != null) {
                background = data.getData();

                mimicImage.setImageURI(background);
                mimicImage.setImageAlpha(150);
                alphaSlider.setProgress(150);
            }
        }
    }
}