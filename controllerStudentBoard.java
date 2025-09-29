package guiStudentBoard;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * controllerStudentBoard
 *
 * In-memory model + service logic for the Student Board.
 * Works with ViewStudentBoard. No database—maps only (fine for demo/testing).
 *
 * Public API consumed by ViewStudentBoard:
 *  - Set<String> getAllowedThreads()
 *  - Post createPost(String author, String content, String thread)
 *  - boolean deletePost(int postId)
 *  - Reply addReply(int postId, String author, String content)
 *  - List<Map<String,Object>> listPostSummaries(String username, boolean othersOnly)
 *  - List<Post> listUnreadPosts(String username)
 *  - List<Post> searchPosts(String keyword, String thread)
 *  - void markPostRead(int postId, String username)
 *  - List<Reply> listReplies(int postId, String username, boolean unreadOnly)
 *  - Map<String,Object> getPostSummary(Post post, String username)
 *  - String getReplyDisplayContent(int replyId)
 */
public class controllerStudentBoard {

    // ------------------ Thread policy ------------------
    // Predefined threads; students cannot create/edit/delete threads.
    private static final Set<String> ALLOWED_THREADS = new LinkedHashSet<>(Arrays.asList(
        "General", "Homework", "Projects", "Help", "Announcements"
    ));

    public Set<String> getAllowedThreads() {
        return ALLOWED_THREADS;
    }

    // ------------------ Model classes ------------------

    public static class Reply {
        private final int id;
        private final String author;
        private final String content;
        private final LocalDateTime createdAt;
        private final int parentPostId;

        // Track readers of this reply (student usernames)
        private final Set<String> readers = new HashSet<>();

        public Reply(int id, String author, String content, int parentPostId) {
            this.id = id;
            this.author = author;
            this.content = content;
            this.parentPostId = parentPostId;
            this.createdAt = LocalDateTime.now();
        }

        public void markRead(String username) { readers.add(username); }
        public boolean isReadBy(String username) { return readers.contains(username); }

        public int getId() { return id; }
        public String getAuthor() { return author; }
        public String getContent() { return content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public int getParentPostId() { return parentPostId; }
    }

    public static class Post {
        private final int id;
        private final String author;
        private final String content;       // keep original content for search
        private final String thread;
        private final LocalDateTime createdAt;
        private final List<Reply> replies = new ArrayList<>();

        // Track readers of this post
        private final Set<String> readers = new HashSet<>();
        private boolean deleted = false;

        public Post(int id, String author, String content, String thread) {
            this.id = id;
            this.author = author;
            this.content = content;
            String t = (thread == null || thread.isBlank()) ? "General" : thread.trim();
            this.thread = ALLOWED_THREADS.contains(t) ? t : "General";
            this.createdAt = LocalDateTime.now();
        }

        public void markRead(String username) { readers.add(username); }
        public boolean isReadBy(String username) { return readers.contains(username); }

        public void addReply(Reply reply) { replies.add(reply); }

        public List<Reply> getReplies() { return replies; }
        public int getId() { return id; }
        public String getAuthor() { return author; }
        public String getThread() { return thread; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        /** Returns visible content (tombstone if deleted). */
        public String getContent() { return deleted ? "[deleted]" : content; }

        /** Returns original (non-tombstone) text; used internally for search. */
        public String getRawContent() { return content; }

        public boolean isDeleted() { return deleted; }
        public void setDeleted() { this.deleted = true; }
    }

    // ------------------ Store ------------------
    private final Map<Integer, Post> posts = new LinkedHashMap<>();
    private final Map<Integer, Reply> replies = new LinkedHashMap<>();
    private final AtomicInteger postIdCounter = new AtomicInteger(1);
    private final AtomicInteger replyIdCounter = new AtomicInteger(1);

    // ------------------ API methods ------------------

    /** Create a new post (defaults to "General" thread if invalid). */
    public Post createPost(String author, String content, String thread) {
        int id = postIdCounter.getAndIncrement();
        Post post = new Post(id, author, content, thread);
        posts.put(id, post);
        return post;
    }

    /**
     * Delete a post by id (soft delete). Replies are preserved; viewers see "[deleted]".
     * Returns true if the post existed and is now marked deleted.
     * (Auth check can be added if you pass current user into this method.)
     */
    public boolean deletePost(int postId) {
        Post post = posts.get(postId);
        if (post == null) return false;
        post.setDeleted();
        return true;
    }

    /** Add a reply to a post. Returns the new reply or null if post not found. */
    public Reply addReply(int postId, String author, String content) {
        Post post = posts.get(postId);
        if (post == null) return null;
        int id = replyIdCounter.getAndIncrement();
        Reply reply = new Reply(id, author, content, postId);
        replies.put(id, reply);
        post.addReply(reply);
        return reply;
    }

    /** List all posts (newest first). */
    public List<Post> listAllPosts() {
        return posts.values().stream()
            .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    /** Search posts by keyword (case-insensitive) and optional thread. */
    public List<Post> searchPosts(String keyword, String thread) {
        String k = (keyword == null) ? "" : keyword.toLowerCase();
        return posts.values().stream()
            .filter(p -> thread == null || thread.isBlank() || p.getThread().equalsIgnoreCase(thread))
            .filter(p -> p.getRawContent().toLowerCase().contains(k))
            .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    /** Mark a post as read by username. */
    public void markPostRead(int postId, String username) {
        Post post = posts.get(postId);
        if (post != null) post.markRead(username);
    }

    /** Mark a reply as read by username. */
    public void markReplyRead(int replyId, String username) {
        Reply reply = replies.get(replyId);
        if (reply != null) reply.markRead(username);
    }

    /** Posts the user has NOT read (newest first). */
    public List<Post> listUnreadPosts(String username) {
        return posts.values().stream()
            .filter(p -> !p.isReadBy(username))
            .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    /** List replies on a post; unreadOnly filters using the username. */
    public List<Reply> listReplies(int postId, String username, boolean unreadOnly) {
        Post post = posts.get(postId);
        if (post == null) return List.of();
        return post.getReplies().stream()
            .filter(r -> !unreadOnly || !r.isReadBy(username))
            .sorted(Comparator.comparing(Reply::getCreatedAt))
            .collect(Collectors.toList());
    }

    /** Compose a summary map for a post suitable for a TableView row. */
    public Map<String, Object> getPostSummary(Post post, String username) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", post.getId());
        summary.put("author", post.getAuthor());
        summary.put("thread", post.getThread());
        summary.put("content", post.getContent());
        summary.put("createdAt", post.getCreatedAt());
        summary.put("deleted", post.isDeleted());
        summary.put("replies", post.getReplies().size());
        long unreadReplies = post.getReplies().stream().filter(r -> !r.isReadBy(username)).count();
        summary.put("unreadReplies", unreadReplies);
        summary.put("read", post.isReadBy(username));
        return summary;
    }

    /** Convenience: list summaries for feed UI. */
    public List<Map<String, Object>> listPostSummaries(String username, boolean othersOnly) {
        return (othersOnly
                ? listAllPosts().stream().filter(p -> !p.getAuthor().equals(username))
                : listAllPosts().stream())
            .map(p -> getPostSummary(p, username))
            .collect(Collectors.toList());
    }

    /** When viewing a reply, append a note if the parent post was deleted. */
    public String getReplyDisplayContent(int replyId) {
        Reply r = replies.get(replyId);
        if (r == null) return "";
        Post parent = posts.get(r.getParentPostId());
        String suffix = (parent != null && parent.isDeleted())
                ? "  (original post has been deleted)" : "";
        return r.getContent() + suffix;
    }

    // ---------- (Optional) Demo main ----------
    public static void main(String[] args) {
        controllerStudentBoard c = new controllerStudentBoard();
        Post p1 = c.createPost("Alice", "How do I fix error X?", "General");
        c.addReply(p1.getId(), "Bob", "Try cleaning and rebuilding.");
        c.addReply(p1.getId(), "Charlie", "Check JDK version.");

        c.markPostRead(p1.getId(), "Cherry");

        System.out.println("=== Summaries ===");
        c.listPostSummaries("Cherry", false).forEach(System.out::println);

        System.out.println("\n=== Search 'fix' ===");
        c.searchPosts("fix", null).forEach(p -> System.out.println(p.getContent()));

        System.out.println("\n=== Unread for Cherry ===");
        c.listUnreadPosts("Cherry").forEach(p -> System.out.println(p.getContent()));

        System.out.println("\n=== Delete post ===");
        c.deletePost(p1.getId());
        System.out.println(c.getPostSummary(p1, "Cherry"));
        System.out.println("Reply view: " + c.getReplyDisplayContent(1));
    }
}
