package guiRole2;
import java.util.HashMap;

import guiAdminHome.ViewAdminHome;

public class ControllerRole2Home {
	
	/*-*******************************************************************************************

	User Interface Actions for this page
	
	**********************************************************************************************/
	
	protected static void performUpdate () {
		guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewRole2Home.theStage, ViewRole2Home.theUser);
	}	

	
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewRole2Home.theStage);
	}
	
	protected static void performQuit() {
		System.exit(0);
	}
	
	// My code
	protected static void reviewStudent() {
		System.out.printf("Still gotta make this shit\n");
		return;
	}

	// go to guiDiscussionBoard -> ViewDiscussionBoard and set up the GUI
	// Establish the references to the GUI and the current user
	protected static void discussionThread() {
	    guiDiscussionBoard.ViewDiscussionBoard.display(
	        guiRole2.ViewRole2Home.theStage,
	        guiRole2.ViewRole2Home.theUser
	    );
	}
	
	protected static void parameter() {
		System.out.printf("Still gotta make this shit. No clue what this means\n");
		return;
	}
	
	protected static void makeRequest() {
	    guiRequests.ViewRequests.display(
	            ViewRole2Home.theStage,
	            ViewRole2Home.theUser
	        );
	}





	

}
