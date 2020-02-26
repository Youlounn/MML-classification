package org.xtext.example.mydsl.tests;

import java.io.BufferedReader; 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xtext.example.mydsl.mml.*;

import com.google.common.io.Files;
import com.google.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(MmlInjectorProvider.class)
public class MmlParsingJavaCompilerXGBoost {
	@Inject
	ParseHelper<MMLModel> parseHelper;
	
	public MMLModel getModel(String fileName) throws Exception {
		File file = new File("prog1.mml"); 
		BufferedReader br = new BufferedReader(new FileReader(file));
		String prog = "", line;
		while ((line = br.readLine()) != null) {
			prog += line;
		}
		br.close();
		System.out.println(prog);
		return parseHelper.parse(prog);
	}
	
	public void compileDataInput(MMLModel model, MLAlgorithm al, int numAlgo) throws Exception {
		DataInput dataInput = model.getInput();
		String fileLocation = dataInput.getFilelocation();
		String pythonImport = "import xgboost as xgb\n"; 
		String pythonImport2 = "import pandas as pd\r\n" + "from sklearn.model_selection import train_test_split\r\n";
		
		pythonImport+=pythonImport2;
		String DEFAULT_COLUMN_SEPARATOR = ","; // by default
		String csv_separator = DEFAULT_COLUMN_SEPARATOR;
		CSVParsingConfiguration parsingInstruction = dataInput.getParsingInstruction();
		
		if (parsingInstruction != null) {			
			System.err.println("parsing instruction..." + parsingInstruction);
			csv_separator = parsingInstruction.getSep().toString();
		}
			
		String csvReading = "mml_data = pd.read_csv(" + mkValueInSingleQuote(fileLocation) + ", sep=" + mkValueInSingleQuote(csv_separator) + ")\n";							
		String x ="", y="";
		
		//Formula
		RFormula f = model.getFormula();
		if(f == null) {
			y = "Y = mml_data.iloc[:,-1]\n";
			x = "X = mml_data.iloc[:, :-1]\n";
		}else {
			//Predictives
			FormulaItem fi = f.getPredictive();
			String colName = fi.getColName();
			int colIndex = fi.getColumn();
			String column = "";
			if(colName.length() > 0 )
				column = colName;
			else
				column = colIndex+"";
			y = "Y = mml_data[\""+column+"\"]\n";
			
			//Predictors
			XFormula preds = f.getPredictors();
			if(preds instanceof AllVariables)
				x = "X = mml_data.drop(columns=[\""+column+ "\"])\n";
			else if(preds instanceof PredictorVariables) {
				FormulaItem[] predictors = (FormulaItem[]) ((PredictorVariables) preds).getVars().toArray();
				String predictorsCol = "";
				FormulaItem item;
				for(int i = 0; i < predictors.length; i++) {
					item = predictors[i];
					colName = item.getColName();
					colIndex = item.getColumn();
					column = "";
					if(colName.length() > 0 )
						column = colName;
					else
						column = colIndex+"";
					predictorsCol+="'"+column+"',";
				}
				if(predictorsCol.length() > 0) {
					predictorsCol =  predictorsCol.substring(0, predictorsCol.length()-1);
				}
				x = "X = mml_data[["+predictorsCol+"]]\n";
			}
		}
		
		//Algorithm		
		String algorithm = "";
		if (al instanceof DT) {
			DT defAlg = (DT) al;
			int maxDepth = defAlg.getMax_depth();
			if(maxDepth == 0)
				algorithm = "clf = xgb.XGBClassifier()\n";
			else
				algorithm = "clf = xgb.XGBClassifier(max_depth="+maxDepth+")\n";
		}
		else if (al instanceof SVM) {
			//Non supporte par XGBoost
			
		}
		else if (al instanceof RandomForest) {
			//Non supporte par XGBoost
			RandomForest defAlg = (RandomForest) al;
			pythonImport+="from sklearn.ensemble import RandomForestClassifier \n";
			algorithm+="#Algorithme non gere\n";
			algorithm+="clf = RandomForestClassifier(n_estimators=100)\n";

		}
		else if (al instanceof LogisticRegression) {
			//Non supporte par XGBoost
			//Potentiellement resolvable par l'outil Regressor de XGBoost
			LogisticRegression defAlg = (LogisticRegression) al;
			pythonImport+="from sklearn.linear_model import LogisticRegression \n";
			algorithm+="#Algorithme non gere\n";
			algorithm+="clf = LogisticRegression(solver='lbfgs', multi_class='auto')\n";
		}
		
		
		Validation validation = model.getValidation();
		StratificationMethod strat = validation.getStratification();
		EList<ValidationMetric> metric = validation.getMetric();
		ValidationMetric[] metriquesArray = (ValidationMetric[]) metric.toArray();
		String stratificationAndMetrique = "";
		int number; 
		boolean macro, accuracy = false;
		String metriques = "", metrique="";
		String printScores = "";
		
		if (strat instanceof CrossValidation) {
			number = strat.getNumber();
			metriques = "scoring = [";
			for(int i = 0; i < metriquesArray.length; i++) {
				macro = false;
				metrique = metriquesArray[i].getLiteral().toLowerCase();
				if((metrique.equals("accuracy") || metrique.equals("macro_accuracy")) && accuracy) {
					if(i == metriquesArray.length-1) {
						metriques +="]\n";
					}
					continue;
				}
				if(metrique.equals("accuracy") || metrique.equals("macro_accuracy"))
					accuracy = true;
				if (metrique.contains("macro")){
					metrique = metrique.substring(6, metrique.length());
					macro = true;
					if(!metrique.equals("accuracy")) {
						metrique += "_macro";
					}
				}else {
					if(!metrique.equals("accuracy") && !metrique.equals("balanced_accuracy")) {
						metrique += "_micro";
					}
				}
				if(i != metriquesArray.length-1)
					metriques+= "'"+metrique + "',";
				else
					metriques+= "'"+metrique + "'] \n";
				if(macro)
					printScores+="print('"+metriquesArray[i].getLiteral()+" : ')\n";
				else
					printScores+="print('"+metrique+" : ')\n";
				printScores+="print(sum(scores['test_"+metrique+"']) / len(scores['test_"+metrique+"']))\n";
			}
			pythonImport+="from sklearn.model_selection import cross_validate\n";
			stratificationAndMetrique = metriques+"scores = cross_validate(clf,X, Y, scoring=scoring,cv="+number+")\n";
		} 
		else if (strat instanceof TrainingTest) {
			metrique = "";
			number = strat.getNumber();
			stratificationAndMetrique = "test_size = 0."+number+"\nX_train, X_test, y_train, y_test = train_test_split(X, Y, test_size=test_size)\nclf.fit(X_train, y_train)\n";
			for(int i = 0; i < metriquesArray.length; i++) {
				metrique = metriquesArray[i].getLiteral();
				if(metrique == "accuracy") {
					pythonImport += "from sklearn.metrics import accuracy_score\n";
					stratificationAndMetrique+="accuracy = accuracy_score(y_test, clf.predict(X_test))\nprint('accuracy :')\nprint(accuracy)\n";
				}else if(metrique == "balanced_accuracy") {
					pythonImport += "from sklearn.metrics import balanced_accuracy_score\n";
					stratificationAndMetrique+="accuracy = balanced_accuracy_score(y_test, clf.predict(X_test))\nprint('balanced accuracy :')\nprint(accuracy)\n";
				}else if(metrique == "recall") {
					pythonImport += "from sklearn.metrics import recall_score\n";
					stratificationAndMetrique+="recall = recall_score(y_test, clf.predict(X_test),average='micro')\nprint('recall :')\nprint(recall)\n";
				}else if(metrique == "macro_recall") {
					pythonImport += "from sklearn.metrics import recall_score\n";
					stratificationAndMetrique+="recall = recall_score(y_test, clf.predict(X_test),average='macro')\nprint('recall :')\nprint(recall)\n";
				}else if(metrique == "precision") {
					pythonImport += "from sklearn.metrics import precision_score\n";
					stratificationAndMetrique+="precision = precision_score(y_test, clf.predict(X_test),average='micro')\nprint('precision :')\nprint(precision)\n";
				}else if(metrique == "macro_precision") {
					pythonImport += "from sklearn.metrics import precision_score\n";
					stratificationAndMetrique+="precision = precision_score(y_test, clf.predict(X_test),average='macro')\nprint('macro precision :')\nprint(precision)\n";
				}else if(metrique == "F1") {
					pythonImport += "from sklearn.metrics import f1_score\n";
					stratificationAndMetrique+="F1 = f1_score(y_test, clf.predict(X_test),average='micro')\nprint('F1 :')\nprint(F1)\n";
				}else if(metrique == "macro_F1") {
					pythonImport += "from sklearn.metrics import f1_score\n";
					stratificationAndMetrique+="F1 = f1_score(y_test, clf.predict(X_test),average='macro')\nprint('macro F1 :')\nprint(F1)\n";
				}
			}
		}
		
		//build python code
		String pythonCode = addPythonText("", pythonImport);
		pythonCode = addPythonText(pythonCode, csvReading);
		pythonCode = addPythonText(pythonCode, x);
		pythonCode = addPythonText(pythonCode, y);
		pythonCode = addPythonText(pythonCode, algorithm);
		pythonCode = addPythonText(pythonCode, stratificationAndMetrique);
		pythonCode = addPythonText(pythonCode, printScores);
		
		Files.write(pythonCode.getBytes(), new File("output_LAFONT_LEMANCEL_MANDE_RIALET/mml_"+numAlgo+".py"));
		// end of Python generation
		
		
		/*
		 * Calling generated Python script (basic solution through systems call)
		 * we assume that "python" is in the path
		 */
		Process p = Runtime.getRuntime().exec("python output_LAFONT_LEMANCEL_MANDE_RIALET/mml_"+numAlgo+".py");
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line; 
		while ((line = in.readLine()) != null) {
			System.out.println(line);
	    }
		
		
		
	}

	private String mkValueInSingleQuote(String val) {
		return "'" + val + "'";
	}
	
	private String addPythonText(String pythonCode, String toAdd) {
		return pythonCode + toAdd;
	}
}
