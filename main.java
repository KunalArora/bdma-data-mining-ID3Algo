import java.io.*;
import java.util.*;

public class main {
	
	private static String[] attributeList = null;
	private static HashSet<String> classAttributeValues = new HashSet<String>();
	private static int classAttributeIndex = -1;

	public static void main(String [] arg) throws IOException{
		BufferedReader input = null;

		// Read input file and run algorithm to create a decision tree
		try {
			input = new BufferedReader(new FileReader(arg[0]));
			String firstLine[] = input.readLine().split(",");
			attributeList = new String[firstLine.length]; 
			for(int i =0, j = 0; i < firstLine.length; i++) {
				attributeList[i] = String.valueOf(j++);
			}
			
			String[] attributeListExceptClassAttr = new String[attributeList.length - 1];
			for(int i = 0, j = 0; i<attributeList.length; i++) {
				if(i == Integer.parseInt(arg[1])) {
					classAttributeIndex = i;
				} else {
					attributeListExceptClassAttr[j++] = attributeList[i];
				}
			}
			
			ArrayList<String[]> data = new ArrayList<String[]>();
			String lineInput = null;
			while((lineInput = input.readLine()) != null) {
				String[] line = lineInput.split(",");
				data.add(line);
				classAttributeValues.add(line[classAttributeIndex]);
			}
			ID3_algo algo = new ID3_algo();
			DecisionTree tree = new DecisionTree();
			
			tree.root = algo.runID3algorithm(attributeList, classAttributeValues,
					classAttributeIndex, attributeListExceptClassAttr, data);
			tree.attributes = attributeList;
			tree.print();		
			
		}
		finally {
			if (input!=null) {
				input.close();
			}
		}		
	}
}

class Node {
	public String name;
	public int attribute;
	public Node[] nodes;
	public String[] attributeValues;
	boolean classNode;
}

class DecisionTree {
	String[] attributes;
	Node root = null;
		
	LinkedList<String> path = new LinkedList<String>();
	int leavesNodeCount = 0;
		
	public void print() {
		System.out.println("All possible combinations are as follows:\n");
		print(root, "");
		System.out.println("\nNumber of leaf to root paths: " + leavesNodeCount);
	}		
		
	private void print(Node nodeToPrint, String value) {
		if(nodeToPrint == null) {
			return;
		}
		if(value!="") {
			path.add(value);			
		}
		
		if(nodeToPrint.classNode){
			path.add(nodeToPrint.name);
			printArray(path, value);
		}else{
			path.add((attributes[nodeToPrint.attribute]));

			for(int i=0; i< nodeToPrint.nodes.length; i++){
				print(nodeToPrint.nodes[i], nodeToPrint.attributeValues[i]);
			}
			popValues(path, value);
		}
	}	
		
	private void printArray(LinkedList<String>path, String popUntilVal) {
		leavesNodeCount++;
		Iterator<String> itr = path.descendingIterator();
		System.out.print(itr.next());
		System.out.print(" --> ");
		while(itr.hasNext()) {
			System.out.print("(Split on) " + itr.next() + " --> " + 
					"(Attribute index) " + itr.next());
			if(itr.hasNext()) {				
				System.out.print(" --> ");
			}
		}
		popValues(path, popUntilVal);
		System.out.println();
	}
		
	private void popValues(LinkedList<String>path, String val) {
		while(path.getLast()!=val) {
			if(val == ""){
				break;
			} else {
				path.pollLast();					
			}
		}
		if(path.getLast()!=null && path.getLast() == val) {
			path.pollLast();							
		}
	}
}

class ID3_algo {
	public Node runID3algorithm(String[] attributeList, 
			HashSet<String> classAttributeValues, 
			int classAttributeIndex,
			String[] attributeListExceptClassAttr, 
			ArrayList<String[]> data) 
	{
		if(attributeListExceptClassAttr.length == 0) {
			HashMap<String, Integer> classAttributeFreq = FreqOfValuesInAttribute(
					data, classAttributeIndex);
			int highestCount = 0;
			String highestName = "";
			for(HashMap.Entry<String, Integer> entry : classAttributeFreq.entrySet()) {
				if(entry.getValue() > highestCount) {
					highestCount = entry.getValue();
					highestName = entry.getKey();
				}
			}
			Node node = new Node();
			node.name = highestName;
			node.classNode = true;
			return node;
		}
		
		HashMap<String, Integer> classAttributeFreq = FreqOfValuesInAttribute(
				data, classAttributeIndex);
		
		if(classAttributeFreq.entrySet().size() == 1) {
			Node node = new Node();
			node.name = (String) classAttributeFreq.keySet().toArray()[0];
			node.classNode = true;
			return node;
		}
		
		double classAttributeEntropy = 0d;
		for(String value : classAttributeValues) {
			Integer freq = classAttributeFreq.get(value);
			if(freq != null) {
				double freqDouble = freq/(double) data.size(); 
				classAttributeEntropy -= (freqDouble) * Math.log(freqDouble)/Math.log(2);
			}
		}
		
		int highestGainAttribute = 0;
		double highestGain = -1;
		for(String attribute : attributeListExceptClassAttr) {
			int attributeIndex = Arrays.asList(attributeList).indexOf(attribute);
			HashMap<String, Integer> attributeValuesFreq = FreqOfValuesInAttribute(
					data, attributeIndex);
			double sum = 0;
			for(HashMap.Entry<String, Integer> entry : attributeValuesFreq.entrySet()) {
				sum += entry.getValue()/((double) data.size()) *
						calWeightedEntropy(classAttributeValues, classAttributeIndex, 
								data, attributeIndex, entry.getKey());
			}
			double gain =  classAttributeEntropy - sum;
			if(gain >= highestGain) {
				highestGain = gain;
				highestGainAttribute = attributeIndex;
			}
		}
		
		if(highestGain == 0) {
			Node node = new Node();
			int topFrequency = 0;
			String className = null;
			for(HashMap.Entry<String, Integer> entry : classAttributeFreq.entrySet()) {
				if(entry.getValue() > topFrequency) {
					topFrequency = entry.getValue();
					className = entry.getKey();
				}
			}
			node.name = className;
			node.classNode = true;
			return node;
		}
		
		Node node = new Node();
		node.attribute = highestGainAttribute;
		
		String[] newRemainingAttributeList = new String[attributeListExceptClassAttr.length - 1];
		for (int i=0, j=0; i < attributeListExceptClassAttr.length; i++) {
			if(attributeListExceptClassAttr[i]!=attributeList[highestGainAttribute]) {
				newRemainingAttributeList[j++] = attributeListExceptClassAttr[i];
			}
		}
		
		HashMap<String, ArrayList<String[]>> split = new HashMap<String, ArrayList<String[]>>();
		for(String[] dataValue : data) {
			String value = dataValue[highestGainAttribute];
			ArrayList<String[]> dataInstances = split.get(value);
			if(dataInstances == null) {
				dataInstances = new ArrayList<String[]>();
				split.put(value, dataInstances);
			}
			dataInstances.add(dataValue);
		}
		
		node.nodes = new Node[split.size()];
		node.attributeValues = new String[split.size()];
		node.classNode = false;
		
		int index=0;
		for(HashMap.Entry<String, ArrayList<String[]>> entry : split.entrySet()) {
			node.attributeValues[index] = entry.getKey();
			node.nodes[index] = runID3algorithm(attributeList, classAttributeValues,
					classAttributeIndex, newRemainingAttributeList, entry.getValue());
			index++;
		}
		return node;
	}
		
	public static double calWeightedEntropy(HashSet<String> classAttributeValues,
			int classAttributeIndex,
			ArrayList<String[]>data, 
			int attributeIndex, String value) {
		
		HashMap<String, Integer> attributeValueFreq = new HashMap<String, Integer>();
		int attributeValueInstanceCount = 0;
		
		for(String[] dataValue : data) {
			if(dataValue[attributeIndex].equals(value)) {
				String targetValue = dataValue[classAttributeIndex];
				if(attributeValueFreq.get(targetValue) == null) {
					attributeValueFreq.put(targetValue, 1);
				} else {
					attributeValueFreq.put(targetValue, 
							attributeValueFreq.get(targetValue) + 1);
				}
				attributeValueInstanceCount++;
			}
		}
		
		double entropy = 0;
		for (String res :  classAttributeValues) {
			Integer count = attributeValueFreq.get(res);
			if(count != null) {
				double freq = count/(double) attributeValueInstanceCount;
				entropy -= freq * Math.log(freq)/Math.log(2);
			}
		}
		return entropy;
	}
	
	public static HashMap<String, Integer> FreqOfValuesInAttribute (ArrayList<String[]> data,
			int attributeIndex) {
		HashMap<String, Integer> freqOfAttributeValues = new HashMap<String, Integer>();
		
		for(String[] dataValue : data) {
			String value = dataValue[attributeIndex];
			if(freqOfAttributeValues.get(value) == null) {
				freqOfAttributeValues.put(value, 1);
			} else {
				freqOfAttributeValues.put(value, 
						freqOfAttributeValues.get(value) + 1);
			}
		}		
		return freqOfAttributeValues;
	}
}
