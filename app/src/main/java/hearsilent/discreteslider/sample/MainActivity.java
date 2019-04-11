package hearsilent.discreteslider.sample;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import hearsilent.discreteslider.Dash;
import hearsilent.discreteslider.DiscreteSlider;
import hearsilent.discreteslider.Dot;
import hearsilent.discreteslider.libs.Utils;

public class MainActivity extends AppCompatActivity {

    private DiscreteSlider mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        setUpView();
    }

    private void findView() {
        mSlider = findViewById(R.id.discreteSlider);
    }

    private void setUpView() {
        mSlider.setTrackWidth(Utils.convertDpToPixel(4, this));
        mSlider.setTrackColor(0xFFD81B60);
        mSlider.setInactiveTrackColor(0x3DD81B60);

        mSlider.setThumbRadius(Utils.convertDpToPixel(6, this));
        mSlider.setThumbColor(0xFFD81B60);
        mSlider.setThumbPressedColor(0x1FD81B60);

        mSlider.setTickMarkColor(0x3DFFFFFF);
        mSlider.setTickMarkInactiveColor(0x1FD81B60);
        mSlider.setTickMarkPatterns(Arrays.asList(new Dot(), new Dash(Utils.convertDpToPixel(1, this))));

        mSlider.setValueLabelTextColor(Color.WHITE);
        mSlider.setValueLabelTextSize(Utils.convertSpToPixel(16, this));
        mSlider.setValueLabelFormatter(new DiscreteSlider.ValueLabelFormatter() {

            @Override
            public String getLabel(int input) {
                return Integer.toString(input);
            }
        });

        mSlider.setCount(21);
        mSlider.setMode(DiscreteSlider.MODE_NORMAL);

        mSlider.setLeftProgress(5);

        mSlider.setOnValueChangedListener(new DiscreteSlider.OnValueChangedListener() {
            @Override
            public void onValueChanged(int progress) {
                super.onValueChanged(progress);
                Log.i("DiscreteSlider", "Progress: " + progress);
            }

            @Override
            public void onValueChanged(int leftProgress, int rightProgress) {
                super.onValueChanged(leftProgress, rightProgress);
                Log.i("DiscreteSlider", "LeftProgress: " + leftProgress + ", RightProgress: " + rightProgress);
            }
        });

        mSlider.setClickable(true);
    }
}
