package com.trains;

import com.trains.backend.BackendSession;
import com.trains.backend.UserService;
import com.trains.backend.BackendException;
import com.trains.backend.OrderService;
import com.trains.backend.TrainService;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.List;

import java.util.Scanner;

public class ClientInputHandler {
	private UserService userService;
	private BackendSession session;
	private Client currentClient;
	private OrderService orderService;
	private TrainService trainService;

	public ClientInputHandler(UserService userService, BackendSession session) {
		this.userService = userService;
		this.session = session;
		this.orderService = session.getOrderService();
		this.trainService = session.getTrainService();
	}

	public void handleInput() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Actions: ");
		System.out.println("1 - Quit ");
		System.out.println("2 - Create account ");
		System.out.println("3 - Print tables");
		System.out.println("4 - Add ticket");
		int choice = 0;
		while (choice != 1) {
			System.out.print("Choose: ");
			choice = Integer.parseInt(scanner.nextLine());
			try {
				switch (choice) {
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
		List<String> availableTrains = orderService.getAvailableTrains(10);

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
		String departureTime = trainDetails[1].split(": ")[1].replace("T", " ");

		System.out.print("Enter number of tickets: ");
		int numberOfTickets = Integer.parseInt(scanner.nextLine());

		orderService.upsertOrder(UUID.randomUUID(), trainId, Timestamp.valueOf(departureTime),
				userId, 1, numberOfTickets);
		System.out.println("Ticket added successfully.");
	}
}
