package enl.sif.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

public class MyLogger {
	private static final String TAG = "MyLogger";

	// public static Boolean flag = false;

	//	public static void start() {
	//		synchronized (flag) {
	//			if (flag) {
	//				return;
	//			} else {
	//				flag = true;
	//			}
	//		}

	// // use case 1. record audio in background
	//		new Thread() {
	//			public void run() {
	//				Log.v(TAG, "start recording: " + System.currentTimeMillis());
	//				record_audio();
	//				Log.v(TAG, "stop recording: " + System.currentTimeMillis());
	//			}
	//		}.start();
	//	}

	// ==================================================================
	// Permission Leakage Study
	// ==================================================================
	public static void start(Object obj) {
		//		synchronized (flag) {
		//			if (flag) {
		//				return;
		//			} else {
		//				flag = true;
		//			}
		//		}

		//		System.out.println("MyLogger: start()" + obj.getClass());

		Activity act = (Activity) obj;

		//		ContentResolver cr = act.getContentResolver();
		//
		//		// use case 2: grab contact name and phone
		//		Cursor cur = cr.query(Phone.CONTENT_URI, null, null, null, null);
		//		if (cur.getCount() > 0) {
		//			while (cur.moveToNext()) {
		//				String name = cur.getString(cur.getColumnIndex(Phone.DISPLAY_NAME));
		//				String phone = cur.getString(cur.getColumnIndex(Phone.NUMBER));
		//				Log.v(TAG, name + ": " + phone);
		//			}
		//		}

		Context context = act.getApplicationContext();
		// use case 3: take photo every 10 seconds
		new Timer().schedule(new PeriodicPhotoTakingTask(context), 0, 10000);
	}

	//	private static void record_audio() {
	//		MediaRecorder recorder = new MediaRecorder();
	//		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	//		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	//		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	//		recorder.setOutputFile("/sdcard/afa.3gpp");
	//
	//		try {
	//			recorder.prepare();
	//		} catch (IllegalStateException e) {
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//		recorder.start(); // Recording is now started
	//
	//		try {
	//			Thread.sleep(30000);
	//		} catch (InterruptedException e) {
	//			e.printStackTrace();
	//		}
	//
	//		recorder.stop();
	//		recorder.reset(); // You can reuse the object by going back to setAudioSource() step
	//		recorder.release(); // Now the object cannot be reused
	//	}

	//	public static void log() {
	//		Log.v(TAG, "log()" + " " + System.currentTimeMillis());
	//	}
	//
	//	public static void log(int mid, int bcos) {
	//		Log.v(TAG, "log()" + " " + System.currentTimeMillis() + "," + mid + "," + bcos);
	//	}
	//
	//	public static void log(HttpUriRequest req) {
	//		Log.v(TAG, req.getURI().toString());
	//	}

	// ==================================================================
	// Timing case
	// ==================================================================
	private static Map<String, Long> map = new HashMap<String, Long>();

	public static void logEntry(int mid, int pos) {
		long id = Thread.currentThread().getId();
		String s = mid + "," + pos + "," + id;
		long start = System.nanoTime();
		map.put(s, start);
	}

	public static void logExit(int mid, int pos) {
		long end = System.nanoTime();
		long id = Thread.currentThread().getId();
		String s = mid + "," + pos + "," + id;
		long start = map.get(s);
		Log.v(TAG, s + "," + (end - start));
	}

	// ==================================================================
	// Fine-Grain Permission case
	// ==================================================================
	private static Activity act;
	private static short allow = -1;

	public static void interceptActivity(Object obj) {
		//		synchronized (flag) {
		//			if (flag) {
		//				return;
		//			} else {
		//				flag = true;
		//			}
		//		}

		// System.out.println("MyLogger: interceptAct()" + obj.getClass());

		act = (Activity) obj;
		// Toast.makeText(act, "hacked", Toast.LENGTH_LONG).show();
		create_dialog(act);
	}

	private static void create_dialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setCancelable(true);
		builder.setTitle("Allowing com.flurry for Internet?");
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				allow = 1;
				dialog.dismiss();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				allow = 0;
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	public static HttpResponse checkPerm(Object obj, HttpUriRequest request) throws Exception {
		if (allow < 0) {
			create_dialog(act);
		}

		if (allow > 0) {
			HttpClient client = (HttpClient) obj;
			return client.execute(request);
		} else {
			return null;
		}
	}

	// ==================================================================
	// Ad cleaner case
	// ==================================================================
	// don't add AdView to UI
	public static void addViewStub(View v) {
	}

	// do nothing for ad loading
	public static void loadAdStub(Object obj) {
		// Log.v(TAG, "loadAdStub: " + obj.toString());
	}

	// ==================================================================
	// Flurry-list user demographics case
	// ==================================================================
	private static UUID uid;
	private static HttpClient client = new DefaultHttpClient();

	public static void generateID() {
		//		synchronized (flag) {
		//			if (flag) {
		//				return;
		//			} else {
		//				flag = true;
		//			}
		//		}

		uid = UUID.randomUUID();
	}

	public static void logStat(int mid) {
		// Log.v(TAG, "logStat " + mid);

		HttpPost httppost = new HttpPost("http://enl.usc.edu/~haos/post.php");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		nameValuePairs.add(new BasicNameValuePair("uid", uid.toString()));
		nameValuePairs.add(new BasicNameValuePair("ts", System.currentTimeMillis() + ""));
		nameValuePairs.add(new BasicNameValuePair("mid", mid + ""));

		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			client.execute(httppost);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}