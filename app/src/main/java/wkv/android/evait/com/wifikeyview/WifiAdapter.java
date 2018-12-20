package wkv.android.evait.com.wifikeyview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by as on 05.07.2016.
 */
public class WifiAdapter extends ArrayAdapter<WifiObject> {
    AppCompatActivity ctx;

    public WifiAdapter(Context context, ArrayList<WifiObject> wifiO) {
        super(context, 0, wifiO);
        this.ctx = (AppCompatActivity) context;

    }

    public WifiAdapter(Context context) {
        super(context, 0);
        this.ctx = (AppCompatActivity) context;
    }

    @Override
    public void sort(Comparator<? super WifiObject> comparator) {
        super.sort(comparator);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        WifiObject wifiO = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_view, parent, false);
        }
        // Lookup view for data population
        TextView tvSsid = convertView.findViewById(R.id.tv_ssid);
        TextView tvKey = convertView.findViewById(R.id.tv_key);
        TextView tvTyp = convertView.findViewById(R.id.tv_typ);
        TextView tvUser = convertView.findViewById(R.id.tv_user);
        if (wifiO.getUser().length() > 0) {
            String begin = "";
            if (wifiO.getTyp().equals(WifiObject.TYP_ENTERPRISE)) {
                begin = "User: ";
            } else if (wifiO.getTyp().equals(WifiObject.TYP_WEP)) {
                begin = "Keyindex: ";
            }
            tvUser.setText(begin + wifiO.getUser());
            tvUser.setVisibility(TextView.VISIBLE);
        } else {
            tvUser.setVisibility(TextView.GONE);
        }
        // Populate the data into the template view using the data object
        tvSsid.setText(wifiO.getSsid());
        tvKey.setText(wifiO.getKey());
        tvTyp.setText(wifiO.getTyp());
        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public Filter getFilter() {

        ArrayList<WifiObject> res = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) {
            res.add(getItem(i));
        }

        return new searchFilter(res, this, ctx);
    }

    private class searchFilter extends Filter {
        ArrayList<WifiObject> org;
        WifiAdapter wiAdapter;
        AppCompatActivity ctx;

        public searchFilter(ArrayList<WifiObject> res, WifiAdapter wiAdapter, AppCompatActivity ctx) {
            org = res;
            this.wiAdapter = wiAdapter;
            this.ctx = ctx;

        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults f = new FilterResults();
            //wiAdapter.clear();
            if (constraint != null) {
                ArrayList<WifiObject> res = new ArrayList<>();
                for (int x = 0; x < org.size(); x++) {
                    if (org.get(x).getSsid().toLowerCase().contains(constraint)) {
                        res.add(org.get(x));
                    }
                }
                f.values = res;//.toArray();
                f.count = res.size();
            }
            return f;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.count > 0) {
                wiAdapter.clear();
                wiAdapter.addAll((ArrayList<WifiObject>) results.values);
                ctx.runOnUiThread(WifiAdapter.this::notifyDataSetChanged);
            } else {
                wiAdapter.clear();
                wiAdapter.addAll(org);
                ctx.runOnUiThread(WifiAdapter.this::notifyDataSetChanged);
            }
        }
    }
}