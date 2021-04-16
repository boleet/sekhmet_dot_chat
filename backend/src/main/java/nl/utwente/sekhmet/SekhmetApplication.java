package nl.utwente.sekhmet;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@SpringBootApplication
public class SekhmetApplication {

	public static void main(String[] args) {
		checkExistsErrorLogs(); //create error log file if does not exists
		SpringApplication.run(SekhmetApplication.class, args);
	}

	public static void checkExistsErrorLogs() {
		Date date = new Date();
		try {
			Path path = Paths.get(System.getProperty("user.dir") + "/log");
			if (Files.notExists(path)) {
				System.out.println("Path created: /log");
				Files.createDirectory(path);
			}
			File file = new File("log/errorLogs.log");
			if (file.createNewFile()) {
				System.out.println("File Created: errorLogs.log");
			}
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

}
