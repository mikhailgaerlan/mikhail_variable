package agents;

import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Int2D;

public class Data {

	public String[] fileheaders = null;	
	public Bag headerBag = null;
	public Bag dataBag = null; 
	public Bag dataSumBag = null;
	
	public Data(int numPlayers, long lengthOfSimulations){
		headerBag = makeHeaderBag(numPlayers,lengthOfSimulations);
	}
	
	/**
	 * Returns a Bag with all the values desired by the user for a data file.
	 * This method will be called for each hypothesis
	 */
	public  Bag getData(final SimState state, int runCount, int batchRun){
		final AgentsSimulation as = (AgentsSimulation)state;  // gets the state of the simulation
		dataBag = new Bag(); 
		
		//First, add the things the state-level parameters
		dataBag.add((double)(batchRun)); 
		dataBag.add((double)(runCount)); 
		//dataBag.add((double)((AgentsSimulation)state).schedule.getSteps());//Time of the data collection	
		//model parameters
		dataBag.add((double)as.NUM_PLAYERS);
		dataBag.add(as.noise);
		//dataBag.add(as.tie_benefit_1);
		//dataBag.add(as.tie_benefit_2);
		dataBag.add(as.tie_cost_1);
		//dataBag.add(as.tie_cost_2);
		dataBag.add(as.triangle_payoff_1);
		//dataBag.add(as.triangle_payoff_2);
		dataBag.add(as.spillover_payoff);
		double startlay0 = (as.shockOneLayer) ? 1 : 0;
		dataBag.add(startlay0);
		//dataBag.add((double)as.searchSize);
		//dataBag.add((double)as.timeOfShock);
		dataBag.add((double)as.numAgentsShocked);
		dataBag.add((double)as.numAgentsOverlapped);
		dataBag.add(as.postShockTieCost_1);
		//dataBag.add(as.postShockTieCost_2);
		//dataBag.add((double)as.equilTime1);
		//dataBag.add((double)as.equilTime2);
		//add avg degree for each layer
		//double[] deg = as.averageDegree();
		//dataBag.add(deg[0]);
		//dataBag.add(deg[1]);
		//add avg clustering for each layer
		//dataBag.add(as.averageClustering(0));
		//dataBag.add(as.averageClustering(1));
		//dataBag.add(as.localClustering(0));
		//dataBag.add(as.localClustering(1));
		/*
		//edge connections (entries for each layer, for each possible edge)
		for(int lay = 0; lay < 2; lay++)
			for(int i = 0; i < as.NUM_PLAYERS - 1; i++)
				for(int j = i+1; j < as.NUM_PLAYERS; j++){
					double x = as.coplayerMatrix[lay][i][j] ? 1 : 0;
					dataBag.add(x);
				}
				*/
	/*	//tie-costs in layer 1
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			Agent a = as.agentList[i];
			dataBag.add(a.tie_cost_1);
		}
		//tie-costs in layer 2
		for(int i = 0; i < as.NUM_PLAYERS; i++){
			Agent a = as.agentList[i];
			dataBag.add(a.tie_cost_2);
		}
		*/
		
		return dataBag;
	}
	
	
	/**
     * These are the column headers for the data file
     */
	public Bag makeHeaderBag(int numPlayers, long lengthOfSimulations){
    	Bag headerBag = new Bag();
    	//run parameters
    	headerBag.add("batch");
    	headerBag.add("run");
    	//headerBag.add("time"); //time the model was run to -- end result only
    	//model parameters
    	headerBag.add("numPlayers");
    	headerBag.add("noise");
    	//headerBag.add("tie_benefit_1");
    	//headerBag.add("tie_benefit_2");
    	headerBag.add("pre_tiecost");//headerBag.add("tie_cost_1");
    	//headerBag.add("tie_cost_2");
    	headerBag.add("triangle_payoff");//headerBag.add("triangle_payoff_1");
    	//headerBag.add("triangle_payoff_2");
    	headerBag.add("spillover_payoff");
    	headerBag.add("oneLayerOnly");
    	//headerBag.add("searchSize");
    	//headerBag.add("timeOfShock");
    	headerBag.add("num_shocked");//headerBag.add("numShocked");
    	headerBag.add("num_overlapped");
    	headerBag.add("postShock_tiecost");//headerBag.add("postShock_tiecost_1");
    	//headerBag.add("postShock_tiecost_2"); 	
    	//headerBag.add("EquilTime_preShock");
    	//headerBag.add("EquilTime_postShock");
    	//headerBag.add("AvgDegree_1");
    	//headerBag.add("AvgDegree_2");
    	//headerBag.add("AvgClustering_1");
    	//headerBag.add("AvgClustering_2");
    	//which nodes shocked
    	//headerBag.add("node_i_tie_cost_1");
    	//headerBag.add("node_i_tie_cost_2");
    	//headerBag.add("LocalClustering_1");
    	//headerBag.add("LocalClustering_2");
    	//headerBag.add("node_i_num_links_added");
    	//headerBag.add("node_i_num_links_dropped");
    	//headerBag.add("node_i_utility_before_turn");
    	//Adjacency matrix
    	headerBag.add("step");
    	headerBag.add("Shockedi");
    	headerBag.add("Overlappedi");
    	headerBag.add("layer");
    	headerBag.add("i");
    	headerBag.add("j");
    	headerBag.add("edges");
    	headerBag.add("Recent_Utility");
    	headerBag.add("Cumulative_Utility");
    	//for(int i = 1; i < lengthOfSimulations+1; i++) {
    	//	headerBag.add("t"+Integer.toString(i)+"_Recent_Utility");
    	//	headerBag.add("t"+Integer.toString(i)+"_Cumulative_Utility");
    	//	headerBag.add("t"+Integer.toString(i)+"_edges");
    	//}
    	//headerBag.add("layer");
    	//headerBag.add("edge");
    	//headerBag.add("recentUtility");
    	//headerBag.add("cumulativeUtility");
    	/*
    	//Add headers of the form "EDGE0_0-1," indicating an edge in layer 0 between nodes 0 and 1. 
    	String[][] edgeStrings = edgeHeaders(numPlayers);
    	for(int i = 0; i < 2; i++)
    		for(int j = 0; j < edgeStrings[0].length; j++){
    			headerBag.add(edgeStrings[i][j]);
    		}
    	*/
    	
    	return headerBag;
    }
	
	
	public String[][] edgeHeaders(int numPlayers){
		int size = comboCount(numPlayers);
		Int2D[] ei = edgeIndices(numPlayers, size);
		String[][] edgeStrings = new String[2][size];
		String base0 = "EDGE0_X-Y";
		String base1 = "EDGE1_X-Y";
		for(int i = 0; i < size; i++){
			String num1 = "" + ei[i].x;
			String num2 = "" + ei[i].y;
			String temp0 = base0.replace("X", num1);
			edgeStrings[0][i] = temp0.replace("Y", num2);
			String temp1 = base1.replace("X", num1);
			edgeStrings[1][i] = temp1.replace("Y", num2);
		}
		return edgeStrings;
	}
	
	/*
	 * Return all the indices for the edge matrix
	 * FOr 6-nodes: 01, 02, 03, 04, 05, 12, 13, 14, 15, 23, 24, 25, 34, 35, 45
	 */
	public Int2D[] edgeIndices(int numPlayers, int comboCount){
		Int2D[] edgeindices = new Int2D[comboCount];
		
		int max = numPlayers - 1;
		int x = 1;
		int index = 0; 
		for(int i = 0; i < numPlayers; i++){
			for(int j = x; j <= max; j++){
				edgeindices[index] = new Int2D(i, j);
				index++;
			}
			x++;
		}
		return edgeindices;
	}
	
	
	/*
	 * Returns the number of possible edges in a network of size N
	 * This equals (N-1)+(N-2)+...+1
	 */
	public int comboCount(int numPlayers){
		int total = 0;
		for(int i = 1; i < numPlayers; i++)
			total += i;
		return total;
	}
	
	
	
	
}//end class
