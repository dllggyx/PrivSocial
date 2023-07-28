package the.dreams.wind.blendingdesktop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.Objects;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

/**
 * Before using the class actions ensure that all required permissions are set
 * @see <a href="https://stackoverflow.com/a/34549690/5690248">Stack answer</a>
 */
class ScreenshotMaker extends MediaProjection.Callback
        implements ImageReader.OnImageAvailableListener {
    interface Callback {
        void onScreenshotTaken(Bitmap bitmap);
    }

    private final static String VIRTUAL_DISPLAY_NAME = "virtual_display";

    private final MediaProjection mMediaProjection;
    private final int mScreenDpi;
    @NonNull
    private final WindowManager mWindowManager;
    @Nullable
    private VirtualDisplay mVirtualDisplay;
    @Nullable
    private Callback mPendingCallback;

    // ========================================== //
    // Lifecycle
    // ========================================== //

    ScreenshotMaker(Context context, @NonNull Intent screenCastData) {
        final Context appContext = context.getApplicationContext();
        final MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) appContext.getSystemService(MEDIA_PROJECTION_SERVICE);



        mMediaProjection = Objects.requireNonNull(mediaProjectionManager)
                .getMediaProjection(Activity.RESULT_OK, screenCastData);
        mMediaProjection.registerCallback(this, null);

        mWindowManager = (WindowManager) Objects.requireNonNull(
                appContext.getSystemService(Context.WINDOW_SERVICE));
        final Display defaultDisplay = mWindowManager.getDefaultDisplay();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        mScreenDpi = displayMetrics.densityDpi;
    }

    // ========================================== //
    // Actions
    // ========================================== //

    void takeScreenshot(final Callback callback) {
        prepareVirtualDisplay(makeImageReader());
        mPendingCallback = callback;
    }

    void release() {
        mMediaProjection.stop();
        mMediaProjection.unregisterCallback(this);
    }

    // ========================================== //
    // MediaProjection.Callback
    // ========================================== //

    @Override
    public void onStop() {
        super.onStop();
        releaseVirtualDisplay();
    }

    // ========================================== //
    // ImageReader.OnImageAvailableListener
    // ========================================== //

    @Override
    public void onImageAvailable(@NonNull ImageReader reader) {
        if (mPendingCallback == null) {
            return;
        }
        final Image image = reader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        final Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride,
                image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        //==================
        image.close();
        reader.close();
        mPendingCallback.onScreenshotTaken(bitmap);
        mPendingCallback = null;
    }

    // ========================================== //
    // Private
    // ========================================== //

    private void prepareVirtualDisplay(@NonNull ImageReader imageReader) {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(imageReader.getSurface());
            return;
        }
        final VirtualDisplay.Callback callback = new VirtualDisplay.Callback() {
            @Override
            public void onStopped() {
                super.onStopped();
                releaseVirtualDisplay();
            }
        };
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME,
                                                                imageReader.getWidth(),
                                                                imageReader.getHeight(),
                                                                mScreenDpi,
                                                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                                                imageReader.getSurface(),
                                                                callback, null);
    }

    private ImageReader makeImageReader() {
        final Display defaultDisplay = mWindowManager.getDefaultDisplay();
        Point displaySize = new Point();
        defaultDisplay.getRealSize(displaySize);
        final ImageReader imageReader = ImageReader.newInstance(displaySize.x, displaySize.y,
                PixelFormat.RGBA_8888, 1);
        imageReader.setOnImageAvailableListener(this, null);
        return imageReader;
    }

    private void releaseVirtualDisplay() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

}
