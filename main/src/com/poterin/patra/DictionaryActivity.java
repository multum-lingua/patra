package com.poterin.patra;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.poterin.andorra.Dialog;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.poterin.patra.UserDictionary.userDictionary;

public class DictionaryActivity extends Activity {

    private ListAdapter listAdapter;
    public ListView listView;
    public Speaker speaker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.currentTheme());
        setContentView(R.layout.dictionary);
        speaker = new Speaker(this);

        listView = (ListView) findViewById(R.id.listView);
        listAdapter = new ListAdapter();
        listView.setAdapter(listAdapter);
    }

    @Override
    public void finish() {
        speaker.shutdown();
        super.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_dictionary, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menuEdit).setEnabled(listView.getCheckedItemCount() == 1);
        menu.findItem(R.id.menuDeleteSelected).setEnabled(listView.getCheckedItemCount() > 0);
        menu.findItem(R.id.menuResetStudyProgress).setEnabled(listView.getCheckedItemCount() > 0);
        menu.findItem(R.id.menuDeleteAll).setEnabled(listView.getCount() > 0);
        menu.findItem(R.id.menuDeleteLearnedWords).setEnabled(listView.getCount() > 0);
        menu.findItem(R.id.menuVoiceInTraining).setChecked(Settings.voiceInTraining());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuStudyWords:
                new WordsTraining(this);
                break;

            case R.id.menuAdd:
                userDictionary.showAddToDictionaryDialog(this, "", "", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                break;

            case R.id.menuDeleteSelected:
                deleteSelectedItems();
                break;

            case R.id.menuDeleteAll:
                deleteAllItems();
                break;

            case R.id.menuEdit:
                editSelectedItem();
                break;

            case R.id.menuResetStudyProgress:
                resetStudyProgress();
                break;

            case R.id.menuDeleteLearnedWords:
                deleteLearnedWords();
                break;

            case R.id.menuVoiceInTraining:
                item.setChecked(!item.isChecked());
                Settings.settingsEditor.putBoolean("voiceInTraining", item.isChecked());
                Settings.settingsEditor.commit();
                break;
        } // switch

        return true;
    } // onOptionsItemSelected

    private void deleteLearnedWords() {
        final List<Element> items = new ArrayList<>();

        for (int i = 0; i < listView.getCount(); i++) {
            Element item = userDictionary.getItem(i);
            if (userDictionary.studyProgress(item) == 100)
                items.add(item);
        }

        if (items.size() == 0) {
            Dialog.showMessage(this, getTitle(), getString(R.string.no_learned_words));
            return;
        }

        Dialog.showOKCancel(
            this,
            getTitle(),
            getString(R.string.delete_learned_words_question, items.size()),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userDictionary.deleteItems(items);
                    listAdapter.notifyDataSetChanged();
                }
            });
    } // deleteLearnedWords

    private void resetStudyProgress() {
        Dialog.showOKCancel(
            this,
            getTitle(),
            getString(R.string.reset_study_progress_selected_items, listView.getCheckedItemCount()),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userDictionary.resetStudyProgress(checkedItems());
                    listAdapter.notifyDataSetChanged();
                }
            });
    } // resetStudyProgress

    private void editSelectedItem() {
        userDictionary.showEditItemDialog(
            this,
            checkedItems().get(0),
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listAdapter.notifyDataSetChanged();
                }
            });
    }

    private void deleteSelectedItems() {
        Dialog.showOKCancel(
            this,
            getTitle(),
            getString(R.string.delete_selected_items, listView.getCheckedItemCount()),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userDictionary.deleteItems(checkedItems());
                    listView.clearChoices();
                    listAdapter.notifyDataSetChanged();
                }
            });
    } // deleteSelectedItems

    private List<Element> checkedItems() {
        List<Element> result = new ArrayList<>();

        for (int i = 0; i < listView.getCount(); i++) {
            if (listView.isItemChecked(i))
                result.add(userDictionary.getItem(i));
        }

        return result;
    }

    private void deleteAllItems() {
        Dialog.showOKCancel(
            this,
            getTitle(),
            getString(R.string.delete_all_items),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userDictionary.deleteAllItems();
                    listAdapter.notifyDataSetChanged();
                }
            });
    } // deleteAllItems

    public void refresh() {
        listAdapter.notifyDataSetChanged();
        listView.setVisibility(View.VISIBLE);
    }

    private class ListAdapter extends BaseAdapter {

        private ListAdapter() {
            super();
        }

        @Override
        public int getCount() {
            return userDictionary.items().getLength();
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
                result = inflater.inflate(R.layout.dictionary_item, parent, false);
            } else
                result = convertView;

            Element element = (Element) userDictionary.items().item(position);

            TextView textViewPhrase = (TextView) result.findViewById(R.id.textViewPhrase);
            textViewPhrase.setText(element.getAttribute("phrase"));

            TextView textViewTranslation = (TextView) result.findViewById(R.id.textViewTranslation);
            textViewTranslation.setText(element.getAttribute("translation"));

            TextView textViewProgress = (TextView) result.findViewById(R.id.textViewProgress);
            textViewProgress.setText(userDictionary.studyProgress(element) + "%");

            return result;
        } // getView
    } // ListAdapter
}