package guiStaffReview;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.User;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import review.ReviewService;
import review.ReviewService.Feedback;
import studentRequirements.StudentRequirements;
import studentRequirements.StudentRequirements.Post;
import studentRequirements.StudentRequirements.Reply;

public class ViewStaffReview {

    private static final double width  = FoundationsMain.WINDOW_WIDTH;
    private static final double height = FoundationsMain.WINDOW_HEIGHT;

    // shared services
    public static final StudentRequirements SR = FoundationsMain.discussionService;
    public static final ReviewService RV       = FoundationsMain.reviewService;
    public static final Database DB            = FoundationsMain.database;

    // singleton wiring
    private static ViewStaffReview theView;
    public static Stage theStage;
    public static Scene theScene;
    public static Pane  root;
    public static User  theUser;

    // header
    public static final Label  title = new Label("Staff Review");
    public static final Label  who   = new Label();
    public static final Button acct  = new Button("Account Update");
    public static final Line   sep1  = new Line(20,95,width-20,95);

    // columns
    public static final Label threadsL = new Label("Threads");
    public static final ListView<String> threadsLV = new ListView<>();

    public static final Label postsL = new Label("Posts");
    public static final ListView<Post> postsLV = new ListView<>();

    public static final Label repliesL = new Label("Replies");
    public static final ListView<Reply> repliesLV = new ListView<>();

    // details + feedback
    public static final Label    detailsL = new Label("Details");
    public static final TextArea detailsTA = new TextArea();

    public static final Label fbL = new Label("Feedback (for selected Post/Reply)");
    public static final ListView<Feedback> fbLV = new ListView<>();
    public static final Button addFbBtn = new Button("Add Feedback");

    // footer
    public static final Line   sep4   = new Line(20,525,width-20,525);
    public static final Button back   = new Button("Return");
    public static final Button logout = new Button("Logout");
    public static final Button quit   = new Button("Quit");

    public static void display(Stage ps, User staff){
        theStage = ps; theUser = staff;
        if (theView == null) theView = new ViewStaffReview();
        DB.getUserAccountDetails(staff.getUserName());

        who.setText("Staff: " + staff.getUserName());
        theStage.setTitle("CSE 360 Foundations: Staff Review");

        ControllerStaffReview.refreshThreads(SR, threadsLV, postsLV, repliesLV, fbLV, detailsTA);
        ControllerStaffReview.formatters(postsLV, repliesLV);

        theStage.setScene(theScene);
        theStage.show();
    }

    private ViewStaffReview(){
        root = new Pane();
        theScene = new Scene(root, width, height);

        // header
        setupLabel(title,"Arial",28,width, Pos.CENTER, 0,5);
        setupLabel(who,"Arial",20,width, Pos.BASELINE_LEFT, 20,55);
        setupButton(acct,"Dialog",18,170, Pos.CENTER, 610,45);
        acct.setOnAction(e -> guiUserUpdate.ViewUserUpdate.displayUserUpdate(theStage, theUser));

        // columns
        setupLabel(threadsL,"Arial",18,200, Pos.BASELINE_LEFT, 20,110);
        threadsLV.setLayoutX(20); threadsLV.setLayoutY(140); threadsLV.setPrefSize(110,360);

        setupLabel(postsL,"Arial",18,200, Pos.BASELINE_LEFT, 150,110);
        postsLV.setLayoutX(150); postsLV.setLayoutY(140); postsLV.setPrefSize(330,360);

        setupLabel(repliesL,"Arial",18,200, Pos.BASELINE_LEFT, 500,110);
        repliesLV.setLayoutX(500); repliesLV.setLayoutY(140); repliesLV.setPrefSize(240,180);

        setupLabel(fbL,"Arial",16,300, Pos.BASELINE_LEFT, 500,330);
        fbLV.setLayoutX(500); fbLV.setLayoutY(360); fbLV.setPrefSize(240,140);

        setupButton(addFbBtn,"Dialog",14,200, Pos.CENTER, 520, 495);
        addFbBtn.setOnAction(e -> ControllerStaffReview.doAddFeedback(theUser, RV, postsLV, repliesLV, fbLV));

        // details
        setupLabel(detailsL,"Arial",18,300, Pos.BASELINE_LEFT, 20,510);
        detailsTA.setEditable(false); detailsTA.setWrapText(true);
        detailsTA.setLayoutX(20); detailsTA.setLayoutY(540); detailsTA.setPrefSize(860,70);

        // selection listeners
        threadsLV.getSelectionModel().selectedItemProperty().addListener((o,a,b) ->
            ControllerStaffReview.loadPostsForThread(SR, b, postsLV, repliesLV, fbLV, detailsTA)
        );
        postsLV.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            ControllerStaffReview.loadRepliesForPost(SR, b, theUser.getUserName(), repliesLV, fbLV);
            ControllerStaffReview.showPostDetails(b, detailsTA);
        });
        repliesLV.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            ControllerStaffReview.showReplyDetails(b, detailsTA);
            ControllerStaffReview.loadFeedback(RV,
                review.ReviewService.Feedback.TargetType.REPLY,
                (b==null ? -1 : b.getId()),
                fbLV
            );
        });

        // footer
        setupButton(back,"Dialog",18,220, Pos.CENTER,20,620);
        back.setOnAction(e -> guiRole2.ViewRole2Home.displayRole2Home(theStage, theUser));
        setupButton(logout,"Dialog",18,220, Pos.CENTER,290,620);
        logout.setOnAction(e -> guiUserLogin.ViewUserLogin.displayUserLogin(theStage));
        setupButton(quit,"Dialog",18,220, Pos.CENTER,550,620);
        quit.setOnAction(e -> System.exit(0));

        root.getChildren().addAll(
            title, who, acct, sep1,
            threadsL, threadsLV, postsL, postsLV, repliesL, repliesLV,
            fbL, fbLV, addFbBtn,
            sep4, detailsL, detailsTA,
            back, logout, quit
        );
    }

    // UI helpers
    private static void setupLabel(Label l, String ff, double f, double w, Pos p, double x, double y){
        l.setFont(Font.font(ff,f)); l.setMinWidth(w); l.setAlignment(p); l.setLayoutX(x); l.setLayoutY(y);
    }
    private static void setupButton(Button b, String ff, double f, double w, Pos p, double x, double y){
        b.setFont(Font.font(ff,f)); b.setMinWidth(w); b.setAlignment(p); b.setLayoutX(x); b.setLayoutY(y);
    }
}
