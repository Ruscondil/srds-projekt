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
		System.out.println("6 - Login");
	}

	public void handleInput() {
		Scanner scanner = new Scanner(System.in);
		printActions();
		int choice = 0;
		while (choice != 1) {
			System.out.print("Choose: ");
			try {
				choice = Integer.parseInt(scanner.nextLine());
			} catch (NumberFormatException e) {
				choice = 0;
				System.out.println("Invalid input. Defaulting to 0.");
			}
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
						break;
					}
					case 6: {
						login(scanner);
						break;
					}
					default: {
						printActions();
						break;
					}
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

		int trainChoice = getUserTrainChoice(scanner, availableTrains);
		if (trainChoice == -1) {
			System.out.println("Invalid choice.");
			return;
		}

		String selectedTrain = availableTrains.get(trainChoice - 1);
		String formattedDepartureTime = getFormattedDepartureTime(selectedTrain);
		if (formattedDepartureTime == null) {
			System.out.println("Invalid date format.");
			return;
		}

		int numberOfTickets = getNumberOfTickets(scanner);
		UUID orderId = UUID.randomUUID();
		String ticketInfo = reserveTickets(orderId, selectedTrain, formattedDepartureTime, userId, numberOfTickets);

		if (ticketInfo == null) {
			System.out.println("Not enough seats available for the requested number of tickets.");
		} else {
			System.out.println("Tickets reserved successfully:");
			System.out.println(ticketInfo);
		}
	}

	private int getUserTrainChoice(Scanner scanner, List<String> availableTrains) {
		System.out.println("Available trains:");
		for (int i = 0; i < availableTrains.size(); i++) {
			System.out.println((i + 1) + " - " + availableTrains.get(i));
		}

		System.out.print("Choose train: ");
		int trainChoice = Integer.parseInt(scanner.nextLine());
		if (trainChoice < 1 || trainChoice > availableTrains.size()) {
			return -1;
		}
		return trainChoice;
	}

	private String getFormattedDepartureTime(String selectedTrain) {
		String rawDepartureTime = selectedTrain.split(", ")[1].split(": ")[1];
		SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			Date parsedDate = inputFormat.parse(rawDepartureTime);
			return outputFormat.format(parsedDate);
		} catch (ParseException e) {
			return null;
		}
	}

	private int getNumberOfTickets(Scanner scanner) {
		System.out.print("Enter number of tickets: ");
		return Integer.parseInt(scanner.nextLine());
	}

	private String reserveTickets(UUID orderId, String selectedTrain, String formattedDepartureTime, UUID userId, int numberOfTickets) {
		String[] trainDetails = selectedTrain.split(", ");
		int trainId = Integer.parseInt(trainDetails[0].split(": ")[1]);
		int cars = Integer.parseInt(trainDetails[2].split(": ")[1]);
		int carCapacity = Integer.parseInt(trainDetails[3].split(": ")[1]);

		int totalCapacity = cars * carCapacity;
		int reservedSeats = orderService.getTakenSeats(trainId, formattedDepartureTime);
		int availableSeats = totalCapacity - reservedSeats;

		if (availableSeats < numberOfTickets) {
			return null;
		}

		int remainingTickets = numberOfTickets;
		StringBuilder ticketInfo = new StringBuilder();

		for (int car = 1; car <= cars && remainingTickets > 0; car++) {
			int availableSeatsInCar = carCapacity - orderService.getTakenSeatsByCar(trainId, formattedDepartureTime, car);
			System.out.println("Car " + car + ": Available seats: " + availableSeatsInCar + ", Car capacity: " + carCapacity + ", Reserved seats: " + orderService.getTakenSeatsByCar(trainId, formattedDepartureTime, car));
			if (availableSeatsInCar > 0) {
				int ticketsToReserve = Math.min(remainingTickets, availableSeatsInCar);
				orderService.upsertOrder(orderId, trainId, Timestamp.valueOf(formattedDepartureTime), userId, car, ticketsToReserve);
				ticketInfo.append(String.format("Reserved %d tickets in car %d\n", ticketsToReserve, car));
				remainingTickets -= ticketsToReserve;
			}
		}

		return remainingTickets > 0 ? null : ticketInfo.toString();
	}

	private void scanTicket(Scanner scanner) throws BackendException {
		List<Client> users = userService.getAllUsers();
		if (users.isEmpty()) {
			System.out.println("No users available.");
			return;
		}

		System.out.println("Available users:");
		for (int i = 0; i < users.size(); i++) {
			System.out.println((i + 1) + " - " + users.get(i).getName());
		}

		System.out.print("Choose user: ");
		int userChoice = Integer.parseInt(scanner.nextLine());
		if (userChoice < 1 || userChoice > users.size()) {
			System.out.println("Invalid choice.");
			return;
		}

		UUID userId = users.get(userChoice - 1).getUserId();

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

	private void login(Scanner scanner) throws BackendException {
		List<Client> users = userService.getAllUsers();
		if (users.isEmpty()) {
			System.out.println("No users available.");
			return;
		}

		System.out.println("Available users:");
		for (int i = 0; i < users.size(); i++) {
			System.out.println((i + 1) + " - " + users.get(i).getName());
		}

		System.out.print("Choose user: ");
		int userChoice = Integer.parseInt(scanner.nextLine());
		if (userChoice < 1 || userChoice > users.size()) {
			System.out.println("Invalid choice.");
			return;
		}

		currentClient = users.get(userChoice - 1);
		System.out.println("Logged in as " + currentClient.getName());
	}

}
