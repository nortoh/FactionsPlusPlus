package factionsplusplus.utils;

import java.util.List;

import com.google.common.collect.Lists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Pagination {
    private Component description = null;
    private List<Component> parts;
    private int pageLength = 10;

    public Pagination(Component description, List<Component> parts, int pageLength) {
        this.description = description;
        this.parts = parts;
        this.pageLength = pageLength;
    }

    public Pagination(Component description, List<Component> parts) {
        this.description = description;
        this.parts = parts;
    }

    public Pagination(List<Component> parts, int pageLength) {
        this.parts = parts;
        this.pageLength = pageLength;
    }

    public Pagination(List<Component> parts) {
        this.parts = parts;
    }

    public Pagination() { }

    public void setDescription(Component description) {
        this.description = description;
    }

    public void setParts(List<Component> parts) {
        this.parts = parts;
    }

    public void setPageLength(int length) {
        this.pageLength = length;
    }

    public Component getHeader(int page) {
        return MiniMessage.miniMessage().deserialize(String.format("<color:light_purple><bold><lang:Generic.Pagination:'%s':'%s'>", page, this.getPages().size()));
    }

    public Component getDescription() {
        return this.description;
    }

    public List<Component> getParts() {
        return this.parts;
    }

    public int getPageLength() {
        return this.pageLength;
    }

    private List<List<Component>> getPages() {
        return Lists.partition(this.parts, pageLength);
    }

    private List<Component> getPage(int page) {
        page = page - 1; // remove 1 since we're a 0-based list
        try {
            return this.getPages().get(page);
        } catch(IndexOutOfBoundsException e) {
            return null;
        }
    }

    private List<Component> getHeaderParts(int page) {
        List<Component> headerParts = Lists.newArrayList();
        if (this.getHeader(page) != null) headerParts.add(this.getHeader(page));
        if (this.getDescription() != null) headerParts.add(this.getDescription());
        return headerParts;
    }

    public List<Component> generatePage(int page) {
        List<Component> parts = this.getPage(page);
        if (parts == null) return null;
        List<Component> headerParts = this.getHeaderParts(page);
        headerParts.addAll(parts);
        return headerParts;
    }

    public static List<Component> partsFromMiniMessages(List<String> miniMessages) {
        final MiniMessage miniMessage = MiniMessage.miniMessage();
        return miniMessages.stream().map(miniMessage::deserialize).toList();
    }

}
