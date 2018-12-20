package wkv.android.evait.com.wifikeyview;

/**
 * Created by as on 05.07.2016.
 */
public class WifiObject {
    private String ssid;
    private String key;
    private String user;
    private String typ;

    public static String TYP_ENTERPRISE = "802.1x";
    public static String TYP_WEP = "WEP";
    public static String TYP_WPA = "WPA/WPA2";

    public WifiObject() {
    }

    public WifiObject(String ssid, String key) {
        this.ssid = ssid;
        this.key = key;
    }

    public String getSsid() {
        if (ssid == null) {
            return "";
        }
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getKey() {
        if (key == null) {
            return "";
        }
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUser() {
        if (user == null) {
            return "";
        }
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTyp() {
        if (typ == null) {
            return "";
        }
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    @Override
    public String toString() {
        String separator = System.getProperty("line.separator");
        String result = "";
        result += "SSID: " + getSsid() + separator;

        if (getUser().length() > 0) {
            if (getTyp().equals(WifiObject.TYP_ENTERPRISE)) {
                result += "User: ";
            } else if (getTyp().equals(WifiObject.TYP_WEP)) {
                result += "Keyindex: ";
            }
            result += getUser() + separator;
        }
        result += "Key: " + getKey();
        return result;
    }
}
