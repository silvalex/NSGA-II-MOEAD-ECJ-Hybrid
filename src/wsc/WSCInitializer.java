package wsc;

import static java.lang.Math.abs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleInitializer;
import ec.util.Parameter;
import wsc.WSCRandom;

public class WSCInitializer extends SimpleInitializer {

	private static final long serialVersionUID = 1L;
	// Constants with of order of QoS attributes
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	public Map<String, Service> serviceMap = new HashMap<String, Service>();
	public Map<String, Integer> serviceToIndexMap = new HashMap<String, Integer>();
	public Set<Service> relevant;
	public List<Service> relevantList;
	public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	public Set<String> taskInput;
	public Set<String> taskOutput;
	public Service startServ;
	public Service endServ;
	public WSCRandom random;
	public int numLayers;

	public double minAvailability = 0.0;
	public double maxAvailability = -1.0;
	public double minReliability = 0.0;
	public double maxReliability = -1.0;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = -1.0;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = -1.0;
	public double w1;
	public double w2;
	public double w3;
	public double w4;
	public static boolean dynamicNormalisation;
	public static boolean tchebycheff;

	public static double[] meanAvailPerGen;
	public static double[] meanReliaPerGen;
	public static double[] meanTimePerGen;
	public static double[] meanCostPerGen;
	public static int availIdx = 0;
	public static int reliaIdx = 0;
	public static int timeIdx = 0;
	public static int costIdx = 0;

	public static ArrayList<Double> convergenceData = new ArrayList<Double>();
	public static File evaluationsLogFile;
	public int evalSampleRate;
	public static long setupTime;

	public double idealPoint[];
	public static int numObjectives;
	public static int numNeighbours;
	public static int popSize;
	public static int numLocalSearchTries;
	public double[][] weights;
	public int[][] neighbourhood;

	public Set<Individual> externalPopulation = new HashSet<Individual>();

	// Statistics tracking
	public int numEvaluations = 1;
	public double globalFitness = 0.0;

	public void trackFitnessPerEvaluations(double fitness) {
		if (fitness > globalFitness)
			globalFitness = fitness;
		if (numEvaluations % evalSampleRate == 0)
			convergenceData.add(globalFitness);
		numEvaluations++;
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		long startTime = System.currentTimeMillis();
		random = new WSCRandom(state.random[0]);
		super.setup(state,base);

		state.population = new Population();
		state.population.subpops = new Subpopulation[0];

		Parameter servicesParam = new Parameter("composition-services");
		Parameter taskParam = new Parameter("composition-task");
		Parameter taxonomyParam = new Parameter("composition-taxonomy");
		Parameter weight1Param = new Parameter("fitness-weight1");
		Parameter weight2Param = new Parameter("fitness-weight2");
		Parameter weight3Param = new Parameter("fitness-weight3");
		Parameter weight4Param = new Parameter("fitness-weight4");
		Parameter evaluationsLogNameParam = new Parameter("stat.evaluations");
		Parameter evalSampleRateParam = new Parameter("stat.eval-sample-rate");
		Parameter dynamicNormalisationParam = new Parameter("dynamic-normalisation");
		Parameter tchebycheffParam = new Parameter("tchebycheff");
		Parameter numObjectivesParam = new Parameter("multi.fitness.num-objectives");
		Parameter popSizeParam = new Parameter("pop.subpop.0.size");
		Parameter numNeighboursParam = new Parameter("numNeighbours");
		Parameter numLocalSearchTriesParam = new Parameter("numLocalSearchTries");

		w1 = state.parameters.getDouble(weight1Param, null);
		w2 = state.parameters.getDouble(weight2Param, null);
		w3 = state.parameters.getDouble(weight3Param, null);
		w4 = state.parameters.getDouble(weight4Param, null);
		dynamicNormalisation = state.parameters.getBoolean(dynamicNormalisationParam, null, false);
		tchebycheff = state.parameters.getBoolean(tchebycheffParam, null, false);
		numObjectives = state.parameters.getInt(numObjectivesParam, null);
		popSize = state.parameters.getInt(popSizeParam, null);
		numNeighbours = state.parameters.getInt(numNeighboursParam, null);
		numLocalSearchTries = state.parameters.getInt(numLocalSearchTriesParam, null);

		int numGens = state.parameters.getInt(new Parameter("generations"), null);
		meanAvailPerGen = new double[numGens];
		meanReliaPerGen = new double[numGens];
		meanTimePerGen = new double[numGens];
		meanCostPerGen = new double[numGens];

		parseWSCServiceFile(state.parameters.getString(servicesParam, null));
		parseWSCTaskFile(state.parameters.getString(taskParam, null));
		parseWSCTaxonomyFile(state.parameters.getString(taxonomyParam, null));
		evaluationsLogFile = state.parameters.getFile( evaluationsLogNameParam, null );
		evalSampleRate = state.parameters.getInt(evalSampleRateParam, null);
		findConceptsForInstances();

		double[] mockQos = new double[4];
		mockQos[TIME] = 0;
		mockQos[COST] = 0;
		mockQos[AVAILABILITY] = 1;
		mockQos[RELIABILITY] = 1;
		Set<String> startOutput = new HashSet<String>();
		startOutput.addAll(taskInput);
		startServ = new Service("start", mockQos, new HashSet<String>(), taskInput);
		endServ = new Service("end", mockQos, taskOutput ,new HashSet<String>());

		populateTaxonomyTree();
		relevant = getRelevantServices(serviceMap, taskInput, taskOutput);
		relevantList = new ArrayList<Service>(relevant);
		if (!dynamicNormalisation) {
			calculateNormalisationBounds(relevant);
			//calculateNormalisationBounds(new HashSet<Service>(serviceMap.values())); // XXX
		}

		// Set size of genome
		Parameter genomeSizeParam = new Parameter("pop.subpop.0.species.genome-size");
		state.parameters.set(genomeSizeParam, "" + relevant.size());
		setupTime = System.currentTimeMillis() - startTime;

		// Initialise the reference point
		if (tchebycheff)
			initIdealPoint();
		// Create a set of uniformly spread weight vectors
		weights = new double[popSize][numObjectives];
		initWeights();
		// Identify the neighbouring weights for each vector
		neighbourhood = new int[popSize][numNeighbours];
		identifyNeighbourWeights();
	}

	/**
	 * Initialises the ideal point used for the Tchebycheff calculation.
	 */
	private void initIdealPoint() {
		idealPoint = new double[numObjectives];
		for (int i = 0; i < numObjectives; i++) {
			idealPoint[i] = 0.0;
		}
	}

	/**
	 * Initialises the uniformely spread weight vectors. This code come from the authors'
	 * original code base.
	 */
	private void initWeights() {
		for (int i = 0; i < popSize; i++) {
			if (numObjectives == 2) {
				double[] weightVector = new double[2];
				weightVector[0] = i / (double) popSize;
				weightVector[1] = (popSize - i) / (double) popSize;
				weights[i] = weightVector;
			}
			else if (numObjectives == 3) {
				for (int j = 0; j < popSize; j++) {
					if (i + j < popSize) {
						int k = popSize - i - j;
						double[] weightVector = new double[3];
						weightVector[0] = i / (double) popSize;
						weightVector[1] = j / (double) popSize;
						weightVector[2] = k / (double) popSize;
						weights[i] = weightVector;
					}
				}
			}
			else {
				throw new RuntimeException("Unsupported number of objectives. Should be 2 or 3.");
			}
		}
	}

	/**
	 * Create a neighbourhood for each weight vector, based on the Euclidean distance between each two vectors.
	 */
	private void identifyNeighbourWeights() {
		// Calculate distance between vectors
		double[][] distanceMatrix = new double[popSize][popSize];

		for (int i = 0; i < popSize; i++) {
			for (int j = 0; j < popSize; j++) {
				if (i != j)
					distanceMatrix[i][j] = calculateDistance(weights[i], weights[j]);
			}
		}

		// Use this information to build the neighbourhood
		for (int i = 0; i < popSize; i++) {
			int[] neighbours = identifyNearestNeighbours(distanceMatrix[i], i);
			neighbourhood[i] = neighbours;
		}
	}

	/**
	 * Calculates the Euclidean distance between two weight vectors.
	 *
	 * @param vector1
	 * @param vector2
	 * @return distance
	 */
	private double calculateDistance(double[] vector1, double[] vector2) {
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += Math.pow((vector1[i] - vector2[i]), 2);
		}
		return Math.sqrt(sum);
	}

	/**
	 * Returns the indices for the nearest neighbours, according to their distance
	 * from the current vector.
	 *
	 * @param distances - a list of distances from the other vectors
	 * @param currentIndex - the index of the current vector
	 * @return indices of nearest neighbours
	 */
	private int[] identifyNearestNeighbours(double[] distances, int currentIndex) {
		Queue<IndexDistancePair> indexDistancePairs = new LinkedList<IndexDistancePair>();

		// Convert the vector of distances to a list of index-distance pairs.
		for(int i = 0; i < distances.length; i++) {
			indexDistancePairs.add(new IndexDistancePair(i, distances[i]));
		}
		// Sort the pairs according to the distance, from lowest to highest.
		Collections.sort((LinkedList<IndexDistancePair>) indexDistancePairs);

		// Get the indices for the required number of neighbours
		int[] neighbours = new int[numNeighbours];

		// Get the neighbours, including the vector itself
		IndexDistancePair neighbourCandidate;
		for (int i = 0; i < numNeighbours; i++) {
			neighbourCandidate = indexDistancePairs.poll();
 			// Uncomment this if you want to exclude the vector itself from being considered as part of the neighbourhood
//			while (neighbourCandidate.getIndex() == currentIndex)
//				neighbourCandidate = indexDistancePairs.poll();
			neighbours[i] = neighbourCandidate.getIndex();
		}
		return neighbours;
	}

	/**
	 * Checks whether set of inputs can be completely satisfied by the search
	 * set, making sure to check descendants of input concepts for the subsumption.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return true if search set subsumed by input set, false otherwise.
	 */
	public boolean isSubsumed(Set<String> inputs, Set<String> searchSet) {
		boolean satisfied = true;
		for (String input : inputs) {
			Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
			if (!isIntersection( searchSet, subsumed )) {
				satisfied = false;
				break;
			}
		}
		return satisfied;
	}

	/**
	 * Checks whether set of inputs can be completely satisfied by the search
	 * set, making sure to check descendants of input concepts for the subsumption.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return true if search set subsumed by input set, false otherwise.
	 */
	public Set<String> getInputsNotSubsumed(Set<String> inputs, Set<String> searchSet) {
		Set<String> notSatisfied = new HashSet<String>();
		for (String input : inputs) {
			Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
			if (!isIntersection( searchSet, subsumed )) {
				notSatisfied.add(input);
			}
		}
		return notSatisfied;
	}

    private static boolean isIntersection( Set<String> a, Set<String> b ) {
        for ( String v1 : a ) {
            if ( b.contains( v1 ) ) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Populates the taxonomy tree by associating services to the
	 * nodes in the tree.
	 */
	private void populateTaxonomyTree() {
		for (Service s: serviceMap.values()) {
			addServiceToTaxonomyTree(s);
		}
	}

	private void addServiceToTaxonomyTree(Service s) {
		// Populate outputs
	    Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
		for (String outputVal : s.getOutputs()) {
			TaxonomyNode n = taxonomyMap.get(outputVal);
			s.getTaxonomyOutputs().add(n);

			// Also add output to all parent nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
			queue.add( n );

			while (!queue.isEmpty()) {
			    TaxonomyNode current = queue.poll();
		        seenConceptsOutput.add( current );
		        current.servicesWithOutput.add(s);
		        for (TaxonomyNode parent : current.parents) {
		            if (!seenConceptsOutput.contains( parent )) {
		                queue.add(parent);
		                seenConceptsOutput.add(parent);
		            }
		        }
			}
		}
		// Populate inputs
		Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
		for (String inputVal : s.getInputs()) {
			TaxonomyNode n = taxonomyMap.get(inputVal);

			// Also add input to all children nodes
			Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
			queue.add( n );

			while(!queue.isEmpty()) {
				TaxonomyNode current = queue.poll();
				seenConceptsInput.add( current );

			    Set<String> inputs = current.servicesWithInput.get(s);
			    if (inputs == null) {
			    	inputs = new HashSet<String>();
			    	inputs.add(inputVal);
			    	current.servicesWithInput.put(s, inputs);
			    }
			    else {
			    	inputs.add(inputVal);
			    }

			    for (TaxonomyNode child : current.children) {
			        if (!seenConceptsInput.contains( child )) {
			            queue.add(child);
			            seenConceptsInput.add( child );
			        }
			    }
			}
		}
		return;
	}

	/**
	 * Converts input, output, and service instance values to their corresponding
	 * ontological parent.
	 */
	private void findConceptsForInstances() {
		Set<String> temp = new HashSet<String>();

		for (String s : taskInput)
			temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskInput.clear();
		taskInput.addAll(temp);

		temp.clear();
		for (String s : taskOutput)
				temp.add(taxonomyMap.get(s).parents.get(0).value);
		taskOutput.clear();
		taskOutput.addAll(temp);

		for (Service s : serviceMap.values()) {
			temp.clear();
			Set<String> inputs = s.getInputs();
			for (String i : inputs)
				temp.add(taxonomyMap.get(i).parents.get(0).value);
			inputs.clear();
			inputs.addAll(temp);

			temp.clear();
			Set<String> outputs = s.getOutputs();
			for (String o : outputs)
				temp.add(taxonomyMap.get(o).parents.get(0).value);
			outputs.clear();
			outputs.addAll(temp);
		}
	}

	/**
	 * Calculates the problem score using the Tchebycheff approach, and the given
	 * set of weights.
	 *
	 * @param ind
	 * @param problemIndex - for retrieving weights and ideal point
	 * @return score
	 */
	public double calculateTchebycheffScore(Individual ind, int problemIndex) {
		double[] problemWeights = weights[problemIndex];
		double max_fun = -1 * Double.MAX_VALUE;

		MultiObjectiveFitness fit = (MultiObjectiveFitness) ind.fitness;

		for (int i = 0; i < numObjectives; i++) {
			double diff = abs(fit.getObjectives()[i] - idealPoint[i]);
			double feval;
			if (problemWeights[i] == 0)
				feval = 0.00001 * diff;
			else
				feval = problemWeights[i] * diff;
			if (feval > max_fun)
				max_fun = feval;
		}
		return max_fun;
	}

	/**
	 * Calculates the problem score for a given individual, using a given
	 * set of weights.
	 *
	 * @param ind
	 * @param problemIndex - for retrieving weights
	 * @return score
	 */
	public double calculateScore(Individual ind, int problemIndex) {
		double[] problemWeights = weights[problemIndex];
		MultiObjectiveFitness fit = (MultiObjectiveFitness) ind.fitness;

		double sum = 0;
		for (int i = 0; i < numObjectives; i++)
			sum += (problemWeights[i]) * fit.getObjectives()[i];
		return sum;
	}

	/**
	 * Goes through the service list and retrieves only those services which
	 * could be part of the composition task requested by the user.
	 *
	 * @param serviceMap
	 * @return relevant services
	 */
	private Set<Service> getRelevantServices(Map<String,Service> serviceMap, Set<String> inputs, Set<String> outputs) {
		// Copy service map values to retain original
		Collection<Service> services = new ArrayList<Service>(serviceMap.values());

		Set<String> cSearch = new HashSet<String>(inputs);
		Set<Service> sSet = new HashSet<Service>();
		int layer = 0;
		Set<Service> sFound = discoverService(services, cSearch);
		while (!sFound.isEmpty()) {
			sSet.addAll(sFound);
			// Record the layer that the services belong to in each node
			for (Service s : sFound)
				s.layer = layer;

			layer++;
			services.removeAll(sFound);
			for (Service s: sFound) {
				cSearch.addAll(s.getOutputs());
			}
			sFound.clear();
			sFound = discoverService(services, cSearch);
		}

		numLayers = layer;

		if (isSubsumed(outputs, cSearch)) {
			return sSet;
		}
		else {
			String message = "It is impossible to perform a composition using the services and settings provided.";
			System.out.println(message);
			System.exit(0);
			return null;
		}
	}

	private void calculateNormalisationBounds(Set<Service> services) {
		for(Service service: services) {
			double[] qos = service.getQos();

			// Availability
			double availability = qos[AVAILABILITY];
			if (availability > maxAvailability)
				maxAvailability = availability;

			// Reliability
			double reliability = qos[RELIABILITY];
			if (reliability > maxReliability)
				maxReliability = reliability;

			// Time
			double time = qos[TIME];
			if (time > maxTime)
				maxTime = time;
			if (time < minTime)
				minTime = time;

			// Cost
			double cost = qos[COST];
			if (cost > maxCost)
				maxCost = cost;
			if (cost < minCost)
				minCost = cost;
		}
		// Adjust max. cost and max. time based on the number of services in shrunk repository
		maxCost *= services.size();
		maxTime *= services.size();

	}

	/**
	 * Discovers all services from the provided collection whose
	 * input can be satisfied either (a) by the input provided in
	 * searchSet or (b) by the output of services whose input is
	 * satisfied by searchSet (or a combination of (a) and (b)).
	 *
	 * @param services
	 * @param searchSet
	 * @return set of discovered services
	 */
	private Set<Service> discoverService(Collection<Service> services, Set<String> searchSet) {
		Set<Service> found = new HashSet<Service>();
		for (Service s: services) {
			if (isSubsumed(s.getInputs(), searchSet))
				found.add(s);
		}
		return found;
	}

	/**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	private void parseWSCServiceFile(String fileName) {
        Set<String> inputs = new HashSet<String>();
        Set<String> outputs = new HashSet<String>();
        double[] qos = new double[4];

        try {
        	File fXmlFile = new File(fileName);
        	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        	Document doc = dBuilder.parse(fXmlFile);

        	NodeList nList = doc.getElementsByTagName("service");

        	for (int i = 0; i < nList.getLength(); i++) {
        		org.w3c.dom.Node nNode = nList.item(i);
        		Element eElement = (Element) nNode;

        		String name = eElement.getAttribute("name");
    		    qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
    		    qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
    		    qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
    		    qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));

				// Get inputs
				org.w3c.dom.Node inputNode = eElement.getElementsByTagName("inputs").item(0);
				NodeList inputNodes = ((Element)inputNode).getElementsByTagName("instance");
				for (int j = 0; j < inputNodes.getLength(); j++) {
					org.w3c.dom.Node in = inputNodes.item(j);
					Element e = (Element) in;
					inputs.add(e.getAttribute("name"));
				}

				// Get outputs
				org.w3c.dom.Node outputNode = eElement.getElementsByTagName("outputs").item(0);
				NodeList outputNodes = ((Element)outputNode).getElementsByTagName("instance");
				for (int j = 0; j < outputNodes.getLength(); j++) {
					org.w3c.dom.Node out = outputNodes.item(j);
					Element e = (Element) out;
					outputs.add(e.getAttribute("name"));
				}

                Service ws = new Service(name, qos, inputs, outputs);
                serviceMap.put(name, ws);
                inputs = new HashSet<String>();
                outputs = new HashSet<String>();
                qos = new double[4];
        	}
        }
        catch(IOException ioe) {
            System.out.println("Service file parsing failed...");
        }
        catch (ParserConfigurationException e) {
            System.out.println("Service file parsing failed...");
		}
        catch (SAXException e) {
            System.out.println("Service file parsing failed...");
		}
    }

	/**
	 * Parses the WSC task file with the given name, extracting input and
	 * output values to be used as the composition task.
	 *
	 * @param fileName
	 */
	private void parseWSCTaskFile(String fileName) {
		try {
	    	File fXmlFile = new File(fileName);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);

	    	org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
	    	NodeList providedList = ((Element) provided).getElementsByTagName("instance");
	    	taskInput = new HashSet<String>();
	    	for (int i = 0; i < providedList.getLength(); i++) {
				org.w3c.dom.Node item = providedList.item(i);
				Element e = (Element) item;
				taskInput.add(e.getAttribute("name"));
	    	}

	    	org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
	    	NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
	    	taskOutput = new HashSet<String>();
	    	for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				taskOutput.add(e.getAttribute("name"));
	    	}
		}
		catch (ParserConfigurationException e) {
            System.out.println("Task file parsing failed...");
            e.printStackTrace();
		}
		catch (SAXException e) {
            System.out.println("Task file parsing failed...");
            e.printStackTrace();
		}
		catch (IOException e) {
            System.out.println("Task file parsing failed...");
            e.printStackTrace();
		}
	}

	/**
	 * Parses the WSC taxonomy file with the given name, building a
	 * tree-like structure.
	 *
	 * @param fileName
	 */
	private void parseWSCTaxonomyFile(String fileName) {
		try {
	    	File fXmlFile = new File(fileName);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);
	    	NodeList taxonomyRoots = doc.getChildNodes();

	    	processTaxonomyChildren(null, taxonomyRoots);
		}

		catch (ParserConfigurationException e) {
            System.err.println("Taxonomy file parsing failed...");
		}
		catch (SAXException e) {
            System.err.println("Taxonomy file parsing failed...");
		}
		catch (IOException e) {
            System.err.println("Taxonomy file parsing failed...");
		}
	}

	/**
	 * Recursive function for recreating taxonomy structure from file.
	 *
	 * @param parent - Nodes' parent
	 * @param nodes
	 */
	private void processTaxonomyChildren(TaxonomyNode parent, NodeList nodes) {
		if (nodes != null && nodes.getLength() != 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node ch = nodes.item(i);

				if (!(ch instanceof Text)) {
					Element currNode = (Element) nodes.item(i);
					String value = currNode.getAttribute("name");
					TaxonomyNode taxNode = taxonomyMap.get( value );
					if (taxNode == null) {
					    taxNode = new TaxonomyNode(value);
					    taxonomyMap.put( value, taxNode );
					}
					if (parent != null) {
					    taxNode.parents.add(parent);
						parent.children.add(taxNode);
					}

					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children);
				}
			}
		}
	}
}
