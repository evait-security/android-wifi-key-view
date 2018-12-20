package wkv.android.evait.com.wifikeyview;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    BillingProcessor bp;
    private static boolean isSearchable = false;
    final static public String TAG = "evait";
    final static public boolean isDebug = true;
    final static private String InAppPubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAowTgroHqoXfm5kxQMKsO6xIMB/GemcYR9BPkud+rXBXcvLX4NmkuxG1GRw9/XMrhD2KqzMNWJg6Mu7u+r7joUbbG664N7kLWPpwAb98N0pa9a/BqXY8al9dVgFAhjTElH9jAs3CXGGgbChy889Z6k+EOHEkU4WE1dTpSl7vil3hbGJDS3RNPlxaZpYmknHFVF4ObnflN3l+qmexrRUsNNjK+FuEwSWAR6WXu/l6PV6wDWEmuJnwKtNvkF3At4de0vfV5Oals76IE1jbxbmcNJCWYE6kkm+7vGk1AT6YfVq6utjg7ZERhlL/OGa3X+PRA9iOiRrDCCxxTUM/BEqxdfQIDAQAB\n\n";

    private Filter sFilter = null;
    MenuItem myActionMenuItem = null;

    AppCompatActivity context = null;
    ListView layout_content = null;
    WifiAdapter wifiAdapter = null;
    Thread rootThread = null;
    private SearchView searchView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        bp = new BillingProcessor(this, InAppPubKey, this);

        setContentView(R.layout.activity_main);

        layout_content = findViewById(R.id.listView);

        wifiAdapter = new WifiAdapter(context);
        ListClickListener listClickListener = new ListClickListener(context);

        layout_content.setAdapter(wifiAdapter);
        layout_content.setOnItemClickListener(listClickListener);
        layout_content.setOnItemLongClickListener(listClickListener);

        startRootThread();
    }

    private void startRootThread() {
        if (rootThread != null) {
            rootThread.interrupt();
        }

        context.runOnUiThread(() -> {
            wifiAdapter.clear();
            wifiAdapter.notifyDataSetChanged();
        });

        rootThread = new Thread(() -> {
            /** check for root */
            int result = -999; //if all work fine, the value should be zero

            try {
                Process process = Runtime.getRuntime().exec("su");
                OutputStream out = process.getOutputStream();
                String cmd = "cat";
                out.write(cmd.getBytes());
                out.flush();
                out.close();
                result = process.waitFor();

            } catch (Exception e) {
                Log.e(TAG, "IOException, " + e.getMessage());
                result = -100;

            }
            try {  // wenn zu oft auf reload gedrückt wird, bricht die ab an der stelle ab, deswegen liegt es in einem try catch block
                if (result != 0) {
                    context.runOnUiThread(() -> {
                        Toast.makeText(context, "I need root!", Toast.LENGTH_LONG).show();
                        context.finish();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "IOException, " + e.getMessage());
            }

            /** read xmls since oreo */
            try {
                Process process = Runtime.getRuntime().exec("su");
                //BufferedReader reader = new BufferedReader(new InputStreamReader());

                OutputStream out = process.getOutputStream();
                String cmd = "cat /data/misc/wifi/WifiConfigStore.xml";
                out.write(cmd.getBytes());
                out.flush();
                out.close();

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(process.getInputStream());


                Element element = doc.getDocumentElement();
                element.normalize();

                WifiObject currWifi = null;
                NodeList nListNet = doc.getElementsByTagName("Network");
                for (int indexNet = 0; indexNet < nListNet.getLength(); indexNet++) {
                    Node nNet = nListNet.item(indexNet);

                    NodeList nListWC = ((Element) nNet).getElementsByTagName("WifiConfiguration");
                    for (int i = 0; i < nListWC.getLength(); i++) {
                        Node nodeWC = nListWC.item(i);
                        if (nodeWC.getNodeType() == Node.ELEMENT_NODE) {
                            if (xmlHasNodeAttribute(nodeWC, "string", "ConfigKey")) {
                                currWifi = new WifiObject();
                                String name = xmlGetNodeValueByAttribute(nodeWC, "string", "SSID");
                                name = name.substring(1, name.length() - 1); //trim the first and the last char that are "
                                currWifi.setSsid(name);

                                if (xmlHasNodeAttribute(nodeWC, "string", "PreSharedKey")) {
                                    currWifi.setTyp(WifiObject.TYP_WPA);
                                    String key = xmlGetNodeValueByAttribute(nodeWC, "string", "PreSharedKey");
                                    key = key.substring(1, key.length() - 1); //trim the first and the last char that are "
                                    currWifi.setKey(key);
                                } else {
                                    Node nodeWep = xmlGetNodeByAttribute(nodeWC, "string-array", "WEPKeys");
                                    if (nodeWep != null) {
                                        currWifi.setTyp(WifiObject.TYP_WEP);

                                        NodeList nListWepKeys = ((Element) nodeWep).getElementsByTagName("item");
                                        for (int indexWepKey = 0; indexWepKey < nListWepKeys.getLength(); indexWepKey++) {
                                            NamedNodeMap temp = nListWepKeys.item(indexWepKey).getAttributes();
                                            for (int tempIndex = 0; tempIndex < temp.getLength(); tempIndex++) {
                                                String key = nListWepKeys.item(indexWepKey).getAttributes().item(tempIndex).getNodeValue();
                                                if (key.length() > 2) {
                                                    key = key.substring(1, key.length() - 1); //trim the first and the last char that are "
                                                    currWifi.setKey(key);
                                                    currWifi.setUser((indexWepKey + 1) + "");
                                                }
                                            }
                                        }
                                    }

                                }

                                //currWifi.setKey("bla");
                                //currWifi.setTyp(WifiObject.TYP_WPA);
                                //addNewItem(currWifi);
                            }
                        }
                    }

                    NodeList nListWpaeC = ((Element) nNet).getElementsByTagName("WifiEnterpriseConfiguration");
                    for (int i = 0; i < nListWpaeC.getLength(); i++) {
                        Node nodeWpaeC = nListWpaeC.item(i);
                        if (nodeWpaeC.getNodeType() == Node.ELEMENT_NODE) {
                            String user = xmlGetNodeValueByAttribute(nodeWpaeC, "string", "Identity");
                            //user = user.substring(1, user.length() - 1); //trim the first and the last char that are "
                            currWifi.setUser(user);

                            String key = xmlGetNodeValueByAttribute(nodeWpaeC, "string", "Password");
                            //key = user.substring(1, key.length() - 1); //trim the first and the last char that are "
                            currWifi.setKey(key);

                            currWifi.setTyp(WifiObject.TYP_ENTERPRISE);
                        }
                    }

                    addNewItem(currWifi);
                }

                //String content = new String(buffer, 0, length);
                //Wait until reading finishes
                result = process.waitFor();
                //Do your stuff here with "content" string
                //The "content" String has the content of read file
                //final String temp = content;


            } catch (Exception e) {
                Log.e(TAG, "IOException, " + e.getMessage());

                result = -100;

            }

            /** read config files before oreo **/
            String[] wConfFiles = {
                    "/data/misc/wifi/*_supplicant*.conf",
                    "/data/wifi/bcm_supp.conf"
            };
            for (String wConfFile : wConfFiles) {
                try {
                    Process process = Runtime.getRuntime().exec("su");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    OutputStream out = process.getOutputStream();
                    // different OS version use different file to save wifi passes
                    // e.g.
                    // /data/misc/wifi/wpa_supplicant.conf
                    // /data/misc/wifi/wpa_supplicant_wcn.conf
                    // /data/misc/wifi/p2p_supplicant.conf
                    String cmd = "cat " + wConfFile;
                    out.write(cmd.getBytes());
                    out.flush();
                    out.close();
                    String line = reader.readLine();

                    WifiObject currWifi = null;
                    while (line != null) {
                        if (line.trim().startsWith("network={")) {
                            if (isDebug) {
                                Log.d(TAG, "new network block start");
                            }
                            currWifi = new WifiObject();
                        } else if (line.trim().startsWith("}")) {
                            if (currWifi != null) {
                                boolean ignore = false;

                                //some HTC devices have a fake wifi that should not be printed
                                if (currWifi.getSsid().equalsIgnoreCase("FLAG_FOR_CONFIGURATION_FILE")) {
                                    ignore = true;
                                }

                                if (!ignore) {
                                    addNewItem(currWifi);
                                }
                            }
                            if (isDebug) {
                                Log.d(TAG, "network block stop");
                            }
                            currWifi = null;
                        }

                        //we are in a 'network' Block
                        else if (currWifi != null) {

                            String[] keyValue = line.split("=", 2);
                            if (keyValue.length == 2) {   //the first should be the key and the second the value
                                if (isDebug) {
                                    Log.d(TAG, "   key: '" + keyValue[0].trim() + "' value: '" + keyValue[1].trim() + "'");
                                }
                                String value = keyValue[1].trim();
                                switch (keyValue[0].trim()) {
                                    case "ssid":
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setSsid(value);
                                        break;
                                    case "password":
                                        currWifi.setTyp(WifiObject.TYP_ENTERPRISE);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "psk":
                                        currWifi.setTyp(WifiObject.TYP_WPA);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "identity":
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setUser(value);
                                        break;
                                    case "wep_key0":
                                        currWifi.setUser("1");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key1":
                                        currWifi.setUser("2");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key2":
                                        currWifi.setUser("3");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
                                        currWifi.setKey(value);
                                        break;
                                    case "wep_key3":
                                        currWifi.setUser("4");
                                        currWifi.setTyp(WifiObject.TYP_WEP);
                                        value = value.substring(1, value.length() - 1); //trim the first and the last char that are "
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
                    //The "content" String has the content of rea file
                    //final String temp = content;


                } catch (Exception e) {
                    Log.e(TAG, "IOException, " + e.getMessage());
                    result = -100;

                }
            }
        });
        rootThread.start();
    }


    public static Node xmlGetNodeByAttribute(Node root, String tag, String attribute) {
        NodeList nListS = ((Element) root).getElementsByTagName(tag);
        for (int indexS = 0; indexS < nListS.getLength(); indexS++) {
            Node nodeS = nListS.item(indexS);
            if (nodeS.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap nodeSAttributes = nodeS.getAttributes();
                for (int t = 0; t < nodeSAttributes.getLength(); t++) {
                    if (nodeSAttributes.item(t).getNodeValue().equals(attribute)) {
                        return nodeS;
                    }
                }
            }
        }
        return null;
    }

    public static String xmlGetNodeValueByAttribute(Node root, String tag, String attribute) {
        Node node = xmlGetNodeByAttribute(root, tag, attribute);
        if (node != null) {
            return node.getChildNodes().item(0).getNodeValue();
        }
        return "";
    }

    public static boolean xmlHasNodeAttribute(Node root, String tag, String attribute) {
        return xmlGetNodeByAttribute(root, tag, attribute) != null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //for the search icon
        myActionMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(myActionMenuItem);
        searchView.setOnQueryTextListener(new SearchChangeListener(myActionMenuItem));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result;
        final Dialog dialog = new Dialog(context);

        switch (item.getItemId()) {
            case R.id.menu_reload:
                startRootThread();

                result = true;
                break;

            case R.id.menu_about:
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.about_view);
                ((TextView) dialog.findViewById(R.id.textViewVersionName)).setText(BuildConfig.VERSION_NAME);
                dialog.show();

                result = true;
                break;

            case R.id.menu_donate:
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.donate_view);
                ((Spinner) dialog.findViewById(R.id.spinner_donate)).setSelection(1);
                Button btnDonate = dialog.findViewById(R.id.btn_donate);
                btnDonate.setOnClickListener(new DonateButtonListener(dialog));
                dialog.show();

                result = true;
                break;

            default:
                result = super.onOptionsItemSelected(item);
                break;
        }

        return result;
    }

    private void addNewItem(final WifiObject value) {
        context.runOnUiThread(() -> {
            if (value != null && value.getSsid().length() > 0 && value.getKey().length() > 0)
                wifiAdapter.add(value);
            wifiAdapter.sort(new WifiComparator());
            sFilter = wifiAdapter.getFilter();
            wifiAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
    }

    @Override
    public void onPurchaseHistoryRestored() {
    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
    }

    @Override
    public void onBillingInitialized() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }

    private class SearchChangeListener implements SearchView.OnQueryTextListener {
        MenuItem myActionMenuItem;

        public SearchChangeListener(MenuItem myActionMenuItem) {
            this.myActionMenuItem = myActionMenuItem;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
            }
            MenuItemCompat.collapseActionView(myActionMenuItem);
            if (sFilter != null) {
                sFilter.filter(query);
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(final String newText) {
            if (sFilter != null && !searchView.isIconified()) {
                sFilter.filter(newText);
            }
            return true;
        }
    }

    private class DonateButtonListener implements Button.OnClickListener {
        private Dialog dialog;

        public DonateButtonListener(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View view) {
            int position = ((Spinner) dialog.findViewById(R.id.spinner_donate)).getSelectedItemPosition();
            if (isDebug) {
                Log.d(TAG, "selected donate: " + position);
            }
            switch (position) {
                case 0:
                    bp.purchase(context, "donate_min");
                    break;
                case 2:
                    bp.purchase(context, "donate_big");
                    break;
                case 1: // do not break, because donate_normal is the default value
                default:
                    bp.purchase(context, "donate_normal");
                    break;
            }
            dialog.cancel();
        }
    }
}
