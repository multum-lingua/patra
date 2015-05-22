package com.poterin.patra;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import com.poterin.andorra.Dialog;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.poterin.patra.UserDictionary.userDictionary;

public class Translator {

    private static Context context;
    public static final String[] dictionariesIds = new String[] {"google_translator", "color_dict", "yandex"};

    public static void translate(Context context, String phrase, String dictionaryId) {
        Translator.context = context;

        try {
            switch (dictionaryId) {
                case "google_translator": googleTranslate(phrase); break;
                case "color_dict": colorDict(phrase); break;
                case "yandex": yandexTranslate(phrase); break;
            }
        }
        catch (ActivityNotFoundException e) {
            String dictionaryName = null;

            for (int i = 0; i < dictionariesIds.length; i ++) {
                if (dictionariesIds[i].equals(dictionaryId)) {
                    dictionaryName = context.getResources().getStringArray(R.array.dictionaries)[i];
                    break;
                }
            }

            Dialog.showMessage(
                context,
                context.getString(R.string.app_name),
                context.getString(
                    R.string.dictionary_not_found,
                    dictionaryName));
        }
    } // translate

    public static void translate(Context context, String phrase) {
        translate(context, phrase, Settings.defaultDictionary());
    } // translate

    private static void colorDict(String phrase) {
        Intent intent = new Intent("colordict.intent.action.SEARCH");
        intent.putExtra("EXTRA_QUERY", phrase);
        context.startActivity(intent);

        /* http://blog.socialnmobile.com/2011/01/colordict-api-for-3rd-party-developers.html

        public static final String SEARCH_ACTION   = "colordict.intent.action.SEARCH";
        public static final String EXTRA_QUERY    = "EXTRA_QUERY";
        public static final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
        public static final String EXTRA_HEIGHT   = "EXTRA_HEIGHT";
        public static final String EXTRA_WIDTH    = "EXTRA_WIDTH";
        public static final String EXTRA_GRAVITY   = "EXTRA_GRAVITY";
        public static final String EXTRA_MARGIN_LEFT  = "EXTRA_MARGIN_LEFT";
        public static final String EXTRA_MARGIN_TOP   = "EXTRA_MARGIN_TOP";
        public static final String EXTRA_MARGIN_BOTTOM  = "EXTRA_MARGIN_BOTTOM";
        public static final String EXTRA_MARGIN_RIGHT  = "EXTRA_MARGIN_RIGHT";
        intent.putExtra(EXTRA_FULLSCREEN, true);
        intent.putExtra(EXTRA_HEIGHT, 400);
        intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
        intent.putExtra(EXTRA_MARGIN_LEFT, 100);*/
    } // colorDict

    private static void googleTranslate(String phrase) {
        // http://stackoverflow.com/questions/4931245/how-to-use-google-translator-app
        // http://stackoverflow.com/questions/19132441/google-translate-activity-not-working-anymore
        Intent intent = new Intent();

        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, phrase);
        /*intent.putExtra("key_text_output", "");
        intent.putExtra("key_text_input", word);
        intent.putExtra("key_language_from", "rus");
        intent.putExtra("key_language_to", "en");
        intent.putExtra("key_suggest_translation", "");
        intent.putExtra("key_from_floating_window", false);*/

        intent.setComponent(new ComponentName(
            "com.google.android.apps.translate", "com.google.android.apps.translate.TranslateActivity"));
        context.startActivity(intent);
    } // googleTranslate

    private static void yandexTranslate(String phrase) {
        new YandexTask(phrase);
    } // yandexTranslate

    private static class YandexTask extends YandexTaskBase {
        private ProgressDialog progressDialog;

        protected YandexTask(String text) {
            super(text, true);
        }

        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.yandex_translating));
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    YandexTask.this.cancel(true);
                }
            });
        }

        private void showTranslationsDialog(JSONObject result) throws JSONException {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            final List<String> translations = new ArrayList<String>();

            if (result.has("def")) {
                JSONArray def = result.getJSONArray("def");

                for (int i = 0; i < def.length(); i++) {
                    JSONArray tr = def.getJSONObject(i).getJSONArray("tr");
                    for (int j = 0; j < tr.length(); j++) {
                        translations.add(tr.getJSONObject(j).getString("text"));
                    }
                }
            }
            else
                translations.add(getTranslation(result));

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.translation, null);
            builder.setView(dialogView);

            TextView textViewTitle = (TextView) dialogView.findViewById(R.id.textViewTitle);
            textViewTitle.setText(text);
            final ListView listView = (ListView) dialogView.findViewById(R.id.listView);
            ListAdapter listAdapter = new ListAdapter(translations);
            listView.setAdapter(listAdapter);

            final ImageButton imageButtonVoice = (ImageButton) dialogView.findViewById(R.id.imageButtonVoice);
            imageButtonVoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If do it via "new Speaker(context)", then the app hangs on there if another speaker
                    // is working already.
                    DocumentViewerActivity.speaker.speakOut(text, null);
                }
            });

            builder.setPositiveButton(R.string.to_dictionary, null);

            builder.setNeutralButton(android.R.string.ok, null);
            final AlertDialog translationsDialog = builder.create();
            translationsDialog.show();

            translationsDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String translation = null;
                        for (int i = 0; i < translations.size(); i++) {
                            if (listView.isItemChecked(i) || listView.getCheckedItemCount() == 0) {
                                if (translation == null)
                                    translation = translations.get(i);
                                else
                                    translation += ", " + translations.get(i);
                            }
                        }

                        userDictionary.showAddToDictionaryDialog(
                            context,
                            text,
                            translation,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    translationsDialog.dismiss();
                                }
                            });
                    }
                }); // setOnClickListener
        } // showTranslationsDialog

        @Override
        protected void onPostExecute(JSONObject result) {
            progressDialog.dismiss();
            if (raisedException == null) {
                try {
                    showTranslationsDialog(result);
                } catch (Exception e) {
                    Utils.processException(context, e);
                }
            }
            else {
                if (raisedException instanceof IOException)
                    Utils.processException(context, new Exception(context.getString(R.string.yandex_translate_error)));
                else
                    Utils.processException(context, raisedException);
            }
        }

        private class ListAdapter extends BaseAdapter {
            private final List<String> translations;

            private ListAdapter(List<String> translations) {
                super();
                this.translations = translations;
            }

            @Override
            public int getCount() {
                return translations.size();
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
                View result;

                if (convertView == null) {
                    LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    result = inflater.inflate(R.layout.translation_item, parent, false);
                } else
                    result = convertView;

                TextView textView = (TextView) result.findViewById(R.id.textView);
                textView.setText(translations.get(position));

                return result;
            } // getView
        } // ListAdapter
    } // YandexTask

    public abstract static class YandexTaskBase extends AsyncTask<Void, Void, JSONObject> {
        protected final String text;
        private final boolean useDictionary;
        protected Exception raisedException = null;

        protected YandexTaskBase(String text, boolean useDictionary) {
            this.text = text;
            this.useDictionary = useDictionary;
            execute();
        } // YandexTaskBase

        private byte[] httpToByteArray(String url) throws Exception {
            AndroidHttpClient httpClient = AndroidHttpClient.newInstance("AndroidHttpClient");
            HttpResponse response = httpClient.execute(new HttpGet(url));
            byte[] result = EntityUtils.toByteArray(response.getEntity());
            httpClient.close();
            return result;
        }

        private String httpToString(String url) throws Exception {
            return new String(httpToByteArray(url), "UTF-8");
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                String urlSuffix = "&text=" + URLEncoder.encode(text, "UTF-8")
                    + "&lang=" + Languages.getLocaleById(Settings.primaryLanguage()).getLanguage() + "-"
                    + Languages.getLocaleById(Settings.secondaryLanguage()).getLanguage();

                if (useDictionary) {
                    String url = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key="
                        + "dict.1.1.20140811T113738Z.9219c0947b553040.f2ea9f17d698af0fce87b2299e8106ea014da8b8"
                        + urlSuffix;
                    JSONObject response = new JSONObject(httpToString(url));
                    if (response.getJSONArray("def").length() > 0) return response;
                }

                String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?key="
                    + "trnsl.1.1.20140227T121902Z.2354b6990d72edd9.a3c57abf45dbe8dc55e5ed8225868455ce679952"
                    + urlSuffix;

                return new JSONObject(httpToString(url));
            } catch (Exception e) {
                raisedException = e;
                return null;
            }
        } // doInBackground

        protected String getTranslation(JSONObject response) throws JSONException {
            JSONArray jsonArray = response.getJSONArray("text");
            String result = "";
            for (int i = 0; i < jsonArray.length(); i++) result += jsonArray.getString(i) + " ";
            return result.trim();
        }

    } // YandexTaskBase

}
