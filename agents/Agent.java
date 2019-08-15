package agents;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Bag;

public class Agent {
	public int index; //the agent's index, so that other agents can identify it. 
	double payoff; //the total payoff for the current round. 
	
	double tie_cost_1;
	double tie_cost_2;
	double c_1;
	double c_2;
	 double util_before_turn=0; 
	 double util=0; 
	 double cumulativeUtil = 0;
	 
	 boolean if_overlapped = false;
	 
	 //number of links added/dropped each round
	 int num_links_added = 0;
	 int num_links_dropped = 0;
	 
	 /*
	  * GET methods (for viz)
	  */
	 public double getutility (){    return util;        }
	 
	 /**
	  * Constructor
	  */
	 public Agent(int i, double tc1, double tc2){
	  index = i; 
	  tie_cost_1 = tc1;
	  tie_cost_2 = tc2;
	  c_1 = 0;
	  c_2 = 0;
	  util = 0; 
	  cumulativeUtil = 0; 
	  num_links_added = 0;
	  num_links_dropped = 0;
	  if_overlapped = false;
	 }

	
	/**
	 * Clustering coefficient for this agent/node in layer x
	 * C = (2 * # triangles)/(k*(k-1)), where k is number of edges. 
	 */
	public double clustering(AgentsSimulation as, int layer){
		double k = (double)numTies(as, layer);
		if(k < 2)
			return 0; 
		double tri = (double)numTriangles(as, layer);
		double c = (2 * tri)/(k*(k-1));
		return c;
	}
	
	
	/*
	 * Return true if agent is fully connected (to all nodes in both layers), false otherwise. 
	 */
	public boolean fullyConnected(AgentsSimulation as){
		if(as.oneLayerOnly && numTies(as, 0) == as.NUM_PLAYERS - 1)
			return true;
		else if(numTies(as, 0) < as.NUM_PLAYERS - 1 || numTies(as, 1) < as.NUM_PLAYERS - 1)
			return false; 
		else return true;	
	}
	
	/*
	 * Return true if agent is has no current ties, false otherwise. 
	 */
	public boolean noTies(AgentsSimulation as){
		if(numTies(as, 0) > 0 || numTies(as, 1)  > 0)
			return false; 
		else return true; 
	}
	
	
	/**
	 * Return the number of ties held in layer
	 */
	public int numTies(AgentsSimulation as, int layer){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && as.coplayerMatrix[layer][this.index][i])
				count++;
		}
		return count;
	}
	
	/**
	 * Return the number of ties held in layer, for a hypothetical adjacency matrix
	 */
	public int numTies(AgentsSimulation as, int layer, boolean[][][] adjMatrix){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && adjMatrix[layer][this.index][i])
				count++;
		}
		return count;
	}
	
	/**
	 * Return the number of closed triangles in layer
	 */
	public int numTriangles(AgentsSimulation as, int layer){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && as.coplayerMatrix[layer][this.index][i]){
				for(int j = 0; j < as.NUM_PLAYERS; j++){
					if(j != i && j != this.index && 
							as.coplayerMatrix[layer][i][j] && as.coplayerMatrix[layer][this.index][j])
						count++;
				}
			}
		}
		count = (int)((double)count * 0.5); //correct for double-counting.
		return count;
	}
	
	/**
	 * Return the number of closed triangles in layer, for a hypothetical adjacency matrix
	 */
	public int numTriangles(AgentsSimulation as, int layer, boolean[][][] adjMatrix){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && adjMatrix[layer][this.index][i]){
				for(int j = 0; j < as.NUM_PLAYERS; j++){
					if(j != i && j != this.index && 
							adjMatrix[layer][i][j] && adjMatrix[layer][this.index][j])
						count++;
				}
			}
		}
		count = (int)((double)count * 0.5); //correct for double-counting.
		return count;
	}
	
	
	/**
	 * Return the number of spillover ties (ties in both layers)
	 */
	public int numSpilloverTies(AgentsSimulation as){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && as.coplayerMatrix[0][this.index][i] && as.coplayerMatrix[1][this.index][i])
				count++;
		}
		return count;
	}
	
	/**
	 * Return the number of spillover ties (ties in both layers), for hypothetical adjacency matrix
	 */
	public int numSpilloverTies(AgentsSimulation as, boolean[][][] adjMatrix){
		int count = 0; 
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			if(i != this.index && adjMatrix[0][this.index][i] && adjMatrix[1][this.index][i])
				count++;
		}
		return count;
	}
	
	
	/**
	 * Return the {layer, index, utility gain} of the agent of an edge not currently connected, 
	 * whose addition would be result in the largest marginal utility gain. 
	 * index = -1 if fully connected. 
	 */
	public double[] bestAdd(AgentsSimulation as){	
		if(fullyConnected(as)){//don't bother if agent is fully connected. 
			double[] temp =  {0.0, -1, 0};
			return temp;
		}
		//otherwise, find the highest utility add. 
		double currentUtil = as.currentUtility(this); //agent's current utility
		double[][] addUtil = as.addUtilities(this); //a matrix of all the new utilities after adds	
		double maxGain = -998; 	
		int layer = as.random.nextInt(2); //start search at random layer
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly) layer = 0; //force search to start at layer 0
		int i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
		int bestIndex = i; 
		int bestLayer = layer; 	
		int countLAYER = 0; 
		int countPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(countLAYER < max){
			while(countPLAYER < as.NUM_PLAYERS){
				if(i != this.index && !isTie(as, i, layer) && (addUtil[layer][i] - currentUtil) > maxGain){ 
					maxGain = addUtil[layer][i] - currentUtil;
					bestIndex = i; 
					bestLayer = layer;
					//System.out.println("New best move in layer " + layer);
				}
				i++;
				if(i >= as.NUM_PLAYERS) i = 0;
				countPLAYER++;
			}
		layer = layer + 1 - 2*layer;
		countLAYER++;
		i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
		countPLAYER = 0; 
		}
		double[] bestAdd = {bestLayer, bestIndex, maxGain};
		return bestAdd; 
	}
	
	/**
	 * Return the {layer, index, utility gain} of the agent of an edge not currently connected, 
	 * whose addition would be result in the largest marginal utility gain. 
	 * index = -1 if fully connected. 
	 * This one only searches among a list of searchSize randomly selected agents, rather than the entire population.
	 */
	public double[] bestAdd(AgentsSimulation as, int searchSize){	
		if(fullyConnected(as)){//don't bother if agent is fully connected. 
			double[] temp =  {0.0, -1, 0};
			return temp;
		}
		//otherwise, find the highest utility add. 
		if(searchSize > as.NUM_PLAYERS)
			searchSize = as.NUM_PLAYERS;
		double currentUtil = as.currentUtility(this); //agent's current utility
		double[][] addUtil = as.addUtilities(this); //a matrix of all the new utilities after adds	
		double maxGain = -998; 	
		int layer = as.random.nextInt(2); //start search at random layer
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly) layer = 0; //force search to start at layer 0
		int i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
		int init = i;
		int bestIndex = i; 
		int bestLayer = layer; 	
		int countLAYER = 0; 
		int countPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(countLAYER < max){
			while(countPLAYER < searchSize){ //as.NUM_PLAYERS){
				if(i != this.index && !isTie(as, i, layer) && (addUtil[layer][i] - currentUtil) > maxGain){ 
					maxGain = addUtil[layer][i] - currentUtil;
					bestIndex = i; 
					bestLayer = layer;
					//System.out.println("New best move in layer " + layer);
				}
				i = as.random.nextInt(as.NUM_PLAYERS);
				if(i >= as.NUM_PLAYERS) i = 0;
				countPLAYER++;
			}
		layer = layer + 1 - 2*layer;
		countLAYER++;
		countPLAYER = 0; 
		}
		double[] bestAdd = {bestLayer, bestIndex, maxGain};
		return bestAdd; 
	}
	
	public double[] bestAddSmartSearch(AgentsSimulation as, int searchSize){	
		if(fullyConnected(as)){//don't bother if agent is fully connected. 
			double[] temp =  {0.0, -1, 0};
			return temp;
		}
		//otherwise, find the highest utility add. 
		if(searchSize > as.NUM_PLAYERS)
			searchSize = as.NUM_PLAYERS;
		double currentUtil = as.currentUtility(this); //agent's current utility
		double[][] addUtil = as.addUtilities(this); //a matrix of all the new utilities after adds	
		double maxGain = -998; 	
		int layer = as.random.nextInt(2); //start search at random layer
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly) layer = 0; //force search to start at layer 0
		int i,j;
		int init;
		int bestIndex = 0; 
		int bestLayer = layer; 	
		int countLAYER = 0; 
		int countPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		// search in a "smart" way until we have looked at all neighbor's neighbors
		for(layer=0;layer<max;layer++)
		{
			if(numTies(as,layer) > 0 && searchSize > 0)
			{
				for(i = 0; i < as.NUM_PLAYERS; i++)
				{
					// if neighbor
					if(i != this.index && as.coplayerMatrix[layer][this.index][i]&&searchSize > 0)
					{
						// check spillover or triangle benefits with equal probability
						int spillover_or_tri = as.random.nextInt(2);
						// check if spillover is a good idea
						if(spillover_or_tri==1)
						{
							//opposite layer
							int Opplayer = layer+1-2*layer;
							// if there is not a tie on the opposite layer 
							// and adding a tie on the opposite later is the best idea yet...
							if(!isTie(as, i,Opplayer ) && (addUtil[Opplayer][i] - currentUtil) > maxGain)
							{
								// find node name j associated with tie i<->j
								maxGain = addUtil[Opplayer][i] - currentUtil;
								bestIndex = i; 
								bestLayer = Opplayer;
							}
							// search size reduced by 1
							searchSize -= 1;
						}
						//check if we can make triangle benefits
						else
						{
							// look at neighbor's neighbors, if they exist, 
							for(j=0; j < as.NUM_PLAYERS; j++)
							{
								//if j is not the neighbor
								// and j is not you, and j is one of i's neighbors: (You)---(i)---(j!=you)
								if(j != i && j != this.index && as.coplayerMatrix[layer][i][j] && searchSize > 0)
								{
									// if we can still look
									if(countPLAYER < searchSize){ //as.NUM_PLAYERS){
										//if there is not a tie to j already, and adding creates a greater utility
										if(!isTie(as, j, layer) && (addUtil[layer][j] - currentUtil) > maxGain){ 
											maxGain = addUtil[layer][j] - currentUtil;
											bestIndex = j; 
											bestLayer = layer;
											//System.out.println("New best move in layer " + layer);
										}
										countPLAYER++;
									}
							
									// search size reduced by 1
									searchSize -= 1;
								}
							}
						}
					}
				}
			}
		}	
		// if we still have neighbor's neighbors to look through, look at random
		if (searchSize > 0)
		{
			//repeat of bestAdd(as,searchSize);
			layer = as.random.nextInt(2); //start search at random layer
			i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
			init = i;
			bestIndex = i; 
			bestLayer = layer; 	
			countLAYER = 0; 
			countPLAYER = 0; 
			max = 2;
			if(as.oneLayerOnly) max = 1;
			while(countLAYER < max){
				while(countPLAYER < searchSize){ //as.NUM_PLAYERS){
					if(i != this.index && !isTie(as, i, layer) && (addUtil[layer][i] - currentUtil) > maxGain){ 
						maxGain = addUtil[layer][i] - currentUtil;
						bestIndex = i; 
						bestLayer = layer;
						//System.out.println("New best move in layer " + layer);
					}
					i = as.random.nextInt(as.NUM_PLAYERS);
					if(i >= as.NUM_PLAYERS) i = 0;
					countPLAYER++;
				}
			layer = layer + 1 - 2*layer;
			countLAYER++;
			countPLAYER = 0; 
			}
		}
		double[] bestAdd = {bestLayer, bestIndex, maxGain};
		return bestAdd; 
	}
	
	
	/**
	 * Return the {layer, index, utility gain} of the agent of an edge currently connected, 
	 * whose deletion would be result in the largest marginal utility gain. 
	 * Index = -1 if no current edges
	 */
	public double[] bestDrop(AgentsSimulation as){		
		if(noTies(as)){//don't bother if agent no current edges. 
			double[] temp =  {0.0, -1, 0};
			return temp;
		}
		//otherwise, find the highest utility drop. 
		double currentUtil = as.currentUtility(this); //agent's current utility
		double[][] lossUtil = as.lossUtilities(this); //a matrix of all the new utilities after losses
		
		double maxGain = -998; 
		
		int layer = as.random.nextInt(2); //start search at random layer
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly) layer = 0; //force search to start at layer 0
		int i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
		int bestIndex = i; 
		int bestLayer = layer; 	
		int countLAYER = 0; 
		int countPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(countLAYER < max){
			while(countPLAYER < as.NUM_PLAYERS){
				if(i != this.index && isTie(as, i, layer) && (lossUtil[layer][i] - currentUtil) > maxGain){ 
					maxGain = lossUtil[layer][i] - currentUtil;
					bestIndex = i; 
					bestLayer = layer;
				}
				i++;
				if(i >= as.NUM_PLAYERS) i = 0;
				countPLAYER++;
			}
		layer = layer + 1 - 2*layer;
		countLAYER++;
		i = as.random.nextInt(as.NUM_PLAYERS); //start search at random player
		countPLAYER = 0; 
		}
		double[] bestDrop = {bestLayer, bestIndex, maxGain};
		return bestDrop; 
	}
	
	
	/**
	 * Considers each combination of adding and dropping ties, and returns the one with highest marginal utility. 
	 * Returns double[2][3]: [0=add, 1=drop][0=layer, 1=index, 2=utility gain]
	 */
	public double[][] bestAddDropCombo(AgentsSimulation as){		
		if(this.noTies(as) || this.fullyConnected(as)){//don't bother if agent no current edges or fully connected
			double[][] temp =  {{0.0, -1, 0}, {0.0, -1, 0}};
			return temp;
		}
		double currentUtil = as.currentUtility(this); //agent's current utility
		double maxGain = -998; 	
		int addLayer = as.random.nextInt(2); //to get next, i = i + 1 - 2*i (starts search at random layer)
		int dropLayer = as.random.nextInt(2);
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly){
			addLayer = 0; 
			dropLayer = 0; 
		}			
		int i = as.random.nextInt(as.NUM_PLAYERS); //start add search at random player
		int j = as.random.nextInt(as.NUM_PLAYERS); //start drop search at random player
		int bestAddIndex = i; 
		int bestAddLayer = addLayer; 
		int bestDropIndex = j; 
		int bestDropLayer = dropLayer; 
		int addCount = 0; 
		int dropCount = 0; 	
		int countADDPLAYER = 0; 
		int countDROPPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(addCount < max){//start at random layer			
			while(countADDPLAYER < as.NUM_PLAYERS){//loop over potential adds
				if(i != this.index && !isTie(as, as.agentList[i], addLayer)){ //if no tie exists 					
					Agent addAgent = as.agentList[i];					
					while(dropCount < max){ //loop over potential deletes
						while(countDROPPLAYER < as.NUM_PLAYERS){
							if(j != this.index && j != i && isTie(as, as.agentList[j], dropLayer)){ //if tie exists
								Agent dropAgent = as.agentList[j];
								double newUtil = as.utilityIfAddDrop(this, addAgent, dropAgent, addLayer, dropLayer);
								double gain = newUtil - currentUtil;
								if(gain > maxGain){//if best move
									maxGain = gain;
									bestAddIndex = i; 
									bestAddLayer = addLayer; 
									bestDropIndex = j; 
									bestDropLayer = dropLayer; 
								}	
							}
							j++;
							if(j >= as.NUM_PLAYERS) j = 0;
							countDROPPLAYER++;	
						}
						dropLayer = dropLayer + 1 - 2*dropLayer;
						dropCount++;	
						countDROPPLAYER = 0;
						j = as.random.nextInt(as.NUM_PLAYERS);
					}
				}
				i = as.random.nextInt(as.NUM_PLAYERS);
				if(i >= as.NUM_PLAYERS) i = 0;
				countADDPLAYER++;				
			}
			addLayer = addLayer + 1 - 2*addLayer;
			addCount++;
			countADDPLAYER = 0;
			i = as.random.nextInt(as.NUM_PLAYERS); 
		}
		double[][] bestAddDrop = {{bestAddLayer, bestAddIndex, maxGain}, {bestDropLayer, bestDropIndex, maxGain}};
		return bestAddDrop; 
	}
	
	
	/**
	 * Considers each combination of adding and dropping ties, and returns the one with highest marginal utility. 
	 * Returns double[2][3]: [0=add, 1=drop][0=layer, 1=index, 2=utility gain]
	 */
	public double[][] bestAddDropCombo(AgentsSimulation as, int searchSize){		
		if(this.noTies(as) || this.fullyConnected(as)){//don't bother if agent no current edges or fully connected
			double[][] temp =  {{0.0, -1, 0}, {0.0, -1, 0}};
			return temp;
		}
		
		if(searchSize > as.NUM_PLAYERS)
			searchSize = as.NUM_PLAYERS;
		double currentUtil = as.currentUtility(this); //agent's current utility
		double maxGain = -998; 	
		int addLayer = as.random.nextInt(2); //to get next, i = i + 1 - 2*i (starts search at random layer)
		int dropLayer = as.random.nextInt(2);
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly){
			addLayer = 0; 
			dropLayer = 0; 
		}			
		int i = as.random.nextInt(as.NUM_PLAYERS); //start add search at random player
		int j = as.random.nextInt(as.NUM_PLAYERS); //start drop search at random player
		int bestAddIndex = i; 
		int init_i = i;
		int init_j = j;
		int bestAddLayer = addLayer; 
		int bestDropIndex = j; 
		int bestDropLayer = dropLayer; 
		int addCount = 0; 
		int dropCount = 0; 	
		int countADDPLAYER = 0; 
		int countDROPPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(addCount < max){//start at random layer			
			while(countADDPLAYER < searchSize){ //as.NUM_PLAYERS){//loop over potential adds
				if(i != this.index && !isTie(as, as.agentList[i], addLayer)){ //if no tie exists 					
					Agent addAgent = as.agentList[i];					
					while(dropCount < max){ //loop over potential deletes
						while(countDROPPLAYER < searchSize){ //as.NUM_PLAYERS){
							if(j != this.index && j != i && isTie(as, as.agentList[j], dropLayer)){ //if tie exists
								Agent dropAgent = as.agentList[j];
								double newUtil = as.utilityIfAddDrop(this, addAgent, dropAgent, addLayer, dropLayer);
								double gain = newUtil - currentUtil;
								if(gain > maxGain){//if best move
									maxGain = gain;
									bestAddIndex = i; 
									bestAddLayer = addLayer; 
									bestDropIndex = j; 
									bestDropLayer = dropLayer; 
								}	
							}
							j = as.random.nextInt(as.NUM_PLAYERS);;
							//if(j >= as.NUM_PLAYERS) j = 0;
							countDROPPLAYER++;	
						}
						dropLayer = dropLayer + 1 - 2*dropLayer;
						dropCount++;	
						countDROPPLAYER = 0;
						//j = as.random.nextInt(as.NUM_PLAYERS);
						j = init_j; //search same set of nodes each time. 
					}
				}
				i = as.random.nextInt(as.NUM_PLAYERS);
				//if(i >= as.NUM_PLAYERS) i = 0;
				countADDPLAYER++;				
			}
			addLayer = addLayer + 1 - 2*addLayer;
			addCount++;
			countADDPLAYER = 0;
			//i = as.random.nextInt(as.NUM_PLAYERS); 
			i = init_i; //search the same set of agents each time
		}
		double[][] bestAddDrop = {{bestAddLayer, bestAddIndex, maxGain}, {bestDropLayer, bestDropIndex, maxGain}};
		return bestAddDrop; 
	}
	
	/**
	 * Considers each combination of adding and dropping ties, and returns the one with highest marginal utility. 
	 * Returns double[2][3]: [0=add, 1=drop][0=layer, 1=index, 2=utility gain]
	 */
	public double[][] bestSmartSearchAddDropCombo(AgentsSimulation as, int searchSize){		
		if(this.noTies(as) || this.fullyConnected(as)){//don't bother if agent no current edges or fully connected
			double[][] temp =  {{0.0, -1, 0}, {0.0, -1, 0}};
			return temp;
		}
		
		if(searchSize > as.NUM_PLAYERS)
			searchSize = as.NUM_PLAYERS;
		double currentUtil = as.currentUtility(this); //agent's current utility
		double maxGain = -998; 	
		int addLayer = as.random.nextInt(2); //to get next, i = i + 1 - 2*i (starts search at random layer)
		int dropLayer = as.random.nextInt(2);
		if(as.alwaysStartSearchAtLayer0 || as.oneLayerOnly){
			addLayer = 0; 
			dropLayer = 0; 
		}			
		int i = as.random.nextInt(as.NUM_PLAYERS); //start add search at random player
		int j = as.random.nextInt(as.NUM_PLAYERS); //start drop search at random player
		int bestAddIndex = i; 
		int init_i = i;
		int init_j = j;
		int bestAddLayer = addLayer; 
		int bestDropIndex = j; 
		int bestDropLayer = dropLayer; 
		int addCount = 0; 
		int dropCount = 0; 	
		int countADDPLAYER = 0; 
		int countDROPPLAYER = 0; 
		int max = 2;
		if(as.oneLayerOnly) max = 1;
		while(addCount < max){//start at random layer			
			while(countADDPLAYER < searchSize){ //as.NUM_PLAYERS){//loop over potential adds
				if(i != this.index && !isTie(as, as.agentList[i], addLayer)){ //if no tie exists 					
					Agent addAgent = as.agentList[i];					
					while(dropCount < max){ //loop over potential deletes
						while(countDROPPLAYER < searchSize){ //as.NUM_PLAYERS){
							if(j != this.index && j != i && isTie(as, as.agentList[j], dropLayer)){ //if tie exists
								Agent dropAgent = as.agentList[j];
								double newUtil = as.utilityIfAddDrop(this, addAgent, dropAgent, addLayer, dropLayer);
								double gain = newUtil - currentUtil;
								if(gain > maxGain){//if best move
									maxGain = gain;
									bestAddIndex = i; 
									bestAddLayer = addLayer; 
									bestDropIndex = j; 
									bestDropLayer = dropLayer; 
								}	
							}
							j = as.random.nextInt(as.NUM_PLAYERS);;
							//if(j >= as.NUM_PLAYERS) j = 0;
							countDROPPLAYER++;	
						}
						dropLayer = dropLayer + 1 - 2*dropLayer;
						dropCount++;	
						countDROPPLAYER = 0;
						//j = as.random.nextInt(as.NUM_PLAYERS);
						j = init_j; //search same set of nodes each time. 
					}
				}
				i = as.random.nextInt(as.NUM_PLAYERS);;
				//if(i >= as.NUM_PLAYERS) i = 0;
				countADDPLAYER++;				
			}
			addLayer = addLayer + 1 - 2*addLayer;
			addCount++;
			countADDPLAYER = 0;
			//i = as.random.nextInt(as.NUM_PLAYERS); 
			i = init_i; //search the same set of agents each time
		}
		double[][] bestAddDrop = {{bestAddLayer, bestAddIndex, maxGain}, {bestDropLayer, bestDropIndex, maxGain}};
		return bestAddDrop; 
	}
		
	/**
	 * Checks to see if adding a tie is beneficial to the other agent. 
	 * If so, adds the tie and returns true. 
	 * May also randomly accept tie with probability noise. 
	 * Otherwise, returns false. 
	 */
	public boolean addTie(AgentsSimulation as, double[] bestAdd){
		Agent b = as.agentList[(int)bestAdd[1]];
		int layer = (int)bestAdd[0];
		double otherCurrentUtil = as.currentUtility(b); // partner's current utility
		double otherNewUtil = as.utilityIfAdded(b, this, layer);
		if(otherNewUtil > otherCurrentUtil || as.random.nextBoolean(as.noise)){ //if increases utility or by chance
			as.tie_form(this, b, layer);
			return true;
		}
		else return false;
	}
	
	/**
	 * Checks to see if adding a tie is beneficial to the other agent. 
	 * If so, adds the tie and returns true. 
	 * May also randomly accept tie with probability noise.
	 * Otherwise, returns false. 
	 */
	public boolean addTie(AgentsSimulation as, Agent other, int layer){
		double otherCurrentUtil = as.currentUtility(other); // partner's current utility
		double otherNewUtil = as.utilityIfAdded(other, this, layer);
		if(otherNewUtil > otherCurrentUtil || as.random.nextBoolean(as.noise)){ //if increases utility or by chance
			as.tie_form(this, other, layer);
			return true;
		}
		else return false;
	}
	
	/**
	 * Drops the tie 
	 */
	public void dropTie(AgentsSimulation as, double[] bestDrop){
		Agent b = as.agentList[(int)bestDrop[1]];
		int layer = (int)bestDrop[0];
		as.tie_delete(this, b, layer);
	}
	
	/**
	 * Returns true if an edge exists, otherwise return false. 
	 */
	public boolean isTie(AgentsSimulation as, Agent other, int layer){
		return as.coplayerMatrix[layer][this.index][other.index];	
	}
	
	/**
	 * Returns true if an edge exists, otherwise return false. 
	 */
	public boolean isTie(AgentsSimulation as, int index, int layer){
		return as.coplayerMatrix[layer][this.index][index];	
	}
	
	
}//end class
