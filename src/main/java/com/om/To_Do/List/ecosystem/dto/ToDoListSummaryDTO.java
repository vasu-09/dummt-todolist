package com.om.To_Do.List.ecosystem.dto;

import lombok.Data;
import java.util.List;

@Data
public class ToDoListSummaryDTO {
    private String title;
    private List<ToDoItemDTO> items;

    public ToDoListSummaryDTO() {
    }

    public ToDoListSummaryDTO(String title, List<ToDoItemDTO> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ToDoItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ToDoItemDTO> items) {
        this.items = items;
    }
}
