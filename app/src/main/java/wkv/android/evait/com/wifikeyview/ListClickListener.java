package wkv.android.evait.com.wifikeyview;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import androidx.appcompat.app.AppCompatActivity;


/**
 * Created by as on 06.07.2016.
 */
public class ListClickListener implements OnItemClickListener, OnItemLongClickListener {
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

    private void copyToClipBoard(String value) {
        copyToClipBoard("Key", value);
    }

    private void copyToClipBoard(String name, String value) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(value);
        } else {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(name, value);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(ctx, "Copied " + name + " to Clipboard", Toast.LENGTH_SHORT).show();
    }

    public void showDialog(final WifiObject curO) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.log_click_dialog);

        LinearLayout btn_name = dialog.findViewById(R.id.btn_name);
        LinearLayout btn_key = dialog.findViewById(R.id.btn_key);
        LinearLayout btn_user = dialog.findViewById(R.id.btn_user);
        LinearLayout btn_qr = dialog.findViewById(R.id.btn_qr);
        LinearLayout btn_share = dialog.findViewById(R.id.btn_share);

        btn_name.setOnClickListener(v -> {
            copyToClipBoard("SSID", curO.getSsid());
            dialog.dismiss();
        });

        btn_key.setOnClickListener(v -> {
            copyToClipBoard("Key", curO.getKey());
            dialog.dismiss();
        });

        if (curO.getTyp().equals(WifiObject.TYP_ENTERPRISE) && curO.getUser().length() > 0) {
            btn_user.setOnClickListener(v -> {
                copyToClipBoard("User", curO.getUser());
                dialog.dismiss();
            });
            btn_user.setVisibility(LinearLayout.VISIBLE);
        } else {
            btn_user.setVisibility(LinearLayout.GONE);
        }

        btn_qr.setOnClickListener(v -> {

            final Dialog dialogQr = new Dialog(ctx);
            dialogQr.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogQr.setCancelable(true);
            dialogQr.setContentView(R.layout.qr_dialog);

            ImageView imageQr = dialogQr.findViewById(R.id.imageQr);
            TextView TextQrSsid = dialogQr.findViewById(R.id.textViewQrSsid);
            TextView TextQrWarn = dialogQr.findViewById(R.id.textViewQrWarn);


            try {

                //WIFI:T:WPA;S:mynetwork;P:mypass;;

                String wifiUser = "";
                String wifiTyp = "WEP"; //default
                if (curO.getTyp().equals(WifiObject.TYP_WEP)) {
                    wifiTyp = "WEP";
                } else if (curO.getTyp().equals(WifiObject.TYP_WPA)) {
                    wifiTyp = "WPA";
                } else if (curO.getTyp().equals(WifiObject.TYP_ENTERPRISE)) {
                    // wpa enterprise is not supported by default qr codes
                    wifiTyp = "WPA";
                    wifiUser = "U:" + qrEncode(curO.getUser());
                    //Toast.makeText(ctx, R.string.error_qr_wpa_enterprise, Toast.LENGTH_LONG).show();
                    TextQrWarn.setText(ctx.getText(R.string.error_qr_wpa_enterprise));
                    TextQrWarn.setVisibility(View.VISIBLE);
                }


                String wifiText = "WIFI:T:"
                        + qrEncode(wifiTyp) + ";S:"
                        + qrEncode(curO.getSsid()) + ";P:"
                        + qrEncode(curO.getKey()) + ";" + wifiUser + ";";


                final Bitmap pix = encodeAsBitmap(wifiText);

                if (pix != null) {
                    imageQr.setImageBitmap(pix);
                    TextQrSsid.setText(curO.getSsid());
                    dialogQr.show();
                } else {
                    Toast.makeText(ctx, R.string.error_qr_generate, Toast.LENGTH_SHORT).show();
                }

            } catch (WriterException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }


            dialog.dismiss();
        });

        btn_share.setOnClickListener(v -> {
            Intent txtIntent = new Intent(Intent.ACTION_SEND);
            txtIntent.setType("text/plain");
            txtIntent.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.app_name));
            txtIntent.putExtra(Intent.EXTRA_TEXT, curO.toString());
            ctx.startActivity(Intent.createChooser(txtIntent, "Share"));
            dialog.dismiss();
        });

        dialog.show();

    }

    Bitmap encodeAsBitmap(String str) throws WriterException {

        BitMatrix result;
        try {
            int qrSize = getQrSize();
            if (MainActivity.isDebug) {
                Log.d(MainActivity.TAG, "QR-Size: " + qrSize);
            }

            result = new QRCodeWriter().encode(str,
                    BarcodeFormat.QR_CODE, qrSize, qrSize);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private String qrEncode(String str) {
        // https://github.com/zxing/zxing/wiki/Barcode-Contents#wifi-network-config-android
        // some magic at this point. they say wi have to escape  "\", ";", "," and ":"
        // but everyone seems to escape only some of them...
        // eg. https://zxing.appspot.com/generator/ did not escape ","


        String result = str;
        if (result == null || result.isEmpty()) {
            result = "";
        }

        result = result.replaceAll("\\\\", "\\\\\\\\");
        result = result.replaceAll(";", "\\\\;");
        result = result.replaceAll(",", "\\\\,");
        result = result.replaceAll(":", "\\\\:");


        return result;
    }

    private int getQrSize() {

        // get available height and width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((AppCompatActivity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // get the smallest value
        int result = width;
        if (height < width) {
            result = height;
        }

        if (result <= 0) {
            return 900;
        } else {
            //return 70% of screen
            return ((int) ((((double) result) * 70) / 100));
        }

    }


}
