package co.median.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

public class CircleImageView extends AppCompatImageView {

    private static final int KEY_SHADOW_COLOR = 0x1E000000;
    private static final int FILL_SHADOW_COLOR = 0x3D000000;
    private static final float X_OFFSET = 0f;
    private static final float Y_OFFSET = 1.75f;
    private static final float SHADOW_RADIUS = 3.5f;
    private static final int SHADOW_ELEVATION = 4;

    private Animation.AnimationListener mListener;
    int mShadowRadius;

    private final Path path = new Path();
    private final RectF rect = new RectF();
    private boolean isCircularClip = false;

    public CircleImageView(Context context) {
        super(context);
        isCircularClip = true;
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        isCircularClip = true;
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        isCircularClip = true;
    }

    public CircleImageView(Context context, int color) {
        super(context);
        isCircularClip = false;
        final float density = getContext().getResources().getDisplayMetrics().density;
        final int shadowYOffset = (int) (density * Y_OFFSET);
        final int shadowXOffset = (int) (density * X_OFFSET);

        mShadowRadius = (int) (density * SHADOW_RADIUS);

        ShapeDrawable circle;
        if (elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
        } else {
            OvalShape oval = new OvalShadow(mShadowRadius);
            circle = new ShapeDrawable(oval);
            setLayerType(View.LAYER_TYPE_SOFTWARE, circle.getPaint());
            circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset,
                    KEY_SHADOW_COLOR);
            final int padding = mShadowRadius;
            setPadding(padding, padding, padding, padding);
        }
        circle.getPaint().setColor(color);
        ViewCompat.setBackground(this, circle);
    }

    private boolean elevationSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!elevationSupported() && !isCircularClip) {
            setMeasuredDimension(getMeasuredWidth() + mShadowRadius * 2, getMeasuredHeight()
                    + mShadowRadius * 2);
        }
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }

    public void setBackgroundColorRes(int colorRes) {
        setBackgroundColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    public void setBackgroundColor(int color) {
        if (getBackground() instanceof ShapeDrawable) {
            ((ShapeDrawable) getBackground()).getPaint().setColor(color);
        } else {
            super.setBackgroundColor(color);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isCircularClip) {
            rect.set(0, 0, getWidth(), getHeight());
            path.reset();
            path.addOval(rect, Path.Direction.CW);
            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private Paint mShadowPaint;

        OvalShadow(int shadowRadius) {
            super();
            mShadowPaint = new Paint();
            mShadowRadius = shadowRadius;
            updateRadialGradient((int) rect().width());
        }

        @Override
        protected void onResize(float width, float height) {
            super.onResize(width, height);
            updateRadialGradient((int) width);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            final int viewWidth = CircleImageView.this.getWidth();
            final int viewHeight = CircleImageView.this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, viewWidth / 2, mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, viewWidth / 2 - mShadowRadius, paint);
        }

        private void updateRadialGradient(int diameter) {
            mRadialGradient = new RadialGradient(diameter / 2, diameter / 2,
                    mShadowRadius, new int[] { FILL_SHADOW_COLOR, Color.TRANSPARENT },
                    null, Shader.TileMode.CLAMP);
            mShadowPaint.setShader(mRadialGradient);
        }
    }
}
