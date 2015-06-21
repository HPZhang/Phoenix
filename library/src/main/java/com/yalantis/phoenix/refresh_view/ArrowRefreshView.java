package com.yalantis.phoenix.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.yalantis.phoenix.PullToRefreshView;
import com.yalantis.phoenix.R;
import com.yalantis.phoenix.util.Utils;

public class ArrowRefreshView extends BaseRefreshView implements Animatable {

    private static final int ANIMATION_DURATION = 80;

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;

    private int mArrowSize = 32;
    private float mArrowLeftOffset;
    private float mArrowTopOffset;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mArrow;

    private boolean isRefreshing = false;

    public ArrowRefreshView(Context context, final PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();

        setupAnimations();
        parent.post(new Runnable() {
            @Override
            public void run() {
                initiateDimens(parent.getWidth());
            }
        });
    }

    public void initiateDimens(int viewWidth) {
        if (viewWidth <= 0 || viewWidth == mScreenWidth) return;

        mScreenWidth = viewWidth;

        mArrowSize          = Utils.convertDpToPixel(getContext(), mArrowSize);
        mArrowLeftOffset    = ((mScreenWidth - mArrowSize) / 2);
        mArrowTopOffset     = mParent.getTotalDragDistance() - mArrowSize;

        mTop = -mParent.getTotalDragDistance();

        createBitmaps();
    }

    private void createBitmaps() {
        mArrow = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.arrow);
        mArrow = Bitmap.createScaledBitmap(mArrow, mArrowSize, mArrowSize, true);
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mScreenWidth <= 0) return;

        final int saveCount = canvas.save();

        canvas.translate(0, mTop);
        canvas.clipRect(0, -mTop, mScreenWidth, mParent.getTotalDragDistance());

        drawArrow(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawArrow(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        /**
         * 如果到达箭头的指定位置后，减速移动箭头 */
        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        /**
         * 移动箭头 */
        float offsetX = mArrowLeftOffset;
        float offsetY = mArrowTopOffset
                - ((mParent.getTotalDragDistance() / 2) - (mArrowSize / 2)) * dragPercent;
        matrix.postTranslate(offsetX, offsetY);

        /**
         * 旋转箭头 */
        float degrees;
        float arrowRadius = (float) mArrowSize / 2.0f;
        offsetX += arrowRadius;
        offsetY += arrowRadius;
        if (isRefreshing) {
            degrees = 360 * mRotate;
        } else {
            degrees = (mPercent >= 1.0f ? 180f : 0);
        }
        matrix.postRotate(degrees, offsetX, offsetY);

        canvas.drawBitmap(mArrow, matrix, null);
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, ((int) (mScreenWidth * .65f)) + top);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.RESTART);
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

}