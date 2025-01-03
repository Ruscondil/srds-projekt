package com.trains;

import com.trains.backend.*;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientInputHandler {
	private UserService userService;
	private BackendSession session;
	private Client currentClient;
	private OrderService orderService;
	private TrainService trainService;

	private UserOrderService userOrderService;

	public ClientInputHandler(UserService userService, BackendSession session) {
		this.userService = userService;
		this.session = session;
		this.orderService = session.getOrderService();
		this.trainService = session.getTrainService();
		this.userOrderService = session.getUserOrderService();
	}

	public void printActions() {
		System.out.println("Actions: ");
		System.out.println("0 - Print actions");
		System.out.println("1 - Quit");
		System.out.println("2 - Create account");
		System.out.println("3 - Print tables");
		System.out.println("4 - Add ticket");
		System.out.println("5 - Scan ticket");
	}

	public void handleInput() {
		Scanner scanner = new Scanner(System.in);
		printActions();
		int choice = 0;
		while (choice != 1) {
			System.out.print("Choose: ");
			choice = Integer.parseInt(scanner.nextLine());
			try {
				switch (choice) {
					case 0: {
						printActions();
						break;
					}
					case 1: break;
					case 2: {
						currentClient = userService.createNewUser();
						break;
					}
					case 3: {
						session.printAllTables();
						break;
					}
					case 4: {
						addTicket(scanner);
						break;
					}
					case 5: {
						scanTicket(scanner);
					}
					default: System.out.println("Wrong action");
				}
			} catch (BackendException e) {
				System.out.println("An error occurred: " + e.getMessage());
			}
		}
	}

	private void addTicket(Scanner scanner) throws BackendException {
		if (currentClient == null) {
			throw new BackendException("Najpierw trzeba stworzyć użytkownika.");
		}

		UUID userId = currentClient.getUserId();

		List<String> availableTrains = trainService.getAvailableTrains(10);

		if (availableTrains.isEmpty()) {
			System.out.println("No available trains.");
			return;
		}

		System.out.println("Available trains:");
		for (int i = 0; i < availableTrains.size(); i++) {
			System.out.println((i + 1) + " - " + availableTrains.get(i));
		}

		System.out.print("Choose train: ");
		int trainChoice = Integer.parseInt(scanner.nextLine());
		if (trainChoice < 1 || trainChoice > availableTrains.size()) {
			System.out.println("Invalid choice.");
			return;
		}

		String selectedTrain = availableTrains.get(trainChoice - 1);
		String[] trainDetails = selectedTrain.split(", ");
		int trainId = Integer.parseInt(trainDetails[0].split(": ")[1]);



		// Pobieranie daty i czasu
		String rawDepartureTime = trainDetails[1].split(": ")[1]; // np. Sat Dec 28 12:30:00 CET 2024
		SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDepartureTime;

		try {
			Date parsedDate = inputFormat.parse(rawDepartureTime);
			formattedDepartureTime = outputFormat.format(parsedDate);
		} catch (ParseException e) {
			System.out.println("Invalid date format.");
			return;
		}

		int cars = Integer.parseInt(trainDetails[2].split(": ")[1]);
		int carCapacity = Integer.parseInt(trainDetails[3].split(": ")[1]);
		//TODO jakieś smart przydzielanie, (by jak najwięcej możę siedziało razem, ale nie musi być w sumie)
		System.out.print("Enter number of tickets: ");
		int numberOfTickets = Integer.parseInt(scanner.nextLine());
		int remainingTickets = numberOfTickets;
		UUID orderId = UUID.randomUUID();
		StringBuilder ticketInfo = new StringBuilder();

		for (int car = 1; car <= cars && remainingTickets > 0; car++) {
			int availableSeats = carCapacity - orderService.getReservedSeats(trainId, formattedDepartureTime, car);
			if (availableSeats > 0) {
				int ticketsToReserve = Math.min(remainingTickets, availableSeats);
				orderService.upsertOrder(orderId, trainId, Timestamp.valueOf(formattedDepartureTime), userId, car, ticketsToReserve);
				ticketInfo.append(String.format("Reserved %d tickets in car %d\n", ticketsToReserve, car));
				remainingTickets -= ticketsToReserve;
			}
		}

		if (remainingTickets > 0) {
			System.out.println("Not enough seats available for the requested number of tickets.");
		} else {
			System.out.println("Tickets reserved successfully:");
			System.out.println(ticketInfo.toString());
		}
	}

	private void scanTicket(Scanner scanner) throws BackendException {
		UUID userId = currentClient.getUserId();

		List<String> availableTrains = trainService.getAvailableTrains(10);

		if (availableTrains.isEmpty()) {
			System.out.println("No available trains.");
			return;
		}

		System.out.println("Available trains:");
		for (int i = 0; i < availableTrains.size(); i++) {
			System.out.println((i + 1) + " - " + availableTrains.get(i));
		}

		System.out.print("Choose train: ");
		int trainChoice = Integer.parseInt(scanner.nextLine());
		if (trainChoice < 1 || trainChoice > availableTrains.size()) {
			System.out.println("Invalid choice.");
			return;
		}

		String selectedTrain = availableTrains.get(trainChoice - 1);
		String[] trainDetails = selectedTrain.split(", ");
		int trainId = Integer.parseInt(trainDetails[0].split(": ")[1]);
		
		// Pobieranie daty i czasu
		String rawDepartureTime = trainDetails[1].split(": ")[1]; // np. Sat Dec 28 12:30:00 CET 2024
		SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String tripDate;

		try {
			Date parsedDate = inputFormat.parse(rawDepartureTime);
			tripDate = outputFormat.format(parsedDate);
		} catch (ParseException e) {
			System.out.println("Invalid date format.");
			return;
		}
		
		System.out.println(trainId);
		System.out.println(tripDate);
		System.out.println(userId);

		userOrderService.selectOrders(trainId, Timestamp.valueOf(tripDate), userId);

		


	}

}
