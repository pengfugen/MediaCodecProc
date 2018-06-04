package pfg.com.mediacodecproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.*;

/**
 * Created by FPENG3 on 2018/5/23.
 */

public class ScreenShotThread extends Thread {

    private final String TAG = "ScreenShotThread";
    Looper mLooper;

    private MediaProjection mProjection;
    ImageReader reader;
    private Activity mActivity;
    private boolean stoped = false;
    Handler mHandler;
    HandlerThread thread;

    public ScreenShotThread(Activity activity, MediaProjection projection) {
        mActivity = activity;
        mProjection = projection;
    }

    @Override
    public void run() {
        synchronized (this) {
            if(!stoped) {
                DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                Util.logi(TAG, "start run============");
                reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
                VirtualDisplay display = mProjection.createVirtualDisplay("ScreenShotDisplay", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, reader.getSurface(), null, null);
                Image image = reader.acquireNextImage();


                if(image != null){
                    int format = image.getFormat();
                    Util.logi(TAG, "ImageFormat:"+format+" width:"+image.getWidth()+" height:"+image.getHeight());
                    Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
                    image.close();
                    try {
                        File file = new File("/sdcard/screenshot.jpeg");
                        file.createNewFile();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(baos.toByteArray());
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        image.close();
                    }
                }

            }

        }


    }

    public void init() {
        thread = new HandlerThread("ScreenShot");
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }


    public void release() {
        synchronized (this) {
            stoped = true;
        }
        reader.close();
        mProjection.stop();


    }
}
