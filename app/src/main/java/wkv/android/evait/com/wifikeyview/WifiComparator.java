package wkv.android.evait.com.wifikeyview;

import java.util.Comparator;

/**
 * Created by as on 27.02.2017.
 */

public class WifiComparator implements Comparator<WifiObject> {
    @Override
    public int compare(WifiObject o1, WifiObject o2) {
        return o1.getSsid().compareToIgnoreCase(o2.getSsid());
    }
}
