package pfg.com.mediacodecproc;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener{

    private String TAG = "MainActivity";
    MediaCodec mCodec;
    String filePath = Environment.getExternalStorageDirectory()+"/"+"iso_1280x720.mkv";

    private VideoDecoderThread mVideoDecoderThread;
    private AudioDecoderThread mAudioDecoderThread;
    private DecEncCtsCaseThread mDecEncCtsCaseThread;

    private ScreenRecordThread mScreenRecordThread;
    private ScreenShotThread mScreenShotThread;

    private Button btn_record;
    private Button btn_shot;
    MediaProjectionManager manager;
    private final int REQUEST_CODE_SCREENRECORD = 1;
    private final int REQUEST_CODE_SCREENSHOT = 2;

    public final int REQUEST_AUDIO_PERMISSION_RESULT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        SurfaceView view = (SurfaceView) findViewById(R.id.surfaceview);
        view.getHolder().addCallback(this);
        mVideoDecoderThread = new VideoDecoderThread();
        mAudioDecoderThread = new AudioDecoderThread();
        mDecEncCtsCaseThread = new DecEncCtsCaseThread();
        btn_record = (Button)findViewById(R.id.btn_start_record);
        btn_record.setOnClickListener(this);

        btn_shot = (Button)findViewById(R.id.btn_start_shot);
        btn_shot.setOnClickListener(this);
        manager =(MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if(mVideoDecoderThread.init(surfaceHolder, filePath)) {
            mVideoDecoderThread.start();
        }

        if(mAudioDecoderThread.init(surfaceHolder, filePath)) {
            mAudioDecoderThread.start();
        }
        /*try {
            PermisionUtils.verifyStoragePermissions(this);
            mDecEncCtsCaseThread.testEncodeDecodeVideoFromBufferToBuffer720p();
            mDecEncCtsCaseThread.testEncodeDecodeVideoFromBufferToSurface720p();
            mDecEncCtsCaseThread.testEncodeDecodeVideoFromSurfaceToSurface720p();

        } catch (Throwable e) {
            e.printStackTrace();
        }*/



    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mVideoDecoderThread.finalize();
        mAudioDecoderThread.finalize();
        mScreenRecordThread.release();
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_start_record) {
            Intent intent = manager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE_SCREENRECORD);
            PermisionUtils.verifyAudioRecordPermissions(this);
        } else if(view.getId() == R.id.btn_start_shot){
            Intent intent = manager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE_SCREENSHOT);
            PermisionUtils.verifyAudioRecordPermissions(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Util.logi(TAG,"onActivityResult requestCode:"+requestCode+" resultCode:"+resultCode+" data:"+data);
        if(Activity.RESULT_OK == resultCode && REQUEST_CODE_SCREENRECORD == requestCode) {
            MediaProjection projection = manager.getMediaProjection(resultCode, data);
            mScreenRecordThread = new ScreenRecordThread(projection);
            mScreenRecordThread.start();
        } else if (Activity.RESULT_OK == resultCode && REQUEST_CODE_SCREENSHOT == requestCode) {
            MediaProjection projection = manager.getMediaProjection(resultCode, data);
            mScreenShotThread = new ScreenShotThread(this, projection);
            mScreenShotThread.init();
            mScreenShotThread.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
