package nl.utwente.sekhmet;

import nl.utwente.sekhmet.api.RestErrorApiController;
import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.*;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * The "singleton" The security enforcer.
 * <p>
 * "singleton" since it's not a proper singleton, but a set of statefull static functions that query the database based on incoming data from the endpoints.
 */
@Component
public class TheSecurityEnforcer {

	private static EnrollmentRepository enrollmentRepository;
	private static UserRepository userRepository;
	private static TestRepository testRepository;
	private static CourseRepository courseRepository;
	private static ConversationRepository conversationRepository;

	/**
	 * Instantiates a new The security enforcer.
	 * <p>
	 * Only to be called by spoopy spring shite
	 *
	 * @param enrollmentRepository   the enrollment repository
	 * @param userRepository         the user repository
	 * @param testRepository         the test repository
	 * @param courseRepository       the course repository
	 * @param conversationRepository the conversation repository
	 */
	public TheSecurityEnforcer(EnrollmentRepository enrollmentRepository, UserRepository userRepository, TestRepository testRepository, CourseRepository courseRepository, ConversationRepository conversationRepository) {
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.testRepository = testRepository;
		this.courseRepository = courseRepository;
		this.conversationRepository = conversationRepository;
	}

	// Role required: Logged in: should be implied, uid is set

	/**
	 * Is employee boolean.
	 *
	 * @param pid the pid
	 * @return the boolean
	 */
	public static boolean isEmployee(long pid) {
		User user = userRepository.findUserById(pid);
		try {
			return user.isEmployee() || user.isSystemAdmin();
		}catch (NullPointerException | NoSuchElementException e) {
			RestErrorApiController.logError(System.currentTimeMillis(),"invalid User isEmployee request","for: " + pid);
			return false;
		}
	}

	/**
	 * Is coordinator for course boolean.
	 *
	 * @param pid the pid
	 * @param cid the cid
	 * @return the boolean
	 */
	public static boolean isCoordinatorForCourse(long pid, long cid) {
		try {
			System.out.println();
			Course course = courseRepository.findById(cid).get();
			return course.getModuleCoordinator().getId() == pid || userRepository.findUserById(pid).isSystemAdmin();
		}catch (NullPointerException | NoSuchElementException e) {
			if (userRepository.findUserById(pid).isSystemAdmin()) {
				return true;
			}
			RestErrorApiController.logError(System.currentTimeMillis(),"invalid User isCoordinatorForCourse request","for person: " + pid + " and course: " + cid);
			return false;
		}
	}

	/**
	 * Is teacher for test boolean.
	 *
	 * @param pid the pid
	 * @param tid the tid
	 * @return the boolean
	 */
	public static boolean isTeacherForTest(long pid, long tid) {
		User user = userRepository.findUserById(pid);
		//return user.getEnrollment(testRepository.findTestById(tid)) != null || user.isSystemAdmin();
		try {
			return enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(tid, user.getId()).getRole() == Enrollment.Role.TEACHER || isCoordinatorForTest(pid, tid);
		}catch (NullPointerException | NoSuchElementException e) {
			if (isCoordinatorForTest(pid, tid)) {
				return true;
			}
			RestErrorApiController.logError(System.currentTimeMillis(),"invalid User isTeacherForTest request","for person: " + pid + " and test: " + tid);
			return false;
		}
	}

	/**
	 * Is coordinator for test boolean.
	 *
	 * @param pid the pid
	 * @param tid the tid
	 * @return the boolean
	 */
	public static boolean isCoordinatorForTest(long pid, long tid) {
		try {
			return testRepository.findTestById(tid).getCourse().getModuleCoordinator().getId().equals(pid) || userRepository.findUserById(pid).isSystemAdmin();
		}catch (NullPointerException | NoSuchElementException e) {
			if (userRepository.findUserById(pid).isSystemAdmin()) {
				return true;
			}
			RestErrorApiController.logError(System.currentTimeMillis(),"invalid User isCoordinatorForTest request","for person: " + pid + " and test: " + tid);
			return false;
		}
	}

	/**
	 * Is user for conversation boolean.
	 *
	 * @param pid the pid
	 * @param cid the cid
	 * @return the boolean
	 */
	public static boolean isUserForConversation(long pid, long cid) {
		Conversation conversation = conversationRepository.findConversationById(cid);
		User user = userRepository.findUserById(pid);
		try {
			//return user.getEnrollment(conversation.getTest()).getRole() == Enrollment.Role.TEACHER || conversation.getUser1().getId() == pid;
			return enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(conversation.getTest().getId(), user.getId()).getRole() == Enrollment.Role.TEACHER || conversation.getUser1().getId() == pid;
		}catch (NullPointerException | NoSuchElementException e) { // I love this.
			try { // yay, java
				return isCoordinatorForTest(pid,conversation.getTest().getId());
			} catch (NullPointerException f) {
				RestErrorApiController.logError(System.currentTimeMillis(),"invalid User isCoordinatorForConversation request","for person: " + pid + " and chat: " + cid);
				return  false;
			}
		}
	}

}
