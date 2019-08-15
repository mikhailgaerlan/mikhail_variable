import csv;
import os;
import codecs;
import ast;
from contextlib import contextmanager;
import sys, os;
import time;
import numpy as np;

@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:  
            yield
        finally:
            sys.stdout = old_stdout


#This is the directory for the simulation data you want to parse
directory="";

# file(s) to read from
read_file_1 = "";

#simulation parameters
# Number of nodes
NumNodes = 40;

# Number of timesteps recorded:
# this code assumes two timesteps are recorded: pre- and post-shock
end_time = 2;

# Do you want to be verbose?
verbose = 0;



flatten = lambda list_of_lists: [item for sublist in list_of_lists for item in sublist]
roundlist = lambda list_to_round: [round(float(item),5) if item != '' else '' for item in list_to_round ]
heaviside = lambda x: 0.5 if x == 0 else 0 if x < 0 else 1;
heaviside_list = lambda l: [heaviside(x) for x in l];
pos1_list = lambda l: np.array([heaviside(x-0.0001) for x in l]);


for read_file in [read_file_1]:
    
    NumBatches=0;
    if os.path.isfile(directory+read_file):
        print(read_file);
    else:
        print("ERROR: File not found: %s" %read_file);
        continue;
    r = open(directory+read_file,'r');
    # find number of lines in file
    end_line = len([line for line in r])-1;
    r.close();
    r = open(directory+read_file,'r');
    w = open(directory+read_file[:-4]+'_NetworkStats.csv','w');

    read_csv_file=csv.reader(r, delimiter=',');
    
    header=read_csv_file.next();
    write_csv_file=csv.writer(w);
    # header: 
    # noise
    # precost
    # [d]*NumNodes
    # [e]*NumNodes
    # preUtility
    # preDegree
    # preFracSpillover
    # postUtility
    # postDegree
    # postFracSpillover
    # shocked
    # fractneighborsshocked
    
    write_csv_file.writerow(["Noise","Pre-Cost","triangle_benefit","spillover-benefit","pre-shock_utility","pre-shock_degree","pre-shock_frac_spillover","pre-shock_clustering","post-shock_utility","post-shock_degree","post-shock_frac_spillover","post-shock_clustering","Shocked?","Fraction_neighbors_shocked","num_nodes_shocked"]);
    w.flush();
    os.fsync(w);

    first_line = True;
    sim = [];
    stats = [];
    counter = 0;
    for row in read_csv_file:
        counter = counter + 1;
        if row[0]!= "" or counter == end_line+1:
            if not first_line:
                #noise,precost,d,e
                sim = np.array(sim).astype(np.float);
                # 1 if shocked, else 0
                shocked = sim[:NumNodes**2:NumNodes,0];

                #num nodes shocked
                num_shocked = np.sum(shocked);
                
                preUtility = sim[:NumNodes**2:NumNodes,2];
                premat1 = np.reshape(sim[:NumNodes**2,1],(NumNodes,NumNodes));
                premat2 = np.reshape(sim[NumNodes**2:2*NumNodes**2,1],(NumNodes,NumNodes));
                
                predeg1 = np.sum(premat1,axis=0);
                predeg2 = np.sum(premat2,axis=0);
                preDegree = np.add(predeg1,predeg2);
                preSpilloverMat = np.multiply(premat1,premat2);# 0 if no edge, 1 if edge
                preSpillover = np.sum(preSpilloverMat,axis=0); # total number of spillover edge pairs

                # number of spillover pairs divided by (number of ties/2)
                # value is 0 if no spillover/spillover undefined, 1 if all spillover
                preFracSpillover = np.divide(2*preSpillover,preDegree);
                preFracSpillover = np.array([s if k > 0 else 0 for s,k in zip(preFracSpillover,preDegree)]);
                
                # number of 3-cycles
                pre_num_3cycles_1 = (premat1.dot(premat1.dot(premat1))).diagonal()/2;#divide by 2 so we do not overcount
                pre_num_3cycles_2 = (premat2.dot(premat2.dot(premat2))).diagonal()/2;#divide by 2 so we do not overcount

                # number of triangles possible: k_i (k_i-1)/2
                pre_num_tri_possible_1 = np.multiply(predeg1,predeg1-1)/2;
                # this avoids nans for clustering coefficient
                pre_num_tri_possible_1 = np.array([p if p>0 else 1 for p in pre_num_tri_possible_1]);

                pre_num_tri_possible_2 = np.multiply(predeg2,predeg2-1)/2;
                # this avoids nans for clustering coefficient
                pre_num_tri_possible_2 = np.array([p if p>0 else 1 for p in pre_num_tri_possible_2]);
                
                # pre-shock local clustering coefficient:
                # by default, is 0 if degree is 1 or 0
                pre_local_cluster1 = np.divide(pre_num_3cycles_1,pre_num_tri_possible_1);
                pre_local_cluster2 = np.divide(pre_num_3cycles_2,pre_num_tri_possible_2);
                pre_local_cluster = (pre_local_cluster1+pre_local_cluster2)/2;

                postUtility = sim[:NumNodes**2:NumNodes,5];
                postmat1 = np.reshape(sim[:NumNodes**2,4],(NumNodes,NumNodes));
                postmat2 = np.reshape(sim[NumNodes**2:2*NumNodes**2,4],(NumNodes,NumNodes));
                postdeg1 = np.sum(postmat1,axis=0);
                postdeg2 = np.sum(postmat2,axis=0);
                postDegree = np.add(postdeg1,postdeg2);
                postSpilloverMat = np.multiply(postmat1,postmat2);
                postSpillover = np.sum(postSpilloverMat,axis=0);
                postFracSpillover = np.divide(2*postSpillover,postDegree);
                postFracSpillover = np.array([s if k > 0 else 0 for s,k in zip(postFracSpillover,postDegree)]);
                # number of 3-cycles
                post_num_3cycles_1 = (postmat1.dot(postmat1.dot(postmat1))).diagonal()/2;#divide by 2 so we do not overcount
                post_num_3cycles_2 = (postmat2.dot(postmat2.dot(postmat2))).diagonal()/2;#divide by 2 so we do not overcount

                # number of triangles possible: k_i (k_i-1)/2
                post_num_tri_possible_1 = np.multiply(postdeg1,postdeg1-1)/2;
                # this avoids nans for clustering coefficient
                post_num_tri_possible_1 = np.array([p if p>0 else 1 for p in post_num_tri_possible_1]);
                
                post_num_tri_possible_2 = np.multiply(postdeg2,postdeg2-1)/2;
                # this avoids nans for clustering coefficient
                post_num_tri_possible_2 = np.array([p if p>0 else 1 for p in post_num_tri_possible_2]);

                # post-shock local clustering coefficient:
                # by default, is 0 if degree is 1 or 0
                post_local_cluster1 = np.divide(post_num_3cycles_1,post_num_tri_possible_1);
                post_local_cluster2 = np.divide(post_num_3cycles_2,post_num_tri_possible_2);
                post_local_cluster = (post_local_cluster1+post_local_cluster2)/2;

                    
                shockedMat = np.array([shocked]*NumNodes);
                fullpremat = np.add(premat1,premat2); # values = 0 if no edges, 1 is edge in 1 layer, 2 if edges in both layers
                # this says whether neighbor AND shocked
                neighborsshocked = [np.multiply(pos1_list(l),shocked) for l in fullpremat];

                # number of shocked neighbors/number of neighbors
                num_neighbors = np.array([np.sum(pos1_list(pre)) for pre in fullpremat])
                #print(num_neighbors)
                fractneighborsshocked = [np.divide(np.sum(nshock),n) if n > 0 else 0 for nshock,n in zip(neighborsshocked,num_neighbors)]
                
                # record:
                # [noise]*NumNodes
                # [precost]*NumNodes
                # [d]*NumNodes
                # [e]*NumNodes
                # preUtility
                # preDegree
                # preFracSpillover
                # preClustering
                # postUtility
                # postDegree
                # postFracSpillover
                # postClustering
                # shocked
                # fractneighborsshocked
                write_mat = np.transpose(np.array([[noise]*NumNodes,[precost]*NumNodes,[d]*NumNodes,[e]*NumNodes,preUtility,preDegree,preFracSpillover,pre_local_cluster,postUtility,postDegree,postFracSpillover,post_local_cluster,shocked,fractneighborsshocked,[num_shocked]*NumNodes]));
                for write_line in write_mat:
                    write_csv_file.writerow([str(l) for l in write_line]);
                    w.flush();
                    os.fsync(w);
                
            else:
                first_line = False;
            sim = [];
            noise,precost,d,e = row[3:7];
            
            sim.append([row[10]]+[row[14]]+row[17:20]+row[22:])

        else:            
            sim.append([row[10]]+[row[14]]+row[17:20]+row[22:])
            

        

    
