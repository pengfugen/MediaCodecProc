package pfg.com.mediacodecproc;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by FPENG3 on 2018/5/18.
 */

public class AudioDecoderThread extends Thread implements DecoderInterface{
    private String TAG = "VideoDecoderThread";

    private MediaExtractor mExtrator;
    private MediaCodec mCodec;
    int mSampleRate;
    int channel;

    private boolean eosReceived = false;

    @Override
    public boolean init(SurfaceHolder holder, String filePath) {
        mExtrator = new MediaExtractor();
        try {
            mExtrator.setDataSource(filePath);
            int count = mExtrator.getTrackCount();
            for(int i = 0; i < count; i++) {
                MediaFormat format = mExtrator.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime != null && mime.startsWith("audio/")) {
                    mExtrator.selectTrack(i);
                    mCodec = MediaCodec.createDecoderByType(mime);
                    MediaCodecInfo.AudioCapabilities cap = mCodec.getCodecInfo().getCapabilitiesForType(mime).getAudioCapabilities();

                    mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    //获取当前帧的通道数
                    channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mCodec.configure(format, null, null, 0);
                    mCodec.start();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void run() {
        int buffsize = AudioTrack.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        // 创建AudioTrack对象
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffsize,
                AudioTrack.MODE_STREAM);
        //启动AudioTrack
        audioTrack.play();
        while (!eosReceived) {
            int inIndex = mCodec.dequeueInputBuffer(1000);
            if (inIndex >= 0) {
                ByteBuffer buffer = mCodec.getInputBuffer(inIndex);
                //从MediaExtractor中读取一帧待解数据
                int sampleSize = mExtrator.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    // We shouldn't stop the playback at this point, just pass the EOS
                    // flag to mCodec, we will get it again from the
                    // dequeueOutputBuffer
                    Util.logi(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                } else {
                    //向MediaDecoder输入一帧待解码数据
                    mCodec.queueInputBuffer(inIndex, 0, sampleSize, mExtrator.getSampleTime(), 0);
                    mExtrator.advance();
                }
                //从MediaDecoder队列取出一帧解码后的数据
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int outIndex = mCodec.dequeueOutputBuffer(info, 1000);
                if (outIndex >= 0) {
                    ByteBuffer outBuffer = mCodec.getOutputBuffer(outIndex);
                    //Log.v(TAG, "outBuffer: " + outBuffer);

                    final byte[] chunk = new byte[info.size];
                    // Read the buffer all at once
                    outBuffer.get(chunk);
                    //清空buffer,否则下一次得到的还会得到同样的buffer
                    outBuffer.clear();
                    // AudioTrack write data
                    audioTrack.write(chunk, info.offset, info.offset + info.size);
                    mCodec.releaseOutputBuffer(outIndex, false);
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Subsequent data will conform to new format.
                    MediaFormat format = mCodec.getOutputFormat();
                    Util.logi(TAG, "New format " + format);
                    audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                }


                // 所有帧都解码、播放完之后退出循环
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Util.logi(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
        }

        //释放MediaDecoder资源
        mCodec.stop();
        mCodec.release();
        mCodec = null;

        //释放MediaExtractor资源
        mExtrator.release();
        mExtrator = null;

        //释放AudioTrack资源
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;

    }

    @Override
    public void finalize() {
        eosReceived = true;
    }
}
