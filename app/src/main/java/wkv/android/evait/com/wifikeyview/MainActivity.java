package wkv.android.evait.com.wifikeyview;

import android.app.Activity;
import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static boolean isSearchable = false;
    final private String TAG = "evait";
    final private boolean isDebug = false;

    private Filter sFilter = null;
    MenuItem myActionMenuItem = null;

    Activity context = null;
    ListView layout_content = null;
    WifiAdapter wifiAdapter = null;
    Thread rootThread = null;
    private SearchView searchView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_main);

        layout_content = (ListView) findViewById(R.id.listView);

        wifiAdapter = new WifiAdapter(context);
        ListClickListener listClickListener = new ListClickListener(context);


        layout_content.setAdapter(wifiAdapter);
        layout_content.setOnItemClickListener(listClickListener);
        layout_content.setOnItemLongClickListener(listClickListener);


        startRootThread();
    }

    private void startRootThread(){
        if ( rootThread != null){
            rootThread.interrupt();
        }

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wifiAdapter.clear();
                wifiAdapter.notifyDataSetChanged();
            }
        });

        rootThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int result = -999; //if all work fine, the value should be zero
                try {
                    Process process = Runtime.getRuntime().exec("su");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    OutputStream out = process.getOutputStream();
                    String cmd = "cat /data/misc/wifi/wpa_supplicant.conf";
                    out.write(cmd.getBytes());
                    out.flush();
                    out.close();
                    String line = reader.readLine();

                    WifiObject currWifi = null;
                    while(line!=null){
                        if (line.trim().startsWith("network={")){
                            if(isDebug) {
                                Log.d(TAG, "new network block start");
                            }
                            currWifi = new WifiObject();
                        }else if(line.trim().startsWith("}")){
                            if (currWifi!=null){
                                boolean ignore = false;

                                //some HTC devices have a fake wifi that should not be printed
                                if(currWifi.getSsid().equalsIgnoreCase("FLAG_FOR_CONFIGURATION_FILE")) {
                                    ignore = true;
                                }

                                if(!ignore) {
                                    addNewItem(currWifi);
                                }
                            }
                            if(isDebug) {
                                Log.d(TAG, "network block stop");
                            }
                            currWifi = null;
                        }

                        //we are in a 'network' Block
                        else if (currWifi!=null){

                            String[] keyValue = line.split("=", 2);
                            if(keyValue.length == 2){   //the first should be the key and the second the value
                                if(isDebug) {
                                    Log.d(TAG, "   key: '" + keyValue[0].trim() + "' value: '" + keyValue[1].trim() + "'");
                                }
                                String value = keyValue[1].trim();
                                switch (keyValue[0].trim()){
                                    case "ssid":
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setSsid(value);
                                        break;
                                    case "password":
                                        currWifi.setTyp(WifiObject.TYP_ENTERPRISE);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "psk":
                                        currWifi.setTyp(WifiObject.TYP_WPA);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "identity":
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setUser(value);
                                        break;
                                    case "wep_key0":
                                        currWifi.setUser("1");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key1":
                                        currWifi.setUser("2");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key2":
                                        currWifi.setUser("3");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key3":
                                        currWifi.setUser("4");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length()-1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                }
                            }
                        }

                        //prepare next line
                        line = reader.readLine();
                    }

                    //String content = new String(buffer, 0, length);
                    //Wait until reading finishes
                    result = process.waitFor();
                    //Do your stuff here with "content" string
                    //The "content" String has the content of readed file
                    //final String temp = content;



                } catch (Exception e) {
                    Log.e(TAG, "IOException, " + e.getMessage());
                    result = -100;

                }
                try{    // wenn zu oft auf reload gedrückt wird, bricht die ab an der stelle ab, deswegen liegt es in einem try catch block
                    if (result != 0){
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "I need root!", Toast.LENGTH_LONG).show();
                                context.finish();
                            }
                        });

                    }
                } catch (Exception e) {
                    Log.e(TAG, "IOException, " + e.getMessage());

                }
            }
        });
        rootThread.start();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //for the search icon
        myActionMenuItem = menu.findItem( R.id.action_search);
        searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchChangeListener(myActionMenuItem));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = false;

        switch (item.getItemId()){
            case R.id.menu_reload:
                startRootThread();

                result = true;
                break;
            case R.id.menu_about:
                final Dialog dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.about_view);

                dialog.show();

                result = true;
                break;

            default:
                result = super.onOptionsItemSelected(item);
                break;
        }

        return result;
    }

    private void addNewItem(final WifiObject value){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value!=null && value.getSsid().length()>0 && value.getKey().length()>0)
                wifiAdapter.add(value);
                wifiAdapter.sort(new WifiComparator());
                sFilter = wifiAdapter.getFilter();
                wifiAdapter.notifyDataSetChanged();
            }
        });
    }

    private class SearchChangeListener implements SearchView.OnQueryTextListener {
        MenuItem myActionMenuItem = null;

        public SearchChangeListener(MenuItem myActionMenuItem) {
            this.myActionMenuItem = myActionMenuItem;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            if(!searchView.isIconified()){
                searchView.setIconified(true);
            }
            myActionMenuItem.collapseActionView();
            if(sFilter != null) {
                sFilter.filter(query);
            }
            return true;
    }
        @Override
        public boolean onQueryTextChange(final String newText) {
            if(sFilter != null  && !searchView.isIconified()) {
                sFilter.filter(newText);
            }
            return true;
        }
    }
}
