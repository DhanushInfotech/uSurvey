package org.uSurvey.android.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.uSurvey.android.R;
import org.uSurvey.android.adapters.FormListAdaptor;
import org.uSurvey.android.application.Collect;
import org.uSurvey.android.provider.InstanceProviderAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wladek on 2/11/17.
 */

public class SearchFormsActivity extends Activity {
    private static final String TAG = SearchFormsActivity.class.getName();
    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private AlertDialog mAlertDialog;

    private FormListAdaptor mFormListAdapter;
    private ListView searchFormsResults;
    private SearchView searchForms;
    private LinkedHashMap<String, String> searchResults = new LinkedHashMap<>();

    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_forms);
        searchFormsResults = (ListView) findViewById(R.id.searchFormsResults);
        searchForms = (SearchView) findViewById(R.id.searchForms);

        searchFormsResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String instanceFilePath = new ArrayList<>(searchResults.values()).get(i);
                Cursor c = getInstanceCursorBaseInstanceFile(instanceFilePath);
                while (c.moveToNext()) {
                    Uri instanceUri =
                            ContentUris.withAppendedId(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                                    c.getLong(c.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID)));

                    Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", instanceUri.toString());

                    String action = getIntent().getAction();
                    if (Intent.ACTION_PICK.equals(action)) {
                        // caller is waiting on a picked form
                        setResult(RESULT_OK, new Intent().setData(instanceUri));
                    } else {
                        // the form can be edited if it is incomplete or if, when it was
                        // marked as complete, it was determined that it could be edited
                        // later.
                        String status = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.STATUS));
                        String strCanEditWhenComplete =
                                c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE));

                        boolean canEdit = status.equals(InstanceProviderAPI.STATUS_INCOMPLETE)
                                || Boolean.parseBoolean(strCanEditWhenComplete);
                        if (!canEdit) {
                            createErrorDialog(getString(R.string.cannot_edit_completed_form),
                                    DO_NOT_EXIT);
                            return;
                        }
                        // caller wants to view/edit a form, so launch formentryactivity
                        startActivity(new Intent(Intent.ACTION_EDIT, instanceUri));
                    }
                    finish();

//                Intent intent = new Intent(SearchFormsActivity.this, InstanceChooserList.class);
//
//                intent.setAction(Intent.ACTION_EDIT);
////                intent.setData(Uri.parse((String) adapterView.getItemAtPosition(i)));
//                intent.setData(Uri.parse(val));
//
//                startActivity(intent);
                }
            }
        });

        searchForms.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                if (s.length() > 3) {
                    SearchFileAsync searchFileAsync = new SearchFileAsync(searchResults, SearchFormsActivity.this);
                    searchFileAsync.execute(s);
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog",
                                shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    public Cursor getInstanceCursorBaseInstanceFile(String instanceFilePath) {
        // get instances based on instanceFilePath
        String selection = InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String selectionArgs[] = {instanceFilePath};
        String sortOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor c = managedQuery(InstanceProviderAPI.InstanceColumns.CONTENT_URI, null, selection,
                selectionArgs, sortOrder);
        return c;
    }

    public class SearchFileAsync extends AsyncTask<String, Void, String> {
        private String searchText;
        ProgressDialog mProgressDialog;
        private LinkedHashMap<String, String> searchResults;
        Context context;

        public SearchFileAsync(LinkedHashMap<String, String> searchResults, Context context) {
            this.searchResults = searchResults;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("Searching forms");
            mProgressDialog.setMessage(getString(R.string.please_wait));
            mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }


        @Override
        protected String doInBackground(String... strings) {
            searchText = strings[0];

            try {
                File directory = new File(Collect.INSTANCES_PATH);
                System.out.println(" INSTANCE PATH : " + Collect.INSTANCES_PATH);
                searchResults.clear();
                readDir(directory);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "Ok";
        }


        protected void readDir(File directory) {
            Log.d(TAG, "++ READING DIRECTORY ++");

            try {

                File[] fList = directory.listFiles();

                int count = 0;

                for (File file : fList) {
                    System.out.println(" DIRECTORY : " + count);
                    count++;
                    if (file.isDirectory()) {
                        readDir(file);
                    } else if (file.isFile()) {
                        try {
                            readFile(file, searchText);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        protected void readFile(File file, String searchText) throws IOException {
            Log.d(TAG, " ++ READING FILE ++ ");

            if (searchText.toLowerCase().contains("instance")) {
                return;
            }
            BufferedReader fileStream = null;

            StringBuilder stringBuilder = new StringBuilder();


            try {

                fileStream = new BufferedReader(new FileReader(file));

                String line;
                String replaceStr = "__";
                while ((line = fileStream.readLine()) != null) {
                    stringBuilder.append(line);
                }
                line = stringBuilder.toString();
                String tags = "<\\/?[^>]+>";
                String output = line.replaceAll(tags, replaceStr).replaceAll("\t", "");

                int index = output.toLowerCase().indexOf(searchText.toLowerCase());
                String result = "";

                if (index != -1) {
                    String str1 = output.substring(0, index);
                    String str2 = output.substring(index, output.length() - 1);
                    String subStr1 = str1.substring(str1.lastIndexOf(replaceStr) != -1 ? str1.lastIndexOf(replaceStr) + replaceStr.length()
                            : 0, str1.length());
                    String subStr2 = str2.substring(0, str2.indexOf(replaceStr) != -1 ? str2.indexOf(replaceStr) : str2.length() - 1);
                    result = subStr1 + subStr2;

                    String filtered = line.substring(line.indexOf("<instanceName>") + 14, line.indexOf("</instanceName>")) + "\n" + result;
                    System.out.println(" ++++++ Line Match +++ " + line);
                    searchResults.put(filtered, file.getAbsolutePath());
                }

//                DocumentBuilderFactory builderFactory =
//                        DocumentBuilderFactory.newInstance();
//                DocumentBuilder builder = null;
//
//
//                try {
//                    builder = builderFactory.newDocumentBuilder();
//
//                    Document document = builder.parse(
//                            new FileInputStream(file.getAbsolutePath()));
//                    String expression = "string(/)";
//                    XPath xPath = XPathFactory.newInstance().newXPath();
//                    String resultStr = null;
//                    resultStr = xPath.compile(expression).evaluate(document);
//
//                    if (resultStr.toLowerCase().contains(searchText.toLowerCase())) {
//                        String filtered = line.substring(line.indexOf("<instanceName>") + 14, line.indexOf("</instanceName>"));
//                        System.out.println(" ++++++ Line Match +++ " + line);
//                        searchResults.put(filtered, file.getAbsolutePath());
//
//                    }
//
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (OutOfMemoryError e) {
//                    e.printStackTrace();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            } finally {
                fileStream.close();
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            System.out.println(" RESULT LENGTH : " + searchResults.size());
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (searchResults.size() < 1 || searchResults.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Nothing found!", Toast.LENGTH_SHORT).show();
            } else {
                mFormListAdapter =
                        new FormListAdaptor(searchResults, getApplicationContext());

                searchFormsResults.setAdapter(mFormListAdapter);
            }
        }
    }

}
