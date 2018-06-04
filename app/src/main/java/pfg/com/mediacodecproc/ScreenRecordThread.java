package pfg.com.mediacodecproc;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.MediaRecorder;
import android.hardware.display.DisplayManager;

import java.io.IOException;

/**
 * Created by FPENG3 on 2018/5/22.
 */

public class ScreenRecordThread extends Thread {

    private MediaRecorder mRecord;
    MediaProjection mMediaProjection;

    private String TAG = "ScreenRecordThread";

    public ScreenRecordThread(MediaProjection projection) {
        //projection.createVirtualDisplay()
        mMediaProjection = projection;
    }

    private void initMediaRecorder() {
        mRecord = new MediaRecorder();

        mRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecord.setAudioChannels(2);


        /*mRecord.setAudioSamplingRate(MediaRecorder.);
        mRecord.setAudioEncodingBitRate();*/
        mRecord.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置封装格式
        mRecord.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecord.setOutputFile("/sdcard/screenshot01.mp4");

        mRecord.setVideoFrameRate(15);
        mRecord.setVideoSize(1280, 720);

        mRecord.setVideoEncodingBitRate(6000000);

        mRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecord.setVideoEncoder(MediaRecorder.VideoEncoder.H264);


    }

    @Override
    public void run() {
        try {
            initMediaRecorder();

            mRecord.prepare();

            VirtualDisplay mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display", 1280, 720, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mRecord.getSurface(), null, null);
            Util.logi(TAG, "created virtual display: " + mVirtualDisplay);

            mRecord.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void release() {
        mRecord.reset();
        mRecord.stop();
        mRecord.release();
    }
}
