package applicationMain;

/**
 * Standalone UserNameRecognizer.
 * Validates usernames: start with A–Z/a–z, followed by A–Z/a–z/0–9/.-_, length 4–16.
 */
public class UserNameRecognizer {
	public static String userNameRecognizerErrorMessage = "";
	public static String userNameRecognizerInput = "";
	public static int userNameRecognizerIndexofError = -1;

	private static int state = 0;
	private static int nextState = 0;
	private static boolean finalState = false;
	private static String inputLine = "";
	private static char currentChar;
	private static int currentCharNdx;
	private static boolean running;
	private static int userNameSize = 0;

	private static void moveToNextCharacter() {
		currentCharNdx++;
		if (currentCharNdx < inputLine.length())
			currentChar = inputLine.charAt(currentCharNdx);
		else {
			currentChar = ' ';
			running = false;
		}
	}

	/**
	 * @param input Username to validate
	 * @return empty string if valid; otherwise an error message
	 */
	public static String checkForValidUserName(String input) {
		if (input.length() <= 0) {
			userNameRecognizerIndexofError = 0;
			return "\n*** ERROR *** The input is empty";
		}

		state = 0;
		inputLine = input;
		currentCharNdx = 0;
		currentChar = input.charAt(0);

		userNameRecognizerInput = input;
		running = true;
		nextState = -1;
		userNameSize = 0;
		finalState = false;

		while (running) {
			switch (state) {
				case 0:
					// Must start with A-Z or a-z
					if ((currentChar >= 'A' && currentChar <= 'Z') ||
						(currentChar >= 'a' && currentChar <= 'z')) {
						nextState = 1;
						userNameSize++;
					} else {
						running = false;
					}
					break;

				case 1:
					// A-Z, a-z, 0-9 stay in state 1
					if ((currentChar >= 'A' && currentChar <= 'Z') ||
						(currentChar >= 'a' && currentChar <= 'z') ||
						(currentChar >= '0' && currentChar <= '9')) {
						nextState = 1;
						userNameSize++;
					}
					// . - _ -> state 2
					else if (currentChar == '.' || currentChar == '-' || currentChar == '_') {
						nextState = 2;
						userNameSize++;
					} else {
						running = false;
					}
					if (userNameSize > 16) running = false;
					break;

				case 2:
					// After punctuation, must be A-Z, a-z, or 0-9
					if ((currentChar >= 'A' && currentChar <= 'Z') ||
						(currentChar >= 'a' && currentChar <= 'z') ||
						(currentChar >= '0' && currentChar <= '9')) {
						nextState = 1;
						userNameSize++;
					} else {
						running = false;
					}
					if (userNameSize > 16) running = false;
					break;
			}

			if (running) {
				moveToNextCharacter();
				state = nextState;
				if (state == 1) finalState = true;
				nextState = -1;
			}
		}

		userNameRecognizerIndexofError = currentCharNdx;
		userNameRecognizerErrorMessage = "\n*** ERROR *** ";

		switch (state) {
			case 0:
				return userNameRecognizerErrorMessage + "A UserName must start with A-Z, a-z.\n";

			case 1:
				if (userNameSize < 4)
					return userNameRecognizerErrorMessage + "A UserName must have at least 4 characters.\n";
				else if (userNameSize > 16)
					return userNameRecognizerErrorMessage + "A UserName must have no more than 16 characters.\n";
				else if (currentCharNdx < input.length())
					return userNameRecognizerErrorMessage +
						"A UserName character may only contain the characters A-Z, a-z, 0-9,-,.,_\n";
				else {
					userNameRecognizerIndexofError = -1;
					userNameRecognizerErrorMessage = "";
					return userNameRecognizerErrorMessage;
				}

			case 2:
				return userNameRecognizerErrorMessage +
					"A UserName character after a period must be A-Z, a-z, 0-9.\n";

			default:
				return "";
		}
	}
}
