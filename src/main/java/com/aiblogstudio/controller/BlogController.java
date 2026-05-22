package com.aiblogstudio.controller;

import com.aiblogstudio.dto.BlogDetailDto;
import com.aiblogstudio.dto.BlogListDto;
import com.aiblogstudio.dto.BlogResponse;
import com.aiblogstudio.enums.Role;
import com.aiblogstudio.exception.BlogAccessDeniedException;
import com.aiblogstudio.exception.ResourceNotFoundException;
import com.aiblogstudio.model.Blog;
import com.aiblogstudio.model.User;
import com.aiblogstudio.repository.BlogRepository;
import com.aiblogstudio.service.BlogOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogOrchestratorService orchestratorService;
    private final BlogRepository blogRepository;

    /**
     * Fetch all blogs history.
     * Admins see all blogs, regular users see only their own.
     */
    @GetMapping
    public ResponseEntity<List<BlogListDto>> getAllBlogs(@AuthenticationPrincipal User currentUser) {
        List<BlogListDto> blogs;
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            blogs = blogRepository.summarizeAllBlogs();
        } else {
            blogs = blogRepository.summarizeBlogsByUser(currentUser.getId());
        }
        return ResponseEntity.ok(blogs);
    }

    /**
     * Fetch only the current user's blogs.
     */
    @GetMapping("/my")
    public ResponseEntity<List<BlogListDto>> getMyBlogs(@AuthenticationPrincipal User currentUser) {
        List<BlogListDto> blogs = blogRepository.summarizeBlogsByUser(currentUser.getId());
        return ResponseEntity.ok(blogs);
    }

    /**
     * Fetch blog by ID.
     * Users can only view their own blogs, admins can view any.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlogDetailDto> getBlogById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));

        // Check ownership (allow if admin or blog has no owner for backward compatibility)
        if (currentUser.getRole() != Role.ROLE_ADMIN
                && blog.getCreatedBy() != null
                && !blog.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BlogAccessDeniedException();
        }

        return ResponseEntity.ok(BlogDetailDto.fromEntity(blog));
    }

    /**
     * Generates and optionally publishes a blog for the given topic.
     * The blog is automatically associated with the currently logged-in user.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateBlog(
            @RequestParam("topic") String topic,
            @RequestParam("platform") String platform,
            @AuthenticationPrincipal User currentUser) {
        log.info("🎯 Received generation request for topic: '{}' by user: {}", topic, currentUser.getEmail());

        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter a topic."));
        }

        // Run the full autonomous pipeline with user association
        BlogResponse response = orchestratorService.generateAndPublish(topic.trim(), platform, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update a blog by ID.
     * Users can only edit their own blogs, admins can edit any.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlogDetailDto> updateBlog(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal User currentUser) {

        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));

        // Check ownership
        if (currentUser.getRole() != Role.ROLE_ADMIN
                && blog.getCreatedBy() != null
                && !blog.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BlogAccessDeniedException();
        }

        // Apply updates
        if (updates.containsKey("title")) blog.setTitle(updates.get("title"));
        if (updates.containsKey("content")) blog.setContent(updates.get("content"));
        if (updates.containsKey("summary")) blog.setSummary(updates.get("summary"));
        if (updates.containsKey("metaDescription")) blog.setMetaDescription(updates.get("metaDescription"));
        if (updates.containsKey("tags")) blog.setTags(updates.get("tags"));

        blog = blogRepository.save(blog);
        log.info("✏️ Blog {} updated by user: {}", id, currentUser.getEmail());

        return ResponseEntity.ok(BlogDetailDto.fromEntity(blog));
    }

    /**
     * Delete blog by ID.
     * Users can only delete their own blogs, admins can delete any.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlogById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));

        // Check ownership
        if (currentUser.getRole() != Role.ROLE_ADMIN
                && blog.getCreatedBy() != null
                && !blog.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BlogAccessDeniedException();
        }

        blogRepository.delete(blog);
        log.info("🗑️ Blog {} deleted by user: {}", id, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }
}
