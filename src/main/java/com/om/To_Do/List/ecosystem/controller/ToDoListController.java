package com.om.To_Do.List.ecosystem.controller;


import com.om.To_Do.List.ecosystem.dto.*;
import com.om.To_Do.List.ecosystem.model.ToDoItem;
import com.om.To_Do.List.ecosystem.services.ToDoListService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ToDoListController {

    @Autowired
    private  ToDoListService toDoListService;

    // Step 1: Create list without recipients
    @PostMapping
    public ResponseEntity<?> createList(@RequestBody CreateListRequest request) throws AccessDeniedException {
        return ResponseEntity.ok(toDoListService.createList(request));
    }

    // Step 2: Add recipients later
    @PostMapping("/{listId}/recipients")
    public ResponseEntity<?> addRecipients(@PathVariable Long listId, @RequestBody AddRecipientsByPhoneRequest request) throws IllegalAccessException {
        if(request.getPhoneNumbers() == null || request.getPhoneNumbers().isEmpty()){
            throw new IllegalAccessException("phoneNumbers must not be null or empty.");
        }
        toDoListService.addRecipientsByPhone(listId, request.getPhoneNumbers());
        return ResponseEntity.ok("Recipients added successfully.");
    }

    @DeleteMapping("/{listId}/recipients-by-phone/{creatorId}")
    public ResponseEntity<?> deleteRecipientByPhone(
            @PathVariable Long listId,
            @PathVariable Long creatorId,
            @RequestBody String phoneNumber) throws AccessDeniedException {

        toDoListService.deleteRecipientByPhone(listId, phoneNumber.trim().replace("\"", ""), creatorId);
        return ResponseEntity.ok("Recipient removed successfully.");
    }

    @PostMapping("/checklist")
    public ResponseEntity<?> createChecklist(@RequestBody CreateChecklistRequest request) {
        return ResponseEntity.ok(toDoListService.createChecklist(request));
    }

    @DeleteMapping("/{listId}/leave")
    public ResponseEntity<?> leaveSharedList(@PathVariable Long listId, @RequestBody LeaveListRequest request) {
        toDoListService.removeRecipientFromList(listId, request.getRecipientUserId());
        return ResponseEntity.ok("You have left the list.");
    }

    @PutMapping("/{listId}")
    public ResponseEntity<?> updateList(@PathVariable Long listId,  @RequestHeader("X-User-Id") String userId, @RequestBody UpdateListRequest request) throws AccessDeniedException {
        return ResponseEntity.ok(toDoListService.updateList(listId, Long.valueOf(userId), request));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<?> deleteList(@PathVariable Long listId, @RequestHeader("X-User-Id") String userId ) throws AccessDeniedException {
        toDoListService.deleteList(listId, Long.valueOf(userId));
        return ResponseEntity.ok("List deleted successfully.");
    }

    @GetMapping("/created")
    public ResponseEntity<List<ToDoListTitleDTO>> getListsByUser(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(toDoListService.getListsByCreator(Long.valueOf(userId)));
    }

    @GetMapping("/{listId}/creator/{userId}")
    public ResponseEntity<ToDoListSummaryDTO> getListByIdAndCreator(
            @PathVariable Long listId,
            @PathVariable Long userId) {

        return ResponseEntity.ok(toDoListService.getListByIdAndCreator(listId, userId));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<ToDoListTitleDTO>> getSharedLists(
            @RequestParam Long creatorId,
            @RequestParam Long recipientId) {

        return ResponseEntity.ok(toDoListService.getSharedListTitles(creatorId, recipientId));
    }

    @GetMapping("/{listId}/shared")
    public ResponseEntity<ToDoListSummaryDTO> getSharedList(
            @PathVariable Long listId,
            @RequestParam Long creatorId,
            @RequestParam Long recipientId) {

        return ResponseEntity.ok(toDoListService.getSharedList(listId, creatorId, recipientId));
    }

    @GetMapping("/{listId}/recipients")
    public ResponseEntity<ListRecipientsDTO> getRecipientsForList(
            @PathVariable Long listId,
            @RequestParam Long creatorId) {
        return ResponseEntity.ok(toDoListService.getRecipientsForList(listId, creatorId));
    }

    @PutMapping("/{listId}/items/{itemId}")
    public ResponseEntity<ToDoItemRes> updateItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateItemRequest request
    ) throws AccessDeniedException {
        ToDoItemRes updated = toDoListService.updateItem(listId, itemId, Long.valueOf(userId), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete one item from a list.
     */
    @DeleteMapping("/{listId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestHeader("X-User-Id") String userId
    ) throws AccessDeniedException {
        toDoListService.deleteItem(listId, itemId, Long.valueOf(userId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{listId}/checklist/items/{itemId}")
    public ResponseEntity<ToDoItem> updateChecklistItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateChecklistItemRequest request
    ) throws java.nio.file.AccessDeniedException {
        ToDoItem updated = toDoListService.updateChecklistItem(listId, itemId, Long.valueOf(userId), request);
        return ResponseEntity.ok(updated);
    }

    // ✅ CHECKLIST: Delete one item (no subscription check)
    @DeleteMapping("/{listId}/checklist/items/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestHeader("X-User-Id") String userId
    ) throws java.nio.file.AccessDeniedException {
        toDoListService.deleteChecklistItem(listId, itemId, Long.valueOf(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{listId}/sync")
    public ResponseEntity<List<ToDoItem>> getUpdatesSince(
            @PathVariable Long listId,
            @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    ) {
        return ResponseEntity.ok(toDoListService.getUpdatesSince(listId, since));
    }

    @PostMapping("/{listId}/sync")
    public ResponseEntity<SyncResponse> syncOfflineUpdates(
            @PathVariable Long listId,
            @RequestBody SyncRequest request
    ) {
        return ResponseEntity.ok(toDoListService.syncOfflineUpdates(listId, request));
    }



}
