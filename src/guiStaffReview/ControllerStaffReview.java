package guiStaffReview;

import javafx.collections.FXCollections;
import javafx.scene.control.*;
import review.ReviewService;
import review.ReviewService.Feedback;
import review.ReviewService.Feedback.Scope;
import review.ReviewService.Feedback.TargetType;
import entityClasses.User;
import studentRequirements.StudentRequirements;
import studentRequirements.StudentRequirements.Post;
import studentRequirements.StudentRequirements.Reply;

import java.util.stream.Collectors;

class ControllerStaffReview {

    // ---------- Actions ----------
    static void doAddFeedback(User staff,
                              ReviewService rv,
                              ListView<Post> postsLV,
                              ListView<Reply> repliesLV,
                              ListView<Feedback> fbLV) {

        Reply r = repliesLV.getSelectionModel().getSelectedItem();
        Post  p = postsLV.getSelectionModel().getSelectedItem();
        if (r==null && p==null){ info("Feedback","Select a post or a reply first."); return; }

        var scopeChoice = new ChoiceDialog<>(Scope.PRIVATE_TO_STUDENT, Scope.PRIVATE_TO_STUDENT, Scope.STAFF_ONLY);
        scopeChoice.setTitle("Feedback Scope");
        scopeChoice.setHeaderText("Who should see this feedback?");
        var scope = scopeChoice.showAndWait().orElse(null);
        if (scope == null) return;

        var dlg = new TextInputDialog("");
        dlg.setTitle("Feedback");
        dlg.setHeaderText("Enter feedback text");
        dlg.setContentText("Text:");
        var text = dlg.showAndWait().orElse(null);
        if (text==null || text.isBlank()) return;

        TargetType tt;
        int targetId;
        String recipient;

        if (r != null){
            tt = TargetType.REPLY;
            targetId = r.getId();
            recipient = r.getAuthor();
        } else {
            tt = TargetType.POST;
            targetId = p.getId();
            recipient = p.getAuthor();
        }

        rv.addFeedback(tt, targetId, staff.getUserName(), recipient, text.trim(), scope);
        loadFeedback(rv, tt, targetId, fbLV);
    }

    // ---------- Data helpers ----------
    static void refreshThreads(StudentRequirements sr,
                               ListView<String> threadsLV,
                               ListView<Post> postsLV,
                               ListView<Reply> repliesLV,
                               ListView<Feedback> fbLV,
                               TextArea detailsTA) {

        var names = sr.listAllPosts()
                .stream()
                .map(Post::getThread)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (!names.contains("General")) names.add(0,"General");

        threadsLV.setItems(FXCollections.observableArrayList(names));
        if (!names.isEmpty()) {
            if (threadsLV.getSelectionModel().getSelectedItem() == null) {
                threadsLV.getSelectionModel().select(0);
            }
            loadPostsForThread(sr, threadsLV.getSelectionModel().getSelectedItem(),
                    postsLV, repliesLV, fbLV, detailsTA);
        }
    }

    static void loadPostsForThread(StudentRequirements sr,
                                   String thread,
                                   ListView<Post> postsLV,
                                   ListView<Reply> repliesLV,
                                   ListView<Feedback> fbLV,
                                   TextArea detailsTA) {
        if (thread == null) {
            postsLV.setItems(FXCollections.observableArrayList());
            repliesLV.setItems(FXCollections.observableArrayList());
            fbLV.setItems(FXCollections.observableArrayList());
            detailsTA.clear();
            return;
        }
        var posts = FXCollections.observableArrayList(sr.searchPosts("", thread));
        postsLV.setItems(posts);
        repliesLV.setItems(FXCollections.observableArrayList());
        fbLV.setItems(FXCollections.observableArrayList());
        detailsTA.clear();
    }

    static void loadRepliesForPost(StudentRequirements sr,
                                   Post p,
                                   String currentUsername,
                                   ListView<Reply> repliesLV,
                                   ListView<Feedback> fbLV) {
        if (p==null){ repliesLV.setItems(FXCollections.observableArrayList()); fbLV.setItems(FXCollections.observableArrayList()); return; }
        repliesLV.setItems(FXCollections.observableArrayList(sr.listReplies(p.getId(), currentUsername, false)));
        fbLV.setItems(FXCollections.observableArrayList());
    }

    static void loadFeedback(ReviewService rv,
                             TargetType tt,
                             int targetId,
                             ListView<Feedback> fbLV){
        if (targetId < 0){ fbLV.setItems(FXCollections.observableArrayList()); return; }
        fbLV.setItems(FXCollections.observableArrayList(rv.listFeedbackForTarget(tt, targetId)));
    }

    static void showPostDetails(Post p, TextArea detailsTA){
        if (p==null){ detailsTA.clear(); return; }
        detailsTA.setText("POST #" + p.getId() + " ("+p.getThread()+") by "+p.getAuthor()+" @ "+p.getCreatedAt()+"\n\n"+p.getContent());
    }

    static void showReplyDetails(Reply r, TextArea detailsTA){
        if (r==null){ return; }
        detailsTA.setText("REPLY #" + r.getId() + " to Post "+r.getParentPostId()+" by "+r.getAuthor()+" @ "+r.getCreatedAt()+"\n\n"+r.getContent());
    }

    static void formatters(ListView<Post> postsLV, ListView<Reply> repliesLV){
        postsLV.setCellFactory(lv -> new ListCell<>(){
            @Override protected void updateItem(Post p, boolean empty){
                super.updateItem(p, empty);
                setText(empty||p==null ? null :
                    "#"+p.getId()+" • "+p.getAuthor()+" — "+snippet(p.getContent(), 60));
            }
        });
        repliesLV.setCellFactory(lv -> new ListCell<>(){
            @Override protected void updateItem(Reply r, boolean empty){
                super.updateItem(r, empty);
                setText(empty||r==null ? null :
                    r.getAuthor()+": "+snippet(r.getContent(), 50));
            }
        });
    }

    private static String snippet(String s, int n){ return s != null && s.length()>n ? s.substring(0,n)+"…" : s; }

    private static void info(String t, String m){
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(t); a.setContentText(m); a.showAndWait();
    }
}
