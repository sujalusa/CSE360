package guiStudentBoard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Launcher for the Student Board app */
public class MainStudentBoard extends Application {

    @Override
    public void start(Stage stage) {
        controllerStudentBoard controller = new controllerStudentBoard();

        // Optional demo seed
        controller.createPost("Alice", "How do I fix error X?", "General");
        controller.addReply(1, "Bob", "Try cleaning and rebuilding.");
        controller.addReply(1, "Charlie", "Check JDK version.");

        String currentUser = "Cherry";
        ViewStudentBoard view = new ViewStudentBoard(controller, currentUser);

        stage.setTitle("Student Board");
        stage.setScene(new Scene(view, 1100, 650));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
