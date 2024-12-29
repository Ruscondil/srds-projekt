package com.trains;

import com.trains.backend.BackendSession;
import com.trains.backend.UserService;
import com.trains.backend.BackendException;

import java.util.Scanner;

public class ClientInputHandler {
	private UserService userService;
	private BackendSession session;
	private Client currentClient;

	public ClientInputHandler(UserService userService, BackendSession session) {
		this.userService = userService;
		this.session = session;
	}

	public void handleInput() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Actions: ");
		System.out.println("1 - Quit ");
		System.out.println("2 - Create account ");
		System.out.println("3 - Print tables");
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
					default: System.out.println("Wrong action");
				}
			} catch (BackendException e) {
				System.out.println("An error occurred: " + e.getMessage());
			}
		}
	}
}
