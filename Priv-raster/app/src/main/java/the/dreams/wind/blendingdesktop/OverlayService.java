package the.dreams.wind.blendingdesktop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Icon;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.Vector;

import org.opencv.android.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import the.dreams.wind.blendingdesktop.BitmapUtils;

import static android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
import static org.opencv.core.Core.rotate;
import static org.opencv.core.CvType.CV_8UC3;

public class OverlayService extends Service implements ScreenshotMaker.Callback,
        View.OnTouchListener {
    final static String INTENT_KEY_SCREEN_CAST_DATA = "INTENT_KEY_SCREEN_CAST_DATA";
    final static String INTENT_ACTION_START_OVERLAY = "INTENT_ACTION_START_OVERLAY";
    private final static String INTENT_ACTION_STOP = "INTENT_ACTION_STOP";

    private final static int NOTIFICATION_ID = 0xFFFF;
    private final static String NOTIFICATION_CHANNEL_ID = "overlay_notification_channel_id";
    private final static int RESTART_DELAY = 1024;
    private final static int IDLE_TIMEOUT = (int) (1024 * 1.5f);
    private final static int FADE_IN_OUT_DURATION = 512;
    private final Random mRandom = new Random();



    private ScreenshotMaker mScreenshotMaker;
    private OverlayImageView mImageView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mOverlayLayoutParams;
    private Handler mRestartHandler;
    private Runnable mRestartRunnable;
    private Handler mIdleHandler;
    private Runnable mIdleRunnable;
    private ViewPropertyAnimator mOverlayAnimator;

    // ========================================== //
    // Lifecycle
    // ========================================== //

    public OverlayService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();


        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mOverlayLayoutParams = overlayLayoutParams();
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Objects.requireNonNull(projectionManager).createScreenCaptureIntent();
        final int smallIconRes = R.drawable.ic_notification_foreground_service;
        final Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher_round);
        Notification mNotification = makeNotificationBuilder()
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notification_ticker))
                .setSmallIcon(smallIconRes)
                .setLargeIcon(largeIcon)
                .addAction(makeStopServiceAction())
                .build();
        startForeground(NOTIFICATION_ID, mNotification);
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        if (INTENT_ACTION_START_OVERLAY.equals(intent.getAction())) {
            addOverlay();
            Intent screenCastData = intent.getParcelableExtra(INTENT_KEY_SCREEN_CAST_DATA);
            mScreenshotMaker = new ScreenshotMaker(this, screenCastData);
            mScreenshotMaker.takeScreenshot(this);
        } else if (INTENT_ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        restartOverlay();
        mScreenshotMaker.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWindowManager.removeView(mImageView);
        mScreenshotMaker.release();
    }

    // ========================================== //
    // ScreenshotMaker.Callback
    // ========================================== //

    @Override
    public void onScreenshotTaken(Bitmap bitmap) {
        mImageView.setVisibility(View.VISIBLE);
        showDesktopScreenshot(bitmap, mImageView);
        fadeOverlay(true, null);
    }

    // ========================================== //
    // View.OnTouchListener
    // ========================================== //

    @Override
    public boolean onTouch(@NonNull View view, @NonNull MotionEvent event) {
        // Just for to silence lint warnings
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP :
                if (event.getEventTime() - event.getDownTime() < 512) {
                    view.performClick();
                }
                break;
        }

        restartIdleState();

        return true;
    }

    // ========================================== //
    // Private
    // ========================================== //

    private Notification.Action makeStopServiceAction() {
        Intent stopService = new Intent(getApplicationContext(), OverlayService.class);
        stopService.setAction(INTENT_ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopService, 0);
        CharSequence actionTitle = getText(R.string.notification_action_stop);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Icon icon = Icon.createWithResource(this, R.mipmap.ic_launcher);
            return new Notification.Action.Builder(icon, actionTitle, pendingIntent).build();
        } else {
            //noinspection deprecation
            return new Notification.Action(R.mipmap.ic_launcher, actionTitle, pendingIntent);
        }
    }

    private void restartIdleState() {
        if (mIdleHandler == null) {
            mIdleHandler = new Handler();
        }
        if (mIdleRunnable == null) {
            mIdleRunnable = () -> mScreenshotMaker.takeScreenshot(OverlayService.this);
        }
        mIdleHandler.removeCallbacks(mIdleRunnable);
        if (mImageView.getAlpha() > 0) {
            fadeOverlay(false, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIdleHandler.postDelayed(mIdleRunnable, IDLE_TIMEOUT);
                }
            });
        } else {
            mIdleHandler.postDelayed(mIdleRunnable, IDLE_TIMEOUT);
        }
    }

    private void restartOverlay() {
        // Halt all fading animations and hide the view
        if (mOverlayAnimator != null) {
            mOverlayAnimator.cancel();
        }
        if (mIdleHandler != null) {
            mIdleHandler.removeCallbacks(mIdleRunnable);
        }
        if (mImageView != null) {
            mImageView.setVisibility(View.INVISIBLE);
        }

        // Lazy init
        if (mRestartHandler == null) {
            mRestartHandler = new Handler();
        }
        if (mRestartRunnable == null) {
            mRestartRunnable = () -> {
                Context appContext = getApplicationContext();
                Intent mainActivityIntent = new Intent(appContext, MainActivity.class);
                mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(mainActivityIntent);
            };
        }

        // There is a need to wait for a short wile until screen rotation is finished
        mRestartHandler.removeCallbacks(mRestartRunnable);
        mRestartHandler.postDelayed(mRestartRunnable, RESTART_DELAY);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void makeNotificationChannel() {
        CharSequence name = getString(R.string.notification_channel_name);
        String description = getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
    }

    private Notification.Builder makeNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeNotificationChannel();
            return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        } else {
            //noinspection deprecation
            return new Notification.Builder(this);
        }
    }

    private WindowManager.LayoutParams overlayLayoutParams() {
        final int overlayFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // TYPE_SYSTEM_ALERT is the closest one to TYPE_APPLICATION_OVERLAY flag for
            // pre-O android OSes
            overlayFlag = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        final int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | FLAG_WATCH_OUTSIDE_TOUCH;
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayFlag,
                flags,
                PixelFormat.TRANSLUCENT);
    }

    private void addOverlay() {
        if (mImageView != null && mImageView.isAttachedToWindow()) {
            mWindowManager.removeView(mImageView);
        }
        mImageView = new OverlayImageView(this);

        mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImageView.setPadding(0,0,0,0);

        mImageView.setAlpha(0f);
        mImageView.setOnTouchListener(this);
        mImageView.setVisibility(View.INVISIBLE);
        mWindowManager.addView(mImageView, mOverlayLayoutParams);
    }


    private void fadeOverlay(boolean in, Animator.AnimatorListener animatorListener) {
        if (mOverlayAnimator != null) {
            mOverlayAnimator.cancel();
        }

        mOverlayAnimator = mImageView.animate().alpha(in ? 1.0f : 0f)
                .setDuration(FADE_IN_OUT_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(animatorListener);
    }

    private void showDesktopScreenshot(Bitmap screenshot, ImageView imageView) {
        // The goal is to position the bitmap such it is attached to top of the screen display, by
        // moving it under status bar and/or navigation buttons bar
        Rect displayFrame = new Rect();
        imageView.getWindowVisibleDisplayFrame(displayFrame);
        final int statusBarHeight = displayFrame.top;
        //ImageView.ScaleType.MATRIX：不改变原图大小从ImageView的左上角开始绘制，超过ImageView部分不再显示
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        Matrix imageMatrix = new Matrix();
        //函数：matrix.setTranslate(a,b)功能：像素点相对画布向右asp,向下移动bsp
        imageMatrix.setTranslate(-displayFrame.left, -statusBarHeight);
        //imageMatrix.setTranslate(0, 0);
        imageView.setImageMatrix(imageMatrix);


        //<editor-fold desc="* 1. 各类对其参数（已注释）">

        //(1)大图参数(1024×1920、微信)<有边框但是不计入foreground>：
        //int width = 1048,height = 1964
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 16, 217, width, height)

        //(2)小图参数(1024×1920被压缩)：
        //int width = 281,height = 525
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 190, 1219, width, height)

        //(3)带边框小图参数((1024+32)×(1920+60)被压缩)：
        //int width = 288,height = 540
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 188, 1211, width, height)

        //(4)九宫格参数(768*768加边框被压缩)：
        //int width = 267,height = 266
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 191, 1215, width, height)

        //(4)九宫格大图参数(768*768)：
        //int width = 1048,height = 1048
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 16, 676, width, height)

        //(5)大图参数(1024×1920、推特)<有边框但是不计入foreground>：
        //int width = 1048,height = 1964
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 16, 217, width, height)

        //(5)大图参数(1024×1920、facebook)<有边框但是不计入foreground>：
        //int width = 983,height = 1844
        //Bitmap foreground0=Bitmap.createBitmap(screenshot, 48, 308, width, height)


        //</editor-fold>


        //<editor-fold desc="* 2. 无边框大图小图解密（已注释）">

//        //前景bitmap生成
//        //int width = screenshot.getWidth(),height = screenshot.getHeight()-227;
//        int width = 983,height = 1844;
//        //大分辨率为1088×2400
//        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap mybitmap = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
//        Canvas canvas = new Canvas(mybitmap);
//        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
//        Bitmap foreground0=Bitmap.createBitmap(screenshot, 48, 308, width, height);  // 190 1219
//        System.out.println("==========="+screenshot.getWidth()+"============"+screenshot.getHeight());
//
//        //对foreground缩放，变为64的倍数后处理
//        Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,256,512);
//
//
//        //解密操作
//        Mat imgcut = bitmapToMat(foreground);
//        int t = 0;
//
//
//        int M = 64, N = 64;
//        int SUB_WIDTH = foreground.getWidth() / M;
//        int SUB_HEIGHT = foreground.getHeight() / N;
//
//        //roi裁剪
//        Vector ceil_img = new Vector(4096);
//        Mat image_cut, roi_img;
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                org.opencv.core.Rect rect = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                image_cut = new Mat(imgcut, rect);
//                roi_img = image_cut.clone();
//                ceil_img.addElement(roi_img);
//            }
//        }
//        Mat MergeImage = imgcut.clone();
//        Vector result_img = new Vector(4096);
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                org.opencv.core.Rect ROI = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                result_img.addElement(new Mat(MergeImage,ROI));
//            }
//        }
//
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                Mat temp = (Mat)ceil_img.get(t);
//                temp.copyTo((Mat)result_img.get((t * 49) % 4096));
//                t++;
//            }
//        }
//
//        //处理拼接线
//        //MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
//
//        foreground = matToBitmap(MergeImage);
//
//
//        //将缩放后的bitmap还原
//        Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);
//
//        //bitmap融合
//        //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
//        canvas.drawBitmap(foreground1, 48, 308, null);//前景  //190 1219



    //</editor-fold>

        //<editor-fold desc="* 3. 有边框大图小图解密（已注释）">

//        //前景bitmap生成
//        int width = 1048,height = 1048;
//        //大分辨率为1088×2400
//        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap mybitmap = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
//        Canvas canvas = new Canvas(mybitmap);
//        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
//        Bitmap foreground0=Bitmap.createBitmap(screenshot, 16, 217, width, height);  // 188 1211
//        System.out.println("==========="+screenshot.getWidth()+"============"+screenshot.getHeight());
//
//        //对foreground缩放，变为64的倍数后处理
//        Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,330,528);  //必须为66的倍数
//
//
//        //解密操作
//        Mat imgcut = bitmapToMat(foreground);
//        int t = 0;
//
//
//        int M = 64, N = 64;
//        int SUB_WIDTH = foreground.getWidth() / 66;
//        int SUB_HEIGHT = foreground.getHeight() / 66;
//
//        //roi裁剪
//        Vector ceil_img = new Vector(4096);
//        Mat image_cut, roi_img;
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                org.opencv.core.Rect rect = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                image_cut = new Mat(imgcut, rect);
//                roi_img = image_cut.clone();
//                ceil_img.addElement(roi_img);
//            }
//        }
//        Mat MergeImage = imgcut.clone();
//        Vector result_img = new Vector(4096);
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                org.opencv.core.Rect ROI = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                result_img.addElement(new Mat(MergeImage,ROI));
//            }
//        }
//
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                Mat temp = (Mat)ceil_img.get(t);
//                temp.copyTo((Mat)result_img.get((t * 49) % 4096));
//                t++;
//            }
//        }
//
//        //处理拼接线
//        MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
//
//        foreground = matToBitmap(MergeImage);
//
//
//        //将缩放后的bitmap还原
//        Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);
//
//        //bitmap融合
//        //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
//        canvas.drawBitmap(foreground1, 16, 217, null);//前景  //188 1211



        //</editor-fold>

        //<editor-fold desc="* 4. 有边框九宫格小图解密（启用中）">

//        //前景bitmap生成
//        //int width = screenshot.getWidth(),height = screenshot.getHeight()-227;
//
//
//        int width = 266,height = 266;
//        //大分辨率为1088×2400
//        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap mybitmap = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
//        Canvas canvas = new Canvas(mybitmap);
//        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
//
//
//        for(int q = 0;q<3;q++)
//        {
//            for(int p = 0;p < 3;p++)
//            {
//                Bitmap foreground0=Bitmap.createBitmap(screenshot, 192 + p*275, 1215 + q*275, width, height);  // 188 1211
//                System.out.println("==========="+screenshot.getWidth()+"============"+screenshot.getHeight());
//
//                //对foreground缩放，变为64的倍数后处理
//                Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,792,792);  //必须为66的倍数
//
//
//                //解密操作
//                Mat imgcut = bitmapToMat(foreground);
//                int t = 0;
//
//
//                int M = 64, N = 64;
//                int SUB_WIDTH = foreground.getWidth() / 66;
//                int SUB_HEIGHT = foreground.getHeight() / 66;
//
//                //roi裁剪
//                Vector ceil_img = new Vector(4096);
//                Mat image_cut, roi_img;
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        org.opencv.core.Rect rect = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                        image_cut = new Mat(imgcut, rect);
//                        roi_img = image_cut.clone();
//                        ceil_img.addElement(roi_img);
//                    }
//                }
//                Mat MergeImage = imgcut.clone();
//                Vector result_img = new Vector(4096);
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        org.opencv.core.Rect ROI = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                        result_img.addElement(new Mat(MergeImage,ROI));
//                    }
//                }
//
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        Mat temp = (Mat)ceil_img.get(t);
//                        temp.copyTo((Mat)result_img.get((t * 49) % 4096));
//                        t++;
//                    }
//                }
//
//                //处理拼接线
//                //MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
//                //锐化处理（未做）
//
//
//                foreground = matToBitmap(MergeImage);
//
//
//                //将缩放后的bitmap还原
//                Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);
//                //bitmap融合
//                //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
//                canvas.drawBitmap(foreground1, 192 + p*275, 1215 + q*275, null);//前景  //188 1214
//
//            }
//        }




        //</editor-fold>

        //<editor-fold desc="* 5. 有边框、等比例缩放、九宫格小图解密（启用中）">

//        //前景bitmap生成
//        int rate = 10;
//
//        int width = (int)(265.5*rate),height = (int)(265.5*rate);
//        //大分辨率为1088×2400
//        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap mybitmap0 = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
//        Bitmap mybitmap1 = BitmapUtils.scaleBitmap(mybitmap0,rate);
//        Canvas canvas = new Canvas(mybitmap1);
//        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
//
//
//        for(int q = 0;q<3;q++)
//        {
//            for(int p = 0;p < 3;p++)
//            {
//                Bitmap foreground0=Bitmap.createBitmap(mybitmap1, (192 + p*275)*rate, (1215 + q*275)*rate, width, height);  // 188 1211
//
//                //对foreground缩放，变为64的倍数后处理
//                Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,792,792);  //必须为66的倍数
//
//
//                //解密操作
//                Mat imgcut = bitmapToMat(foreground);
//                int t = 0;
//
//
//                int M = 64, N = 64;
//                int SUB_WIDTH = foreground.getWidth() / 66;
//                int SUB_HEIGHT = foreground.getHeight() / 66;
//
//                //roi裁剪
//                Vector ceil_img = new Vector(4096);
//                Mat image_cut, roi_img;
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        org.opencv.core.Rect rect = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                        image_cut = new Mat(imgcut, rect);
//                        roi_img = image_cut.clone();
//                        ceil_img.addElement(roi_img);
//                    }
//                }
//                Mat MergeImage = imgcut.clone();
//                Vector result_img = new Vector(4096);
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        org.opencv.core.Rect ROI = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                        result_img.addElement(new Mat(MergeImage,ROI));
//                    }
//                }
//
//                for (int j = 0; j < N; j++)
//                {
//                    for (int i = 0; i < M; i++)
//                    {
//                        Mat temp = (Mat)ceil_img.get(t);
//                        temp.copyTo((Mat)result_img.get((t * 49) % 4096));
//                        t++;
//                    }
//                }
//
//                //处理拼接线
//                //MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
//                //锐化处理（未做）
//
//
//                foreground = matToBitmap(MergeImage);
//
//
//                //将缩放后的bitmap还原
//                Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);
//                //bitmap融合
//                //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
//                canvas.drawBitmap(foreground1, (192 + p*275)*rate, (1215 + q*275)*rate, null);//前景  //188 1214
//
//            }
//        }
//        Bitmap mybitmap = BitmapUtils.scaleBitmap(mybitmap1,0.1f);



        //</editor-fold>

        //<editor-fold desc="* 6. 有边框解异或大图解密">

//        //不带异或：(缩放大小为64倍数)
//        //Bitmap foreground0=Bitmap.createBitmap(mybitmap, 48, 308, width, height);
//        //int width = 983,height = 1844;
//
//        //带异或：(缩放大小为64倍数)
//        //Bitmap foreground0=Bitmap.createBitmap(mybitmap, 48, 308, width, height);
//        //int width = 983,height = 1844;
//
//        //前景bitmap生成
//        int width = 1046,height = 1962;
//        //大分辨率为1088×2400
//        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap mybitmap = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
//        Canvas canvas = new Canvas(mybitmap);
//        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
//        Bitmap foreground0=Bitmap.createBitmap(mybitmap, 17, 250, width, height);  // 188 1211
//        System.out.println("==========="+screenshot.getWidth()+"============"+screenshot.getHeight());
//
//        //对foreground缩放，变为66的倍数后处理
//        //Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,330,528);  //必须为66的倍数
//        Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,320,512);
//
//        //解密操作
//        Mat imgcut = bitmapToMat(foreground);
//        int t = 0;
//
//
//        int M = 64, N = 64;
//        int SUB_WIDTH = foreground.getWidth() / 64;//66;
//        int SUB_HEIGHT = foreground.getHeight() / 64;//66;
//
//        //roi裁剪
//        Vector ceil_img = new Vector(4096);
//        Mat image_cut, roi_img;
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                //org.opencv.core.Rect rect = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                org.opencv.core.Rect rect = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                image_cut = new Mat(imgcut, rect);
//                roi_img = image_cut.clone();
//                ceil_img.addElement(roi_img);
//            }
//        }
//        Mat MergeImage = imgcut.clone();
//        Vector result_img = new Vector(4096);
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                //org.opencv.core.Rect ROI = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                org.opencv.core.Rect ROI = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
//                result_img.addElement(new Mat(MergeImage,ROI));
//            }
//        }
//
//        for (int j = 0; j < N; j++)
//        {
//            for (int i = 0; i < M; i++)
//            {
//                Mat temp = (Mat)ceil_img.get(t);
//                temp.copyTo((Mat)result_img.get((t * 49) % 4096));
//                t++;
//            }
//        }
//
//
//
//        //种子密钥rand
//        Random rand = new Random(1234);
//        int k = 0;
//        //解密异或（解密用）
//        for (int times = 0; times < M * N; times++)
//        {
//        	//可添加处理程序
//        	Mat matrix_dst = (Mat)result_img.get(times);
//
//        	int rows = matrix_dst.rows();
//        	int cols = matrix_dst.cols();
//
//        	int r1 = (2*k)%255;//0b10101010;//(int) (_rand()%255);//(rand.nextInt(255));
//            k++;
//        	int g1 = (3*k)%255;//0b10101010;//(int) (_rand()%255);//(rand.nextInt(255));
//            k++;
//        	int b1 = (4*k)%255;//0b10101010;//(int) (_rand()%255);//(rand.nextInt(255));
//            k++;
//        	for (int row = 0; row < rows; row++)
//        	{
//        		for (int col = 0; col < cols; col++)
//        		{
//                    double[] values = matrix_dst.get(row,col);
//                    int r = (int)values[0];
//                    int g = (int)values[1];
//                    int b = (int)values[2];
//                    int []index = {row,col};
//                    //r g b a
//                    ((Mat) result_img.get(times)).put(index,r ^ r1,g ^ g1,b ^ b1,255);
//        		}
//        	}
//        }
//
//
//        //处理拼接线
//        //MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
//
//        foreground = matToBitmap(MergeImage);
//
//
//        //将缩放后的bitmap还原
//        Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);
//
//        //bitmap融合
//        //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
//        canvas.drawBitmap(foreground1, 16, 250, null);//前景  //188 1211



        //</editor-fold>

        //<editor-fold desc="* 7. 8*8像素块带边框加解密">

        //不带异或：(缩放大小为64倍数)
        //Bitmap foreground0=Bitmap.createBitmap(mybitmap, 48, 308, width, height);
        //int width = 983,height = 1844;

        //带异或：(缩放大小为64倍数)
        //Bitmap foreground0=Bitmap.createBitmap(mybitmap, 48, 308, width, height);
        //int width = 983,height = 1844;

        //前景bitmap生成
        int width = 1080,height = 1080; //1080*1080
        //大分辨率为1088×2400
        //Bitmap mybitmap = Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap mybitmap = Bitmap.createBitmap(screenshot, 0, 0, screenshot.getWidth(), screenshot.getHeight());
        Canvas canvas = new Canvas(mybitmap);
        //Bitmap foreground=Bitmap.createBitmap(screenshot, 30 + displayFrame.left, 30 + statusBarHeight, 1024, 1600);
        Bitmap foreground0=Bitmap.createBitmap(mybitmap, 0, 660, width, height);  // 0 660
        System.out.println("==========="+screenshot.getWidth()+"============"+screenshot.getHeight());

        //对foreground缩放，变为66的倍数后处理
        //Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,330,528);  //必须为66的倍数
        Bitmap foreground = BitmapUtils.scaleBitmap(foreground0,800,800);

        //解密操作
        Mat imgcut = bitmapToMat(foreground);
        int t = 0;


        int M = 100, N = 100;
        int SUB_WIDTH = foreground.getWidth() / 100;//66;
        int SUB_HEIGHT = foreground.getHeight() / 100;//66;

        //roi裁剪
        Vector ceil_img = new Vector(10000);
        Vector result_img = new Vector(10000);

        /*
        * 对从截图中截取出并缩放后的Mat对象imgcut进行处理
        * 首先将imgcut切块并存入ceil_img
        * */
        Mat image_cut, roi_img;
        for (int j = 0; j < N; j++)
        {
            for (int i = 0; i < M; i++)
            {
                //org.opencv.core.Rect rect = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
                org.opencv.core.Rect rect = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
                image_cut = new Mat(imgcut, rect);
                roi_img = image_cut.clone();
                ceil_img.addElement(roi_img);
            }
        }
        Mat MergeImage = imgcut.clone();

        //result_img是最终结果，现在给他初始化，便于分块处理
        for (int j = 0; j < N; j++)
        {
            for (int i = 0; i < M; i++)
            {
                //org.opencv.core.Rect ROI = new org.opencv.core.Rect((i+1) * SUB_WIDTH, (j+1) * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
                org.opencv.core.Rect ROI = new org.opencv.core.Rect(i * SUB_WIDTH, j * SUB_HEIGHT, SUB_WIDTH, SUB_HEIGHT);
                result_img.addElement(new Mat(MergeImage,ROI));
            }
        }

        for (int j = 0; j < N; j++)
        {
            for (int i = 0; i < M; i++)
            {
                Mat temp = (Mat)ceil_img.get((t * 129) % (M*N));
                temp.copyTo((Mat)result_img.get(t));
                t++;
            }
        }

        Random rand0 = new Random(5678);
        //翻转解密
        for(int i = 0;i<M*N;i++)
        {
            Mat temp;
            temp = flip_demo((Mat)result_img.get(i),rand0.nextInt(3));
            temp.copyTo((Mat)result_img.get(i));
        }

        //种子密钥rand
        Random rand = new Random(1234);
        int k = 0;
        //解密异或（解密用）
        for (int times = 0; times < M * N; times++)
        {
            //可添加处理程序
            //Mat matrix_dst = (Mat)result_img.get(times);
            //Mat temp = (Mat)result_img.get(times);
            Mat matrix_dst;
            matrix_dst = rotate_demo((Mat)result_img.get(times), 2 - rand.nextInt(3));
            int rows = matrix_dst.rows();
            int cols = matrix_dst.cols();

            int b1,g1,r1;
            if(rand.nextInt(2) == 0)
            {
                b1 = 0b00000000;
                g1 = 0b00000000;
                r1 = 0b00000000;
            }else
            {
                b1 = 0b11111111;
                g1 = 0b11111111;
                r1 = 0b11111111;
            }
            //int colorswap = rand.nextInt(6);
            for (int row = 0; row < rows; row++)
            {
                for (int col = 0; col < cols; col++)
                {
                    double[] values = matrix_dst.get(row,col);
                    int r = (int)values[0];
                    int g = (int)values[1];
                    int b = (int)values[2];
                    int []index = {row,col};
                    //r g b a
                    ((Mat) result_img.get(times)).put(index,r ^ r1,g ^ g1,b ^ b1,255);
//                    if(colorswap == 0)
//                        ((Mat) result_img.get(times)).put(index,r ^ r1,g ^ g1,b ^ b1,255);
//                    else if(colorswap == 1)
//                        ((Mat) result_img.get(times)).put(index,r ^ r1,b ^ b1,g ^ g1,255);
//                    else if(colorswap == 2)
//                        ((Mat) result_img.get(times)).put(index,g ^ g1,r ^ r1,b ^ b1,255);
//                    else if(colorswap == 3)
//                        ((Mat) result_img.get(times)).put(index,g ^ g1,b ^ b1,r ^ r1,255);
//                    else if(colorswap == 4)
//                        ((Mat) result_img.get(times)).put(index,b ^ b1,r ^ r1,g ^ g1,255);
//                    else if(colorswap == 5)
//                        ((Mat) result_img.get(times)).put(index,b ^ b1,g ^ g1,r ^ r1,255);
                }
            }
        }


        //处理拼接线
        MergeImage = smooth(MergeImage,SUB_WIDTH,SUB_HEIGHT);
        foreground = matToBitmap(MergeImage);


        /*
        * foreground是处理完成之后的bitmap，此时还是800*800，可以将foreground存储下来，作为解密图
        * 如何存储bitmap到手机相册:saveImageToGallery()
        * */
        saveImageToGallery(this,foreground);



        //将缩放后的bitmap还原
        Bitmap foreground1 = BitmapUtils.scaleBitmap(foreground,width,height);

        //bitmap融合
        //canvas.drawBitmap(foreground, 184 + displayFrame.left, 300 + statusBarHeight, null);//前景
        canvas.drawBitmap(foreground1, 0, 660, null);//前景  //0 660



        //</editor-fold>

        canvas.save();
        canvas.restore();
        imageView.setImageBitmap(mybitmap);


        //imageView.setImageBitmap(screenshot);
    }


    public Bitmap matToBitmap(Mat inputFrame) {
        Bitmap bitmap =Bitmap.createBitmap( inputFrame.width(),  inputFrame.height(),  Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputFrame,bitmap);
        return bitmap;
    }
    public Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }



    public Mat smooth(Mat MergeImage,int SUB_WIDTH,int SUB_HEIGHT) {
        //处理拼接线
        int M = 100,N = 100;
        for(int i = 1;i < M;i++)
        {//处理横向拼接线

            //读取当前像素位置
            //当前像素值等于上与下的平均值
            for(int k = 0;k < MergeImage.width();k++)
            {
//                MergeImage.get(i*SUB_WIDTH,k)[0] = 0b11111111;//(int)((MergeImage.get(i*SUB_WIDTH - 2,k)[0]*0.5 + MergeImage.get(i*SUB_WIDTH + 2,k)[0])*0.5);
//                MergeImage.get(i*SUB_WIDTH,k)[1] = 0b11111111;//(int)((MergeImage.get(i*SUB_WIDTH - 2,k)[1]*0.5 + MergeImage.get(i*SUB_WIDTH + 2,k)[1])*0.5);
//                MergeImage.get(i*SUB_WIDTH,k)[2] = 0b11111111;//(int)((MergeImage.get(i*SUB_WIDTH - 2,k)[2]*0.5 + MergeImage.get(i*SUB_WIDTH +2,k)[2])*0.5);
                //MergeImage.put(i*SUB_WIDTH,k,0b00000000,0b00000000,0b00000000,255);
                MergeImage.put(i*SUB_WIDTH-1,k,MergeImage.get(i*SUB_WIDTH-2,k)[0],MergeImage.get(i*SUB_WIDTH-2,k)[1],MergeImage.get(i*SUB_WIDTH-2,k)[2],255);
                MergeImage.put(i*SUB_WIDTH+1,k,MergeImage.get(i*SUB_WIDTH+2,k)[0],MergeImage.get(i*SUB_WIDTH+2,k)[1],MergeImage.get(i*SUB_WIDTH+2,k)[2],255);
                MergeImage.put(i*SUB_WIDTH,k,
                        (MergeImage.get(i*SUB_WIDTH-1,k)[0] + MergeImage.get(i*SUB_WIDTH+1,k)[0])*0.5,
                        (MergeImage.get(i*SUB_WIDTH-1,k)[1] + MergeImage.get(i*SUB_WIDTH+1,k)[1])*0.5,
                        (MergeImage.get(i*SUB_WIDTH-1,k)[2] + MergeImage.get(i*SUB_WIDTH+1,k)[2])*0.5,255);


            }

        }

        for(int j = 1;j < N ;j++)
        {//处理纵向拼接线
            //读取当前像素位置
            //当前像素值等于左与右的平均值
            for(int k = 0;k < MergeImage.height();k++)
            {
//                MergeImage.get(k,j*SUB_WIDTH)[0] = 0b11111111;//(int)((MergeImage.get(k,j*SUB_WIDTH)[0] + MergeImage.get(k,j*SUB_WIDTH)[0])*0.5);
//                MergeImage.get(k,j*SUB_WIDTH)[1] = 0b11111111;//(int)((MergeImage.get(k,j*SUB_WIDTH)[1] + MergeImage.get(k,j*SUB_WIDTH)[1])*0.5);
//                MergeImage.get(k,j*SUB_WIDTH)[2] = 0b11111111;//(int)((MergeImage.get(k,j*SUB_WIDTH)[2] + MergeImage.get(k,j*SUB_WIDTH)[2])*0.5);

                //MergeImage.put(k,j*SUB_WIDTH,0b00000000,0b00000000,0b00000000,255);
                MergeImage.put(k,j*SUB_WIDTH-1,MergeImage.get(k,j*SUB_WIDTH-2)[0],MergeImage.get(k,j*SUB_WIDTH-2)[1],MergeImage.get(k,j*SUB_WIDTH-2)[2],255);
                MergeImage.put(k,j*SUB_WIDTH+1,MergeImage.get(k,j*SUB_WIDTH+2)[0],MergeImage.get(k,j*SUB_WIDTH+2)[1],MergeImage.get(k,j*SUB_WIDTH+2)[2],255);
                MergeImage.put(k,j*SUB_WIDTH,
                        (MergeImage.get(k,j*SUB_WIDTH-1)[0] + MergeImage.get(k,j*SUB_WIDTH+1)[0])*0.5,
                        (MergeImage.get(k,j*SUB_WIDTH-1)[1] + MergeImage.get(k,j*SUB_WIDTH+1)[1])*0.5,
                        (MergeImage.get(k,j*SUB_WIDTH-1)[2] + MergeImage.get(k,j*SUB_WIDTH+1)[2])*0.5,255);


            }
        }
        return MergeImage;
    }



    public static long _next = 1234;

    public static long _rand(){
        _next = _next * 1103515245 + 12345;
        return ((_next / 65536) % 32768);
    }
    //图像翻转
    public static Mat flip_demo(Mat image,int flag)
    {
        Mat dst = image;
        if(flag == 0)//0
            Core.flip(image, dst, 0);  //上下翻转
        else if(flag == 1)//1
            Core.flip(image, dst, 1);  //左右翻转
        else//2
        {
            Mat temp = image;
            Core.flip(image, temp, 0);  //上下翻转
            Core.flip(temp, dst, 1);  //左右翻转
        }
        return dst;
    }

    //图像旋转
    public static Mat rotate_demo(Mat image,int flag)
    {
        Mat dst = image;
        //flag==0:90;flag==1:180;flag==2:270
        rotate(image, dst, flag);
        return dst;
    }


    public void saveImageToGallery(Context context, Bitmap bmp) {
        //检查有没有存储权限
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "请至权限中心打开应用权限", Toast.LENGTH_SHORT).show();
        } else {
            // 新建目录appDir，并把图片存到其下
            File appDir = new File(context.getExternalFilesDir(null).getPath()+ "BarcodeBitmap");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String fileName = System.currentTimeMillis() + ".jpg";
            File file = new File(appDir, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 把file里面的图片插入到系统相册中
            try {
                MediaStore.Images.Media.insertImage(context.getContentResolver(),
                        file.getAbsolutePath(), fileName, null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Toast.makeText(this, fileName, Toast.LENGTH_LONG);

            // 通知相册更新
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
    }

}
