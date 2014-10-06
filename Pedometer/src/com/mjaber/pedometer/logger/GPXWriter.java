package com.mjaber.pedometer.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class GPXWriter {
	
	private static final String TAG = "DataLogger";
	
	private static XmlSerializer xmlSerializer;
	private static StringWriter writer;
	private static SimpleDateFormat sdf;
	
	private static void writeHeaders(){
		
		Log.d(TAG, "Called writeHeaders");
		
		xmlSerializer = Xml.newSerializer();
		sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
		
		try {
			xmlSerializer.setOutput(writer);
			// start DOCUMENT
			xmlSerializer.startDocument("UTF-8", null);
			// open tag: <kml>
			xmlSerializer.startTag("","gpx");
			// add attribute tag: <xmlns>
			xmlSerializer.attribute("", "version", "1.1");
			// add attribute tag: <xmlns>
			xmlSerializer.attribute("", "creator", "Pedometer"); 
			// add attribute tag: <xmlns>
			xmlSerializer.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); 
			// add attribute tag: <xmlns>
			xmlSerializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
			// add attribute tag: <xmlns>
			xmlSerializer.attribute("", "xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"); 
			// open tag: <metadata>
			xmlSerializer.startTag("","metadata");
			// open tag: <time>
			xmlSerializer.startTag("","time");
			// add entry in time
			xmlSerializer.text(sdf.format(System.currentTimeMillis()).toString());
			// end tag: </time> 
			xmlSerializer.endTag("","time");
			// end tag: </,metadata> 
			xmlSerializer.endTag("","metadata");
			// open tag: <Folder>
			xmlSerializer.startTag("","trk");
			// open tag: <name>
			xmlSerializer.startTag("","trkseg");
			
		}catch(Exception e){
			Log.e(TAG, "Exception when creating XML:1 ");
		}
	}
	
	private static void writeLocationData(double longitute, double latitude, double altitude,long time){
		try{
			// open tag: <trkpt>	
			xmlSerializer.startTag("","trkpt");
			xmlSerializer.attribute("", "lat", Double.toString(latitude));
			xmlSerializer.attribute("", "lon", Double.toString(longitute));
				
			// open tag: <ele>
			xmlSerializer.startTag("","ele");
			// add entry Timestamp as name of point
			xmlSerializer.text(Double.toString(altitude));
			// end tag: </ele> 
			xmlSerializer.endTag("","ele");
			
			// open tag: <time>
			xmlSerializer.startTag("","time");
			// add entry Timestamp as name of point
			xmlSerializer.text(sdf.format(time).toString());
			// end tag: </time> 
			xmlSerializer.endTag("","time");
	
			//end tag: </trkpt>
			xmlSerializer.endTag("","trkpt");
			
		}catch(Exception e){
			Log.e(TAG, "Exception when creating XML:1 ");
			e.printStackTrace();
		}
	}
	
	private static void writeClosingHeaders(){
		
		Log.d(TAG, "Called writeClosingHeaders");
		
		try{
			//end tag: </Folder>
			xmlSerializer.endTag("","trkseg");
			//end tag: </Document>
			xmlSerializer.endTag("","trk");
			// close tag: </kml>
			xmlSerializer.endTag("", "gpx");
			// end DOCUMENT
			xmlSerializer.endDocument();
		}catch(Exception e){
			Log.e(TAG, "Exception when creating XML:1 ");
		}
	}
	
	public static synchronized void writeFile(List<LocationData> locations){
		
		writer = new StringWriter();
		
		writeHeaders();
		
		for(LocationData location : locations){
			writeLocationData(location.getLongitute(), location.getLatitute(),
					location.getAltitude(), location.getTime());
		}
		
		writeClosingHeaders();
		
		//Create external File
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath(), "PedometerLogger");
		
		if(!dir.exists())
			dir.mkdir();
		
		if(sdCard.canWrite()){
			File gpxFile= new File(dir, "gpxFile"+System.currentTimeMillis()+".gpx");
			FileWriter gpxWriter = null;
			try {
				gpxWriter = new FileWriter(gpxFile);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "File write failed: " + e.toString());
			}
			BufferedWriter out = new BufferedWriter(gpxWriter);
			try {
				out.write(writer.toString());
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "File write failed: " + e.toString());
			}finally{
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "File write failed: " + e.toString());
				}
			}
			
		}
		
		
	}
	
}
