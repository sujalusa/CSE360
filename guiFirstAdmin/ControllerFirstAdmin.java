package guiFirstAdmin;

import applicationMain.FoundationsMain;
import applicationMain.UserNameRecognizer;
import database.Database;
import entityClasses.User;
import guiUserUpdate.ViewUserUpdate;
import java.sql.SQLException;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import passwordPopUpWindow.Model;
import validation.PasswordValidator;

public class ControllerFirstAdmin {
	/*-********************************************************************************************

	The controller attributes for this page
	
	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and 
	the Model is often just a stub, or will be a singleton instantiated object.
	
	*/
	
	private static String adminUsername = "";
	private static String adminPassword1 = "";
	private static String adminPassword2 = "";		
	protected static Database theDatabase = applicationMain.FoundationsMain.database;		

	/*-********************************************************************************************

	The User Interface Actions for this page
	
	*/
	
	
	/**********
	 * <p> Method: setAdminUsername() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the username field in the
	 * View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminUsername() {
		adminUsername = ViewFirstAdmin.text_AdminUsername.getText();
	}
	
	
	/**********
	 * <p> Method: setAdminPassword1() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the password 1 field in
	 * the View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminPassword1() {
		adminPassword1 = ViewFirstAdmin.text_AdminPassword1.getText();
		ViewFirstAdmin.label_PasswordsDoNotMatch.setText("");
//		String validPassword = PasswordValidator.evaluate(adminPassword1);
//		if (!validPassword.isEmpty())
//		{
//			showValidationError("Invalid Password", validPassword, adminPassword1, PasswordValidator.passwordIndexofError);
//			return;
//		}
		
	}
	
	
	/**********
	 * <p> Method: setAdminPassword2() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the password 2 field in
	 * the View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminPassword2() {
		adminPassword2 = ViewFirstAdmin.text_AdminPassword2.getText();		
		ViewFirstAdmin.label_PasswordsDoNotMatch.setText("");
	}
	
	
	/**********
	 * <p> Method: doSetupAdmin() </p>
	 * 
	 * <p> Description: This method is called when the user presses the button to set up the Admin
	 * account.  It start by trying to establish a new user and placing that user into the
	 * database.  If that is successful, we proceed to the UserUpdate page.</p>
	 * 
	 */
   protected static void doSetupAdmin(Stage ps, int rs) {
	if (adminPassword1.equals(adminPassword2)) {
	    
	    String usernameError = UserNameRecognizer.checkForValidUserName(adminUsername);
	    if (!usernameError.equals("")) {
	        ViewFirstAdmin.label_PasswordsDoNotMatch.setText("Invalid Username: " + usernameError);
	        return;
	    }

	    if(!isPasswordValid(adminPassword1))
	    {
	    	return;
	    }

	    
	   
	    User user = new User(adminUsername, adminPassword1, "", "", "", "", "", true, false, false);
	    try {
	        theDatabase.register(user);
	    }
	    catch (SQLException e) {
	        System.err.println("*** ERROR *** Database error trying to register a user: " + e.getMessage());
	        e.printStackTrace();
	        System.exit(0);
	    }

	    guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewFirstAdmin.theStage, user);
	    
	} else {
	    ViewFirstAdmin.text_AdminPassword1.setText("");
	    ViewFirstAdmin.text_AdminPassword2.setText("");
	    ViewFirstAdmin.label_PasswordsDoNotMatch.setText(
	        "The two passwords must match. Please try again!");
	}

	
	}
	
	
	/**********
	 * <p> Method: performQuit() </p>
	 * 
	 * <p> Description: This method terminates the execution of the program.  It leaves the
	 * database in a state where the normal login page will be displayed when the application is
	 * restarted.</p>
	 * 
	 */
	protected static void performQuit() {
		System.out.println("Perform Quit");
		System.exit(0);
	}	
	
	private static void showValidationError(String title, String message, String input, int at)
	{
		// Making the error Message
		String pointer = "";
		if (at >= 0 && at <= input.length())
		{
			String beforeError = input.substring(0, at);	//Empty Pointer
			pointer = beforeError;
		}
		
		Alert alert = new Alert(Alert.AlertType.ERROR);  //Heres the error alert
		alert.setTitle(title);							// Whats the alert
		alert.setHeaderText(title);
		
	    // Combine the message, input, and pointer into the alert’s content
	    StringBuilder content = new StringBuilder();
	    content.append(message).append("\n\n");
	    content.append(input).append("\n");
	    content.append(pointer);

	    alert.setContentText(content.toString());
	    alert.showAndWait();						//show to user
	    
	}
	
	private static boolean isPasswordValid(String pwd) {
	    String error = Model.evaluatePassword(pwd);   // empty string = valid
	    if (error == null || error.isEmpty()) return true;

	    // Model.passwordIndexofError is where it failed (or current length)
	    showValidationError("Invalid Password", error, pwd, Model.passwordIndexofError);
	    return false;
	}

}

