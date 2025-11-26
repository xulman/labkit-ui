package demo.custom_segmenter;

import sc.fiji.labkit.ui.utils.RemoteSegmenterCommunications;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class RemoteSegmenterCommunication {

	public static void main(String[] args) {
		final String serverURL = "http://localhost:7999";

		try {
			System.out.println(RemoteSegmenterCommunications.listAvailableNetworks(serverURL) );
			testRoundTripTime(serverURL, 102400);
			RemoteSegmenterCommunications.testRoundTripTime(serverURL, 102400);
		} catch (Exception e) {
			System.out.println("GOT AN ERROR:");
			System.out.println(e.getMessage());
		}
	}


	public static void testRoundTripTime(final String serverURL, final int packetSize)
			throws IOException, InterruptedException {
		final String serverCMD = "/connection_test/data_transfer_times";
		URL url = new URL(serverURL+serverCMD);
		HttpURLConnection comm = (HttpURLConnection)url.openConnection();
		comm.setRequestMethod("POST");
		comm.setRequestProperty("Content-Type","application/octet-stream"); //to prevent from 415 err code (Unsupported Media Type)
		comm.setDoOutput(true);
		comm.connect();

		long startMillis = System.currentTimeMillis();

		byte[] outBuf = new byte[packetSize];
		new Random().nextBytes(outBuf);

		OutputStream outputStream = comm.getOutputStream();
		outputStream.write(outBuf);
		outputStream.close();

		byte[] inBuf = new byte[packetSize];
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
