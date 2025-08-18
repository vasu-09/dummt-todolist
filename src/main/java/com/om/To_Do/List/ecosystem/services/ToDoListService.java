package com.om.To_Do.List.ecosystem.services;

import com.om.To_Do.List.ecosystem.client.UserServiceClient;
import com.om.To_Do.List.ecosystem.dto.*;
import com.om.To_Do.List.ecosystem.model.ListRecipient;
import com.om.To_Do.List.ecosystem.model.ToDoItem;
import com.om.To_Do.List.ecosystem.model.ToDoList;
import com.om.To_Do.List.ecosystem.repository.ListRecipientRepository;
import com.om.To_Do.List.ecosystem.repository.ToDoItemRepository;
import com.om.To_Do.List.ecosystem.repository.ToDoListRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToDoListService {

    @Autowired
    private ToDoListRepository toDoListRepository;
    @Autowired
    private ToDoItemRepository toDoItemRepository;
    @Autowired
    private ListRecipientRepository listRecipientRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    // injected via @RequiredArgsConstructor
    private  PaymentService paymentService;

    // Step 1: Create list without recipients
    @Transactional
    public ToDoList createList(CreateListRequest request) throws AccessDeniedException {
        // 🔒 Require active subscription for this endpoint
        boolean active = paymentService.isSubscriptionActive(request.getCreatedByUserId());
        if (!active) {
            throw new AccessDeniedException(
                    "Subscription required to create priced lists. " +
                            "Use /api/lists/checklist for simple item-only lists.");
        }

        ToDoList list = new ToDoList();
        list.setCreatedByUserId(request.getCreatedByUserId());
        list.setTitle(request.getTitle());
        list.setCreatedAt(LocalDateTime.now());

        toDoListRepository.save(list);

        List<ToDoItem> items = request.getItems().stream().map(dto -> {
            ToDoItem item = new ToDoItem();
            item.setItemName(dto.getItemName());
            item.setQuantity(dto.getQuantity());
            item.setPriceText(dto.getPriceText());
            item.setSubQuantitiesJson(dto.getSubQuantitiesJson());
            item.setList(list);
            return item;
        }).toList();

        toDoItemRepository.saveAll(items);
        return list;
    }

    // --- FREE (and also allowed for paid) simple checklists ---
    @Transactional
    public ToDoList createChecklist(CreateChecklistRequest request) {
        ToDoList list = new ToDoList();
        list.setCreatedByUserId(request.getCreatedByUserId());
        list.setTitle(request.getTitle());
        list.setCreatedAt(LocalDateTime.now());

        toDoListRepository.save(list);

        List<ToDoItem> items = request.getItems().stream().map(dto -> {
            ToDoItem item = new ToDoItem();
            item.setItemName(dto.getItemName());
            // force simple fields
            item.setQuantity(null);
            item.setPriceText(null);
            item.setSubQuantitiesJson(null);
            item.setList(list);
            return item;
        }).toList();

        toDoItemRepository.saveAll(items);
        return list;
    }

    // Step 2: Add recipients
    @Transactional
    public void addRecipientsByPhone(Long listId, List<String> phoneNumbers) {
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        // Fetch matching user IDs from Authentication Service
        List<Long> userIds = userServiceClient
                .getUserIdsByPhoneNumbers(phoneNumbers)
                .getBody();
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("No users found for given phone numbers");
        }

        Long creatorUserId = list.getCreatedByUserId();

        // Filter out self
        userIds = userIds.stream()
                .filter(id -> !id.equals(creatorUserId))
                .toList();

        if (userIds.isEmpty()) return;

        // Fetch existing recipients to avoid duplicates
        List<Long> existingRecipientIds = listRecipientRepository
                .findByListId(listId).stream()
                .map(ListRecipient::getRecipientUserId)
                .toList();

        // Build new recipient entities
        List<ListRecipient> newRecipients = userIds.stream()
                .filter(id -> !existingRecipientIds.contains(id))
                .map(id -> {
                    ListRecipient r = new ListRecipient();
                    r.setList(list);
                    r.setRecipientUserId(id);
                    return r;
                })
                .toList();

        if (newRecipients.isEmpty()) return;

        // 1) Persist all new recipients
        listRecipientRepository.saveAll(newRecipients);

        // 2) Publish the event for downstream Notification-Service
        List<Long> newUserIds = newRecipients.stream()
                .map(ListRecipient::getRecipientUserId)
                .toList();

        RecipientAddedEvent evt = new RecipientAddedEvent(
                listId,
                list.getTitle(),
                creatorUserId,
                newUserIds
        );
        eventPublisher.publishEvent(evt);
    }

    public void removeRecipientFromList(Long listId, Long recipientUserId) {
        ListRecipient recipient = listRecipientRepository.findByListIdAndRecipientUserId(listId, recipientUserId)
                .orElseThrow(() -> new RuntimeException("You are not a recipient of this list"));

        listRecipientRepository.delete(recipient);
    }

    public ToDoList updateList(Long listId, Long userId, UpdateListRequest request) throws AccessDeniedException {
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        // Update title if provided
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            list.setTitle(request.getTitle());
        }

        if (!list.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Only the meeting creator can update this meeting.");
        }
        // Delete old items and add new ones
        toDoItemRepository.deleteByListId(listId);

        List<ToDoItem> updatedItems = request.getItems().stream().map(dto -> {
            ToDoItem item = new ToDoItem();
            item.setItemName(dto.getItemName());
            item.setQuantity(dto.getQuantity());
            item.setPriceText(dto.getPriceText());
            item.setSubQuantitiesJson(dto.getSubQuantitiesJson());
            item.setList(list);
            return item;
        }).toList();

        toDoItemRepository.saveAll(updatedItems);

        return toDoListRepository.save(list);
    }

    public void deleteList(Long listId) {
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        toDoListRepository.delete(list);
    }

    public List<ToDoListTitleDTO> getListsByCreator(Long userId) {
        List<ToDoList> lists = toDoListRepository.findByCreatedByUserId(userId);

        return lists.stream().map(list -> {
            ToDoListTitleDTO dto = new ToDoListTitleDTO();
            dto.setId(list.getId());
            dto.setTitle(list.getTitle());
            return dto;
        }).toList();
    }

    public ToDoListSummaryDTO getListByIdAndCreator(Long listId, Long userId) {
        ToDoList list = toDoListRepository.findByIdAndCreatedByUserId(listId, userId)
                .orElseThrow(() -> new RuntimeException("List not found or you are not the owner"));

        ToDoListSummaryDTO dto = new ToDoListSummaryDTO();
        dto.setTitle(list.getTitle());

        List<ToDoItemDTO> itemDTOs = list.getItems().stream().map(item -> {
            ToDoItemDTO itemDTO = new ToDoItemDTO();
            itemDTO.setItemName(item.getItemName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPriceText(item.getPriceText());
            itemDTO.setSubQuantitiesJson(item.getSubQuantitiesJson());
            return itemDTO;
        }).toList();

        dto.setItems(itemDTOs);
        return dto;
    }

    public List<ToDoListTitleDTO> getSharedListTitles(Long creatorId, Long recipientId) {
        List<ToDoList> lists = listRecipientRepository.findListsByCreatorAndRecipient(creatorId, recipientId);

        return lists.stream().map(list -> {
            ToDoListTitleDTO dto = new ToDoListTitleDTO();
            dto.setId(list.getId());
            dto.setTitle(list.getTitle());
            return dto;
        }).toList();
    }

    public ToDoListSummaryDTO getSharedList(Long listId, Long creatorId, Long recipientId) {
        ToDoList list = listRecipientRepository.findSharedList(listId, creatorId, recipientId)
                .orElseThrow(() -> new RuntimeException("List not found or not shared with this user"));

        ToDoListSummaryDTO dto = new ToDoListSummaryDTO();
        dto.setTitle(list.getTitle());

        List<ToDoItemDTO> itemDTOs = list.getItems().stream().map(item -> {
            ToDoItemDTO itemDTO = new ToDoItemDTO();
            itemDTO.setItemName(item.getItemName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPriceText(item.getPriceText());
            itemDTO.setSubQuantitiesJson(item.getSubQuantitiesJson());
            return itemDTO;
        }).toList();

        dto.setItems(itemDTOs);
        return dto;
    }

    public ListRecipientsDTO getRecipientsForList(Long listId, Long creatorId) {
        ToDoList list = toDoListRepository.findByIdAndCreatedByUserId(listId, creatorId)
                .orElseThrow(() -> new RuntimeException("List not found or unauthorized"));

        List<Long> recipientIds = listRecipientRepository.findRecipientIdsByListIdAndCreatorId(listId, creatorId);

        ListRecipientsDTO dto = new ListRecipientsDTO();
        dto.setListId(list.getId());
        dto.setTitle(list.getTitle());
        dto.setRecipientUserIds(recipientIds);

        return dto;
    }

    public void deleteRecipientByPhone(Long listId, String phoneNumber, Long creatorId) throws AccessDeniedException {
        ToDoList list = toDoListRepository.findByIdAndCreatedByUserId(listId, creatorId)
                .orElseThrow(() -> new RuntimeException("List not found or unauthorized"));

        Long userId = userServiceClient.getUseridByPhoneNumber(phoneNumber).getBody();

        if (!list.getCreatedByUserId().equals(creatorId)) {
            throw new AccessDeniedException("Only the meeting creator can update this meeting.");
        }
        if (userId == null) {
            throw new RuntimeException("No user found with this phone number");
        }

        Long userIdToDelete = userId;

        ListRecipient recipient = listRecipientRepository
                .findByListIdAndRecipientUserId(listId, userIdToDelete)
                .orElseThrow(() -> new RuntimeException("User is not a recipient of this list"));

        listRecipientRepository.delete(recipient);

        RecipientsRemovedFromListEvent evt = new RecipientsRemovedFromListEvent(
                listId,
                list.getTitle(),
                userId,
                creatorId
        );
        eventPublisher.publishEvent(evt);
    }

    public ToDoItem updateItem(Long listId, Long itemId, Long userId, UpdateItemRequest req) throws AccessDeniedException {

        // 1) verify list exists
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        boolean active = paymentService.isSubscriptionActive(list.getCreatedByUserId());
        if (!active) {
            throw new AccessDeniedException(
                    "Subscription required to create priced lists. " +
                            "Use /api/lists/checklist for simple item-only lists.");
        }
        // 2) load the item
        ToDoItem item = toDoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // 3) ensure it belongs to the right list
        if (!item.getList().getId().equals(listId)) {
            throw new RuntimeException("Item does not belong to list " + listId);
        }

        if (!list.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Only the meeting creator can update this meeting.");
        }

        // 4) apply updates if provided
        if (req.getItemName() != null)       item.setItemName(req.getItemName());
        if (req.getQuantity() != null)       item.setQuantity(req.getQuantity());
        if (req.getPriceText() != null)      item.setPriceText(req.getPriceText());
        if (req.getSubQuantitiesJson() != null) {
            item.setSubQuantitiesJson(req.getSubQuantitiesJson());
        }

        // 5) persist and return
        return toDoItemRepository.save(item);
    }

    /**
     * Delete a single ToDoItem from a list.
     */
    public void deleteItem(Long listId, Long itemId, Long userId) throws AccessDeniedException {

        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        boolean active = paymentService.isSubscriptionActive(list.getCreatedByUserId());
        if (!active) {
            throw new AccessDeniedException(
                    "Subscription required to create priced lists. " +
                            "Use /api/lists/checklist for simple item-only lists.");
        }

        if (!list.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Only the meeting creator can update this meeting.");
        }

        ToDoItem item = toDoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getList().getId().equals(listId)) {
            throw new RuntimeException("Item does not belong to list " + listId);
        }

        toDoItemRepository.delete(item);
    }

    @Transactional
    public ToDoItem updateChecklistItem(Long listId, Long itemId, Long userId, UpdateChecklistItemRequest req)
            throws AccessDeniedException {

        // 1) verify list exists
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        // 2) only creator can update
        if (!list.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Only the list creator can update this list.");
        }

        // 3) load item & ensure it belongs
        ToDoItem item = toDoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getList().getId().equals(listId)) {
            throw new RuntimeException("Item does not belong to list " + listId);
        }

        // 4) apply checklist-only fields
        if (req.getItemName() != null) item.setItemName(req.getItemName());

        // Hard-enforce checklist shape (strip any priced fields if they existed)
        item.setQuantity(null);
        item.setPriceText(null);
        item.setSubQuantitiesJson(null);

        return toDoItemRepository.save(item);
    }

    // ✅ CHECKLIST: Delete (no subscription check)
    @Transactional
    public void deleteChecklistItem(Long listId, Long itemId, Long userId) throws AccessDeniedException {

        // 1) verify list exists
        ToDoList list = toDoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        // 2) only creator can delete
        if (!list.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("Only the list creator can update this list.");
        }

        // 3) load item & ensure it belongs
        ToDoItem item = toDoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        if (!item.getList().getId().equals(listId)) {
            throw new RuntimeException("Item does not belong to list " + listId);
        }

        // 4) delete
        toDoItemRepository.delete(item);
    }
}
