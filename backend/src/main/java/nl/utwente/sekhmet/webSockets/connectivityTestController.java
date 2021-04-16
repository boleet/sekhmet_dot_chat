package nl.utwente.sekhmet.webSockets;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class connectivityTestController {

	@GetMapping("/connectivityTest")
	private String connectivityTest() {
		return "connectivityTest";
	}
}
