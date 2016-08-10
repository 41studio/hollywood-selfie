package com.fourtyonestudio.hollywoodselfie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity implements View.OnTouchListener {
    @Bind(R.id.camera_preview)
    RelativeLayout cameraPreview;
    //    @Bind(R.id.img_artis)
//    ImageView imgArtis;
    @Bind(R.id.btn_capture)
    ImageView btnCapture;
    @Bind(R.id.btn_add_image)
    ImageView btnAddImage;
    @Bind(R.id.menu_bottom)
    LinearLayout menuBottom;
    @Bind(R.id.img_preview)
    ImageView imgPreview;
    @Bind(R.id.menu_filter)
    LinearLayout menuFilter;
    @Bind(R.id.layout_preview)
    RelativeLayout layoutPreview;
    @Bind(R.id.photo1)
    ImageView photo1;
    @Bind(R.id.photo2)
    ImageView photo2;
    @Bind(R.id.layout_photo)
    LinearLayout layoutPhoto;
    @Bind(R.id.layout_menu_bottom)
    LinearLayout layoutMenuBottom;

    ImageView imgArtis;
    @Bind(R.id.main)
    RelativeLayout main;
    @Bind(R.id.saturation_bar)
    SeekBar saturationBar;
    @Bind(R.id.brightness_bar)
    SeekBar brightnessBar;
    @Bind(R.id.contrast_bar)
    SeekBar contrastBar;
    @Bind(R.id.btn_switch)
    ImageView btnSwitch;
    @Bind(R.id.btn_save)
    Button btnSave;


    private Camera mCamera;
    private CameraPreview mPreview;
    private PictureCallback mPicture;
    private Context myContext;
    private boolean cameraFront = false;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float d = 0f;
    private float newRot = 0f;
    private float[] lastEvent = null;
    Bitmap bitmap = null;

    private boolean isOpen = false;
    private boolean afterCapture = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                btnSwitch.setVisibility(View.GONE);
            }
            mCamera = Camera.open(findBackFacingCamera());
            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    public void initialize() {
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);
        main.setDrawingCacheEnabled(true);

        imgArtis = (ImageView) findViewById(R.id.img_artis);
        imgArtis.setOnTouchListener(this);

        SeekBar satBar = (SeekBar) findViewById(R.id.saturation_bar);
        satBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                //progress = progresValue;
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
                loadBitmapSat();
            }
        });


        SeekBar seekbarbrightness = (SeekBar) findViewById(R.id.brightness_bar);
        seekbarbrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {

                // TODO Auto-generated method stub
                int brightness;
                brightness = progress;
                Bitmap resultBitmap = doBrightness(bitmap, brightness);
                imgArtis.setImageBitmap(resultBitmap);
            }
        });


    }

    @OnClick(R.id.btn_switch)
    void switchClick() {
        //get the number of cameras
        int camerasNumber = Camera.getNumberOfCameras();
        if (camerasNumber > 1) {
            //release the old camera instance
            //switch camera, from the front and the back and vice versa

            releaseCamera();
            chooseCamera();
        } else {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
            toast.show();
        }
    }


    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    Bitmap bitmapCamera;


    private PictureCallback getPictureCallback() {
        PictureCallback picture = new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //make a new picture file
                File pictureFile = getOutputMediaFile();

                if (pictureFile == null) {
                    return;
                }
                try {
                    //write the file
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                    toast.show();


                    showLayoutAfterCapture();


                    bitmapCamera = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                    if (!cameraFront) {
                        imgPreview.setImageBitmap(RotateBitmap(bitmapCamera, 90));
                    } else {
                        imgPreview.setImageBitmap(RotateBitmap(bitmapCamera, 270));
                    }

                    imgPreview.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                    imgPreview.setScaleType(ImageView.ScaleType.FIT_XY);

//                    Display display = getWindowManager().getDefaultDisplay();
//                    Point size = new Point();
//                    display.getSize(size);
//                    int width = size.x;
//                    int height = size.y;
//
//                    previewImageView.getLayoutParams().height = height;
//                    previewImageView.getLayoutParams().width = width;


//                    Bitmap bitmapCamera = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
//                    save(overlayBitmapToCenter(bitmapCamera, bitmap));

                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }

                //refresh camera to continue preview
                mPreview.refreshCamera(mCamera);


//                Code for set bitmap on one canvas
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                //o.inJustDecodeBounds = true;
//                Bitmap cameraBitmapNull = BitmapFactory.decodeByteArray(data, 0,
//                        data.length, options);
//
//                int wid = options.outWidth;
//                int hgt = options.outHeight;
//                Matrix nm = new Matrix();
//
//                Camera.Size cameraSize = camera.getParameters().getPictureSize();
//                float ratio = relativeLayout.getHeight()*1f/cameraSize.height;
//                if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//                    nm.postRotate(90);
//                    nm.postTranslate(hgt, 0);
//                    wid = options.outHeight;
//                    hgt = options.outWidth;
//                    ratio = relativeLayout.getWidth()*1f/cameraSize.height;
//
//                }else {
//                    wid = options.outWidth;
//                    hgt = options.outHeight;
//                    ratio = relativeLayout.getHeight()*1f/cameraSize.height;
//                }
//
//                float[] f = new float[9];
//                matrix.getValues(f);
//
//                f[0] = f[0]/ratio;
//                f[4] = f[4]/ratio;
//                f[5] = f[5]/ratio;
//                f[2] = f[2]/ratio;
//                matrix.setValues(f);
//
//                Bitmap newBitmap = Bitmap.createBitmap(wid, hgt,
//                        Bitmap.Config.ARGB_8888);
//
//                Canvas canvas = new Canvas(newBitmap);
//                Bitmap cameraBitmap = BitmapFactory.decodeByteArray(data, 0,
//                        data.length, options);
//
//                canvas.drawBitmap(cameraBitmap, nm, null);
//                //cameraBitmap.recycle();
//
//                canvas.drawBitmap(bitmap, matrix, null);
//                //bitmap.recycle();
//
//
//                File storagePath = new File(
//                        Environment.getExternalStorageDirectory() + "/HollywoodSelfie/");
//                storagePath.mkdirs();
//
//                File myImage = new File(storagePath, Long.toString(System
//                        .currentTimeMillis()) + ".jpg");
//
//                try {
//                    FileOutputStream out = new FileOutputStream(myImage);
//                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//
//
//                    out.flush();
//                    out.close();
//                } catch (FileNotFoundException e) {
//                    Log.d("In Saving File", e + "");
//                } catch (IOException e) {
//                    Log.d("In Saving File", e + "");
//                }
//
//                mPreview.refreshCamera(mCamera);
            }
        };
        return picture;
    }

    @OnClick(R.id.btn_save)
    void saveImage() {

        Bitmap newBitmap = overlay(bitmapCamera, bitmap);

        File storagePath = new File(
                Environment.getExternalStorageDirectory() + "/HollywoodSelfie/");
        storagePath.mkdirs();

        File myImage = new File(storagePath, Long.toString(System
                .currentTimeMillis()) + ".jpg");

        try {
            FileOutputStream out = new FileOutputStream(myImage);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);


            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            Log.d("In Saving File", e + "");
        } catch (IOException e) {
            Log.d("In Saving File", e + "");
        }



//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmapCamera.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        byte[] dat = stream.toByteArray();
//
//        // Code for set bitmap on one canvas
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        //o.inJustDecodeBounds = true;
//        Bitmap cameraBitmapNull = BitmapFactory.decodeByteArray(dat, 0,
//                dat.length, options);
//
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int width = size.x;
//        int height = size.y;
//
//        int wid = options.outWidth;
//        int hgt = options.outHeight;
//        Matrix nm = new Matrix();
//
//        //Camera.Size cameraSize = cam.getParameters().getPictureSize();
//        float ratio = main.getHeight() * 1f / height;
//        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//            nm.postRotate(90);
//            nm.postTranslate(hgt, 0);
//            wid = options.outHeight;
//            hgt = options.outWidth;
//            ratio = main.getWidth() * 1f / height;
//
//        } else {
//            wid = options.outWidth;
//            hgt = options.outHeight;
//            ratio = main.getHeight() * 1f / height;
//        }
//
//        float[] f = new float[9];
//        matrix.getValues(f);
//
//        f[0] = f[0] / ratio;
//        f[4] = f[4] / ratio;
//        f[5] = f[5] / ratio;
//        f[2] = f[2] / ratio;
//        matrix.setValues(f);
//
//        Bitmap newBitmap = Bitmap.createBitmap(wid, hgt,
//                Bitmap.Config.ARGB_8888);
//
//        Canvas canvas = new Canvas(newBitmap);
//
//
//
//        Bitmap cameraBitmap = BitmapFactory.decodeByteArray(dat, 0,
//                dat.length, options);
//
//        canvas.drawBitmap(cameraBitmap, nm, null);
//        //cameraBitmap.recycle();
//
//        canvas.drawBitmap(bitmap, matrix, null);
//        //bitmap.recycle();
//
//
//        File storagePath = new File(
//                Environment.getExternalStorageDirectory() + "/HollywoodSelfie/");
//        storagePath.mkdirs();
//
//        File myImage = new File(storagePath, Long.toString(System
//                .currentTimeMillis()) + ".jpg");
//
//        try {
//            FileOutputStream out = new FileOutputStream(myImage);
//            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//
//
//            out.flush();
//            out.close();
//        } catch (FileNotFoundException e) {
//            Log.d("In Saving File", e + "");
//        } catch (IOException e) {
//            Log.d("In Saving File", e + "");
//        }

    }

    @OnClick(R.id.btn_capture)
    void captureClick() {
        //
//
//            all.setDrawingCacheEnabled(true);
//            bm = all.getDrawingCache();
//
//            if (bm != null) {
//                Log.d("bitmap", "not null");
//                PinchToZoomImageView imageView = (PinchToZoomImageView) findViewById(R.id.testView);
//                imageView.setImageBitmap(bm);
//            } else {
//                Log.d("bitmap", "null");
//            }
//            save(bm);
//


        mCamera.takePicture(null, null, mPicture);
    }


    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap overlayBitmapToCenter(Bitmap bitmap1, Bitmap bitmap2) {
        int bitmap1Width = bitmap1.getWidth();
        int bitmap1Height = bitmap1.getHeight();
        int bitmap2Width = bitmap2.getWidth();
        int bitmap2Height = bitmap2.getHeight();

        float marginLeft = (float) (bitmap1Width * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float) (bitmap1Height * 0.5 - bitmap2Height * 0.5);

        Bitmap overlayBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.getConfig());
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawBitmap(bitmap1, new Matrix(), null);
        canvas.drawBitmap(bitmap2, marginLeft, marginTop, null);
        return overlayBitmap;
    }


    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }


    public Bitmap combineImages(Bitmap frame, Bitmap image) {
        Bitmap cs = null;
        Bitmap rs = null;

        rs = Bitmap.createScaledBitmap(frame, image.getWidth() + 50,
                image.getHeight() + 50, true);

        cs = Bitmap.createBitmap(rs.getWidth(), rs.getHeight(),
                Bitmap.Config.RGB_565);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(image, 25, 25, null);
        comboImage.drawBitmap(rs, 0, 0, null);
        if (rs != null) {
            rs.recycle();
            rs = null;
        }
        Runtime.getRuntime().gc();
        return cs;
    }


    public void save(Bitmap mBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

        try {
            //you can create a new file name "test.jpg" in sdcard folder.
            File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "test.jpg");
            f.createNewFile();

            //write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

            // remember close de FileOutput
            fo.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @OnClick(R.id.btn_add_image)
    void addImage() {
        if (isOpen) {
            isOpen = false;
            layoutPhoto.setVisibility(View.GONE);
        } else {
            isOpen = true;
            layoutPhoto.setVisibility(View.VISIBLE);
        }

    }

    @OnClick(R.id.photo1)
    void setPhoto1() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.zyan);
        imgArtis.setImageBitmap(bitmap);
    }

    @OnClick(R.id.photo2)
    void setPhoto2() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ariana);
        imgArtis.setImageBitmap(bitmap);
    }


    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "HollywoodSelfie");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        // handle touch events here
        ImageView view = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    if (lastEvent != null && event.getPointerCount() == 3) {
                        newRot = rotation(event);
                        float r = newRot - d;
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float tx = values[2];
                        float ty = values[5];
                        float sx = values[0];
                        float xc = (view.getWidth() / 2) * sx;
                        float yc = (view.getHeight() / 2) * sx;
                        matrix.postRotate(r, tx + xc, ty + yc);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix);

        return true;
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Calculate the degree to be rotated by.
     *
     * @param event
     * @return Degrees
     */
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private void showLayoutBeforeCapture() {
//        cameraPreview.setVisibility(View.VISIBLE);
//        layoutMenuBottom.setVisibility(View.VISIBLE);
//        cameraPreview.setVisibility(View.VISIBLE);
//        imgPreview.setVisibility(View.GONE);
//        layoutPreview.setVisibility(View.GONE);

        afterCapture = false;

        Intent intent = getIntent();
        finish();
        startActivity(intent);

//        this.recreate();


    }

    private void showLayoutAfterCapture() {
        cameraPreview.setVisibility(View.GONE);
        layoutMenuBottom.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);
        imgPreview.setVisibility(View.VISIBLE);
        layoutPreview.setVisibility(View.VISIBLE);
        layoutPreview.bringToFront();

        afterCapture = true;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (afterCapture) {
            showLayoutBeforeCapture();
        } else {
            finish();
        }
    }

    private void loadBitmapSat() {
        if (bitmap != null) {

            int progressSat = saturationBar.getProgress();

            //Saturation, 0=gray-scale. 1=identity
            float sat = (float) progressSat / 256;
            //satText.setText("Saturation: " + String.valueOf(sat));
            imgArtis.setImageBitmap(updateSat(bitmap, sat));
        }
    }

    private Bitmap updateSat(Bitmap src, float settingSat) {

        int w = src.getWidth();
        int h = src.getHeight();

        Bitmap bitmapResult =
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvasResult = new Canvas(bitmapResult);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(settingSat);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvasResult.drawBitmap(src, 0, 0, paint);

        return bitmapResult;
    }

    public static Bitmap doBrightness(Bitmap src, int value) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                // increase/decrease each channel
                R += value;
                if (R > 255) {
                    R = 255;
                } else if (R < 0) {
                    R = 0;
                }

                G += value;
                if (G > 255) {
                    G = 255;
                } else if (G < 0) {
                    G = 0;
                }

                B += value;
                if (B > 255) {
                    B = 255;
                } else if (B < 0) {
                    B = 0;
                }

                // apply new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return final image
        return bmOut;
    }


}