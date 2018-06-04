package pfg.com.mediacodecproc;

import android.view.SurfaceHolder;

/**
 * Created by FPENG3 on 2018/5/22.
 */

public interface EncoderInterface {

    public boolean init(SurfaceHolder holder, String filePath);

    public void finalize();
}
