package org.xtext.example.mydsl.tests;

import java.io.BufferedReader; 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xtext.example.mydsl.mml.*;

import com.google.common.io.Files;
import com.google.inject.Inject;

@ExtendWith(InjectionExtension.class)
@InjectWith(MmlInjectorProvider.class)
public class MmlParsingJavaCompiler {
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
	
	@Test
	public void loadModel1() throws Exception {	
		/*MMLModel model = getModel("prog1.mml");
		Assertions.assertNotNull(model);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		Assertions.assertTrue(errors.isEmpty(), "Unexpected errors");*/			
	}
	
	@Test
	public void compileDataInput() throws Exception {
		MMLModel model = parseHelper.parse("datainput \"iris.csv\"\n"
				+ "mlframework xgboost\n"
				+ "algorithm DT\n"
				+ "mlframework Weka\n"
				+ "algorithm RF\n"
				+ "mlframework Weka\n"
				+ "algorithm LogisticRegression\n"
				+ "TrainingTest { percentageTraining 30 }\n"
				+ "balanced_accuracy recall precision F1 accuracy macro_recall macro_precision macro_F1 macro_accuracy\n" 
				+ "");	
		
		//Algorithm
		MLChoiceAlgorithm[] algos = (MLChoiceAlgorithm[]) model.getAlgorithms().toArray();
		
		
		for(int i = 0; i < algos.length; i++) {
			MLAlgorithm al = (MLAlgorithm) algos[i].getAlgorithm();
			FrameworkLang framework = algos[i].getFramework();
			if (framework.getLiteral() == "scikit-learn") {
				MmlParsingJavaCompilerPython compiler = new MmlParsingJavaCompilerPython();
				compiler.compileDataInput(model,al,i+1);
			}else if(framework.getLiteral() == "R") {
				//Traitement R
			}else if(framework.getLiteral() == "Weka") {
				//Traitement Weka
			}else if(framework.getLiteral() == "xgboost") {
				System.out.println("test");
			}
		}		
	}
}
