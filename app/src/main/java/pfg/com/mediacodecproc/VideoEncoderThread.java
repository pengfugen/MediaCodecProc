package pfg.com.mediacodecproc;

import android.view.SurfaceHolder;

/**
 * Created by FPENG3 on 2018/5/22.
 */

public class VideoEncoderThread extends Thread implements EncoderInterface{

    @Override
    public boolean init(SurfaceHolder holder, String filePath) {
        return false;
    }

    @Override
    public void finalize() {

    }
}
