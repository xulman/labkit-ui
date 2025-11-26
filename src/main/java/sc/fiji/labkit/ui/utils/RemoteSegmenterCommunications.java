package sc.fiji.labkit.ui.utils;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RemoteSegmenterCommunications {
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


	public static void testRoundTripTime(final String serverURL, final int packetSize)
			throws IOException {
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

		try (DataOutputStream ostream = new DataOutputStream( new BufferedOutputStream( comm.getOutputStream(), 1 << 20 ) )) {
			for (byte b : outBuf) ostream.writeByte( b );
		}

		byte[] inBuf = new byte[packetSize];

		try (DataInputStream istream = new DataInputStream( new BufferedInputStream( comm.getInputStream(), 1 << 20 ) )) {
			for (int i = 0; i < inBuf.length; ++i) inBuf[i] = istream.readByte();
		}

		long stopMillis = System.currentTimeMillis();

		int diffsCnt = 0;
		for (int i = 0; i < inBuf.length; ++i) {
			diffsCnt += (inBuf[i] != outBuf[i]) ? 1 : 0;
		}
		System.out.println("Incoming buffer differs at "+diffsCnt+" positions.");

		final float timeNeededSeconds = (stopMillis-startMillis) /1000.0f;
		System.out.println("Both up and down transfers alone took "+timeNeededSeconds+" seconds.");
		System.out.println("...that's "+(2.0f*packetSize/(timeNeededSeconds*1024.f))+" kilobytes/second transfer rate.");
	}


	public static <IT extends RealType<IT> & NativeType<IT>, MT extends IntegerType<MT> & NativeType<MT>>
	void runRemoteSegmentation2D(
			final String serverURL,
			final Img<IT> inputImg,
			final Img<MT> maskImg,
			final String methodName) throws IOException {
		if (inputImg.numDimensions() < 2 || maskImg.numDimensions() < 2 ||
				inputImg.dimension(0) != maskImg.dimension(0) ||
				inputImg.dimension(1) != maskImg.dimension(1)) {
			throw new IllegalArgumentException("Input and mask images must be at least two-dimensional"+
					" and the same in their first two dinenbsions; got "+inputImg.dimensionsAsLongArray()+
					" and "+maskImg.dimensionsAsLongArray());
		}

		final String serverCMD = "/segmentation_2D/on_posted_stream_of/"+
				inputImg.dimension(0)+"/"+inputImg.dimension(1)+"/use/"+methodName;
		URL url = new URL(serverURL+serverCMD);
		HttpURLConnection comm = (HttpURLConnection)url.openConnection();
		comm.setRequestMethod("POST");
		comm.setRequestProperty("Content-Type","application/octet-stream"); //to prevent from 415 err code (Unsupported Media Type)
		comm.setDoOutput(true);
		comm.connect();

		long startMillis = System.currentTimeMillis();

		try (DataOutputStream ostream = new DataOutputStream( new BufferedOutputStream( comm.getOutputStream(), 1 << 20 ) )) {
			Cursor<IT> c = Views.flatIterable(inputImg).cursor();
			while (c.hasNext()) ostream.writeFloat( c.next().getRealFloat() );
		}

		try (DataInputStream istream = new DataInputStream( new BufferedInputStream( comm.getInputStream(), 1 << 20 ) )) {
			Cursor<MT> c = Views.flatIterable(maskImg).cursor();
			while (c.hasNext()) c.next().setReal( istream.readUnsignedShort() );
		}

		long stopMillis = System.currentTimeMillis();
		final float timeNeededSeconds = (stopMillis-startMillis) /1000.0f;
		System.out.println("Both up and down transfers alone took "+timeNeededSeconds+" seconds.");
		final long pixelsCnt = inputImg.dimension(0) * inputImg.dimension(1);
		System.out.println("...that's "+(6.0f*pixelsCnt/(timeNeededSeconds*1024.f))+" kilobytes/second transfer rate.");
	}
}
