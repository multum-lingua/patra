package com.poterin.andorra;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class NumberScrollerPreferences extends DialogPreference {

    private float min, max, step;
    private String format = "%.0f";
    private NumberScroller numberScroller;
    private float value;

    public NumberScrollerPreferences(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setDialogLayoutResource(R.layout.number_scroller_preferences);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);


        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.NumberScrollerPreferences);
        min = ta.getFloat(R.styleable.NumberScrollerPreferences_min, 1);
        max = ta.getFloat(R.styleable.NumberScrollerPreferences_max, 100);
        step = ta.getFloat(R.styleable.NumberScrollerPreferences_step, 1);
        if (ta.hasValue(R.styleable.NumberScrollerPreferences_format))
            format = ta.getString(R.styleable.NumberScrollerPreferences_format);
        ta.recycle();
    } // NumberScrollerPreferences

    @Override
    protected View onCreateDialogView() {
        View result = super.onCreateDialogView();

        TextView textView = (TextView) result.findViewById(R.id.textViewTitle);
        textView.setText(getDialogMessage());

        numberScroller = (NumberScroller) result.findViewById(R.id.numberScroller);
        numberScroller.step = step;
        numberScroller.format = format;
        numberScroller.setMinValue(min);
        numberScroller.setMaxValue(max);
        numberScroller.setValue(value);

        return result;
    } // onCreateDialogView

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue)
            value = getPersistedFloat(min);
        else
            value = (Float) defaultValue;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            value = numberScroller.getValue();
            persistFloat(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, min);
    }
}
