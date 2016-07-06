package wkv.android.evait.com.wifikeyview;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by as on 06.07.2016.
 */
public class ListClickListener implements OnItemClickListener, OnItemLongClickListener{
    private Context ctx;
    private int sdk = android.os.Build.VERSION.SDK_INT;
    public ListClickListener(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        WifiObject curO = (WifiObject) parent.getItemAtPosition(position);
        copyToClipBoard(curO.getKey());

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        showDialog((WifiObject) parent.getItemAtPosition(position));
        return true;
    }

    private void copyToClipBoard(String value){
        copyToClipBoard("Key", value);
    }
    private void copyToClipBoard(String name, String value){
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(value);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(name, value);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(ctx, "Copied " +name +" to Clipboard", Toast.LENGTH_SHORT).show();
    }

    public void showDialog(final WifiObject curO){
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.log_click_dialog);

        LinearLayout btn_name = (LinearLayout) dialog.findViewById(R.id.btn_name);
        LinearLayout btn_key = (LinearLayout) dialog.findViewById(R.id.btn_key);
        LinearLayout btn_user = (LinearLayout) dialog.findViewById(R.id.btn_user);
        LinearLayout btn_share = (LinearLayout) dialog.findViewById(R.id.btn_share);

        btn_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipBoard("SSID", curO.getSsid());
                dialog.dismiss();
            }
        });
        btn_key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipBoard("Key", curO.getKey());
                dialog.dismiss();
            }
        });
        if(curO.getTyp().equals(WifiObject.TYP_ENTERPRISE) && curO.getUser().length()>0) {
            btn_user.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyToClipBoard("User", curO.getUser());
                    dialog.dismiss();
                }
            });
            btn_user.setVisibility(LinearLayout.VISIBLE);
        }else{
            btn_user.setVisibility(LinearLayout.GONE);
        }

        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
                txtIntent.setType("text/plain");
                txtIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ctx.getString(R.string.app_name));
                txtIntent.putExtra(android.content.Intent.EXTRA_TEXT, curO.toString());
                ctx.startActivity(Intent.createChooser(txtIntent ,"Share"));
                dialog.dismiss();
            }
        });

        dialog.show();

    }


}
