package edu.curtin.saed.assignment1;

public class Wall {
    private double x;
    private double y;
    private int hp;
    private String type;

    public Wall(double x, double y, int hp, String type) {
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.type= type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    // Check if the wall is destroyed
    public boolean isDestroyed() {
        return hp <= 0;
    }
}
