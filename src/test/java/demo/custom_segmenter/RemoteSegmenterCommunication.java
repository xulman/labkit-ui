package demo.custom_segmenter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RemoteSegmenterCommunication {

	public static class ListAvailableMethods {
		public String status;
		public List<String> available_network_names;
	}

	/**
	 * @param serverURL Make sure the URL starts with 'http://'.
	 * @return The list, or null if there was any problem.
	 */
	public static List<String> listAvailableNetworks(final String serverURL) {
		final ObjectMapper mapper = new ObjectMapper();
		ListAvailableMethods methods = null;
		try {
			methods = mapper.readValue(
					new URL(serverURL+"/segmentation_2D/list_available_methods"),
					ListAvailableMethods.class );
		} catch (IOException e) {
			System.out.println("SEGMENTATION SERVER COMMUNICATION ERROR: "+e.getMessage());
			return Collections.emptyList();
		}
		if (methods == null || !methods.status.equals("OK")) return Collections.emptyList();

		return methods.available_network_names;
	}

	public static void testRoundTripTime(final String serverURL) throws IOException, InterruptedException {
		final String serverCMD = "/connection_test/data_transfer_times";
		URL url = new URL(serverURL+serverCMD);
		HttpURLConnection comm = (HttpURLConnection)url.openConnection();
		comm.setRequestMethod("POST");
		comm.setRequestProperty("Content-Type","application/octet-stream"); //to prevent from 415 err code (Unsupported Media Type)
		comm.setDoOutput(true);
		comm.connect();

		long startMillis = System.currentTimeMillis();

		final int BUF_SIZE = 102400;
		byte[] outBuf = new byte[BUF_SIZE];
		new Random().nextBytes(outBuf);

		OutputStream outputStream = comm.getOutputStream();
		outputStream.write(outBuf);
		outputStream.close();

		byte[] inBuf = new byte[BUF_SIZE];
		int inBufPos = 0;

		InputStream inputStream = comm.getInputStream();
		while (inBufPos < inBuf.length) {
			//NB: note that if, for whatever reason, the expected bytes would not
			//    arrive (and the while-loop may thus seem to be iterating forever),
			//    the busyWait_() below will detect a prolonged communication delay
			//    and would throw an exception, which will exit this while-loop
			busyWaitOrThrowOnTimeOut(inputStream);

			//NB: 2nd param says offset in the 1st param from which to start saving
			//NB: 3rd param says how much we're willing to accept now
			int actualArrivedSize = inputStream.read(inBuf, inBufPos, inBuf.length - inBufPos);
			inBufPos += actualArrivedSize;
			System.out.println("received now: "+actualArrivedSize);
			System.out.println("filling stopped at: "+inBufPos);
		}
		inputStream.close();

		long stopMillis = System.currentTimeMillis();

		if (inBufPos != inBuf.length) {
			System.out.println("Only "+inBufPos+" bytes came back, expected was "+inBuf.length+" bytes.");
		} else {
			System.out.println("All "+inBufPos+" bytes came back, good.");
		}

		int diffsCnt = 0;
		for (int i = 0; i < inBuf.length; ++i) {
			diffsCnt += (inBuf[i] != outBuf[i]) ? 1 : 0;
		}
		System.out.println("Incoming buffer differs at "+diffsCnt+" positions.");

		final float timeNeededSeconds = (stopMillis-startMillis) /1000.0f;
		System.out.println("Both up and down transfers alone took "+timeNeededSeconds+" seconds.");
		System.out.println("...that's "+(2.0f*inBufPos/(timeNeededSeconds*1024.f))+" kilobytes/second transfer rate.");
	}


	public static void main(String[] args) {
		final String serverURL = "http://localhost:8000";

		try {
			testRoundTripTime(serverURL);
		} catch (Exception e) {
			System.out.println("GOT AN ERROR:");
			System.out.println(e.getMessage());
		}
	}


	private static void busyWaitOrThrowOnTimeOut(final InputStream dataSrc)
			throws IOException, InterruptedException {
		int tries = 0;
		long waitTime = 20;

		while (dataSrc.available() == 0 && tries < 10) {
			//System.out.println((tries+1)+". waiting for "+waitTime);
			Thread.sleep(waitTime);
			waitTime += 0.84*waitTime; //...nearly doubling-waiting time
			++tries;
		}

		//System.out.println("Done with the waiting....");
		if (dataSrc.available() == 0)
			throw new IOException("Gave up waiting for incoming data");
	}
}
