/**
 * 
 */
package edu.ncsu.csc216.travel.model.file_io;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Scanner;

import edu.ncsu.csc216.travel.list_utils.SimpleArrayList;
import edu.ncsu.csc216.travel.model.office.DuplicateClientException;
import edu.ncsu.csc216.travel.model.office.DuplicateTourException;
import edu.ncsu.csc216.travel.model.office.TourCoordinator;
import edu.ncsu.csc216.travel.model.participants.Client;
import edu.ncsu.csc216.travel.model.vacation.CapacityException;
import edu.ncsu.csc216.travel.model.vacation.Tour;

/**
 * TravelReader class which is used for loading in data from a file.
 * @author dkudo
 */
public class TravelReader {
	
	private static SimpleArrayList<Client> clientList;

	/**
	 * Writes the data currently in the TourCoordinator to the given file
	 * @param filename : filename which the data would be loaded in from
	 * @throws IllegalArgumentException : when any error occurs
	 */
	public static void readTravelData(String filename) {
		clientList = new SimpleArrayList<Client>();
		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(new File(filename));
			
			String currentLine = fileScanner.nextLine().trim();
			while (currentLine.length() == 0) {
				if (!fileScanner.hasNext()) {
					break;
				}
				currentLine = fileScanner.nextLine().trim();
			}
			
			// client lines
			while (currentLine.contains("(")) {
				TravelReader.callAddNewClient(currentLine);
				
				currentLine = fileScanner.nextLine().trim();
				while (currentLine.length() == 0) {
					if (!fileScanner.hasNext()) {
						break;
					}
					currentLine = fileScanner.nextLine().trim();
				}
			}
			
			// read the iterating lines of (tour > reservations)
			for (;;) {
				
				Tour currentTour = null; 

				// if the line represents the tour name (begins with "#"),,,
				while (currentLine.charAt(0) == '#') {
					currentTour = callAddNewTour(currentLine);
					
					currentLine = fileScanner.nextLine().trim();
					while (currentLine.length() == 0) {
						if (!fileScanner.hasNext()) {
							break;
						}
						currentLine = fileScanner.nextLine().trim();
					}
				}
				
				// read reservation infos
				while (currentLine.charAt(0) != '#') {
					// read in the reservation info from lines
					callAddOldReservation(currentTour, currentLine);
					currentLine = fileScanner.nextLine().trim();
					while (currentLine.length() == 0) {
						if (!fileScanner.hasNext()) {
							break;
						}
						currentLine = fileScanner.nextLine().trim();
					}
				}
				
			}
			
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File not loaded");
		} catch (NoSuchElementException e) {
			// end of the file 
			// just finish this reading file
			fileScanner.close();
		}
	}
	
	
	/**
	 * Call TourCoordinator.addNewClient to add a Client represented by a given String line.
	 * @param clientLine : String which represents Client
	 * @throws IllegalArgumentException when a Client cannot be added, or it already exists in TourCoordinator.
	 */
	private static void callAddNewClient(String clientLine) {
		int index = clientLine.indexOf('(');
		
		// get tokens which represents name and contact
		String customer = clientLine.substring(0, index - 1);
		String contact = clientLine.substring(index + 1, clientLine.length() - 1);
		
		// add a client represented by this line
		try {
			Client newClient = TourCoordinator.getInstance().addNewClient(customer, contact);
			clientList.add(newClient);
		} catch (DuplicateClientException e) {
			throw new IllegalArgumentException("Duplicate Clients in a file.");
		}
	}
	
	
	/**
	 * Call TourCoordinator.addNewTour to add a Tour represented by a given String line.
	 * @param tourLine ; String which represents Tour
	 * @return Tour newly added
	 * @throws IllegalArgumentException when Tour cannot be added, or it already exists in TourCoordinator.
	 */
	private static Tour callAddNewTour(String tourLine) {
		// "ED" or "RC" or "LT"
		int idx1 = tourLine.indexOf('-');
		String kind = tourLine.substring(1, idx1).trim();
		int idx2 = tourLine.indexOf(':');
		// "Tour name"
		String name = tourLine.substring(idx1 + 1, idx2);
		// the rest of line signifies date, duration, cost, capacity
		Scanner tourLineScanner  = new Scanner(tourLine.substring(idx2 + 1));
		
		try {
			// "01/15/19"	
			String dateToken = tourLineScanner.next();
			int index1 = dateToken.indexOf('/');
			int index2 = dateToken.indexOf('/', index1 + 1);
			//System.out.println(""+index1+index2);
			int month = Integer.valueOf(dateToken.substring(0, index1));
			int day = Integer.valueOf(dateToken.substring(index1 + 1, index2));
			int year = Integer.valueOf(dateToken.substring(index2 + 1));
			//System.out.println(""+month+day+year);
			LocalDate date = LocalDate.of(year, month, day);
			
			// integer 10
			int duration = tourLineScanner.nextInt();
			
			// integer 200
			int cost = Integer.valueOf(tourLineScanner.next().substring(1));
			// capacity "150" or "150*"
			String capacityToken = tourLineScanner.next();
			int capacity;
			
			Tour newTour;
			// for Educational Trip WITH *
			if (capacityToken.contains("*")) {
				capacity = Integer.valueOf(capacityToken.substring(0, capacityToken.length() - 1));
				newTour = TourCoordinator.getInstance().addNewTour(kind, name, date, duration, cost, capacity);
			// for any tour WITHOUT * ()
			} else {
				capacity = Integer.valueOf(capacityToken);
				newTour = TourCoordinator.getInstance().addNewTour(kind, name, date, duration, cost, capacity);
				newTour.fixCapacity();
			}
			
			tourLineScanner.close();
			return newTour;
		} catch (DuplicateTourException e) {
			tourLineScanner.close();
			throw new IllegalArgumentException("Duplicate Tours in a file.");
		} catch (Exception e) {
			tourLineScanner.close();
			throw new IllegalArgumentException("Something is wrong in Tour line." + tourLine);
		}
	}
	
	
	/**
	 * Call TourCoordinator.addOldReservation() to add a Reservation represented by a given Tour and String line.
	 * @param tour Tour of the Reservation
	 * @param reservationLine String which represents Reservation
	 * @throws IllegalArgumentException when a Reservation cannot be added, or it causes capacity over.
	 */
	private static void callAddOldReservation(Tour tour, String reservationLine) {
		Scanner reservationLineScanner = new Scanner(reservationLine);
		try {
			int code = Integer.valueOf(reservationLineScanner.next());
			int numInParty = Integer.valueOf(reservationLineScanner.next());
			
			String userAndContact = reservationLineScanner.nextLine().trim();
			
			int index1 = userAndContact.indexOf('(');
			int index2 = userAndContact.indexOf(')');
			
			String user = userAndContact.substring(0, index1 - 1).trim();
			String contact = userAndContact.substring(index1 + 1, index2).trim();
			
			reservationLineScanner.close();
			
			//get the Tour object corresponds to the current Tour
			Client theClient = null;
			
		
			for (int i = 0 ; i < clientList.size() ; i++){
				if (clientList.get(i).getName().equals(user)
					&& clientList.get(i).getContact().equals(contact)){
		
					theClient =  clientList.get(i); 
					break;
				}
			}
			
			if (theClient == null) {
				throw new IllegalArgumentException("Could not find the Client of the loaded Reservation of " 
							+ reservationLine + ":" + user + contact);
			}
			
			TourCoordinator.getInstance().addOldReservation(theClient, tour, numInParty, code);
		} catch (CapacityException e) {
			reservationLineScanner.close();
			throw new IllegalArgumentException("Capacity over when reading a file.");
		} catch (Exception e) {
			throw new IllegalArgumentException("Something wrong in Reservation line. " + e.getMessage());
		}
	}
}
