package edu.curtin.saed.assignment1;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class Robot {
    private double x;
    private double y;
    private int delay;
    private int id;
    private int hp;
    private double targetX;
    private double targetY;

    public Robot(double x, double y, int delay, int id, int hp) {
        this.x = x;
        this.y = y;
        this.delay = delay;
        this.id = id;
        this.hp = hp;
    }

    public GridPosition generateNewMove(Random random, int gridWidth, int gridHeight, BlockingQueue<Robot> robots) {
        while (true) {
            int direction;
            // the robot moving towards (4, 4) most of the time (90% probability)
            if (x < 4 && random.nextDouble() < 0.9) {
                direction = 3; // Move right
            } else if (x > 4 && random.nextDouble() < 0.9) {
                direction = 2; // Move left
            } else if (y < 4 && random.nextDouble() < 0.9) {
                direction = 1; // Move down
            } else if (y > 4 && random.nextDouble() < 0.9) {
                direction = 0; // Move up
            } else {
                // Occasionally choose a random direction
                direction = random.nextInt(4);
            }

            double newX = x;
            double newY = y;
    
            switch (direction) {
                case 0:
                    newY--;
                    break;
                case 1:
                    newY++;
                    break;
                case 2:
                    newX--;
                    break;
                case 3:
                    newX++;
                    break;
                default:
                    break;
            }

            // Check if the new position is within the grid bounds
            if (newX >= 0 && newX < gridWidth && newY >= 0 && newY < gridHeight) {
                boolean collision = false;
                // Check if the other robot at the robot target grid
                for (Robot otherRobot : robots) {
                    if (!otherRobot.equals(this) && otherRobot.getX() == newX && otherRobot.getY() == newY) {
                        collision = true;
                        break;
                    }
                }
                // If not collision occur
                if (!collision) {
                    // Valid move found; update the robot target position
                    targetX = newX;
                    targetY = newY;
                    return new GridPosition(newX, newY);
                }
            }
        }
    }

    public boolean checkGameStatus(double newX, double newY) {  
        if (x == 4 && y == 4) { //arrived citadel
            return true; // Robot entered the citadel square
        }
        return false; // Move successful
    }
    
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public int getMoveDelay() {
        return delay;
    }

     public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    // Check if the robot is destroyed
    public boolean isDestroyed() {
        return hp <= 0;
    }
}
