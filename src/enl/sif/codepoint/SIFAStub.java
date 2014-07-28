package enl.sif.codepoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class SIFAStub {
	private final static String TAG = "SIFAStub";

	// wrapper to the reflective call: Method.invoke()
	public static Object invoke(Method mthd, Object obj, Object... args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		// show the runtime class binding
		Log.v(TAG, obj.getClass().getCanonicalName() + ":" + mthd.toString() + ": " + args.length + " args");

		// TODO: add runtime invoke filtering code here, e.g. permission map checking

		// finally, call original reflective invoke
		return mthd.invoke(obj, args);
	}
}
