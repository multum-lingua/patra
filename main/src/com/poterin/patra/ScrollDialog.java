package com.poterin.patra;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.SeekBar;
import com.poterin.andorra.NumberScroller;

public class ScrollDialog  {

    public static void open(final DocumentViewerActivity documentViewer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(documentViewer);
        builder.setIcon(R.drawable.menu_scroll);
        builder.setTitle(R.string.menu_scroll);
        builder.setCancelable(false);

        View dialogView = documentViewer.getLayoutInflater().inflate(R.layout.scroll_dialog, null);
        builder.setView(dialogView);

        final int currentPage = documentViewer.viewPager.getCurrentItem();
        final int pageCount = documentViewer.viewPager.getAdapter().getCount();

        final NumberScroller numberSelectorPhrase = (NumberScroller) dialogView.findViewById(R.id.numberSelectorPhrase);
        numberSelectorPhrase.setMinValue(1);
        numberSelectorPhrase.setMaxValue(documentViewer.phrasesCount());
        numberSelectorPhrase.setValue(currentPage * Settings.phrasesOnPage() + 1);

        final NumberScroller numberSelectorPage = (NumberScroller) dialogView.findViewById(R.id.numberSelectorPage);
        numberSelectorPage.setMinValue(1);
        numberSelectorPage.setMaxValue(pageCount);
        numberSelectorPage.setValue(currentPage+ 1);

        final SeekBar seekBar = (SeekBar) dialogView.findViewById(R.id.seekBar);
        seekBar.setProgress(Math.round((float) 100 * currentPage / pageCount));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    int selectedPage = Math.round((float) i * pageCount / 100);
                    numberSelectorPhrase.setValue(selectedPage * Settings.phrasesOnPage() + 1);
                    numberSelectorPage.setValue(selectedPage + 1);
                    documentViewer.viewPager.setCurrentItem(selectedPage);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        numberSelectorPhrase.onValueChangedListener = new NumberScroller.OnValueChangedListener() {
            @Override
            public void onValueChanged(NumberScroller numberScroller, boolean byUser) {
                if (!byUser) return;
                int selectedPage = ((int) numberScroller.getValue() - 1) / Settings.phrasesOnPage();
                numberSelectorPage.setValue(selectedPage + 1);
                seekBar.setProgress(Math.round((float) 100 * selectedPage / pageCount));
                documentViewer.viewPager.setCurrentItem(selectedPage);
            }
        };

        numberSelectorPage.onValueChangedListener = new NumberScroller.OnValueChangedListener() {
            @Override
            public void onValueChanged(NumberScroller numberScroller, boolean byUser) {
                if (!byUser) return;
                int selectedPage = (int) numberScroller.getValue() - 1;
                numberSelectorPhrase.setValue(selectedPage * Settings.phrasesOnPage() + 1);
                seekBar.setProgress(Math.round((float) 100 * selectedPage / pageCount));
                documentViewer.viewPager.setCurrentItem(selectedPage);
            }
        };

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                documentViewer.viewPager.setCurrentItem(currentPage);
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.show();
    }  // open

}
