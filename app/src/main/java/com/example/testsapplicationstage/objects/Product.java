package com.example.testsapplicationstage.objects;

public class Product {
    private String label;
    private Double stock_reel;

    public void setLabel(String label) {
        this.label = label;
    }

    public int getStock_reel() {
        return stock_reel == null ? 0 : stock_reel.intValue();
    }

    public void setStock_reel(Double stock_reel) {
        this.stock_reel = stock_reel;
    }

    @Override
    public String toString() {
        return label;
    }
}
