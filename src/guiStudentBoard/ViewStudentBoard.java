package guiStudentBoard;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.User;
import guiRole1.ViewRole1Home;
import studentRequirements.StudentRequirements;
import studentRequirements.StudentRequirements.Post;
import studentRequirements.StudentRequirements.Reply;

public class ViewStudentBoard {

    // App sizing
    private static final double width  = FoundationsMain.WINDOW_WIDTH;
    private static final double height = FoundationsMain.WINDOW_HEIGHT;

    // Shared in-memory discussion service
    public static final StudentRequirements sr = FoundationsMain.discussionService;

    // Singleton wiring
    private static ViewStudentBoard theView;
    public static Stage theStage;
    public static Scene theScene;
    public static Pane  root;
    public static User  theUser;
    private static final Database db = FoundationsMain.database;

    // Header
    public static final Label  title    = new Label("Student Discussion");
    public static final Label  userLab  = new Label();
    public static final Button acctBtn  = new Button("Account Update");
    public static final Line   sep1     = new Line(20,95,width-20,95);

    // Lists
    public static final Label threadsL = new Label("Threads");
    public static final ListView<String> threadsLV = new ListView<>();

    public static final Label postsL = new Label("Posts");
    public static final ListView<Post>   postsLV   = new ListView<>();

    public static final Label repliesL = new Label("Replies");
    public static final ListView<Reply>  repliesLV = new ListView<>();

    // Details
    public static final Label    detailsL = new Label("Details");
    public static final TextArea detailsTA = new TextArea();

    // Actions
    public static final Button newPostBtn = new Button("New Post");
    public static final Button replyBtn   = new Button("Reply");
    public static final Button deleteBtn  = new Button("Delete My Post");

    public static final Line   sep4      = new Line(20,525,width-20,525);
    public static final Button backBtn   = new Button("Return");
    public static final Button logoutBtn = new Button("Logout");
    public static final Button quitBtn   = new Button("Quit");

    // Entry point
    public static void display(Stage ps, User user){
        theStage = ps;
        theUser  = user;

        if (theView == null) theView = new ViewStudentBoard();

        // Ensure DB cached flags (roles, etc.)
        db.getUserAccountDetails(user.getUserName());

        userLab.setText("User: " + user.getUserName());
        theStage.setTitle("CSE 360 Foundations: Student Discussion");

        guiStudentBoard.ControllerStudentBoard.refreshThreads(sr, threadsLV, postsLV, repliesLV, detailsTA);
        theStage.setScene(theScene);
        theStage.show();
    }

    private ViewStudentBoard(){
        // Pane + Scene
        root = new Pane();
        theScene = new Scene(root, width, height);

        // Header
        setupLabel(title, "Arial", 28, width, Pos.CENTER, 0, 5);
        setupLabel(userLab,"Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);
        setupButton(acctBtn, "Dialog", 18, 170, Pos.CENTER, 610, 45);
        acctBtn.setOnAction(e -> guiUserUpdate.ViewUserUpdate.displayUserUpdate(theStage, theUser));

        // Lists layout
        setupLabel(threadsL, "Arial", 18, 220, Pos.BASELINE_LEFT, 20, 110);
        threadsLV.setLayoutX(20);   threadsLV.setLayoutY(140); threadsLV.setPrefSize(110, 360);

        setupLabel(postsL, "Arial", 18, 320, Pos.BASELINE_LEFT, 150, 110);
        postsLV.setLayoutX(150);    postsLV.setLayoutY(140);   postsLV.setPrefSize(120, 360);

        setupLabel(repliesL, "Arial", 18, 300, Pos.BASELINE_LEFT, 300, 110);
        repliesLV.setLayoutX(300);  repliesLV.setLayoutY(140); repliesLV.setPrefSize(400, 360);

        // Details
        setupLabel(detailsL, "Arial", 18, 300, Pos.BASELINE_LEFT, 20, 510);
        detailsTA.setEditable(false);
        detailsTA.setWrapText(true);
        detailsTA.setLayoutX(20); detailsTA.setLayoutY(540); detailsTA.setPrefSize(860, 70);

        // Selection behavior
        threadsLV.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) ->
            ControllerStudentBoard.loadPostsForThread(sr, newV, postsLV, repliesLV, detailsTA)
        );
        postsLV.getSelectionModel().selectedItemProperty().addListener((o, oldP, newP) -> {
            ControllerStudentBoard.loadRepliesForPost(sr, newP, repliesLV);
            ControllerStudentBoard.showPostDetails(newP, detailsTA);
        });
        repliesLV.getSelectionModel().selectedItemProperty().addListener((o, oldR, newR) -> {
            if (newR != null) detailsTA.appendText("\n\nReply by " + newR.getAuthor()
                    + " @ " + newR.getCreatedAt() + ":\n" + newR.getContent());
        });

        // Actions
        setupButton(newPostBtn, "Dialog", 16, 150, Pos.CENTER, 260, 510);
        newPostBtn.setOnAction(e -> ControllerStudentBoard.doNewPost(theStage, theUser, sr, threadsLV));

        setupButton(replyBtn, "Dialog", 16, 120, Pos.CENTER, 430, 510);
        replyBtn.setOnAction(e -> ControllerStudentBoard.doReply(theUser, sr, postsLV, repliesLV, detailsTA));

        setupButton(deleteBtn, "Dialog", 16, 150, Pos.CENTER, 560, 510);
        deleteBtn.setOnAction(e -> ControllerStudentBoard.doDeleteMyPost(theUser, sr, postsLV, threadsLV, repliesLV, detailsTA));

        // Footer
        setupButton(backBtn, "Dialog", 18, 220, Pos.CENTER, 20, 540);
        backBtn.setOnAction(e -> ViewRole1Home.displayRole1Home(theStage, theUser));

        setupButton(logoutBtn, "Dialog", 18, 220, Pos.CENTER, 290, 540);
        logoutBtn.setOnAction(e -> guiUserLogin.ViewUserLogin.displayUserLogin(theStage));

        setupButton(quitBtn, "Dialog", 18, 220, Pos.CENTER, 550, 540);
        quitBtn.setOnAction(e -> System.exit(0));

        // Assemble
        root.getChildren().addAll(
            title, userLab, acctBtn, sep1,
            threadsL, threadsLV, postsL, postsLV, repliesL, repliesLV,
            sep4, newPostBtn, replyBtn, deleteBtn,
            backBtn, logoutBtn, quitBtn
        );

        // Seed demo data once
        ControllerStudentBoard.seedDemoIfEmpty(sr, threadsLV);
    }

    // Tiny UI helpers
    private static void setupLabel(Label l, String ff, double f, double w, Pos p, double x, double y){
        l.setFont(Font.font(ff, f)); l.setMinWidth(w); l.setAlignment(p); l.setLayoutX(x); l.setLayoutY(y);
    }
    private static void setupButton(Button b, String ff, double f, double w, Pos p, double x, double y){
        b.setFont(Font.font(ff, f)); b.setMinWidth(w); b.setAlignment(p); b.setLayoutX(x); b.setLayoutY(y);
    }
}
