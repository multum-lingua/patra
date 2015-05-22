package com.poterin.andorra;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class NumberScroller extends FrameLayout {

    public interface OnValueChangedListener {
        public void onValueChanged(NumberScroller numberScroller, boolean byUser);
    }

    public OnValueChangedListener onValueChangedListener = null;

    private float value;
    private float minValue;
    private float maxValue;
    public float step = 1f;
    public String format = "%.0f";

    private ImageButton buttonDecrement;
    private ImageButton buttonIncrement;
    private TextView valueText;
    private SeekBar seekBar;

    private Handler repeatUpdateHandler = new Handler();

    private boolean autoIncrement = false;
    private boolean autoDecrement = false;

    private class RepetitivelyUpdater implements Runnable {
        private final long REPEAT_DELAY = 50;

        public void run() {
            if (autoIncrement) {
                increment();
                repeatUpdateHandler.postDelayed(new RepetitivelyUpdater(), REPEAT_DELAY);
            } else if (autoDecrement) {
                decrement();
                repeatUpdateHandler.postDelayed(new RepetitivelyUpdater(), REPEAT_DELAY);
            }
        }
    }

    public NumberScroller(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.number_scroller, null, false);
        addView(rowView);

        valueText = (TextView) rowView.findViewById(R.id.textView);
        buttonIncrement = (ImageButton) rowView.findViewById(R.id.buttonRight);
        initIncrementButton(context);
        buttonDecrement = (ImageButton) rowView.findViewById(R.id.buttonLeft);
        initDecrementButton(context);

        TypedArray ta = context.obtainStyledAttributes(attributeSet, R.styleable.NumberScroller);

        valueText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            ta.getDimension(R.styleable.NumberScroller_android_textSize, valueText.getTextSize()));
        valueText.setTextColor(
            ta.getColor(R.styleable.NumberScroller_android_textColor, valueText.getCurrentTextColor()));
        valueText.setMinWidth(ta.getDimensionPixelSize(R.styleable.NumberScroller_minTextWidth, 0));

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int buttonSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, metrics));
        buttonSize = Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            ta.getDimensionPixelSize(R.styleable.NumberScroller_buttonSize, buttonSize),
            metrics));

        seekBar = (SeekBar) rowView.findViewById(R.id.seekBarNumberScroller);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) setValue(minValue + i * step, true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(buttonSize, buttonSize);

        if (!ta.getBoolean(R.styleable.NumberScroller_showSeekBar, true)) {
            LinearLayout layout = (LinearLayout) rowView.findViewById(R.id.linearLayout);
            layout.removeView(buttonDecrement);
            layout.addView(buttonDecrement, 0);
            seekBar.setVisibility(View.GONE);
        }
        else
            lp.leftMargin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, metrics));

        buttonDecrement.setLayoutParams(lp);
        lp.leftMargin = 0;
        buttonIncrement.setLayoutParams(lp);
        if (ta.hasValue(R.styleable.NumberScroller_rightDrawable))
            buttonIncrement.setImageDrawable(ta.getDrawable(R.styleable.NumberScroller_rightDrawable));
        if (ta.hasValue(R.styleable.NumberScroller_leftDrawable))
            buttonDecrement.setImageDrawable(ta.getDrawable(R.styleable.NumberScroller_leftDrawable));

        ta.recycle();

        setMinValue(0);
        setMaxValue(999);
        setValue(99);
    } // NumberScroller

    private void initIncrementButton(Context context) {
        buttonIncrement.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                increment();
            }
        });

        buttonIncrement.setOnLongClickListener(
            new OnLongClickListener() {
                public boolean onLongClick(View arg0) {
                    autoIncrement = true;
                    repeatUpdateHandler.post(new RepetitivelyUpdater());
                    return false;
                }
            }
        );

        buttonIncrement.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && autoIncrement) {
                    autoIncrement = false;
                }
                return false;
            }
        });
    }

    private void initDecrementButton(Context context) {
        buttonDecrement.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                decrement();
            }
        });

        buttonDecrement.setOnLongClickListener(
            new OnLongClickListener() {
                public boolean onLongClick(View arg0) {
                    autoDecrement = true;
                    repeatUpdateHandler.post(new RepetitivelyUpdater());
                    return false;
                }
            }
        );

        buttonDecrement.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && autoDecrement) {
                    autoDecrement = false;
                }
                return false;
            }
        });
    }

    private void increment() {
        if (value < maxValue) {
            setValue(value + step, true);
        }
    }

    private void decrement() {
        if (value > minValue) {
            setValue(value - step, true);
        }
    }

    private void setValue(float value, boolean byUser) {
        if (value > maxValue) value = maxValue;
        if (value < minValue) value = minValue;

        this.value = value;
        valueText.setText(String.format(format, value));
        seekBar.setProgress(Math.round((value - minValue) / step));
        if (onValueChangedListener != null) onValueChangedListener.onValueChanged(this, byUser);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        setValue(value, false);
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        seekBar.setMax(Math.round((maxValue - minValue) / step));
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        seekBar.setMax(Math.round((maxValue - minValue) / step));
    }
}

/*
http://developer.android.com/training/custom-views/index.html
http://stackoverflow.com/questions/2695646/declaring-a-custom-android-ui-element-using-xml

    <com.poterin.andorra.NumberScroller
          xmlns:number_scroller="http://schemas.android.com/apk/res/com.poterin.andorra"
          xmlns:number_scroller="http://schemas.android.com/apk/res-auto"
          android:layout_width="fill_parent"
          android:layout_height="wrap_content"
          android:id="@+id/view"
          android:textColor="#FFFFFF"
          android:textSize="20sp"
          number_scroller:minTextWidth="50dp"
          number_scroller:buttonSize="30dp"
          number_scroller:leftDrawable="@drawable/to_left"
          number_scroller:rightDrawable="@drawable/to_right"
          number_scroller:showSeekBar="false"
          />
*/