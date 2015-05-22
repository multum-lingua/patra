package com.poterin.patra;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;
import com.poterin.andorra.Dialog;

import java.lang.reflect.Field;

public class MainActivity extends Activity {

    private final static int REQUEST_FOR_OPEN_FILE = Settings.REQUEST_ID + 1;
    private final static int REQUEST_FOR_VIEWER = REQUEST_FOR_OPEN_FILE + 1;
    private final static int REQUEST_FOR_BOOK_LOADER = REQUEST_FOR_VIEWER + 1;

    private static MainActivity one;

    private MainMenuList mainMenuList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        one = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Utils.defaultContext = this;
        Languages.init();

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Utils.logException(e);
        }

        try {
            Settings.loadSettings(this);
            new UserDictionary();
            mainMenuList = new MainMenuList(this);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        setActualTheme();
    } // onCreate

    public static void setActualTheme() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;

        LinearLayout layoutRoot = (LinearLayout) one.findViewById(R.id.layoutRoot);
        TextView textViewTitle = (TextView) one.findViewById(R.id.textViewTitle);
        if (Settings.isNightMode()) {
            one.setTheme(android.R.style.Theme_Holo_NoActionBar);
            layoutRoot.setBackgroundResource(0);
            textViewTitle.setTextColor(Color.parseColor("#CCCCCC"));
        }
        else {
            one.setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
            layoutRoot.setBackgroundResource(R.drawable.main_background);
            textViewTitle.setTextColor(Color.parseColor("#FFFFFF"));
        }
        one.mainMenuList.setActualTheme();
    } // setActualTheme

    public void openFileDialog() {
        Intent intent = new Intent(this, FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, "/sdcard");
        intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "mlb" });
        intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
        if (Settings.isNightMode())
            intent.putExtra(FileDialog.THEME, android.R.style.Theme_Holo_Dialog_NoActionBar);
        else
            intent.putExtra(FileDialog.THEME, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        startActivityForResult(intent, REQUEST_FOR_OPEN_FILE);
    }

    public void openViewer() {
        startActivityForResult(new Intent(this, DocumentViewerActivity.class), REQUEST_FOR_VIEWER);
    }

    public void openBookLoader() {
        startActivityForResult(new Intent(this, BookLoaderActivity.class), REQUEST_FOR_BOOK_LOADER);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_FOR_OPEN_FILE:
                if (resultCode == Activity.RESULT_OK)
                    mainMenuList.bookHistory.openDocument(data.getStringExtra(FileDialog.RESULT_PATH));
            break;

            case REQUEST_FOR_VIEWER: {
                switch (resultCode) {
                    case DocumentViewerActivity.RESULT_EXIT: finish(); break;

                    case DocumentViewerActivity.RESULT_REOPEN: openViewer(); break;

                    case DocumentViewerActivity.RESULT_EXCEPTION: {
                        Dialog.showMessage(this, getString(R.string.app_name), data.getStringExtra("message"));
                    }
                    break;

                    default: mainMenuList.refresh();
                }
            }
            break;

            case REQUEST_FOR_BOOK_LOADER:
                if (resultCode == Activity.RESULT_OK)
                    mainMenuList.bookHistory.openDocument(data.getStringExtra("file"));
            break;
        } // switch
    } // onActivityResult

    public static void saveDocuments() {
        one.mainMenuList.bookHistory.saveDocuments();
    }

    @Override
    protected void onPause() {
        saveDocuments();
        super.onPause();
    }
}
