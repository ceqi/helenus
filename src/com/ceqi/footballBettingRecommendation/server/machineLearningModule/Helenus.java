package com.ceqi.footballBettingRecommendation.server.machineLearningModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression;
import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.ModelDissector;
import org.apache.mahout.classifier.sgd.ModelSerializer;

import com.ceqi.footballBettingRecommendation.server.features.EPLFeatures;
import com.ceqi.footballBettingRecommendation.server.features.EPLTeams;

/**
 * 
 * A classifier to predict football game result (win/lose).
 * 
 * use this classifier via invoking train method.
 * 
 * @author ce
 *
 */
public class Helenus {
	public static final int NUM_CATAGORIES = 2;
	private EPLTeams teamName;
	// constructor set up it.
	private List<Example> examples = new ArrayList<Example>();
	// get them after training
	private AdaptiveLogisticRegression learningAlgo = null;
	private CrossFoldLearner learner = null;
	// get after model generated.
	String modelPath = null;

	// the constructor should setup the examples for me
	public Helenus(List<String> localDatasets, EPLTeams teamName,
			List<EPLFeatures> featureNamesList, int history) {
		ExampleParser exampleParser = new ExampleParser(teamName,
				localDatasets, history);
		List<Example> examplesList = exampleParser
				.extractExamples(featureNamesList);
		this.teamName = teamName;
		examples = examplesList;
	}

	/**
	 * train using AdaptiveLogisticRegression
	 */
	public void train() {
		System.out.println(this.teamName.toString() + ": " + examples.size()
				+ " training examples");
		double aucTotal = 0;
		double auc = 0, perctCorrect = 0;
		int run = 0;
		while (true) {
			learningAlgo = new AdaptiveLogisticRegression(NUM_CATAGORIES,
					Example.FEATURES, new L1());
			Collections.shuffle(examples);

			learningAlgo.setInterval(800);
			learningAlgo.setAveragingWindow(500);
			for (int pass = 0; pass < 20; pass++) {
				for (Example observation : examples) {
					learningAlgo.train(observation.getTarget(),
							observation.asVector());

				}

			}
			learningAlgo.close();
			CrossFoldLearner tmpLearner = learningAlgo.getBest().getPayload()
					.getLearner();
			tmpLearner.close();

			// find highest auc
			if (auc == 0 || auc < tmpLearner.auc()) {
				auc = tmpLearner.auc();
				perctCorrect = tmpLearner.percentCorrect();
				learner = tmpLearner;
				learner.close();
			}
			aucTotal = aucTotal + tmpLearner.auc();

			run++;
			if (auc > 0.7 || run > 50) {
				break;
			}

		}
		System.out.println(String.format(
				"model's auc: %.2f, correct percentage: %.0f%%", auc,
				perctCorrect * 100));
		// System.out.println("avg auc: " + aucTotal / run);

	}

	/**
	 * dissect a model
	 *
	 * 
	 */
	// TODO: find out which features to drop, so in App class can train the
	// model again with used features only
	public void dissect() {

		ModelDissector modelDissector = new ModelDissector();
		Collections.shuffle(examples);
		for (Example observation : examples) {

			observation.getTraceDict().clear();
			modelDissector.update(observation.asVector(),
					observation.getTraceDict(), learner);

		}

		List<ModelDissector.Weight> weights = modelDissector.summary(20);
		System.out.println("Model Dissection");
		for (ModelDissector.Weight w : weights) {
			System.out.printf("%s\t%.8f\t\n", w.getFeature(), w.getWeight());
		}

		// if weight equals to zero, keep a record of the corresponding feature

	}

	public void createModel(String modelName) throws IOException {

		File file = new File(modelName);
		modelPath = file.getPath() + ".model";
		ModelSerializer.writeBinary(modelPath, learner);
	}

	public CrossFoldLearner getModel() throws IOException {

		InputStream in = new FileInputStream(modelPath);
		CrossFoldLearner best = ModelSerializer.readBinary(in,
				CrossFoldLearner.class);
		in.close();
		return best;
	}

	/*
	 * Getters
	 */

	public List<Example> getExamples() {
		return examples;
	}

	public double getLearnerAuc() {
		return learner.auc();
	}

}
