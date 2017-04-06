package org.uSurvey.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.uSurvey.android.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wladek on 2/13/17.
 */

public class FormListAdaptor extends BaseAdapter {
    private LinkedHashMap<String, String> forms;
    private Context context;
    private LayoutInflater layoutInflater;

    public FormListAdaptor(LinkedHashMap<String, String> forms, Context context) {
        this.forms = forms;
        this.context = context;
    }

    @Override
    public int getCount() {
        return forms.size();
    }

    @Override
    public Object getItem(int i) {
        return forms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        layoutInflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (view == null) {
            view = layoutInflater.inflate(R.layout.two_item_multiple_choice, null);
        }

        TextView txtFormName = (TextView) view.findViewById(R.id.text1);
        TextView txtFormName2 = (TextView) view.findViewById(R.id.text2);
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        txtFormName2.setVisibility(View.INVISIBLE);
        checkBox.setVisibility(View.INVISIBLE);

        txtFormName.setText(new ArrayList<>(forms.keySet()).get(i));

        return view;
    }
}
