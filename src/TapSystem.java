import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.lang.reflect.Field;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.*;
import org.json.simple.parser.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;

public class TapSystem {

	private HashMap<Long, tap> previousTaps = new HashMap<Long, tap>(); //Storing the previous taps in a hashmap, Normally a database will handle this. 

	public TapSystem() {
		this.previousTaps = new HashMap<Long, tap>();
	}

	public JSONArray addTapOn(JSONArray tripsArray,String pan, Date taptime, String busid, String companyid, String stopid, TapType taptype) {
		
		tap currentTap = new tap(taptime, busid, companyid, stopid, taptype); //Creating a tap object
		pan = pan.trim();
		long PAN = Long.parseLong(pan);
		if (this.previousTaps.containsKey(PAN)) { //If there is a previous tap with the PAN completes the trip, otherwises adds to hashmap
			tripsArray = completeTrip(PAN, currentTap, tripsArray);
		} else {
			this.previousTaps.put(PAN, currentTap);
		}
		
		return tripsArray;
		
	}

	public JSONArray completeTrip(long PAN, tap currenttap, JSONArray arr) {
		JSONObject trip = new JSONObject();
		tap previoustap = this.previousTaps.get(PAN); //Getting the previous tap from the hashmap
		
		long duration = currenttap.getTapDate().getTime() - previoustap.getTapDate().getTime();
		duration = duration / 1000; //Obtaining duration in seconds
		this.previousTaps.remove(PAN); //Removing the previous tap according to PAN
		if (previoustap.getTapType() == TapType.ON && currenttap.getTapType() == TapType.OFF) { //Condition to determine if trip is complete or not. 

			if (duration < 200 && previoustap.getBusId().equals(currenttap.getBusId()) //If the bus id of the destination and orgin matches, it is a complete trip. 
					&& previoustap.getStopId().equals(currenttap.getStopId())) { //Trip is cancelled, if the user cancels within 3 minutes
				trip = fillJsonOutput(previoustap, currenttap, TripStatus.CANCELLED, duration, PAN);
			} else {
				trip = fillJsonOutput(previoustap, currenttap, TripStatus.COMPLETE, duration, PAN);
			}

		} else if (previoustap.getTapType() == TapType.ON && currenttap.getTapType() == TapType.ON) {
			trip = fillJsonOutput(previoustap, previoustap, TripStatus.INCOMPLETE, duration, PAN);
			this.previousTaps.put(PAN, currenttap); //If the trip is incomplete, adds the current to the previous taps. 
		}
		
		arr.add(trip); //Adds to the output array. 
		return arr;
	}

	private JSONObject fillJsonOutput(tap previoustap, tap currenttap, TripStatus tripstatus, long duration, long PAN) {
		JSONObject output = new JSONObject(); //Creating output JSON object, simple dateformat.
	
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String started = formatter.format(previoustap.getTapDate());

		String finaldestination = "";
		String finished = "";
		double chargeamount = 0;

		String status = "";
		switch (tripstatus) { //Assigning outputs based on trip conditons
		case COMPLETE:
			chargeamount = returnCompleteTrip(previoustap.getStopId(), currenttap.getStopId());
			status = "COMPLETED"; 
			finaldestination = currenttap.getStopId(); //If the trip is complete, gets the fare and sets the destination to the current stop.
			finished = formatter.format(currenttap.getTapDate());
			break;
		case INCOMPLETE:
			chargeamount = returnIncompleteTrip(previoustap.getStopId());
			status = "INCOMPLETED"; //If the trip is incomplete, the destination and finished time is left blank. As the trip is incomplete 

			break;
		case CANCELLED:
			status = "CANCELLED"; 
			finaldestination = currenttap.getStopId();
			finished = formatter.format(currenttap.getTapDate());
			break;
		}

	

		String pan = Long.toString(PAN);
		output.put("started", started);  //Assigning variables to JSON fields. 
		output.put("finished", finished);
		output.put("durationSecs", duration);
		output.put("fromStopId", previoustap.getStopId());
		output.put("toStopId", finaldestination);
		output.put("chargeAmount", chargeamount);
		output.put("companyId", currenttap.getCompanyId());
		output.put("busId", currenttap.getBusId());
		output.put("primaryAccountNumber", pan);
		output.put("status", status);
		return output;

	}

	private double returnIncompleteTrip(String stopId) {

		double returnValue = 0;

		if (stopId.equals("Stop1")) { //Returns highest price of incomplete trips. 
			returnValue = 7.30;
		} else if (stopId.equals("Stop2")) {
			returnValue = 5.50;
		} else if (stopId.equals("Stop3")) {
			returnValue = 7.30;
		}

		return returnValue;

	}

	private double returnCompleteTrip(String originStopId, String destinationStopId) {
		double returnValue = 0;
		if (originStopId.equals("Stop1") && destinationStopId.equals("Stop2")) { //Returns price of the trip, based on destination and origin
			returnValue = 3.25;
		} else if (originStopId.equals("Stop2") && destinationStopId.equals("Stop1")) {
			returnValue = 3.25;
		} else if (originStopId.equals("Stop1") && destinationStopId.equals("Stop3")) {
			returnValue = 7.30;
		} else if (originStopId.equals("Stop3") && destinationStopId.equals("Stop1")) {
			returnValue = 7.30;
		} else if (originStopId.equals("Stop3") && destinationStopId.equals("Stop2")) {
			returnValue = 5.50;
		} else {
			returnValue = 5.50;
		}
		return returnValue;
	}

	public void readjson() {
		JSONArray tripsArray = new JSONArray(); // Array to store the trips.
		Date currentTime=null;
		try (JsonParser jparser = new JsonFactory().createParser(new File("src/taps.json"))) {

			while (jparser.nextToken() != JsonToken.END_OBJECT) {
				String fieldname = "taps"; // Used to access the taps array in JSON input
				if ("taps".equals(fieldname)) {
					jparser.nextToken(); // Next token method to shift through the JSON file
					while (jparser.nextToken() != JsonToken.END_ARRAY) { // if not END of array, parse tap objects
						if (jparser.currentToken() == JsonToken.START_OBJECT) {
							String busid = ""; // Initalizing string variables
							String companyid = "";
							String stopid = "";
							String taptypestring = "";
							String taptimestring = "";
							String pan = "";
							while (jparser.nextToken() != JsonToken.END_OBJECT) {
								String paramname = jparser.getCurrentName(); // Gets the current field name
								if ("datetimeUTC".equals(paramname)) { // Assigning the JSON data into the strings
									jparser.nextToken();
									taptimestring = jparser.getText();
								}
								if ("tapType".equals(paramname)) {
									jparser.nextToken();
									taptypestring = jparser.getText();
								}
								if ("stopId".equals(paramname)) {
									jparser.nextToken();
									stopid = jparser.getText();
								}
								if ("companyId".equals(paramname)) {
									jparser.nextToken();
									companyid = jparser.getText();
								}
								if ("busId".equals(paramname)) {
									jparser.nextToken();
									busid = jparser.getText();
								}
								if ("primaryAccountNumber".equals(paramname)) {
									jparser.nextToken();
									pan = jparser.getText();
								}

							}

							TapType taptype = TapType.ON; //Assigning the taptype string into an Enum. 
							switch (taptypestring) {
							case "OFF":
								taptype = TapType.OFF;
								break;
							}
							SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
							Date taptime = formatter.parse(taptimestring);
							currentTime = taptime; //Assuming a linear sequence, sets the current time to the latest tap object
							tripsArray = addTapOn(tripsArray,pan, taptime, busid, companyid, stopid, taptype);
						}
					}
				}
			}
			
			tripsArray = writeRemainingIncompleteTrips(tripsArray, currentTime);
			
			writeJson(tripsArray);

			

		} catch (FileNotFoundException e) { //Exception handling
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeJson(JSONArray tripsArray) {
		try {
			JSONObject outputObject = new JSONObject(); //Creating output object that will be printed to the JSON file
			ObjectMapper mapper = new ObjectMapper();

			outputObject.put("trips", tripsArray); 
			String outputstring = mapper.defaultPrettyPrintingWriter().writeValueAsString(outputObject);
			FileWriter fw = new FileWriter("src/trips.json", false); //Auto formatting JSON
			fw.write(outputstring);
			fw.flush();
			
		} catch (FileNotFoundException e) { //Exception handling
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public JSONArray writeRemainingIncompleteTrips(JSONArray tripsArray, Date currentTime) {
		
		for(long pan : this.previousTaps.keySet()) { //Obtains the Single ON Taps and assigns them as incomplete. From the sixth assumption
			tap incompleteTap = this.previousTaps.get(pan); 
			long duration = currentTime.getTime() - incompleteTap.getTapDate().getTime(); // THe current time is the latest in the JSON that has been read.
			duration = duration/1000;
			if (incompleteTap.getTapType() == TapType.ON) {   
				JSONObject incompletetripObject =  fillJsonOutput(incompleteTap, incompleteTap, TripStatus.INCOMPLETE, duration, pan);
				tripsArray.add(incompletetripObject);
			}
			
		}
		
		return tripsArray;
	}
	
	

}
