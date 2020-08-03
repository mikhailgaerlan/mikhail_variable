package agents;
/* Copyright 2019 Keith Burghardt
 * Edited by Mikhail Gaerlan
 */



import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

public class AgentsSimulation extends SimState { //extend the class SimState

	public static boolean record_params_each_time = false;
	//Experiment setup
    public static boolean experimentMode = true; 
    public static long lengthOfSimulations = 12; //how many steps in a run? (maximum)
    public static int numberOfSimulations = 10; 	// The number of Simulations -- need more for smoother data. 
    public static String datafilename = "pilot.csv"; //the name of the file where data will be saved. 
    public static int stepCount = 1; //collect Dynamic data every [this] number of time steps.  
	public static Data data; //Object controlling data output to file. 
	public boolean collectFullNetwork = true;//If true, collect full adjacency matrix. Otherwise, don't. TODO

	
	//Display of the network. 
	public ContinuousPortrayal2D agentsPortrayal_layer1 = new ContinuousPortrayal2D();
	public ContinuousPortrayal2D agentsPortrayal_layer2 = new ContinuousPortrayal2D();
	public int gridWidth = 70;
	public int gridHeight = 70;
	public Continuous2D agentsSpace1; //displays the agents for layer 1
	public Continuous2D agentsSpace2; //displays the agents for layer 2
	public Network[] links; //the network edges for layers 1 and 2
	public static boolean doGraphics = true; 
	public Color bgcolor = Color.white;
	
	//Model Parameters
	public static int NUM_PLAYERS = 6; 
	public static double noise = 0.0; //probability of random tie change 
	public static double tie_benefit_1 = 1.0; //benefit to tie in layer 1. 
	public static double tie_benefit_2 = 1.0; //benefit to tie in layer 2. 
	public static double tie_cost_1 = -0.2; //cost to tie in layer 1. 
	public static double tie_cost_2 = -0.2; //cost to tie in layer 2. 
	public static double triangle_payoff_1 = 0.3; //payoff for closed triangle in layer 1. 
	public static double triangle_payoff_2 = 0.3; //payoff for closed triangle in layer 2. 
	public static double spillover_payoff = 0.2; //bonus for spillover ties (ties present in each layer)
	public static boolean alwaysStartSearchAtLayer0 = false; //if true, always start search for partner in layer 0.  	
	//shocks
	public static int timeOfShock = 5; //When to administer shock. 
	public static int numAgentsShocked = 6; // How many agents shocked?
	public static int numAgentsOverlapped = 2; //How many agents get spillover?
	public static double postShockTieCost_1 = -0.6; //new tie_cost_1 for shocked agent
	public static double postShockTieCost_2 = -0.6; //new tie_cost_2 for shocked agent
	public boolean preshock = true; //a shock hasn't happened yet. 
	public boolean timeToEnd = false; //is it time to finish?
	//scaling
	public static int searchSize = 6; //number of nodes agent searches for a possible add edge
	//single network
	public static boolean oneLayerOnly = false; //if true, ignore spillover and limit to one layer only.
	public static boolean shockOneLayer = false; //if true, only shock one layer.
	public static boolean whichLayerShocked = false; //if shockOneLayer and if true, shock layer 0, else shock layer 1
	
	//agent management
	GamePlayerAgent gpa = null; //this agent steps through the agents and adjusts ties. 
	Agent[] agentList = null;
	
	//stability
	public int timeSinceLastChange = 0; //used for stopping condition. Stop after 3 rounds w no changes. 
	public boolean[][][] oldCoplayerMatrix = new boolean[2][6][6];
	public int[] whoGetsSpillover; //array of who gets spillover
	public boolean shockAtEquilibrium = false; //if true, shock once equilibrium is reached. Otherwise, shock at designated time. 
	
	//data collection
	public boolean[][][] coplayerMatrix; //co-player matrix [layer][agent1][agent2]
	public static int equilTime1; //time of equilibrium pre-shock
	public int equilTime2; //time of equilibrium post-shock
	
	public static int[][] adjIndices;  //output for adjacency matrix
	public static double[][] agentUtils; //matrix of agent current and cumulative utilities
	
	/** GET and SET Methods ***********************************************************************************/

	public int getNUM_PLAYERS (){   	return NUM_PLAYERS;        }
    public void setNUM_PLAYERS(int w){    if(w > 1 && w <= 1000){	NUM_PLAYERS = w;      }  }
    public int getgridWidth (){   	return gridWidth;        }
    public void setgridWidth(int w){    if(w >= 10 && w <= 1000){	gridWidth = w; gridHeight = w;     }  }
	public double getnoise (){   	return noise;        }
    public void setnoise(double w){    if(w >= 0 && w <= 1){	noise = w;      }  }
    public int getsearchSize(){   	return searchSize;        }
    public void setsearchSize(int w){    if(w >= 1 && w <= NUM_PLAYERS){	searchSize = w;      }  }
    public double gettie_benefit_1 (){   	return tie_benefit_1;        }
    public void settie_benefit_1(double w){    if(w >= 0){	tie_benefit_1 = w;      }  }
    public double gettie_benefit_2 (){   	return tie_benefit_2;        }
    public void settie_benefit_2(double w){    if(w >= 0){	tie_benefit_2 = w;      }  }
    public double gettie_cost_1 (){   	return tie_cost_1;        }
    public void settie_cost_1(double w){    if(w <= 0){	tie_cost_1 = w;      }  }
    public double gettie_cost_2 (){   	return tie_cost_2;        }
    public void settie_cost_2(double w){    if(w <= 0){	tie_cost_2 = w;      }  }
    public double gettriangle_payoff_1 (){   	return triangle_payoff_1;        }
    public void settriangle_payoff_1(double w){    triangle_payoff_1 = w;       }
    public double gettriangle_payoff_2 (){   	return triangle_payoff_2;        }
    public void settriangle_payoff_2(double w){    triangle_payoff_2 = w;       }
    public double getspillover_payoff (){   	return spillover_payoff;        }
    public void setspillover_payoff(double w){   spillover_payoff = w;        }
    public boolean getalwaysStartSearchAtLayer0 (){   	return alwaysStartSearchAtLayer0;        }
    public void setalwaysStartSearchAtLayer0(boolean w){   alwaysStartSearchAtLayer0 = w;        }
    public boolean getoneLayerOnly (){   	return oneLayerOnly;        }
    public void setoneLayerOnly(boolean w){   oneLayerOnly = w;        }
    public String get_(){ return "SHOCK VARIABLES";}
    public boolean getshockAtEquilibrium (){   	return shockAtEquilibrium;        }
    public void setshockAtEquilibrium(boolean w){   shockAtEquilibrium = w;        }
    public int gettimeOfShock (){   	return timeOfShock;        }
    public void settimeOfShock(int w){   if(w >= 0) timeOfShock = w;        }
    public int getnumAgentsShocked (){   	return numAgentsShocked;        }
    public void setnumAgentsShocked(int w){   if(w >= 0 && w <= NUM_PLAYERS) numAgentsShocked = w;        }
    public double getpostShockTieCost_1 (){   	return postShockTieCost_1;        }
    public void setpostShockTieCost_1(double w){   if(w <= 0) postShockTieCost_1 = w;        }
    public double getpostShockTieCost_2 (){   	return postShockTieCost_2;        }
    public void setpostShockTieCost_2(double w){   if(w <= 0) postShockTieCost_2 = w;        }
    
	
	/***********************************************************************************************************/
	/**
	 * Constructor method
	 */
	public AgentsSimulation(long seed) {
            super(seed); //use the constructor for SimState
	}
	/************************************************* START METHOD ********************************************/
	public void start(){
		super.start(); // reuse the SimState start method
		createGrids(); //make a space for where the agents live 
		createAgents(); //create the agents and place them on the networks. 
		data = new Data(NUM_PLAYERS,lengthOfSimulations); //Create the new data class and headers    
		equilTime1 = -1;
		equilTime2 = -1;
		preshock = true;
		timeToEnd = false;
		agentUtils = new double[2][NUM_PLAYERS];
	}
	/************************************************* end start method *****************************************/
	


	/** Initialization Methods ***********************************************************************************/

	
	/**
	 * Initialize a matrix for outputting data. 
	 */
	public void makeAdjIndices(){
		int layers = oneLayerOnly ? 1 : 2; 
		adjIndices = new int[6][NUM_PLAYERS*NUM_PLAYERS*layers];
		int count = 0; 
		for(int k = 0; k < layers; k++)	
			for (int i = 0; i < NUM_PLAYERS; i++) {
				for(int j = 0; j < NUM_PLAYERS; j++){
					adjIndices[0][count] = i;//node_1
					adjIndices[1][count] = j;//node_2
					adjIndices[2][count] = k;//layer
					adjIndices[3][count] = this.coplayerMatrix[k][i][j] ? 1 : 0; //edge
					if (shockOneLayer) {
						if (whichLayerShocked) {
							adjIndices[4][count] = (1-k); //not shocked player
						}
						else {
							adjIndices[4][count] = k; //shocked player
						}
						}
					else {
						adjIndices[4][count] = 1; //shocked_player
					}
					adjIndices[5][count] = this.agentList[i].if_overlapped ? 1 : 0;
					count++;
				}
			}
	}

	
	/*
	 * Create a space for where the agents live. 
	 * 
	 */
	public void createGrids(){
		agentsSpace1 = new Continuous2D(gridWidth, gridWidth,gridHeight);
		agentsSpace2 = new Continuous2D(gridWidth, gridWidth,gridHeight);
		links = new Network[3]; //layer 1, layer 2, spillover
		links[0] = new Network();
		links[1] = new Network();
		links[2] = new Network();
		
		coplayerMatrix = new boolean[2][NUM_PLAYERS][NUM_PLAYERS];
		whoGetsSpillover = new int[NUM_PLAYERS];
		oldCoplayerMatrix = new boolean[2][NUM_PLAYERS][NUM_PLAYERS];
		timeSinceLastChange = 0; 
		bgcolor = Color.white;
	}
	
	
	/*
	 * Create 6 agents, each with an index. 
	 */
	public void createAgents(){
		agentList = new Agent[NUM_PLAYERS];		
	
		double radius  = gridWidth*0.4; 
		double[] angles = new double[NUM_PLAYERS];
		double delta_angle = 2*Math.PI/(double)NUM_PLAYERS;
		for(int i = 0; i < NUM_PLAYERS; i++)
			angles[i] = (double)i*delta_angle;
		
		double[] xpositions = new double[NUM_PLAYERS];
		double[] ypositions = new double[NUM_PLAYERS];
		
		for(int i = 0; i < NUM_PLAYERS; i++){
			double[] xy = polarToCartesian(radius, angles[i]);
			double x = xy[0];
			double y = xy[1];
			
			Agent a = new Agent(i, tie_cost_1, tie_cost_2); 
			agentList[i] = a;
			agentsSpace1.setObjectLocation(a, new Double2D(x,y)); //place in layer 1
			agentsSpace2.setObjectLocation(a, new Double2D(x,y)); //played in layer 2
			links[0].addNode(a); //add as node in network 1
			links[1].addNode(a); //add as node in network 2
			links[2].addNode(a); //add as node in network 2
			
			
		}
		gpa = new GamePlayerAgent();
		gpa.event = schedule.scheduleRepeating(gpa, 1, 1); //set the agent who performs tie adjustments to step	
	}
	
	
	
	
	/** Operational Methods ***********************************************************************************/

	/*
	 * Takes polar coordinates and converts to Cartesian coordinates. 
	 */
	public double[] polarToCartesian(double r, double theta){
		double x = 0, y = 0; 
		x = r*Math.cos(theta) + (double)gridWidth*0.5;
		y = r*Math.sin(theta) + (double)gridHeight*0.5;
		double[] xy = {x, y};
		return xy;
	}
	
	
	/**
	 * Calculates if the network hasn't chanted in X number of time steps. 
	 * If so, returns true, otherwise false. 
	 * Let's do this the easy way. Just check for changes in the last X time steps. 
	 * If none, end simulation (return true) 
	 */
	public boolean pairwiseStability(){
		if(anyChanges()){ //if any new changes to coplayer matrix since last time step. 
			oldCoplayerMatrix = cloneMatrix(coplayerMatrix); //make the old matrix the current matrix. 
			timeSinceLastChange = 0; 
		}
		else
			timeSinceLastChange++; 
		
		return (timeSinceLastChange >= 5) ? true : false; 	
	}
	
	/*
	 * Returns true if there have been any changes between the current coplayer matrix and a recent one, 
	 * otherwise returns false. 
	 */
	public boolean anyChanges(){
		for(int i = 0; i < 2; i++)
			for(int j = 0; j < NUM_PLAYERS; j++){
				if(!java.util.Arrays.equals(coplayerMatrix[i][j], oldCoplayerMatrix[i][j]))
					return true;
					
				}
		return false;	
	}
	
	
	public void spillOverEdgeViz(){
		links[2].removeAllEdges();
		for(int i = 0; i < NUM_PLAYERS; i++)
			for(int j = 0; j < NUM_PLAYERS; j++){
				if(coplayerMatrix[0][i][j] && coplayerMatrix[1][j][i]){
					Agent a = agentList[i];
					Agent b = agentList[j];
					links[2].addEdge(a, b, 2);	
				}
			}
	}
	
	/**
	 * Create a new tie between Agents a and b in layer. 
	 */
	public void tie_form(Agent a, Agent b, int layer){
		if(coplayerMatrix[layer][a.index][b.index]){ //check if there is already a tie. 
			return;		
		}
		else{
			coplayerMatrix[layer][a.index][b.index] = true;
			coplayerMatrix[layer][b.index][a.index] = true;
			links[layer].addEdge(a, b, 1);	
			links[layer].addEdge(b, a, 1);	
			if(doGraphics)
				spillOverEdgeViz();
		}
	}
	
	
	/**
	 * Delete existing tie between Agents a and b in layer. 
	 */
	public void tie_delete(Agent a, Agent b, int layer){
		if(!coplayerMatrix[layer][a.index][b.index]){ //check if there is already a tie. 
			return;
		}
		else{
			coplayerMatrix[layer][a.index][b.index] = false;
			coplayerMatrix[layer][b.index][a.index] = false;
			Edge e1 = links[layer].getEdge(a, b);
			Edge e2 = links[layer].getEdge(b, a);
			links[layer].removeEdge(e1);	
			links[layer].removeEdge(e2);
			if(doGraphics)
				spillOverEdgeViz();
		}
	}
	
	/**
	 * Returns the current utility of agent ego
	 */
	public double currentUtility(Agent ego){
		double util = 0; 
		
		//add direct benefits and costs of ties
		double numTies1 = (double)(ego.numTies(this, 0));
		double numTies2 = (double)(ego.numTies(this, 1));
		util += tie_benefit_1*numTies1 + tie_benefit_2*numTies2;
		util += ego.tie_cost_1*(numTies1*numTies1) + ego.tie_cost_2*(numTies2*numTies2);
		
		//add benefits/costs of closed triangles
		double numTriangles1 = (double)(ego.numTriangles(this, 0));
		double numTriangles2 = (double)(ego.numTriangles(this, 1));
		util += triangle_payoff_1*numTriangles1 + triangle_payoff_2*numTriangles2;
		
		//add spillover benefits
		if (ego.if_overlapped) {
			util += spillover_payoff*(double)(ego.numSpilloverTies(this));
		}
		return util;
	}
	
	/**
	 * Returns the utility of agent ego for a given (hypothetical) adjacency matrix
	 */
	public double utility(Agent ego, boolean[][][] adjMatrix){
		double util = 0; 
		
		//add direct benefits and costs of ties
		double numTies1 = (double)(ego.numTies(this, 0, adjMatrix));
		double numTies2 = (double)(ego.numTies(this, 1, adjMatrix));
		util += tie_benefit_1*numTies1 + tie_benefit_2*numTies2;
		util += ego.tie_cost_1*(numTies1*numTies1) + ego.tie_cost_2*(numTies2*numTies2);
		
		//add benefits/costs of closed triangles
		double numTriangles1 = (double)(ego.numTriangles(this, 0, adjMatrix));
		double numTriangles2 = (double)(ego.numTriangles(this, 1, adjMatrix));
		util += triangle_payoff_1*numTriangles1 + triangle_payoff_2*numTriangles2;
		
		//add spillover benefits
		if (ego.if_overlapped) {
			util += spillover_payoff*(double)(ego.numSpilloverTies(this, adjMatrix));
		}
		
		return util;
	}
	
	/**
	 * Returns the utility of agent ego if a hypothetical partner were added in layer
	 */
	public double utilityIfAdded(Agent ego, Agent other, int layer){
		boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
		adjmatrix[layer][ego.index][other.index] = true;
		adjmatrix[layer][other.index][ego.index] = true;
		double newUtilOther = utility(ego, adjmatrix);
		return newUtilOther;
	}
	
	/**
	 * Returns the utility of agent ego if a hypothetical partner were added in layer
	 */
	public double utilityIfAddDrop(Agent ego, Agent addAgent, Agent dropAgent, int addLayer, int dropLayer){
		boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
		//add agent
		adjmatrix[addLayer][ego.index][addAgent.index] = true;
		adjmatrix[addLayer][addAgent.index][ego.index] = true;
		//drop agent
		adjmatrix[dropLayer][ego.index][dropAgent.index] = false;
		adjmatrix[dropLayer][dropAgent.index][ego.index] = false;
		double newUtilOther = utility(ego, adjmatrix);
		return newUtilOther;
	}
	
	
	
	/**
	 * Return a vector of the utilities gains if any current ties are dropped. 
	 */
	public double[] lossUtilities(Agent ego, int layer){
		double[] lossUtil = new double[NUM_PLAYERS];
		
		for(int i = 0; i < NUM_PLAYERS; i++){
			if(i == ego.index || !coplayerMatrix[layer][ego.index][i]) //ignore if self or no tie present
				lossUtil[i] = 0;
			else{
				boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
				adjmatrix[layer][ego.index][i] = false;
				adjmatrix[layer][i][ego.index] = false;
				lossUtil[i] = utility(ego, adjmatrix);
			}
		}
		return lossUtil;
	}
	
	/**
	 * Return a matrix of the utilities gains if any current ties are dropped. 
	 */
	public double[][] lossUtilities(Agent ego){
		double[][] lossUtil = new double[2][NUM_PLAYERS];
		
		for(int j = 0; j < 2; j++){
			for(int i = 0; i < NUM_PLAYERS; i++){
				if(i == ego.index || !coplayerMatrix[j][ego.index][i]) //ignore if self or no tie present
					lossUtil[j][i] = -9999;
				else{
					boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
					adjmatrix[j][ego.index][i] = false;
					adjmatrix[j][i][ego.index] = false;
					lossUtil[j][i] = utility(ego, adjmatrix);
				}
			}
		}
		return lossUtil;
	}
	
	
	/**
	 * Return a vector of the utilities gains if any new ties are added. 
	 */
	public double[] addUtilities(Agent ego, int layer){
		double[] addUtil = new double[NUM_PLAYERS];
		
		for(int i = 0; i < NUM_PLAYERS; i++){
			if(i == ego.index || coplayerMatrix[layer][ego.index][i]) //ignore if self or tie already present
				addUtil[i] = -9999;
			else{
				boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
				adjmatrix[layer][ego.index][i] = true;
				adjmatrix[layer][i][ego.index] = true;
				addUtil[i] = utility(ego, adjmatrix);
			}
		}
		return addUtil;
	}
	
	/**
	 * Return a matrix of the utilities gains if any new ties are added, for each layer
	 */
	public double[][] addUtilities(Agent ego){
		double[][] addUtil = new double[2][NUM_PLAYERS];
		
		for(int j = 0; j < 2; j++){ //check both layers
			for(int i = 0; i < NUM_PLAYERS; i++){
				if(i == ego.index || coplayerMatrix[j][ego.index][i]) //ignore if self or tie already present
					addUtil[j][i] = -9999;
				else{
					boolean[][][] adjmatrix = cloneMatrix(coplayerMatrix);
					adjmatrix[j][ego.index][i] = true;
					adjmatrix[j][i][ego.index] = true;
					addUtil[j][i] = utility(ego, adjmatrix);
				}
			}
		}
		return addUtil;
	}
	
	/*
	 * Shuffle
	 * Had to make my own shuffler. 
	 * Takes as argument a vector of Agents. Returns a vector of the same in random order. 
	 */
	public Agent[] shuffle(Agent[] list){
		if(list == null || list.length <= 1)
			return null;
		int[] order = shuffleOrder(list.length);		
		Agent[] newAgentList = new Agent[list.length];
		for(int i = 0; i < list.length; i++){
			newAgentList[i] = list[order[i]];
		}
		return newAgentList;
	}
	
	public int[] shuffleOrder(int size) {
		int[] array = new int[size];
		for(int j = 0; j < size; j++)
			array[j] = j;
        for (int i = size; i > 1; i--) {
            swap(array, i - 1, random.nextInt(i));
        }
        return array;
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
	
	
	
	
	public void printCoPlayerMatrix(){
		System.out.println("Layer 0");
		System.out.println("\t" + "[0]" + "\t"+ "[1]" + "\t" + "[2]" + "\t" + "[3]" + "\t" + "[4]" + "\t" + "[5]" );
		for(int i = 0; i < NUM_PLAYERS; i++){
			System.out.print("[" + i + "]" + "\t");
			for(int j = 0; j < NUM_PLAYERS; j++){
				int x = coplayerMatrix[0][j][i] ? 1 : 0; 
				System.out.print(x + "\t");
			}
			System.out.println("");
		}
	}
	
	public void printTrueClone(){
		boolean[][][] adjMatrix = cloneMatrix(coplayerMatrix);
		for(int i = 0; i < 6; i++)
			for(int j = 0; j < 6; j++)
				adjMatrix[0][i][j] = true;
		
		System.out.println("Layer 0");
		System.out.println("\t" + "[0]" + "\t"+ "[1]" + "\t" + "[2]" + "\t" + "[3]" + "\t" + "[4]" + "\t" + "[5]" );
		for(int i = 0; i < NUM_PLAYERS; i++){
			System.out.print("[" + i + "]" + "\t");
			for(int j = 0; j < NUM_PLAYERS; j++){
				int x = adjMatrix[0][j][i] ? 1 : 0; 
				System.out.print(x + "\t");
			}
			System.out.println("");
		}	
	}
	
	/**
	 * Creates a new 3D boolean matrix with values identical to the inputted matrix
	 * Used to make a copy of the coplayerMatrix.
	 */
	public boolean [][][] cloneMatrix(boolean[][][] matrix){
		int a = matrix.length;
		int b = matrix[0].length;
		int c = matrix[0][0].length;
		
		boolean[][][] clone = new boolean[a][b][c];
		for(int i = 0; i < a; i++)
			for(int j = 0; j < b; j++)
				for(int k = 0; k < c; k++)
					clone[i][j][k] = matrix[i][j][k];
		return clone;
	}
	
	
	/**
	 * Shock the system!
	 */
	public void shockNetwork(){
		if(numAgentsShocked < 1) return; //do nothing if no one is shocked. 
		if(numAgentsShocked > NUM_PLAYERS)
			numAgentsShocked = NUM_PLAYERS;
		Agent[] agentListShuf;
		
		if(this.numAgentsShocked == this.NUM_PLAYERS)
			agentListShuf = agentList;
		else
			agentListShuf = shuffle(agentList); //create a new shuffled list
		
		//System.out.println(tie_cost_1+" "+tie_cost_2);
		//System.out.println(postShockTieCost_1);
		//System.out.println(schedule.getSteps());
		for(int i = 0; i < numAgentsShocked; i++){
			Agent a = agentListShuf[i]; //shock the agent
			//if (i == 0)
				//System.out.println(shockOneLayer+" "+a.tie_cost_1+" "+a.tie_cost_2);
			if (shockOneLayer) {//if one layer shocked
				if (whichLayerShocked) { //choose which layer to shock
					a.tie_cost_1 = postShockTieCost_1;
					//tie_cost_1 = postShockTieCost_1;
				}
				else {
					a.tie_cost_2 = postShockTieCost_2;
					//tie_cost_2 = postShockTieCost_2;
				}
			}
			else {//shock both layers
				a.tie_cost_1 = postShockTieCost_1;
				a.tie_cost_2 = postShockTieCost_2;
				//tie_cost_1 = postShockTieCost_1;
				//tie_cost_2 = postShockTieCost_2;
			}
			//if (i==0)
			//	System.out.println(shockOneLayer+" "+a.tie_cost_1+" "+a.tie_cost_2);
		}
		//System.out.println("Shocked!!");
		//System.out.println(tie_cost_1+" "+tie_cost_2);
		bgcolor = Color.yellow; //I can't get this to work just now. Not a big priority. 
	}
	
	/**
	 * Returns the factorial of x (x!)
	 */
	public int factorial(int x){
		int y = 1;
		int total = 1;
		do{
			y++;
			total = total * y;
		}while(y < x);
		return total;
	}
	
	
	/**
	 * Return average degree of nodes in layer 1, layer 2, and projection network
	 * {layer 1, layer 2, projection}
	 */
	public double[] averageDegree(){
		double degree1 = 0;
		double degree2 = 0; 
		double degreeProj = 0;
		
		for(int i = 0; i < NUM_PLAYERS; i++){
			Agent a = (Agent)agentList[i];
			double d1 = a.numTies(this, 0);
			double d2 = a.numTies(this, 1);
			degree1 += d1;
			degree2 += d2;
			degreeProj  += (d1 + d2);
		}
		double ad1 = degree1/(double)NUM_PLAYERS;
		double ad2 = degree2/(double)NUM_PLAYERS;
		double adP = degreeProj/(double)NUM_PLAYERS;
		double[] ad = {ad1, ad2, adP};
		return ad;
	}
	
	
	/**
	 * returns the average clustering coefficient for the network. 
	 */
	public double averageClustering(int layer){
		if(oneLayerOnly && layer > 0) // don't bother if unused layer. 
			return 0;
		double c = 0;
		for(int i = 0; i < NUM_PLAYERS; i++){
			Agent a = agentList[i];
			c += a.clustering(this, layer);
			if(layer == 0){
				a.c_1 = a.clustering(this, layer);
			}
			else if(layer==1){
				a.c_2 = a.clustering(this, layer);
			}
		}
		return c/(double)(NUM_PLAYERS);
	}
	
	/**
	 * returns the local clustering coefficient for each node
	 */
	public double[] localClustering(int layer){
		double[] c = new double[NUM_PLAYERS];
		if(oneLayerOnly && layer > 0) // don't bother if unused layer. 
			return c;
		
		for(int i = 0; i < NUM_PLAYERS; i++){
			Agent a = agentList[i];
			c[i] = a.clustering(this, layer);
		}
		return c;
	}
	
/************************* MAIN LOOP - BATCH MODE (TEXT) / SINGLE RUN MODE (COMPATIBLE WITH GRAPHICS) *************/
	
    public static void main(String[] args){
    	/*--------------- experimentMode = TRUE --------------------------------------------------- */
        if (experimentMode == true) { //are we running a (batch of) experiment(s)? 
               doGraphics = false; 
            try {
	        	PrintWriter pwExperimentData = new PrintWriter(new FileWriter(new File(datafilename))); //where data will be saved.
	        	Data dat = new Data(NUM_PLAYERS,lengthOfSimulations);
	            //Data dat = new Data(NUM_PLAYERS,lengthOfSimulations);
	        	Bag headerBag = dat.headerBag; //headers to data file
	        	//System.out.println(headerBag.numObjs);
	        	/*new int[headerBag.numObjs];
	        	for (int i = 0; i < headerBag.numObjs; i++) {
	        		whichparams[i] = i;
	        	}*/
	        	//int[] whichparams = {}; //which parameters to print
	            for(int i = 0; i < headerBag.numObjs; i++) { //write out data file headers
                    pwExperimentData.print((String)headerBag.get(i));
                    pwExperimentData.print(",");
                }
                pwExperimentData.println();  
                
                int batchRun = 0; //keep track of which batch of parameters is being run. 
                int modelRun = 0;
                int printRun = 0; //To keep track of runs visually. 
                
                int shock_index = 0;
                /*
                 * This is where to play with parameter values. 
                 * We can set up for-loops to go through various values for each parameter. 
                 */
                //PARAMETER VALUES FOR BATCH RUNS 
            	int[] numPlayers_array = {6}; //{20, 40, 60, 80}; //number of players
            	double[] tieben1_array = {1}; //tie_benefit_1
            	double[] noise_array = {0,0.1,0.3};//,0.25,0.5,0.75}; //tie_benefit_2
            	double[] tiecost1_array = {-0.2,-0.6}; //initial tie cost for both layers
            	double[] tiecost2_array = {0}; //tie_cost_2 //DEFUNCT
            	double[] trianglepay1_array = {0.3,0.9};//{0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0}; //triangle payoffs for both layers
            	double[] trianglepay2_array = {0}; //triangle_payoff_2 //DEFUNCT

            	double[] spilloverpay_array = {0.9};
            	boolean[] startLayer0_array = {false}; //alwaysStartSearchAtLayer0
            	boolean[] shockOne_array = {false, true}; //if true, shock just one layer
            	boolean[] whichLayerShocked_array = {false, true}; //if only layer shocked specify which layer //do not change
            	int[] timeOfShock_array = {5}; //timeOfShock
            	int[] numShocked_array = {NUM_PLAYERS}; //numAgentsShocked //DEFUNCT
            	int[] numOverlapped_array = {2,4}; //numOverlapped
            	double[] postShockTC1_array = {-0.2,-0.6}; //postShockTieCost_1, 2 for both layers
            	
            	double[] runClusters = {3};
            	int numTotalSims = numPlayers_array.length 
            			* tieben1_array.length 
            			* noise_array.length 
            			* tiecost1_array.length 
            			* tiecost2_array.length 
            			* trianglepay1_array.length 
            			* trianglepay2_array.length 
            			* spilloverpay_array.length 
            			* startLayer0_array.length
            			* timeOfShock_array.length 
            			* numShocked_array.length 
            			* postShockTC1_array.length 
            			* runClusters.length
            			* numOverlapped_array.length
            			* 3;//(one for when both layers are shocked and two for just one layer is shocked) shockOne_array.length
            			//* whichLayerShocked_array.length;
            	
            	for (int a = 0; a < numPlayers_array.length; a++) {
                	NUM_PLAYERS = numPlayers_array[a];
                	//numAgentsShocked = NUM_PLAYERS;
                	searchSize = 10; //(NUM_PLAYERS > 50) ? 10 : 5;
                	oneLayerOnly = false;
                	for (int b = 0; b < tieben1_array.length; b++) {
                		tie_benefit_1 = tieben1_array[b];
                		tie_benefit_2 = tieben1_array[b];
                		for (int c = 0; c < noise_array.length; c++) {
                			noise = noise_array[c];
                        	for (int d = 0; d < tieben1_array.length; d++) {
                        		//tie_benefit_1 = tieben1_array[d];
                        		for (int e = 0; e < tiecost1_array.length; e++) {
                        			tie_cost_1 = tiecost1_array[e];
                        			tie_cost_2 = tiecost1_array[e];                       	                      			
                                	for (int f = 0; f < tiecost2_array.length; f++) {
                                		//tie_cost_2 = tiecost2_array[f];
                                		for (int g = 0; g < trianglepay1_array.length; g++) {
                                			triangle_payoff_1 = trianglepay1_array[g];
                                			triangle_payoff_2 = trianglepay1_array[g];
                                        	for (int h = 0; h < trianglepay2_array.length; h++) {
                                        		//triangle_payoff_2 = trianglepay2_array[h];
                                        		for (int i = 0; i < spilloverpay_array.length; i++) {
                                        			spillover_payoff = spilloverpay_array[i];
                                        			//oneLayerOnly = spillover_payoff > 0 ? false : true;
                                                	for (int j = 0; j < startLayer0_array.length; j++) {
                                                		alwaysStartSearchAtLayer0 = startLayer0_array[j];
                                                		for (int k = 0; k < timeOfShock_array.length; k++) {
                                                			timeOfShock = timeOfShock_array[k];
                                                        	for (int l = 0; l < numShocked_array.length; l++) {
                                                        		numAgentsShocked = numShocked_array[l];
                                                                for (int m = 0; m < postShockTC1_array.length; m++) {
                                                                    //shock_index = 1 - e;
                                                                	postShockTieCost_1 = postShockTC1_array[m];
                                                                    postShockTieCost_2 = postShockTC1_array[m];
                                                                        for (int n = 0; n < numOverlapped_array.length; n++) {
                                                                        	numAgentsOverlapped = numOverlapped_array[n];
                                                                        	for (int o = 0; o < shockOne_array.length; o++) {
                                                                        		shockOneLayer = shockOne_array[o];
                                                                        			for (int p = shockOneLayer ? 0 : 1; p < whichLayerShocked_array.length; p++) {
                                                                        				whichLayerShocked = whichLayerShocked_array[p];
                                                                	for (int w = 0; w < runClusters.length; w++) {
                                                                		double rcl = 0.5*(double)runClusters.length;
                                                                		

                                                                		
               //WHAT REPEATS FOR EVERY PARAMETER COMBO   
               modelRun = 0;
               printRun++;                     
               boolean stop = false;
               for (int z=0; z < numberOfSimulations; z++) { //how many repetitions for each parameter combo
   	               //for(int p : whichparams) { //write out data file headers
                   //    pwExperimentData.print((String)headerBag.get(p));
                   //    pwExperimentData.print(",");
                   //}
   	               //pwExperimentData.println();
                   System.out.println("Batch " + printRun + " of " + numTotalSims + ", run " + (modelRun+1));
                   long steps = 0;
                   AgentsSimulation as = new AgentsSimulation(System.currentTimeMillis());
                   as.start();
                   modelRun++;
                   //for (int x : whichparams) {
                   //for (int x = 0; x < ds.numObjs; x++) {
                   //pwExperimentData
					//		.print(ds.objs[x]);
                	 //  pwExperimentData
                	 //  		.print(",");
                   //}
                   
                   //make a matrix of the adjacency matrix indices
                   
                   
                // simulation
					do {
						if (!as.schedule
								.step(as)) { // stops
												// if
												// it
												// can't
												// process
												// another
												// iteration
												// of
												// the
												// model
							System.out
									.println("ouch");
							break;
						}
						steps = as.schedule
								.getSteps();

						// write
						// dynamics
						// data

						// if
						// time
						// to
						// print
						// data
						if (steps <= as.lengthOfSimulations)/*if (steps == 0
								|| as.timeToEnd
								//|| steps <= as.lengthOfSimulations)// 
							    ||steps == (as.timeOfShock -1)|| steps == as.lengthOfSimulations)*/
						{
							Bag ds = as.data
									.getData(
											(SimState) as,
											modelRun,
											printRun);
							if (steps == 1) {
								for (int x = 0; x < ds.numObjs; x++) {
									pwExperimentData
										.print(ds.objs[x]);
									pwExperimentData
										.print(",");
								}
							}

							// Agent
							// current_agent;
							if (as.collectFullNetwork) {
								as.makeAdjIndices(); // TODO
								for (int y = 0; y < as.adjIndices[0].length; y++) {
									/*if (as.record_params_each_time
											|| (steps == 1 && !stop)
											|| (steps == (as.timeOfShock -1) && !stop)) {
										//for(int x = 0; x < headerBag.numObjs; x++) { //write out data file headers
						                //       pwExperimentData.print((String)headerBag.get(x));
						                //       pwExperimentData.print(",");
						                //}
										//pwExperimentData.println();
										//for (int x : whichparams) {
										for (int x = 0; x < ds.numObjs; x++) {
											pwExperimentData
													.print(ds.objs[x]);
											pwExperimentData
													.print(",");
										}
										//pwExperimentData.println();
									}*/
									int index = as.adjIndices[0][y];
									Agent current_agent = as.agentList[index];
									// if(as.record_params_each_time||(steps
									// ==
									// 1&&!stop)){
									if (steps != 1 || y != 0){
										for (int x = 0; x < ds.numObjs; x++) {
											pwExperimentData.print(",");
										}

									}
									//pwExperimentData.print(current_agent.tie_cost_1);
									//pwExperimentData.print(",");
									//pwExperimentData.print(current_agent.tie_cost_2);
									//pwExperimentData.print(",");
									stop = true;
									
									//pwExperimentData.print(current_agent.c_1);//current_agent.clustering(as,0));
									//pwExperimentData.print(",");
									//pwExperimentData.print(current_agent.c_2);
									//pwExperimentData.print(",");
									pwExperimentData.print(steps);//step
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[4][y]);//shocked
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[5][y]);//overlapped
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[2][y]);//layer
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[0][y]);//i
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[1][y]);//j
									pwExperimentData.print(",");
									pwExperimentData.print(as.adjIndices[3][y]);//edge
									pwExperimentData.print(",");
									pwExperimentData.print(as.agentUtils[0][index]);//recent
									pwExperimentData.print(",");
									pwExperimentData.print(as.agentUtils[1][index]);//cumulative

									pwExperimentData.println();
								}
							} else {
								for (int x = 0; x < ds.numObjs; x++) {
									pwExperimentData
											.print(ds.objs[x]);
									pwExperimentData
											.print(",");
								}
								double avgCost = 0;
								for (int x = 0; x < as.NUM_PLAYERS; x++) {
									Agent aa = as.agentList[x];// previously
																// this
																// was
																// "[i]".
																// That
																// is
																// probably
																// wrong
									avgCost += aa.util;
								}
								avgCost = avgCost
										/ (double) as.NUM_PLAYERS;
								pwExperimentData
										.print("0,0,0,0,");
								pwExperimentData
										.print(avgCost);
								pwExperimentData
										.print(",0,");
								
								pwExperimentData
										.println();
							}

						}
					} while (!as.timeToEnd
							&& steps < lengthOfSimulations);// &&
															// equilTime1
															// <
															// 0);
															// //the
															// latter
															// stops
															// after
															// equilibrium.
					// finish
					// the
					// simulation
					as.finish();
					System.gc();
				}
                                                                				}//w
                                                                			}//p
                                                                		}//o
                                                                	}//n
                                                        		}//m
                                                        	}//l
                                                		}//k
                                                	}//j
                                        		}//i
                                        	}//h
                                		}//g
                                	}//f
                        		}//e
                        	}//d
                		}//c
                	}//b
            	}//a
            	pwExperimentData.close();//close data file. 	   
            } catch (IOException e) {}
               System.exit(0);           
        }//end if(experiment == true)
    	
    }
	
	
	
}//end class
