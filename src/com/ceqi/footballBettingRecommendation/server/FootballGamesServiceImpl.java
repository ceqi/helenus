package com.ceqi.footballBettingRecommendation.server;

import java.util.ArrayList;

import com.ceqi.footballBettingRecommendation.client.FootballGamesService;
import com.ceqi.footballBettingRecommendation.server.machineLearningModule.Prediction;
import com.ceqi.footballBettingRecommendation.shared.ScoreWithPos;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class FootballGamesServiceImpl extends RemoteServiceServlet implements
		FootballGamesService {

	@Override
	public ArrayList<String> getGames() {
		return Prediction.getGames();
	}

	@Override
	public ArrayList<ScoreWithPos> getPredictions() {
		ArrayList<ScoreWithPos> scoreWithPosList = new ArrayList<ScoreWithPos>();
		int size = Prediction.getGames().size() * 2;
		ArrayList<String> scores = Prediction.getScores();
		ArrayList<Integer> rows = Prediction.getRows();
		ArrayList<Integer> cols = Prediction.getCols();

		for (int i = 0; i < size; i++) {
			String score = scores.get(i);
			int row = rows.get(i);
			int col = cols.get(i);
			scoreWithPosList.add(new ScoreWithPos(score, row, col));
		}
		return scoreWithPosList;
	}

}
