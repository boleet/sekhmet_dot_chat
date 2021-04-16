package nl.utwente.sekhmet.webSockets;

import nl.utwente.sekhmet.jpa.model.Enrollment;
import nl.utwente.sekhmet.jpa.repositories.EnrollmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The "singleton" The secured session map.
 * <p>
 * A wrapper for retrieving a web socket connection, verifying the sender is allowed to contact that receiver first.
 */
@Component
public class TheSecuredSessionMap {
	private static ConcurrentHashMap<Long, WebSocketSession> theSessionMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Long, Set<Long>> verifiedList = new ConcurrentHashMap<>();

	private static EnrollmentRepository enrollmentRepository;

	/**
	 * Instantiates a new The secured session map.
	 * <p>
	 * called exclusively by spooky spring garbage
	 *
	 * @param enrollmentRepository the enrollment repository
	 */
	public TheSecuredSessionMap(EnrollmentRepository enrollmentRepository) {
		this.enrollmentRepository = enrollmentRepository;
	}

	/**
	 * Put.
	 *
	 * @param pid     the key
	 * @param session the value
	 */
	public static void put(long pid, WebSocketSession session) {
		theSessionMap.put(pid, session);
	}

	/**
	 * Get web socket session of receiver.
	 *
	 * @param receiver the receiver
	 * @param sender   the sender
	 * @return the web socket session
	 */
	public static WebSocketSession get(String receiver, long sender) {

		boolean verified = false;
		Set verifiedPeople = verifiedList.get(sender);
		if (verifiedPeople != null ) {
            verified = verifiedPeople.contains(Long.valueOf(receiver));
		} else {
			verifiedPeople = new ConcurrentSkipListSet();
			verifiedList.put(sender,verifiedPeople);
		}

		if (verified) {
			return theSessionMap.get(Long.valueOf(receiver));
		}else {

			List<Enrollment> enrollments1 = enrollmentRepository.findByEnrollmentIdUserId(sender);
			HashMap<Long, Enrollment> enrollmentHashMap = new HashMap<>();
			for (Enrollment e : enrollments1) {
				enrollmentHashMap.put(e.getTest().getId(), e);
			}
			List<Enrollment> enrollments2 = enrollmentRepository.findByEnrollmentIdUserId(Long.valueOf(receiver));
			for (Enrollment e1 : enrollments2) {
				if (enrollmentHashMap.keySet().contains(e1.getTest().getId())) {
					Enrollment e2 = enrollmentHashMap.get(e1.getTest().getId());
					if ((e1.getRole() == Enrollment.Role.TEACHER || e2.getRole() == Enrollment.Role.TEACHER)
							&& (e1.getTest().getStartTime() != null && e1.getTest().getEndTime() == null)
							) {
						verified = true;

						verifiedPeople.add(Long.valueOf(receiver));
						//Added by jan, makes sure the system doesnt crash when receiver not in list
						Set<Long> temp = verifiedList.get(Long.valueOf(receiver));
						if (temp == null) {
							temp = new ConcurrentSkipListSet<>();
							verifiedList.put(Long.valueOf(receiver),temp); // pointer magic yay
						}
						temp.add(sender);
						break;
					}
				}
			}
			if (verified) {
				return theSessionMap.get(Long.valueOf(receiver));
			}else
				return null;
		}
	}

	/**
	 * Remove.
	 *
	 * @param pid the person Id of the closed session
	 */
	public static void remove(long pid) {
		theSessionMap.remove(pid);
	}

	/**
	 * Clear cache.
	 */
	public static void clearCache(){
		verifiedList.clear();
	}
}
