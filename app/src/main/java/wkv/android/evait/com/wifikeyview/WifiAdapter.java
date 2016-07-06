package wkv.android.evait.com.wifikeyview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by as on 05.07.2016.
 */
public class WifiAdapter extends ArrayAdapter<WifiObject> {
    public WifiAdapter(Context context, ArrayList<WifiObject> wifiO) {
        super(context, 0, wifiO);
    }
    public WifiAdapter(Context context) {
        super(context, 0, new  ArrayList<WifiObject>());
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
        TextView tvSsid = (TextView) convertView.findViewById(R.id.tv_ssid);
        TextView tvKey = (TextView) convertView.findViewById(R.id.tv_key);
        TextView tvTyp = (TextView) convertView.findViewById(R.id.tv_typ);
        TextView tvUser = (TextView) convertView.findViewById(R.id.tv_user);
        if (wifiO.getUser().length()>0) {
            String begin = "";
            if (wifiO.getTyp().equals(WifiObject.TYP_ENTERPRISE)){
                begin = "User: ";
            } else if(wifiO.getTyp().equals(WifiObject.TYP_WEP)){
                begin = "Keyindex: ";
            }
            tvUser.setText(begin + wifiO.getUser());
            tvUser.setVisibility(TextView.VISIBLE);
        }else{
            tvUser.setVisibility(TextView.GONE);
        }
        // Populate the data into the template view using the data object
        tvSsid.setText(wifiO.getSsid());
        tvKey.setText(wifiO.getKey());
        tvTyp.setText(wifiO.getTyp());
        // Return the completed view to render on screen
        return convertView;
    }
}