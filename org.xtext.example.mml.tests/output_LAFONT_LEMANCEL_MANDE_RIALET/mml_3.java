import java.io.File;
import java.util.Random;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.converters.CSVLoader;
import weka.core.Instances;
public class test {
	public static void main(String[] args) throws Exception {
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File("iris.csv"));
		Instances data = loader.getDataSet();
		data.randomize(new Random());

		data.setClassIndex(data.numAttributes()-1);
		System.out.println("Impossible d'utiliser SVM avec Weka");
	}
}