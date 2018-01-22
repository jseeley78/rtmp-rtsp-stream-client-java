package com.pedro.rtpstreamer.openglexample;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pedro.encoder.utils.gl.GifStreamObject;
import com.pedro.encoder.utils.gl.ImageStreamObject;
import com.pedro.encoder.utils.gl.TextStreamObject;
import com.pedro.rtpstreamer.R;
import com.pedro.rtplibrary.rtmp.RtmpCamera2;
import com.pedro.rtplibrary.view.OpenGlView;
import java.io.IOException;
import net.ossrs.rtmp.ConnectCheckerRtmp;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera2Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera2}
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class OpenGlRtmpActivity extends AppCompatActivity
    implements ConnectCheckerRtmp, View.OnClickListener {

  private RtmpCamera2 rtmpCamera2;
  private Button button;
  private EditText etUrl;
  private EditText etVideoBitrate, etFps, etAudioBitrate, etSampleRate;
  private int originalVideoBitrate, currentVideoBitrate, videoBitrateRecoveryRate;
  private Handler incrBitrateHandler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_open_gl);
    OpenGlView openGlView = findViewById(R.id.surfaceView);
    button = findViewById(R.id.b_start_stop);
    button.setOnClickListener(this);

    etVideoBitrate = findViewById(R.id.et_video_bitrate);
//    etFps = findViewById(R.id.et_fps);
//    etAudioBitrate = findViewById(R.id.et_audio_bitrate);
//    etSampleRate = findViewById(R.id.et_samplerate);
    etVideoBitrate.setText("2500");
    originalVideoBitrate = Integer.parseInt(etVideoBitrate.getText().toString());
    currentVideoBitrate = originalVideoBitrate;
    videoBitrateRecoveryRate = 5000; // 5 Secs
//    etFps.setText("30");
//    etAudioBitrate.setText("128");
//    etSampleRate.setText("44100");

    etUrl = findViewById(R.id.et_rtp_url);
    etUrl.setHint(R.string.hint_rtmp);
    rtmpCamera2 = new RtmpCamera2(openGlView, this);
    // NOTE: listen for reduce bitrate events
    rtmpCamera2.setReduceBitrateListener(new RtmpCamera2.ReduceBitrateListener() {
      @Override
      public void onReduceBitrate(Boolean reduceBitrate) {
          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
              decrVideoBitrate();
          } else {
              onBitrateUpdate("ignored, Required min API 19");
          }
      }
    });
    etUrl.setText("rtmp://ids3-ls.dev.caster.tv/origin/e47830bd-c3a6-4c4a-99c4-c2614b8df9ee");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.gl_menu, menu);
      return true;
  }

  private Runnable incrVideoBitrateRunnable = new Runnable() {
      @Override
      public void run() {
          incrVideoBitrate();
          incrBitrateHandler.postDelayed(this, videoBitrateRecoveryRate);
      }
  };

  public void incrVideoBitrate() {
      int newBitrate = (int) (currentVideoBitrate * 1.05);
      if (rtmpCamera2.isStreaming() && newBitrate < originalVideoBitrate) {
          rtmpCamera2.setVideoBitrateOnFly(newBitrate * 1024);
          currentVideoBitrate = newBitrate;
          onBitrateUpdate(Integer.toString(newBitrate));
      } else {
          // NOTE: if bitrate is recovered, stop incr's
          incrBitrateHandler.removeCallbacks(incrVideoBitrateRunnable);
      }
  }

  public void decrVideoBitrate() {
      int newBitrate = (int) (currentVideoBitrate * .9);
      if (newBitrate > (int) (originalVideoBitrate * .33)) {
          // NOTE: if bitrate is reducing, clear any peding incr's
          incrBitrateHandler.removeCallbacks(incrVideoBitrateRunnable);
          rtmpCamera2.setVideoBitrateOnFly(newBitrate * 1024);
          currentVideoBitrate = newBitrate;
          onBitrateUpdate(Integer.toString(newBitrate));
          // NOTE: call to increase bitrate in 10 seconds
          incrBitrateHandler.postDelayed(incrVideoBitrateRunnable, videoBitrateRecoveryRate);
      }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (rtmpCamera2.isStreaming()) {
      switch (item.getItemId()) {
        case R.id.text:
          setTextToStream();
          return true;
        case R.id.image:
          setImageToStream();
          return true;
        case R.id.gif:
          setGifToStream();
          return true;
        case R.id.clear:
          rtmpCamera2.clearStreamObject();
          return true;
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  private void setTextToStream() {
    try {
      TextStreamObject textStreamObject = new TextStreamObject();
      textStreamObject.load("Hello world", 22, Color.RED);
      rtmpCamera2.setTextStreamObject(textStreamObject);
    } catch (IOException e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void setImageToStream() {
    try {
      ImageStreamObject imageStreamObject = new ImageStreamObject();
      imageStreamObject.load(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
      rtmpCamera2.setImageStreamObject(imageStreamObject);
    } catch (IOException e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void setGifToStream() {
    try {
      GifStreamObject gifStreamObject = new GifStreamObject();
      gifStreamObject.load(getResources().openRawResource(R.raw.banana));
      rtmpCamera2.setGifStreamObject(gifStreamObject);
    } catch (IOException e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onConnectionSuccessRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(OpenGlRtmpActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onConnectionFailedRtmp(final String reason) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(OpenGlRtmpActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
            .show();
        rtmpCamera2.stopStream();
        rtmpCamera2.stopPreview();
        button.setText(R.string.start_button);
      }
    });
  }

  @Override
  public void onDisconnectRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(OpenGlRtmpActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAuthErrorRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(OpenGlRtmpActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAuthSuccessRtmp() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(OpenGlRtmpActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
      }
    });
  }


  public void onBitrateUpdate(final String bitrate) {
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              Toast.makeText(OpenGlRtmpActivity.this, "New Bitrate " + bitrate, Toast.LENGTH_SHORT).show();
          }
      });
  }

  @Override
  public void onClick(View view) {
    if (!rtmpCamera2.isStreaming()) {
      if (rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo(1280, 720, 30, currentVideoBitrate, false, 180)) {
        button.setText(R.string.stop_button);
        rtmpCamera2.startStream(etUrl.getText().toString());
      } else {
        Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
            .show();
      }
    } else {
      button.setText(R.string.start_button);
      rtmpCamera2.stopStream();
      rtmpCamera2.stopPreview();
      currentVideoBitrate = originalVideoBitrate;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (rtmpCamera2.isStreaming()) {
      rtmpCamera2.stopStream();
      rtmpCamera2.stopPreview();
      currentVideoBitrate = originalVideoBitrate;
    }
  }
}
