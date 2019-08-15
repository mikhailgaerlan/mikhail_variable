# MultiplexShockCode
This is code to analyze how multiplex networks evolve after shocks.
Please cite this code as: 
Keith Burghardt and Zeev Maoz. Partial Shocks on Cooperative Multiplex Networks with Varying Degrees of Noise.
[in preparation] (2018).

We want to especially thank Paul Smaldino for creating the initial code we base our simulations on. Code is from paper:
Smaldino PE, D’Souza R, Maoz Z Resilience by structural entrenchment: Dynamics of single-layer and multiplex networks following sudden changes to tie costs. Network Science: 1-19. doi:10.1017/nws.2017.35 (2017).

Our paper: Keith Burghardt and Zeev Maoz. Partial Shocks on Cooperative Multiplex Networks with Varying Degrees of Noise. [In Submission] (2018)

Link to Paul Smaldino's code: https://www.comses.net/codebases/5148/releases/1.0.0/

Agents: This code is based on Paul Smaldino's code to simulate how multiplex networks evolve. More details are discussed in the README file in the Agents folder.

Analysis: This code analyzes the output of our simulations, and creates cleaned and more compact files we need to parse the data.

Outline of our code:

AgentsSimulation.java: this is where we can vary simulation parameters, make a filename for the output, and list how many timesteps we record in the output file
Data.java: this is where we vary what data is recorded
GamePlayerAgent.java: this is where we code how agents behave when it is there turn to add/drop links
Agent.java: this is the meat of the code, where we code how agents add, drop, and rewire links, as well as accept links offered to them.
GraphicalUI.java: this is where we can see how the network updates in real time. This is legacy code and is currently defunct (but can work with slight modifications we hope to do in the future).
What agents do: Each agent is on a 2-layer multiplex network. They can connect to up to N other agents, but when offering ties, search among m=10 agents. If run as-is the m agents are picked at random, but we also allow for agents to be chosen in a "smarter" fashion and see the same qualitative results.

If it is not their turn and a tie is offered to them:

Probability p: accept link
Probability 1-p: accept link only if it increases their utility
If it is their turn:

Probability p: an agent offers a link to a random node in a random layer, and deletes a link
Probability 1-p: Agents can offer, drop, or offer-and-drop (rewire) links N times as long as each choice increases their utility AND is the choice that maximizes their utility the most in the next round. This simulates agents offering to create an "organization", in which multiple agents connect to one another.
Utility function: This utility function is a minimal model to simulate several properties seen in cooperative networks. Utility for each agent, i, is: u_i = ev_i + ∑_l (bt_{i,l} - c_it_{i,l}^2 + dz_{i,l})

e, b, c_i, and d are parameters (b=1 for now)

v_i are the number of edges that are the same across layers ("spillover" edges) for node i. These edges are seen more often than chance in real systems.
t_{i,l} are the number of edges from node i in each layer (edges are undirected). There is a benefit (bt_{i,l}) and associated cost (c_it_{i,l}^2) for each tie
z_{i,l} are the number of triangles made by node i. This is used to create clustering seen in cooperative (as well as many other) networks
Agents can either maximize u_i at each timestep or rewire, which allows agents to "explore". This is intuitively similar to the epsilon-greedy algorithm in Multi-Armed Bandit theory, or alike to temperature in simulated annealing. In either case, this randomness allows agents to explore the system more than they otherwise would, which in a rough utility landscape, allows them to find unexpected ways their utility can be maximized.
