package d2d.testing.gui;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.Toolbar;


import d2d.testing.gui.main.MainFragment;
import d2d.testing.gui.main.dialogName.CustomDialogFragment;
import d2d.testing.gui.main.dialogName.CustomDialogListener;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.sessions.SessionBuilder;
import d2d.testing.streaming.video.CameraController;
import d2d.testing.streaming.video.VideoPacketizerDispatcher;
import d2d.testing.streaming.video.VideoQuality;
import d2d.testing.utils.IOUtils;

import d2d.testing.R;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class StreamActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, CameraController.Callback, CustomDialogListener {

    private final static String TAG = "StreamActivity";

    private AutoFitTextureView mTextureView;

    private SessionBuilder mSessionBuilder;

    private FloatingActionButton recordButton;
    public boolean mRecording = false;

    private String mNameStreaming = "defaultName";
    private VideoQuality mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;

    private boolean isDownload;
    CameraController ctrl;

    private SaveStream saveStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(MainActivity.mode.equals(getString(R.string.mode_humanitarian))){
            CustomDialogFragment dialog = new CustomDialogFragment();
            dialog.show(getSupportFragmentManager(), "CustomDialogFragment");
        }
        else {
            putAutorInDefaultName();
        }

        mTextureView = findViewById(R.id.textureView);

        // Configures the SessionBuilder
        mSessionBuilder = SessionBuilder.getInstance()
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(mVideoQuality);

        mTextureView.setSurfaceTextureListener(this);
        //mSurfaceView.getHolder().addCallback(this);

        recordButton = findViewById(R.id.button_capture);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mRecording) {
                    startStreaming();
                } else {
                    stopStreaming();
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public void startStreaming() {
        final UUID localStreamUUID = UUID.randomUUID();
        StreamingRecord.getInstance().addLocalStreaming(localStreamUUID, mNameStreaming, mSessionBuilder);

        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop));
        mRecording = true;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        isDownload = preferences.getBoolean("saveMyStreaming", false);
        if(isDownload) {
            saveStream = new SaveStream(getApplicationContext(), localStreamUUID.toString());
            saveStream.startDownload();
        }
    }

    private void stopStreaming() {
        StreamingRecord.getInstance().removeLocalStreaming();
        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.videocam));
        mRecording = false;
        if(isDownload) saveStream.stopDownload();
        Toast.makeText(this,"Stopped retransmitting the streaming", Toast.LENGTH_SHORT).show();
    }

    public void onDestroy(){
        if(mRecording) {
            stopStreaming();
        }
        VideoPacketizerDispatcher.stop();
        CameraController.getInstance().stopCamera();

        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        ctrl = CameraController.getInstance();
        List<Surface> surfaces = new ArrayList<>();
        String cameraId = ctrl.getCameraIdList()[0];
        Size[] resolutions = ctrl.getPrivType_2Target_MaxResolutions(cameraId, SurfaceTexture.class, MediaCodec.class);

        mTextureView.setAspectRatio(resolutions[0].getWidth(), resolutions[0].getHeight());
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(resolutions[0].getWidth(), resolutions[0].getHeight());
        Surface surfaceT = new Surface(surfaceTexture);
        surfaces.add(surfaceT);

        try {
            VideoPacketizerDispatcher.start(PreferenceManager.getDefaultSharedPreferences(this), mVideoQuality);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "No se pudo iniciar la grabacion", Toast.LENGTH_LONG).show();
        }
        surfaces.add(VideoPacketizerDispatcher.getEncoderInputSurface());

        ctrl.startCamera(cameraId, surfaces);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    public void cameraStarted() {

    }

    @Override
    public void cameraError(int error) {
        Toast.makeText(this, "Camera error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void cameraError(Exception ex) {
        Toast.makeText(this, "Camera error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void cameraClosed() {
        //Toast.makeText(this, "Camera closed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAction(Object object) {

    }

    @Override
    public void onDialogPositive(Object object) {
        String name = (String)object;
        String author = getIntent().getStringExtra("author");

        name = name.replaceAll("\\s+$", "");
        name = name.replaceAll("\\s+", "_");

        author = author.replaceAll("\\s+$", "");
        author = author.replaceAll("\\s+", "_");

        mNameStreaming = name + "__" + author;
        //Toast.makeText(getApplicationContext(), mNameStreaming, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onDialogNegative(Object object) {
        putAutorInDefaultName();

    }

    private void putAutorInDefaultName(){
        String author = getIntent().getStringExtra("author");
        author = author.replaceAll("\\s+$", "");
        author = author.replaceAll("\\s+", "_");
        mNameStreaming = mNameStreaming +  "__" + author;
    }
}