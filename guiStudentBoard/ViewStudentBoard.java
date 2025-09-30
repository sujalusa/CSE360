package guiStudentBoard;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Student Board view (UI) for the "student requirements".
 * Works with controllerStudentBoard (in-memory model/logic).
 */
public class ViewStudentBoard extends BorderPane {

    private final controllerStudentBoard controller;
    private final String currentUser;

    // Top controls
    private final javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
    private final ComboBox<String> threadFilter = new ComboBox<>();
    private final Button btnRefresh   = new Button("Refresh");
    private final Button btnShowUnread= new Button("Show Unread");

    // Posts table
    private final TableView<Map<String, Object>> postsTable = new TableView<>();
    private final ObservableList<Map<String, Object>> postsData = FXCollections.observableArrayList();

    // Action buttons
    private final Button btnNewPost  = new Button("New Post");
    private final Button btnReply    = new Button("Reply");
    private final Button btnMarkRead = new Button("Mark Read");
    private final Button btnDelete   = new Button("Delete");

    // Replies panel
    private final ListView<String> repliesList = new ListView<>();
    private final CheckBox chkUnreadOnly = new CheckBox("Unread only");

    public ViewStudentBoard(controllerStudentBoard controller, String currentUser) {
        this.controller = controller;
        this.currentUser = currentUser;

        setPadding(new Insets(12));
        setTop(buildTopBar());
        setCenter(buildCenter());
        setRight(buildRight());

        configurePostsTable();
        wireActions();

        // Initial load
        refreshPosts(false);
    }

    // ===== UI builders =====

    private Node buildTopBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(0, 0, 10, 0));

        searchField.setPromptText("Search keyword… (press Enter)");
        searchField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) doSearch(); });

        // Threads from controller (must return Set<String>)
        List<String> threads = new ArrayList<>(controller.getAllowedThreads());
        Collections.sort(threads);
        threads.add(0, "All threads");
        threadFilter.setItems(FXCollections.observableArrayList(threads));
        threadFilter.getSelectionModel().selectFirst();

        bar.getChildren().addAll(
                new Label("Search:"), searchField,
                new Label("Thread:"), threadFilter,
                btnRefresh, btnShowUnread
        );
        return bar;
    }

    private Node buildCenter() {
        VBox v = new VBox(8);
        postsTable.setItems(postsData);

        // action buttons row
        HBox actions = new HBox(8, btnNewPost, btnReply, btnMarkRead, btnDelete);
        actions.setPadding(new Insets(6, 0, 0, 0));

        v.getChildren().addAll(postsTable, actions);
        VBox.setVgrow(postsTable, Priority.ALWAYS);
        return v;
    }

    private Node buildRight() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 0, 0, 10));
        box.setPrefWidth(380);

        Label title = new Label("Replies");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        chkUnreadOnly.setSelected(false);
        chkUnreadOnly.setOnAction(e -> loadRepliesForSelected());

        box.getChildren().addAll(title, chkUnreadOnly, repliesList);
        VBox.setVgrow(repliesList, Priority.ALWAYS);
        return box;
    }

    // ===== Table config =====
    private void configurePostsTable() {
        // Define columns once
        TableColumn<Map<String, Object>, Object> cId      = makeCol("ID",      "id",            60);
        TableColumn<Map<String, Object>, Object> cAuthor  = makeCol("Author",  "author",       120);
        TableColumn<Map<String, Object>, Object> cThread  = makeCol("Thread",  "thread",       110);
        TableColumn<Map<String, Object>, Object> cContent = makeCol("Content", "content",      320);
        TableColumn<Map<String, Object>, Object> cReplies = makeCol("Replies", "replies",       70);
        TableColumn<Map<String, Object>, Object> cUnread  = makeCol("Unread",  "unreadReplies", 70);
        TableColumn<Map<String, Object>, Object> cRead    = makeCol("Read?",   "read",          60);

        postsTable.getColumns().setAll(cId, cAuthor, cThread, cContent, cReplies, cUnread, cRead);

        // Selection listener
        postsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = newV != null;
            btnReply.setDisable(!hasSel);
            btnMarkRead.setDisable(!hasSel);
            btnDelete.setDisable(!hasSel);
            loadRepliesForSelected();
        });

        // Initial state
        btnReply.setDisable(true);
        btnMarkRead.setDisable(true);
        btnDelete.setDisable(true);
    }

    private TableColumn<Map<String, Object>, Object> makeCol(String title, String key, int width) {
        TableColumn<Map<String, Object>, Object> c = new TableColumn<>(title);
        c.setMinWidth(width);
        c.setPrefWidth(width);
        c.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue() == null ? null : param.getValue().get(key))
        );
        return c;
    }

    // ===== Wiring =====

    private void wireActions() {
        btnRefresh.setOnAction(e -> refreshPosts(false));
        btnShowUnread.setOnAction(e -> refreshPosts(true));
        threadFilter.setOnAction(e -> doSearch());

        btnNewPost.setOnAction(e -> showNewPostDialog());
        btnReply.setOnAction(e -> showReplyDialog());
        btnMarkRead.setOnAction(e -> markSelectedPostRead());
        btnDelete.setOnAction(e -> deleteSelectedPost());
    }

    // ===== Data ops =====

    private void refreshPosts(boolean unreadOnly) {
        List<Map<String, Object>> rows;
        if (unreadOnly) {
            rows = controller.listUnreadPosts(currentUser).stream()
                    .map(p -> controller.getPostSummary(p, currentUser))
                    .collect(Collectors.toList());
        } else {
            rows = controller.listPostSummaries(currentUser, false);
        }
        postsData.setAll(rows);
        loadRepliesForSelected();
    }

    private void doSearch() {
        String keyword = Optional.ofNullable(searchField.getText()).orElse("").trim();
        String selectedThread = threadFilter.getSelectionModel().getSelectedItem();
        String thread = (selectedThread == null || selectedThread.equals("All threads")) ? null : selectedThread;

        List<Map<String, Object>> rows = controller.searchPosts(keyword, thread).stream()
                .map(p -> controller.getPostSummary(p, currentUser))
                .collect(Collectors.toList());

        postsData.setAll(rows);
        loadRepliesForSelected();
    }

    private Map<String, Object> getSelectedRow() {
        return postsTable.getSelectionModel().getSelectedItem();
    }

    private int getSelectedPostId() {
        Map<String, Object> row = getSelectedRow();
        return (row == null) ? -1 : (int) row.get("id");
    }

    private void loadRepliesForSelected() {
        repliesList.getItems().clear();
        int postId = getSelectedPostId();
        if (postId <= 0) return;

        boolean unreadOnly = chkUnreadOnly.isSelected();
        controller.listReplies(postId, currentUser, unreadOnly).forEach(r -> {
            String text = controller.getReplyDisplayContent(r.getId());
            repliesList.getItems().add("#" + r.getId() + " by " + r.getAuthor() + ": " + text);
        });
    }

    // ===== Dialogs / actions =====

    private void showNewPostDialog() {
        Dialog<Pair<String, String>> dlg = new Dialog<>();
        dlg.setTitle("New Post");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea content = new TextArea();
        content.setPromptText("Write your statement or question…");
        content.setPrefRowCount(5);

        ComboBox<String> threadBox = new ComboBox<>();
        threadBox.getItems().addAll(controller.getAllowedThreads());
        threadBox.getSelectionModel().select("General");

        GridPane gp = new GridPane();
        gp.setVgap(8); gp.setHgap(8); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Thread:"), threadBox);
        gp.addRow(1, new Label("Content:"), content);

        dlg.getDialogPane().setContent(gp);
        dlg.setResultConverter(bt -> bt == ButtonType.OK
                ? new Pair<>(threadBox.getValue(), content.getText().trim())
                : null);

        Optional<Pair<String, String>> res = dlg.showAndWait();
        if (res.isPresent()) {
            String thread = res.get().getKey();
            String text = res.get().getValue();
            if (!text.isBlank()) {
                controller.createPost(currentUser, text, thread);
                refreshPosts(false);
            }
        }
    }

    private void showReplyDialog() {
        int postId = getSelectedPostId();
        if (postId <= 0) return;

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reply");
        dlg.setHeaderText("Write a reply to post #" + postId);
        dlg.getEditor().setPromptText("Your reply…");
        dlg.getEditor().setPrefColumnCount(40);

        dlg.showAndWait().ifPresent(text -> {
            String msg = text.trim();
            if (!msg.isBlank()) {
                controller.addReply(postId, currentUser, msg);
                refreshPosts(false);
                postsTable.getSelectionModel().select(postsData.stream()
                        .filter(m -> (int)m.get("id") == postId).findFirst().orElse(null));
                loadRepliesForSelected();
            }
        });
    }

    private void markSelectedPostRead() {
        int postId = getSelectedPostId();
        if (postId <= 0) return;
        controller.markPostRead(postId, currentUser);
        refreshPosts(false);
        postsTable.getSelectionModel().select(postsData.stream()
                .filter(m -> (int)m.get("id") == postId).findFirst().orElse(null));
    }

    private void deleteSelectedPost() {
        int postId = getSelectedPostId();
        if (postId <= 0) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete your post #" + postId + "?\nReplies will remain; the post will show as [deleted].",
                ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirm Delete");
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean ok = controller.deletePost(postId); // one-arg signature
            if (!ok) {
                new Alert(Alert.AlertType.ERROR,
                        "Delete failed. You can only delete posts you authored.").showAndWait();
            }
            refreshPosts(false);
        }
    }
}
