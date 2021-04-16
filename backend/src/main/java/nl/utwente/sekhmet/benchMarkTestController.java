package nl.utwente.sekhmet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class benchMarkTestController {

	@GetMapping("/benchmarkTestReceiver")
	protected String benchmarkTestReceiver() {
		return "benchmarkTestReceiver";
	}

	@GetMapping("/tcpBenchmarkSender")
	protected String tcpBenchmarkSender() {
		return "tcpBenchmarkSender";
	}

	@GetMapping("/udpBenchmarkSender")
	protected String udpBenchmarkSender() {
		return "udpBenchmarkSender";
	}

	@GetMapping("/serverBenchmarkSender")
	protected String serverBenchmarkSender() {
		return "serverBenchmarkSender";
	}
}
