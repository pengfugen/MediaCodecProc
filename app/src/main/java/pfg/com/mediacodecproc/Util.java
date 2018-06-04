package pfg.com.mediacodecproc;

import android.util.Log;

/**
 * Created by FPENG3 on 2018/5/17.
 */

public class Util {

    private static String PRE_TAG = "MediaCodecProc/";

    static void logi(String tag, String msg)
    {
        Log.i(PRE_TAG+tag, msg);
    }
}
