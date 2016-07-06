package wkv.android.evait.com.wifikeyview;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    final private String TAG = "evait";
    Activity context = null;
    WifiAdapter wifiAdapter = null;
    Thread rootThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);


        wifiAdapter = new WifiAdapter(context);
        ListClickListener listClickListener = new ListClickListener(context);


        listView.setAdapter(wifiAdapter);
        listView.setOnItemClickListener(listClickListener);
        listView.setOnItemLongClickListener(listClickListener);

        /* //testvalues
        WifiObject tmp = new WifiObject();
        tmp.setSsid("easybox");
        tmp.setKey("qwertzuiopasdfghjklyxcvbnm123456789qwertzuiopasdfghjklyxcvbnm123456789qwertzuiopasdfghjklyxcvbnm123456789");
        WifiObject tmp2 = new WifiObject();
        tmp2.setSsid("easybox");
        tmp2.setKey("123456");

        wifiAdapter.add(tmp);
        wifiAdapter.add(tmp2);

        wifiAdapter.notifyDataSetChanged();
        */

        startRootThread();




    }

    private void startRootThread(){
        if ( rootThread != null){
            rootThread.interrupt();
        }

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //wifiAdapter.add(new WifiObject("wifi", temp));
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
                    //InputStream in = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    OutputStream out = process.getOutputStream();
                    String cmd = "cat /data/misc/wifi/wpa_supplicant.conf";
                    //cmd = "ls /data/misc/wifi/";
                    out.write(cmd.getBytes());
                    out.flush();
                    out.close();
                    //byte[] buffer = new byte[1024 * 100]; //Able to read up to 12 KB (12288 bytes)
                    //int length = in.read(buffer);
                    //Log.d(TAG, "laenge: " +length);
                    String line = reader.readLine();

                    WifiObject currWifi = null;
                    while(line!=null){
                        if (line.trim().startsWith("network={")){

                            Log.d(TAG, "network start");
                            currWifi = new WifiObject();
                        }else if(line.trim().startsWith("}")){
                            if (currWifi!=null){
                                addNewItem(currWifi);
                            }

                            Log.d(TAG, "network stop");
                            currWifi = null;
                        }

                        //we are in a 'network' Block
                        else if (currWifi!=null){

                            //Log.d(TAG, line);
                            String[] keyValue = line.split("=", 2);
                            if(keyValue.length == 2){   //the first should be the key and the second the value

                                Log.d(TAG, "   key: '" +keyValue[0].trim() +"' value: '" +keyValue[1].trim() +"'");

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
                //wifiAdapter.add(new WifiObject("wifi", temp));
                if (value!=null && value.getSsid().length()>0 && value.getKey().length()>0)
                wifiAdapter.add(value);
                wifiAdapter.notifyDataSetChanged();
            }
        });
    }
}
