package guiDiscussionBoard;

import java.util.Optional;
import guiUserLogin.ViewUserLogin;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

public class ControllerDiscussionBoard {

	//just a pop up
    private static void info(String title, String msg){
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // open up selected Thread
    protected static void openThread(String threadName){
        if (threadName == null) {
            info("Open Thread", "Please select a thread to open.");
            return;
        }
        ViewDiscussionBoard.showThread(threadName);
    }

    // Get the current listView then add in the thread
    protected static void newThread(ListView<String> listView){
    	
    	//Quick input (Do some validation checking later)
    	TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("New Thread"); // Set the title of the dialog window
        dialog.setHeaderText("Please enter Thread Name"); // Set the header text
        dialog.setContentText("Thread Name:"); // Set the prompt text for the input field
        
        dialog.showAndWait().ifPresent(name -> {
            String thread = name.trim();
            if (thread.isEmpty()) {
                info("New Thread", "Thread name cannot be empty.");
                return;
            }
            if (listView.getItems().contains(thread)) {
                info("New Thread", "A thread with that name already exists.");
                return;
            }
            listView.getItems().add(thread); //This is how you add to Thread list.
            listView.getSelectionModel().select(thread);
        });

        

    }

    // Get thread name from he thread list and delete it.
    protected static void deleteThread(ListView<String> listView, String threadName){
        if (threadName == null) {
            info("Delete Thread", "Please select a thread to delete.");
            return;
        }
        	ViewDiscussionBoard.hideThread();
        	listView.getItems().remove(threadName);  //This is how you delete from thread list
    }
}
