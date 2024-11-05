package edu.curtin.saed.assignment1;

import javafx.scene.canvas.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text and lines.
 */
public class JFXArena extends Pane
{
    // Represents an image to draw, retrieved as a project resource.
    private static final String IMAGE_FILE = "1554047213.png"; //Robot Image
    private static final String CITADEL_FILE = "rg1024-isometric-tower.png"; //Citadel Image
    private static final String WALL_FILE = "181478.png"; //Perfect wall
    private static final String WALL2_FILE = "181479.png"; //Broken wall
    private Image robot1;
    private Image citadel;
    private Image wall1;
    private Image wall2;
    
    // The following values are arbitrary, and you may need to modify them according to the 
    // requirements of your application.
    private int gridWidth = 9;
    private int gridHeight = 9;

    private double citadelX = 4.0;
    private double citadelY = 4.0;

    private BlockingQueue<Robot> robots;

    private BlockingQueue<Wall> walls;
    private int remainingWalls = 10;

    private boolean gameOver = false;
    private ExecutorService executorService;

    private TextArea logger;
    private Label label;
    private Label label2;
    private int score = 0;
    private int queueup = 0;
    private ExecutorService wallPlacementExecutor;
    private long lastWallPlacementTime = 0;
    private int uniqueid = 1;

    private Set<String> wallPlacementInProgressSet = Collections.synchronizedSet(new HashSet<>());

    private double gridSquareSize; // Auto-calculated
    private Canvas canvas; // Used to provide a 'drawing surface'.

    private List<ArenaListener> listeners = null;
    
    /**
     * Creates a new arena object, loading the robot image and initialising a drawing surface.
     */
    public JFXArena()
    {
        // Here's how (in JavaFX) you get an Image object from an image file that's part of the 
        // project's "resources". If you need multiple different images, you can modify this code 
        // accordingly.
        
        // (NOTE: _DO NOT_ use ordinary file-reading operations here, and in particular do not try
        // to specify the file's path/location. That will ruin things if you try to create a 
        // distributable version of your code with './gradlew build'. The approach below is how a 
        // project is supposed to read its own internal resources, and should work both for 
        // './gradlew run' and './gradlew build'.)
                
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(IMAGE_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + IMAGE_FILE);
            }
            robot1 = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + IMAGE_FILE, e);
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream(WALL_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + WALL_FILE);
            }
            wall1 = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + WALL_FILE, e);
        }

         try(InputStream is = getClass().getClassLoader().getResourceAsStream(WALL2_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + WALL2_FILE);
            }
            wall2 = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + WALL2_FILE, e);
        }
        
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(CITADEL_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + CITADEL_FILE);
            }
            citadel = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + CITADEL_FILE, e);
        }

        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        walls = new LinkedBlockingQueue<>();
        robots = new LinkedBlockingQueue<>();
        executorService = Executors.newFixedThreadPool(200);
        wallPlacementExecutor = Executors.newFixedThreadPool(1);
        generateRandomRobot();
        trackScore();
    }

    public void placeWall(int x, int y) {
        // Check if there's already a wall and a robot at the specified grid location and also 10 walls at the arena
        if (!gameOver && remainingWalls > 0 && !isOccupiedwall(x, y) && !isOccupied(x, y) && (x!=4 || y!=4)) {
                // Check if a wall placement process is already in progress for this grid location
                if (!isWallPlacementInProgress(x, y)) {
                    // Set a wall placement process is already in progress for this grid location
                    setWallPlacementInProgress(x, y);
                    // the wall queued
                    queueup += 1; 
                    Platform.runLater(()->label2.setText("Queued-up wall-building: " + queueup));
                    
                    wallPlacementExecutor.execute(() -> {
                    // Assign current time
                    long currentTime = System.currentTimeMillis();
                    // Check if the 2000 milliseconds achieved from last built wall duration and got robot at the grid
                    if (currentTime - lastWallPlacementTime <= 2000 && !isOccupied(x, y)) {
                            try {
                                Thread.sleep(2000 - (currentTime - lastWallPlacementTime)); // Sleep for waiting time before placing the wall
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    // If after sleep got robot at the grid, skip the command
                    if(!gameOver && isOccupied(x, y)) {
                        // Clear the flag indicating wall placement is in progress
                        clearWallPlacementInProgress(x, y);
                        Platform.runLater(() -> requestLayout());
                        queueup -= 1;
                        Platform.runLater(()->label2.setText("Queued-up wall-building: " + queueup));
                    } else if (!gameOver && !isOccupied(x, y)) {
                        // Add a wall to the grid
                        Wall wall = new Wall(x, y, 2, "Perfect");
                        try {
                            walls.put(wall);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        logger.appendText("Player put the wall at " + wall.getX() + ", " + wall.getY() + "\n");
                        remainingWalls--;
                        lastWallPlacementTime = currentTime;

                        // Clear the flag indicating wall placement is in progress
                        clearWallPlacementInProgress(x, y);
                        Platform.runLater(() -> requestLayout());
                        // Wall succesfully built and queue minus one
                        queueup -= 1;
                        Platform.runLater(()->label2.setText("Queued-up wall-building: " + queueup));
                    }
                    });
                }
        }
    }

    private boolean isWallPlacementInProgress(int x, int y) {
        return wallPlacementInProgressSet.contains(getGridLocationKey(x, y));
    }

    private void setWallPlacementInProgress(int x, int y) {
        wallPlacementInProgressSet.add(getGridLocationKey(x, y));
    }

    private void clearWallPlacementInProgress(int x, int y) {
        wallPlacementInProgressSet.remove(getGridLocationKey(x, y));
    }

    private String getGridLocationKey(int x, int y) {
        return x + "," + y;
    }

    private void startRobotTasks(Robot robot) {
        executorService.submit(() -> {
            // If the game not over and the robot is not destroyed
            while (!gameOver && !robot.isDestroyed()) {
                Random random = new Random();
                // Assign the robot target grid to the grid position object
                GridPosition newTarget = robot.generateNewMove(random, gridWidth, gridHeight, robots);
    
                // Check if the new target grid is the same as any other robot's target
                boolean hasCollision = false;
                for (Robot otherRobot : robots) {
                    if (!otherRobot.equals(robot) && otherRobot.getTargetX() == newTarget.getX() && otherRobot.getTargetY() == newTarget.getY()) {
                        hasCollision = true;
                        break;
                    }
                }
    
                // If there's a collision, generate a new move until a unique target grid is found
                while (hasCollision) {
                    newTarget = robot.generateNewMove(random, gridWidth, gridHeight, robots);
                    hasCollision = false;
                    for (Robot otherRobot : robots) {
                        if (!otherRobot.equals(robot) && otherRobot.getTargetX() == newTarget.getX() && otherRobot.getTargetY() == newTarget.getY()) {
                            hasCollision = true;
                            break;
                        }
                    }
                }
                robot.setTargetX(newTarget.getX());
                robot.setTargetY(newTarget.getY());
                // The robot perform the move and check if the robot already entered citadel
                simulateAnimation(robot, newTarget.getX(), newTarget.getY());
                boolean enteredCitadel = robot.checkGameStatus(newTarget.getX(), newTarget.getY());
                

                List<Wall> wallsToRemove = new ArrayList<>();
                List<Robot> robotsToRemove = new ArrayList<>();
                for (Wall wall : walls) {
                    // If the wall still got 1 hp and the robot run into the wall
                    if (!wall.isDestroyed() && robot.getX() == wall.getX() && robot.getY() == wall.getY()) {
                        // Robot HP minus one
                        robot.setHp(robot.getHp() - 1);
                        // If the robot destroyed 
                        if (robot.isDestroyed()) {
                            // Remove the robot and plus the score by 100
                            logger.appendText("Robot "+ robot.getId() + " destroyed" + "\n"); 
                            robotsToRemove.add(robot);
                            score += 100;
                            Platform.runLater(()->label.setText("Score: " + score));
                        }
                        // If the wall haven't destroyed
                        if (!wall.isDestroyed()) {
                            // Reduce the wall HP and become broken
                            wall.setHp(wall.getHp() - 1);
                            wall.setType("Broken");
                            logger.appendText("wall "+ wall.getX() + ", " + wall.getY() + " broken" + "\n"); 
                            // If the wall destroyed
                            if (wall.isDestroyed()) {
                                // Remove the wall
                                logger.appendText("wall "+ wall.getX() + ", " + wall.getY() + " destroyed" + "\n"); 
                                wallsToRemove.add(wall);
                                remainingWalls++;
                            }
                        }
                    }
                }
                // Remove the marked wall and robot
                robots.removeAll(robotsToRemove);
                walls.removeAll(wallsToRemove);

                // If the robot entered citadel, shut down all of the threads
                if (enteredCitadel) {
                    gameOver = true;
                    executorService.shutdown();
                    wallPlacementExecutor.shutdown();
                }
                Platform.runLater(() -> requestLayout());
    
                // Sleep according to the robot's move delay
                try {
                    Thread.sleep(robot.getMoveDelay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void simulateAnimation(Robot robot, double targetX, double targetY) {
        // Divide the animation into smaller intervals
        int numIntervals = 10; // Adjust as needed
        double intervalX = (targetX - robot.getX()) / numIntervals;
        double intervalY = (targetY - robot.getY()) / numIntervals;

        for (int i = 0; i < numIntervals; i++) {
            robot.setX(robot.getX() + intervalX);
            robot.setY(robot.getY() + intervalY);

             Platform.runLater(() -> requestLayout());
            // Sleep to create animation effect
            try {
                Thread.sleep(40); // Sleep for 40 milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        robot.setX(targetX);
        robot.setY(targetY);
    }
    
    private void generateRandomRobot() {
        executorService.submit(() -> {
            while (!gameOver) {
                // Check if there is a robot in each of the four corners
                boolean topLeftOccupied = isOccupied(0, 0);
                boolean topRightOccupied = isOccupied(gridWidth - 1, 0);
                boolean bottomLeftOccupied = isOccupied(0, gridHeight - 1);
                boolean bottomRightOccupied = isOccupied(gridWidth - 1, gridHeight - 1);
    
                // If any of the corners is not occupied, spawn a new robot
                if (!topLeftOccupied || !topRightOccupied || !bottomLeftOccupied || !bottomRightOccupied) {
                    Random random = new Random();
                    int x = 0;
                    int y = 0;
    
                    // Find a corner that is not occupied
                    while (true) {
                        int corner = random.nextInt(4);
                        if ((corner == 0 && !topLeftOccupied) ||
                                (corner == 1 && !topRightOccupied) ||
                                (corner == 2 && !bottomLeftOccupied) ||
                                (corner == 3 && !bottomRightOccupied)) {
                            switch (corner) {
                                case 0:
                                    x = 0;
                                    y = 0;
                                    break;
                                case 1:
                                    x = gridWidth - 1;
                                    y = 0;
                                    break;
                                case 2:
                                    x = 0;
                                    y = gridHeight - 1;
                                    break;
                                case 3:
                                    x = gridWidth - 1;
                                    y = gridHeight - 1;
                                    break;
                                default:
                                    break;
                            }
                            break;
                        }
                    }
                    
                    // Assign a random delay value to every single robot between 500 and 2000 milliseconds
                    int minDelay = 500;
                    int maxDelay = 2000;
                    int delay = random.nextInt(maxDelay - minDelay + 1) + minDelay;
                    // Unique ID for the robot and robot into the list
                    int id = uniqueid++; 
                    Robot robot = new Robot(x, y, delay, id, 1);
                    addRobot(robot);
                    Platform.runLater(() -> {
                        synchronized (logger) {
                            logger.appendText("Robot " + robot.getId() + " spawned at " + robot.getX() + ", " + robot.getY() + "\n");
                        }
                        // Assign the movement task to each robot
                        startRobotTasks(robot);
                    });
                }
    
                // Sleep for 1500 milliseconds before generating the next robot
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void trackScore() {
        // Track the score until the game is ended
        executorService.submit(() -> {
            while (!gameOver) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // The score plus 10 every 1000 milliseconds
                score += 10;
                Platform.runLater(()->label.setText("Score: " + score));
            }
        });
    }

    public void addRobot(Robot robot) {
        try {
            robots.put(robot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void addWall(Wall wall) {
        try {
            walls.put(wall);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setLabel(Label label)
    {
        this.label = label;
    }

    public void setLabel2(Label label2)
    {
        this.label2 = label2;
    }

    public void setLogger(TextArea logger)
    {
        this.logger = logger;
    }

    private boolean isOccupied(int x, int y) {
        // Check if the grid already occupied by robot
        for (Robot robot : robots) {
            if (robot.getX() == x && robot.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupiedwall(int x, int y) {
        // Check if the grid already occupied by wall
        for (Wall wall : walls) {
            if (wall.getX() == x && wall.getY() == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves a robot image to a new grid position. This is highly rudimentary, as you will need
     * many different robots in practice. This method currently just serves as a demonstration.
     */
    public void setCitadelPosition(double x, double y)
    {
        citadelX = x;
        citadelY = y;
        requestLayout();
    }
    
    /**
     * Adds a callback for when the user clicks on a grid square within the arena. The callback 
     * (of type ArenaListener) receives the grid (x,y) coordinates as parameters to the 
     * 'squareClicked()' method.
     */
    public void addListener(ArenaListener newListener)
    {
        if(listeners == null)
        {
            listeners = new LinkedList<>();
            setOnMouseClicked(event ->
            {
                int gridX = (int)(event.getX() / gridSquareSize);
                int gridY = (int)(event.getY() / gridSquareSize);
                
                if(gridX < gridWidth && gridY < gridHeight)
                {
                    for(ArenaListener listener : listeners)
                    {   
                        listener.squareClicked(gridX, gridY);
                    }
                }
            });
        }
        listeners.add(newListener);
    }
        
        
    /**
     * This method is called in order to redraw the screen, either because the user is manipulating 
     * the window, OR because you've called 'requestLayout()'.
     *
     * You will need to modify the last part of this method; specifically the sequence of calls to
     * the other 'draw...()' methods. You shouldn't need to modify anything else about it.
     */
    @Override
    public void layoutChildren()
    {
        super.layoutChildren(); 
        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        
        // First, calculate how big each grid cell should be, in pixels. (We do need to do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(
            getWidth() / (double) gridWidth,
            getHeight() / (double) gridHeight);
            
        double arenaPixelWidth = gridWidth * gridSquareSize;
        double arenaPixelHeight = gridHeight * gridSquareSize;
            
            
        // Draw the arena grid lines. This may help for debugging purposes, and just generally
        // to see what's going on.
        gfx.setStroke(Color.DARKGREY);
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0); // Outer edge

        for(int gridX = 1; gridX < gridWidth; gridX++) // Internal vertical grid lines
        {
            double x = (double) gridX * gridSquareSize;
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight);
        }
        
        for(int gridY = 1; gridY < gridHeight; gridY++) // Internal horizontal grid lines
        {
            double y = (double) gridY * gridSquareSize;
            gfx.strokeLine(0.0, y, arenaPixelWidth, y);
        }

        // Invoke helper methods to draw things at the current location.
        // ** You will need to adapt this to the requirements of your application. **
        // drawImage(gfx, robot1, robotX, robotY); //draw robot image
        // drawLabel(gfx, "Robot Name", robotX, robotY); //draw robot label
        Iterator<Robot> robotList = robots.iterator();
        while (robotList.hasNext()){
            Robot selectedrobot = robotList.next();
            drawImage(gfx, robot1, selectedrobot.getX(), selectedrobot.getY());
            drawLabel(gfx, "Robot " + selectedrobot.getId(), selectedrobot.getX(), selectedrobot.getY());
        }

        Iterator<Wall> wallList = walls.iterator();
        while (wallList.hasNext()){
            Wall selectedWall = wallList.next();
            if(selectedWall.getType().equalsIgnoreCase("Perfect"))
            {
                drawImage(gfx, wall1, selectedWall.getX(), selectedWall.getY());
            }
            else if(selectedWall.getType().equalsIgnoreCase("Broken"))
            {
                drawImage(gfx, wall2, selectedWall.getX(), selectedWall.getY());
            }
        }

        drawImage(gfx, citadel, citadelX, citadelY); //draw citadel image
        drawLabel(gfx, "Citadel Name", citadelX, citadelY); //draw citadel label
    }
    
    
    /** 
     * Draw an image in a specific grid location. *Only* call this from within layoutChildren(). 
     *
     * Note that the grid location can be fractional, so that (for instance), you can draw an image 
     * at location (3.5,4), and it will appear on the boundary between grid cells (3,4) and (4,4).
     *     
     * You shouldn't need to modify this method.
     */
    private void drawImage(GraphicsContext gfx, Image image, double gridX, double gridY)
    {
        // Get the pixel coordinates representing the centre of where the image is to be drawn. 
        double x = (gridX + 0.5) * gridSquareSize;
        double y = (gridY + 0.5) * gridSquareSize;
        
        // We also need to know how "big" to make the image. The image file has a natural width 
        // and height, but that's not necessarily the size we want to draw it on the screen. We 
        // do, however, want to preserve its aspect ratio.
        double fullSizePixelWidth = robot1.getWidth();
        double fullSizePixelHeight = robot1.getHeight();

        double displayedPixelWidth, displayedPixelHeight;
        if(fullSizePixelWidth > fullSizePixelHeight)
        {
            // Here, the image is wider than it is high, so we'll display it such that it's as 
            // wide as a full grid cell, and the height will be set to preserve the aspect 
            // ratio.
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        }
        else
        {
            // Otherwise, it's the other way around -- full height, and width is set to 
            // preserve the aspect ratio.
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        // Actually put the image on the screen.
        gfx.drawImage(image,
            x - displayedPixelWidth / 2.0,  // Top-left pixel coordinates.
            y - displayedPixelHeight / 2.0, 
            displayedPixelWidth,              // Size of displayed image.
            displayedPixelHeight);
    }
    
    
    /**
     * Displays a string of text underneath a specific grid location. *Only* call this from within 
     * layoutChildren(). 
     *     
     * You shouldn't need to modify this method.
     */
    private void drawLabel(GraphicsContext gfx, String label, double gridX, double gridY)
    {
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(Color.BLUE);
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize);
    }
}
