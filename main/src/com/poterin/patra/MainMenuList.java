package com.poterin.patra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainMenuList {

    private final List<StandardMenuItem> menuItems;
    private final Context context;
    private MainMenuAdapter mainMenuAdapter;
    public MainActivity mainActivity;
    public BookHistory bookHistory;
    private int textColor = Color.WHITE;

	public MainMenuList(final MainActivity mainActivity) throws Exception {
        this.mainActivity = mainActivity;
        context = mainActivity;
        menuItems = new ArrayList<StandardMenuItem>();

        new StandardMenuItem(
            R.drawable.open_book,
            R.string.open_book,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    // todo save last folder
                    mainActivity.openFileDialog();
                }
            }
        );

        new StandardMenuItem(
            R.drawable.internet_library,
            R.string.internet_library,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    mainActivity.openBookLoader();
                }
            }
        );

        new StandardMenuItem(
            R.drawable.menu_my_dictionary,
            R.string.menu_my_dictionary,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    mainActivity.startActivity(new Intent(mainActivity, DictionaryActivity.class));
                }
            }
        );

        new StandardMenuItem(
            R.drawable.menu_settings,
            R.string.menu_settings,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    Settings.open(mainActivity);
                }
            }
        );

        new StandardMenuItem(
            R.drawable.about,
            R.string.about,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    showAbout();
                }
            }
        );

        new StandardMenuItem(
            R.drawable.menu_exit,
            R.string.menu_exit,
            new OnMenuItemClickListener () {
                public void onItemClick() {
                    mainActivity.finish();
                }
            }
        );

        bookHistory = new BookHistory(this);

        ListView listView = (ListView) mainActivity.findViewById(R.id.listView);
        listView.setItemsCanFocus(true);
        mainMenuAdapter = new MainMenuAdapter();
        listView.setAdapter(mainMenuAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < bookHistory.bookItems.size())
                    bookHistory.bookItems.get(i).onItemClick();
                else
                    menuItems.get(i - bookHistory.bookItems.size()).onItemClick();
            }
        });
    } // MainMenuList

    public void notifyDataSetChanged() {
        mainMenuAdapter.notifyDataSetChanged();
    }

    public void setActualTheme() {
        textColor = Color.parseColor(Settings.isNightMode() ? "#CCCCCC" : "#FFFFFF");
        mainMenuAdapter.notifyDataSetChanged();
    }

    private void showAbout() {
        Spanned message;
        try {
            message =
                Html.fromHtml(
                    String.format(
                        context.getString(R.string.about_text),
                        context.getString(R.string.app_name) + " " +
                            context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName)
                );
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.about);
        builder.setMessage(message);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alert.show();
        ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    } // showAbout

    public void refresh() {
        mainMenuAdapter.notifyDataSetChanged();
    }

    private interface OnMenuItemClickListener {
        public void onItemClick();
    }

    private class StandardMenuItem implements OnMenuItemClickListener {

        private String caption;
        private int imageId;

        private OnMenuItemClickListener onMenuItemClickListener;

        private StandardMenuItem(int imageId, int captionId, OnMenuItemClickListener onMenuItemClickListener) {
            this.imageId = imageId;
            this.caption = context.getString(captionId);
            this.onMenuItemClickListener = onMenuItemClickListener;

            menuItems.add(this);
        }

        public void onItemClick() {
            onMenuItemClickListener.onItemClick();
        }

        public Bitmap getImage() {
            return BitmapFactory.decodeResource(context.getResources(), imageId);
        }

    } // StandardMenuItem

    private class MainMenuAdapter extends BaseAdapter {

        @Override
        public int getCount () {
            return menuItems.size() + bookHistory.bookItems.size();
        }

        @Override
        public long getItemId (int position) {
            return -1;
        }

        @Override
        public Object getItem (int position) {
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;

            if (position < bookHistory.bookItems.size()) {
                rowView = inflater.inflate(R.layout.document_menu_item, parent, false);
                bookHistory.fillBookView(position, rowView);
            }
            else {
                rowView = inflater.inflate(R.layout.main_menu_item, parent, false);
                fillStandardView(position - bookHistory.bookItems.size(), rowView);
            }

            return rowView;
        } // getView

        private void fillStandardView(int index, View view) {
            StandardMenuItem menuItem = menuItems.get(index);

            TextView textView = (TextView) view.findViewById(R.id.label);
            ImageView imageView = (ImageView) view.findViewById(R.id.logo);

            textView.setText(menuItem.caption);
            textView.setTextColor(textColor);
            imageView.setImageBitmap(menuItem.getImage());
        }
    }  // MainMenuAdapter
}
