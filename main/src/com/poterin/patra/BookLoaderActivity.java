package com.poterin.patra;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class BookLoaderActivity extends Activity {

    public Document catalogue;

    // private static final String libraryServer = "http://192.168.0.100:8080/library/";
    private static final String libraryServer = "http://poterin.ru/ml/library/";

    private boolean catalogueIsLoaded = false;
    private ExpandableListView listView;
    private ListViewAdapter listViewAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.currentTheme());
        setContentView(R.layout.book_loader);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            getActionBar().setIcon(R.drawable.internet_library);

        setTitle(getTitle() + " - " + getString(R.string.internet_library));

        (new Loader()).execute(libraryServer, "index.xml");
    } // onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_loader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuFilter:
                LibraryFilterDialog.open(this);
                break;

            case R.id.menuExpandAll:
                for (int position = 0; position < listView.getExpandableListAdapter().getGroupCount(); position++)
                    listView.expandGroup(position);
                break;

            case R.id.menuCollapseAll:
                collapseAll();
                break;
        } // switch

        return true;
    } // onOptionsItemSelected

    private void collapseAll() {
        for (int position = 0; position < listView.getExpandableListAdapter().getGroupCount(); position++)
            listView.collapseGroup(position);
    }

    private void onCatalogueLoaded(String fileName) {
        try {
            catalogue = XMLUtil.parseFile(fileName);
            XMLUtil.dropVoidTexts(catalogue.getDocumentElement());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!catalogue.getDocumentElement().getAttribute("version").equals("2")) {
            showExceptionDialog(getString(R.string.incorrect_library_version));
            return;
        }

        listViewAdapter = new ListViewAdapter(LibraryFilterDialog.fillBookCatalogue(this));
        checkForEmpty();
        listView = (ExpandableListView) findViewById(R.id.listView);
        listView.setAdapter(listViewAdapter);
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
                Element book = (Element) expandableListView.getExpandableListAdapter().getChild(i, i2);
                (new Loader()).execute(libraryServer, book.getAttribute("file") + ".mlb");
                return true;
            }
        });

        catalogueIsLoaded = true;
    }  // onCatalogueLoaded

    private void onFileLoaded(String fileName) {
        Intent intent = new Intent();
        intent.putExtra("file", fileName);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void onException(Exception e) {
        Utils.logException(e);
        showExceptionDialog(getString(R.string.connection_error));
    }

    private void showExceptionDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.internet_library);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    } // showExceptionDialog

    public void checkForEmpty() {
        TextView textViewNoBooks = (TextView) findViewById(R.id.textViewNoBooks);
        if (listViewAdapter.isEmpty())
            textViewNoBooks.setVisibility(View.VISIBLE);
        else
            textViewNoBooks.setVisibility(View.GONE);
    }

    public void setFilteredCatalogue(BookCatalogue filteredCatalogue) {
        collapseAll();
        listViewAdapter.bookCatalogue = filteredCatalogue;
        listViewAdapter.notifyDataSetChanged();
        checkForEmpty();
    } // setFilteredCatalogue

    private class Loader extends AsyncTask<String, Integer, String> {
        private Exception raisedException = null;
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute () {
            progressDialog = new ProgressDialog(BookLoaderActivity.this);
            if (catalogueIsLoaded)
                progressDialog.setMessage(getString(R.string.loading_book));
            else
                progressDialog.setMessage(getString(R.string.loading_catalogue));
            progressDialog.setProgressNumberFormat("%1d/%2dKb");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Loader.this.cancel(true);
                }
            });
        } // onPreExecute

        @Override
        protected String doInBackground(String... params) {
            try {
                String dataDir = getApplicationInfo().dataDir;
                if (getExternalFilesDir(null) != null) dataDir = getExternalFilesDir(null).getAbsolutePath();
                String fileName = dataDir + "/" + params[1];

                URL url = new URL(params[0] + params[1]);
                URLConnection connection = url.openConnection();
                connection.connect();
                int lengthOfFile = connection.getContentLength();
                progressDialog.setMax(Math.max(lengthOfFile / 1024, 1));

                InputStream input = url.openStream();
                OutputStream output = new FileOutputStream(fileName);

                byte data[] = new byte[1024];
                int total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    // Thread.sleep(2000);
                    total += count;
                    publishProgress(total / 1024);
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                return fileName;
            } catch (Exception e) {
                raisedException = e;
                return null;
            }
        } // doInBackground

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (raisedException == null) {
                if (catalogueIsLoaded) onFileLoaded(result);
                else onCatalogueLoaded(result);
            }
            else
                onException(raisedException);
        }
    } // Loader

    private class ListViewAdapter extends BaseExpandableListAdapter {

        private BookCatalogue bookCatalogue;
        private LayoutInflater inflater;

        private ListViewAdapter(BookCatalogue bookCatalogue) {
            this.bookCatalogue = bookCatalogue;
            inflater = getLayoutInflater();
        }

        @Override
        public int getGroupCount() {
            return bookCatalogue.authors.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return bookCatalogue.authors.get(groupPosition).books.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return bookCatalogue.authors.get(groupPosition).books.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View rowView = convertView;

            if (convertView == null)
                rowView = inflater.inflate(R.layout.author_item, parent, false);

            final TextView textViewAuthor = (TextView) rowView.findViewById(R.id.textViewAuthor);
            textViewAuthor.setText(bookCatalogue.authors.get(groupPosition).name());

            return rowView;
        } // getGroupView

        @Override
        public View getChildView(
            int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
        {
            View rowView;

            if (convertView == null) {
                rowView = inflater.inflate(R.layout.book_title_item, parent, false);
            }
            else
                rowView = convertView;

            Element book = (Element) getChild(groupPosition, childPosition);

            TextView textViewTitle = (TextView) rowView.findViewById(R.id.textViewTitle);
            textViewTitle.setText(
                XMLUtil.findFirstNode(XMLUtil.findFirstNode(book, "title"), bookCatalogue.langId).getTextContent());

            String[] readingMethods = getResources().getStringArray(R.array.read_method_list);
            String bookInfo =
                (book.getAttribute("rmethod").equals("frank") ? readingMethods[2] : readingMethods[1]);

            ArrayList<String> bookLanguages = BookCatalogue.getBookLanguages(book);
            String langList = "";
            for (String LangId : bookLanguages) {
                if (!langList.equals("")) langList += ", ";
                langList += Languages.getLanguageName(LangId);
            }

            TextView textViewBookInfo = (TextView) rowView.findViewById(R.id.textViewBookInfo);
            textViewBookInfo.setText(bookInfo + ";  " + langList);

            return rowView;
        } // getChildView

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    } // ListViewAdapter
}