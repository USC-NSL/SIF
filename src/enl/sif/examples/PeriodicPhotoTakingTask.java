package enl.sif.examples;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimerTask;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.util.Log;
import android.view.SurfaceView;

public class PeriodicPhotoTakingTask extends TimerTask {
	private static final String TAG = "PeriodicPhotoTakingTask";
	private SurfaceView view;
	private final AudioManager mgr;
	private Camera cam;

	private ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// Play your sound here.
		}
	};

	private PictureCallback jpegPictureCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
				outStream.write(data);
				outStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			// Log.d(TAG, "onPictureTaken - jpeg");

			// unmute the sound
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);

			// post to server
			//			HttpClient httpClient = new DefaultHttpClient();
			//			HttpPost httpPost = new HttpPost("");

			//			MultipartEntity multiPart = new MultipartEntity();
			//			multiPart.addPart("my_picture", new FileBody(new File(IMG_URL)));
			//
			//			httpPost.setEntity(multiPart);
			//			try {
			//				httpClient.execute(httpPost);
			//			} catch (ClientProtocolException e) {
			//				e.printStackTrace();
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//			}
		}
	};

	public PeriodicPhotoTakingTask(Context context) {
		view = new SurfaceView(context);
		mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		cam = Camera.open();
		try {
			cam.setPreviewDisplay(view.getHolder());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		cam.startPreview();

		// Log.d(TAG, "start taking picture");
		// mute the sound
		mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);

		cam.takePicture(shutterCallback, null, jpegPictureCallback);

		try {
			Thread.sleep(500); // IMPORTANT: w/o this, takePicture will fail
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
