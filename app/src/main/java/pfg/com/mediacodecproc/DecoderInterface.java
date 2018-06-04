package pfg.com.mediacodecproc;

import android.view.SurfaceHolder;

/**
 * Created by FPENG3 on 2018/5/18.
 */

public interface DecoderInterface {

    public boolean init(SurfaceHolder holder, String filePath);

    public void finalize();
}
