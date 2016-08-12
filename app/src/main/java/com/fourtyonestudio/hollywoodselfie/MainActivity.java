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
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.fourtyonestudio.hollywoodselfie.utils.filters.ThumbnailCallback;
import com.fourtyonestudio.hollywoodselfie.utils.filters.ThumbnailItem;
import com.fourtyonestudio.hollywoodselfie.utils.filters.ThumbnailsAdapter;
import com.fourtyonestudio.hollywoodselfie.utils.filters.ThumbnailsManager;
import com.fourtyonestudio.hollywoodselfie.utils.images.CameraPreview;
import com.zomato.photofilters.SampleFilters;
import com.zomato.photofilters.imageprocessors.Filter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity implements ThumbnailCallback, View.OnTouchListener {
    @Bind(R.id.camera_preview)
    RelativeLayout cameraPreview;
    @Bind(R.id.img_preview)
    ImageView imgPreview;
    @Bind(R.id.layout_preview)
    RelativeLayout layoutPreview;
    @Bind(R.id.layout_photo)
    LinearLayout layoutPhoto;
    @Bind(R.id.layout_menu_bottom)
    LinearLayout layoutMenuBottom;
    @Bind(R.id.main)
    RelativeLayout main;
    @Bind(R.id.btn_switch)
    ImageView btnSwitch;
    @Bind(R.id.layout_filter)
    RelativeLayout layoutFilter;

    private ImageView imgArtist;
    private Camera mCamera;
    private CameraPreview mPreview;
    private PictureCallback mPicture;
    private Camera.Size cameraSize;
    private byte[] bitmapBytes;
    private Context context;
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

    private Bitmap artistBitmap = null;
    private Bitmap cameraBitmap = null;
    private Bitmap filterBitmap = null;
    private Bitmap overlayBitmap = null;

    private boolean isArtistContainerOpen = false;
    private boolean afterCapture = false;

    private int saturation = 0;
    private int contrast = 0;
    private int brightness = 0;

    //Initialization on layout filter
    static {
        System.loadLibrary("NativeImageProcessor");
    }

    private Activity activity;
    private RecyclerView thumbListView;
    private ImageView placeHolderImageView;
    private Bitmap finalBitmap = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;
        initialize();
    }

    //Initialize layout id
    public void initialize() {

        //Show camera using camera preview
        mPreview = new CameraPreview(context, mCamera);
        cameraPreview.addView(mPreview);

        //Screenshoot main layout
        main.setDrawingCacheEnabled(true);

        //Set OnTouchListener on imgArtist
        imgArtist = (ImageView) findViewById(R.id.img_artis);
        imgArtist.setOnTouchListener(this);

        //Set default artist photo
        setPhoto1();

        //Set OnSeekBarChangeListener on saturation bar, brightness bar, and contrast bar


        SeekBar saturationBar = (SeekBar) findViewById(R.id.saturation_bar);
        saturationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                overlayBitmap = setSaturation(artistBitmap, saturation);
                imgArtist.setImageBitmap(overlayBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {

                // TODO Auto-generated method stub

                saturation = progress;

            }
        });


        SeekBar brightnessBar = (SeekBar) findViewById(R.id.brightness_bar);
        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                overlayBitmap = setBrightness(artistBitmap, brightness);
                imgArtist.setImageBitmap(overlayBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {

                // TODO Auto-generated method stub

                brightness = progress;

            }
        });

        SeekBar contrastBar = (SeekBar) findViewById(R.id.contrast_bar);
        contrastBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub
                overlayBitmap = setContrast(artistBitmap, contrast);
                imgArtist.setImageBitmap(overlayBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {

                // TODO Auto-generated method stub

                contrast = progress;

            }
        });

    }


    ////////////////////////// Layout capture

    @OnClick(R.id.btn_switch)
    void switchClick() {
        //Get the number of cameras
        int camerasNumber = Camera.getNumberOfCameras();
        if (camerasNumber > 1) {
            //Release the old camera instance
            //Switch camera, from the front and the back and vice versa

            releaseCamera();
            chooseCamera();
        } else {
            Toast toast = Toast.makeText(context, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @OnClick(R.id.btn_capture)
    void captureClick() {
        mCamera.takePicture(null, null, mPicture);
    }

    @OnClick(R.id.btn_add_image)
    void addImage() {
        if (isArtistContainerOpen) {
            isArtistContainerOpen = false;
            layoutPhoto.setVisibility(View.GONE);
        } else {
            isArtistContainerOpen = true;
            layoutPhoto.setVisibility(View.VISIBLE);
        }

    }

    @OnClick(R.id.photo1)
    void setPhoto1() {
        artistBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floyd01);
        imgArtist.setImageBitmap(artistBitmap);
    }

    @OnClick(R.id.photo2)
    void setPhoto2() {
        artistBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floyd02);
        imgArtist.setImageBitmap(artistBitmap);
    }

    @OnClick(R.id.btn_close)
    void close() {
        isArtistContainerOpen = false;
        layoutPhoto.setVisibility(View.GONE);
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(context)) {
            Toast toast = Toast.makeText(context, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //If the front facing camera does not exist
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

    @Override
    protected void onPause() {
        super.onPause();
        //When on Pause, release camera in order to be used from other applications
        releaseCamera();
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
        //Get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //For every camera check
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

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //Open the backFacingCamera
                //Set a picture callback
                //Refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //Open the backFacingCamera
                //Set a picture callback
                //Refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }


    private boolean hasCamera(Context context) {
        //Check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private void releaseCamera() {
        //Stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    //OnTouch imgArtist
    public boolean onTouch(View v, MotionEvent event) {
        //Handle touch events here
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


    ////////////////////////// Layout preview

    private PictureCallback getPictureCallback() {
        PictureCallback picture = new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                cameraBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                //Handling camera result orientation
                if (!cameraFront) {
                    cameraBitmap = rotateBitmap(cameraBitmap, 90);
                    imgPreview.setImageBitmap(cameraBitmap);
                } else {
                    Bitmap bm = rotateBitmap(cameraBitmap, 270);
                    Matrix matrix = new Matrix();
                    //matrix.setRotate(270);
                    matrix.preScale(-1.0f, 1.0f);
                    cameraBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

                    imgPreview.setImageBitmap(cameraBitmap);
                }

                cameraSize = camera.getParameters().getPictureSize();
                bitmapBytes = data;

                imgPreview.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                showLayoutPreview();

                mPreview.refreshCamera(mCamera);

            }
        };
        return picture;
    }

    @OnClick(R.id.btn_edit)
    void editImage() {

        if (overlayBitmap == null) {
            overlayBitmap = artistBitmap;
        }

        // Code for set artistBitmap on one canvas
        BitmapFactory.Options options = new BitmapFactory.Options();
        //o.inJustDecodeBounds = true;
        Bitmap cameraBitmapNull = BitmapFactory.decodeByteArray(bitmapBytes, 0,
                bitmapBytes.length, options);

        int wid = options.outWidth;
        int hgt = options.outHeight;
        Matrix nm = new Matrix();

        float ratio = main.getHeight() * 1f / cameraSize.height;
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            nm.postRotate(90);
            nm.postTranslate(hgt, 0);
            wid = options.outHeight;
            hgt = options.outWidth;
            ratio = main.getWidth() * 1f / cameraSize.height;

        } else {
            wid = options.outWidth;
            hgt = options.outHeight;
            ratio = main.getHeight() * 1f / cameraSize.height;
        }

        float[] f = new float[9];
        matrix.getValues(f);

        f[0] = f[0] / ratio;
        f[4] = f[4] / ratio;
        f[5] = f[5] / ratio;
        f[2] = f[2] / ratio;
        matrix.setValues(f);


        filterBitmap = Bitmap.createBitmap(wid, hgt,
                Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(filterBitmap);
        cameraBitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0,
                bitmapBytes.length, options);

        //Handling bitmap on front camera
        if (cameraFront) {
            cameraBitmap = rotateBitmapFront(cameraBitmap, 180);

        }

        canvas.drawBitmap(cameraBitmap, nm, null);
        //cameraBitmap.recycle();

        canvas.drawBitmap(overlayBitmap, matrix, null);
        //overlayBitmap.recycle();


        //Go To Filter
        if (filterBitmap != null) {
            showLayoutFilter();

        } else {
            Toast.makeText(getApplicationContext(), "Bitmap null", Toast.LENGTH_LONG);
        }

        mPreview.refreshCamera(mCamera);


    }

    //Rotate artistBitmap from camera result
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //Handling rotate for bitmap from front camera
    public static Bitmap rotateBitmapFront(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        matrix.preScale(1.0f, -1.0f);

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    //Handle show layout capture
    private void showLayoutCapture() {

        afterCapture = false;

        Intent intent = getIntent();
        finish();
        startActivity(intent);


    }

    //Handle show layout preview
    private void showLayoutPreview() {
        cameraPreview.setVisibility(View.GONE);
        layoutMenuBottom.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);
        imgPreview.setVisibility(View.VISIBLE);
        layoutPreview.setVisibility(View.VISIBLE);
        layoutPreview.bringToFront();

        afterCapture = true;
    }

    //Handle show layout filter
    private void showLayoutFilter() {
        main.setVisibility(View.GONE);
        layoutMenuBottom.setVisibility(View.GONE);
        layoutPreview.setVisibility(View.GONE);
        layoutFilter.setVisibility(View.VISIBLE);
        layoutFilter.bringToFront();

        afterCapture = true;

        activity = this;
        initUIWidgets();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (afterCapture) {
            showLayoutCapture();
        } else {
            finish();
        }
    }


    //Set saturation
    private Bitmap setSaturation(Bitmap src, float settingSat) {

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

    //Set brightness
    public static Bitmap setBrightness(Bitmap src, int value) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output artistBitmap
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

                // apply new pixel color to output artistBitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return final image
        return bmOut;
    }


    //Set contrast
    private Bitmap setContrast(Bitmap src, double value) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output artistBitmap

        // create a mutable empty artistBitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());

        // create a canvas so that we can draw the bmOut Bitmap from source artistBitmap
        Canvas c = new Canvas();
        c.setBitmap(bmOut);

        // draw artistBitmap to bmOut from src artistBitmap so we can modify it
        c.drawBitmap(src, 0, 0, new Paint(Color.BLACK));


        // color information
        int A, R, G, B;
        int pixel;
        // get contrast value
        double contrast = Math.pow((100 + value) / 100, 2);

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                // apply filter contrast for every channel R, G, B
                R = Color.red(pixel);
                R = (int) (((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (R < 0) {
                    R = 0;
                } else if (R > 255) {
                    R = 255;
                }

                G = Color.green(pixel);
                G = (int) (((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (G < 0) {
                    G = 0;
                } else if (G > 255) {
                    G = 255;
                }

                B = Color.blue(pixel);
                B = (int) (((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if (B < 0) {
                    B = 0;
                } else if (B > 255) {
                    B = 255;
                }

                // set new pixel color to output artistBitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return bmOut;
    }

    ////////////////////////// Layout Filter

    boolean isFiltered = false;

    private void initUIWidgets() {
        thumbListView = (RecyclerView) findViewById(R.id.thumbnails);
        placeHolderImageView = (ImageView) findViewById(R.id.place_holder_imageview);
        placeHolderImageView.setImageBitmap(Bitmap.createScaledBitmap(filterBitmap, 640, 640, false));
        initHorizontalList();
    }

    private void initHorizontalList() {


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.scrollToPosition(0);
        thumbListView.setLayoutManager(layoutManager);
        thumbListView.setHasFixedSize(true);
        bindDataToAdapter();

        isFiltered = false;
    }

    private void bindDataToAdapter() {
        final Context context = this.getApplication();
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                Bitmap thumbImage = Bitmap.createScaledBitmap(filterBitmap, 640, 640, false);
                ThumbnailItem t1 = new ThumbnailItem();
                ThumbnailItem t2 = new ThumbnailItem();
                ThumbnailItem t3 = new ThumbnailItem();
                ThumbnailItem t4 = new ThumbnailItem();
                ThumbnailItem t5 = new ThumbnailItem();
                ThumbnailItem t6 = new ThumbnailItem();

                t1.image = thumbImage;
                t2.image = thumbImage;
                t3.image = thumbImage;
                t4.image = thumbImage;
                t5.image = thumbImage;
                t6.image = thumbImage;
                ThumbnailsManager.clearThumbs();
                ThumbnailsManager.addThumb(t1); // Original Image

                t2.filter = SampleFilters.getStarLitFilter();
                ThumbnailsManager.addThumb(t2);

                t3.filter = SampleFilters.getBlueMessFilter();
                ThumbnailsManager.addThumb(t3);

                t4.filter = SampleFilters.getAweStruckVibeFilter();
                ThumbnailsManager.addThumb(t4);

                t5.filter = SampleFilters.getLimeStutterFilter();
                ThumbnailsManager.addThumb(t5);

                t6.filter = SampleFilters.getNightWhisperFilter();
                ThumbnailsManager.addThumb(t6);

                List<ThumbnailItem> thumbs = ThumbnailsManager.processThumbs(context);

                ThumbnailsAdapter adapter = new ThumbnailsAdapter(thumbs, (ThumbnailCallback) activity);
                thumbListView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }

    @Override
    public void onThumbnailClick(Filter filter) {
        isFiltered = true;
        finalBitmap = filter.processFilter(Bitmap.createScaledBitmap(filterBitmap, 640, 640, false));
        placeHolderImageView.setImageBitmap(finalBitmap);
    }

    @OnClick(R.id.btn_save)
    void saveClick() {

        if (!isFiltered) {
            finalBitmap = filterBitmap;
        }

        File storagePath = new File(
                Environment.getExternalStorageDirectory() + "/HollywoodSelfie/");
        storagePath.mkdirs();

        File myImage = new File(storagePath, File.separator + "IMG_" + Long.toString(System
                .currentTimeMillis()) + ".jpg");

        try {
            FileOutputStream out = new FileOutputStream(myImage);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);


            out.flush();
            out.close();

            galleryAddPic(myImage.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.d("In Saving File", e + "");
        } catch (IOException e) {
            Log.d("In Saving File", e + "");
        }

        Toast toast = Toast.makeText(getApplicationContext(), "Picture saved: " + myImage.getAbsolutePath(), Toast.LENGTH_LONG);
        toast.show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //Set image to gallery
    private void galleryAddPic(String path) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

}