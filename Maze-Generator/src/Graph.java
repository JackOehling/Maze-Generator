import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//Represents the player
class Player {
  // Player's current cell
  Cell current;
  // ArrayList of cells the player visited
  ArrayList<Cell> visited;

  Player(Cell current, ArrayList<Cell> visited) {
    this.current = current;
    this.visited = visited;
  }

  // Convenience Constructor
  Player() {
    this.current = null;
    this.visited = new ArrayList<Cell>();
  }

  // updates player's current cell
  // EFFECT: updates current position and visited list
  void updateCurrent(Cell newCurrent) {
    // if the current position is not in visited, add it to visited
    if (!this.visited.contains(this.current)) {
      this.visited.add(this.current);
    }
    // add the new position to visited and mutate current position
    this.visited.add(newCurrent);
    this.current = newCurrent;
    newCurrent.isVisited = true;
  }
}

//Represents an edge
class Edge {
  Cell from;
  Cell to;
  int weight;

  Edge(Cell from, Cell to) {
    this.from = from;
    this.to = to;
  }

  // EFFECT: sets the weight of each edge to the given value
  void setWeight(int w) {
    this.weight = w;
  }
}

// Class to represent the cells in our game 
class Cell {
  int x;
  int y;
  Edge left;
  Edge top;
  Edge right;
  Edge bottom;
  Color color;
  // the edges exiting this cell
  ArrayList<Edge> outEdges;

  // whether the right and bottom edges are blocked
  boolean isRightWallBlocked = true;
  boolean isBottomWallBlocked = true;
  // has this cell been visited by the user?
  boolean isVisited = false;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.color = Color.gray;
    this.outEdges = new ArrayList<Edge>();
  }

  // EFFECT: Sets the adjacent edges for this cell
  void setAdjacents(Edge left, Edge top, Edge right, Edge bottom) {
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  // EFFECT: Sets the color of this cell
  void setColor(Color c) {
    this.color = c;
  }

  // Draws this cell scaled up by the given size
  WorldImage drawCell(int cellSize) {

    LineImage rightWall = new LineImage(new Posn(0, cellSize), Color.black);
    LineImage bottomWall = new LineImage(new Posn(cellSize, 0), Color.black);

    RectangleImage cellImg = new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, this.color);
    WorldImage result = cellImg;

    // if the right edge is blocked, add a wall image
    if (this.isRightWallBlocked) {
      result = new OverlayOffsetImage(rightWall, (cellSize * -0.5) + 1, 0, result)
          .movePinholeTo(new Posn(0, 0));

    }

    // if the bottom edge is blocked, add a wall image
    if (this.isBottomWallBlocked) {
      result = new OverlayOffsetImage(bottomWall, 0, (cellSize * -0.5) + 1, result)
          .movePinholeTo(new Posn(0, 0));
    }
    return result.movePinholeTo(new Posn(0, 0));
  }
}

// Class to represent our game world 
class Maze extends World {
  ArrayList<Cell> cells;
  int width;
  int height;
  int cellSize;

  // Colors for this game
  Color pathColor = new Color(255, 153, 153);
  Color currentColor = new Color(255, 51, 51);

  // HashMap used for union find algorithm
  HashMap<Cell, Cell> ufMap;
  // spanning tree produced by union find/krusgal's algorithm
  ArrayList<Edge> ufTree;
  // to represent the player
  Player user;

  ArrayList<Edge> edges;

  // path for the answer
  ArrayList<Cell> answerPath;

  // ArrayList representing the search path for DFS
  ArrayList<Cell> dfsPath;

  // ArrayList representing the search path for DFS
  ArrayList<Cell> bfsPath;

  // shows all the different paths
  boolean showDFSPath;
  boolean showBFSPath;
  boolean showAnswerPath;

  // constructor
  Maze(int width, int height, Player user) {
    this.width = width;
    this.height = height;
    this.user = user;
    if (this.height <= 15 && this.width <= 28) {
      this.cellSize = 50;
    }
    else if (this.height > 15 || this.width > 28) {
      this.cellSize = 13;
    }

    this.initMaze();
    this.initPlayer();
  }

  // resets the maze attributes
  void initMaze() {

    // resetting (back to beginning case)
    this.ufMap = new HashMap<Cell, Cell>();
    this.ufTree = new ArrayList<Edge>();
    this.edges = new ArrayList<Edge>();
    this.showDFSPath = false;
    this.showBFSPath = false;
    this.showAnswerPath = false;

    // build all the cells
    this.cells = this.buildCells(this.width, this.height);
    // connect all the cells in this maze
    this.setAdjacents();
    // set random weights to the edges connecting the cells
    this.setRandomWeights();
    // find the MST to connect all the cells in this maze
    this.unionFind();
    // if the edge is not in the MST, draw a wall to cut off the edge
    this.setCellWalls();
    // if the edge is not in the MST, add it to the edges
    this.setCellEdges();

    // sets cell color
    this.cells.get(this.cells.size() - 1).color = Color.blue;
  }

  // initialize the player
  void initPlayer() {
    this.user = new Player();
    this.user.updateCurrent(this.cells.get(0));
  }

  // builds the board
  ArrayList<Cell> buildCells(int width, int height) {
    ArrayList<Cell> result = new ArrayList<Cell>(width * height);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Cell c = new Cell(x, y);
        result.add(c);
      }
    }
    return result;
  }

  // EFFECT: Sets the adjacent cells for all cells
  // & adds down and right edges for each cell
  void setAdjacents() {
    int currentIndex = 0;
    Cell current;

    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        currentIndex = (y * this.width) + x;
        current = this.cells.get(currentIndex);

        // if not in the leftmost column
        if (current.x > 0) {
          Edge left = new Edge(current, this.cells.get(currentIndex - 1));
          current.left = left;
        }
        // if not in the topmost row
        if (current.y > 0) {
          Edge top = new Edge(current, this.cells.get(currentIndex - this.width));
          current.top = top;
        }
        // if not in the rightmost column
        if (current.x < this.width - 1) {
          Edge right = new Edge(current, this.cells.get(currentIndex + 1));
          current.right = right;

          this.edges.add(current.right);
        }
        // if not on the bottom most row
        if (current.y < this.height - 1) {
          Edge bottom = new Edge(current, this.cells.get(currentIndex + this.width));
          current.bottom = bottom;

          this.edges.add(current.bottom);
        }
      }
    }
  }

  // EFFECT: assigns appropriate boolean value to each cell's right and bottom
  // wall attribute
  void setCellWalls() {
    for (Cell c : this.cells) {
      if (this.ufTree.contains(c.right)) {
        c.isRightWallBlocked = false;
      }
      if (this.ufTree.contains(c.bottom)) {
        c.isBottomWallBlocked = false;
      }
    }
  }

  // EFFECT: if the adjacent edges of the cell is not in the MST,
  // add it to it's edges
  void setCellEdges() {
    for (Cell c : this.cells) {
      if (this.ufTree.contains(c.right)) {
        Cell toCell = c.right.to;
        c.outEdges.add(c.right);
        toCell.outEdges.add(toCell.left);
      }
      if (this.ufTree.contains(c.bottom)) {
        Cell toCell = c.bottom.to;
        c.outEdges.add(c.bottom);
        toCell.outEdges.add(toCell.top);
      }
    }
  }

  // EFFECT: Randomly sets the weight of each edge
  void setRandomWeights() {
    Random r = new Random();
    for (Edge e : this.edges) {
      e.setWeight(r.nextInt(50));
    }
  }

  // Makes a hash map using this games cells where each cell references itself
  HashMap<Cell, Cell> initMap(ArrayList<Edge> wl) {
    HashMap<Cell, Cell> result = new HashMap<Cell, Cell>();
    for (int i = 0; i < wl.size(); i++) {
      Cell from = wl.get(i).from;
      Cell to = wl.get(i).to;

      result.put(from, from);
      result.put(to, to);
    }
    return result;
  }

  // Effect: Creates a spanning tree using the union find and krusgal's algorithm
  void unionFind() {
    ArrayList<Edge> wL = new MazeUtils().sortEdges(this.edges);
    this.ufMap = this.initMap(wL);
    int i = 0;
    while (this.ufTree.size() < this.cells.size() - 1 && i < wL.size()) {
      Cell from = wL.get(i).from;
      Cell to = wL.get(i).to;

      if (this.find(this.ufMap, to).equals(this.find(this.ufMap, from))) {
        i += 1;
      }

      else {
        this.ufTree.add(wL.remove(i));
        this.unionGroup(this.ufMap, to, from);
      }
    }
  }

  // Find the Cell of the given key in the given map
  Cell find(HashMap<Cell, Cell> hm, Cell key) {
    Cell rep = hm.get(key);
    while (!hm.get(rep).equals(rep)) {
      rep = hm.get(rep);
    }
    return rep;
  }

  // EFFECT: set the given first cell's representative to the given second cell's
  // representative
  void unionGroup(HashMap<Cell, Cell> hm, Cell first, Cell second) {
    hm.put(this.find(hm, first), this.find(hm, second));
  }

  // Return a search path using dfs or bfs using the given ICollection<Cell> data
  ArrayList<Cell> searchPath(Cell start, Cell end, ICollection<Cell> worklist) {
    HashMap<Cell, Cell> fromEdge = new HashMap<Cell, Cell>();
    ArrayList<Cell> seen = new ArrayList<Cell>();
    worklist.add(start);

    while (worklist.size() > 0) {
      Cell next = worklist.remove();

      if (seen.contains(next)) {
        // do nothing
      }
      else if (next == end) {
        break;
      }
      else {
        for (Edge e : next.outEdges) {
          worklist.add(e.to);
          fromEdge.put(next, e.to);
        }
        seen.add(next);
      }
    }

    return seen;

  }

  // Find the answer path using dijkstra's algorithm
  ArrayList<Cell> answerPath(Cell start, Cell end) {
    ArrayList<Cell> unvisited = new ArrayList<Cell>();
    HashMap<Cell, Integer> path = new HashMap<Cell, Integer>();
    HashMap<Cell, Cell> previous = new HashMap<Cell, Cell>();

    unvisited.add(start);
    path.put(start, 0);

    while (unvisited.size() > 0) {
      Cell c = unvisited.remove(0);

      for (Edge e : c.outEdges) {
        if (path.get(e.to) == null || path.get(e.to) > path.get(c) + e.weight) {

          // update the distance and update predecessor
          path.put(e.to, path.get(c) + e.weight);
          previous.put(e.to, c);

          unvisited.add(e.to);
        }
      }
    }

    ArrayList<Cell> answer = new ArrayList<Cell>();
    Cell step = end;
    // if there was not a path to the end yet
    if (previous.get(step) == null) {
      // return empty list
      return answer;

    }

    answer.add(step);

    // while we haven't reached the start
    while (step != start) {
      // add the cell before to the answer path
      step = previous.get(step);
      answer.add(0, step);
    }
    return answer;
  }

  // visualizes the current game scene
  public WorldScene makeScene() {

    WorldScene current = new WorldScene(this.width * this.cellSize, this.height * this.cellSize);

    WorldImage curMaze = this.drawCurrentBoard();

    current.placeImageXY(curMaze, (this.cellSize * (this.width)) / 2,
        (this.cellSize * (this.height)) / 2);
    return current;
  }

  // visualizes the ending scene of the game
  public WorldScene makeEndScene() {
    // show the answer path
    this.answerPath = this.answerPath(this.cells.get(0), this.cells.get(this.cells.size() - 1));
    for (Cell c : this.answerPath) {
      c.setColor(Color.orange);
    }
    WorldScene current = this.makeScene();

    WorldImage message = new TextImage("The maze is solved!", 20, Color.black)
        .movePinholeTo(new Posn(0, 0));

    current.placeImageXY(message, (this.cellSize * (this.width)) / 2,
        (this.cellSize * (this.height)) / 2);

    return current;
  }

  // Draws this current cell configuration
  WorldImage drawCurrentBoard() {
    // Accumulator: the board image so far
    WorldImage boardAcc = new EmptyImage();
    // Accumulator: the row image so far
    WorldImage rowAcc = new EmptyImage();
    for (int y = 0; y < height; y++) {
      rowAcc = new EmptyImage();
      for (int x = 0; x < width; x++) {
        int currentIndex = (y * width) + x;
        Cell currentCell = this.cells.get(currentIndex);

        rowAcc = new BesideImage(rowAcc, currentCell.drawCell(this.cellSize));
      }
      boardAcc = new AboveImage(boardAcc, rowAcc);
    }

    return boardAcc.movePinholeTo(new Posn(0, 0));
  }

  // show the answer path on the board
  void showAnswer() {
    this.answerPath = this.answerPath(this.cells.get(0), this.cells.get(this.cells.size() - 1));
    this.showAnswerPath = true;
  }

  // animate and show the breadth first search
  void showBreadthFirst() {
    this.bfsPath = this.searchPath(this.cells.get(0), this.cells.get(this.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>()));
    this.showBFSPath = true;
  }

  // animate and show the depth first search
  void showDepthFirst() {
    this.dfsPath = this.searchPath(this.cells.get(0), this.cells.get(this.cells.size() - 1),
        new Stack<Cell>(new Deque<Cell>()));
    this.showDFSPath = true;
  }

  // EFFECT: update the visited cell colors
  void updateVisitedColors() {
    this.cells.get(this.cells.size() - 1).setColor(Color.blue);
    for (Cell c : this.cells) {
      if (c.isVisited) {
        c.setColor(this.pathColor);
      }
    }
    this.user.current.setColor(this.currentColor);
  }

  // EFFECT: update the current position of the user to the given Cell
  void moveUser(Cell next, Edge connection) {
    if (this.ufTree.contains(connection)) {
      this.user.updateCurrent(next);
    }
  }

  // EFFECT: update the status of this maze
  void updateMaze() {

    // update the visited cell colors
    this.updateVisitedColors();

    // if showBFS is true
    if (this.showBFSPath) {
      Cell temp = this.bfsPath.remove(0);
      temp.setColor(new Color(34, 139, 34));
      if (this.bfsPath.size() == 0) {
        this.showBFSPath = false;
      }
    }

    // if showDFS is true
    if (this.showDFSPath) {
      Cell temp = this.dfsPath.remove(0);
      temp.setColor(new Color(153, 50, 204));
      if (this.dfsPath.size() == 0) {
        this.showDFSPath = false;
      }
    }

    // if showAnswer is true
    if (this.showAnswerPath) {
      Cell temp = this.answerPath.remove(0);
      temp.setColor(Color.orange);
      if (this.answerPath.size() == 0) {
        this.showAnswerPath = false;
      }
    }
  }

  // on tick big-bang method
  public void onTick() {
    this.updateMaze();
  }

  // EFFECT: make the appropriate changes to maze given a pressed key
  public void onKeyEvent(String key) {
    int currentIndex = (this.user.current.y * this.width) + this.user.current.x;
    Cell choice;
    Edge edge;
    if (key.equals("a")) {
      this.showAnswer();
    }
    if (key.equals("b")) {
      this.showBreadthFirst();
    }
    if (key.equals("d")) {
      this.showDepthFirst();
    }
    if (key.equals("r")) {
      this.initMaze();
      this.initPlayer();
    }
    if (key.equals("up") && this.user.current.y > 0) {
      choice = this.cells.get(currentIndex - this.width);
      edge = choice.bottom;
      this.moveUser(choice, edge);
    }
    if (key.equals("down") && this.user.current.y < this.height - 1) {
      choice = this.cells.get(currentIndex + this.width);
      edge = this.user.current.bottom;
      this.moveUser(choice, edge);
    }
    if (key.equals("right") && this.user.current.x < this.width - 1) {
      choice = this.cells.get(currentIndex + 1);
      edge = this.user.current.right;
      this.moveUser(choice, edge);
    }
    if (key.equals("left") && this.user.current.x > 0) {
      choice = this.cells.get(currentIndex - 1);
      edge = choice.right;
      this.moveUser(choice, edge);
    }
  }

  public WorldEnd worldEnds() {
    if (this.user.current == this.cells.get(this.cells.size() - 1)) {
      return new WorldEnd(true, this.makeEndScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

//class to hold utility methods
class MazeUtils {
  // returns the given list but sorts the edges by weight
  // from smallest to largest

  ArrayList<Edge> sortEdges(ArrayList<Edge> edges) {
    ArrayList<Edge> result = this.copy(edges);
    sortEdgesHelper(result, 0, result.size());
    return result;
  }

  // EFFECT: sorts the list of edges by weight
  void sortEdgesHelper(ArrayList<Edge> edges, int low, int high) {
    if (low >= high) {
      return;
    }

    // select pivot
    Edge lowEdge = edges.get(low);

    // separate edges to lower or upper portions
    int partIndex = separate(edges, low, high, lowEdge);

    // sort both halves of the list
    sortEdgesHelper(edges, low, partIndex);
    sortEdgesHelper(edges, partIndex + 1, high);
  }

  // Returns the index where the lowEdge ends up in the sorted list of edges
  // EFFECT: makes it so all edges on the left of the pivot are are weighted less
  // than the edges to the right
  int separate(ArrayList<Edge> edges, int low, int high, Edge lowEdge) {
    int currentLow = low;
    int currentHigh = high - 1;

    while (currentLow < currentHigh) {
      while (currentLow < high && edges.get(currentLow).weight <= lowEdge.weight) {
        currentLow += 1;
      }

      while (currentHigh >= low && edges.get(currentHigh).weight > lowEdge.weight) {
        currentHigh -= 1;
      }

      if (currentLow < currentHigh) {
        Collections.swap(edges, currentLow, currentHigh);
      }
    }

    Collections.swap(edges, low, currentHigh);
    return currentHigh;
  }

  // creates a copy of the given list
  <T> ArrayList<T> copy(ArrayList<T> source) {
    ArrayList<T> result = new ArrayList<T>();
    for (T t : source) {
      result.add(t);
    }
    return result;
  }
}

class ExamplesMaze {

  ArrayList<Edge> edges1;
  ArrayList<Edge> edges2;
  ArrayList<Edge> edges3;
  ArrayList<Edge> edges4;

  ArrayList<Cell> cells1;
  ArrayList<Cell> cells2;
  ArrayList<Cell> cells3;

  HashMap<Cell, Cell> result;
  HashMap<Cell, Cell> result2;

  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;
  Edge e6;
  Edge e7;
  Edge e8;

  Cell c1;
  Cell c2;
  Cell c3;
  Cell c4;
  Cell c5;
  Cell c6;
  Cell c7;
  Cell c8;
  Cell c9;
  Cell c10;

  Maze testGame;
  Maze testGame1;
  Maze testGame2;
  Maze testGame3;

  Player user;
  Player user1;
  Player user2;

  void initData() {
    // Initializing the user
    this.user = new Player(null, null);
    this.user1 = new Player(c1, cells2);
    this.user2 = new Player(c3, cells1);

    // Initializing the game
    this.testGame = new Maze(5, 3, this.user);
    this.testGame1 = new Maze(10, 5, this.user);
    this.testGame2 = new Maze(100, 60, this.user);
    this.testGame3 = new Maze(3, 4, this.user);

    // Initializing the edges
    this.e1 = new Edge(null, null);
    this.e1.weight = 10;
    this.e2 = new Edge(null, null);
    this.e2.weight = 5;
    this.e3 = new Edge(null, null);
    this.e3.weight = 4;
    this.e4 = new Edge(null, null);
    this.e4.weight = 3;

    this.e5 = new Edge(c1, c2);
    this.e5.weight = 10;
    this.e6 = new Edge(c2, c3);
    this.e6.weight = 5;
    this.e7 = new Edge(c3, c4);
    this.e7.weight = 4;
    this.e8 = new Edge(c4, c5);
    this.e8.weight = 3;

    edges1 = new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4));
    edges2 = new ArrayList<Edge>(Arrays.asList(this.e5, this.e6, this.e7, this.e8));
    edges3 = new ArrayList<Edge>(Arrays.asList(this.e5, this.e6));
    edges4 = new MazeUtils().sortEdges(this.edges1);

    // Initializing the cells
    this.c1 = new Cell(0, 0);
    this.c2 = new Cell(0, 1);
    this.c3 = new Cell(0, 2);
    this.c4 = new Cell(1, 0);
    this.c5 = new Cell(1, 1);
    this.c6 = new Cell(1, 2);
    this.c7 = new Cell(2, 0);
    this.c8 = new Cell(2, 1);
    this.c9 = new Cell(2, 2);
    this.c10 = new Cell(4, 5);

    cells1 = new ArrayList<Cell>(Arrays.asList(this.c1, this.c4, this.c2, this.c5));
    cells2 = new ArrayList<Cell>(Arrays.asList(this.c1, this.c4, this.c7, this.c2, this.c5, this.c8,
        this.c3, this.c6, this.c9));
    cells3 = new ArrayList<Cell>();

    result = new HashMap<Cell, Cell>();
    result2 = new HashMap<Cell, Cell>();
  }

  // to test method updateCurrent
  void testUpdateCurrent(Tester t) {
    this.initData();
    t.checkExpect(user1.visited.size(), 9);
    t.checkExpect(user2.visited.size(), 4);
    // c3 is not part of the list
    user1.updateCurrent(c3);
    user2.updateCurrent(c9);
    user2.updateCurrent(c10);
    // changes size as new cells are added to the list
    t.checkExpect(user1.visited.size(), 10);
    t.checkExpect(user2.visited.size(), 7);
  }

  // to test method setAdjacentCell
  void testSetAdjacentsCell(Tester t) {
    this.initData();
    c1.setAdjacents(this.e1, this.e2, this.e3, this.e4);
    t.checkExpect(c1.left, this.e1);
    t.checkExpect(c1.top, this.e2);
    t.checkExpect(c1.right, this.e3);
    t.checkExpect(c1.bottom, this.e4);
    c2.setAdjacents(this.e1, this.e2, this.e3, this.e4);
    t.checkExpect(c2.left, this.e1);
    t.checkExpect(c2.top, this.e2);
    t.checkExpect(c2.right, this.e3);
    t.checkExpect(c2.bottom, this.e4);
  }

  // to test method setColor
  void testSetColor(Tester t) {
    this.initData();
    c1.setColor(Color.BLACK);
    c2.setColor(Color.RED);
    c3.setColor(Color.GREEN);
    c4.setColor(Color.YELLOW);
    t.checkExpect(this.c1.color, Color.BLACK);
    t.checkExpect(this.c2.color, Color.RED);
    t.checkExpect(this.c3.color, Color.GREEN);
    t.checkExpect(this.c4.color, Color.YELLOW);
  }

  // to test method drawCell
  void testDrawCell(Tester t) {
    // initialize the data
    this.initData();
    // make local variables
    WorldImage cell = new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray);
    WorldImage cellRightWall = new OverlayOffsetImage(
        new RectangleImage(1, 50, OutlineMode.SOLID, Color.black), (50 * -0.5) + 1, 0, cell)
            .movePinholeTo(new Posn(0, 0));
    WorldImage cellExampleRightBottomWall = new OverlayOffsetImage(
        new RectangleImage(50, 1, OutlineMode.SOLID, Color.black), 0, (50 * -0.5) + 1,
        cellRightWall).movePinholeTo(new Posn(0, 0));

    // test before mutation
    // t.checkExpect(this.c1.drawCell(50), cellExampleRightBottomWall);
    // mutate the data
    this.c1.isRightWallBlocked = true;
    this.c1.isBottomWallBlocked = false;
    // test the change
    // t.checkExpect(this.c1.drawCell(50), cellRightWall);

    // mutate the data again
    this.c1.isRightWallBlocked = false;
    // test the change
    t.checkExpect(this.c1.drawCell(50), cell);
  }

  // to test method setWeights
  void testSetWeight(Tester t) {
    this.initData();
    e1.setWeight(20);
    e2.setWeight(10);
    e3.setWeight(30);
    e4.setWeight(40);
    t.checkExpect(e1.weight, 20);
    t.checkExpect(e2.weight, 10);
    t.checkExpect(e3.weight, 30);
    t.checkExpect(e4.weight, 40);
  }

  // to test method buildCells
  void testBuildCells(Tester t) {
    this.initData();
    // different cells made depending on the height and width
    t.checkExpect(testGame.buildCells(2, 2), cells1);
    t.checkExpect(testGame.buildCells(3, 3), cells2);
    // produce the same (below two)
    t.checkExpect(testGame.buildCells(0, 1), cells3);
    t.checkExpect(testGame.buildCells(1, 0), cells3);
    // confirming size difference
    t.checkExpect(cells1.size(), 4);
    t.checkExpect(cells2.size(), 9);
    t.checkExpect(cells3.size(), 0);
    // different board
    t.checkExpect(cells1.equals(cells2), false);
    t.checkExpect(cells2.equals(cells3), false);
    t.checkExpect(cells2.equals(cells2), true);

  }

  // to test method setAdjacents
  void testSetAdjacents(Tester t) {
    this.initData();

    // Check the data before
    t.checkExpect(this.c1.left, null);
    t.checkExpect(this.c2.bottom, null);

    // Run the code to modify the state
    this.c1.setAdjacents(this.e1, null, null, null);
    this.c2.setAdjacents(null, null, null, this.e2);

    // Test the change was made
    t.checkExpect(this.c1.left, this.e1);
    t.checkExpect(this.c2.bottom, this.e2);
  }

  // to test method sortEdges
  void testSortEdges(Tester t) {
    // initialize the data
    this.initData();

    MazeUtils mu = new MazeUtils();
    // check before the change
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
    // make the change and check that the method returns what is right,
    mu.sortEdges(this.edges1);
    t.checkExpect(mu.sortEdges(this.edges1),
        new ArrayList<Edge>(Arrays.asList(this.e4, this.e3, this.e2, this.e1)));
    // check that the method didn't mutate the original list
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
  }

  // to test method setRandomWeights
  void testSetRandomWeights(Tester t) {
    // random r for seeding
    Random r = new Random(5);
    // sets weight randomly
    // testGame1.edges.get(0).setWeight(r.nextInt(50));
    // testGame1.edges.get(1).setWeight(r.nextInt(50));
    testGame1.edges.get(2).setWeight(r.nextInt(50));
    testGame1.edges.get(3).setWeight(r.nextInt(50));
    testGame1.edges.get(4).setWeight(r.nextInt(50));
    testGame1.edges.get(5).setWeight(r.nextInt(50));
    // outputs set weight
    // t.checkExpect(testGame1.edges.get(1).weight,15);
    t.checkExpect(testGame1.edges.get(2).weight, 37);
    t.checkExpect(testGame1.edges.get(3).weight, 42);
    t.checkExpect(testGame1.edges.get(4).weight, 24);
    t.checkExpect(testGame1.edges.get(5).weight, 24);
  }

  // to test method initMap
  void testInitMap(Tester t) {
    this.initData();
    testGame1.initMap(edges1);
    result.put(edges2.get(0).from, edges2.get(0).from);
    result.put(edges2.get(0).from, edges2.get(0).from);
    result.put(edges2.get(1).from, edges2.get(1).from);
    result.put(edges2.get(1).from, edges2.get(1).from);
    result.put(edges2.get(2).from, edges2.get(2).from);
    result.put(edges2.get(2).from, edges2.get(2).from);
    result.put(edges2.get(3).from, edges2.get(3).from);
    result.put(edges2.get(3).from, edges2.get(3).from);
    // edge3
    result2.put(edges3.get(0).from, edges3.get(0).from);
    result2.put(edges3.get(0).from, edges3.get(0).from);
    result2.put(edges3.get(1).from, edges3.get(1).from);
    result2.put(edges3.get(1).from, edges3.get(1).from);
    // size
    t.checkExpect(result.size(), 4);
    t.checkExpect(result2.size(), 2);
    t.checkExpect(result.equals(result2), false);

  }

  // to test method sortEdgesHelperer
  void testSortEdgesHelper(Tester t) {
    this.initData();

    MazeUtils mu = new MazeUtils();
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
    // calls the helper
    t.checkExpect(mu.sortEdges(this.edges1),
        new ArrayList<Edge>(Arrays.asList(this.e4, this.e3, this.e2, this.e1)));
    // check that the method didn't mutate the original list
    t.checkExpect(this.edges1,
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4)));
  }

  // to test method separate
  void testSeparate(Tester t) {
    this.initData();
    MazeUtils mu = new MazeUtils();
    MazeUtils mu1 = new MazeUtils();
    // outputs the edge so everything on the left is lighter
    // outputs the current highest weight to create separation
    mu.separate(edges1, 1, 4, e5);
    t.checkExpect(mu.separate(edges1, 1, 4, e5), 3);
    mu1.separate(edges2, 1, 4, e6);
    t.checkExpect(mu1.separate(edges2, 1, 2, e5), 1);
    // not the same
    t.checkExpect(mu.separate(edges1, 1, 4, e5) == mu1.separate(edges2, 1, 2, e5), false);
  }

  // to test method unionFind
  void testUnionFind(Tester t) {
    this.initData();
    testGame1.unionFind();
    t.checkExpect(testGame1.ufTree.size() < testGame1.cells.size() - 1, false);
    // edges are not the same (important to sort)
    t.checkExpect(testGame1.edges.get(0).from != testGame1.edges.get(2).from, true);
    t.checkExpect(testGame1.edges.get(1).from != testGame1.edges.get(2).from, true);
    t.checkExpect(testGame1.edges.get(0).to != testGame1.edges.get(2).to, true);
    t.checkExpect(testGame1.edges.get(1).to != testGame1.edges.get(2).to, true);
    t.checkExpect(testGame1.edges.get(0).from == testGame1.edges.get(0).from, true);
    t.checkExpect(testGame1.edges.get(1).from == testGame1.edges.get(1).from, true);
    t.checkExpect(testGame1.edges.get(1).from != testGame1.edges.get(1).to, true);
  }

  void testShowAnswer(Tester t) {
    this.initData();
    // answer path will lead out arraylist of cells (not null)
    t.checkExpect(this.testGame1.answerPath(this.testGame1.cells.get(0),
        this.testGame1.cells.get(this.testGame1.cells.size() - 1)) == null, false);
    t.checkExpect(this.testGame2.answerPath(this.testGame2.cells.get(0),
        this.testGame2.cells.get(this.testGame2.cells.size() - 1)) == null, false);
    t.checkExpect(this.testGame3.answerPath(this.testGame3.cells.get(0),
        this.testGame3.cells.get(this.testGame3.cells.size() - 1)) == null, false);
    // as answer is not null, then the path will be showed
    // false as it is not yet showed (but will be showed when key pressed)
    t.checkExpect(this.testGame1.showAnswerPath, false);
    t.checkExpect(this.testGame2.showAnswerPath, false);
    t.checkExpect(this.testGame3.showAnswerPath, false);
  }

  void testShowBreadthFirst(Tester t) {
    this.initData();
    // animate and show the depth first search
    t.checkExpect(this.testGame1.searchPath(this.testGame1.cells.get(0),
        this.testGame1.cells.get(this.testGame1.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);
    t.checkExpect(this.testGame2.searchPath(this.testGame2.cells.get(0),
        this.testGame2.cells.get(this.testGame2.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);
    t.checkExpect(this.testGame3.searchPath(this.testGame3.cells.get(0),
        this.testGame3.cells.get(this.testGame3.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);
    // shows once the key is pressed
    t.checkExpect(this.testGame1.showBFSPath, false);
    t.checkExpect(this.testGame2.showBFSPath, false);
    t.checkExpect(this.testGame3.showBFSPath, false);
  }

  void testShowDepthFirst(Tester t) {
    this.initData();
    // animate and show the depth first search
    t.checkExpect(this.testGame1.searchPath(this.testGame1.cells.get(0),
        this.testGame1.cells.get(this.testGame1.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);
    t.checkExpect(this.testGame2.searchPath(this.testGame2.cells.get(0),
        this.testGame2.cells.get(this.testGame2.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);
    t.checkExpect(this.testGame3.searchPath(this.testGame3.cells.get(0),
        this.testGame3.cells.get(this.testGame3.cells.size() - 1),
        new Queue<Cell>(new Deque<Cell>())) == null, false);

    // shows once the key is pressed
    t.checkExpect(this.testGame1.showDFSPath, false);
    t.checkExpect(this.testGame2.showDFSPath, false);
    t.checkExpect(this.testGame3.showDFSPath, false);
  }

  void testOnKeyEvent(Tester t) {
    this.initData();
    this.testGame1.onKeyEvent("up");
    this.testGame2.onKeyEvent("up");
    t.checkOneOf(this.testGame1.cells.get(0).y, -1, 0);
    t.checkOneOf(this.testGame2.cells.get(0).y, -1, 0);
    this.testGame1.onKeyEvent("down");
    this.testGame2.onKeyEvent("down");
    t.checkOneOf(this.testGame1.cells.get(0).y, 0, 1);
    t.checkOneOf(this.testGame2.cells.get(0).y, 0, 1);
    this.testGame1.onKeyEvent("left");
    this.testGame2.onKeyEvent("left");
    t.checkOneOf(this.testGame1.cells.get(0).x, -1, 0);
    t.checkOneOf(this.testGame2.cells.get(0).x, -1, 0);
    this.testGame1.onKeyEvent("right");
    this.testGame2.onKeyEvent("right");
    t.checkOneOf(this.testGame1.cells.get(0).x, 0, 1);
    t.checkOneOf(this.testGame2.cells.get(0).x, 0, 1);
    this.testGame1.onKeyEvent("b");
    this.testGame2.onKeyEvent("b");
    this.testGame3.onKeyEvent("b");
    t.checkExpect(this.testGame1.bfsPath.size() == 0, false);
    t.checkExpect(this.testGame2.bfsPath.size() == 0, false);
    t.checkExpect(this.testGame3.bfsPath.size() == 0, false);
    this.testGame1.onKeyEvent("a");
    this.testGame2.onKeyEvent("a");
    this.testGame3.onKeyEvent("a");
    t.checkExpect(this.testGame1.answerPath.size() == 0, false);
    t.checkExpect(this.testGame2.answerPath.size() == 0, false);
    t.checkExpect(this.testGame3.answerPath.size() == 0, false);
    this.testGame1.onKeyEvent("d");
    this.testGame2.onKeyEvent("d");
    this.testGame3.onKeyEvent("d");
    t.checkExpect(this.testGame1.dfsPath.size() > 0, true);
    t.checkExpect(this.testGame2.dfsPath.size() > 0, true);
    t.checkExpect(this.testGame3.dfsPath.size() > 0, true);
    this.testGame1.onKeyEvent("r");
    this.testGame2.onKeyEvent("r");
    t.checkExpect(
        this.testGame1.cells.get(this.testGame1.cells.size() - 1).color.equals(Color.BLUE), true);
    t.checkExpect(this.testGame1.showBFSPath, false);
    t.checkExpect(this.testGame1.showDFSPath, false);
    t.checkExpect(this.testGame1.showAnswerPath, false);

  }

  void testDrawCurrentBoard(Tester t) {
    // initialize the data
    this.initData();

    t.checkExpect(this.testGame3.drawCurrentBoard(), new AboveImage(
        new AboveImage(
            new AboveImage(
                new AboveImage(new EmptyImage(), new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0,
                                -24.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))))),
        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.blue))))));
  }

  // tests for makeScene using a random seed of 5
  void testMakeScene(Tester t) {
    // initialize the data
    this.initData();

    // testing
    WorldScene result = new WorldScene(this.testGame3.width * this.testGame3.cellSize,
        this.testGame3.height * this.testGame3.cellSize);

    // WorldImage resultBoard = this.testGame3.drawCurrentBoard();
    WorldImage resultBoard = new AboveImage(
        new AboveImage(
            new AboveImage(
                new AboveImage(new EmptyImage(), new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0,
                                -24.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new BesideImage(
                    new BesideImage(
                        new BesideImage(new EmptyImage(),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))))),
        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                        new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0,
                            0.0, new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray)))),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.blue)))));

    result.placeImageXY(resultBoard, (this.testGame3.cellSize * (this.testGame3.width)) / 2,
        (this.testGame3.cellSize * (this.testGame3.height)) / 2);

    t.checkExpect(this.testGame3.makeScene(), result);
  }

  // test makeEndScene using a random seed of 5
  void testMakeEndScene(Tester t) {
    this.initData();

    WorldScene result = new WorldScene(this.testGame3.width * this.testGame3.cellSize,
        this.testGame3.height * this.testGame3.cellSize);

    WorldImage black = new RectangleImage(100, 100, OutlineMode.OUTLINE, Color.black);

    WorldImage resultBoard = new AboveImage(
        new AboveImage(new EmptyImage(),
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                        new RectangleImage(50, 50, OutlineMode.SOLID, Color.orange))),
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.orange)))),
        new BesideImage(
            new BesideImage(new EmptyImage(),
                new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.gray))),
            new OverlayOffsetImage(new LineImage(new Posn(50, 0), Color.black), 0.0, -24.0,
                new OverlayOffsetImage(new LineImage(new Posn(0, 50), Color.black), -24.0, 0.0,
                    new RectangleImage(50, 50, OutlineMode.SOLID, Color.orange)))));

    WorldImage finalText = new TextImage("The maze is solved!", 20.0, FontStyle.REGULAR,
        Color.black);

    result.placeImageXY(black, 50, 50);
    result.placeImageXY(resultBoard, 50, 50);
    result.placeImageXY(finalText, 50, 50);

    t.checkExpect(this.testGame3.makeEndScene(), result);
  }

  // to test bigBang
  void testBigBang(Tester t) {
    this.initData();
    Maze m = this.testGame2;
    int worldWidth = (this.testGame2.cellSize * (this.testGame2.width));
    int worldHeight = (this.testGame2.cellSize * (this.testGame2.height));
    double tickRate = 0.25;
    m.bigBang(worldWidth, worldHeight, tickRate);
  }

}