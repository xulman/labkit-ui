package net.imglib2.atlas;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

/*
 * @author Matthias Arzt
 */
public class AtlasUtils {
	static long[] extend(long[] array, long value) {
		int length = array.length;
		long[] result = new long[length + 1];
		System.arraycopy(array, 0, result, 0, length);
		result[length] = value;
		return result;
	}

	static int[] extend(int[] array, int value) {
		int length = array.length;
		int[] result = new int[length + 1];
		System.arraycopy(array, 0, result, 0, length);
		result[length] = value;
		return result;
	}

	public static <R,T> R uncheckedCast(T value) {
		@SuppressWarnings("unchecked") R r = (R) value;
		return r;
	}
}
