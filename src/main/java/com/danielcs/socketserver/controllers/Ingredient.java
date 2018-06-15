package com.danielcs.socketserver.controllers;

public class Ingredient {

    public static final Ingredient[] ingredients = {
            new Ingredient(1, "Beef sirloin", 10),
            new Ingredient(2, "Cheese", 5),
            new Ingredient(3, "Potato", 20),
            new Ingredient(4, "Goose liver", 7)
    };

    private int id;
    private String name;
    private int amount;

    public Ingredient() {
    }

    public Ingredient(int id, String name, int amount) {
        this.id = id;
        this.name = name;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
