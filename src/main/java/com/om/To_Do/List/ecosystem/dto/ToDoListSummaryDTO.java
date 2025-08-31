package com.om.To_Do.List.ecosystem.dto;

import lombok.Data;
import java.util.List;

@Data
public class ToDoListSummaryDTO {
    private String title;
    private List<ToDoItemDTO> items;
    private String listType;

    public ToDoListSummaryDTO() {
    }

    public ToDoListSummaryDTO(String title, List<ToDoItemDTO> items, String listType) {
        this.title = title;
        this.items = items;
        this.listType = listType;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
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
