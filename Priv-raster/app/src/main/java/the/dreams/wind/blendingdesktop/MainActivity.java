package the.dreams.wind.blendingdesktop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Objects;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    private final static int PERMISSION_REQUEST_CODE = 0x0001;
    private final static int SCREEN_RECORDING_REQUEST_CODE = 0x0002;


    // ========================================== //
    // Lifecycle
    // ========================================== //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


//        View decor_View = getWindow().getDecorView();
//
//        int ui_Options = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//
//        decor_View.setSystemUiVisibility(ui_Options);



        //隐藏底部导航栏
//        getWindow().getAttributes().systemUiVisibility =
//                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE;

        if (requestPermissionIfNeeded()) {
            requestScreenCapture();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    // For the OS versions lower than M there should not be the case that it reached
                    // this part of code (they don't ask for this permission)
                    return;
                }
                if (Settings.canDrawOverlays(this)) {
                    requestScreenCapture();
                } else {
                    finishWithMessage(R.string.error_permission_overlay);
                }
                break;
            case SCREEN_RECORDING_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    launchOverlay(data);
                } else {
                    finishWithMessage(R.string.error_permission_screen_capture);
                }
                break;
        }
    }

    // ========================================== //
    // Private
    // ========================================== //

    private void finishWithMessage(@StringRes int resourceId) {
        Toast.makeText(this, resourceId, Toast.LENGTH_LONG).show();
        finish();
    }

    private void requestScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = Objects.requireNonNull(mediaProjectionManager).createScreenCaptureIntent();
        startActivityForResult(intent, SCREEN_RECORDING_REQUEST_CODE);
    }

    private void launchOverlay(@NonNull Intent screenCastData) {
        Intent toOverlayService = new Intent(this, OverlayService.class);
        toOverlayService.setAction(OverlayService.INTENT_ACTION_START_OVERLAY);
        toOverlayService.putExtra(OverlayService.INTENT_KEY_SCREEN_CAST_DATA, screenCastData);
        startService(toOverlayService);
        finish();
    }

    /**
     * @return true if explicit request is not needed. false otherwise
     */
    private Boolean requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (Settings.canDrawOverlays(this)) {
            return true;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        return false;
    }

    //启动opencv
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //openCV4Android 需要加载用到
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    //隐藏SystemUI
    public static boolean hideNavigation(Context context){
        boolean ishide;
        try
        {
            String command;
            command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib service call activity 42 s16 com.android.systemui";
            Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c",
                    command });
            proc.waitFor();
            ishide = true;
        }
        catch(Exception ex)
        {
            Toast.makeText(context, ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            ishide = false;
        }
        return ishide;
    }


    //显示SystemUI
    public static boolean showNavigation(){
        boolean isshow;
        try
        {
            String command;
            command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
            Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c",
                    command });
            proc.waitFor();
            isshow = true;
        }
        catch (Exception e)
        {
            isshow = false;
            e.printStackTrace();
        }
        return isshow;
    }

}
