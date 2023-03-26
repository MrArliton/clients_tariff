package com.nexign.tariffs.clients_tariffs;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.Duration;


class Client_Data {
	
	short call_type;
	LocalDateTime start_time;
	LocalDateTime end_time;
	short mode;
	Duration dur;
	
	
		Client_Data(short call_type, LocalDateTime start_time, LocalDateTime end_time, short mode)
		{
			this.call_type = call_type;
			this.start_time = start_time;
			this.end_time = end_time;
			dur = Duration.between(start_time, end_time);
			this.mode = mode;
		}
		
	
		short getCallType() {
			return call_type;
		}
		
		LocalDateTime getStartTime() {
			return start_time;
		}
		
		LocalDateTime getEndTime() {
			return end_time;
		}
		
		int getMode() {
			return mode;
		}
		Duration getDuration() {
			return dur;
		}

}

public class Main {
	
	//// Tariff
	private static Map<Short, double[][]> tariffs = new HashMap<Short, double[][]>();
	
	private static double[][] tariff06 = {{300,Double.MAX_VALUE},{0,1},{Double.MAX_VALUE},{0},{100},{1,0}}; // format {{timeOfAction,...}{pay,...}(outgoing),{timeOfAction,...}{pay,...}(ingoing), {start_pay}, {active_outgoing_pay,active_ingoing_pay}} 
	private static double[][] tariff03 = {{Double.MAX_VALUE},{1.5},{Double.MAX_VALUE},{0},{0},{1,0}}; // in minutes and rubles per minute
	private static double[][] tariff11 = {{100,Double.MAX_VALUE},{0.5,1.5},{Double.MAX_VALUE},{0},{0},{1,0}};
	////
	
	private static void writeClientData(String path, long phone_number ,List<Client_Data> data) {
		
		try(FileWriter file_writer = new FileWriter(path)){
			
			
			file_writer.write(String.format("Tariff index: %02d\n",data.get(0).getMode()));
			file_writer.write("----------------------------------------------------------------------------\n");
			file_writer.write(String.format("Report for phone phone_number %d:\n", phone_number));
			file_writer.write("----------------------------------------------------------------------------\n");
			file_writer.write("| Call Type |   Start Time        |     End Time        | Duration | Cost  |\n");
			file_writer.write("----------------------------------------------------------------------------\n");
			
			
			long time = 0;
			int[] i = {0,0};
			double pay = 0;
			double sum_pay = 0;
			short lev = 0; 
			
			for(Client_Data dt: data) {
				
				time += dt.getDuration().getSeconds()*tariffs.get((short)data.get(0).getMode())[5][dt.getCallType()-1];
				
				//// Work with tariffs
				if(tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2][i[dt.getCallType()-1]]*60>=time) {
					pay = tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2+1][i[dt.getCallType()-1]]*(double)(dt.getDuration().getSeconds())/60D;
					lev = 0;
				}else {
					
					i[dt.getCallType()-1]++;
					
					if(lev==0) {
					
						pay = tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2+1][i[dt.getCallType()-1]]*(time-tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2][i[dt.getCallType()-1]-1]*60)/60D;
						pay += tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2+1][i[dt.getCallType()-1]-1]*(tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2][i[dt.getCallType()-1]-1]*60-time+dt.getDuration().getSeconds())/60D;
			
						lev = dt.getCallType();
					
					}else{
					
						pay = tariffs.get((short)data.get(0).getMode())[(dt.getCallType()-1)*2+1][i[dt.getCallType()-1]]*(double)(dt.getDuration().getSeconds())/60D;
						lev = 0;
					
					}
					}
				////
				
				file_writer.write(String.format("|     %02d    |%20s |%20s | %02d:%02d:%02d |%6.2f |\n",
									dt.getCallType(),
									dt.getStartTime().toString().replaceAll("T"," "),
									dt.getEndTime().toString().replaceAll("T"," "),
									dt.getDuration().toHoursPart(),
									dt.getDuration().toMinutesPart(),
									dt.getDuration().toSecondsPart(),
									pay));
				sum_pay+=pay;
			}
			
			file_writer.write("----------------------------------------------------------------------------\n");
			
			file_writer.write(String.format("|                                           Total Cost: | %9.2f rubles |\n",sum_pay+tariffs.get((short)data.get(0).getMode())[4][0]));

			file_writer.write("----------------------------------------------------------------------------\n");
		}catch(IOException ex) {
		
			System.out.println("Error:"+ex.getMessage());
			
		}
	}
	
	public static void main(String[] args) {
		
		//add a tariffs
		tariffs.put((short)6,tariff06);
		tariffs.put((short)3,tariff03);
		tariffs.put((short)11,tariff11);
		
		
		ArrayList<Client_Data> list_clients_data = new ArrayList<Client_Data>();
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		
		
		Map<Long, List<Client_Data>> clients = new HashMap<Long,List<Client_Data>>();
		
		// read calls from file and group by phone phone_number
		try(BufferedReader reader = new BufferedReader(new FileReader(args[0])))
        {
			String raw_data = reader.readLine();
			String[] data;
            
			while(raw_data!=null){ 
				
				data = raw_data.replaceAll(" ","").split(",");
				
				long phone_number = Long.parseLong(data[1]);
				
				if(clients.containsKey(phone_number)) {
					
					clients.get(phone_number).add(new Client_Data(Short.valueOf(data[0]), // calltype
													LocalDateTime.parse(data[2],formatter), // start time
													LocalDateTime.parse(data[3],formatter), // end tim
													Short.valueOf(data[4])) // mode
					);
					
				}else {
				
					clients.put(phone_number,new ArrayList<Client_Data>());
					clients.get(phone_number).add(new Client_Data(Short.valueOf(data[0]), // calltype
													LocalDateTime.parse(data[2],formatter), // start time
													LocalDateTime.parse(data[3],formatter), // end time
													Short.valueOf(data[4])) // mode
					);
					
				}
				
				
				raw_data = reader.readLine();
            } 
        }
        catch(IOException | ArrayIndexOutOfBoundsException ex){
             
            System.out.println("Error:"+ex.getMessage());
            System.out.println("Please check arguments: 1-path to CDR 2-path to records directory");
            return;
        }
		

		Comparator<Client_Data> byStartDate = new Comparator<Client_Data>() {
			@Override
			public int compare(Client_Data d1, Client_Data d2) {
				
				return d1.getStartTime().compareTo(d2.getStartTime());
			}
		};
		
		
		new File(args[1]+"/reports").mkdir();
		
		// Sort lists by start time and write file about client
		for (Map.Entry<Long, List<Client_Data>> entry : clients.entrySet()) {
			
			Collections.sort(entry.getValue(),byStartDate);
	        
			writeClientData(args[1]+"/reports/"+entry.getKey()+".txt"
							,entry.getKey()
							,entry.getValue());
	    
		}
		
		System.out.println("The reports is created!" );
    }
}