package wsc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import ec.vector.VectorIndividual;

public class SequenceVectorIndividual extends VectorIndividual {

	private static final long serialVersionUID = 1L;

	private double availability;
	private double reliability;
	private double time;
	private double cost;
	public Service[] genome;
	public Set<Service> usedServices;

	@Override
	public Parameter defaultBase() {
		return new Parameter("sequencevectorindividual");
	}

	@Override
	/**
	 * Initializes the individual.
	 */
	public void reset(EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;
		List<Service> relevantList = init.relevantList;
		Collections.shuffle(relevantList, init.random);

		genome = new Service[relevantList.size()];
		relevantList.toArray(genome);

		usedServices = new HashSet<Service>();
		this.evaluated = false;
	}

	@Override
	public boolean equals(Object ind) {
		boolean result = false;

		if (ind != null && ind instanceof SequenceVectorIndividual) {
			result = true;
			SequenceVectorIndividual other = (SequenceVectorIndividual) ind;

			for (int i = 0; i < genome.length; i++) {
				if (!genome[i].equals(other.genome[i])) {
					result = false;
					break;
				}

			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(genome);
	}

	@Override
	public String toString() {
		return Arrays.toString(genome);
	}

	@Override
	public SequenceVectorIndividual clone() {
    	SequenceVectorIndividual g = new SequenceVectorIndividual();
    	g.species = this.species;
    	g.usedServices = this.usedServices;
    	if (this.fitness == null)
    		g.fitness = (Fitness) g.species.f_prototype.clone();
    	else
    		g.fitness = (Fitness) this.fitness.clone();
    	if (genome != null) {
    		// Shallow cloning is fine in this approach
    		g.genome = genome.clone();
    	}
    	return g;
	}

	public String toGraphString(EvolutionState state) {
		WSCInitializer init = (WSCInitializer) state.initializer;
		Graph g = createNewGraph(init.numLayers, init.startServ, init.endServ, genome, init);
		return g.toString();
	}

	public Graph createNewGraph(int numLayers, Service start, Service end, Service[] sequence, WSCInitializer init) {
		Node endNode = new Node(end);
		Node startNode = new Node(start);

        Graph graph = new Graph();
        graph.nodeMap.put(endNode.getName(), endNode);

        // Populate inputs to satisfy with end node's inputs
        List<InputNodeLayerTrio> nextInputsToSatisfy = new ArrayList<InputNodeLayerTrio>();

        for (String input : end.getInputs()){
            nextInputsToSatisfy.add( new InputNodeLayerTrio(input, end.getName(), numLayers) );
        }

        // Fulfil inputs layer by layer
        for (int currLayer = numLayers; currLayer > 0; currLayer--) {

            // Filter out the inputs from this layer that need to fulfilled
            List<InputNodeLayerTrio> inputsToSatisfy = new ArrayList<InputNodeLayerTrio>();
            for (InputNodeLayerTrio p : nextInputsToSatisfy) {
               if (p.layer == currLayer)
                   inputsToSatisfy.add( p );
            }
            nextInputsToSatisfy.removeAll( inputsToSatisfy );

            int index = 0;
            while (!inputsToSatisfy.isEmpty()){

                if (index >= sequence.length) {
                    nextInputsToSatisfy.addAll( inputsToSatisfy );
                    inputsToSatisfy.clear();
                }
                else {
                	Service nextNode = sequence[index++];
                	if (nextNode.layer < currLayer) {
	                    Node n = new Node(nextNode);
	                    //int nLayer = nextNode.layerNum;

	                    List<InputNodeLayerTrio> satisfied = getInputsSatisfiedGraphBuilding(inputsToSatisfy, n, init);

	                    if (!satisfied.isEmpty()) {
	                        if (!graph.nodeMap.containsKey( n.getName() )) {
	                            graph.nodeMap.put(n.getName(), n);
	                        }

	                        // Add edges
	                        createEdges(n, satisfied, graph);
	                        inputsToSatisfy.removeAll(satisfied);


	                        for(String input : n.getInputs()) {
	                            nextInputsToSatisfy.add( new InputNodeLayerTrio(input, n.getName(), n.getLayer()) );
	                        }
	                    }
	                }
                }
            }
        }

        // Connect start node
        graph.nodeMap.put(startNode.getName(), startNode);
        createEdges(startNode, nextInputsToSatisfy, graph);

        return graph;
    }

	public void createEdges(Node origin, List<InputNodeLayerTrio> destinations, Graph graph) {
		// Order inputs by destination
		Map<String, Set<String>> intersectMap = new HashMap<String, Set<String>>();
		for(InputNodeLayerTrio t : destinations) {
			addToIntersectMap(t.service, t.input, intersectMap);
		}

		for (Entry<String,Set<String>> entry : intersectMap.entrySet()) {
			Edge e = new Edge(entry.getValue());
			origin.getOutgoingEdgeList().add(e);
			Node destination = graph.nodeMap.get(entry.getKey());
			destination.getIncomingEdgeList().add(e);
			e.setFromNode(origin);
        	e.setToNode(destination);
        	graph.edgeList.add(e);
		}
	}

	private void addToIntersectMap(String destination, String input, Map<String, Set<String>> intersectMap) {
		Set<String> intersect = intersectMap.get(destination);
		if (intersect == null) {
			intersect = new HashSet<String>();
			intersectMap.put(destination, intersect);
		}
		intersect.add(input);
	}

	public List<InputNodeLayerTrio> getInputsSatisfiedGraphBuilding(List<InputNodeLayerTrio> inputsToSatisfy, Node n, WSCInitializer init) {
	    List<InputNodeLayerTrio> satisfied = new ArrayList<InputNodeLayerTrio>();
	    for(InputNodeLayerTrio p : inputsToSatisfy) {
            if (init.taxonomyMap.get(p.input).servicesWithOutput.contains( n.getService() ))
                satisfied.add( p );
        }
	    return satisfied;
	}

	   public void calculateSequenceFitness(int numLayers, Service end, Service[] sequence, WSCInitializer init, EvolutionState state, boolean isOperation) {

	        Set<Service> solution = new HashSet<Service>();

	        cost = 0.0;
	        availability = 1.0;
	        reliability = 1.0;

	        // Populate inputs to satisfy with end node's inputs
	        List<InputTimeLayerTrio> nextInputsToSatisfy = new ArrayList<InputTimeLayerTrio>();
	        double t = end.getQos()[WSCInitializer.TIME];
	        for (String input : end.getInputs()){
	            nextInputsToSatisfy.add( new InputTimeLayerTrio(input, t, numLayers) );
	        }

	        // Fulfil inputs layer by layer
	        for (int currLayer = numLayers; currLayer > 0; currLayer--) {
	            // Filter out the inputs from this layer that need to fulfilled
	            List<InputTimeLayerTrio> inputsToSatisfy = new ArrayList<InputTimeLayerTrio>();
	            for (InputTimeLayerTrio p : nextInputsToSatisfy) {
	               if (p.layer == currLayer)
	                   inputsToSatisfy.add( p );
	            }
	            nextInputsToSatisfy.removeAll( inputsToSatisfy );

	            int index = 0;
	            while (!inputsToSatisfy.isEmpty()){
	                // If all nodes have been attempted, inputs must be fulfilled with start node
	                if (index >= sequence.length) {
	                    nextInputsToSatisfy.addAll(inputsToSatisfy);
	                    inputsToSatisfy.clear();
	                }
	                else {
	                Service nextNode = sequence[index++];
	                if (nextNode.layer < currLayer) {

	   	                List<InputTimeLayerTrio> satisfied = getInputsSatisfied(inputsToSatisfy, nextNode, init);
	   	                if (!satisfied.isEmpty()) {
	                           double[] qos = nextNode.getQos();
	                           if (!solution.contains( nextNode )) {
	                               solution.add(nextNode);
	                               cost += qos[WSCInitializer.COST];
	                               availability *= qos[WSCInitializer.AVAILABILITY];
	                               reliability *= qos[WSCInitializer.RELIABILITY];
	                           }
	                           t = qos[WSCInitializer.TIME];
	                           inputsToSatisfy.removeAll(satisfied);

	                           double highestT = findHighestTime(satisfied);

	                           for(String input : nextNode.getInputs()) {
	                               nextInputsToSatisfy.add( new InputTimeLayerTrio(input, highestT + t, nextNode.layer) );
	                           }
	                       }
		               }
	                }
	            }
	        }

	        // Find the highest overall time
	        time = findHighestTime(nextInputsToSatisfy);

	        // Save the current set of used services
	        usedServices = solution;

	        if (!WSCInitializer.dynamicNormalisation || isOperation)
	        	finishCalculatingSequenceFitness(init, state);
	    }

	   public void finishCalculatingSequenceFitness(WSCInitializer init, EvolutionState state) {
		   double[] objectives = calculateFitness(cost, time, availability, reliability, init);
			//init.trackFitnessPerEvaluations(f);

			((MultiObjectiveFitness) fitness).setObjectives(state, objectives);
			evaluated = true;
	   }

		public double findHighestTime(List<InputTimeLayerTrio> satisfied) {
		    double max = Double.MIN_VALUE;

		    for (InputTimeLayerTrio p : satisfied) {
		        if (p.time > max)
		            max = p.time;
		    }

		    return max;
		}

		public double[] calculateFitness(double c, double t, double a, double r, WSCInitializer init) {
	        a = normaliseAvailability(a, init);
	        r = normaliseReliability(r, init);
	        t = normaliseTime(t, init);
	        c = normaliseCost(c, init);

	        double[] objectives = new double[2];
	        //objectives[GraphInitializer.AVAILABILITY] = a;
	        //objectives[GraphInitializer.RELIABILITY] = r;
	        //objectives[WSCInitializer.TIME] = t;
	        //objectives[WSCInitializer.COST] = c;
	        objectives[0] = t + c;
	        objectives[1] = a + r;

	        return objectives;
		}

		private double normaliseAvailability(double availability, WSCInitializer init) {
			if (init.maxAvailability - init.minAvailability == 0.0)
				return 1.0;
			else
				//return (availability - init.minAvailability)/(init.maxAvailability - init.minAvailability);
				return (init.maxAvailability - availability)/(init.maxAvailability - init.minAvailability);
		}

		private double normaliseReliability(double reliability, WSCInitializer init) {
			if (init.maxReliability- init.minReliability == 0.0)
				return 1.0;
			else
				//return (reliability - init.minReliability)/(init.maxReliability - init.minReliability);
				return (init.maxReliability - reliability)/(init.maxReliability - init.minReliability);
		}

		private double normaliseTime(double time, WSCInitializer init) {
			if (init.maxTime - init.minTime == 0.0)
				return 1.0;
			else
				//return (init.maxTime - time)/(init.maxTime - init.minTime);
				return (time - init.minTime)/(init.maxTime - init.minTime);
		}

		private double normaliseCost(double cost, WSCInitializer init) {
			if (init.maxCost - init.minCost == 0.0)
				return 1.0;
			else
				//return (init.maxCost - cost)/(init.maxCost - init.minCost);
				return (cost - init.minCost)/(init.maxCost - init.minCost);
		}

		public List<InputTimeLayerTrio> getInputsSatisfied(List<InputTimeLayerTrio> inputsToSatisfy, Service n, WSCInitializer init) {
		    List<InputTimeLayerTrio> satisfied = new ArrayList<InputTimeLayerTrio>();
		    for(InputTimeLayerTrio p : inputsToSatisfy) {
	            if (init.taxonomyMap.get(p.input).servicesWithOutput.contains( n ))
	                satisfied.add( p );
	        }
		    return satisfied;
		}

		public void setAvailability(double availability) {
			this.availability = availability;
		}

		public void setReliability(double reliability) {
			this.reliability = reliability;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public void setCost(double cost) {
			this.cost = cost;
		}

		public double getAvailability() {
			return availability;
		}

		public double getReliability() {
			return reliability;
		}

		public double getTime() {
			return time;
		}

		public double getCost() {
			return cost;
		}

}
