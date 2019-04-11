package hearsilent.discreteslider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import hearsilent.discreteslider.libs.MoveGestureDetector;
import hearsilent.discreteslider.libs.Utils;

public class DiscreteSlider extends View {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_RANGE = 1;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mRectF = new RectF();

    private float mOffsetX = 0f;
    private float mMinOffsetX, mMaxOffsetX;

    private float mRadius, mTrackWidth;

    private int mTrackColor;
    private int mInactiveTrackColor;
    private int mThumbColor;
    private int mThumbPressedColor;

    private List<Object> mTickMarkPatterns;
    private int mTickMarkColor;
    private int mTickMarkInactiveColor;

    private int mValueLabelTextColor;

    private int mCount;
    private int mLeftProgress, mRightProgress = -1;
    private int mPaddingPosition = -1, mPressedPosition = -1;

    @Mode
    private int mMode = MODE_NORMAL;

    private Path mInactiveTrackPath = new Path();

    private MoveGestureDetector mMoveDetector;
    private float mValueLabelTextSize;
    private ValueLabelFormatter mValueLabelFormatter;
    private Rect mBounds = new Rect();
    private Path mValueLabelPath = new Path();
    private ValueAnimator mValueLabelAnimator;
    private float mValueLabelAnimValue = 0f;

    private float mWidth;

    private OnValueChangedListener mListener;

    @IntDef({MODE_NORMAL, MODE_RANGE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Mode {

    }

    public DiscreteSlider(Context context) {
        super(context);
        init(context, null);
    }

    public DiscreteSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DiscreteSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        mPaint.setStyle(Paint.Style.FILL);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiscreteSlider);

            mTrackWidth = a.getDimension(R.styleable.DiscreteSlider_trackWidth,
                    Utils.convertDpToPixel(4, getContext()));
            mTrackWidth = Math.max(mTrackWidth, Float.MIN_VALUE);
            mTrackColor = a.getColor(R.styleable.DiscreteSlider_trackColor, 0xff5123da);
            mInactiveTrackColor =
                    a.getColor(R.styleable.DiscreteSlider_inactiveTrackColor, 0x3d5123da);

            mRadius = a.getDimension(R.styleable.DiscreteSlider_thumbRadius,
                    Utils.convertDpToPixel(6, getContext()));
            mRadius = Math.max(mRadius, Float.MIN_VALUE);
            mThumbColor = a.getColor(R.styleable.DiscreteSlider_thumbColor, 0xff5123da);
            mThumbPressedColor =
                    a.getColor(R.styleable.DiscreteSlider_thumbPressedColor, 0x1f5123da);

            mTickMarkColor = a.getColor(R.styleable.DiscreteSlider_tickMarkColor, 0xff9972ed);
            mTickMarkInactiveColor =
                    a.getColor(R.styleable.DiscreteSlider_tickMarkInactiveColor, 0xff936ce2);

            mValueLabelTextColor =
                    a.getColor(R.styleable.DiscreteSlider_valueLabelTextColor, Color.WHITE);
            mValueLabelTextSize = a.getDimension(R.styleable.DiscreteSlider_valueLabelTextSize,
                    Utils.convertSpToPixel(16, context));

            mCount = a.getInt(R.styleable.DiscreteSlider_count, 11);
            mCount = Math.max(mCount, 2);
            mMode = a.getInt(R.styleable.DiscreteSlider_mode, MODE_NORMAL);

            mLeftProgress = a.getInt(R.styleable.DiscreteSlider_progress,
                    a.getInt(R.styleable.DiscreteSlider_leftProgress, 0));
            if (mMode == MODE_NORMAL) {
                mRightProgress = -1;
            } else {
                mRightProgress = a.getInt(R.styleable.DiscreteSlider_rightProgress, mCount - 1);
            }

            if (a.hasValue(R.styleable.DiscreteSlider_tickMarkPatterns)) {
                String patterns = a.getString(R.styleable.DiscreteSlider_tickMarkPatterns);
                if (!TextUtils.isEmpty(patterns)) {
                    float length = a.getDimension(R.styleable.DiscreteSlider_tickMarkDashLength,
                            Utils.convertDpToPixel(1, context));
                    mTickMarkPatterns = new ArrayList<>();
                    if (patterns.contains(",")) {
                        for (String pattern : patterns.split(",")) {
                            if (pattern.equalsIgnoreCase("dot")) {
                                mTickMarkPatterns.add(new Dot());
                            } else if (pattern.equalsIgnoreCase("dash")) {
                                mTickMarkPatterns.add(new Dash(length));
                            }
                        }
                    } else {
                        if (patterns.equalsIgnoreCase("dot")) {
                            mTickMarkPatterns.add(new Dot());
                        } else if (patterns.equalsIgnoreCase("dash")) {
                            mTickMarkPatterns.add(new Dash(length));
                        }
                    }
                }
                generateInactiveTrackPath();
            }

            setMode(mMode);

            a.recycle();
        } else {
            mTrackWidth = Utils.convertDpToPixel(4, context);
            mTrackColor = 0xff5123da;
            mInactiveTrackColor = 0x3d5123da;

            mRadius = Utils.convertDpToPixel(6, context);
            mThumbColor = 0xff5123da;
            mThumbPressedColor = 0x1f5123da;

            mTickMarkColor = 0xff9972ed;
            mTickMarkInactiveColor = 0xff936ce2;

            mValueLabelTextSize = Utils.convertSpToPixel(16, context);
            mValueLabelTextColor = Color.WHITE;

            mCount = 11;
        }

        setValueLabelFormatter(new ValueLabelFormatter() {

            @Override
            public String getLabel(int input) {
                return Integer.toString(input);
            }
        });

        mMoveDetector = new MoveGestureDetector(context, new MoveListener());
    }

    public void setTrackWidth(@FloatRange(from = Float.MIN_VALUE) float trackWidth) {
        if (trackWidth <= 0) {
            throw new IllegalArgumentException("Track width must be a positive number.");
        }
        mTrackWidth = trackWidth;
        generateInactiveTrackPath();
        invalidate();
    }

    public void setTrackColor(@ColorInt int trackColor) {
        mTrackColor = trackColor;
        invalidate();
    }

    public void setInactiveTrackColor(@ColorInt int inactiveTrackColor) {
        mInactiveTrackColor = inactiveTrackColor;
        invalidate();
    }

    public void setThumbRadius(@FloatRange(from = Float.MIN_VALUE) float radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Thumb radius must be a positive number.");
        }
        mRadius = radius;
        generateInactiveTrackPath();
        invalidate();
    }

    public void setThumbColor(@ColorInt int thumbColor) {
        mThumbColor = thumbColor;
        invalidate();
    }

    public void setThumbPressedColor(@ColorInt int thumbPressedColor) {
        mThumbPressedColor = thumbPressedColor;
        invalidate();
    }

    public void setTickMarkColor(@ColorInt int tickMarkColor) {
        mTickMarkColor = tickMarkColor;
        invalidate();
    }

    public void setTickMarkInactiveColor(@ColorInt int tickMarkInactiveColor) {
        mTickMarkInactiveColor = tickMarkInactiveColor;
        invalidate();
    }

    public void setValueLabelTextColor(@ColorInt int valueLabelTextColor) {
        mValueLabelTextColor = valueLabelTextColor;
        invalidate();
    }

    public void setValueLabelTextSize(@FloatRange(from = Float.MIN_VALUE) float valueLabelTextSize) {
        if (valueLabelTextSize <= 0) {
            throw new IllegalArgumentException("Value label text size must be a positive number.");
        }
        mValueLabelTextSize = valueLabelTextSize;
        invalidate();
    }

    public void setMode(@Mode int mode) {
        if (mode != MODE_RANGE && mode != MODE_NORMAL) {
            throw new IllegalArgumentException("Mode must be normal or range.");
        }
        mMode = mode;
        checkProgressBound();
        invalidate();
    }

    public void setCount(@IntRange(from = 2) int count) {
        if (count < 2) {
            throw new IllegalArgumentException("Count must larger than 2.");
        }
        mCount = count;
        checkProgressBound();
        invalidate();
    }

    public void setTickMarkPatterns(List<Object> patterns) {
        if (patterns == null) {
            mTickMarkPatterns = null;
        } else {
            for (Object pattern : patterns) {
                if (!(pattern instanceof Dot) && !(pattern instanceof Dash)) {
                    throw new IllegalArgumentException("Pattern only accepted dot or dash.");
                }
            }
            mTickMarkPatterns = patterns;
        }
        generateInactiveTrackPath();
        invalidate();
    }

    public void setValueLabelFormatter(@NonNull ValueLabelFormatter formatter) {
        mValueLabelFormatter = formatter;
    }

    public void setProgress(int progress) {
        setLeftProgress(progress);
    }

    public void setLeftProgress(int progress) {
        mLeftProgress = progress;
        checkProgressBound();
        invalidate();
    }

    public int getProgress() {
        return getLeftProgress();
    }

    public int getLeftProgress() {
        return mLeftProgress;
    }

    public void setRightProgress(int progress) {
        if (mMode != MODE_RANGE) {
            throw new IllegalStateException("Set right progress must be range mode.");
        }
        mRightProgress = progress;
        checkProgressBound();
        invalidate();
    }

    public int getRightProgress() {
        return mRightProgress;
    }

    private void checkProgressBound() {
        if (mMode != MODE_NORMAL) {
            if (mRightProgress == -1) {
                mRightProgress = mCount - 1;
            } else if (mRightProgress > mCount - 1) {
                mRightProgress = mCount - 1;
            }
            if (mLeftProgress >= mRightProgress) {
                mLeftProgress = mRightProgress - 1;
            }
        } else {
            mRightProgress = -1;
            if (mLeftProgress > mCount - 1) {
                mLeftProgress = mCount - 1;
            }
        }
    }

    public void setOnValueChangedListner(OnValueChangedListener listener) {
        mListener = listener;
    }

    private void generateInactiveTrackPath() {
        mWidth = getWidth() - getPaddingLeft() - getPaddingRight() - mRadius * 2 + mTrackWidth;
        float left = getPaddingLeft() + mRadius - mTrackWidth / 2f;
        float top = (getHeight() - mTrackWidth) / 2f + getPaddingTop();
        float right = left + mWidth;
        float bottom = top + mTrackWidth;
        float radius = mTrackWidth / 2f;

        mInactiveTrackPath.reset();
        if (mTickMarkPatterns != null && mTickMarkPatterns.size() > 0) {
            if (mTickMarkPatterns.get(0) instanceof Dot) {
                mRectF.set(left, top, left + mTrackWidth, bottom);
                mInactiveTrackPath.arcTo(mRectF, 90, 180, true);
            } else {
                mInactiveTrackPath.moveTo(left, bottom);
                mInactiveTrackPath.lineTo(left, top);
            }
            if (mTickMarkPatterns.get((mCount - 1) % mTickMarkPatterns.size()) instanceof Dot) {
                mInactiveTrackPath.lineTo(right - radius, top);
                mRectF.set(right - mTrackWidth, top, right, bottom);
                mInactiveTrackPath.arcTo(mRectF, -90, 180, true);
            } else {
                mInactiveTrackPath.lineTo(right, top);
                mInactiveTrackPath.lineTo(right, bottom);
            }
            if (mTickMarkPatterns.get(0) instanceof Dot) {
                mInactiveTrackPath.lineTo(left + radius, bottom);
            } else {
                mInactiveTrackPath.lineTo(left, bottom);
            }
            mInactiveTrackPath.close();
        } else {
            mRectF.set(left, top, right, bottom);
            mInactiveTrackPath.addRoundRect(mRectF, radius, radius, Path.Direction.CW);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        return isEnabled() && handleTouchEvent(event);
    }

    private boolean handleTouchEvent(MotionEvent event) {
        if (mCount < 2) {
            mMoveDetector.onTouchEvent(event);
            return true;
        }
        float width = mWidth - mTrackWidth;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mOffsetX = 0;
            float x = event.getX();
            if (mRightProgress == -1 && mMode == MODE_NORMAL) {
                float cx = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius;
                if (cx - mRadius * 3.5 <= x && x <= cx + mRadius * 3.5) {
                    mPaddingPosition = mLeftProgress;
                }
            } else {
                float cx1 = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius;
                float cx2 = getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius;
                if (cx1 - mRadius * 3.5 <= x && x <= cx1 + mRadius * 3.5) {
                    mPaddingPosition = mLeftProgress;
                } else if (cx2 - mRadius * 3.5 <= x && x <= cx2 + mRadius * 3.5) {
                    mPaddingPosition = mRightProgress;
                }
            }
            if (mPaddingPosition == -1) {
                mPaddingPosition = getClosestPosition(x, width);
            }

            x = getPaddingLeft() + width / (mCount - 1) * mPaddingPosition + mRadius;
            if (mPaddingPosition == mLeftProgress) {
                mMinOffsetX = (getPaddingLeft() + mRadius) - x;
                if (mRightProgress != -1 && mMode == MODE_RANGE) {
                    mMaxOffsetX = (getPaddingLeft() + width / (mCount - 1) * (mRightProgress - 1) +
                            mRadius) - x;
                } else {
                    mMaxOffsetX = (getPaddingLeft() + width + mRadius) - x;
                }
                mPressedPosition = mPaddingPosition;
            } else if (mPaddingPosition == mRightProgress && mMode != MODE_NORMAL) {
                mMinOffsetX =
                        (getPaddingLeft() + width / (mCount - 1) * (mLeftProgress + 1) + mRadius) -
                                x;
                mMaxOffsetX = (getPaddingLeft() + width + mRadius) - x;
                mPressedPosition = mPaddingPosition;
            } else if (!isClickable()) {
                mPaddingPosition = -1;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mPaddingPosition == -1) {
                mOffsetX = 0;
                mMoveDetector.onTouchEvent(event);
                return true;
            }
            if (mPaddingPosition != mLeftProgress && mPaddingPosition != mRightProgress) {
                float x = event.getX();
                final int position = getClosestPosition(x, width);
                if (position == mPaddingPosition) {
                    if (mRightProgress == -1 && mMode == MODE_NORMAL) {
                        mPaddingPosition = mLeftProgress;
                    } else {
                        if (Math.abs(mLeftProgress - position) <=
                                Math.abs(mRightProgress - position)) {
                            mPaddingPosition = mLeftProgress;
                        } else {
                            mPaddingPosition = mRightProgress;
                        }
                    }

                    if (mListener != null) {
                        if (mRightProgress != -1 && mMode != MODE_NORMAL) {
                            if (mPaddingPosition == mLeftProgress) {
                                mListener.onValueChanged(position, mRightProgress);
                            } else {
                                mListener.onValueChanged(mLeftProgress, position);
                            }
                        } else {
                            mListener.onValueChanged(position);
                        }
                    }

                    setEnabled(false);

                    mOffsetX = 0;
                    float dis = width / (mCount - 1) * (position - mPaddingPosition);
                    ValueAnimator animator = ValueAnimator.ofFloat(mOffsetX, mOffsetX + dis);
                    animator.setInterpolator(new AccelerateInterpolator());
                    animator.setDuration(100);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mOffsetX = (float) animation.getAnimatedValue();
                            invalidate();
                        }
                    });
                    animator.addListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mOffsetX = 0;
                            if (mPaddingPosition == mLeftProgress) {
                                mLeftProgress = position;
                            } else if (mPaddingPosition == mRightProgress && mMode != MODE_NORMAL) {
                                mRightProgress = position;
                            }
                            mPaddingPosition = -1;
                            setEnabled(true);

                            invalidate();
                        }
                    });
                    animator.start();
                }
            } else {
                float x = getPaddingLeft() + width / (mCount - 1) * mPaddingPosition + mRadius +
                        mOffsetX;
                float dis = Float.MAX_VALUE;
                int position = -1;
                for (int i = 0; i < mCount; i++) {
                    float _dis = (getPaddingLeft() + width / (mCount - 1) * i + mRadius) - x;
                    if (Math.abs(_dis) < Math.abs(dis)) {
                        dis = _dis;
                        position = i;
                    }
                }

                if (mListener != null) {
                    if (mRightProgress != -1 && mMode != MODE_NORMAL) {
                        if (mPaddingPosition == mLeftProgress) {
                            mListener.onValueChanged(position, mRightProgress);
                        } else {
                            mListener.onValueChanged(mLeftProgress, position);
                        }
                    } else {
                        mListener.onValueChanged(position);
                    }
                }

                setEnabled(false);

                final int _position = position;
                ValueAnimator animator = ValueAnimator.ofFloat(mOffsetX, mOffsetX + dis);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.setDuration(Math.round(200 * (Math.abs(dis) / (width / (mCount - 1)))));
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mOffsetX = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mOffsetX = 0;
                        if (mPaddingPosition == mLeftProgress) {
                            mLeftProgress = _position;
                        } else if (mPaddingPosition == mRightProgress && mMode != MODE_NORMAL) {
                            mRightProgress = _position;
                        }
                        if (mValueLabelAnimator == null) {
                            mPaddingPosition = -1;
                            setEnabled(true);
                        } else {
                            mPaddingPosition = _position;
                        }
                        invalidate();
                    }
                });
                animator.start();

                float value = mValueLabelAnimValue;
                if (mValueLabelAnimator != null) {
                    value = mValueLabelAnimator.getAnimatedFraction();
                    mValueLabelAnimator.cancel();
                }

                if (value > 0) {
                    mValueLabelAnimator = ValueAnimator.ofFloat(value, 0);
                    mValueLabelAnimator.setDuration(Math.round(250 * value));
                    mValueLabelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mValueLabelAnimValue = (float) animation.getAnimatedValue();
                            generateValueLabelPath();
                            invalidate();
                        }
                    });
                    mValueLabelAnimator.addListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mValueLabelAnimator = null;
                            if (mOffsetX == 0) {
                                mPaddingPosition = -1;
                                setEnabled(true);
                            }

                            invalidate();
                        }
                    });
                    mValueLabelAnimator.start();
                }
            }
            mPressedPosition = -1;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mPaddingPosition == mLeftProgress || mPaddingPosition == mRightProgress) {
                setEnabled(false);

                ValueAnimator animator = ValueAnimator.ofFloat(mOffsetX, 0);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.setDuration(100);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mOffsetX = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mOffsetX = 0;
                        mPaddingPosition = -1;
                        setEnabled(true);
                        invalidate();
                    }
                });
                animator.start();
            } else {
                mOffsetX = 0;
            }

            mPaddingPosition = -1;
            mPressedPosition = -1;
        }
        mMoveDetector.onTouchEvent(event);
        invalidate();
        return true;
    }

    private void animValueLabel() {
        mValueLabelAnimator = ValueAnimator.ofFloat(0, 1);
        mValueLabelAnimator.setDuration(250);
        mValueLabelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mValueLabelAnimValue = (float) animation.getAnimatedValue();
                generateValueLabelPath();
                invalidate();
            }
        });
        mValueLabelAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                (int) Math.max(Math.ceil(mRadius * 2 * 3), mTrackWidth) + getPaddingTop() +
                        getPaddingBottom());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        generateInactiveTrackPath();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = mWidth - mTrackWidth;
        mPaint.setColor(mInactiveTrackColor);
        canvas.drawPath(mInactiveTrackPath, mPaint);

        mPaint.setColor(mTrackColor);
        float offsetRight = (mPaddingPosition == mLeftProgress ? mOffsetX : 0);
        if (mMode != MODE_NORMAL && mRightProgress != -1) {
            float offsetLeft = offsetRight;
            offsetRight = (mPaddingPosition == mRightProgress ? mOffsetX : 0);
            mRectF.set(getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius + offsetLeft,
                    (getHeight() - mTrackWidth) / 2f + getPaddingTop(),
                    getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius +
                            offsetRight,
                    (getHeight() - mTrackWidth) / 2f + getPaddingTop() + mTrackWidth);
            canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
        } else {
            if (mTickMarkPatterns == null || mTickMarkPatterns.size() == 0 ||
                    mTickMarkPatterns.get(0) instanceof Dot) {
                mRectF.set(getPaddingLeft() + mRadius - mTrackWidth / 2f,
                        (getHeight() - mTrackWidth) / 2f + getPaddingTop(),
                        getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                                offsetRight,
                        (getHeight() - mTrackWidth) / 2f + getPaddingTop() + mTrackWidth);
                canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
            } else {
                canvas.drawRect(getPaddingLeft() + mRadius,
                        (getHeight() - mTrackWidth) / 2f + getPaddingTop(),
                        getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                                offsetRight,
                        (getHeight() - mTrackWidth) / 2f + getPaddingTop() + mTrackWidth, mPaint);
            }
        }

        float cy = getHeight() / 2f + (getPaddingTop() - getPaddingBottom());

        if (mTickMarkPatterns != null && mTickMarkPatterns.size() > 0) {
            float left, right;
            if (mRightProgress != -1 && mMode != MODE_NORMAL) {
                left = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                        (mPaddingPosition == mLeftProgress ? mOffsetX : 0);
                right = getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius +
                        (mPaddingPosition == mRightProgress ? mOffsetX : 0);
            } else {
                left = 0;
                right = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                        (mPaddingPosition == mLeftProgress ? mOffsetX : 0);
            }

            for (int i = 0; i < mCount; i++) {
                Object pattern = mTickMarkPatterns.get(i % mTickMarkPatterns.size());
                float cx = getPaddingLeft() + width / (mCount - 1) * i + mRadius;

                if (left <= cx && cx <= right) {
                    mPaint.setColor(mTickMarkColor);
                } else {
                    mPaint.setColor(mTickMarkInactiveColor);
                }

                if (pattern instanceof Dot) {
                    canvas.drawCircle(cx, cy, mTrackWidth / 2f, mPaint);
                } else {
                    float length = ((Dash) pattern).length;
                    canvas.drawRect(cx - length / 2f,
                            (getHeight() - mTrackWidth) / 2f + getPaddingTop(), cx + length / 2f,
                            (getHeight() - mTrackWidth) / 2f + getPaddingTop() + mTrackWidth,
                            mPaint);
                }
            }
        }

        if (mPressedPosition != -1) {
            mPaint.setColor(mThumbPressedColor);
            if (mPressedPosition == mLeftProgress) {
                float cx = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                        (mPaddingPosition == mLeftProgress ? mOffsetX : 0);
                canvas.drawCircle(cx, cy, mRadius * 3.5f, mPaint);
            } else if (mPressedPosition == mRightProgress && mMode != MODE_NORMAL) {
                float cx = getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius +
                        (mPaddingPosition == mRightProgress ? mOffsetX : 0);
                canvas.drawCircle(cx, cy, mRadius * 3.5f, mPaint);
            }
        }

        mPaint.setColor(mThumbColor);
        float cx = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                (mPaddingPosition == mLeftProgress ? mOffsetX : 0);

        float cy1 = cy - mRadius - Utils.convertDpToPixel(16, getContext()) - mRadius * 3;
        if (mPaddingPosition == mLeftProgress && mPaddingPosition != -1) {
            canvas.drawPath(mValueLabelPath, mPaint);
            cy1 = cy + (cy1 - cy) * mValueLabelAnimValue;
            canvas.drawCircle(cx, cy1, mRadius * 3 * mValueLabelAnimValue, mPaint);
            if (cy1 + mRadius * 3 * mValueLabelAnimValue < cy - mRadius) {
                drawValueLabel(canvas, cx, cy1, width);
            }
        }

        mPaint.setColor(mThumbColor);
        canvas.drawCircle(cx, cy, mRadius, mPaint);

        if (mRightProgress != -1 && mMode != MODE_NORMAL) {
            mPaint.setColor(mThumbColor);
            cx = getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius +
                    (mPaddingPosition == mRightProgress ? mOffsetX : 0);

            if (mPaddingPosition == mRightProgress) {
                canvas.drawPath(mValueLabelPath, mPaint);
                cy1 = cy + (cy1 - cy) * mValueLabelAnimValue;
                canvas.drawCircle(cx, cy1, mRadius * 3 * mValueLabelAnimValue, mPaint);
                drawValueLabel(canvas, cx, cy1, width);
            }

            mPaint.setColor(mThumbColor);
            canvas.drawCircle(cx, cy, mRadius, mPaint);
        }
    }

    private void drawValueLabel(Canvas canvas, float cx, float cy, float width) {
        String label = mValueLabelFormatter.getLabel(getClosestPosition(cx, width));
        if (!TextUtils.isEmpty(label)) {
            mPaint.setTextSize(mValueLabelTextSize * mValueLabelAnimValue);
            mPaint.setColor(mValueLabelTextColor);
            mPaint.getTextBounds(label, 0, label.length(), mBounds);
            canvas.drawText(label, cx - mBounds.width() / 2f - mBounds.left,
                    cy + mBounds.height() / 2f - mBounds.bottom, mPaint);
        }
    }

    private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {

        @Override
        public boolean onMove(MoveGestureDetector detector) {
            if ((mPaddingPosition == mLeftProgress ||
                    mPaddingPosition == mRightProgress && mMode != MODE_NORMAL) &&
                    mPaddingPosition != -1) {
                PointF d = detector.getFocusDelta();
                mOffsetX += d.x;
                mOffsetX = Math.min(Math.max(mOffsetX, mMinOffsetX), mMaxOffsetX);
                generateValueLabelPath();
                if (Math.abs(mOffsetX) >= mRadius * 2 && mValueLabelAnimator == null) {
                    animValueLabel();
                }
            }
            return true;
        }
    }

    private void generateValueLabelPath() {
        float cx;
        float r2 = mRadius, cy2 = getHeight() / 2f + (getPaddingTop() - getPaddingBottom());
        float r1 = mRadius * 3, cy1 = cy2 - mRadius - Utils.convertDpToPixel(16, getContext()) - r1;

        float width = getWidth() - getPaddingLeft() - getPaddingRight() - mRadius * 2;

        if (mPaddingPosition == mLeftProgress) {
            cx = getPaddingLeft() + width / (mCount - 1) * mLeftProgress + mRadius +
                    (mPaddingPosition == mLeftProgress ? mOffsetX : 0);
        } else if (mRightProgress != -1 && mMode != MODE_NORMAL) {
            cx = getPaddingLeft() + width / (mCount - 1) * mRightProgress + mRadius +
                    (mPaddingPosition == mRightProgress ? mOffsetX : 0);
        } else {
            mValueLabelPath.reset();
            return;
        }

        cy1 = cy2 + (cy1 - cy2) * mValueLabelAnimValue;
        r1 *= mValueLabelAnimValue;

        if (cy1 + r1 >= cy2 - r2) {
            mValueLabelPath.reset();
            return;
        }

        mValueLabelPath.reset();
        mRectF.set(cx - r1, cy1 - r1, cx + r1, cy1 + r1);
        mValueLabelPath.arcTo(mRectF, 135, 270, true);
        mValueLabelPath.quadTo(cx - Utils.convertDpToPixel(5, getContext()) * mValueLabelAnimValue,
                cy1 + r1 + Utils.convertDpToPixel(8, getContext()) * mValueLabelAnimValue,
                cx + r2 * (float) Math.cos(Math.toRadians(-45)),
                cy2 + r2 * (float) Math.sin(Math.toRadians(-45)));
        mRectF.set(cx - r2, cy2 - r2, cx + r2, cy2 + r2);
        mValueLabelPath.arcTo(mRectF, -45, 270, true);
        mValueLabelPath.quadTo(cx + Utils.convertDpToPixel(5, getContext()) * mValueLabelAnimValue,
                cy1 + r1 + Utils.convertDpToPixel(8, getContext()) * mValueLabelAnimValue,
                cx + r1 * (float) Math.cos(Math.toRadians(135)),
                cy1 + r1 * (float) Math.sin(Math.toRadians(135)));
        mValueLabelPath.moveTo(cx + r1 * (float) Math.cos(Math.toRadians(135)),
                cy1 + r1 * (float) Math.sin(Math.toRadians(135)));
        mValueLabelPath.close();
    }

    private int getClosestPosition(float x, float width) {
        float dis = Float.MAX_VALUE;
        int position = -1;
        for (int i = 0; i < mCount; i++) {
            float _dis = (getPaddingLeft() + width / (mCount - 1) * i + mRadius) - x;
            if (Math.abs(_dis) < Math.abs(dis)) {
                dis = _dis;
                position = i;
            }
        }
        return position;
    }

    public static abstract class ValueLabelFormatter {

        @Nullable
        public abstract String getLabel(int input);
    }

    public static class OnValueChangedListener {

        // Only called when mode is {@Code MODE_NORMAL}
        public void onValueChanged(int progress) {

        }

        // Only called when mode is {@Code MODE_RANGE}
        public void onValueChanged(int leftProgress, int rightProgress) {

        }
    }
}
