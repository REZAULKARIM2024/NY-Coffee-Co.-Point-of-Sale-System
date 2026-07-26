package com.possystem.model;

public class RecipeStep {
    private int id;
    private int menuItemId;
    private String language;   // e.g. "en", "es"
    private int stepNumber;
    private String instruction;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}
