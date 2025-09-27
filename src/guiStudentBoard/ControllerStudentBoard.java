package guiStudentBoard;

import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.stage.Stage;

import entityClasses.User;
import studentRequirements.StudentRequirements;
import studentRequirements.StudentRequirements.Post;
import studentRequirements.StudentRequirements.Reply;

import java.util.stream.Collectors;

class ControllerStudentBoard {

    // --- Actions ---
    static void doNewPost(Stage stage, User user, StudentRequirements sr, ListView<String> threadsLV) {
        var t = new TextInputDialog("");
        t.setTitle("New Post");
        t.setHeaderText("Enter a thread (e.g., General, Homework, Projects)");
        t.setContentText("Thread:");
        var threadOpt = t.showAndWait();
        if (threadOpt.isEmpty()) return;

        var d = new TextArea();
        d.setPrefRowCount(8);
        var dlg = new Dialog<String>();
        dlg.setTitle("Post Content");
        dlg.setHeaderText("What would you like to post?");
        var OK = ButtonType.OK;
        dlg.getDialogPane().getButtonTypes().addAll(OK, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(d);
        dlg.setResultConverter(bt -> bt == OK ? d.getText() : null);
        var content = dlg.showAndWait().orElse(null);
        if (content == null || content.isBlank()) return;

        sr.createPost(user.getUserName(), content.trim(), threadOpt.get().trim());
        refreshThreads(sr, threadsLV, null, null, null);
        threadsLV.getSelectionModel().select(threadOpt.get().trim());
    }

    static void doReply(User user, StudentRequirements sr,
                        ListView<Post> postsLV, ListView<Reply> repliesLV, TextArea detailsTA) {
        Post p = postsLV.getSelectionModel().getSelectedItem();
        if (p == null) { info("Reply", "Select a post first."); return; }

        var d = new TextInputDialog("");
        d.setTitle("Reply");
        d.setHeaderText("Enter your reply to the selected post");
        d.setContentText("Reply:");
        var text = d.showAndWait();
        if (text.isEmpty() || text.get().isBlank()) return;

        sr.addReply(p.getId(), user.getUserName(), text.get().trim());
        loadRepliesForPost(sr, p, repliesLV);
        showPostDetails(p, detailsTA);
    }

    static void doDeleteMyPost(User user, StudentRequirements sr,
                               ListView<Post> postsLV, ListView<String> threadsLV,
                               ListView<Reply> repliesLV, TextArea detailsTA) {
        Post p = postsLV.getSelectionModel().getSelectedItem();
        if (p == null) { info("Delete", "Select a post first."); return; }
        if (!user.getUserName().equals(p.getAuthor())) {
            info("Delete", "You can only delete your own post.");
            return;
        }
        sr.deletePost(p.getId());
        loadPostsForThread(sr, threadsLV.getSelectionModel().getSelectedItem(), postsLV, repliesLV, detailsTA);
        repliesLV.setItems(FXCollections.observableArrayList());
        detailsTA.clear();
    }

    // --- Data helpers ---
    static void refreshThreads(StudentRequirements sr,
                               ListView<String> threadsLV,
                               ListView<Post> postsLV,
                               ListView<Reply> repliesLV,
                               TextArea detailsTA) {

        var names = sr.listAllPosts()
                .stream()
                .map(Post::getThread)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (!names.contains("General"))  names.add(0, "General");
        if (!names.contains("Homework")) names.add("Homework");
        if (!names.contains("Projects")) names.add("Projects");

        threadsLV.setItems(FXCollections.observableArrayList(names));

        if (!names.isEmpty()) {
            if (threadsLV.getSelectionModel().getSelectedItem() == null) {
                threadsLV.getSelectionModel().select(0);
            }
            loadPostsForThread(sr, threadsLV.getSelectionModel().getSelectedItem(),
                    postsLV, repliesLV, detailsTA);
        }
    }

    static void loadPostsForThread(StudentRequirements sr, String thread,
                                   ListView<Post> postsLV, ListView<Reply> repliesLV, TextArea detailsTA) {
        if (postsLV == null) return; // allows refreshThreads() to be called early

        if (thread == null) {
            postsLV.setItems(FXCollections.observableArrayList());
            if (repliesLV != null) repliesLV.setItems(FXCollections.observableArrayList());
            if (detailsTA != null) detailsTA.clear();
            return;
        }

        var posts = sr.searchPosts("", thread);
        postsLV.setItems(FXCollections.observableArrayList(posts));
        if (repliesLV != null) repliesLV.setItems(FXCollections.observableArrayList());
        if (detailsTA != null) detailsTA.clear();

        postsLV.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Post p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) setText(null);
                else setText("#" + p.getId() + " • " + p.getAuthor() + " — " +
                        (p.getContent().length() > 60 ? p.getContent().substring(0,60) + "…" : p.getContent()));
            }
        });
        if (repliesLV != null) {
            repliesLV.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Reply r, boolean empty) {
                    super.updateItem(r, empty);
                    if (empty || r == null) setText(null);
                    else setText(r.getAuthor() + ": " +
                            (r.getContent().length() > 50 ? r.getContent().substring(0,50) + "…" : r.getContent()));
                }
            });
        }
    }

    static void loadRepliesForPost(StudentRequirements sr, Post p, ListView<Reply> repliesLV) {
        if (repliesLV == null) return;
        if (p == null) {
            repliesLV.setItems(FXCollections.observableArrayList());
            return;
        }
        var reps = sr.listReplies(p.getId(), ViewStudentBoard.theUser.getUserName(), false);
        repliesLV.setItems(FXCollections.observableArrayList(reps));
    }

    static void showPostDetails(Post p, TextArea detailsTA) {
        if (detailsTA == null) return;
        if (p == null) { detailsTA.clear(); return; }
        var sb = new StringBuilder();
        sb.append("Post #").append(p.getId())
          .append(" in ").append(p.getThread())
          .append(" by ").append(p.getAuthor())
          .append(" @ ").append(p.getCreatedAt()).append("\n\n")
          .append(p.getContent());
        detailsTA.setText(sb.toString());
    }	

    static void seedDemoIfEmpty(StudentRequirements sr, ListView<String> threadsLV) {
        if (!sr.listAllPosts().isEmpty()) return;
        refreshThreads(sr, threadsLV, null, null, null);
    }

    // Small helper
    private static void info(String t, String m){
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(t); a.setContentText(m); a.showAndWait();
    }
}
