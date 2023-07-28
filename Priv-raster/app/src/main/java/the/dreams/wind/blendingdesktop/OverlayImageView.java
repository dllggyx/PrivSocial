package the.dreams.wind.blendingdesktop;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import java.util.Random;


class OverlayImageView extends ImageView {
    @NonNull
    private final Paint mOverlayPaint;

    // ========================================== //
    // Lifecycle
    // ========================================== //
    {
        mOverlayPaint = new Paint();
        //mOverlayPaint.setARGB(100, 255, 255, 255);  //ARGB A=0:transparent,A=255:opaque
        mOverlayPaint.setARGB(255 , 255, 255, 255);  //ARGB A=0:transparent,A=255:opaque

    }

    public OverlayImageView(Context context) {
        super(context);
    }

    public OverlayImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // ========================================== //
    // Accessors
    // ========================================== //


    //更改蒙版绘制这里可以动手
    void setOverlayColor(@SuppressWarnings("SameParameterValue") int color) {
        //设置画笔颜色
        mOverlayPaint.setColor(color);
        invalidate();
    }

    void setOverlayPorterDuffMode(@SuppressWarnings("SameParameterValue") PorterDuff.Mode mode) {
        mOverlayPaint.setXfermode(new PorterDuffXfermode(mode));
        invalidate();
    }

    // ========================================== //
    // View
    // ========================================== //

    //更改蒙版绘制这里可以动手
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        //from (30,30) to (30+16*64,30+25*64)=(30+1024,30+1600)


    }

    // For the purposes of silencing lint warnings all custom views that has custom onTouchListener
    // should override the performClick method
    @SuppressWarnings("EmptyMethod")
    @Override
    public boolean performClick() {
        return super.performClick();
    }


}
