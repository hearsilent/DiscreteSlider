package hearsilent.discreteslider.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import hearsilent.discreteslider.DiscreteSlider;

public class CustomDiscreteSlider extends DiscreteSlider {

	@Override
	public void onDrawThumb(Canvas canvas, float cx, float cy, boolean hasTouched) {
		// Draw thumb you want
	}

	// Override this value to your thumb size
	@Override
	public int getSize() {
		return super.getSize();
	}

	public CustomDiscreteSlider(Context context) {
		super(context);
	}

	public CustomDiscreteSlider(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomDiscreteSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
}
