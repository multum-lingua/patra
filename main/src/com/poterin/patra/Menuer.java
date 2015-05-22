package com.poterin.patra;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class Menuer {

    public interface MenuSelector {
        public void onMenuItemSelect(String menuId);
    }

    private Context context;
    private String[] menuIds;

    public Menuer(final View anchor, final String[] menuIds, final MenuSelector menuSelector, final int widthId) {
        context = anchor.getContext();
        this.menuIds = menuIds;

        final ListPopupWindow lpw = new ListPopupWindow(context);
        lpw.setAnchorView(anchor);
        lpw.setHeight(ListPopupWindow.WRAP_CONTENT);
        lpw.setWidth(Math.round(context.getResources().getDimension(widthId)));
        lpw.setModal(true);
        lpw.setAdapter(new MenuAdapter());
        lpw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lpw.dismiss();
                menuSelector.onMenuItemSelect(menuIds[position]);
            }
        });
        lpw.show();
    } // Menuer

    private class MenuAdapter extends BaseAdapter {

        public MenuAdapter() {
        }

        @Override
        public int getCount() {
            return menuIds.length;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View result = inflater.inflate(R.layout.action_bar_menu_item, parent, false);
            TextView textViewLabel = (TextView) result.findViewById(R.id.textViewLabel);
            ImageView icon = (ImageView) result.findViewById(R.id.imageViewIcon);

            textViewLabel.setText(
                context.getResources().getIdentifier(
                    "menu_" + menuIds[position],
                    "string",
                    context.getPackageName()));

            icon.setImageResource(
                context.getResources().getIdentifier(
                    "menu_" + menuIds[position],
                    "drawable",
                    context.getPackageName()));

            return result;
        } // getView
    } // MenuAdapter
}