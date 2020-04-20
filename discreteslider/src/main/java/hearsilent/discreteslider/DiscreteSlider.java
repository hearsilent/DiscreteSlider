package hearsilent.discreteslider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

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

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	public static final int TOP = 0;
	public static final int RIGHT = 90;
	public static final int BOTTOM = 180;
	public static final int LEFT = 270;

	public static final int MODE_NORMAL = 0;
	public static final int MODE_RANGE = 1;

	private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private RectF mRectF = new RectF();

	private float mOffset = 0f;
	private float mMinOffset, mMaxOffset;

	private float mRadius, mTrackWidth;

	private int mTrackColor;
	private int mInactiveTrackColor;
	private int mThumbColor;
	private int mThumbPressedColor;

	private List<Object> mTickMarkPatterns;
	private int mTickMarkColor;
	private int mTickMarkInactiveColor;
	private int mTickMarkStep;

	private int mValueLabelTextColor;

	private int mCount;
	private int mProgressOffset = 0;
	private int mMinProgress = 0, mTmpMinProgress = 0, mMaxProgress = -1, mTmpMaxProgress = -1;
	private int mPendingPosition = -1, mPressedPosition = -1;

	@Mode private int mMode = MODE_NORMAL;

	private Path mInactiveTrackPath = new Path();

	private MoveGestureDetector mMoveDetector;
	private float mValueLabelTextSize;
	private ValueLabelFormatter mValueLabelFormatter;
	private Rect mBounds = new Rect();
	private Path mValueLabelPath = new Path();
	private ValueAnimator mValueLabelAnimator;
	private Matrix mValueLabelMatrix = new Matrix();
	private float mValueLabelAnimValue = 0f;
	@ValueLabelGravity private int mValueLabelGravity;
	private int mValueLabelMode = 1;
	private int mValueLabelDuration = 1500;
	private Handler mShowValueLabelHandler = new Handler();
	private boolean mValueLabelIsShowing = false;

	private boolean mSkipMove;

	private float mLength;
	@OrientationMode private int mOrientation;

	private OnValueChangedListener mListener;
	private boolean mValueChangedImmediately = false;

	@IntDef({MODE_NORMAL, MODE_RANGE}) @Retention(RetentionPolicy.SOURCE) private @interface Mode {

	}

	@IntDef({TOP, RIGHT, BOTTOM, LEFT}) @Retention(RetentionPolicy.SOURCE)
	private @interface ValueLabelGravity {

	}

	@IntDef({HORIZONTAL, VERTICAL}) @Retention(RetentionPolicy.SOURCE)
	private @interface OrientationMode {

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

			mTrackWidth = a.getDimension(R.styleable.DiscreteSlider_ds_trackWidth,
					Utils.convertDpToPixel(4, getContext()));
			mTrackWidth = Math.max(mTrackWidth, Float.MIN_VALUE);
			mTrackColor = a.getColor(R.styleable.DiscreteSlider_ds_trackColor, 0xff5123da);
			mInactiveTrackColor =
					a.getColor(R.styleable.DiscreteSlider_ds_inactiveTrackColor, 0x3d5123da);

			mRadius = a.getDimension(R.styleable.DiscreteSlider_ds_thumbRadius,
					Utils.convertDpToPixel(6, getContext()));
			mRadius = Math.max(mRadius, Float.MIN_VALUE);
			mThumbColor = a.getColor(R.styleable.DiscreteSlider_ds_thumbColor, 0xff5123da);
			mThumbPressedColor =
					a.getColor(R.styleable.DiscreteSlider_ds_thumbPressedColor, 0x1f5123da);

			mTickMarkColor = a.getColor(R.styleable.DiscreteSlider_ds_tickMarkColor, 0xff9972ed);
			mTickMarkInactiveColor =
					a.getColor(R.styleable.DiscreteSlider_ds_tickMarkInactiveColor, 0xff936ce2);
			mTickMarkStep = a.getInteger(R.styleable.DiscreteSlider_ds_tickMarkStep, 1);

			mValueLabelTextColor =
					a.getColor(R.styleable.DiscreteSlider_ds_valueLabelTextColor, Color.WHITE);
			mValueLabelTextSize = a.getDimension(R.styleable.DiscreteSlider_ds_valueLabelTextSize,
					Utils.convertSpToPixel(16, context));
			mValueLabelGravity = a.getInt(R.styleable.DiscreteSlider_ds_valueLabelGravity, TOP);
			mValueLabelMode = a.getInt(R.styleable.DiscreteSlider_ds_valueLabelMode, 1);
			mValueLabelDuration = a.getInt(R.styleable.DiscreteSlider_ds_valueLabelDuration, 1500);
			mValueLabelDuration = Math.max(mValueLabelDuration, 500);

			mCount = a.getInt(R.styleable.DiscreteSlider_ds_count, 11);
			mCount = Math.max(mCount, 2);
			mMode = a.getInt(R.styleable.DiscreteSlider_ds_mode, MODE_NORMAL);

			if (1 > mTickMarkStep || (mCount - 1) % mTickMarkStep != 0) {
				mTickMarkStep = 1;
			}

			mProgressOffset = a.getInt(R.styleable.DiscreteSlider_ds_progressOffset, 0);
			mTmpMinProgress = mMinProgress = a.getInt(R.styleable.DiscreteSlider_ds_progress,
					a.getInt(R.styleable.DiscreteSlider_ds_minProgress, 0));
			if (mMode == MODE_NORMAL) {
				mTmpMaxProgress = mMaxProgress = -1;
			} else {
				mTmpMaxProgress = mMaxProgress =
						a.getInt(R.styleable.DiscreteSlider_ds_maxProgress, mCount - 1);
			}

			if (a.hasValue(R.styleable.DiscreteSlider_ds_tickMarkPatterns)) {
				String patterns = a.getString(R.styleable.DiscreteSlider_ds_tickMarkPatterns);
				if (!TextUtils.isEmpty(patterns)) {
					float length = a.getDimension(R.styleable.DiscreteSlider_ds_tickMarkDashLength,
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

			mOrientation = a.getInt(R.styleable.DiscreteSlider_ds_orientation, HORIZONTAL);

			if (mOrientation == HORIZONTAL &&
					(mValueLabelGravity != TOP && mValueLabelGravity != BOTTOM)) {
				mValueLabelGravity = TOP;
			} else if (mOrientation == VERTICAL &&
					(mValueLabelGravity != RIGHT && mValueLabelGravity != LEFT)) {
				mValueLabelGravity = RIGHT;
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
			mTickMarkStep = 1;

			mValueLabelTextSize = Utils.convertSpToPixel(16, context);
			mValueLabelTextColor = Color.WHITE;
			mValueLabelGravity = TOP;

			mOrientation = HORIZONTAL;

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

	public float getTrackWidth() {
		return mTrackWidth;
	}

	public void setTrackColor(@ColorInt int trackColor) {
		mTrackColor = trackColor;
		invalidate();
	}

	@ColorInt
	public int getTrackColor() {
		return mInactiveTrackColor;
	}

	public void setInactiveTrackColor(@ColorInt int inactiveTrackColor) {
		mInactiveTrackColor = inactiveTrackColor;
		invalidate();
	}

	@ColorInt
	public int getInactiveTrackColor() {
		return mInactiveTrackColor;
	}

	public void setThumbRadius(@FloatRange(from = Float.MIN_VALUE) float radius) {
		if (radius <= 0) {
			throw new IllegalArgumentException("Thumb radius must be a positive number.");
		}
		mRadius = radius;
		generateInactiveTrackPath();
		invalidate();
	}

	public float getThumbRadius() {
		return mRadius;
	}

	public void setThumbColor(@ColorInt int thumbColor) {
		mThumbColor = thumbColor;
		invalidate();
	}

	@ColorInt
	public int getThumbColor() {
		return mThumbColor;
	}

	public void setThumbPressedColor(@ColorInt int thumbPressedColor) {
		mThumbPressedColor = thumbPressedColor;
		invalidate();
	}

	@ColorInt
	public int getThumbPressedColor() {
		return mThumbPressedColor;
	}

	public void setTickMarkColor(@ColorInt int tickMarkColor) {
		mTickMarkColor = tickMarkColor;
		invalidate();
	}

	@ColorInt
	public int getTickMarkColor() {
		return mTickMarkColor;
	}

	public void setTickMarkInactiveColor(@ColorInt int tickMarkInactiveColor) {
		mTickMarkInactiveColor = tickMarkInactiveColor;
		invalidate();
	}

	@ColorInt
	public int getTickMarkInactiveColor() {
		return mTickMarkInactiveColor;
	}

	public int getTickMarkStep() {
		return mTickMarkStep;
	}

	public void setTickMarkStep(int tickMarkStep) {
		if (1 > tickMarkStep) {
			throw new IllegalArgumentException("TickMark step must >= 1.");
		}
		if ((mCount - 1) % tickMarkStep != 0) {
			throw new IllegalArgumentException(
					"TickMark step must be a factor of " + (mCount - 1) + ".");
		}
		mTickMarkStep = tickMarkStep;
	}

	public void setValueLabelTextColor(@ColorInt int valueLabelTextColor) {
		mValueLabelTextColor = valueLabelTextColor;
		invalidate();
	}

	@ColorInt
	public int getValueLabelTextColor() {
		return mValueLabelTextColor;
	}

	public void setValueLabelTextSize(
			@FloatRange(from = Float.MIN_VALUE) float valueLabelTextSize) {
		if (valueLabelTextSize <= 0) {
			throw new IllegalArgumentException("Value label text size must be a positive number.");
		}
		mValueLabelTextSize = valueLabelTextSize;
		invalidate();
	}

	public float getValueLabelTextSize() {
		return mValueLabelTextSize;
	}

	public void setValueLabelGravity(@ValueLabelGravity int valueLabelGravity) {
		if (mOrientation == HORIZONTAL && valueLabelGravity != TOP && valueLabelGravity != BOTTOM) {
			throw new IllegalArgumentException(
					"Horizontal orientation value label gravity must be top or bottom.");
		} else if (mOrientation == VERTICAL && valueLabelGravity != RIGHT &&
				valueLabelGravity != LEFT) {
			throw new IllegalArgumentException(
					"Vertical orientation value label gravity must be right or left.");
		}
		mValueLabelGravity = valueLabelGravity;
		invalidate();
	}

	@ValueLabelGravity
	public int getValueLabelGravity() {
		return mValueLabelGravity;
	}

	public void setMode(@Mode int mode) {
		if (mode != MODE_RANGE && mode != MODE_NORMAL) {
			throw new IllegalArgumentException("Mode must be normal or range.");
		}
		mMode = mode;
		checkProgressBound();
		invalidate();
	}

	@Mode
	public int getMode() {
		return mMode;
	}

	public void setCount(@IntRange(from = 2) int count) {
		if (count < 2) {
			throw new IllegalArgumentException("Count must larger than 2.");
		}
		mCount = count;
		checkProgressBound();
		invalidate();
	}

	public int getCount() {
		return mCount;
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

	@Nullable
	public List<Object> getTickMarkPatterns() {
		return mTickMarkPatterns;
	}

	public void setValueLabelFormatter(@NonNull ValueLabelFormatter formatter) {
		mValueLabelFormatter = formatter;
	}

	public ValueLabelFormatter getValueLabelFormatter() {
		return mValueLabelFormatter;
	}

	public void setValueLabelMode(int mode) {
		mValueLabelMode = mode;
		invalidate();
	}

	public int getValueLabelMode() {
		return mValueLabelMode;
	}

	public void setValueLabelDuration(@IntRange(from = 500) int duration) {
		mValueLabelDuration = duration;
		invalidate();
	}

	public int getValueLabelDuration() {
		return mValueLabelDuration;
	}

	public void setProgressOffset(int progressOffset) {
		mProgressOffset = progressOffset;
		invalidate();
	}

	public void setProgress(int progress) {
		setMinProgress(progress);
	}

	public void setMinProgress(int progress) {
		int _progress = mMinProgress;
		mMinProgress = progress;
		checkProgressBound();
		if (_progress != mMinProgress && mListener != null) {
			if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
				mListener.onValueChanged(mMinProgress + mProgressOffset,
						mMaxProgress + mProgressOffset, false);
			} else {
				mListener.onValueChanged(mMinProgress + mProgressOffset, false);
			}
		}

		if ((mValueLabelMode >> 1 & 0x1) == 1) {
			showMinValueLabel();
		}

		invalidate();
	}

	public int getProgress() {
		return getMinProgress();
	}

	public int getMinProgress() {
		return mMinProgress;
	}

	public void setMaxProgress(int progress) {
		if (mMode != MODE_RANGE) {
			throw new IllegalStateException("Set max progress must be range mode.");
		}
		int _progress = mMaxProgress;
		mMaxProgress = progress;
		checkProgressBound();
		if (_progress != mMaxProgress && mListener != null) {
			if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
				mListener.onValueChanged(mMinProgress + mProgressOffset,
						mMaxProgress + mProgressOffset, false);
			}
		}

		if ((mValueLabelMode >> 1 & 0x1) == 1) {
			showMaxValueLabel();
		}

		invalidate();
	}

	public int getMaxProgress() {
		return mMaxProgress;
	}

	private void checkProgressBound() {
		if (mMode != MODE_NORMAL) {
			if (mMaxProgress == -1) {
				mMaxProgress = mCount - 1;
			} else if (mMaxProgress > mCount - 1) {
				mMaxProgress = mCount - 1;
			}
			if (mMinProgress >= mMaxProgress) {
				mMinProgress = mMaxProgress - 1;
			}
		} else {
			mMaxProgress = -1;
			if (mMinProgress > mCount - 1) {
				mMinProgress = mCount - 1;
			}
		}
		mTmpMinProgress = mMinProgress;
		mTmpMaxProgress = mMaxProgress;
	}

	public void setOnValueChangedListener(@Nullable OnValueChangedListener listener) {
		mListener = listener;
	}

	public void setValueChangedImmediately(boolean immediately) {
		mValueChangedImmediately = immediately;
	}

	private void generateInactiveTrackPath() {
		float radius = mTrackWidth / 2f;
		float left, top, right, bottom;
		mInactiveTrackPath.reset();
		if (mOrientation == HORIZONTAL) {
			mLength = getWidth() - getPaddingLeft() - getPaddingRight() - mRadius * 2 + mTrackWidth;
			left = getPaddingLeft() + mRadius - radius;
			top = ((getHeight() - getPaddingTop() - getPaddingBottom()) - mTrackWidth) / 2f +
					getPaddingTop();
			right = left + mLength;
			bottom = top + mTrackWidth;
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
		} else {
			mLength =
					getHeight() - getPaddingTop() - getPaddingBottom() - mRadius * 2 + mTrackWidth;
			left = ((getWidth() - getPaddingLeft() - getPaddingRight()) - mTrackWidth) / 2f +
					getPaddingLeft();
			top = getPaddingTop() + mRadius - radius;
			right = left + mTrackWidth;
			bottom = top + mLength;
			if (mTickMarkPatterns != null && mTickMarkPatterns.size() > 0) {
				if (mTickMarkPatterns.get(0) instanceof Dot) {
					mRectF.set(left, top, right, top + mTrackWidth);
					mInactiveTrackPath.arcTo(mRectF, 180, 180, true);
				} else {
					mInactiveTrackPath.moveTo(left, top);
					mInactiveTrackPath.lineTo(right, top);
				}
				if (mTickMarkPatterns.get((mCount - 1) % mTickMarkPatterns.size()) instanceof Dot) {
					mInactiveTrackPath.lineTo(right, bottom - radius);
					mRectF.set(left, bottom - mTrackWidth, right, bottom);
					mInactiveTrackPath.arcTo(mRectF, 0, 180, true);
				} else {
					mInactiveTrackPath.lineTo(right, bottom);
					mInactiveTrackPath.lineTo(left, bottom);
				}
				if (mTickMarkPatterns.get(0) instanceof Dot) {
					mInactiveTrackPath.lineTo(left, top + radius);
				} else {
					mInactiveTrackPath.lineTo(left, top);
				}
				mInactiveTrackPath.close();
			} else {
				mRectF.set(left, top, right, bottom);
				mInactiveTrackPath.addRoundRect(mRectF, radius, radius, Path.Direction.CW);
			}
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

	private void requestDisallowInterceptTouchEvent(ViewParent parent, boolean isDragging) {
		if (parent != null) {
			parent.requestDisallowInterceptTouchEvent(isDragging);
			requestDisallowInterceptTouchEvent(parent.getParent(), isDragging);
		}
	}

	private boolean handleTouchEvent(MotionEvent event) {
		if (mCount < 2) {
			mMoveDetector.onTouchEvent(event);
			return true;
		}
		float length = mLength - mTrackWidth;
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int pendingPosition = mPendingPosition;

			mOffset = 0;
			mPendingPosition = -1;
			mSkipMove = false;

			float p = mOrientation == HORIZONTAL ? event.getX() : event.getY();
			if (mMaxProgress == -1 && mMode == MODE_NORMAL) {
				float c = getPosition(length, mMinProgress, false);
				if (c - mRadius * 3.5 <= p && p <= c + mRadius * 3.5) {
					mPendingPosition = mMinProgress;
				}
			} else {
				float c1 = getPosition(length, mMinProgress, false);
				float c2 = getPosition(length, mMaxProgress, false);
				if (c1 - mRadius * 3.5 <= p && p <= c1 + mRadius * 3.5) {
					mPendingPosition = mMinProgress;
				} else if (c2 - mRadius * 3.5 <= p && p <= c2 + mRadius * 3.5) {
					mPendingPosition = mMaxProgress;
				}
			}
			if (mPendingPosition == -1) {
				mPendingPosition = (int) getClosestPosition(p, length)[0];
			}

			if (pendingPosition != mPendingPosition) {
				if (mValueLabelAnimator != null) {
					mValueLabelAnimator.cancel();
					mValueLabelAnimator = null;
				}
				mValueLabelAnimValue = 0;
				mValueLabelIsShowing = false;
				mShowValueLabelHandler.removeCallbacksAndMessages(null);
			}

			if (isClickable()) {
				if (mPendingPosition != mMinProgress &&
						!(mPendingPosition == mMaxProgress && mMode != MODE_NORMAL)) {
					animValueLabel();
				}
				if (mMaxProgress != -1 && mMode == MODE_RANGE) {
					if (Math.abs(mMinProgress - mPendingPosition) >
							Math.abs(mMaxProgress - mPendingPosition)) {
						mMaxProgress = mPendingPosition;
					} else {
						mMinProgress = mPendingPosition;
					}
				} else {
					mMinProgress = mPendingPosition;
				}
			}

			p = getPosition(length, mPendingPosition, false);
			if (mPendingPosition == mMinProgress) {
				mMinOffset = getPosition(length, 0, false) - p;
				if (mMaxProgress != -1 && mMode == MODE_RANGE) {
					mMaxOffset = getPosition(length, mMaxProgress - 1, false) - p;
				} else {
					mMaxOffset = getPosition(length, mCount - 1, false) - p;
				}
				mPressedPosition = mPendingPosition;
			} else if (mPendingPosition == mMaxProgress && mMode != MODE_NORMAL) {
				mMinOffset = getPosition(length, mMinProgress + 1, false) - p;
				mMaxOffset = getPosition(length, mCount - 1, false) - p;
				mPressedPosition = mPendingPosition;
			} else if (!isClickable()) {
				mPendingPosition = -1;
			}

			if (mPendingPosition == mMinProgress ||
					mPendingPosition == mMaxProgress && mMaxProgress != -1 && mMode == MODE_RANGE) {
				requestDisallowInterceptTouchEvent(getParent(), true);
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (mPendingPosition == -1) {
				mOffset = 0;
				mMoveDetector.onTouchEvent(event);
				return true;
			}
			if (mPendingPosition != mMinProgress && mPendingPosition != mMaxProgress) {
				float p = mOrientation == HORIZONTAL ? event.getX() : event.getY();
				final int position = (int) getClosestPosition(p, length)[0];
				if (position == mPendingPosition && !mSkipMove) {
					if (mMaxProgress == -1 && mMode == MODE_NORMAL) {
						mPendingPosition = mMinProgress;
					} else {
						if (Math.abs(mMinProgress - position) <=
								Math.abs(mMaxProgress - position)) {
							mPendingPosition = mMinProgress;
						} else {
							mPendingPosition = mMaxProgress;
						}
					}

					if (mListener != null) {
						if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
							if (mPendingPosition == mMinProgress) {
								mListener.onValueChanged(position + mProgressOffset,
										mMaxProgress + mProgressOffset, true);
							} else {
								mListener.onValueChanged(mMinProgress + mProgressOffset,
										position + mProgressOffset, true);
							}
						} else {
							mListener.onValueChanged(position + mProgressOffset, true);
						}
					}

					setEnabled(false);

					mOffset = 0;
					float dis = length / (mCount - 1) * (position - mPendingPosition);
					ValueAnimator animator = ValueAnimator.ofFloat(mOffset, mOffset + dis);
					animator.setInterpolator(new DecelerateInterpolator(2.5f));
					animator.setDuration(250);
					animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							mOffset = (float) animation.getAnimatedValue();
							invalidate();
						}
					});
					animator.addListener(new AnimatorListenerAdapter() {

						@Override
						public void onAnimationEnd(Animator animation) {
							super.onAnimationEnd(animation);
							mOffset = 0;
							if (mPendingPosition == mMinProgress) {
								mMinProgress = position;
							} else if (mPendingPosition == mMaxProgress && mMode != MODE_NORMAL) {
								mMaxProgress = position;
							}
							mPendingPosition = -1;
							setEnabled(true);

							invalidate();
						}
					});
					animator.start();
				}
			} else {
				float p = getPosition(length, mPendingPosition, true);
				float[] closestPosition = getClosestPosition(p, length);
				float dis = closestPosition[1];
				int position = (int) closestPosition[0];

				if (mListener != null) {
					if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
						if (mPendingPosition == mMinProgress) {
							mListener.onValueChanged(position + mProgressOffset,
									mMaxProgress + mProgressOffset, true);
						} else {
							mListener.onValueChanged(mMinProgress + mProgressOffset,
									position + mProgressOffset, true);
						}
					} else {
						mListener.onValueChanged(position + mProgressOffset, true);
					}
				}

				setEnabled(false);

				final int _position = position;
				ValueAnimator animator = ValueAnimator.ofFloat(mOffset, mOffset + dis);
				animator.setInterpolator(new DecelerateInterpolator(2.5f));
				animator.setDuration(250);
				animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						mOffset = (float) animation.getAnimatedValue();
						invalidate();
					}
				});
				animator.addListener(new AnimatorListenerAdapter() {

					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						mOffset = 0;
						if (mPendingPosition == mMinProgress) {
							mMinProgress = _position;
						} else if (mPendingPosition == mMaxProgress && mMode != MODE_NORMAL) {
							mMaxProgress = _position;
						}
						if (mValueLabelAnimator == null) {
							mPendingPosition = -1;
							setEnabled(true);
						} else {
							mPendingPosition = _position;
						}
						invalidate();
					}
				});
				animator.start();

				hideValueLabel();
			}
			mPressedPosition = -1;
			requestDisallowInterceptTouchEvent(getParent(), false);
		} else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
			if (mPendingPosition == mMinProgress || mPendingPosition == mMaxProgress) {
				setEnabled(false);

				ValueAnimator animator = ValueAnimator.ofFloat(mOffset, 0);
				animator.setInterpolator(new DecelerateInterpolator(2.5f));
				animator.setDuration(250);
				animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						mOffset = (float) animation.getAnimatedValue();
						invalidate();
					}
				});
				animator.addListener(new AnimatorListenerAdapter() {

					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						mOffset = 0;
						mPendingPosition = -1;
						setEnabled(true);
						invalidate();
					}
				});
				animator.start();
			} else {
				mOffset = 0;
			}

			mPendingPosition = -1;
			mPressedPosition = -1;
			requestDisallowInterceptTouchEvent(getParent(), false);
		}
		mMoveDetector.onTouchEvent(event);
		invalidate();
		return true;
	}

	private void animValueLabel() {
		mValueLabelIsShowing = true;
		mShowValueLabelHandler.removeCallbacksAndMessages(null);

		float value = mValueLabelAnimValue;
		if (mValueLabelAnimator != null) {
			value = mValueLabelAnimator.getAnimatedFraction();
			mValueLabelAnimator.cancel();
		}

		if (value == 1) {
			mValueLabelAnimator = null;
			generateValueLabelPath();
			return;
		}

		mValueLabelAnimator = ValueAnimator.ofFloat(value, 1);
		mValueLabelAnimator.setDuration(Math.round((250 * (1 - value))));
		mValueLabelAnimator.setInterpolator(new AccelerateInterpolator());
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

	private void hideValueLabel() {
		mValueLabelIsShowing = false;
		mShowValueLabelHandler.removeCallbacksAndMessages(null);

		float value = mValueLabelAnimValue;
		if (mValueLabelAnimator != null) {
			value = mValueLabelAnimator.getAnimatedFraction();
			mValueLabelAnimator.cancel();
		}

		if (value > 0) {
			mValueLabelAnimator = ValueAnimator.ofFloat(value, 0);
			mValueLabelAnimator.setDuration(Math.round(250 * value));
			mValueLabelAnimator.setInterpolator(new DecelerateInterpolator());
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
					if (mOffset == 0) {
						mPendingPosition = -1;
						setEnabled(true);
					}

					invalidate();
				}
			});
			mValueLabelAnimator.start();
		} else {
			mValueLabelAnimator = null;
		}
	}

	private void showMinValueLabel() {
		mPendingPosition = mMinProgress;
		showValueLabel();
	}

	private void showMaxValueLabel() {
		mPendingPosition = mMaxProgress;
		showValueLabel();
	}

	private void showValueLabel() {
		animValueLabel();
		mShowValueLabelHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				hideValueLabel();
			}
		}, mValueLabelDuration - 250);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mOrientation == HORIZONTAL) {
			setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
					getSize() + getPaddingTop() + getPaddingBottom());
		} else {
			setMeasuredDimension(getSize() + getPaddingLeft() + getPaddingRight(),
					getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
		}
	}

	public int getSize() {
		return (int) Math.max(Math.ceil(mRadius * 2 * 3), mTrackWidth);
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
		boolean isValueLabelVisible =
				(mValueLabelMode & 0x1) == 1 || (mValueLabelMode >> 1 & 0x1) == 1;
		float length = mLength - mTrackWidth;
		mPaint.setColor(mInactiveTrackColor);
		canvas.drawPath(mInactiveTrackPath, mPaint);

		float min, max;
		mPaint.setColor(mTrackColor);
		if (mOrientation == HORIZONTAL) {
			float top = ((getHeight() - getPaddingTop() - getPaddingBottom()) - mTrackWidth) / 2f +
					getPaddingTop();
			float bottom = top + mTrackWidth;
			if (mMode != MODE_NORMAL && mMaxProgress != -1) {
				float left = min = getPosition(length, mMinProgress, true) - mTrackWidth / 2f;
				float right = max = getPosition(length, mMaxProgress, true) + mTrackWidth / 2f;
				mRectF.set(left, top, right, bottom);
				canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
			} else {
				float left = min = getPosition(length, 0, false) - mTrackWidth / 2f;
				float right = max = getPosition(length, mMinProgress, true) + mTrackWidth / 2f;
				mRectF.set(left, top, right, bottom);
				if (mTickMarkPatterns == null || mTickMarkPatterns.size() == 0 ||
						mTickMarkPatterns.get(0) instanceof Dot) {
					canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
				} else {
					canvas.drawRect(mRectF, mPaint);
				}
			}
		} else {
			float left = ((getWidth() - getPaddingLeft() - getPaddingRight()) - mTrackWidth) / 2f +
					getPaddingLeft();
			float right = left + mTrackWidth;
			if (mMode != MODE_NORMAL && mMaxProgress != -1) {
				float top = min = getPosition(length, mMinProgress, true) - mTrackWidth / 2f;
				float bottom = max = getPosition(length, mMaxProgress, true) + mTrackWidth / 2f;
				mRectF.set(left, top, right, bottom);
				canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
			} else {
				float top = min = getPosition(length, 0, false) - mTrackWidth / 2f;
				float bottom = max = getPosition(length, mMinProgress, true) + mTrackWidth / 2f;
				mRectF.set(left, top, right, bottom);
				if (mTickMarkPatterns == null || mTickMarkPatterns.size() == 0 ||
						mTickMarkPatterns.get(0) instanceof Dot) {
					canvas.drawRoundRect(mRectF, mTrackWidth / 2f, mTrackWidth / 2f, mPaint);
				} else {
					canvas.drawRect(mRectF, mPaint);
				}
			}
		}

		float cx = (getWidth() - getPaddingLeft() - getPaddingRight()) / 2f + getPaddingLeft();
		float cy = (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f + getPaddingTop();

		if (mTickMarkPatterns != null && mTickMarkPatterns.size() > 0) {
			if (mOrientation == HORIZONTAL) {
				for (int i = 0; i < mCount; i++) {
					if (i % mTickMarkStep != 0) {
						continue;
					}

					Object pattern = mTickMarkPatterns.get(i % mTickMarkPatterns.size());
					cx = getPosition(length, i, false);

					if (min <= cx && cx <= max) {
						mPaint.setColor(mTickMarkColor);
					} else {
						mPaint.setColor(mTickMarkInactiveColor);
					}

					if (pattern instanceof Dot) {
						canvas.drawCircle(cx, cy, mTrackWidth / 2f, mPaint);
					} else {
						float dashLength = ((Dash) pattern).length;
						canvas.drawRect(cx - dashLength / 2f, cy - mTrackWidth / 2f,
								cx + dashLength / 2f, cy + mTrackWidth / 2f, mPaint);
					}
				}
			} else {
				for (int i = 0; i < mCount; i++) {
					if (i % mTickMarkStep != 0) {
						continue;
					}

					Object pattern = mTickMarkPatterns.get(i % mTickMarkPatterns.size());
					cy = getPosition(length, i, false);

					if (min <= cy && cy <= max) {
						mPaint.setColor(mTickMarkColor);
					} else {
						mPaint.setColor(mTickMarkInactiveColor);
					}

					if (pattern instanceof Dot) {
						canvas.drawCircle(cx, cy, mTrackWidth / 2f, mPaint);
					} else {
						float dashLength = ((Dash) pattern).length;
						canvas.drawRect(cx - mTrackWidth / 2f, cy - dashLength / 2f,
								cx + mTrackWidth / 2f, cy + dashLength / 2f, mPaint);
					}
				}
			}
		}

		if (mOrientation == HORIZONTAL) {
			cx = getPosition(length, mMinProgress, true);
		} else {
			cy = getPosition(length, mMinProgress, true);
		}

		float _cx = cx;
		float _cy = cy;
		float dp6 = Utils.convertDpToPixel(6, getContext());
		float ratio = mRadius / dp6;
		if (mOrientation == HORIZONTAL) {
			if (mValueLabelGravity == TOP) {
				_cy -= dp6 + Utils.convertDpToPixel(16, getContext()) + dp6 * 3;
				_cy = cy + (_cy - cy) * mValueLabelAnimValue * ratio;
			} else if (mValueLabelGravity == BOTTOM) {
				_cy += dp6 + Utils.convertDpToPixel(16, getContext()) + dp6 * 3;
				_cy = cy + (_cy - cy) * mValueLabelAnimValue * ratio;
			}
		} else {
			if (mValueLabelGravity == RIGHT) {
				_cx += dp6 + Utils.convertDpToPixel(16, getContext()) + dp6 * 3;
				_cx = cx + (_cx - cx) * mValueLabelAnimValue * ratio;
			} else if (mValueLabelGravity == LEFT) {
				_cx -= dp6 + Utils.convertDpToPixel(16, getContext()) + dp6 * 3;
				_cx = cx + (_cx - cx) * mValueLabelAnimValue * ratio;
			}
		}
		if (mPendingPosition == mMinProgress && mPendingPosition != -1 &&
				mValueLabelAnimValue > 0 && isValueLabelVisible) {
			mPaint.setColor(mThumbColor);
			canvas.drawPath(mValueLabelPath, mPaint);
			canvas.drawCircle(_cx, _cy, mRadius * 3 * mValueLabelAnimValue, mPaint);
			drawValueLabel(canvas, cx, cy, _cx, _cy, length);
		}

		onDrawThumb(canvas, cx, cy, mPressedPosition != -1 && mPressedPosition == mMinProgress);

		int progress;
		if (mOrientation == HORIZONTAL) {
			progress = (int) getClosestPosition(cx, length)[0];
		} else {
			progress = (int) getClosestPosition(cy, length)[0];
		}
		if (mTmpMinProgress != progress) {
			mTmpMinProgress = progress;
			if (isHapticFeedbackEnabled()) {
				performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			}
			if (mListener != null && mValueChangedImmediately) {
				if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
					mListener.onValueChanged(progress + mProgressOffset,
							mMaxProgress + mProgressOffset, true);
				} else {
					mListener.onValueChanged(progress + mProgressOffset, true);
				}
			}
		}

		if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
			mPaint.setColor(mThumbColor);
			if (mOrientation == HORIZONTAL) {
				cx = getPosition(length, mMaxProgress, true);
				_cx = cx;
				progress = (int) getClosestPosition(cx, length)[0];
			} else {
				cy = getPosition(length, mMaxProgress, true);
				_cy = cy;
				progress = (int) getClosestPosition(cy, length)[0];
			}

			if (mPendingPosition == mMaxProgress && mValueLabelAnimValue > 0 &&
					isValueLabelVisible) {
				canvas.drawPath(mValueLabelPath, mPaint);
				canvas.drawCircle(_cx, _cy, mRadius * 3 * mValueLabelAnimValue, mPaint);
				drawValueLabel(canvas, cx, cy, _cx, _cy, length);
			}

			onDrawThumb(canvas, cx, cy, mPressedPosition != -1 && mPressedPosition == mMaxProgress);

			if (mTmpMaxProgress != progress) {
				mTmpMaxProgress = progress;
				if (isHapticFeedbackEnabled()) {
					performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				}
				if (mListener != null && mValueChangedImmediately) {
					if (mMaxProgress != -1 && mMode != MODE_NORMAL) {
						mListener.onValueChanged(mMinProgress + mProgressOffset,
								progress + mProgressOffset, true);
					}
				}
			}

		}
	}

	public void onDrawThumb(Canvas canvas, float cx, float cy, boolean hasTouched) {
		if (hasTouched) {
			mPaint.setColor(mThumbPressedColor);
			canvas.drawCircle(cx, cy, mRadius * 3.5f, mPaint);
		}
		mPaint.setColor(mThumbColor);
		canvas.drawCircle(cx, cy, mRadius, mPaint);
	}

	private void drawValueLabel(Canvas canvas, float cx, float cy, float _cx, float _cy,
	                            float length) {
		if (mValueLabelGravity == TOP && _cy + mRadius * 3 * mValueLabelAnimValue > cy - mRadius) {
			return;
		} else if (mValueLabelGravity == BOTTOM &&
				_cy - mRadius * 3 * mValueLabelAnimValue < cy + mRadius) {
			return;
		} else if (mValueLabelGravity == RIGHT &&
				_cx - mRadius * 3 * mValueLabelAnimValue < cx + mRadius) {
			return;
		} else if (mValueLabelGravity == LEFT &&
				_cx + mRadius * 3 * mValueLabelAnimValue > cx - mRadius) {
			return;
		}

		String label;
		if (mOrientation == HORIZONTAL) {
			label = mValueLabelFormatter
					.getLabel((int) getClosestPosition(cx, length)[0] + mProgressOffset);
		} else {
			label = mValueLabelFormatter
					.getLabel((int) getClosestPosition(cy, length)[0] + mProgressOffset);
		}
		if (!TextUtils.isEmpty(label)) {
			mPaint.setTextSize(mValueLabelTextSize * mValueLabelAnimValue);
			mPaint.setColor(mValueLabelTextColor);
			mPaint.getTextBounds(label, 0, label.length(), mBounds);
			canvas.drawText(label, _cx - mBounds.width() / 2f - mBounds.left,
					_cy + mBounds.height() / 2f - mBounds.bottom, mPaint);
		}
	}

	private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {

		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF d = detector.getFocusDelta();
			if (mOrientation == HORIZONTAL) {
				mOffset += d.x;
			} else {
				mOffset += d.y;
			}
			if ((mPendingPosition == mMinProgress ||
					mPendingPosition == mMaxProgress && mMode != MODE_NORMAL) &&
					mPendingPosition != -1) {
				mOffset = Math.min(Math.max(mOffset, mMinOffset), mMaxOffset);
				generateValueLabelPath();
				if (Math.abs(mOffset) >= mRadius * 2 && !mValueLabelIsShowing &&
						(mValueLabelMode & 0x1) == 1) {
					animValueLabel();
				} else if ((mValueLabelMode & 0x1) == 1) {
					mShowValueLabelHandler.removeCallbacksAndMessages(null);
				}
			} else if (Math.abs(mOffset) >= mRadius * 3.5) {
				mSkipMove = true;
			}
			return true;
		}
	}

	private void generateValueLabelPath() {
		float r2 = Utils.convertDpToPixel(6, getContext()), cx2, cy2;
		float r1 = r2 * 3, cx1, cy1;
		float ratio = mRadius / r2;

		float length = mLength - mTrackWidth;
		if (mPendingPosition == mMinProgress || mMaxProgress != -1 && mMode != MODE_NORMAL) {
			if (mOrientation == HORIZONTAL) {
				cy2 = (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f + getPaddingTop();
				cx2 = getPosition(length, mPendingPosition, true);
			} else {
				cy2 = getPosition(length, mPendingPosition, true);
				cx2 = (getWidth() - getPaddingLeft() - getPaddingRight()) / 2f + getPaddingLeft();
			}
		} else {
			mValueLabelPath.reset();
			return;
		}
		cx1 = cx2;
		cy1 = cy2;

		float dp1 = Utils.convertDpToPixel(1, getContext()), dp16 =
				Utils.convertDpToPixel(16, getContext());
		float ox1, oy1, ox2, oy2;
		if (mValueLabelGravity == TOP) {
			cy1 -= r2 + dp16 + r1;
			ox1 = -Math.max((r2 - dp1), 0);
			ox2 = -ox1;
			oy1 = oy2 = r1 + dp16 / 2;
		} else if (mValueLabelGravity == BOTTOM) {
			cy1 += r2 + dp16 + r1;
			ox1 = Math.max((r2 - dp1), 0);
			ox2 = -ox1;
			oy1 = oy2 = -r1 - dp16 / 2;
		} else if (mValueLabelGravity == RIGHT) {
			cx1 += r2 + dp16 + r1;
			ox1 = ox2 = -r1 - dp16 / 2;
			oy1 = -Math.max((r2 - dp1), 0);
			oy2 = -oy1;
		} else {
			cx1 -= r2 + dp16 + r1;
			ox1 = ox2 = r1 + dp16 / 2;
			oy1 = Math.max((r2 - dp1), 0);
			oy2 = -oy1;
		}

		if (mValueLabelGravity == TOP && cy1 + r1 >= cy2 - r2) {
			mValueLabelPath.reset();
			return;
		} else if (mValueLabelGravity == BOTTOM && cy1 - r1 <= cy2 + r2) {
			mValueLabelPath.reset();
			return;
		} else if (mValueLabelGravity == RIGHT && cx1 - r1 <= cx2 + r2) {
			mValueLabelPath.reset();
			return;
		} else if (mValueLabelGravity == LEFT && cx1 + r1 >= cx2 - r2) {
			mValueLabelPath.reset();
			return;
		}

		mValueLabelPath.reset();
		mRectF.set(cx1 - r1, cy1 - r1, cx1 + r1, cy1 + r1);
		mValueLabelPath.arcTo(mRectF, 135 + mValueLabelGravity, 270, true);
		mValueLabelPath.quadTo(cx1 + ox1, cy1 + oy1,
				cx2 + r2 * (float) Math.cos(Math.toRadians(-45 + mValueLabelGravity)),
				cy2 + r2 * (float) Math.sin(Math.toRadians(-45 + mValueLabelGravity)));
		mRectF.set(cx2 - r2, cy2 - r2, cx2 + r2, cy2 + r2);
		mValueLabelPath.arcTo(mRectF, -45 + mValueLabelGravity, 270, true);
		mValueLabelPath.quadTo(cx1 + ox2, cy1 + oy2,
				cx1 + r1 * (float) Math.cos(Math.toRadians(135 + mValueLabelGravity)),
				cy1 + r1 * (float) Math.sin(Math.toRadians(135 + mValueLabelGravity)));
		mValueLabelPath
				.moveTo(cx1 + r1 * (float) Math.cos(Math.toRadians(135 + mValueLabelGravity)),
						cy1 + r1 * (float) Math.sin(Math.toRadians(135 + mValueLabelGravity)));
		mValueLabelPath.close();

		if (mValueLabelAnimValue * ratio != 1) {
			mValueLabelPath.computeBounds(mRectF, true);
			if (mOrientation == HORIZONTAL) {
				mValueLabelMatrix
						.setScale(mValueLabelAnimValue * ratio, mValueLabelAnimValue * ratio,
								mRectF.centerX(), cy2);
			} else {
				mValueLabelMatrix
						.setScale(mValueLabelAnimValue * ratio, mValueLabelAnimValue * ratio, cx2,
								mRectF.centerY());
			}
			mValueLabelPath.transform(mValueLabelMatrix);
		}
	}

	private float[] getClosestPosition(float p, float length) {
		float dis = Float.MAX_VALUE;
		int position = -1;
		for (int i = 0; i < mCount; i++) {
			float _dis = getPosition(length, i, false) - p;
			if (Math.abs(_dis) < Math.abs(dis)) {
				dis = _dis;
				position = i;
			}
		}
		return new float[]{position, dis};
	}

	private float getPosition(float length, int progress, boolean withOffset) {
		if (mOrientation == HORIZONTAL) {
			return getPaddingLeft() + length / (mCount - 1) * progress + mRadius +
					(withOffset && mPendingPosition == progress ? mOffset : 0);
		} else {
			return getPaddingTop() + length / (mCount - 1) * progress + mRadius +
					(withOffset && mPendingPosition == progress ? mOffset : 0);
		}
	}

	public static abstract class ValueLabelFormatter {

		@Nullable
		public abstract String getLabel(int input);
	}

	public static class OnValueChangedListener {

		// Only called when mode is {@Code MODE_NORMAL}
		public void onValueChanged(int progress, boolean fromUser) {

		}

		// Only called when mode is {@Code MODE_RANGE}
		public void onValueChanged(int minProgress, int maxProgress, boolean fromUser) {

		}
	}
}
