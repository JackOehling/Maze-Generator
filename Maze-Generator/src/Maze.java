import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;

import javalib.worldimages.*;


// create cells using lines and then remove lines 

class Maze extends World {
  int sizex;
  int sizey;
  ArrayList<ArrayList<Vertex>> lov2d;
  ArrayList<ArrayList<Edge>> loe2d;
  Random rand;

  boolean dfsorbfs;


  ArrayList<Edge> finalloe;

  HashMap<Vertex, Vertex> reps = new HashMap<Vertex, Vertex>();

  int numberofticks;
  int numberoftickspath;


  ArrayList<Vertex> processed;

  ArrayList<Vertex> reconlist;



  Maze(int sizex, int sizey) {
    this.rand = new Random();
    this.sizex = sizex;
    this.sizey = sizey;
    this.lov2d = new ArrayList<ArrayList<Vertex>>();
    this.loe2d = new ArrayList<ArrayList<Edge>>();

    this.finalloe = this.kruskal();

    // true for dfs, false for bfs
    this.dfsorbfs = false;
    this.processed = new ArrayList<Vertex>();
    this.reconlist = new ArrayList<Vertex>();
    this.numberofticks = 0;
    this.numberoftickspath = 0;
  }


  Maze(Random rand, int sizex, int sizey) {
    this.rand = rand;
    this.sizex = sizex;
    this.sizey = sizey;
    this.lov2d = new ArrayList<ArrayList<Vertex>>();
    this.loe2d = new ArrayList<ArrayList<Edge>>();

    this.finalloe = this.kruskal();
    this.dfsorbfs = false;
    this.processed = new ArrayList<Vertex>();
    this.reconlist = new ArrayList<Vertex>();
    this.numberofticks = 0;
    this.numberoftickspath = 0;
  }

  public void onKeyEvent(String key) {

    if (key.equals("b")) {
      this.dfsorbfs = false;
      this.processed = this.mazeSolver();

    } else if (key.equals("d")) {
      this.dfsorbfs = true;
      this.processed = this.mazeSolver();
    }
  }

  public void onTick() {
    if (this.dfsorbfs) {
      if (this.numberofticks < processed.size()) {
        Vertex vertex = this.processed.get(numberofticks);
        vertex.color = new Color(240, 105, 105);
        this.numberofticks++;


      }
    } else {
      if (this.numberofticks < this.processed.size()) {
        Vertex vertex = this.processed.get(numberofticks);
        vertex.color = new Color(240, 105, 105);
        this.numberofticks++;
      }
    }

    if ((this.numberofticks >= this.processed.size())
        && this.numberoftickspath < this.reconlist.size()) {
      Vertex pathvert = this.reconlist.get(numberoftickspath);
      pathvert.color = Color.RED;
      this.numberoftickspath++;
    }
  }

  public ArrayList<Vertex> reconstruct(HashMap<Vertex, Edge> cfe, Vertex next) {
    ArrayList<Vertex> path = new ArrayList<Vertex>();
    while (cfe.get(next) != null) {
      Edge edge = cfe.get(next);
      path.add(next);

      if (edge.to.equals(next)) {
        next = edge.from;
      }
      else if (edge.from.equals(next)) {
        next = edge.to;
      } 
    }
    this.reconlist = path;
    return path;
  }

  // add colors to vertices
  // changes end point 
  public ArrayList<Vertex> mazeSolver() {
    HashMap<Vertex, Edge> cameFromEdge = new HashMap<Vertex, Edge>();

    //ICollection worklist = sorq; // A Queue or a Stack, depending on the algorithm

    Deque<Vertex> worklist = new ArrayDeque<Vertex>();

    ArrayList<Vertex> processed = new ArrayList<Vertex>();

    //initialize the worklist to contain the starting node
    worklist.addFirst(this.lov2d.get(0).get(0));

    while (!worklist.isEmpty()) {

      Vertex next = worklist.pop();

      if (processed.contains(next)) {

        // worklist.removeFirst();
      }
      else if (next.equals(lov2d.get(sizey - 1).get(sizex - 1))) {
        this.reconstruct(cameFromEdge, next);
        return processed;
      }
      else {
        processed.add(next);
        for (Edge edge : next.edgevertlist) {
          if (this.dfsorbfs) {
            worklist.addFirst(edge.otherside(next));
          }
          if (!this.dfsorbfs) {
            worklist.addLast(edge.otherside(next));
          }
          if (!edge.equals(cameFromEdge.get(next))) {
            cameFromEdge.put(edge.otherside(next), edge);
          }
        }


      }
    }
    return processed;
  }




  public WorldScene makeScene() {
    WorldScene w = new WorldScene(1000, 600);
    int worldsizespace = 600 / this.sizey;

    for (ArrayList<Vertex> vlist : this.lov2d) {
      for (Vertex v : vlist) {

        w.placeImageXY(v.drawVertex(worldsizespace),
            v.x * worldsizespace + worldsizespace / 2,
            v.y * worldsizespace + (worldsizespace / 2));

        if (v.left == null) {
          w.placeImageXY(new LineImage(new Posn(0, worldsizespace), Color.BLACK),
              v.x * worldsizespace,
              v.y * worldsizespace + worldsizespace / 2);
        }

        if (v.top == null) {
          w.placeImageXY(new LineImage(new Posn(worldsizespace, 0), Color.BLACK),
              v.x * worldsizespace + worldsizespace / 2,
              v.y * worldsizespace);
        }

        if (v.bottom == null) {
          w.placeImageXY(new LineImage(new Posn(worldsizespace, 0), Color.BLACK),
              v.x * worldsizespace,
              v.y * worldsizespace + worldsizespace);
        }

        if (v.right == null) {
          w.placeImageXY(new LineImage(new Posn(0, worldsizespace), Color.BLACK),
              v.x * worldsizespace + worldsizespace,
              v.y * worldsizespace);

        }
      }
    }

    for (ArrayList<Edge> list : this.loe2d) {
      for (Edge edge : list) {
        if (!this.finalloe.contains(edge)) {
          if (edge.from.y == edge.to.y) {
            w.placeImageXY(edge.drawEdge(worldsizespace),
                edge.from.x * worldsizespace + worldsizespace,
                edge.to.y * worldsizespace + worldsizespace / 2);  
          }
          else if (edge.from.x == edge.to.x) {
            w.placeImageXY(edge.drawEdge(worldsizespace),
                edge.from.x * worldsizespace + worldsizespace / 2,
                edge.to.y * worldsizespace);  
          }
        } 
      }
    }
    return w;

  }


  public ArrayList<Edge> sortedEdges() {
    ArrayList<Edge> sorted = new ArrayList<Edge>();


    for (int i = 0; i < this.loe2d.size(); i = i + 1) {
      for (int j = 0; j < this.loe2d.get(i).size(); j = j + 1) {
        sorted.add(loe2d.get(i).get(j));
      } 

    } 
    Collections.sort(sorted);
    return sorted;
  }


  public void makeHash() {

    for (int i = 0; i < this.lov2d.size(); i = i + 1) {
      for (int j = 0; j < this.lov2d.get(i).size(); j = j + 1) {
        Vertex currvert = this.lov2d.get(i).get(j);
        this.reps.put(currvert, currvert);

      }
    }
  }

  public Vertex find(Vertex vert) {
    if (this.reps.get(vert).equals(vert)) {
      return vert;
    } else {
      return find(this.reps.get(vert));
    }
  }

  public void union(Vertex vert1, Vertex vert2) {
    this.reps.put(vert1, vert2);
  }

  public boolean stillHasTree() {
    int index = 0;

    for (Entry<Vertex, Vertex> vertex : this.reps.entrySet()) {
      if (vertex.getKey().equals(vertex.getValue())) {
        index++;
      }
    }
    return index > 1;
  }

  public ArrayList<Edge> kruskal() {
    this.makeLov();
    this.connectEdgesAndList();
    this.makeHash();

    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();

    ArrayList<Edge> worklist = new ArrayList<Edge>(this.sortedEdges()); 



    while (this.stillHasTree()) {
      Edge currentEdge = worklist.get(0);

      if (find(currentEdge.from).equals(find(currentEdge.to))) {
        worklist.remove(0); 
      }
      else {
        edgesInTree.add(currentEdge);
        currentEdge.from.edgevertlist.add(currentEdge);
        currentEdge.to.edgevertlist.add(currentEdge);
        union(
            find(currentEdge.from),
            find(currentEdge.to));

      }
    }
    return edgesInTree;
  }

  public void makeLov() {

    for (int i = 0; i < this.sizey; i = i + 1) {

      ArrayList<Vertex> row = new ArrayList<Vertex>();
      for (int j = 0; j < this.sizex; j = j + 1) {
        Vertex vertex = new Vertex(j, i);
        row.add(vertex);

        if (i == 0 && j == 0) {
          vertex.color = Color.CYAN;
        }

        if (i == (this.sizey - 1) && j == (this.sizex - 1)) {
          vertex.color = Color.GREEN;
        }
        // top row
        if (i == 0 && j > 0 && j < this.sizex) {
          vertex.left = row.get(j - 1);
          row.get(j - 1).right = vertex;

          // left column
        } else if (i > 0 && j == 0 && i < this.sizex) {
          vertex.top = this.lov2d.get(i - 1).get(j);
          lov2d.get(i - 1).get(j).bottom = vertex;

          // middle to edges
        } else if (i > 0 && j > 0 && i < this.sizex) {
          vertex.top = lov2d.get(i - 1).get(j);
          vertex.left = row.get(j - 1);
          row.get(j - 1).right = vertex;
          lov2d.get(i - 1).get(j).bottom = vertex;
        }

      }
      lov2d.add(row);
    }

  }

  public void connectEdgesAndList() {


    for (int i = 0; i < this.sizey; i = i + 1) {
      ArrayList<Edge> row = new ArrayList<Edge>();

      for (int j = 0; j < this.sizex; j = j + 1) {
        Vertex currvertex = this.lov2d.get(i).get(j);

        if (currvertex.right != null) {
          Edge addedge = new Edge(currvertex, currvertex.right, this.rand.nextInt(4000));
          row.add(addedge);
        }

        if (currvertex.bottom != null) {
          Edge addedge = new Edge(currvertex, currvertex.bottom, this.rand.nextInt(4000));
          row.add(addedge);
        }

      }
      this.loe2d.add(row);
    } 
  }


}




class Edge implements Comparable<Edge> {
  Vertex from;
  Vertex to;
  int weight;

  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    // weight is going to be random
    this.weight = weight;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Edge)) { 
      return false; }
    // this cast is safe, because we just checked instanceof
    Edge that = (Edge)other;
    return this.from.equals(that.from) && this.to == that.to;
  }

  public int compareTo(Edge compareEdge) {
    return this.weight - compareEdge.weight;
  }

  public WorldImage drawEdge(int space) {
    if (this.from.y == this.to.y) {
      return (new LineImage(new Posn(0, space), Color.BLACK));
    } else {
      return (new LineImage(new Posn(space, 0), Color.BLACK));
    }
  }

  public Vertex otherside(Vertex vert) {
    if (this.from.equals(vert)) {
      return this.to;
    } else if (this.to.equals(vert)) {
      return this.from;
    } else {
      throw new IllegalArgumentException("This vertex is not connected to the edge");
    }
  }


}



// a class that represents a vertex
class Vertex {
  ArrayList<Edge> edgevertlist;
  Vertex top;
  Vertex right;
  Vertex bottom;
  Vertex left;
  int x;
  int y;
  Color color;

  Vertex(Vertex top, Vertex right, Vertex bottom, Vertex left, int x, int y) {
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    this.left = left;
    this.x = x;
    this.y = y;
    this.color = Color.WHITE;
    this.edgevertlist = new ArrayList<Edge>();
  }

  Vertex(int x, int y) {
    this.x = x;
    this.y = y;
    this.color = Color.WHITE;
    this.edgevertlist = new ArrayList<Edge>();
  }

  public WorldImage drawVertex(int size) {
    return new RectangleImage(size, size, "solid", this.color);
  }


}






class ExamplesMaze {
  ExamplesMaze(){}

  ArrayList<ArrayList<Vertex>> lov;
  ArrayList<ArrayList<Vertex>> lov2;

  ArrayList<ArrayList<Edge>> loe;

  Vertex vertex0;
  Vertex vertex1;
  Vertex vertex2;
  Vertex vertex3;
  Vertex vertex4;
  Vertex vertex5;
  Vertex vertex6;
  Vertex vertex7;
  Vertex vertex8;

  Vertex vertex0other;
  Vertex vertex1other;
  Vertex vertex2other;
  Vertex vertex3other;

  Edge edge0010;
  Edge edge1020;
  Edge edge0111;
  Edge edge1121;
  Edge edge0212;
  Edge edge1222;
  Edge edge0001;
  Edge edge1011;
  Edge edge2021;
  Edge edge0102;
  Edge edge1112;
  Edge edge2122;

  Maze maze;

  ArrayList<Vertex> solved;
  HashMap<Vertex, Vertex> hash1;





  void initData() {

    maze = new Maze(new Random(3), 3, 3);
    // top right bottom left
    vertex0 = new Vertex(0, 0);
    vertex1 = new Vertex(1, 0);
    vertex2 = new Vertex(2, 0);
    vertex3 = new Vertex(0, 1);
    vertex4 = new Vertex(1, 1);
    vertex5 = new Vertex(2, 1);
    vertex6 = new Vertex(0, 2);
    vertex7 = new Vertex(1, 2);
    vertex8 = new Vertex(2, 2);

    // vertex0
    vertex0.top = null;
    vertex0.left = null;
    vertex0.bottom = vertex3;
    vertex0.right = vertex1;

    // vertex1
    vertex1.top = null;
    vertex1.left = vertex0;
    vertex1.bottom = vertex4;
    vertex1.right = vertex2;

    // vertex2
    vertex2.top = null;
    vertex2.left = vertex1;
    vertex2.bottom = vertex5;
    vertex2.right = null;

    // vertex3
    vertex3.top = vertex0;
    vertex3.left = null;
    vertex3.bottom = vertex6;
    vertex3.right = vertex4;

    // vertex4
    vertex4.top = vertex1;
    vertex4.left = vertex3;
    vertex4.bottom = vertex7;
    vertex4.right = vertex5;

    // vertex5
    vertex5.top = vertex2;
    vertex5.left = vertex4;
    vertex5.bottom = vertex8;
    vertex5.right = null;

    // vertex6
    vertex6.top = vertex3;
    vertex6.left = null;
    vertex6.bottom = null;
    vertex6.right = vertex7;

    // vertex7
    vertex7.top = vertex4;
    vertex7.left = vertex6;
    vertex7.bottom = null;
    vertex7.right = vertex8;

    // vertex8
    vertex8.top = vertex5;
    vertex8.left = vertex7;
    vertex8.bottom = null;
    vertex8.right = null;

    lov = new ArrayList<ArrayList<Vertex>>(Arrays.asList(
        new ArrayList<Vertex>(Arrays.asList(vertex0, vertex1, vertex2)),

        new ArrayList<Vertex>(Arrays.asList(vertex3, vertex4, vertex5)),

        new ArrayList<Vertex>(Arrays.asList(vertex6, vertex7, vertex8))));


    this.edge0010 = new Edge(this.vertex0, this.vertex1, 8);
    this.edge1020 = new Edge(this.vertex1, this.vertex2, 10);
    this.edge0111 = new Edge(this.vertex3, this.vertex4, 27);
    this.edge1121 = new Edge(this.vertex4, this.vertex5, 17);
    this.edge0212 = new Edge(this.vertex5, this.vertex6, 12);
    this.edge1222 = new Edge(this.vertex7, this.vertex8, 29);
    this.edge0001 = new Edge(this.vertex0, this.vertex3, 9);
    this.solved = maze.mazeSolver();
    this.edge1011 = new Edge(this.vertex1, this.vertex4, 28);
    this.edge2021 = new Edge(this.vertex2, this.vertex5, 6);
    this.edge0102 = new Edge(this.vertex3, this.vertex6, 10);
    this.edge1112 = new Edge(this.vertex4, this.vertex7, 1);
    this.edge2122 = new Edge(this.vertex5, this.vertex8, 20);

    loe = new ArrayList<ArrayList<Edge>>(Arrays.asList(
        new ArrayList<Edge>(Arrays.asList(edge0010, edge1020)),
        new ArrayList<Edge>(Arrays.asList(edge0111, edge1121))));
  }


  void initData1() {
    // top right bottom left
    vertex0other = new Vertex(0, 0);
    vertex1other = new Vertex(1, 0);
    vertex2other = new Vertex(0, 1);
    vertex3other = new Vertex(1, 1);

    // vertex0
    vertex0other.top = null;
    vertex0other.left = null;
    vertex0other.bottom = vertex2other;
    vertex0other.right = vertex1other;

    // vertex1
    vertex1other.top = null;
    vertex1other.left = vertex0other;
    vertex1other.bottom = vertex3other;
    vertex1other.right = null;

    // vertex2
    vertex2other.top = vertex0other;
    vertex2other.left = null;
    vertex2other.bottom = null;
    vertex2other.right = vertex3other;

    // vertex3
    vertex3other.top = vertex1other;
    vertex3other.left = vertex2other;
    vertex3other.bottom = null;
    vertex3other.right = null;

    lov2 = new ArrayList<ArrayList<Vertex>>(Arrays.asList(
        new ArrayList<Vertex>(Arrays.asList(vertex0other, vertex1other)),
        new ArrayList<Vertex>(Arrays.asList(vertex2other, vertex3other))));


    hash1 = new HashMap<Vertex, Vertex>();
    hash1.put(vertex3other, vertex3other);
    hash1.put(vertex2other, vertex2other);
    hash1.put(vertex1other, vertex1other);
    hash1.put(vertex0other, vertex0other);



  }


  // drawVertex

  void testDrawVertex(Tester t) {
    this.initData();
    t.checkExpect(vertex0.drawVertex(20) , new RectangleImage(20,20, "solid", Color.WHITE));
    t.checkExpect(vertex1.drawVertex(20) , new RectangleImage(20,20, "solid", Color.WHITE));
  }

  // compareTo
  void testCompareTo(Tester t) {
    this.initData();
    t.checkExpect(new Edge(vertex0, vertex1, 4).compareTo(new Edge(vertex0, vertex1, 3)) , 1);
    t.checkExpect(new Edge(vertex3, vertex4, 20).compareTo(new Edge(vertex7, vertex8, 3)) , 17);
  }

  // compareTo
  void testDrawEdge(Tester t) {
    this.initData();
    t.checkExpect(new Edge(vertex0, vertex1, 4).drawEdge(3),
        (new LineImage(new Posn(0, 3), Color.BLACK)));
    t.checkExpect(new Edge(vertex3, vertex4, 20).drawEdge(7),
        (new LineImage(new Posn(0, 7), Color.BLACK)));
    t.checkExpect(new Edge(vertex0, vertex3, 4).drawEdge(10),
        (new LineImage(new Posn(10, 0), Color.BLACK)));
    t.checkExpect(new Edge(vertex5, vertex8, 20).drawEdge(30),
        (new LineImage(new Posn(30, 0), Color.BLACK)));
  }


  //tests makeLov

  void testMakeLov(Tester t) {
    this.initData();
    Maze starterWorld = new Maze(3, 3);
    //t.checkExpect(starterWorld.lov2d, this.lov);
  }

  void testMakeLov1(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);
    starterWorld.makeLov();
    t.checkExpect(starterWorld.lov2d.size(), 4);
  }



  //sortedEdges

  void testSortedEdges(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(new Random(2), 2, 1);
    Maze starterWorld1 = new Maze(new Random(90), 20, 10);
    starterWorld.makeLov();
    starterWorld.connectEdgesAndList();
    t.checkExpect(starterWorld.sortedEdges().get(0).weight
        < starterWorld.sortedEdges().get(1).weight , true);
    t.checkExpect(starterWorld1.sortedEdges().get(15).weight
        < starterWorld1.sortedEdges().get(19).weight , true);


  }


  // tests connectEdgesAndList
  void testMakeLoe(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);
    Maze starterWorld1 = new Maze(3, 3);
    t.checkExpect(starterWorld.loe2d.get(0).size()
        + starterWorld.loe2d.get(1).size(), 4);
    t.checkExpect(starterWorld1.loe2d.get(0).size()
        + starterWorld1.loe2d.get(1).size()
        + starterWorld1.loe2d.get(2).size(), 12);

  }

  //hashMap

  void testInitReps(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);
    starterWorld.makeLov();
    starterWorld.makeHash() ;
    HashMap<Vertex, Vertex> representatives = starterWorld.reps;
    for (ArrayList<Vertex> currrow : starterWorld.lov2d) {
      for (Vertex vertex : currrow) {
        t.checkExpect(representatives.get(vertex), vertex);
      }
    }
  }

  void testUnionAndFind(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);

    starterWorld.makeHash();
    starterWorld.makeLov();
    starterWorld.connectEdgesAndList();
    Vertex vertex = starterWorld.lov2d.get(0).get(0);
    Vertex vertex1 = starterWorld.lov2d.get(0).get(1);
    t.checkExpect(starterWorld.find(vertex), vertex);
    t.checkExpect(starterWorld.find(vertex1), vertex1);

    starterWorld.union(starterWorld.find(vertex), starterWorld.find(vertex1));
    t.checkExpect(starterWorld.find(vertex), starterWorld.find(vertex1));
    t.checkExpect(starterWorld.find(vertex1), starterWorld.find(vertex));


  }


  void testHasMultipleTrees(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);

    starterWorld.makeHash();
    starterWorld.makeLov();
    starterWorld.connectEdgesAndList();
    Vertex vertex = starterWorld.lov2d.get(0).get(0);
    Vertex vertex1 = starterWorld.lov2d.get(0).get(1);
    t.checkExpect(starterWorld.stillHasTree(), true) ;
    starterWorld.union(starterWorld.find(vertex),starterWorld.find(vertex1));
    t.checkExpect(starterWorld.stillHasTree(), true);
  }

  // tests kruskal

  void testKruskal(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(5, 5);
    Maze starterWorld1 = new Maze(10, 10);
    // shows that the final edges list has the same count as n-1 vertices
    t.checkExpect(starterWorld.finalloe.size() == 24, true);
    t.checkExpect(starterWorld1.finalloe.size() == 99, true);

  }

  // tests makeScene

  void testmakeScene(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(2, 2);

    t.checkExpect(starterWorld.makeScene(), starterWorld.makeScene());


  }

  // mazeSolver
  void testmazeSolver(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(new Random(3), 3, 3);
    Maze starterWorld1 = new Maze(new Random(3), 2, 2);

    t.checkExpect(starterWorld.mazeSolver(), this.solved);
    //t.checkExpect(starterWorld1.MazeSolver(), null);
  }


  // onTick
  void testOnTick(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(new Random(3), 3, 3);
    Maze starterWorld1 = new Maze(new Random(3), 2, 2);
    starterWorld.onTick();
    t.checkExpect(starterWorld.mazeSolver(), starterWorld.mazeSolver());
    t.checkExpect(starterWorld.loe2d.get(1).get(2), this.edge2122.from);
    //t.checkExpect(starterWorld1.MazeSolver(), null);
  }


  // onKeyEvent
  void testOnKeyEvent(Tester t) {
    this.initData1();
    Maze starterWorld = new Maze(new Random(3), 3, 3);

    t.checkExpect(starterWorld.dfsorbfs, false);
    t.checkExpect(starterWorld.processed, new ArrayList<Vertex>());
    starterWorld.onKeyEvent("d");
    t.checkExpect(starterWorld.dfsorbfs, true);
    t.checkExpect(starterWorld.processed, starterWorld.mazeSolver());
    starterWorld.onKeyEvent("b");
    t.checkExpect(starterWorld.dfsorbfs, false);
    t.checkExpect(starterWorld.processed, starterWorld.mazeSolver());
  }


  // otherside

  void testOtherSide(Tester t) {
    this.initData();

    t.checkExpect(this.edge0111.otherside(vertex3), vertex4);
    t.checkExpect(this.edge1222.otherside(vertex7), vertex8);
    t.checkException(new IllegalArgumentException("This vertex is not connected to the edge"),
        edge1020, "otherside", vertex8);


  }

  // reconstruct
  void testReconstruct(Tester t) {
    this.initData();

    t.checkExpect(this.edge0111.otherside(vertex3), vertex4);
    t.checkExpect(this.edge1222.otherside(vertex7), vertex8);

  }

  // tests starting the game
  void testFloodIt(Tester t) {
    Maze starterWorld = new Maze(3000, 3000);
    starterWorld.bigBang(5000, 5000, 0.001);
  }

}



