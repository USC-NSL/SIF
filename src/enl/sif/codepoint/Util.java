package enl.sif.codepoint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class Util {
	public static void log(String s) {
		// System.out.println("INFO: " + s);
		System.out.println(s);
	}

	public static void err(String s) {
		System.err.println("ERR: " + s);
	}

	public static JavaClass loadJavaClass(String fn) {
		JavaClass jcls = null;

		try {
			jcls = new ClassParser(fn).parse();
		} catch (ClassFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		assert jcls != null : "JavaClass is NULL";

		return jcls;
	}

	public static void writeToFile(String path_fn, Object o) {
		try {
			File file = new File(path_fn);
			File pf = file.getParentFile();
			if (!pf.exists())
				pf.mkdirs();

			OutputStream ops = new FileOutputStream(file);
			OutputStream buffer = new BufferedOutputStream(ops);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(o);
			} finally {
				output.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Util.err("Util.writeToFile() failed for " + path_fn);
		}
	}

	public static Object readFromFile(String path_fn) {
		Object ret = null;
		try {
			InputStream file = new FileInputStream(path_fn);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				ret = input.readObject();
			} finally {
				input.close();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			Util.err("Util.readFromFile() failed for " + path_fn);
		}
		return ret;
	}
}
